import React, { useState, useEffect } from 'react';
import { View, StyleSheet, TouchableOpacity, SafeAreaView, ScrollView, Alert, ActivityIndicator } from 'react-native';
import { scale, verticalScale } from 'react-native-size-matters';
import CustomText from '../../../components/common/CustomText';
import api from '../../../api/axios';

const ESGChallengeScreen = ({ navigation }) => {
    const [missions, setMissions] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        fetchESGMissions();
    }, []);

    const fetchESGMissions = async () => {
        try {
            const res = await api.get('/challenges/esg');
            const data = res.data?.data;
            if (data && Array.isArray(data)) {
                setMissions(data);
            } else if (data && data.challenges) {
                setMissions(data.challenges);
            } else {
                // 더미 데이터 폴백 (안전을 위해)
                setMissions([
                    { id: 1, title: '대중교통 이용하기', reward: 50, status: '주기적' },
                    { id: 2, title: '편의점 텀블러 할인 받기', reward: 100, status: '보상 대기' },
                    { id: 3, title: '친환경 인증 상품 구매', reward: 150, status: '완료' }
                ]);
            }
        } catch (err) {
            console.error('Fetch ESG Missions Error', err);
            // 더미 데이터 폴백
            setMissions([
                { id: 1, title: '다회용기(텀블러) 할인 받기', description: '결제 내역 자동 분석', reward: 50, status: '진행 중' },
                { id: 2, title: '대중교통 이용하기', description: '결제 내역 자동 분석', reward: 100, status: '보상 대기' },
                { id: 3, title: '친환경 인증 제품 구매', description: '결제 내역 자동 분석', reward: 150, status: '완료' }
            ]);
        } finally {
            setLoading(false);
        }
    };

    const handleClaimReward = async (challengeId) => {
        try {
            await api.post(`/challenges/esg/${challengeId}/rewards`);
            Alert.alert('보상 수령 완료', '젤링을 성공적으로 받았습니다! 🎉');

            // 로컬 상태 업데이트
            setMissions(prev => prev.map(m => m.id === challengeId ? { ...m, status: '완료' } : m));
        } catch (err) {
            console.error('Reward Claim Error', err);
            Alert.alert('오류', '보상 수령에 실패했습니다.');

            // 임시(프론트) 성공 처리 (디버깅용)
            // setMissions(prev => prev.map(m => m.id === challengeId ? { ...m, status: '완료' } : m));
        }
    };

    const renderMissionBadge = (status) => {
        switch (status) {
            case '완료':
                return <View style={[styles.badge, styles.badgeSuccess]}><CustomText style={styles.badgeTextSuccess}>완료</CustomText></View>;
            case '보상 대기':
                return <View style={[styles.badge, styles.badgePending]}><CustomText style={styles.badgeTextPending}>보상 대기</CustomText></View>;
            default:
                return <View style={[styles.badge, styles.badgeProgress]}><CustomText style={styles.badgeTextProgress}>진행 중</CustomText></View>;
        }
    };

    return (
        <SafeAreaView style={styles.safeArea}>
            <View style={styles.header}>
                <TouchableOpacity style={styles.backButton} onPress={() => navigation.goBack()}>
                    <CustomText style={styles.backButtonText}>←</CustomText>
                </TouchableOpacity>
                <CustomText style={styles.headerTitle}>ESG 챌린지</CustomText>
                <View style={{ width: scale(32) }} />
            </View>

            <ScrollView contentContainerStyle={styles.container}>
                <View style={styles.topSection}>
                    <CustomText style={styles.pageTitle}>지구를 위한 착한 소비 🌍</CustomText>
                    <CustomText style={styles.pageSubtitle}>내가 한 친환경 결제가 자동으로 인증돼요!</CustomText>
                </View>

                {loading ? (
                    <ActivityIndicator size="large" color="#10B981" style={{ marginTop: 40 }} />
                ) : missions.length === 0 ? (
                    <View style={styles.emptyState}>
                        <CustomText style={styles.emptyText}>이번 주 ESG 미션이 등록되지 않았어요.</CustomText>
                    </View>
                ) : (
                    missions.map((mission, idx) => (
                        <View key={idx} style={styles.missionCard}>
                            <View style={styles.missionHeader}>
                                <View style={styles.missionHeaderLeft}>
                                    {renderMissionBadge(mission.status)}
                                    <View style={{ marginLeft: scale(10) }}>
                                        <CustomText style={styles.missionTitle}>{mission.title || '친환경 미션'}</CustomText>
                                        <CustomText style={styles.missionDesc}>{mission.description || '자동 분석된 미션입니다.'}</CustomText>
                                    </View>
                                </View>
                            </View>

                            <View style={styles.rewardBox}>
                                <CustomText style={styles.rewardText}>보상: 🍬 {mission.reward || 50} 젤링</CustomText>
                                {mission.status === '보상 대기' ? (
                                    <TouchableOpacity style={styles.claimButton} onPress={() => handleClaimReward(mission.id)}>
                                        <CustomText style={styles.claimButtonText}>받기</CustomText>
                                    </TouchableOpacity>
                                ) : (
                                    <TouchableOpacity style={[styles.claimButton, styles.disabledButton]} disabled>
                                        <CustomText style={[styles.claimButtonText, styles.disabledButtonText]}>
                                            {mission.status === '완료' ? '수령 완료' : '진행 중'}
                                        </CustomText>
                                    </TouchableOpacity>
                                )}
                            </View>
                        </View>
                    ))
                )}
            </ScrollView>
        </SafeAreaView>
    );
};

const styles = StyleSheet.create({
    safeArea: { flex: 1, backgroundColor: '#F0FDF4' }, // Eco-friendly green tint
    header: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingHorizontal: scale(16), paddingVertical: verticalScale(16), backgroundColor: '#FFFFFF' },
    backButton: { width: scale(32), height: scale(32), justifyContent: 'center' },
    backButtonText: { fontSize: scale(22), fontWeight: 'bold', color: '#111' },
    headerTitle: { fontSize: scale(18), fontWeight: 'bold', color: '#111' },
    container: { flexGrow: 1, paddingHorizontal: scale(20), paddingTop: verticalScale(20) },
    topSection: { marginBottom: verticalScale(24), backgroundColor: '#FFFFFF', padding: scale(20), borderRadius: scale(16), shadowColor: '#000', shadowOffset: { width: 0, height: 2 }, shadowOpacity: 0.05, shadowRadius: 4, elevation: 2 },
    pageTitle: { fontSize: scale(20), fontWeight: '900', color: '#065F46', marginBottom: verticalScale(8) },
    pageSubtitle: { fontSize: scale(14), color: '#166534', lineHeight: scale(20) },
    emptyState: { alignItems: 'center', marginTop: verticalScale(40) },
    emptyText: { fontSize: scale(15), color: '#6B7280' },
    missionCard: { backgroundColor: '#FFFFFF', borderRadius: scale(16), padding: scale(20), marginBottom: verticalScale(16), shadowColor: '#000', shadowOffset: { width: 0, height: 2 }, shadowOpacity: 0.05, shadowRadius: 4, elevation: 2 },
    missionHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: verticalScale(16) },
    missionHeaderLeft: { flexDirection: 'row', alignItems: 'center', flex: 1 },
    badge: { paddingHorizontal: scale(10), paddingVertical: verticalScale(4), borderRadius: scale(12), borderWidth: 1 },
    badgeSuccess: { backgroundColor: '#F3F4F6', borderColor: '#D1D5DB' },
    badgeTextSuccess: { color: '#6B7280', fontSize: scale(11), fontWeight: 'bold' },
    badgeProgress: { backgroundColor: '#FEF9C3', borderColor: '#FACC15' },
    badgeTextProgress: { color: '#A16207', fontSize: scale(11), fontWeight: 'bold' },
    badgePending: { backgroundColor: '#D1FAE5', borderColor: '#34D399' },
    badgeTextPending: { color: '#059669', fontSize: scale(11), fontWeight: 'bold' },
    missionTitle: { fontSize: scale(15), fontWeight: 'bold', color: '#111', marginBottom: verticalScale(2) },
    missionDesc: { fontSize: scale(12), color: '#6B7280' },
    rewardBox: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', backgroundColor: '#F9FAFB', padding: scale(12), borderRadius: scale(12) },
    rewardText: { fontSize: scale(14), fontWeight: 'bold', color: '#111' },
    claimButton: { backgroundColor: '#10B981', paddingVertical: verticalScale(8), paddingHorizontal: scale(16), borderRadius: scale(12) },
    disabledButton: { backgroundColor: '#E5E7EB' },
    claimButtonText: { color: '#FFFFFF', fontWeight: 'bold', fontSize: scale(14) },
    disabledButtonText: { color: '#9CA3AF' }
});

export default ESGChallengeScreen;
