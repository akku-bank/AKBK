import React, { useState } from 'react';
import { View, StyleSheet, SafeAreaView, TouchableOpacity, ScrollView, Alert } from 'react-native';
import { scale, verticalScale } from 'react-native-size-matters';
import CustomText from '../../../components/common/CustomText';

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
    const [currentQuestionIndex, setCurrentQuestionIndex] = useState(0);
    const [selectedOption, setSelectedOption] = useState(null);
    const [isAnswerRevealed, setIsAnswerRevealed] = useState(false);

    // AI 크레딧 및 힌트 상태
    const [aiCredits, setAiCredits] = useState(3);
    const [isHintUsed, setIsHintUsed] = useState(false);

    const q = MOCK_QUESTIONS[currentQuestionIndex];

    const handleSelectOption = (index) => {
        if (isAnswerRevealed) return;
        setSelectedOption(index);
    };

    const handleUseHint = () => {
        if (isHintUsed) return;
        if (aiCredits <= 0) {
            Alert.alert('알림', 'AI 크레딧이 부족합니다.');
            return;
        }

        Alert.alert(
            '힌트 보기',
            'AI 크레딧 1개를 사용하여 힌트를 볼까요?',
            [
                { text: '취소', style: 'cancel' },
                {
                    text: '사용하기',
                    onPress: () => {
                        setAiCredits(prev => prev - 1);
                        setIsHintUsed(true);
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
        const isCorrect = selectedOption === q.answerIndex;

        if (isCorrect) {
            Alert.alert('정답입니다! 🎉', '보상으로 100 젤링을 받았습니다!');
        } else {
            Alert.alert('아쉬워요.', '다음 문제를 노려보세요!');
        }
    };

    const handleNext = () => {
        if (currentQuestionIndex < MOCK_QUESTIONS.length - 1) {
            setCurrentQuestionIndex(prev => prev + 1);
            setSelectedOption(null);
            setIsAnswerRevealed(false);
            setIsHintUsed(false); // 힌트 초기화
        } else {
            Alert.alert('완료', '모든 퀴즈를 풀었습니다.', [{ text: '확인', onPress: () => navigation.goBack() }]);
        }
    };

    return (
        <SafeAreaView style={styles.safeArea}>
            <View style={styles.header}>
                <TouchableOpacity style={styles.backButton} onPress={() => navigation.goBack()}>
                    <CustomText style={styles.backButtonText}>←</CustomText>
                </TouchableOpacity>
                <CustomText style={styles.headerTitle}>AI 금융 퀴즈</CustomText>
                <View style={{ width: scale(32) }} />
            </View>

            <ScrollView contentContainerStyle={styles.container}>
                <View style={styles.topInfoRow}>
                    <View style={styles.progressBox}>
                        <CustomText style={styles.progressText}>문제 {currentQuestionIndex + 1} / {MOCK_QUESTIONS.length}</CustomText>
                    </View>
                    <View style={styles.creditBox}>
                        <CustomText style={styles.creditText}>🤖 힌트 크레딧: {aiCredits}개</CustomText>
                    </View>
                </View>

                {/* 질문 버블 */}
                <View style={styles.chatBubble}>
                    <CustomText style={styles.questionText}>{q.question}</CustomText>
                </View>

                {/* AI 힌트 영역 */}
                {!isAnswerRevealed && (
                    <View style={styles.hintSection}>
                        {isHintUsed ? (
                            <View style={styles.hintBubble}>
                                <CustomText style={styles.hintLabel}>🤖 AI 아꾸:</CustomText>
                                <CustomText style={styles.hintMessage}>{q.hint}</CustomText>
                            </View>
                        ) : (
                            <TouchableOpacity style={styles.hintButton} onPress={handleUseHint}>
                                <CustomText style={styles.hintButtonText}>💡 AI 힌트 보기 (-1 크레딧)</CustomText>
                            </TouchableOpacity>
                        )}
                    </View>
                )}

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

            </ScrollView>

            <View style={styles.footer}>
                {!isAnswerRevealed ? (
                    <TouchableOpacity style={[styles.mainButton, selectedOption === null && styles.disabledButton]} onPress={handleSubmit}>
                        <CustomText style={styles.mainButtonText}>정답확인</CustomText>
                    </TouchableOpacity>
                ) : (
                    <TouchableOpacity style={styles.mainButton} onPress={handleNext}>
                        <CustomText style={styles.mainButtonText}>{currentQuestionIndex < MOCK_QUESTIONS.length - 1 ? '다음 문제' : '종료하기'}</CustomText>
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

    topInfoRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: verticalScale(24) },
    progressBox: { backgroundColor: '#E5E7EB', paddingHorizontal: scale(12), paddingVertical: verticalScale(6), borderRadius: scale(16) },
    progressText: { fontSize: scale(12), fontWeight: 'bold', color: '#4B5563' },
    creditBox: { backgroundColor: '#F3E8FF', paddingHorizontal: scale(12), paddingVertical: verticalScale(6), borderRadius: scale(16) },
    creditText: { fontSize: scale(12), fontWeight: 'bold', color: '#7E22CE' },

    chatBubble: { backgroundColor: '#FFFFFF', padding: scale(20), borderRadius: scale(20), borderBottomLeftRadius: 0, marginBottom: verticalScale(16), shadowColor: '#000', shadowOffset: { width: 0, height: verticalScale(2) }, shadowOpacity: 0.05, shadowRadius: scale(4), elevation: 2 },
    questionText: { fontSize: scale(18), fontWeight: '900', color: '#111', lineHeight: 28 },

    hintSection: { marginBottom: verticalScale(24), alignItems: 'flex-start' },
    hintButton: { backgroundColor: '#DBEAFE', paddingHorizontal: scale(16), paddingVertical: verticalScale(10), borderRadius: scale(16) },
    hintButtonText: { fontSize: scale(13), fontWeight: 'bold', color: '#1D4ED8' },
    hintBubble: { backgroundColor: '#F0FDF4', padding: scale(16), borderRadius: scale(16), borderBottomLeftRadius: 0, borderWidth: 1, borderColor: '#BBF7D0', alignSelf: 'stretch' },
    hintLabel: { fontSize: scale(12), fontWeight: 'bold', color: '#166534', marginBottom: verticalScale(4) },
    hintMessage: { fontSize: scale(14), fontWeight: '600', color: '#14532D', lineHeight: 20 },

    optionsContainer: { gap: verticalScale(12) },

    optionBtn: { backgroundColor: '#FFFFFF', padding: scale(16), borderRadius: scale(12), borderWidth: 2, borderColor: '#FFFFFF' },
    optionText: { fontSize: scale(16), fontWeight: '600', color: '#4B5563' },

    selectedBtn: { borderColor: '#A3E635', backgroundColor: '#F7FEE7' },
    selectedText: { color: '#4D7C0F' },

    correctBtn: { borderColor: '#A3E635', backgroundColor: '#A3E635' },
    correctText: { color: '#111' },

    wrongBtn: { borderColor: '#EF4444', backgroundColor: '#FEE2E2' },
    wrongText: { color: '#B91C1C' },

    explanationBox: { marginTop: verticalScale(24), backgroundColor: '#FEF3C7', padding: scale(16), borderRadius: scale(12) },
    explanationTitle: { fontSize: scale(14), fontWeight: 'bold', color: '#D97706', marginBottom: verticalScale(4) },
    explanationText: { fontSize: scale(14), color: '#92400E', lineHeight: 20 },

    footer: { padding: scale(16), backgroundColor: '#F3F4F6' },
    mainButton: { backgroundColor: '#A3E635', paddingVertical: verticalScale(16), borderRadius: scale(16), alignItems: 'center' },
    disabledButton: { backgroundColor: '#D1D5DB' },
    mainButtonText: { fontSize: scale(16), fontWeight: 'bold', color: '#111' }
});

export default QuizScreen;
