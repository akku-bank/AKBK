import React, { useCallback, useState } from 'react';
import { View, StyleSheet, SafeAreaView, ScrollView, TouchableOpacity, Alert, Image, Modal } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { scale, verticalScale } from 'react-native-size-matters';
import CustomText from '../../../components/common/CustomText';
import api from '../../../api/axios';

const CROCO_PARENTS_IMAGE = require('../../../assets/croco/croco_parents.png');
const AKKU_WELCOME_IMAGE = require('../../../assets/croco/akku-welcome.png');

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
    REQUESTED: 'REQUESTED',
    IN_PROGRESS: 'IN_PROGRESS',
    COMPLETED: 'COMPLETED',
};

const QUIZ_DIFFICULTIES = ['EASY', 'NORMAL', 'HARD'];

const getDdayLabel = (item, activeTab) => {
    if (!item?.startDate || !item?.endDate) return null;
    if (activeTab !== TABS.IN_PROGRESS) return null;

    const today = new Date();
    const currentDate = new Date(today.getFullYear(), today.getMonth(), today.getDate());
    const endDate = new Date(`${item.endDate}T00:00:00`);
    const msPerDay = 24 * 60 * 60 * 1000;

    const diffDays = Math.ceil((endDate - currentDate) / msPerDay);
    return diffDays <= 0 ? 'D-DAY' : `D-${diffDays}`;
};

const ChallengeScreen = ({ navigation }) => {
    const [activeTab, setActiveTab] = useState(TABS.REQUESTED);
    const [isFabOpen, setIsFabOpen] = useState(false);
    const [isScheduledOpen, setIsScheduledOpen] = useState(true);
    const [thisWeekChallenges, setThisWeekChallenges] = useState([]);
    const [nextWeekChallenges, setNextWeekChallenges] = useState([]);
    const [pastChallenges, setPastChallenges] = useState([]);
    const [isLoading, setIsLoading] = useState(true);
    const [isRewardSuccessVisible, setIsRewardSuccessVisible] = useState(false);

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

    const handleRewardRequestWithModal = async (challengeId) => {
        try {
            await api.post(`/challenges/spending/${challengeId}/reward`);
            setIsRewardSuccessVisible(true);
            fetchChallenges();
        } catch (error) {
            console.error('Reward request error', error);
            Alert.alert('?ㅻ쪟', '蹂댁긽 ?붿껌???ㅽ뙣?덉뒿?덈떎.');
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

    const handleEdit = (challenge) => {
        navigation.navigate('ChallengePropose', { challenge });
    };

    const handleOpenQuizDifficulty = async () => {
        try {
            const quizChecks = await Promise.all(
                QUIZ_DIFFICULTIES.map((difficulty) =>
                    api.get(`/challenges/quizzes?difficulty=${difficulty}`, {
                        validateStatus: () => true,
                    }).then((response) => ({ difficulty, response }))
                )
            );

            const submittedQuiz = quizChecks.find(
                ({ response }) => response.status === 200 && response.data?.data?.isSubmitted === true
            ) || null;

            const activeQuiz = quizChecks.find(({ response }) => response.status === 200)
                || null;

            const hasDifficultyLock = quizChecks.some(
                ({ response }) => response.status === 409 && response.data?.errorCode === 'QZ_002'
            );

            if (submittedQuiz) {
                navigation.navigate('QuizScreen', { difficulty: submittedQuiz.difficulty });
                return;
            }

            if (hasDifficultyLock && activeQuiz) {
                navigation.navigate('QuizScreen', { difficulty: activeQuiz.difficulty });
                return;
            }

            const parentNavigation = navigation.getParent?.();
            if (parentNavigation) {
                parentNavigation.navigate('QuizDifficultySelect');
                return;
            }

            navigation.navigate('QuizDifficultySelect');
        } catch (error) {
            console.error('Quiz difficulty precheck error', error);
            Alert.alert('오류', '퀴즈 정보를 확인하지 못했습니다.');
        }
    };

    const getCurrentChallenges = () => {
        if (activeTab === TABS.REQUESTED) return nextWeekChallenges;
        if (activeTab === TABS.IN_PROGRESS) return thisWeekChallenges;
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
        const canDelete = activeTab === TABS.REQUESTED && (item.status === 'PENDING' || item.status === 'REJECTED');
        const canEdit = activeTab === TABS.REQUESTED && (item.status === 'PENDING' || item.status === 'REJECTED');
        const canRequestReward = activeTab === TABS.COMPLETED && item.status === 'SUCCESS';
        const ddayLabel = getDdayLabel(item, activeTab);
        const canOpenDetail = activeTab === TABS.IN_PROGRESS;

        return (
            <TouchableOpacity
                key={item.challengeId}
                style={styles.card}
                onPress={() => canOpenDetail ? navigation.navigate('ChallengeDetail', { challengeId: item.challengeId }) : null}
                activeOpacity={canOpenDetail ? 0.85 : 1}
            >
                <View style={styles.cardHeader}>
                    <View style={styles.cardHeaderLeft}>
                        {renderStatusBadge(item.status)}
                        <CustomText style={styles.categoryText}>{item.category}</CustomText>
                    </View>
                    {ddayLabel ? <CustomText style={styles.ddayText}>{ddayLabel}</CustomText> : null}
                </View>

                <CustomText style={styles.goalText}>목표 금액: {Number(item.targetSpending || 0).toLocaleString()}원</CustomText>
                <CustomText style={styles.goalSubText}>보상 금액: {Number(item.rewardAmount || 0).toLocaleString()}원</CustomText>

                {item.parentMessage ? (
                    <View style={styles.parentMessageRow}>
                        <Image source={CROCO_PARENTS_IMAGE} style={styles.parentMessageImage} resizeMode="contain" />
                        <CustomText style={styles.parentMessageText}>{item.parentMessage}</CustomText>
                    </View>
                ) : null}

                {canRequestReward && (
                    <TouchableOpacity
                        style={styles.rewardBtn}
                        onPress={() => handleRewardRequestWithModal(item.challengeId)}
                    >
                        <CustomText style={styles.rewardBtnText}>보상 요청</CustomText>
                    </TouchableOpacity>
                )}

                {canDelete && (
                    <View style={styles.actionRow}>
                        {canEdit && (
                            <TouchableOpacity style={[styles.actionBtn, styles.editBtn]} onPress={() => handleEdit(item)}>
                                <CustomText style={styles.editBtnText}>수정</CustomText>
                            </TouchableOpacity>
                        )}
                        <TouchableOpacity style={[styles.actionBtn, styles.deleteBtn]} onPress={() => handleDelete(item.challengeId)}>
                            <CustomText style={styles.deleteBtnText}>삭제</CustomText>
                        </TouchableOpacity>
                    </View>
                )}
            </TouchableOpacity>
        );
    };

    const currentChallenges = getCurrentChallenges();
    const requestedChallenges = activeTab === TABS.REQUESTED
        ? currentChallenges.filter((challenge) => challenge.status !== 'APPROVED')
        : currentChallenges;
    const scheduledChallenges = activeTab === TABS.REQUESTED
        ? currentChallenges.filter((challenge) => challenge.status === 'APPROVED')
        : [];

    return (
        <SafeAreaView style={styles.safeArea}>
            <View style={styles.header}>
                <CustomText style={styles.headerTitle}>내 챌린지</CustomText>
            </View>

            <View style={styles.tabContainer}>
                <TouchableOpacity
                    style={[styles.tabBtn, activeTab === TABS.REQUESTED && styles.tabBtnActive]}
                    onPress={() => { setActiveTab(TABS.REQUESTED); setIsFabOpen(false); }}
                >
                    <CustomText style={[styles.tabText, activeTab === TABS.REQUESTED && styles.tabTextActive]}>요청된 챌린지</CustomText>
                </TouchableOpacity>
                <TouchableOpacity
                    style={[styles.tabBtn, activeTab === TABS.IN_PROGRESS && styles.tabBtnActive]}
                    onPress={() => { setActiveTab(TABS.IN_PROGRESS); setIsFabOpen(false); }}
                >
                    <CustomText style={[styles.tabText, activeTab === TABS.IN_PROGRESS && styles.tabTextActive]}>진행 중</CustomText>
                </TouchableOpacity>
                <TouchableOpacity
                    style={[styles.tabBtn, activeTab === TABS.COMPLETED && styles.tabBtnActive]}
                    onPress={() => { setActiveTab(TABS.COMPLETED); setIsFabOpen(false); }}
                >
                    <CustomText style={[styles.tabText, activeTab === TABS.COMPLETED && styles.tabTextActive]}>완료된 챌린지</CustomText>
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
                            {activeTab === TABS.COMPLETED ? '완료된 챌린지가 없어요!' : '표시할 챌린지가 없어요!'}
                        </CustomText>
                        {activeTab === TABS.REQUESTED && (
                            <TouchableOpacity style={styles.emptyAddBtn} onPress={() => navigation.navigate('ChallengePropose')}>
                                <CustomText style={styles.emptyAddBtnText}>새 챌린지 제안하기</CustomText>
                            </TouchableOpacity>
                        )}
                    </View>
                ) : (
                    activeTab === TABS.REQUESTED ? (
                        <>
                            {requestedChallenges.map(renderChallengeItem)}
                            {scheduledChallenges.length > 0 && (
                                <View style={styles.scheduledSection}>
                                    <TouchableOpacity
                                        style={styles.scheduledHeader}
                                        onPress={() => setIsScheduledOpen((prev) => !prev)}
                                        activeOpacity={0.8}
                                    >
                                        <CustomText style={styles.scheduledTitle}>진행 예정</CustomText>
                                    </TouchableOpacity>
                                    {isScheduledOpen && (
                                        <>
                                            <View style={styles.scheduledNotice}>
                                                <CustomText style={styles.scheduledNoticeText}>다음주 월요일에 챌린지가 시작해요!</CustomText>
                                            </View>
                                            {scheduledChallenges.map(renderChallengeItem)}
                                        </>
                                    )}
                                </View>
                            )}
                        </>
                    ) : (
                        currentChallenges.map(renderChallengeItem)
                    )
                )}

                <View style={[styles.card, { marginTop: verticalScale(24), backgroundColor: '#FFFFFF' }]} >
                    <CustomText style={styles.menuTitle}>데일리 미션</CustomText>

                    <TouchableOpacity style={styles.menuBtn} onPress={handleOpenQuizDifficulty}>
                        <View style={{ flex: 1 }}>
                            <CustomText style={styles.menuBtnTitle}>주간 금융 퀴즈</CustomText>
                            <CustomText style={styles.menuBtnSub}>퀴즈 풀고 랜덤 젤링 받기</CustomText>
                        </View>
                    </TouchableOpacity>

                    <TouchableOpacity style={styles.menuBtn} onPress={() => navigation.navigate('AttendanceScreen')}>
                        <View style={{ flex: 1 }}>
                            <CustomText style={styles.menuBtnTitle}>출석체크</CustomText>
                            <CustomText style={styles.menuBtnSub}>매일 출석하고 보상 받기</CustomText>
                        </View>
                    </TouchableOpacity>

                    <TouchableOpacity style={styles.menuBtn} onPress={() => navigation.navigate('ESGChallengeScreen')}>
                        <View style={{ flex: 1 }}>
                            <CustomText style={styles.menuBtnTitle}>ESG 챌린지</CustomText>
                            <CustomText style={styles.menuBtnSub}>착한 소비 인증하기</CustomText>
                        </View>
                    </TouchableOpacity>
                </View>
            </ScrollView>

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
            <Modal
                visible={isRewardSuccessVisible}
                transparent
                animationType="fade"
                onRequestClose={() => setIsRewardSuccessVisible(false)}
            >
                <View style={styles.successModalOverlay}>
                    <View style={styles.successModalCard}>
                        <View style={styles.successImageWrap}>
                            <Image source={AKKU_WELCOME_IMAGE} style={styles.successImage} resizeMode="contain" />
                        </View>
                        <CustomText style={styles.successTitle}>보상 요청 완료</CustomText>
                        <CustomText style={styles.successDescription}>
                            부모님께 보상 송금 요청을 보냈어요.
                        </CustomText>
                        <TouchableOpacity
                            style={styles.successButton}
                            onPress={() => setIsRewardSuccessVisible(false)}
                            activeOpacity={0.85}
                        >
                            <CustomText style={styles.successButtonText}>확인</CustomText>
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
        flexDirection: 'row', alignItems: 'center', justifyContent: 'center',
        paddingHorizontal: scale(16), paddingVertical: verticalScale(16), backgroundColor: '#FFFFFF'
    },
    headerTitle: { fontSize: scale(22), fontWeight: 'bold', color: '#111' },

    tabContainer: { flexDirection: 'row', backgroundColor: '#FFFFFF', borderBottomWidth: 1, borderBottomColor: '#E5E7EB' },
    tabBtn: { flex: 1, alignItems: 'center', paddingVertical: verticalScale(14), borderBottomWidth: 2, borderBottomColor: 'transparent' },
    tabBtnActive: { borderBottomColor: '#A3E635' },
    tabText: { fontSize: scale(15), color: '#9CA3AF', fontWeight: 'bold' },
    tabTextActive: { color: '#111' },

    container: { flexGrow: 1, padding: scale(16) },

    card: { backgroundColor: '#FFFFFF', borderRadius: scale(16), padding: scale(20), marginBottom: verticalScale(16), shadowColor: '#000', shadowOffset: { width: 0, height: verticalScale(2) }, shadowOpacity: 0.08, shadowRadius: scale(8), elevation: 3 },
    cardHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: verticalScale(12) },
    cardHeaderLeft: { flexDirection: 'row', alignItems: 'center' },
    badge: { paddingHorizontal: scale(10), paddingVertical: verticalScale(4), borderRadius: scale(12) },

    badgeProgress: { backgroundColor: '#FEF9C3', borderColor: '#FACC15', borderWidth: 1 },
    badgeTextProgress: { color: '#A16207', fontSize: scale(11), fontWeight: 'bold' },
    badgePending: { backgroundColor: '#ECFCCB' },
    badgeTextPending: { color: '#4D7C0F', fontSize: scale(11), fontWeight: 'bold' },
    badgeSuccess: { backgroundColor: '#D1FAE5', borderColor: '#34D399', borderWidth: 1 },
    badgeTextSuccess: { color: '#059669', fontSize: scale(11), fontWeight: 'bold' },
    badgeFailed: { backgroundColor: '#FEE2E2', borderColor: '#F87171', borderWidth: 1 },
    badgeTextFailed: { color: '#DC2626', fontSize: scale(11), fontWeight: 'bold' },

    categoryText: { fontSize: scale(14), fontWeight: '600', color: '#4B5563', marginLeft: scale(8) },
    ddayText: { fontSize: scale(12), fontWeight: '700', color: '#6B7280', marginLeft: scale(8) },
    goalText: { fontSize: scale(15), color: '#374151', marginBottom: verticalScale(8) },
    goalSubText: { fontSize: scale(14), color: '#6B7280' },
    parentMessageRow: {
        flexDirection: 'row',
        alignItems: 'center',
        marginTop: verticalScale(14),
        paddingTop: verticalScale(12),
        borderTopWidth: 1,
        borderTopColor: '#E5E7EB',
    },
    parentMessageImage: {
        width: scale(42),
        height: scale(42),
        marginRight: scale(10),
    },
    parentMessageText: {
        flex: 1,
        fontSize: scale(13),
        color: '#374151',
        lineHeight: scale(18),
    },

    rewardBtn: { backgroundColor: '#3B82F6', paddingVertical: verticalScale(12), borderRadius: scale(12), alignItems: 'center', marginTop: verticalScale(16), shadowColor: '#000', shadowOffset: { width: 0, height: 2 }, shadowOpacity: 0.05, shadowRadius: 4, elevation: 1, },
    rewardBtnText: { color: '#FFF', fontSize: scale(14), fontWeight: 'bold' },

    actionRow: { flexDirection: 'row', justifyContent: 'flex-end', marginTop: verticalScale(12) },
    actionBtn: { paddingHorizontal: scale(12), paddingVertical: verticalScale(6), borderRadius: scale(8), marginLeft: scale(8), shadowColor: '#000', shadowOffset: { width: 0, height: verticalScale(2) }, shadowOpacity: 0.08, shadowRadius: scale(4), elevation: 2 },
    editBtn: { backgroundColor: '#ECFCCB' },
    editBtnText: { color: '#4D7C0F', fontWeight: 'bold' },
    deleteBtn: { backgroundColor: '#FEE2E2' },
    deleteBtnText: { color: '#DC2626', fontWeight: 'bold' },

    emptyView: { alignItems: 'center', marginTop: verticalScale(32), marginBottom: verticalScale(8) },
    emptyText: { fontSize: scale(16), color: '#6B7280', marginBottom: verticalScale(16) },
    emptyAddBtn: {
        backgroundColor: '#A3E635',
        paddingVertical: verticalScale(12),
        paddingHorizontal: scale(24),
        borderRadius: scale(25),
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 2 },
        shadowOpacity: 0.05,
        shadowRadius: 4,
        elevation: 2,
    },
    emptyAddBtnText: { fontSize: scale(16), fontWeight: 'bold', color: '#111' },
    scheduledSection: { marginBottom: verticalScale(8) },
    scheduledHeader: {
        flexDirection: 'row',
        alignItems: 'center',
        marginBottom: verticalScale(12),
        paddingHorizontal: scale(4),
    },
    scheduledTitle: { fontSize: scale(16), fontWeight: 'bold', color: '#374151' },
    scheduledNotice: {
        backgroundColor: '#F9FAFB',
        borderRadius: scale(12),
        paddingHorizontal: scale(14),
        paddingVertical: verticalScale(5),
        marginBottom: verticalScale(12),
    },
    scheduledNoticeText: {
        fontSize: scale(12),
        color: '#6B7280',
        fontWeight: '600',
        textAlign: 'center',
    },

    menuTitle: { fontSize: scale(16), fontWeight: 'bold', color: '#4B5563', marginBottom: verticalScale(12), textAlign: 'center' },
    menuBtn: { flexDirection: 'row', alignItems: 'center', justifyContent: 'center', backgroundColor: '#F9FAFB', padding: scale(16), borderRadius: scale(12), marginBottom: verticalScale(12), shadowColor: '#000', shadowOffset: { width: 0, height: 2 }, shadowOpacity: 0.05, shadowRadius: 4, elevation: 1, },
    menuBtnIcon: { fontSize: scale(24), marginRight: scale(16) },
    menuBtnTitle: { fontSize: scale(15), fontWeight: 'bold', color: '#111', marginBottom: verticalScale(2), textAlign: 'center' },
    menuBtnSub: { fontSize: scale(13), color: '#6B7280', textAlign: 'center' },

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
    fabMenuItemText: { fontSize: scale(15), fontWeight: 'bold', color: '#111' },
    successModalOverlay: {
        flex: 1,
        backgroundColor: 'rgba(17, 24, 39, 0.38)',
        justifyContent: 'center',
        alignItems: 'center',
        paddingHorizontal: scale(24),
    },
    successModalCard: {
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
    successImageWrap: {
        width: scale(168),
        height: verticalScale(134),
        justifyContent: 'center',
        alignItems: 'center',
        marginBottom: verticalScale(8),
    },
    successImage: {
        width: '100%',
        height: '100%',
    },
    successTitle: {
        fontSize: scale(22),
        fontWeight: 'bold',
        color: '#111827',
        marginBottom: verticalScale(8),
    },
    successDescription: {
        fontSize: scale(14),
        lineHeight: scale(20),
        color: '#6B7280',
        textAlign: 'center',
        marginBottom: verticalScale(20),
    },
    successButton: {
        minWidth: scale(132),
        backgroundColor: '#A3E635',
        borderRadius: scale(999),
        paddingVertical: verticalScale(12),
        paddingHorizontal: scale(24),
        alignItems: 'center',
    },
    successButtonText: {
        fontSize: scale(15),
        fontWeight: 'bold',
        color: '#1F2937',
    }
});

export default ChallengeScreen;
