import { useState, useEffect } from 'react';
import { View, StyleSheet, TouchableOpacity, SafeAreaView, ScrollView, Alert, ActivityIndicator } from 'react-native';
import { scale, verticalScale } from 'react-native-size-matters';
import CustomText from '../../../components/common/CustomText';
import api from '../../../api/axios';

const DUMMY_MISSIONS = [
    { id: 'dummy-1', title: '대중교통 이용하기', description: '자동 분석된 미션입니다.', reward: 50, status: '진행 중' },
    { id: 'dummy-2', title: '편의점 텀블러 할인 받기', description: '자동 분석된 미션입니다.', reward: 100, status: '진행 중' },
];

const ESGChallengeScreen = ({ navigation }) => {
    const [esgChallenge, setEsgChallenge] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        fetchEsgChallenge();
    }, []);

    const fetchEsgChallenge = async () => {
        try {
            const res = await api.get('/challenges/esg');
            const data = res.data?.data;
            if (data && data.challengeId) {
                setEsgChallenge(data);
            }
        } catch (err) {
            console.error('Fetch ESG Challenge Error', err);
        } finally {
            setLoading(false);
        }
    };

    const getEsgStatus = () => {
        if (!esgChallenge) return '진행 중';
        if (esgChallenge.isRewarded) return '완료';
        if (esgChallenge.isCompleted) return '보상 대기';
        return '진행 중';
    };

    const handleClaimReward = async () => {
        try {
            await api.post(`/challenges/esg/${esgChallenge.challengeId}/rewards`);
            Alert.alert('보상 수령 완료', '젤링을 성공적으로 받았습니다! 🎉');
            setEsgChallenge(prev => ({ ...prev, isCompleted: true, isRewarded: true }));
        } catch (err) {
            console.error('Reward Claim Error', err);
            Alert.alert('오류', '보상 수령에 실패했습니다.');
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

    const renderMissionCard = (mission, isDummy = false) => {
        const status = isDummy ? mission.status : getEsgStatus();
        const reward = isDummy ? mission.reward : (esgChallenge?.rewardAmount ?? 50);
        const title = isDummy ? mission.title : '녹색 가맹점에서 1회 이상 구매';
        const description = isDummy ? mission.description : '친환경 가맹점 결제 시 자동 인증됩니다.';

        return (
            <View key={mission.id} style={styles.missionCard}>
                <View style={styles.missionHeader}>
                    <View style={styles.missionHeaderLeft}>
                        {renderMissionBadge(status)}
                        <View style={{ marginLeft: scale(10) }}>
                            <CustomText style={styles.missionTitle}>{title}</CustomText>
                            <CustomText style={styles.missionDesc}>{description}</CustomText>
                        </View>
                    </View>
                </View>

                <View style={styles.rewardBox}>
                    <CustomText style={styles.rewardText}>보상: {reward} 젤링</CustomText>
                    {status === '보상 대기' ? (
                        <TouchableOpacity style={styles.claimButton} onPress={handleClaimReward}>
                            <CustomText style={styles.claimButtonText}>받기</CustomText>
                        </TouchableOpacity>
                    ) : (
                        <TouchableOpacity style={[styles.claimButton, styles.disabledButton]} disabled>
                            <CustomText style={[styles.claimButtonText, styles.disabledButtonText]}>
                                {status === '완료' ? '수령 완료' : '진행 중'}
                            </CustomText>
                        </TouchableOpacity>
                    )}
                </View>
            </View>
        );
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
                    <CustomText style={styles.pageTitle}>지구를 위한 착한 소비</CustomText>
                    <CustomText style={styles.pageSubtitle}>내가 한 친환경 결제가 자동으로 인증돼요!</CustomText>
                </View>

                {DUMMY_MISSIONS.map(mission => renderMissionCard(mission, true))}

                {loading ? (
                    <ActivityIndicator size="large" color="#A3E635" style={{ marginTop: 16 }} />
                ) : (
                    renderMissionCard({ id: 'esg-real' }, false)
                )}
            </ScrollView>
        </SafeAreaView>
    );
};

const styles = StyleSheet.create({
    safeArea: { flex: 1, backgroundColor: '#ECFCCB' },
    header: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingHorizontal: scale(16), paddingVertical: verticalScale(16), backgroundColor: '#FFFFFF' },
    backButton: { width: scale(32), height: scale(32), justifyContent: 'center' },
    backButtonText: { fontSize: scale(22), fontWeight: 'bold', color: '#111' },
    headerTitle: { fontSize: scale(18), fontWeight: 'bold', color: '#111' },
    container: { flexGrow: 1, paddingHorizontal: scale(20), paddingTop: verticalScale(20) },
    topSection: { marginBottom: verticalScale(24), backgroundColor: '#FFFFFF', padding: scale(20), borderRadius: scale(16), shadowColor: '#000', shadowOffset: { width: 0, height: 2 }, shadowOpacity: 0.05, shadowRadius: 4, elevation: 2 },
    pageTitle: { fontSize: scale(20), fontWeight: '900', color: '#A3E635', marginBottom: verticalScale(8) },
    pageSubtitle: { fontSize: scale(14), color: '#585b54ff', lineHeight: scale(20) },
    missionCard: { backgroundColor: '#FFFFFF', borderRadius: scale(16), padding: scale(20), marginBottom: verticalScale(16), shadowColor: '#000', shadowOffset: { width: 0, height: 2 }, shadowOpacity: 0.05, shadowRadius: 4, elevation: 2 },
    missionHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: verticalScale(16) },
    missionHeaderLeft: { flexDirection: 'row', alignItems: 'center', flex: 1 },
    badge: { paddingHorizontal: scale(10), paddingVertical: verticalScale(4), borderRadius: scale(12), borderWidth: 1 },
    badgeSuccess: { backgroundColor: '#F9FAFB', borderColor: '#D1D5DB' },
    badgeTextSuccess: { color: '#6B7280', fontSize: scale(11), fontWeight: 'bold' },
    badgeProgress: { backgroundColor: '#FEF9C3', borderColor: '#FACC15' },
    badgeTextProgress: { color: '#A16207', fontSize: scale(11), fontWeight: 'bold' },
    badgePending: { backgroundColor: '#D1FAE5', borderColor: '#34D399' },
    badgeTextPending: { color: '#059669', fontSize: scale(11), fontWeight: 'bold' },
    missionTitle: { fontSize: scale(15), fontWeight: 'bold', color: '#111', marginBottom: verticalScale(2) },
    missionDesc: { fontSize: scale(12), color: '#6B7280' },
    rewardBox: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', backgroundColor: '#F9FAFB', padding: scale(12), borderRadius: scale(12), shadowColor: '#000', shadowOffset: { width: 0, height: 2 }, shadowOpacity: 0.05, shadowRadius: 4, elevation: 1, },
    rewardText: { fontSize: scale(14), fontWeight: 'bold', color: '#111' },
    claimButton: { backgroundColor: '#10B981', paddingVertical: verticalScale(8), paddingHorizontal: scale(16), borderRadius: scale(12) },
    disabledButton: { backgroundColor: '#E5E7EB' },
    claimButtonText: { color: '#FFFFFF', fontWeight: 'bold', fontSize: scale(14) },
    disabledButtonText: { color: '#9CA3AF' }
});

export default ESGChallengeScreen;
