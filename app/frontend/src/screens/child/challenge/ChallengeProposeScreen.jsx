import React, { useState, useEffect } from 'react';
import { View, StyleSheet, TouchableOpacity, SafeAreaView, ScrollView, Alert, KeyboardAvoidingView, Platform } from 'react-native';
import { scale, verticalScale } from 'react-native-size-matters';
import CustomText from '../../../components/common/CustomText';
import CustomTextInput from '../../../components/common/CustomTextInput';
import api from '../../../api/axios';

const CATEGORIES = ['간식', '쇼핑', '게임', '기타'];

const ChallengeProposeScreen = ({ navigation, route }) => {
    const editingChallenge = route?.params?.challenge || null;
    const isEditMode = Boolean(editingChallenge?.challengeId);

    const [selectedCategory, setSelectedCategory] = useState(editingChallenge?.category || CATEGORIES[0]);
    const [goalAmount, setGoalAmount] = useState('');
    const [rewardAmount, setRewardAmount] = useState('');

    useEffect(() => {
        if (isEditMode) {
            setSelectedCategory(editingChallenge?.category || CATEGORIES[0]);
            setGoalAmount(String(editingChallenge?.targetSpending || ''));
            setRewardAmount(String(editingChallenge?.rewardAmount || ''));
        }

        // [UI 테스트용 임시 주석] 주말(토, 일)에만 제안 가능 로직
        // const today = new Date().getDay();
        // if (today !== 0 && today !== 6) {
        //     Alert.alert('알림', '용돈 챌린지 제안은 주말(토, 일)에만 가능해요!', [
        //         { text: '확인', onPress: () => navigation.goBack() }
        //     ]);
        // }
    }, [editingChallenge, isEditMode, navigation]);

    const handleSubmit = async () => { // Added async here
        if (!goalAmount || isNaN(goalAmount)) {
            Alert.alert('알림', '목표 금액을 정확히 입력해주세요.');
            return;
        }

        if (!rewardAmount || isNaN(rewardAmount)) {
            Alert.alert('알림', '보상 금액을 정확히 입력해주세요.');
            return;
        }

        try {
            const payload = {
                category: selectedCategory,
                targetSpending: parseInt(goalAmount, 10),
                rewardAmount: parseInt(rewardAmount, 10),
            };

            if (isEditMode) {
                await api.patch(`/challenges/spending/${editingChallenge.challengeId}`, payload);
                Alert.alert('수정 완료', `"${selectedCategory}" 챌린지를 수정했어요!`, [{ text: '확인', onPress: () => navigation.goBack() }]);
            } else {
                await api.post('/challenges/spending', payload);
                Alert.alert('제안 완료', `부모님께 "${selectedCategory}" 소비 목표 챌린지를 제안했어요!`, [{ text: '확인', onPress: () => navigation.goBack() }]);
            }
        } catch (e) {
            console.error('Challenge Propose Error', e);
            if (e.response?.status === 409) {
                Alert.alert('안내', '이미 같은 카테고리의 챌린지를 요청했어요.');
                return;
            }

            Alert.alert('오류', isEditMode ? '챌린지 수정 중 문제가 발생했습니다.' : '챌린지 생성 중 문제가 발생했습니다.');
        }
    };

    return (
        <SafeAreaView style={styles.safeArea}>
            <View style={styles.header}>
                <TouchableOpacity style={styles.backButton} onPress={() => navigation.goBack()}>
                    <CustomText style={styles.backButtonText}>←</CustomText>
                </TouchableOpacity>
                <CustomText style={styles.headerTitle}>{isEditMode ? '용돈 미션 수정하기' : '용돈 미션 제안하기'}</CustomText>
                <View style={{ width: scale(32) }} />
            </View>

            <KeyboardAvoidingView style={{ flex: 1 }} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
                <ScrollView contentContainerStyle={styles.container} showsVerticalScrollIndicator={false}>

                    <View style={styles.instructionCard}>
                        <CustomText style={styles.instructionTitle}>{isEditMode ? '어떤 내용으로 다시 제안할까요?' : '어떤 소비 목표를 정할까요?'}</CustomText>
                        <CustomText style={styles.instructionDesc}>
                            {isEditMode
                                ? '반려되었거나 승인 대기 중인 챌린지를 수정해서 다시 요청할 수 있어요.'
                                : '이번 주에 내가 얼마까지 사용할지 카테고리와 \n목표 금액을 정해 부모님께 제안해보세요.'}
                        </CustomText>
                    </View>

                    <CustomText style={styles.sectionLabel}>소비 카테고리</CustomText>
                    <View style={styles.categoryRow}>
                        {CATEGORIES.map(cat => (
                            <TouchableOpacity
                                key={cat}
                                style={[styles.categoryBtn, selectedCategory === cat && styles.categoryBtnActive]}
                                onPress={() => setSelectedCategory(cat)}
                            >
                                <CustomText style={[styles.categoryText, selectedCategory === cat && styles.categoryTextActive]}>{cat}</CustomText>
                            </TouchableOpacity>
                        ))}
                    </View>

                    <CustomText style={styles.sectionLabel}>얼마까지 쓸까요?</CustomText>
                    <View style={styles.inputContainer}>
                        <CustomTextInput
                            style={styles.amountInput}
                            placeholder="예: 3000"
                            placeholderTextColor="#9CA3AF"
                            keyboardType="numeric"
                            value={goalAmount}
                            onChangeText={setGoalAmount}
                        />
                        <CustomText style={styles.currencyText}>원</CustomText>
                    </View>

                    <CustomText style={styles.sectionLabel}>성공하면 얼마를 받을까요?</CustomText>
                    <View style={styles.inputContainer}>
                        <CustomTextInput
                            style={styles.amountInput}
                            placeholder="예: 1000"
                            placeholderTextColor="#9CA3AF"
                            keyboardType="numeric"
                            value={rewardAmount}
                            onChangeText={setRewardAmount}
                        />
                        <CustomText style={styles.currencyText}>원</CustomText>
                    </View>
                </ScrollView>
                <View style={styles.footer}>
                    <TouchableOpacity style={styles.submitButton} onPress={handleSubmit}>
                        <CustomText style={styles.submitButtonText}>{isEditMode ? '챌린지 수정하기' : '부모님께 제안하기'}</CustomText>
                    </TouchableOpacity>
                </View>
            </KeyboardAvoidingView>
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
    container: { flexGrow: 1, paddingHorizontal: scale(16), paddingTop: verticalScale(20), paddingBottom: verticalScale(40) },

    instructionCard: {
        backgroundColor: '#FFFFFF', borderRadius: scale(16), padding: scale(20), marginBottom: verticalScale(24),
        shadowColor: '#000', shadowOffset: { width: 0, height: verticalScale(2) }, shadowOpacity: 0.05, shadowRadius: scale(8), elevation: 2
    },
    instructionTitle: { fontSize: scale(18), fontWeight: 'bold', color: '#111', marginBottom: verticalScale(8) },
    instructionDesc: { fontSize: scale(13), color: '#6B7280', lineHeight: 20 },

    sectionLabel: { fontSize: scale(14), fontWeight: 'bold', color: '#4B5563', marginBottom: verticalScale(12) },

    categoryRow: { flexDirection: 'row', flexWrap: 'wrap', gap: scale(8), marginBottom: verticalScale(24) },
    categoryBtn: {
        backgroundColor: '#FFFFFF', paddingVertical: verticalScale(10), paddingHorizontal: scale(16),
        borderRadius: scale(20), borderWidth: 1, borderColor: '#E5E7EB'
    },
    categoryBtnActive: { backgroundColor: '#A3E635', borderColor: '#A3E635' },
    categoryText: { fontSize: scale(14), fontWeight: '600', color: '#6B7280' },
    categoryTextActive: { color: '#111' }, // 라임 바탕엔 검정글씨

    inputContainer: {
        flexDirection: 'row', alignItems: 'center', backgroundColor: '#FFFFFF',
        borderRadius: scale(12), paddingHorizontal: scale(16), marginBottom: verticalScale(24), borderWidth: 1, borderColor: '#E5E7EB'
    },
    amountInput: { flex: 1, fontSize: scale(18), fontWeight: 'bold', color: '#111', paddingVertical: verticalScale(16) },
    currencyText: { fontSize: scale(16), fontWeight: 'bold', color: '#111' },
    footer: { paddingHorizontal: scale(16), paddingBottom: verticalScale(24), paddingTop: verticalScale(12), backgroundColor: '#F9FAFB' },
    submitButton: { backgroundColor: '#A3E635', paddingVertical: verticalScale(16), borderRadius: scale(12), alignItems: 'center' },
    submitButtonText: { fontSize: scale(16), fontWeight: 'bold', color: '#111' }
});

export default ChallengeProposeScreen;
