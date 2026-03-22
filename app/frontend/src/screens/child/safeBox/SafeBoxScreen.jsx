import React, { useState } from 'react';
import { View, StyleSheet, SafeAreaView, ScrollView, TouchableOpacity, Alert, Animated, KeyboardAvoidingView, Platform } from 'react-native';
import { RFValue } from 'react-native-responsive-fontsize';
import CustomText from '../../../components/common/CustomText';
import CustomTextInput from '../../../components/common/CustomTextInput';

const TARGET_GOAL = 500;
const INITIAL_CURRENT = 300; // 더미 초기값
const MY_JELLINGS = 1500;

const SafeBoxScreen = ({ navigation }) => {
    const [currentJelling, setCurrentJelling] = useState(INITIAL_CURRENT);
    const [myJellings, setMyJellings] = useState(MY_JELLINGS);
    const [selectedDonation, setSelectedDonation] = useState('earth');
    const [donateAmount, setDonateAmount] = useState('');

    const progressValue = Math.min(currentJelling / TARGET_GOAL, 1);
    const isGoalReached = currentJelling >= TARGET_GOAL;

    const handleDonate = () => {
        const amount = parseInt(donateAmount);
        if (!amount || isNaN(amount) || amount <= 0) {
            Alert.alert('알림', '기부할 금액(젤링)을 똑바로 입력해주세요!');
            return;
        }
        if (myJellings < amount) {
            Alert.alert('알림', '가진 젤링이 부족해요!');
            return;
        }
        if (isGoalReached) {
            Alert.alert('알림', '이미 저금통이 꽉 찼어요! 보상을 뽑아주세요.');
            return;
        }

        const actualDonate = Math.min(amount, TARGET_GOAL - currentJelling);
        setMyJellings(prev => prev - actualDonate);
        setCurrentJelling(prev => Math.min(prev + actualDonate, TARGET_GOAL));
        setDonateAmount('');
    };

    const handleGacha = () => {
        if (!isGoalReached) return;

        setCurrentJelling(0); // 기부 초기화
        navigation.navigate('GachaScreen');
    };

    return (
        <SafeAreaView style={styles.safeArea}>
            <View style={styles.header}>
                <CustomText style={styles.headerTitle}>젤링 주머니</CustomText>
            </View>

            <KeyboardAvoidingView style={{ flex: 1 }} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
                <ScrollView contentContainerStyle={styles.container}>
                    <View style={styles.balanceHeader}>
                        <CustomText style={styles.balanceLabel}>내 젤링 주머니</CustomText>
                        <CustomText style={styles.balanceValue}>{myJellings.toLocaleString()} 💎</CustomText>
                    </View>

                    <View style={styles.donationTargetBox}>
                        <CustomText style={styles.sectionTitle}>현재 기부 목표</CustomText>

                        <View style={styles.activeTargetCard}>
                            <CustomText style={styles.targetEmoji}>🌍</CustomText>
                            <View style={styles.targetInfo}>
                                <CustomText style={styles.targetTitle}>환경 보호 연대</CustomText>
                                <CustomText style={styles.targetDesc}>지구 살리기 캠페인</CustomText>
                            </View>
                        </View>

                        {/* 게이지 바 영역 */}
                        <View style={styles.gaugeContainer}>
                            <View style={styles.gaugeTexts}>
                                <CustomText style={styles.gaugeCurrentText}>{currentJelling} 💎</CustomText>
                                <CustomText style={styles.gaugeGoalText}>/ {TARGET_GOAL} 💎</CustomText>
                            </View>
                            <View style={styles.progressBarBg}>
                                <Animated.View style={[styles.progressBarFill, { width: `${progressValue * 100}%` }]} />
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
                                <CustomText style={styles.gachaButtonText}>기부 완료! 행운의 뽑기 돌리기 🎁</CustomText>
                            </TouchableOpacity>
                        )}
                    </View>

                    <TouchableOpacity style={styles.mapButton} onPress={() => navigation.navigate('BadgeMap')}>
                        <CustomText style={styles.mapButtonText}>내 기부 뱃지 맵 보기 🗺️</CustomText>
                    </TouchableOpacity>
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

    donationTargetBox: { backgroundColor: '#FFF', borderRadius: RFValue(20), padding: RFValue(20), marginBottom: RFValue(20), shadowColor: '#000', shadowOffset: { width: 0, height: 4 }, shadowOpacity: 0.05, shadowRadius: 10, elevation: 3 },
    sectionTitle: { fontSize: RFValue(16), fontWeight: 'bold', color: '#111', marginBottom: RFValue(16) },

    activeTargetCard: { flexDirection: 'row', alignItems: 'center', backgroundColor: '#F9FAFB', padding: RFValue(16), borderRadius: RFValue(16), marginBottom: RFValue(24) },
    targetEmoji: { fontSize: RFValue(36), marginRight: RFValue(16) },
    targetInfo: { flex: 1 },
    targetTitle: { fontSize: RFValue(18), fontWeight: 'bold', color: '#111', marginBottom: RFValue(4) },
    targetDesc: { fontSize: RFValue(14), color: '#6B7280' },

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
