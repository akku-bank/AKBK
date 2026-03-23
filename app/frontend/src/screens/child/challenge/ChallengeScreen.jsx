import React, { useState, useEffect } from 'react';
import { View, StyleSheet, SafeAreaView, ScrollView, TouchableOpacity, Alert } from 'react-native';
import { scale, verticalScale } from 'react-native-size-matters';
import CustomText from '../../../components/common/CustomText';
import api from '../../../api/axios'; // 백엔드 연동 전까지 Mock 사용

const MOCK_CHALLENGES = [
    { id: 1, category: '간식', targetAmount: 5000, status: 'IN_PROGRESS', term: 'THIS_WEEK' },
    { id: 2, category: '쇼핑', targetAmount: 10000, status: 'SUCCESS', term: 'THIS_WEEK', isRewardRequested: false },
    { id: 3, category: '게임', targetAmount: 3000, status: 'FAIL', term: 'THIS_WEEK' },
    { id: 4, category: '기타', targetAmount: 2000, status: 'PENDING', term: 'NEXT_WEEK' },
    { id: 5, category: '간식', targetAmount: 15000, status: 'APPROVED', term: 'NEXT_WEEK' },
    { id: 6, category: '쇼핑', targetAmount: 4000, status: 'REJECTED', term: 'THIS_WEEK', rejectReason: '조금 더 목표를 높여볼까요?' }
];

const STATUS_UI = {
    PENDING: { label: '승인 대기', bg: '#F3F4F6', text: '#6B7280' },
    APPROVED: { label: '승인 완료', bg: '#DBEAFE', text: '#1D4ED8' },
    IN_PROGRESS: { label: '진행 중', bg: '#FEF9C3', text: '#A16207' },
    SUCCESS: { label: '성공', bg: '#DCFCE7', text: '#15803D' },
    FAIL: { label: '실패', bg: '#FEE2E2', text: '#B91C1C' },
    REJECTED: { label: '반려됨', bg: '#FEE2E2', text: '#B91C1C' }
};

const ChallengeScreen = ({ navigation }) => {
    const [activeTab, setActiveTab] = useState('PAST'); // 'PAST', 'THIS_WEEK', 'NEXT_WEEK'
    const [challenges, setChallenges] = useState([]);
    const [isFabOpen, setIsFabOpen] = useState(false);

    useEffect(() => {
        // 백엔드 통신을 가정하여 Mock Data 로드
        let loadedData = [...MOCK_CHALLENGES];

        // 1. 반려된 챌린지 검출 및 추출
        const rejectedItems = loadedData.filter(c => c.status === 'REJECTED');
        if (rejectedItems.length > 0) {
            Alert.alert(
                '챌린지 반려 알림',
                `부모님이 다음 챌린지를 반려하셨어요:\n\n${rejectedItems.map(r => `- ${r.category} (${r.targetAmount}원)\n사유: ${r.rejectReason}`).join('\n')}`,
                [{ text: '확인' }]
            );
            // 반려된 항목 화면 목록에서 제거 (프론트 단독 처리)
            loadedData = loadedData.filter(c => c.status !== 'REJECTED');
        }

        setChallenges(loadedData);
    }, []);

    const handleRewardRequest = async (id) => {
        // [API 우회 연동] 백엔드의 NotificationRequest DTO 구조 문제로 500 에러 발생. 프론트엔드 단독 모의 처리.
        try {
            /* 
            await api.post('/notifications/test', {
                title: '보상 요청',
                body: '자녀가 챌린지 성공 보상을 요청했어요!'
            });
            */
            // 백엔드 요청 없이 성공 처리로 우회 (1초 대기)
            await new Promise(resolve => setTimeout(resolve, 1000));

            Alert.alert('요청 완료', '부모님께 보상 송금 요청 알림을 보냈습니다.');

            // 로컬 상태 업데이트 (버튼 비활성화)
            setChallenges(prev => prev.map(c => c.id === id ? { ...c, isRewardRequested: true } : c));
        } catch (error) {
            console.error('Reward request error', error);
            Alert.alert('오류', '보상 요청 알림 발송에 실패했습니다.');
        }
    };

    const handleDelete = (id) => {
        Alert.alert(
            '챌린지 삭제',
            '정말로 이 챌린지를 삭제하시겠습니까?',
            [
                { text: '취소', style: 'cancel' },
                {
                    text: '삭제',
                    onPress: () => {
                        setChallenges(prev => prev.filter(c => c.id !== id));
                        Alert.alert('삭제 완료', '챌린지가 삭제되었습니다.');
                    },
                    style: 'destructive'
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

    const renderChallengeItem = ({ item }) => {
        let statusBadge = null;
        let showButtons = false;

        // Map existing statuses to new ones for rendering
        let displayStatus = item.status;
        if (item.status === 'IN_PROGRESS') displayStatus = 'PROGRESS';
        if (item.status === 'FAIL') displayStatus = 'FAILED';
        if (item.status === 'APPROVED') displayStatus = 'PROGRESS'; // Assuming APPROVED means it's now in progress

        if (displayStatus === 'PROGRESS') {
            statusBadge = <View style={[styles.badge, styles.badgeProgress]}><CustomText style={styles.badgeTextProgress}>진행중</CustomText></View>;
        } else if (displayStatus === 'PENDING') {
            statusBadge = <View style={[styles.badge, styles.badgePending]}><CustomText style={styles.badgeTextPending}>대기중</CustomText></View>;
            showButtons = true;
        } else if (displayStatus === 'SUCCESS') {
            statusBadge = <View style={[styles.badge, styles.badgeSuccess]}><CustomText style={styles.badgeTextSuccess}>성공 🎉</CustomText></View>;
        } else if (displayStatus === 'FAILED') {
            statusBadge = <View style={[styles.badge, styles.badgeFailed]}><CustomText style={styles.badgeTextFailed}>실패 💦</CustomText></View>;
        }

        return (
            <View style={styles.card}>
                <View style={styles.cardHeader}>
                    <View style={styles.cardHeaderLeft}>
                        {statusBadge}
                        <CustomText style={styles.categoryText}>{item.category} 아끼기</CustomText>
                    </View>
                </View>
                <CustomText style={styles.goalText}>목표 금액: {item.targetAmount.toLocaleString()}원</CustomText>

                {item.status === 'SUCCESS' && (
                    <TouchableOpacity
                        style={[styles.rewardBtn, item.isRewardRequested && styles.rewardBtnDisabled]}
                        disabled={item.isRewardRequested}
                        onPress={() => handleRewardRequest(item.id)}
                    >
                        <CustomText style={[styles.rewardBtnText, item.isRewardRequested && styles.rewardBtnTextDisabled]}>
                            {item.isRewardRequested ? '요청 완료' : '보상 요청'}
                        </CustomText>
                    </TouchableOpacity>
                )}
                {showButtons && (
                    <View style={styles.actionRow}>
                        <TouchableOpacity style={[styles.actionBtn, styles.deleteBtn]} onPress={() => handleDelete(item.id)}>
                            <CustomText style={styles.deleteBtnText}>삭제</CustomText>
                        </TouchableOpacity>
                    </View>
                )}
            </View>
        );
    };

    const filteredList = challenges.filter(c => c.term === activeTab);

    return (
        <SafeAreaView style={styles.safeArea}>
            <View style={styles.header}>
                <CustomText style={styles.headerTitle}>내 챌린지</CustomText>
            </View>

            <View style={styles.tabContainer}>
                <TouchableOpacity
                    style={[styles.tabBtn, activeTab === 'PAST' && styles.tabBtnActive]}
                    onPress={() => { setActiveTab('PAST'); setIsFabOpen(false); }}
                >
                    <CustomText style={[styles.tabText, activeTab === 'PAST' && styles.tabTextActive]}>지난 챌린지</CustomText>
                </TouchableOpacity>
                <TouchableOpacity
                    style={[styles.tabBtn, activeTab === 'THIS_WEEK' && styles.tabBtnActive]}
                    onPress={() => { setActiveTab('THIS_WEEK'); setIsFabOpen(false); }}
                >
                    <CustomText style={[styles.tabText, activeTab === 'THIS_WEEK' && styles.tabTextActive]}>이번 주 챌린지</CustomText>
                </TouchableOpacity>
                <TouchableOpacity
                    style={[styles.tabBtn, activeTab === 'NEXT_WEEK' && styles.tabBtnActive]}
                    onPress={() => { setActiveTab('NEXT_WEEK'); setIsFabOpen(false); }}
                >
                    <CustomText style={[styles.tabText, activeTab === 'NEXT_WEEK' && styles.tabTextActive]}>다음 주 대기</CustomText>
                </TouchableOpacity>
            </View>

            <ScrollView contentContainerStyle={styles.container} showsVerticalScrollIndicator={false}>
                {filteredList.length === 0 ? (
                    <View style={styles.emptyView}>
                        <CustomText style={styles.emptyText}>완료한 챌린지가 없어요!</CustomText>
                        {activeTab !== 'PAST' && (
                            <TouchableOpacity style={styles.emptyAddBtn} onPress={() => navigation.navigate('ChallengePropose')}>
                                <CustomText style={styles.emptyAddBtnText}>새 챌린지 제안하기</CustomText>
                            </TouchableOpacity>
                        )}
                    </View>
                ) : (
                    filteredList.map((item) => renderChallengeItem({ item }))
                )}

                {/* 하단 고정 메뉴들 (기존 퀴즈, 출석 등) */}
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

            {activeTab !== 'PAST' && (
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
    proposeBtn: { backgroundColor: '#A3E635', paddingHorizontal: scale(12), paddingVertical: verticalScale(6), borderRadius: scale(20) },
    proposeBtnText: { fontSize: scale(13), fontWeight: 'bold', color: '#111' },

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
    badgeText: { fontSize: scale(12), fontWeight: 'bold' },

    badgeProgress: { backgroundColor: '#FEF9C3', borderColor: '#FACC15', borderWidth: 1 },
    badgeTextProgress: { color: '#A16207', fontSize: scale(11), fontWeight: 'bold' },
    badgePending: { backgroundColor: '#F3F4F6', borderColor: '#D1D5DB', borderWidth: 1 },
    badgeTextPending: { color: '#6B7280', fontSize: scale(11), fontWeight: 'bold' },

    challengeTitle: { fontSize: scale(16), fontWeight: '600', color: '#111', lineHeight: 24, marginBottom: verticalScale(16) },
    categoryText: { fontSize: scale(14), fontWeight: '600', color: '#4B5563', marginLeft: scale(8) },
    goalText: { fontSize: scale(15), color: '#374151', marginBottom: verticalScale(8) },

    rewardBtn: { backgroundColor: '#3B82F6', paddingVertical: verticalScale(12), borderRadius: scale(12), alignItems: 'center' },
    rewardBtnDisabled: { backgroundColor: '#E5E7EB' },
    rewardBtnText: { color: '#FFF', fontSize: scale(14), fontWeight: 'bold' },
    rewardBtnTextDisabled: { color: '#9CA3AF' },

    actionRow: { flexDirection: 'row', justifyContent: 'flex-end', marginTop: verticalScale(12) },
    actionBtn: { paddingHorizontal: scale(12), paddingVertical: verticalScale(6), borderRadius: scale(8), marginLeft: scale(8) },
    deleteBtn: { backgroundColor: '#FEE2E2' },
    deleteBtnText: { color: '#DC2626', fontWeight: 'bold' },

    emptyView: { flex: 1, justifyContent: 'center', alignItems: 'center', minHeight: verticalScale(150), paddingVertical: verticalScale(40) },
    emptyText: {
        fontSize: scale(16),
        color: '#6B7280',
        marginBottom: verticalScale(16),
    },
    emptyAddBtn: {
        backgroundColor: '#A3E635',
        paddingVertical: verticalScale(12),
        paddingHorizontal: scale(24),
        borderRadius: scale(25),
    },
    emptyAddBtnText: {
        fontSize: scale(16),
        fontWeight: 'bold',
        color: '#111',
    },

    menuTitle: { fontSize: scale(16), fontWeight: 'bold', color: '#4B5563', marginBottom: verticalScale(12) },
    menuBtn: { flexDirection: 'row', alignItems: 'center', backgroundColor: '#FFFFFF', padding: scale(16), borderRadius: scale(12), marginBottom: verticalScale(12), borderWidth: 1, borderColor: '#E5E7EB' },
    menuBtnIcon: { fontSize: scale(24), marginRight: scale(16) },
    menuBtnTitle: { fontSize: scale(15), fontWeight: 'bold', color: '#111', marginBottom: verticalScale(2) },
    menuBtnSub: { fontSize: scale(13), color: '#6B7280' },

    badgeSuccess: { backgroundColor: '#D1FAE5', borderColor: '#34D399', borderWidth: 1 },
    badgeTextSuccess: { color: '#059669', fontSize: scale(11), fontWeight: 'bold' },
    badgeFailed: { backgroundColor: '#FEE2E2', borderColor: '#F87171', borderWidth: 1 },
    badgeTextFailed: { color: '#DC2626', fontSize: scale(11), fontWeight: 'bold' },
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
    fabOpen: {
        backgroundColor: '#6B7280',
    },
    fabIcon: {
        fontSize: scale(32),
        lineHeight: scale(36),
        fontWeight: 'bold',
        color: '#fff',
    },
    fabIconOpen: {
        fontSize: scale(32),
        transform: [{ rotate: '45deg' }],
    },
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
    fabMenuItemText: {
        fontSize: scale(15),
        fontWeight: 'bold',
        color: '#111',
    }
});

export default ChallengeScreen;
