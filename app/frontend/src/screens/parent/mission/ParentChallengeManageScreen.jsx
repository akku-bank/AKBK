import React, { useState } from 'react';
import { View, StyleSheet, TouchableOpacity, ScrollView, SafeAreaView } from 'react-native';
import { scale, verticalScale } from 'react-native-size-matters';
import CustomText from '../../../components/common/CustomText';

// 더미 챌린지 내역
const CHALLENGES = [
    { id: '1', title: '간식비 3000원 아끼기', child: '김싸피', status: '진행중', goal: 3000, current: 1500 },
    { id: '2', title: '게임 결제 안하기', child: '김싸피', status: '대기중', goal: 5000, current: 0 },
    { id: '3', title: '쇼핑 5000원 줄이기', child: '김싸피', status: '실패', goal: 5000, current: 8000 },
];

const ParentChallengeManageScreen = ({ navigation }) => {
    return (
        <SafeAreaView style={styles.safeArea}>
            <View style={styles.header}>
                <TouchableOpacity style={styles.backButton} onPress={() => navigation.goBack()}>
                    <CustomText style={styles.backButtonText}>←</CustomText>
                </TouchableOpacity>
                <CustomText style={styles.headerTitle}>용돈 미션 관리</CustomText>
                <View style={{ width: scale(32) }} />
            </View>

            <ScrollView contentContainerStyle={styles.container}>
                <CustomText style={styles.sectionTitle}>자녀가 제안한 미션</CustomText>

                {CHALLENGES.map(item => (
                    <TouchableOpacity
                        key={item.id}
                        style={styles.card}
                        onPress={() => item.status === '대기중' ? navigation.navigate('MissionApprovalScreen') : null}
                        activeOpacity={item.status === '대기중' ? 0.7 : 1}
                    >
                        <View style={styles.cardHeader}>
                            <CustomText style={styles.cardTitle}>{item.title}</CustomText>
                            <View style={[styles.statusBadge, item.status === '대기중' && styles.statusBadgePending, item.status === '실패' && styles.statusBadgeFail]}>
                                <CustomText style={[styles.statusText, item.status === '대기중' && styles.statusTextPending, item.status === '실패' && styles.statusTextFail]}>{item.status}</CustomText>
                            </View>
                        </View>
                        <CustomText style={styles.childName}>도전자: {item.child}</CustomText>

                        {item.status !== '대기중' && (
                            <View style={styles.progressSection}>
                                <View style={styles.progressRow}>
                                    <CustomText style={styles.progressLabel}>현재 지출</CustomText>
                                    <CustomText style={[styles.progressValue, item.current > item.goal && { color: '#EF4444' }]}>
                                        {item.current.toLocaleString()} / {item.goal.toLocaleString()}원
                                    </CustomText>
                                </View>
                                <View style={styles.progressBarBg}>
                                    <View style={[styles.progressBarFill, { width: `${Math.min((item.current / item.goal) * 100, 100)}%`, backgroundColor: item.current > item.goal ? '#EF4444' : '#A3E635' }]} />
                                </View>
                            </View>
                        )}

                        {item.status === '대기중' && (
                            <CustomText style={styles.actionPrompt}>눌러서 제안 검토하기 &gt;</CustomText>
                        )}
                    </TouchableOpacity>
                ))}
            </ScrollView>
        </SafeAreaView>
    );
};

const styles = StyleSheet.create({
    safeArea: { flex: 1, backgroundColor: '#F3F4F6' },
    header: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingHorizontal: scale(16), paddingVertical: verticalScale(16), backgroundColor: '#F3F4F6' },
    backButton: { width: scale(32), height: scale(32), justifyContent: 'center' },
    backButtonText: { fontSize: scale(22), fontWeight: 'bold', color: '#111' },
    headerTitle: { fontSize: scale(18), fontWeight: 'bold', color: '#111' },
    container: { flexGrow: 1, paddingHorizontal: scale(16), paddingTop: verticalScale(20) },

    sectionTitle: { fontSize: scale(16), fontWeight: 'bold', color: '#4B5563', marginBottom: verticalScale(16) },

    card: { backgroundColor: '#FFFFFF', borderRadius: scale(16), padding: scale(20), marginBottom: verticalScale(16), shadowColor: '#000', shadowOffset: { width: 0, height: verticalScale(2) }, shadowOpacity: 0.05, shadowRadius: scale(8), elevation: 2 },
    cardHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: verticalScale(8) },
    cardTitle: { fontSize: scale(16), fontWeight: 'bold', color: '#111', flex: 1 },

    statusBadge: { backgroundColor: '#A3E635', paddingHorizontal: scale(8), paddingVertical: verticalScale(4), borderRadius: scale(8) },
    statusText: { fontSize: scale(12), fontWeight: 'bold', color: '#111' },

    statusBadgePending: { backgroundColor: '#FEF3C7' },
    statusTextPending: { color: '#D97706' },

    statusBadgeFail: { backgroundColor: '#FEE2E2' },
    statusTextFail: { color: '#B91C1C' },

    childName: { fontSize: scale(13), color: '#6B7280', marginBottom: verticalScale(16) },

    progressSection: { marginTop: verticalScale(8) },
    progressRow: { flexDirection: 'row', justifyContent: 'space-between', marginBottom: verticalScale(8) },
    progressLabel: { fontSize: scale(12), color: '#6B7280' },
    progressValue: { fontSize: scale(12), fontWeight: 'bold', color: '#111' },
    progressBarBg: { height: verticalScale(8), backgroundColor: '#F3F4F6', borderRadius: scale(4), overflow: 'hidden' },
    progressBarFill: { height: '100%', borderRadius: scale(4) },

    actionPrompt: { fontSize: scale(13), color: '#2563EB', fontWeight: 'bold', marginTop: verticalScale(8), textAlign: 'right' }
});

export default ParentChallengeManageScreen;
