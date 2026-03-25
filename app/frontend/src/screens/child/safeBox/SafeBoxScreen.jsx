import React, { useState, useEffect, useRef } from 'react';
import { View, StyleSheet, SafeAreaView, ScrollView, TouchableOpacity, Alert, Animated, KeyboardAvoidingView, Platform, ActivityIndicator } from 'react-native';
import { RFValue } from 'react-native-responsive-fontsize';
import CustomText from '../../../components/common/CustomText';
import CustomTextInput from '../../../components/common/CustomTextInput';
import api from '../../../api/axios';

const DONATION_OPTIONS = [
    {
        id: 'arts-support',
        name: '문화 예술',
        description: '아이들을 위한 공연과 전시를 후원해요.',
        targetAmount: 5,
        emoji: '🎨',
    },
    {
        id: 'tree-planting',
        name: '나무심기',
        description: '도시 숲 조성과 나무 식재 활동에 기부해요.',
        targetAmount: 10,
        emoji: '🌳',
    },
    {
        id: 'animal-shelter',
        name: '유기동물 보호소',
        description: '보호소 사료와 치료비를 지원해요.',
        targetAmount: 15,
        emoji: '🐶',
    },
];

const SafeBoxScreen = ({ navigation }) => {
    const [isLoading, setIsLoading] = useState(true);
    const [hubInfo, setHubInfo] = useState(null); // { totalJelling, activeCharity: { id, name, description, goalAmount, currentAmount } }

    const [donateAmount, setDonateAmount] = useState('');
    const progressAnim = useRef(new Animated.Value(0)).current;

    useEffect(() => {
        fetchHubInfo();
    }, []);

    const fetchHubInfo = async () => {
        setIsLoading(true);
        try {
            const res = await api.get('/jelling-hub');
            const data = res.data?.data;
            if (data) {
                setHubInfo(data);
                if (data.activeCharity) {
                    animateProgress(data.activeCharity.currentAmount, data.activeCharity.targetAmount);
                }
            }
        } catch (e) {
            console.error('Fetch Hub Error:', e);
            Alert.alert('오류', '젤링 허브 정보를 불러오지 못했습니다.');
        } finally {
            setIsLoading(false);
        }
    };

    const animateProgress = (current, goal) => {
        const value = Math.min(current / (goal || 1), 1);
        Animated.timing(progressAnim, {
            toValue: value,
            duration: 800,
            useNativeDriver: false,
        }).start();
    };

    const handleSelectCharity = async (charity) => {
        Alert.alert(
            '기부하시겠어요?',
            `${charity.name}에 ${charity.targetAmount}젤링을 기부할까요?`,
            [
                { text: '취소', style: 'cancel' },
                {
                    text: '확인', onPress: async () => {
                        try {
                            setIsLoading(true);
                            await api.post('/jelling-hub/donations', { amount: charity.targetAmount });
                            await fetchHubInfo();
                            Alert.alert('성공', '기부 요청이 전송되었습니다.');
                        } catch (e) {
                            console.error('Donate Preset Error:', e);
                            Alert.alert('오류', '기부 요청에 실패했습니다.');
                            setIsLoading(false);
                        }
                    }
                }
            ]
        );
    };

    const handleDonate = async () => {
        const amount = parseInt(donateAmount);
        if (!amount || isNaN(amount) || amount <= 0) {
            Alert.alert('알림', '기부할 젤링을 정확히 입력해주세요!');
            return;
        }
        if (hubInfo.remainJelling < amount) {
            Alert.alert('알림', '가진 젤링이 부족해요!');
            return;
        }

        try {
            setIsLoading(true);
            await api.post('/jelling-hub/donations', { amount });
            setDonateAmount('');
            await fetchHubInfo();
        } catch (e) {
            console.error('Donate Error:', e);
            Alert.alert('오류', '기부에 실패했습니다.');
            setIsLoading(false);
        }
    };

    const handleGacha = async () => {
        try {
            setIsLoading(true);
            await api.post('/jelling-hub/donations/rewards');
            // 보상 수령 후 뽑기 화면으로 이동
            navigation.navigate('GachaScreen');
        } catch (e) {
            console.error('Reward Claim Error:', e);
            Alert.alert('오류', '보상 수령에 실패했습니다.');
            setIsLoading(false);
        }
    };

    if (isLoading && !hubInfo) {
        return (
            <SafeAreaView style={[styles.safeArea, { justifyContent: 'center', alignItems: 'center' }]}>
                <ActivityIndicator size="large" color="#10B981" />
            </SafeAreaView>
        );
    }

    const { remainJelling, activeCharity } = hubInfo || { remainJelling: 0, activeCharity: null };
    const isGoalReached = activeCharity && activeCharity.currentAmount >= activeCharity.targetAmount;

    return (
        <SafeAreaView style={styles.safeArea}>
            <View style={styles.header}>
                <CustomText style={styles.headerTitle}>젤링 주머니</CustomText>
            </View>

            <KeyboardAvoidingView style={{ flex: 1 }} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
                <ScrollView contentContainerStyle={styles.container}>
                    {/* 내 젤링 */}
                    <View style={styles.balanceHeader}>
                        <CustomText style={styles.balanceLabel}>내 젤링 주머니</CustomText>
                        <CustomText style={styles.balanceValue}>{remainJelling.toLocaleString()} 💎</CustomText>
                    </View>

                    {/* 모드 1: 기부처 없는 경우 (선택 모드) */}
                    {!activeCharity ? (
                        <View style={styles.charitySelectionBox}>
                            <CustomText style={styles.sectionTitle}>어디에 기부해 볼까요?</CustomText>
                            <CustomText style={{ color: '#6B7280', marginBottom: RFValue(16) }}>
                                원하는 기부처를 먼저 선택해주세요!
                            </CustomText>

                            {DONATION_OPTIONS.map((charity) => (
                                <TouchableOpacity
                                    key={charity.id}
                                    style={styles.charityItemCard}
                                    onPress={() => handleSelectCharity(charity)}
                                >
                                    <CustomText style={styles.targetEmoji}>{charity.emoji}</CustomText>
                                    <View style={styles.targetInfo}>
                                        <CustomText style={styles.targetTitle}>{charity.name}</CustomText>
                                        <CustomText style={styles.targetDesc}>{charity.description}</CustomText>
                                    </View>
                                    <View style={styles.goalPill}>
                                        <CustomText style={styles.goalPillText}>{`💎 x ${charity.targetAmount}`}</CustomText>
                                    </View>
                                </TouchableOpacity>
                            ))}
                        </View>
                    ) : (
                        /* 모드 2: 진행 중인 기부처 있는 경우 (기부 모드) */
                        <View style={styles.donationTargetBox}>
                            <CustomText style={styles.sectionTitle}>현재 기부 목표</CustomText>

                            <View style={styles.activeTargetCard}>
                                <CustomText style={styles.targetEmoji}>🌍</CustomText>
                                <View style={styles.targetInfo}>
                                    <CustomText style={styles.targetTitle}>{activeCharity.name}</CustomText>
                                    <CustomText style={styles.targetDesc}>열심히 기부해봐요!</CustomText>
                                </View>
                            </View>

                            {/* 게이지 바 영역 */}
                            <View style={styles.gaugeContainer}>
                                <View style={styles.gaugeTexts}>
                                    <CustomText style={styles.gaugeCurrentText}>{activeCharity.currentAmount} 💎</CustomText>
                                    <CustomText style={styles.gaugeGoalText}>/ {activeCharity.targetAmount} 💎</CustomText>
                                </View>
                                <View style={styles.progressBarBg}>
                                    <Animated.View style={[
                                        styles.progressBarFill,
                                        {
                                            width: progressAnim.interpolate({
                                                inputRange: [0, 1],
                                                outputRange: ['0%', '100%']
                                            })
                                        }
                                    ]} />
                                </View>
                                {isGoalReached && (
                                    <CustomText style={styles.goalReachedText}>🎉 목표 금액 달성 완료! 🎉</CustomText>
                                )}
                            </View>

                            {/* 액션 버튼 */}
                            {!isGoalReached ? (
                                <View style={styles.donateInputWrapper}>
                                    <View style={styles.donateInputContainer}>
                                        <CustomTextInput
                                            style={styles.donateInput}
                                            placeholder="기부할 금액 입력"
                                            placeholderTextColor="#9CA3AF"
                                            keyboardType="numeric"
                                            value={donateAmount}
                                            onChangeText={setDonateAmount}
                                        />
                                        <CustomText style={{ fontSize: RFValue(16), fontWeight: 'bold', color: '#111' }}>💎</CustomText>
                                    </View>
                                    <TouchableOpacity style={styles.donateButton} onPress={handleDonate}>
                                        <CustomText style={styles.donateButtonText}>기부</CustomText>
                                    </TouchableOpacity>
                                </View>
                            ) : (
                                <TouchableOpacity style={styles.gachaButton} onPress={handleGacha}>
                                    <CustomText style={styles.gachaButtonText}>기부 완료! 보상 획득하기 🎁</CustomText>
                                </TouchableOpacity>
                            )}
                        </View>
                    )}

                    {/* <TouchableOpacity style={styles.mapButton} onPress={() => navigation.navigate('BadgeMap')}>
                        <CustomText style={styles.mapButtonText}>내 기부 뱃지 맵 보기 🗺️</CustomText>
                    </TouchableOpacity> */}
                </ScrollView>
            </KeyboardAvoidingView>
        </SafeAreaView>
    );
};

const styles = StyleSheet.create({
    safeArea: { flex: 1, backgroundColor: '#F3F4F6' },
    header: { padding: RFValue(16), backgroundColor: '#FFF', alignItems: 'center' },
    headerTitle: { fontSize: RFValue(18), fontWeight: 'bold', color: '#111' },
    container: { padding: RFValue(20) },

    balanceHeader: { backgroundColor: '#FFF', borderRadius: RFValue(16), padding: RFValue(24), alignItems: 'center', marginBottom: RFValue(20) },
    balanceLabel: { fontSize: RFValue(14), color: '#6B7280', marginBottom: RFValue(8) },
    balanceValue: { fontSize: RFValue(28), fontWeight: 'bold', color: '#D97706' },

    charitySelectionBox: { backgroundColor: '#FFF', borderRadius: RFValue(20), padding: RFValue(20), marginBottom: RFValue(20) },
    charityItemCard: { backgroundColor: '#F9FAFB', borderWidth: 1, borderColor: '#E5E7EB', padding: RFValue(16), borderRadius: RFValue(12), marginBottom: RFValue(12), flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
    goalPill: { backgroundColor: '#E0E7FF', paddingHorizontal: RFValue(9), paddingVertical: RFValue(7), borderRadius: RFValue(20), marginLeft: RFValue(3) },
    goalPillText: { color: '#4F46E5', fontSize: RFValue(12), fontWeight: 'bold' },

    donationTargetBox: { backgroundColor: '#FFF', borderRadius: RFValue(20), padding: RFValue(20), marginBottom: RFValue(20), shadowColor: '#000', shadowOffset: { width: 0, height: 4 }, shadowOpacity: 0.05, shadowRadius: 10, elevation: 3 },
    sectionTitle: { fontSize: RFValue(16), fontWeight: 'bold', color: '#111', marginBottom: RFValue(8) },

    activeTargetCard: { flexDirection: 'row', alignItems: 'center', backgroundColor: '#F9FAFB', padding: RFValue(16), borderRadius: RFValue(16), marginBottom: RFValue(24) },
    targetEmoji: { fontSize: RFValue(36), marginRight: RFValue(16) },
    targetInfo: { flex: 1, paddingRight: RFValue(8) },
    targetTitle: { fontSize: RFValue(15), fontWeight: 'bold', color: '#111', marginBottom: RFValue(5) },
    targetDesc: { fontSize: RFValue(10.4), color: '#6B7280', lineHeight: RFValue(12.5), paddingRight: RFValue(6) },

    gaugeContainer: { marginBottom: RFValue(24) },
    gaugeTexts: { flexDirection: 'row', alignItems: 'baseline', marginBottom: RFValue(8) },
    gaugeCurrentText: { fontSize: RFValue(24), fontWeight: '900', color: '#10B981' },
    gaugeGoalText: { fontSize: RFValue(14), fontWeight: 'bold', color: '#9CA3AF', marginLeft: RFValue(4) },
    progressBarBg: { height: RFValue(20), backgroundColor: '#E5E7EB', borderRadius: RFValue(10), overflow: 'hidden' },
    progressBarFill: { height: '100%', backgroundColor: '#10B981', borderRadius: RFValue(10) },
    goalReachedText: { textAlign: 'center', marginTop: RFValue(12), fontSize: RFValue(14), fontWeight: 'bold', color: '#F59E0B' },

    donateInputWrapper: { flexDirection: 'row', alignItems: 'center', gap: RFValue(12) },
    donateInputContainer: { flex: 1, flexDirection: 'row', alignItems: 'center', backgroundColor: '#F9FAFB', borderWidth: 1, borderColor: '#E5E7EB', borderRadius: RFValue(12), paddingHorizontal: RFValue(16) },
    donateInput: { flex: 1, fontSize: RFValue(16), fontWeight: 'bold', color: '#111', paddingVertical: RFValue(14) },

    donateButton: { backgroundColor: '#10B981', paddingVertical: RFValue(14), paddingHorizontal: RFValue(24), borderRadius: RFValue(12), alignItems: 'center', justifyContent: 'center' },
    donateButtonText: { color: '#FFF', fontWeight: 'bold', fontSize: RFValue(16) },
    gachaButton: { backgroundColor: '#F59E0B', paddingVertical: RFValue(14), borderRadius: RFValue(12), alignItems: 'center' },
    gachaButtonText: { color: '#FFF', fontWeight: 'bold', fontSize: RFValue(16) },

    mapButton: { backgroundColor: '#111', padding: RFValue(16), borderRadius: RFValue(12), alignItems: 'center' },
    mapButtonText: { color: '#FFF', fontWeight: 'bold', fontSize: RFValue(16) }
});

export default SafeBoxScreen;
