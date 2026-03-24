import React, { useCallback, useState } from 'react';
import { View, StyleSheet, SafeAreaView, ScrollView, TouchableOpacity, Alert } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { scale, verticalScale } from 'react-native-size-matters';
import CustomText from '../../../components/common/CustomText';
import api from '../../../api/axios';

const STATUS_UI = {
    PENDING: { label: '승인 대기', style: 'pending' },
    APPROVED: { label: '승인 완료', style: 'progress' },
    IN_PROGRESS: { label: '진행 중', style: 'progress' },
    SUCCESS: { label: '성공', style: 'success' },
    FAIL: { label: '실패', style: 'failed' },
    REJECTED: { label: '반려됨', style: 'failed' },
    REWARD_REQUESTED: { label: '보상 요청 중', style: 'pending' },
    REWARDED: { label: '보상 완료', style: 'success' },
};

const TABS = {
    THIS_WEEK: 'THIS_WEEK',
    NEXT_WEEK: 'NEXT_WEEK',
    PAST: 'PAST',
};

const ChallengeScreen = ({ navigation }) => {
    const [activeTab, setActiveTab] = useState(TABS.THIS_WEEK);
    const [isFabOpen, setIsFabOpen] = useState(false);
    const [thisWeekChallenges, setThisWeekChallenges] = useState([]);
    const [nextWeekChallenges, setNextWeekChallenges] = useState([]);
    const [pastChallenges, setPastChallenges] = useState([]);
    const [isLoading, setIsLoading] = useState(true);

    const fetchChallenges = useCallback(async () => {
        setIsLoading(true);

        try {
            const [weeklyRes, nextWeekRes, pastRes] = await Promise.all([
                api.get('/challenges'),
                api.get('/challenges/spending'),
                api.get('/challenges/spending/unclaimed'),
            ]);

            setThisWeekChallenges(weeklyRes.data?.data?.spending || []);
            setNextWeekChallenges(nextWeekRes.data?.data?.challenges || []);
            setPastChallenges(pastRes.data?.data?.challenges || []);
        } catch (error) {
            console.error('Challenge Fetch Error', error);
            setThisWeekChallenges([]);
            setNextWeekChallenges([]);
            setPastChallenges([]);
            Alert.alert('오류', '챌린지 목록을 불러오지 못했습니다.');
        } finally {
            setIsLoading(false);
        }
    }, []);

    useFocusEffect(
        useCallback(() => {
            fetchChallenges();
        }, [fetchChallenges])
    );

    const handleRewardRequest = async (challengeId) => {
        try {
            await api.post(`/challenges/spending/${challengeId}/reward`);
            Alert.alert('요청 완료', '부모님께 보상 송금 요청을 보냈습니다.');
            fetchChallenges();
        } catch (error) {
            console.error('Reward request error', error);
            Alert.alert('오류', '보상 요청에 실패했습니다.');
        }
    };

    const handleDelete = (challengeId) => {
        Alert.alert(
            '챌린지 삭제',
            '정말로 이 챌린지를 삭제하시겠습니까?',
            [
                { text: '취소', style: 'cancel' },
                {
                    text: '삭제',
                    style: 'destructive',
                    onPress: async () => {
                        try {
                            await api.delete(`/challenges/spending/${challengeId}`);
                            Alert.alert('삭제 완료', '챌린지가 삭제되었습니다.');
                            fetchChallenges();
                        } catch (error) {
                            console.error('Challenge delete error', error);
                            Alert.alert('오류', '챌린지 삭제에 실패했습니다.');
                        }
                    }
                }
            ]
        );
    };

    const handleOpenQuizDifficulty = () => {
        const parentNavigation = navigation.getParent?.();

        if (parentNavigation) {
            parentNavigation.navigate('QuizDifficultySelect');
            return;
        }

        navigation.navigate('QuizDifficultySelect');
    };

    const getCurrentChallenges = () => {
        if (activeTab === TABS.THIS_WEEK) return thisWeekChallenges;
        if (activeTab === TABS.NEXT_WEEK) return nextWeekChallenges;
        return pastChallenges;
    };

    const renderStatusBadge = (status) => {
        const meta = STATUS_UI[status];
        if (!meta) return null;

        return (
            <View
                style={[
                    styles.badge,
                    meta.style === 'progress' && styles.badgeProgress,
                    meta.style === 'pending' && styles.badgePending,
                    meta.style === 'success' && styles.badgeSuccess,
                    meta.style === 'failed' && styles.badgeFailed,
                ]}
            >
                <CustomText
                    style={[
                        meta.style === 'progress' && styles.badgeTextProgress,
                        meta.style === 'pending' && styles.badgeTextPending,
                        meta.style === 'success' && styles.badgeTextSuccess,
                        meta.style === 'failed' && styles.badgeTextFailed,
                    ]}
                >
                    {meta.label}
                </CustomText>
            </View>
        );
    };

    const renderChallengeItem = (item) => {
        const canDelete = activeTab === TABS.NEXT_WEEK && (item.status === 'PENDING' || item.status === 'REJECTED');
        const canRequestReward = activeTab === TABS.PAST && item.status === 'SUCCESS';

        return (
            <View key={item.challengeId} style={styles.card}>
                <View style={styles.cardHeader}>
                    <View style={styles.cardHeaderLeft}>
                        {renderStatusBadge(item.status)}
                        <CustomText style={styles.categoryText}>{item.category} 아끼기</CustomText>
                    </View>
                </View>

                <CustomText style={styles.goalText}>목표 금액: {Number(item.targetSpending || 0).toLocaleString()}원</CustomText>
                <CustomText style={styles.goalSubText}>보상 금액: {Number(item.rewardAmount || 0).toLocaleString()}원</CustomText>

                {canRequestReward && (
                    <TouchableOpacity
                        style={styles.rewardBtn}
                        onPress={() => handleRewardRequest(item.challengeId)}
                    >
                        <CustomText style={styles.rewardBtnText}>보상 요청</CustomText>
                    </TouchableOpacity>
                )}

                {canDelete && (
                    <View style={styles.actionRow}>
                        <TouchableOpacity style={[styles.actionBtn, styles.deleteBtn]} onPress={() => handleDelete(item.challengeId)}>
                            <CustomText style={styles.deleteBtnText}>삭제</CustomText>
                        </TouchableOpacity>
                    </View>
                )}
            </View>
        );
    };

    const currentChallenges = getCurrentChallenges();

    return (
        <SafeAreaView style={styles.safeArea}>
            <View style={styles.header}>
                <CustomText style={styles.headerTitle}>내 챌린지</CustomText>
            </View>

            <View style={styles.tabContainer}>
                <TouchableOpacity
                    style={[styles.tabBtn, activeTab === TABS.THIS_WEEK && styles.tabBtnActive]}
                    onPress={() => { setActiveTab(TABS.THIS_WEEK); setIsFabOpen(false); }}
                >
                    <CustomText style={[styles.tabText, activeTab === TABS.THIS_WEEK && styles.tabTextActive]}>이번 주</CustomText>
                </TouchableOpacity>
                <TouchableOpacity
                    style={[styles.tabBtn, activeTab === TABS.NEXT_WEEK && styles.tabBtnActive]}
                    onPress={() => { setActiveTab(TABS.NEXT_WEEK); setIsFabOpen(false); }}
                >
                    <CustomText style={[styles.tabText, activeTab === TABS.NEXT_WEEK && styles.tabTextActive]}>다음 주</CustomText>
                </TouchableOpacity>
                <TouchableOpacity
                    style={[styles.tabBtn, activeTab === TABS.PAST && styles.tabBtnActive]}
                    onPress={() => { setActiveTab(TABS.PAST); setIsFabOpen(false); }}
                >
                    <CustomText style={[styles.tabText, activeTab === TABS.PAST && styles.tabTextActive]}>지난 주</CustomText>
                </TouchableOpacity>
            </View>

            <ScrollView contentContainerStyle={styles.container} showsVerticalScrollIndicator={false}>
                {isLoading ? (
                    <View style={styles.emptyView}>
                        <CustomText style={styles.emptyText}>챌린지를 불러오는 중입니다.</CustomText>
                    </View>
                ) : currentChallenges.length === 0 ? (
                    <View style={styles.emptyView}>
                        <CustomText style={styles.emptyText}>
                            {activeTab === TABS.PAST ? '지난 주 챌린지가 없어요!' : '표시할 챌린지가 없어요!'}
                        </CustomText>
                        {activeTab === TABS.NEXT_WEEK && (
                            <TouchableOpacity style={styles.emptyAddBtn} onPress={() => navigation.navigate('ChallengePropose')}>
                                <CustomText style={styles.emptyAddBtnText}>새 챌린지 제안하기</CustomText>
                            </TouchableOpacity>
                        )}
                    </View>
                ) : (
                    currentChallenges.map(renderChallengeItem)
                )}

                <View style={[styles.card, { marginTop: verticalScale(24), backgroundColor: '#F9FAFB' }]}>
                    <CustomText style={styles.menuTitle}>매일매일 미션</CustomText>

                    <TouchableOpacity style={styles.menuBtn} onPress={handleOpenQuizDifficulty}>
                        <CustomText style={styles.menuBtnIcon}>🎓</CustomText>
                        <View style={{ flex: 1 }}>
                            <CustomText style={styles.menuBtnTitle}>주간 금융 퀴즈</CustomText>
                            <CustomText style={styles.menuBtnSub}>퀴즈를 풀고 랜덤 젤링 받기</CustomText>
                        </View>
                    </TouchableOpacity>

                    <TouchableOpacity style={styles.menuBtn} onPress={() => navigation.navigate('AttendanceScreen')}>
                        <CustomText style={styles.menuBtnIcon}>📅</CustomText>
                        <View style={{ flex: 1 }}>
                            <CustomText style={styles.menuBtnTitle}>출석체크</CustomText>
                            <CustomText style={styles.menuBtnSub}>매일 출석하고 보상 받기</CustomText>
                        </View>
                    </TouchableOpacity>

                    <TouchableOpacity style={styles.menuBtn} onPress={() => navigation.navigate('ESGChallengeScreen')}>
                        <CustomText style={styles.menuBtnIcon}>🌍</CustomText>
                        <View style={{ flex: 1 }}>
                            <CustomText style={styles.menuBtnTitle}>주간 ESG 챌린지</CustomText>
                            <CustomText style={styles.menuBtnSub}>착한 소비 인증하고 젤링 받기</CustomText>
                        </View>
                    </TouchableOpacity>
                </View>
            </ScrollView>

            {activeTab === TABS.NEXT_WEEK && (
                <>
                    {isFabOpen && (
                        <View style={styles.fabMenuContainer}>
                            <TouchableOpacity
                                style={styles.fabMenuItem}
                                onPress={() => { setIsFabOpen(false); navigation.navigate('ChallengePropose'); }}
                            >
                                <CustomText style={styles.fabMenuItemText}>새 챌린지 제안하기</CustomText>
                            </TouchableOpacity>
                        </View>
                    )}
                    <TouchableOpacity style={[styles.fab, isFabOpen && styles.fabOpen]} onPress={() => setIsFabOpen(!isFabOpen)}>
                        <CustomText style={[styles.fabIcon, isFabOpen && styles.fabIconOpen]}>{isFabOpen ? '×' : '+'}</CustomText>
                    </TouchableOpacity>
                </>
            )}
        </SafeAreaView>
    );
};

const styles = StyleSheet.create({
    safeArea: { flex: 1, backgroundColor: '#F3F4F6' },
    header: {
        flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between',
        paddingHorizontal: scale(16), paddingVertical: verticalScale(16), backgroundColor: '#FFFFFF'
    },
    headerTitle: { fontSize: scale(22), fontWeight: 'bold', color: '#111' },

    tabContainer: { flexDirection: 'row', backgroundColor: '#FFFFFF', borderBottomWidth: 1, borderBottomColor: '#E5E7EB' },
    tabBtn: { flex: 1, alignItems: 'center', paddingVertical: verticalScale(14), borderBottomWidth: 2, borderBottomColor: 'transparent' },
    tabBtnActive: { borderBottomColor: '#A3E635' },
    tabText: { fontSize: scale(15), color: '#9CA3AF', fontWeight: 'bold' },
    tabTextActive: { color: '#111' },

    container: { flexGrow: 1, padding: scale(16) },

    card: { backgroundColor: '#FFFFFF', borderRadius: scale(16), padding: scale(20), marginBottom: verticalScale(16), shadowColor: '#000', shadowOffset: { width: 0, height: 2 }, shadowOpacity: 0.05, shadowRadius: 4, elevation: 2 },
    cardHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: verticalScale(12) },
    cardHeaderLeft: { flexDirection: 'row', alignItems: 'center' },
    badge: { paddingHorizontal: scale(10), paddingVertical: verticalScale(4), borderRadius: scale(12) },

    badgeProgress: { backgroundColor: '#FEF9C3', borderColor: '#FACC15', borderWidth: 1 },
    badgeTextProgress: { color: '#A16207', fontSize: scale(11), fontWeight: 'bold' },
    badgePending: { backgroundColor: '#F3F4F6', borderColor: '#D1D5DB', borderWidth: 1 },
    badgeTextPending: { color: '#6B7280', fontSize: scale(11), fontWeight: 'bold' },
    badgeSuccess: { backgroundColor: '#D1FAE5', borderColor: '#34D399', borderWidth: 1 },
    badgeTextSuccess: { color: '#059669', fontSize: scale(11), fontWeight: 'bold' },
    badgeFailed: { backgroundColor: '#FEE2E2', borderColor: '#F87171', borderWidth: 1 },
    badgeTextFailed: { color: '#DC2626', fontSize: scale(11), fontWeight: 'bold' },

    categoryText: { fontSize: scale(14), fontWeight: '600', color: '#4B5563', marginLeft: scale(8) },
    goalText: { fontSize: scale(15), color: '#374151', marginBottom: verticalScale(8) },
    goalSubText: { fontSize: scale(14), color: '#6B7280' },

    rewardBtn: { backgroundColor: '#3B82F6', paddingVertical: verticalScale(12), borderRadius: scale(12), alignItems: 'center', marginTop: verticalScale(16) },
    rewardBtnText: { color: '#FFF', fontSize: scale(14), fontWeight: 'bold' },

    actionRow: { flexDirection: 'row', justifyContent: 'flex-end', marginTop: verticalScale(12) },
    actionBtn: { paddingHorizontal: scale(12), paddingVertical: verticalScale(6), borderRadius: scale(8), marginLeft: scale(8) },
    deleteBtn: { backgroundColor: '#FEE2E2' },
    deleteBtnText: { color: '#DC2626', fontWeight: 'bold' },

    emptyView: { flex: 1, justifyContent: 'center', alignItems: 'center', minHeight: verticalScale(150), paddingVertical: verticalScale(40) },
    emptyText: { fontSize: scale(16), color: '#6B7280', marginBottom: verticalScale(16) },
    emptyAddBtn: {
        backgroundColor: '#A3E635',
        paddingVertical: verticalScale(12),
        paddingHorizontal: scale(24),
        borderRadius: scale(25),
    },
    emptyAddBtnText: { fontSize: scale(16), fontWeight: 'bold', color: '#111' },

    menuTitle: { fontSize: scale(16), fontWeight: 'bold', color: '#4B5563', marginBottom: verticalScale(12) },
    menuBtn: { flexDirection: 'row', alignItems: 'center', backgroundColor: '#FFFFFF', padding: scale(16), borderRadius: scale(12), marginBottom: verticalScale(12), borderWidth: 1, borderColor: '#E5E7EB' },
    menuBtnIcon: { fontSize: scale(24), marginRight: scale(16) },
    menuBtnTitle: { fontSize: scale(15), fontWeight: 'bold', color: '#111', marginBottom: verticalScale(2) },
    menuBtnSub: { fontSize: scale(13), color: '#6B7280' },

    fab: {
        position: 'absolute',
        bottom: verticalScale(20),
        right: scale(20),
        backgroundColor: '#A3E635',
        width: scale(56),
        height: scale(56),
        borderRadius: scale(28),
        justifyContent: 'center',
        alignItems: 'center',
        shadowColor: '#000',
        shadowOffset: { width: 0, height: verticalScale(4) },
        shadowOpacity: 0.3,
        shadowRadius: scale(4),
        elevation: 5,
    },
    fabOpen: { backgroundColor: '#6B7280' },
    fabIcon: { fontSize: scale(32), lineHeight: scale(36), fontWeight: 'bold', color: '#fff' },
    fabIconOpen: { fontSize: scale(32), transform: [{ rotate: '45deg' }] },
    fabMenuContainer: {
        position: 'absolute',
        bottom: verticalScale(84),
        right: scale(20),
        alignItems: 'flex-end',
    },
    fabMenuItem: {
        backgroundColor: '#FFFFFF',
        paddingVertical: verticalScale(12),
        paddingHorizontal: scale(20),
        borderRadius: scale(24),
        shadowColor: '#000',
        shadowOffset: { width: 0, height: verticalScale(2) },
        shadowOpacity: 0.1,
        shadowRadius: scale(4),
        elevation: 3,
        marginBottom: verticalScale(8),
    },
    fabMenuItemText: { fontSize: scale(15), fontWeight: 'bold', color: '#111' }
});

export default ChallengeScreen;
