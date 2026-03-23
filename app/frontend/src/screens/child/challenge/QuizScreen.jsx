import React, { useState, useRef, useEffect } from 'react';
import { View, StyleSheet, SafeAreaView, TouchableOpacity, ScrollView, Alert, KeyboardAvoidingView, Platform, Image } from 'react-native';
import { scale, verticalScale } from 'react-native-size-matters';
import CustomText from '../../../components/common/CustomText';
import CustomTextInput from '../../../components/common/CustomTextInput';
import api from '../../../api/axios';
import useAuthStore from '../../../store/useAuthStore';
import EventSource from 'react-native-sse';

// 더미 형태의 질문들
const MOCK_QUESTIONS = [
    {
        id: 1,
        question: "용돈을 받았을 때 가장 먼저 해야 할 일은 무엇일까요?",
        options: ["친구들과 모두 쓰기", "일부 저축하고 계획 세우기", "비싼 장난감 사기", "나중에 생각하기"],
        answerIndex: 1,
        explanation: "용돈을 받으면 먼저 저축할 금액을 떼어두고 계획을 세우는 것이 좋아요.",
        hint: "무엇이든 '계획'을 세우는 것이 중요해요! 돈도 마찬가지랍니다."
    },
    {
        id: 2,
        question: "필요한 물건을 사기 위해 돈을 따로 모으는 것을 의미하는 단어는?",
        options: ["소비", "기부", "저축", "투자"],
        answerIndex: 2,
        explanation: "미래를 위해 혹은 필요한 물건을 사기 위해 돈을 모으는 것을 저축이라고 합니다.",
        hint: "은행에 이 단어가 들어가는 통장을 만들 수 있어요. '저'로 시작한답니다."
    }
];

const QuizScreen = ({ navigation }) => {
    const [quiz, setQuiz] = useState({
        id: null,
        question: "불러오는 중...",
        options: ["...", "...", "...", "..."],
        answerIndex: 0,
        explanation: "",
        hint: ""
    });
    const [selectedOption, setSelectedOption] = useState(null);
    const [isAnswerRevealed, setIsAnswerRevealed] = useState(false);

    // AI 크레딧, 채팅 상태
    const [aiCredits, setAiCredits] = useState(3);
    const [isHintUsed, setIsHintUsed] = useState(false);

    // 채팅 관련
    const [chatInput, setChatInput] = useState('');
    const [chatHistory, setChatHistory] = useState([]);
    const [isAiTyping, setIsAiTyping] = useState(false);

    const scrollViewRef = useRef(null);

    const q = quiz;

    useEffect(() => {
        const fetchQuiz = async () => {
            try {
                // 난이도는 'NORMAL'로 임의 고정 또는 이전 화면에서 선택된 파라미터 활용
                const res = await api.get('/challenges/quizzes?difficulty=NORMAL');
                const qData = res.data?.data;

                if (!qData || !qData.problemJson) {
                    console.log('퀴즈 데이터가 없거나 인증 에러가 발생했습니다.');
                    return; // 상태 업데이트 생략 (불러오는 중... 유지 및 fallback 데이터 사용)
                }

                const problem = JSON.parse(qData.problemJson);

                setQuiz({
                    id: qData.quizId,
                    question: problem.question,
                    options: problem.options,
                    answerIndex: problem.answer_index, // 클라이언트가 알 필요는 없지만 UI용 유지
                    explanation: qData.explanation,
                    hint: problem.hint || "힌트를 생성해드릴게요."
                });
                setAiCredits(qData.remainingCredits);
            } catch (e) { console.error('Quiz Fetch Error', e); }
        };
        fetchQuiz();
    }, []);

    // 하드웨어 뒤로가기 및 스와이프 제스처 방어
    useEffect(() => {
        const unsubscribe = navigation.addListener('beforeRemove', (e) => {
            if (aiCredits < 3 && !isAnswerRevealed) {
                // 화면 이동 차단
                e.preventDefault();
                Alert.alert('알림', '이미 힌트를 사용하여 난이도를 변경하거나 포기할 수 없어요!');
            }
        });
        return unsubscribe;
    }, [navigation, aiCredits, isAnswerRevealed]);

    const handleGoBack = () => {
        if (aiCredits < 3 && !isAnswerRevealed) {
            Alert.alert('알림', '이미 힌트를 사용하여 난이도를 변경하거나 포기할 수 없어요!');
            return;
        }
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
            Alert.alert('알림', '더 이상 AI 크레딧을 사용할 수 없어요.');
            return;
        }

        const userMsg = chatInput;
        setChatInput('');

        Alert.alert(
            '질문하기',
            'AI 크레딧 1개를 사용하여 힌트를 받을까요?',
            [
                { text: '취소', style: 'cancel' },
                {
                    text: '사용하기',
                    onPress: async () => {
                        setAiCredits(prev => prev - 1);
                        setChatHistory(prev => [
                            ...prev,
                            { sender: 'user', text: userMsg }
                        ]);
                        setIsHintUsed(true);
                        setIsAiTyping(true);

                        setTimeout(() => {
                            scrollViewRef.current?.scrollToEnd({ animated: true });
                        }, 100);

                        try {
                            if (quiz.id) {
                                // 1. AI 응답 생성 트리거 API 호출
                                await api.post('/challenges/quizzes/chat', { quizId: quiz.id, message: userMsg });

                                // 2. SSE 연결 설정 및 응답 대기
                                const token = useAuthStore.getState().token;
                                const url = `${api.defaults.baseURL}/challenges/quizzes/chat/stream?quizId=${quiz.id}`;

                                const source = new EventSource(url, {
                                    headers: { Authorization: `Bearer ${token}` }
                                });

                                source.addEventListener('connected', (event) => {
                                    console.log('SSE 연결 완료:', event.data);
                                });

                                source.addEventListener('chat-response', (event) => {
                                    try {
                                        const parsed = JSON.parse(event.data);
                                        const aiReply = parsed.reply || '답변을 가져오지 못했습니다.';

                                        setChatHistory(prev => [
                                            ...prev,
                                            { sender: 'ai', text: aiReply }
                                        ]);
                                    } catch (err) {
                                        console.error('SSE Message Parsing Error', err);
                                        setChatHistory(prev => [...prev, { sender: 'ai', text: '오류가 발생했습니다.' }]);
                                    }
                                    setIsAiTyping(false);

                                    setTimeout(() => {
                                        scrollViewRef.current?.scrollToEnd({ animated: true });
                                    }, 100);

                                    source.close();
                                });

                                source.addEventListener('error', (err) => {
                                    console.error('SSE EventSource Error', err);
                                    setIsAiTyping(false);
                                    source.close();
                                });

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
                        }
                    }
                }
            ]
        );
    };

    const handleSubmit = () => {
        if (selectedOption === null) {
            Alert.alert('알림', '정답을 선택해주세요!');
            return;
        }

        setIsAnswerRevealed(true);
        const submitTask = async () => {
            try {
                if (!quiz.id) {
                    Alert.alert('정답 확인 완료 (임시)', isCorrect ? '정답입니다!' : '틀렸습니다.');
                    return;
                }
                const res = await api.post('/challenges/quizzes/answer', { quizId: quiz.id, selectedAnswer: selectedOption });
                const ansData = res.data?.data;
                if (!ansData) {
                    Alert.alert('안내', '정답 확인 중 데이터가 반환되지 않았습니다.');
                    return;
                }
                const { isCorrect: isServerCorrect, jellingReward } = ansData;

                if (isServerCorrect) {
                    Alert.alert('정답입니다! 🎉', `보상으로 ${jellingReward} 젤링을 받았습니다!`);
                } else {
                    Alert.alert('아쉬워요.', '다음 기회를 노려보세요!');
                }
            } catch (e) {
                console.error('Quiz Submit Error', e.response?.data || e.message);
                // 에러 발생 시 임시 로직
                if (isCorrect) Alert.alert('정답입니다! 🎉', '보상으로 10 젤링을 받았습니다! (임시)');
                else Alert.alert('아쉬워요.', '다음 문제를 노려보세요! (임시)');
            }
        };
        submitTask();
    };

    const handleNext = () => {
        Alert.alert('완료', '오늘의 퀴즈를 모두 풀었습니다.', [{ text: '확인', onPress: () => navigation.goBack() }]);
    };

    return (
        <SafeAreaView style={styles.safeArea}>
            <View style={styles.header}>
                <TouchableOpacity style={styles.backButton} onPress={handleGoBack}>
                    <CustomText style={styles.backButtonText}>←</CustomText>
                </TouchableOpacity>
                <CustomText style={styles.headerTitle}>AI 금융 퀴즈</CustomText>
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
                            <CustomText style={styles.progressText}>오늘의 퀴즈</CustomText>
                        </View>
                        <View style={styles.creditBox}>
                            <CustomText style={styles.creditText}>🤖 힌트 크레딧: {aiCredits}개</CustomText>
                        </View>
                    </View>

                    {/* 질문 버블 */}
                    <View style={styles.chatBubble}>
                        <CustomText style={styles.questionText}>{q.question}</CustomText>
                    </View>

                    {/* 옵션 컨테이너가 채팅창 위로 올라간다 */}
                    <View style={styles.optionsContainer}>
                        {q.options.map((option, index) => {
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

                    {isAnswerRevealed && (
                        <View style={styles.explanationBox}>
                            <CustomText style={styles.explanationTitle}>해설</CustomText>
                            <CustomText style={styles.explanationText}>{q.explanation}</CustomText>
                        </View>
                    )}

                    <View style={styles.divider} />

                    {/* 하단 채팅 영역 (문제와 보기 아래) */}
                    <View style={styles.chatArea}>
                        {/* 입력 폼이 먼저 나옴 */}
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

                        {chatHistory.length > 0 && (
                            <View style={styles.chatSection}>
                                {chatHistory.map((chat, idx) => (
                                    <View key={idx} style={[styles.chatRow, chat.sender === 'user' ? styles.chatRowUser : styles.chatRowAi]}>
                                        {chat.sender === 'ai' && <Image source={require('../../../assets/croco/croco_face.png')} style={styles.aiAvatar} />}
                                        <View style={[styles.chatMsgBubble, chat.sender === 'user' ? styles.userMsgBubble : styles.aiMsgBubble]}>
                                            <CustomText style={[styles.chatMsgText, chat.sender === 'user' ? styles.userMsgText : styles.aiMsgText]}>
                                                {chat.text}
                                            </CustomText>
                                        </View>
                                    </View>
                                ))}
                                {isAiTyping && (
                                    <View style={[styles.chatRow, styles.chatRowAi]}>
                                        <Image source={require('../../../assets/croco/croco_face.png')} style={styles.aiAvatar} />
                                        <View style={[styles.chatMsgBubble, styles.aiMsgBubble]}>
                                            <CustomText style={[styles.chatMsgText, styles.aiMsgText]}>답변을 생각 중이에요...</CustomText>
                                        </View>
                                    </View>
                                )}
                            </View>
                        )}
                    </View>

                </ScrollView>
            </KeyboardAvoidingView>

            <View style={styles.footer}>
                {!isAnswerRevealed ? (
                    <TouchableOpacity style={[styles.mainButton, selectedOption === null && styles.disabledButton]} onPress={handleSubmit}>
                        <CustomText style={styles.mainButtonText}>정답확인</CustomText>
                    </TouchableOpacity>
                ) : (
                    <TouchableOpacity style={styles.mainButton} onPress={handleNext}>
                        <CustomText style={styles.mainButtonText}>종료하기</CustomText>
                    </TouchableOpacity>
                )}
            </View>
        </SafeAreaView>
    );
};

const styles = StyleSheet.create({
    safeArea: { flex: 1, backgroundColor: '#F3F4F6' },
    header: {
        flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between',
        paddingHorizontal: scale(16), paddingVertical: verticalScale(16), backgroundColor: '#FFFFFF'
    },
    backButton: { width: scale(32), height: scale(32), justifyContent: 'center' },
    backButtonText: { fontSize: scale(22), fontWeight: 'bold', color: '#111' },
    headerTitle: { fontSize: scale(18), fontWeight: 'bold', color: '#111' },

    container: { flexGrow: 1, paddingHorizontal: scale(16), paddingTop: verticalScale(20), paddingBottom: verticalScale(40) },

    topInfoRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: verticalScale(16) },
    progressBox: { backgroundColor: '#E5E7EB', paddingHorizontal: scale(12), paddingVertical: verticalScale(6), borderRadius: scale(16) },
    progressText: { fontSize: scale(12), fontWeight: 'bold', color: '#4B5563' },
    creditBox: { backgroundColor: '#F3E8FF', paddingHorizontal: scale(12), paddingVertical: verticalScale(6), borderRadius: scale(16) },
    creditText: { fontSize: scale(12), fontWeight: 'bold', color: '#7E22CE' },

    chatBubble: { backgroundColor: '#FFFFFF', padding: scale(20), borderRadius: scale(20), marginBottom: verticalScale(16), shadowColor: '#000', shadowOffset: { width: 0, height: verticalScale(2) }, shadowOpacity: 0.05, shadowRadius: scale(4), elevation: 2 },
    questionText: { fontSize: scale(18), fontWeight: '900', color: '#111', lineHeight: 28 },

    optionsContainer: { gap: verticalScale(12), marginBottom: verticalScale(24) },

    optionBtn: { backgroundColor: '#FFFFFF', padding: scale(16), borderRadius: scale(12), borderWidth: 2, borderColor: '#FFFFFF' },
    optionText: { fontSize: scale(16), fontWeight: '600', color: '#4B5563' },

    selectedBtn: { borderColor: '#A3E635', backgroundColor: '#F7FEE7' },
    selectedText: { color: '#4D7C0F' },

    correctBtn: { borderColor: '#A3E635', backgroundColor: '#A3E635' },
    correctText: { color: '#111' },

    wrongBtn: { borderColor: '#EF4444', backgroundColor: '#FEE2E2' },
    wrongText: { color: '#B91C1C' },

    explanationBox: { marginBottom: verticalScale(24), backgroundColor: '#FEF3C7', padding: scale(16), borderRadius: scale(12) },
    explanationTitle: { fontSize: scale(14), fontWeight: 'bold', color: '#D97706', marginBottom: verticalScale(4) },
    explanationText: { fontSize: scale(14), color: '#92400E', lineHeight: 20 },

    divider: { height: 1, backgroundColor: '#E5E7EB', marginVertical: verticalScale(16) },

    chatArea: {
        flex: 1,
        justifyContent: 'flex-start',
        minHeight: verticalScale(150),
    },
    chatSection: { marginTop: verticalScale(16), marginBottom: verticalScale(16) },
    chatRow: { flexDirection: 'row', alignItems: 'flex-start', marginBottom: verticalScale(12) },
    chatRowUser: { justifyContent: 'flex-end', marginLeft: scale(32) },
    chatRowAi: { justifyContent: 'flex-start', marginRight: scale(32) },
    aiAvatar: { width: scale(32), height: scale(32), marginRight: scale(8), marginTop: verticalScale(4), resizeMode: 'contain' },
    chatMsgBubble: { paddingHorizontal: scale(16), paddingVertical: verticalScale(12), borderRadius: scale(16) },
    userMsgBubble: { backgroundColor: '#111', borderBottomRightRadius: 0 },
    aiMsgBubble: { backgroundColor: '#F7FEE7', borderBottomLeftRadius: 0, borderWidth: 1, borderColor: '#A3E635' },
    chatMsgText: { fontSize: scale(14), fontWeight: '600', lineHeight: 20 },
    userMsgText: { color: '#FFFFFF' },
    aiMsgText: { color: '#4D7C0F' },

    chatInputWrapper: { flexDirection: 'row', alignItems: 'center', backgroundColor: '#FFFFFF', borderRadius: scale(16), padding: scale(4), borderWidth: 2, borderColor: '#A3E635' },
    chatInput: { flex: 1, paddingHorizontal: scale(12), paddingVertical: verticalScale(12), fontSize: scale(14), color: '#111' },
    sendBtn: { backgroundColor: '#A3E635', paddingHorizontal: scale(16), paddingVertical: verticalScale(10), borderRadius: scale(12), justifyContent: 'center' },
    sendBtnDisabled: { backgroundColor: '#E5E7EB' },
    sendBtnText: { fontSize: scale(14), fontWeight: 'bold', color: '#111' },

    footer: { padding: scale(16), backgroundColor: '#F3F4F6', borderTopWidth: 1, borderTopColor: '#E5E7EB' },
    mainButton: { backgroundColor: '#A3E635', paddingVertical: verticalScale(16), borderRadius: scale(16), alignItems: 'center' },
    disabledButton: { backgroundColor: '#D1D5DB' },
    mainButtonText: { fontSize: scale(16), fontWeight: 'bold', color: '#111' }
});

export default QuizScreen;
