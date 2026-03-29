import React, { useState, useRef, useEffect } from 'react';
import { View, StyleSheet, SafeAreaView, TouchableOpacity, ScrollView, Alert, KeyboardAvoidingView, Platform, Image, Modal } from 'react-native';
import { scale, verticalScale } from 'react-native-size-matters';
import CustomText from '../../../components/common/CustomText';
import CustomTextInput from '../../../components/common/CustomTextInput';
import api from '../../../api/axios';
import useAuthStore from '../../../store/useAuthStore';
import EventSource from 'react-native-sse';

const parseProblemJson = (problemJson) => {
    if (!problemJson) return null;

    try {
        const parsed = JSON.parse(problemJson);
        return {
            question: parsed.question || '문제를 불러오지 못했습니다.',
            options: Array.isArray(parsed.options) ? parsed.options : (Array.isArray(parsed.choices) ? parsed.choices : ['...', '...', '...', '...']),
            answerIndex: Number.isInteger(parsed.answer_index) ? parsed.answer_index : (Number.isInteger(parsed.answerIndex) ? parsed.answerIndex : 0),
            hint: parsed.hint || '힌트를 생성해드릴게요.',
        };
    } catch (error) {
        console.error('Problem JSON Parse Error', error);
        return null;
    }
};

const normalizeQuizData = (qData) => {
    const parsedProblem = parseProblemJson(qData?.problemJson);
    const isSubmitted = Boolean(qData?.isSubmitted);
    const isCorrect = qData?.isCorrect;
    const answerIndex = qData?.correctChoiceNo != null
        ? qData.correctChoiceNo - 1
        : parsedProblem?.answerIndex ?? 0;

    return {
        id: qData?.quizId ?? null,
        question: qData?.question || parsedProblem?.question || '문제를 불러오지 못했습니다.',
        options: parsedProblem?.options || ['...', '...', '...', '...'],
        answerIndex,
        explanation: qData?.explanation || '',
        hint: parsedProblem?.hint || '힌트를 생성해드릴게요.',
        rewardAmount: qData?.rewardAmount ?? null,
        remainingCredits: qData?.remainingCredits ?? 100,
        chatHistory: parseChatHistory(qData?.chatJson),
        isSubmitted,
        isCorrect,
        submittedOptionIndex: isSubmitted && isCorrect === true ? answerIndex : null,
    };
};

const parseChatHistory = (chatJson) => {
    if (!chatJson) return [];

    try {
        const parsed = JSON.parse(chatJson);
        const source = Array.isArray(parsed)
            ? parsed
            : Array.isArray(parsed.messages)
                ? parsed.messages
                : Array.isArray(parsed.chat_history)
                    ? parsed.chat_history
                    : [];

        return source
            .map((item) => {
                const sender = item.sender || item.role || item.type;
                const text = item.text || item.message || item.content || item.reply;

                if (!text) return null;

                return {
                    sender: sender === 'assistant' || sender === 'ai' || sender === 'bot' ? 'ai' : 'user',
                    text,
                };
            })
            .filter(Boolean);
    } catch (error) {
        console.error('Chat JSON Parse Error', error);
        return [];
    }
};

const difficultyLabelMap = {
    EASY: '쉬움',
    NORMAL: '보통',
    HARD: '어려움',
};

const QuizScreen = ({ navigation, route }) => {
    const sseRef = useRef(null);
    const difficulty = route?.params?.difficulty || 'NORMAL';
    const [quiz, setQuiz] = useState({
        id: null,
        question: "불러오는 중...",
        options: ["...", "...", "...", "..."],
        answerIndex: 0,
        explanation: "",
        hint: "",
        rewardAmount: null,
    });
    const [selectedOption, setSelectedOption] = useState(null);
    const [isAnswerRevealed, setIsAnswerRevealed] = useState(false);
    const [isQuizSubmitted, setIsQuizSubmitted] = useState(false);
    const [submittedResult, setSubmittedResult] = useState(null);
    const [resultModal, setResultModal] = useState({
        visible: false,
        isCorrect: false,
        message: '',
    });

    // AI 크레딧, 채팅 상태
    const [aiCredits, setAiCredits] = useState(100);
    const [isHintUsed, setIsHintUsed] = useState(false);

    // 채팅 관련
    const [chatInput, setChatInput] = useState('');
    const [chatHistory, setChatHistory] = useState([]);
    const [isAiTyping, setIsAiTyping] = useState(false);

    const scrollViewRef = useRef(null);

    const q = quiz;
    const difficultyLabel = difficultyLabelMap[difficulty] || difficulty;

    const navigateToHome = () => {
        sseRef.current?.close();
        sseRef.current = null;
        navigation.navigate('ChildMain', { screen: 'Home' });
    };

    useEffect(() => {
        const fetchQuiz = async () => {
            setQuiz({
                id: null,
                question: "불러오는 중...",
                options: ["...", "...", "...", "..."],
                answerIndex: 0,
                explanation: "",
                hint: "",
                rewardAmount: null,
            });
            setSelectedOption(null);
            setIsAnswerRevealed(false);
            setIsQuizSubmitted(false);
            setSubmittedResult(null);
            setResultModal({ visible: false, isCorrect: false, message: '' });
            setAiCredits(100);
            setIsHintUsed(false);
            setChatInput('');
            setChatHistory([]);
            setIsAiTyping(false);
            sseRef.current?.close();
            sseRef.current = null;

            try {
                const res = await api.get(`/challenges/quizzes?difficulty=${difficulty}`);
                const qData = res.data?.data;

                if (!qData) {
                    console.log('퀴즈 데이터가 없거나 인증 에러가 발생했습니다.');
                    return;
                }

                const normalizedQuiz = normalizeQuizData(qData);

                setQuiz({
                    id: normalizedQuiz.id,
                    question: normalizedQuiz.question,
                    options: normalizedQuiz.options,
                    answerIndex: normalizedQuiz.answerIndex,
                    explanation: normalizedQuiz.explanation,
                    hint: normalizedQuiz.hint,
                    rewardAmount: normalizedQuiz.rewardAmount,
                });
                setAiCredits(normalizedQuiz.remainingCredits);
                setChatHistory(normalizedQuiz.chatHistory);
                setIsQuizSubmitted(normalizedQuiz.isSubmitted);
                setSubmittedResult(normalizedQuiz.isCorrect);
                setSelectedOption(normalizedQuiz.submittedOptionIndex);
                setIsAnswerRevealed(normalizedQuiz.isSubmitted);
            } catch (e) { console.error('Quiz Fetch Error', e); }
        };
        fetchQuiz();
    }, [difficulty]);

    useEffect(() => {
        return () => {
            sseRef.current?.close();
            sseRef.current = null;
        };
    }, []);

    const handleGoBack = () => {
        navigation.goBack();
    };

    const handleSelectOption = (index) => {
        if (isAnswerRevealed) return;
        setSelectedOption(index);
    };

    const handleSendChat = () => {
        if (!chatInput.trim()) return;
        if (isAnswerRevealed) return;
        if (isAiTyping) return;

        if (aiCredits <= 0) {
            Alert.alert('알림', '더 이상 힌트를 사용할 수 없어요.');
            return;
        }

        const userMsg = chatInput;
        setChatInput('');
        setChatHistory(prev => [
            ...prev,
            { sender: 'user', text: userMsg }
        ]);
        setIsHintUsed(true);
        setIsAiTyping(true);

        setTimeout(() => {
            scrollViewRef.current?.scrollToEnd({ animated: true });
        }, 100);

        (async () => {
            try {
                if (quiz.id) {
                    const token = useAuthStore.getState().token;
                    const url = `${api.defaults.baseURL}/challenges/quizzes/chat/stream`;

                    sseRef.current?.close();

                    const source = new EventSource(url, {
                        headers: { Authorization: `Bearer ${token}` }
                    });
                    sseRef.current = source;

                    source.addEventListener('connected', (event) => {
                        console.log('SSE 연결 완료:', event.data);
                    });

                    source.addEventListener('chat-response', (event) => {
                        try {
                            const parsed = JSON.parse(event.data);
                            const aiReply = parsed.ai_reply || parsed.reply || '답변을 가져오지 못했습니다.';
                            const deductedCredits = Number(parsed.deducted_credits ?? parsed.deductedCredits ?? 0);

                            setChatHistory(prev => [
                                ...prev,
                                { sender: 'ai', text: aiReply }
                            ]);
                            setAiCredits(prev => Math.max(0, prev - deductedCredits));
                        } catch (err) {
                            console.error('SSE Message Parsing Error', err);
                            setChatHistory(prev => [...prev, { sender: 'ai', text: '오류가 발생했습니다.' }]);
                        }
                        setIsAiTyping(false);

                        setTimeout(() => {
                            scrollViewRef.current?.scrollToEnd({ animated: true });
                        }, 100);

                        source.close();
                        sseRef.current = null;
                    });

                    source.addEventListener('error', (err) => {
                        console.error('SSE EventSource Error', err);
                        setChatHistory(prev => [...prev, { sender: 'ai', text: '힌트 응답을 받지 못했습니다.' }]);
                        setIsAiTyping(false);
                        source.close();
                        sseRef.current = null;
                    });

                    await api.post('/challenges/quizzes/chat', { quizId: quiz.id, message: userMsg });
                } else {
                    // Fallback (No quiz id)
                    setTimeout(() => {
                        setChatHistory(prev => [...prev, { sender: 'ai', text: q.hint }]);
                        setIsAiTyping(false);
                    }, 1200);
                }
            } catch (e) {
                console.error('AI Chat Error', e);
                setChatHistory(prev => [...prev, { sender: 'ai', text: '서버 연결에 실패했습니다.' }]);
                setIsAiTyping(false);
                sseRef.current?.close();
                sseRef.current = null;
            }
        })();
    };

    const handleSubmit = () => {
        if (selectedOption === null) {
            Alert.alert('알림', '정답을 선택해주세요!');
            return;
        }

        const submitTask = async () => {
            try {
                if (!quiz.id) {
                    Alert.alert('안내', '퀴즈 정보가 없어 정답을 제출할 수 없습니다.');
                    return;
                }
                const res = await api.post('/challenges/quizzes/answer', { quizId: quiz.id, selectedAnswer: selectedOption });
                const ansData = res.data?.data;
                if (!ansData) {
                    Alert.alert('안내', '정답 확인 중 데이터가 반환되지 않았습니다.');
                    return;
                }
                const { isCorrect: isServerCorrect, jellingReward, correctChoiceNo } = ansData;
                setQuiz(prev => ({ ...prev, answerIndex: correctChoiceNo - 1 }));
                setIsAnswerRevealed(true);
                setIsQuizSubmitted(true);
                setSubmittedResult(isServerCorrect);

                if (isServerCorrect) {
                    setResultModal({
                        visible: true,
                        isCorrect: true,
                        message: `보상으로 ${jellingReward} 젤링을 받았습니다!`,
                    });
                } else {
                    setResultModal({
                        visible: true,
                        isCorrect: false,
                        message: '다음 기회를 노려보세요!',
                    });
                }
            } catch (e) {
                console.error('Quiz Submit Error', e.response?.data || e.message);
                Alert.alert('오류', '정답 제출에 실패했습니다.');
            }
        };
        submitTask();
    };

    const visibleOptions = isAnswerRevealed
        ? q.options.filter((_, index) => index === q.answerIndex)
        : q.options;

    return (
        <SafeAreaView style={styles.safeArea}>
            <View style={styles.header}>
                <TouchableOpacity style={styles.backButton} onPress={handleGoBack}>
                    <CustomText style={styles.backButtonText}>←</CustomText>
                </TouchableOpacity>
                <CustomText style={styles.headerTitle}>금융 퀴즈</CustomText>
                <View style={{ width: scale(32) }} />
            </View>

            <KeyboardAvoidingView style={{ flex: 1 }} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
                <ScrollView
                    ref={scrollViewRef}
                    contentContainerStyle={styles.container}
                    showsVerticalScrollIndicator={false}
                >
                    <View style={styles.topInfoRow}>
                        <View style={styles.progressBox}>
                            <CustomText style={styles.progressText}>{difficultyLabel}</CustomText>
                        </View>
                        <View style={styles.creditBox}>
                            <CustomText style={styles.creditText}>힌트 크레딧: {aiCredits}개</CustomText>
                        </View>
                    </View>

                    {/* 질문 버블 */}
                    <View style={styles.chatBubble}>
                        <CustomText style={styles.questionText}>{q.question}</CustomText>
                    </View>

                    {/* 옵션 컨테이너가 채팅창 위로 올라간다 */}
                    <View style={styles.optionsContainer}>
                        {visibleOptions.map((option, visibleIndex) => {
                            const index = isAnswerRevealed
                                ? q.answerIndex
                                : visibleIndex;
                            let btnStyle = styles.optionBtn;
                            let textStyle = styles.optionText;

                            if (isAnswerRevealed) {
                                if (index === q.answerIndex) {
                                    btnStyle = [styles.optionBtn, styles.correctBtn];
                                    textStyle = [styles.optionText, styles.correctText];
                                } else if (index === selectedOption) {
                                    btnStyle = [styles.optionBtn, styles.wrongBtn];
                                    textStyle = [styles.optionText, styles.wrongText];
                                }
                            } else {
                                if (selectedOption === index) {
                                    btnStyle = [styles.optionBtn, styles.selectedBtn];
                                    textStyle = [styles.optionText, styles.selectedText];
                                }
                            }

                            return (
                                <TouchableOpacity
                                    key={index}
                                    style={btnStyle}
                                    onPress={() => handleSelectOption(index)}
                                    activeOpacity={isAnswerRevealed ? 1 : 0.7}
                                    disabled={isAnswerRevealed}
                                >
                                    <CustomText style={textStyle}>{index + 1}. {option}</CustomText>
                                </TouchableOpacity>
                            );
                        })}
                    </View>

                    {!isAnswerRevealed && !isQuizSubmitted && (
                        <View style={styles.answerActionRow}>
                            <TouchableOpacity
                                style={[styles.answerCheckButton, selectedOption === null && styles.disabledButton]}
                                onPress={handleSubmit}
                            >
                                <CustomText style={styles.answerCheckButtonText}>정답 확인</CustomText>
                            </TouchableOpacity>
                        </View>
                    )}

                    {isAnswerRevealed && q.explanation ? (
                        <View style={styles.explanationContainer}>
                            <View style={styles.explanationHeaderRow}>
                                <Image source={require('../../../assets/croco/quiz_akku.png')} style={styles.aiAvatar} />
                                <CustomText style={styles.explanationTitleText}>아꾸의 해설</CustomText>
                            </View>
                            <View style={[styles.chatMsgBubble, styles.explanationMsgBubble]}>
                                <CustomText style={[styles.chatMsgText, styles.aiMsgText]}>{q.explanation}</CustomText>
                            </View>
                        </View>
                    ) : null}

                    {!isAnswerRevealed && <View style={styles.divider} />}

                    {/* 하단 채팅 영역 (문제와 보기 아래) */}
                    {!isAnswerRevealed && (
                        <View style={styles.chatArea}>
                            {chatHistory.length > 0 && (
                                <View style={styles.chatSection}>
                                    {chatHistory.map((chat, idx) => (
                                        <View key={idx} style={[styles.chatRow, chat.sender === 'user' ? styles.chatRowUser : styles.chatRowAi]}>
                                            {chat.sender === 'ai' && <Image source={require('../../../assets/croco/quiz_akku.png')} style={styles.aiAvatar} />}
                                            <View
                                                style={[
                                                    styles.chatMsgBubble,
                                                    chat.sender === 'user'
                                                        ? styles.userMsgBubble
                                                        : chat.kind === 'explanation'
                                                            ? styles.explanationMsgBubble
                                                            : styles.aiMsgBubble
                                                ]}
                                            >
                                                <CustomText style={[styles.chatMsgText, chat.sender === 'user' ? styles.userMsgText : styles.aiMsgText]}>
                                                    {chat.text}
                                                </CustomText>
                                            </View>
                                        </View>
                                    ))}
                                    {isAiTyping && (
                                        <View style={[styles.chatRow, styles.chatRowAi]}>
                                            <Image source={require('../../../assets/croco/quiz_akku.png')} style={styles.aiAvatar} />
                                            <View style={[styles.chatMsgBubble, styles.aiMsgBubble]}>
                                                <CustomText style={[styles.chatMsgText, styles.aiMsgText]}>답변을 생각 중이에요...</CustomText>
                                            </View>
                                        </View>
                                    )}
                                </View>
                            )}

                            {!isAnswerRevealed && (
                                <View style={styles.chatInputWrapper}>
                                    <CustomTextInput
                                        style={styles.chatInput}
                                        placeholder="힌트가 필요하면 물어보세요!"
                                        placeholderTextColor="#9CA3AF"
                                        value={chatInput}
                                        onChangeText={setChatInput}
                                        editable={!isAnswerRevealed && !isAiTyping}
                                    />
                                    <TouchableOpacity style={[styles.sendBtn, (!chatInput.trim() || isAiTyping) && styles.sendBtnDisabled]} onPress={handleSendChat}>
                                        <CustomText style={styles.sendBtnText}>전송</CustomText>
                                    </TouchableOpacity>
                                </View>
                            )}
                        </View>
                    )}

                </ScrollView>
            </KeyboardAvoidingView>

            <Modal
                visible={resultModal.visible}
                transparent
                animationType="fade"
                onRequestClose={() => setResultModal((prev) => ({ ...prev, visible: false }))}
            >
                <View style={styles.resultModalOverlay}>
                    <View style={styles.resultModalCard}>
                        <View style={styles.resultImageWrap}>
                            <Image
                                source={
                                    resultModal.isCorrect
                                        ? require('../../../assets/croco/akku-welcome.png')
                                        : require('../../../assets/croco/akku-tired.png')
                                }
                                style={styles.resultImage}
                                resizeMode="contain"
                            />
                        </View>
                        <CustomText style={styles.resultTitle}>
                            {resultModal.isCorrect ? '정답입니다!' : '아쉬워요.'}
                        </CustomText>
                        <CustomText style={styles.resultDescription}>{resultModal.message}</CustomText>
                        <TouchableOpacity
                            style={styles.resultButton}
                            onPress={() => setResultModal((prev) => ({ ...prev, visible: false }))}
                            activeOpacity={0.85}
                        >
                            <CustomText style={styles.resultButtonText}>확인</CustomText>
                        </TouchableOpacity>
                    </View>
                </View>
            </Modal>
        </SafeAreaView>
    );
};

const styles = StyleSheet.create({
    safeArea: { flex: 1, backgroundColor: '#ECFCCB' },
    header: {
        flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between',
        paddingHorizontal: scale(16), paddingVertical: verticalScale(16), backgroundColor: '#FFFFFF'
    },
    backButton: { width: scale(32), height: scale(32), justifyContent: 'center' },
    backButtonText: { fontSize: scale(22), fontWeight: 'bold', color: '#111' },
    headerTitle: { fontSize: scale(18), fontWeight: 'bold', color: '#111' },

    container: { flexGrow: 1, paddingHorizontal: scale(16), paddingTop: verticalScale(14), paddingBottom: verticalScale(8) },

    topInfoRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: verticalScale(10) },
    progressBox: { backgroundColor: '#FFFFFF', paddingHorizontal: scale(12), paddingVertical: verticalScale(6), borderRadius: scale(16), shadowColor: '#000', shadowOffset: { width: 0, height: 2 }, shadowOpacity: 0.05, shadowRadius: 4, elevation: 1, },
    progressText: { fontSize: scale(12), fontWeight: 'bold', color: '#4D7C0F' },
    creditBox: { backgroundColor: '#FFFFFF', paddingHorizontal: scale(12), paddingVertical: verticalScale(6), borderRadius: scale(16), shadowColor: '#000', shadowOffset: { width: 0, height: 2 }, shadowOpacity: 0.05, shadowRadius: 4, elevation: 1, },
    creditText: { fontSize: scale(12), fontWeight: 'bold', color: '#4D7C0F' },

    chatBubble: { backgroundColor: '#FFFFFF', padding: scale(16), borderRadius: scale(20), marginBottom: verticalScale(10), shadowColor: '#000', shadowOffset: { width: 0, height: verticalScale(2) }, shadowOpacity: 0.05, shadowRadius: scale(4), elevation: 2 },
    questionText: { fontSize: scale(18), fontWeight: '900', color: '#111', lineHeight: scale(32) },

    optionsContainer: { gap: verticalScale(8), marginBottom: verticalScale(14) },

    optionBtn: { backgroundColor: '#FFFFFF', padding: scale(14), borderRadius: scale(12), borderWidth: 2, borderColor: '#FFFFFF', shadowColor: '#000', shadowOffset: { width: 0, height: 2 }, shadowOpacity: 0.05, shadowRadius: 4, elevation: 1, },
    optionText: { fontSize: scale(16), fontWeight: '600', color: '#4B5563', lineHeight: scale(24) },

    selectedBtn: { borderColor: '#A3E635', backgroundColor: '#F7FEE7' },
    selectedText: { color: '#4D7C0F' },

    correctBtn: { borderColor: '#A3E635', backgroundColor: '#A3E635' },
    correctText: { color: '#111' },

    wrongBtn: { borderColor: '#EF4444', backgroundColor: '#FEE2E2' },
    wrongText: { color: '#B91C1C' },

    answerActionRow: { alignItems: 'flex-end', marginBottom: verticalScale(4) },
    answerCheckButton: {
        backgroundColor: '#A3E635',
        paddingHorizontal: scale(18),
        paddingVertical: verticalScale(10),
        borderRadius: scale(12),
    },
    answerCheckButtonText: { fontSize: scale(14), fontWeight: 'bold', color: '#111' },

    explanationContainer: { marginTop: verticalScale(16), marginBottom: verticalScale(16) },
    explanationHeaderRow: { flexDirection: 'row', alignItems: 'center', marginBottom: verticalScale(8) },
    explanationTitleText: { fontSize: scale(16), fontWeight: 'bold', color: '#111827' },

    divider: { height: 1, backgroundColor: '#E5E7EB', marginVertical: verticalScale(10) },

    chatArea: {
        flex: 1,
        justifyContent: 'flex-end',
        minHeight: verticalScale(120),
        marginBottom: verticalScale(24),
    },
    chatSection: { marginTop: verticalScale(8), marginBottom: verticalScale(10) },
    chatRow: { flexDirection: 'row', alignItems: 'flex-start', marginBottom: verticalScale(8) },
    chatRowUser: { justifyContent: 'flex-end', marginLeft: scale(32) },
    chatRowAi: { justifyContent: 'flex-start', marginRight: scale(32) },
    aiAvatar: { width: scale(32), height: scale(32), marginRight: scale(8), marginTop: verticalScale(1), resizeMode: 'contain' },
    chatMsgBubble: { paddingHorizontal: scale(14), paddingVertical: verticalScale(10), borderRadius: scale(16), shadowColor: '#000', shadowOffset: { width: 0, height: verticalScale(2) }, shadowOpacity: 0.08, shadowRadius: scale(4), elevation: 2 },
    userMsgBubble: { backgroundColor: '#FFFFFF', borderBottomRightRadius: 0 },
    aiMsgBubble: { backgroundColor: '#F7FEE7', borderTopLeftRadius: 0 },
    explanationMsgBubble: {
        backgroundColor: '#FFFFFF',
        borderTopLeftRadius: 0,
        shadowColor: '#000',
        shadowOffset: { width: 0, height: verticalScale(2) },
        shadowOpacity: 0.08,
        shadowRadius: scale(4),
        elevation: 2,
    },
    chatMsgText: { fontSize: scale(14), fontWeight: '600', lineHeight: scale(24) },
    userMsgText: { color: '#111827' },
    aiMsgText: { color: '#4D7C0F' },

    chatInputWrapper: { flexDirection: 'row', alignItems: 'center', backgroundColor: '#FFFFFF', borderRadius: scale(16), paddingLeft: scale(4), paddingVertical: scale(4), paddingRight: scale(4), shadowColor: '#000', shadowOffset: { width: 0, height: verticalScale(2) }, shadowOpacity: 0.08, shadowRadius: scale(8), elevation: 3 },
    chatInput: { flex: 1, paddingHorizontal: scale(12), paddingVertical: verticalScale(12), fontSize: scale(14), color: '#111' },
    sendBtn: { backgroundColor: '#A3E635', paddingHorizontal: scale(16), paddingVertical: verticalScale(10), borderRadius: scale(12), justifyContent: 'center', shadowColor: '#000', shadowOffset: { width: 0, height: 2 }, shadowOpacity: 0.05, shadowRadius: 4, elevation: 1, },
    sendBtnDisabled: { backgroundColor: '#E5E7EB' },
    sendBtnText: { fontSize: scale(14), fontWeight: 'bold', color: '#111' },

    resultModalOverlay: {
        flex: 1,
        backgroundColor: 'rgba(17, 24, 39, 0.38)',
        justifyContent: 'center',
        alignItems: 'center',
        paddingHorizontal: scale(24),
    },
    resultModalCard: {
        width: '100%',
        maxWidth: scale(320),
        backgroundColor: '#FFFFFF',
        borderRadius: scale(28),
        paddingHorizontal: scale(24),
        paddingTop: verticalScale(26),
        paddingBottom: verticalScale(22),
        alignItems: 'center',
        shadowColor: '#111827',
        shadowOffset: { width: 0, height: verticalScale(10) },
        shadowOpacity: 0.14,
        shadowRadius: scale(20),
        elevation: 12,
    },
    resultImageWrap: {
        width: scale(168),
        height: verticalScale(134),
        justifyContent: 'center',
        alignItems: 'center',
        marginBottom: verticalScale(8),
    },
    resultImage: {
        width: '100%',
        height: '100%',
    },
    resultTitle: {
        fontSize: scale(22),
        fontWeight: 'bold',
        color: '#111827',
        marginBottom: verticalScale(8),
    },
    resultDescription: {
        fontSize: scale(14),
        lineHeight: scale(20),
        color: '#6B7280',
        textAlign: 'center',
        marginBottom: verticalScale(20),
    },
    resultButton: {
        minWidth: scale(132),
        backgroundColor: '#A3E635',
        borderRadius: scale(999),
        paddingVertical: verticalScale(12),
        paddingHorizontal: scale(24),
        alignItems: 'center',
        shadowColor: '#000',
        shadowOffset: { width: 0, height: verticalScale(2) },
        shadowOpacity: 0.1,
        shadowRadius: scale(4),
        elevation: 3,
    },
    resultButtonText: {
        fontSize: scale(15),
        fontWeight: 'bold',
        color: '#1F2937',
    },

    disabledButton: { opacity: 0.5 },
});

export default QuizScreen;
