import React, { useCallback, useMemo, useState } from 'react';
import { View, StyleSheet, TouchableOpacity, ScrollView, SafeAreaView, Alert } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { scale, verticalScale } from 'react-native-size-matters';
import CustomText from '../../../components/common/CustomText';
import api from '../../../api/axios';

const TABS = {
    REQUESTED: 'REQUESTED',
    THIS_WEEK: 'THIS_WEEK',
    REWARD_REQUESTED: 'REWARD_REQUESTED',
};

const STATUS_META = {
    PENDING: { label: '\uC2B9\uC778 \uB300\uAE30', type: 'pending' },
    APPROVED: { label: '승인 완료', type: 'approved' },
    IN_PROGRESS: { label: '진행 중', type: 'approved' },
    SUCCESS: { label: '\uC131\uACF5', type: 'success' },
    FAIL: { label: '\uC2E4\uD328', type: 'fail' },
    REJECTED: { label: '\uBC18\uB824', type: 'fail' },
    REWARD_REQUESTED: { label: '\uBCF4\uC0C1 \uC694\uCCAD', type: 'pending' },
    REWARDED: { label: '\uC1A1\uAE08 \uC644\uB8CC', type: 'success' },
};

const ParentChallengeManageScreen = ({ navigation, route }) => {
    const routeChild = route?.params?.child;
    const routeChildId = route?.params?.childId || routeChild?.childId;
    const routeChildName = route?.params?.childName || routeChild?.name;

    const [activeTab, setActiveTab] = useState(TABS.REQUESTED);
    const [children, setChildren] = useState([]);
    const [selectedChildId, setSelectedChildId] = useState(routeChildId || null);
    const [childName, setChildName] = useState(routeChildName || '\uC790\uB140');
    const [childAccountId, setChildAccountId] = useState(routeChild?.accountId || null);
    const [parentAccountId, setParentAccountId] = useState(null);
    const [requestedChallenges, setRequestedChallenges] = useState([]);
    const [thisWeekChallenges, setThisWeekChallenges] = useState([]);
    const [rewardRequestedChallenges, setRewardRequestedChallenges] = useState([]);
    const [isLoading, setIsLoading] = useState(true);
    const [transferringId, setTransferringId] = useState(null);

    const fetchChallenges = useCallback(async () => {
        setIsLoading(true);

        try {
            const familyRes = await api.get('/families/members');

            const childMembers = (familyRes.data?.data?.members || []).filter(
                (member) => member.role === 'CHILD' && member.userId
            );
            setChildren(childMembers);

            const resolvedChild =
                childMembers.find((member) => member.userId === (selectedChildId || routeChildId)) ||
                childMembers[0];

            if (!resolvedChild) {
                setSelectedChildId(null);
                setChildName('\uC790\uB140');
                setChildAccountId(null);
                setRequestedChallenges([]);
                setThisWeekChallenges([]);
                setRewardRequestedChallenges([]);
                return;
            }

            const resolvedChildId = resolvedChild.userId;
            const resolvedChildName = resolvedChild.name;
            const resolvedChildAccountId = Array.isArray(resolvedChild.accountIds) ? resolvedChild.accountIds[0] : null;

            if (resolvedChildId !== selectedChildId) {
                setSelectedChildId(resolvedChildId);
            }

            setChildName(resolvedChildName || '\uC790\uB140');
            setChildAccountId(resolvedChildAccountId || null);

            const [requestedRes, thisWeekRes, rewardRes] = await Promise.all([
                api.get('/challenges/spending', { params: { childId: resolvedChildId } }),
                api.get('/challenges/spending/this-week', { params: { childId: resolvedChildId } }),
                api.get('/challenges/spending/reward-requests', { params: { childId: resolvedChildId } }),
            ]);

            setRequestedChallenges(requestedRes.data?.data?.challenges || []);
            setThisWeekChallenges(thisWeekRes.data?.data?.challenges || []);
            setRewardRequestedChallenges(rewardRes.data?.data?.challenges || []);
        } catch (error) {
            console.error('Parent challenge fetch error', error);
            setRequestedChallenges([]);
            setThisWeekChallenges([]);
            setRewardRequestedChallenges([]);
            Alert.alert('\uC624\uB958', '\uBD80\uBAA8 \uCC4C\uB9B0\uC9C0 \uBAA9\uB85D\uC744 \uBD88\uB7EC\uC624\uC9C0 \uBABB\uD588\uC2B5\uB2C8\uB2E4.');
        } finally {
            setIsLoading(false);
        }
    }, [routeChildId, selectedChildId]);

    useFocusEffect(
        useCallback(() => {
            fetchChallenges();
        }, [fetchChallenges])
    );

    const currentChallenges = useMemo(() => {
        if (activeTab === TABS.REQUESTED) return requestedChallenges;
        if (activeTab === TABS.THIS_WEEK) return thisWeekChallenges;
        return rewardRequestedChallenges;
    }, [activeTab, requestedChallenges, thisWeekChallenges, rewardRequestedChallenges]);

    const renderStatusBadge = (status) => {
        const meta = STATUS_META[status] || { label: status, type: 'default' };

        return (
            <View
                style={[
                    styles.statusBadge,
                    meta.type === 'pending' && styles.statusBadgePending,
                    meta.type === 'fail' && styles.statusBadgeFail,
                    meta.type === 'success' && styles.statusBadgeSuccess,
                    meta.type === 'approved' && styles.statusBadgeApproved,
                ]}
            >
                <CustomText
                    style={[
                        styles.statusText,
                        meta.type === 'pending' && styles.statusTextPending,
                        meta.type === 'fail' && styles.statusTextFail,
                        meta.type === 'success' && styles.statusTextSuccess,
                        meta.type === 'approved' && styles.statusTextApproved,
                    ]}
                >
                    {meta.label}
                </CustomText>
            </View>
        );
    };

    const handleRewardTransfer = (challenge) => {
        if (!childAccountId) {
            Alert.alert('\uC548\uB0B4', '\uC790\uB140 \uACC4\uC88C\uAC00 \uC5F0\uB3D9\uB418\uC5B4 \uC788\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4.');
            return;
        }

        Alert.alert(
            '\uBCF4\uC0C1 \uC1A1\uAE08',
            `${childName}\uC5D0\uAC8C ${Number(challenge.rewardAmount || 0).toLocaleString()}\uC6D0\uC744 \uC1A1\uAE08\uD560\uAE4C\uC694?`,
            [
                { text: '\uCDE8\uC18C', style: 'cancel' },
                {
                    text: '\uC1A1\uAE08\uD558\uAE30',
                    onPress: async () => {
                        try {
                            setTransferringId(challenge.challengeId);
                            let resolvedParentAccountId = parentAccountId;

                            if (!resolvedParentAccountId) {
                                const accountRes = await api.get('/bank/accounts/me');
                                const parentAccounts = accountRes.data?.data?.accounts || [];
                                resolvedParentAccountId =
                                    parentAccounts[0]?.accountId || accountRes.data?.data?.accountId || null;

                                if (!resolvedParentAccountId) {
                                    Alert.alert('\uC548\uB0B4', '\uBD80\uBAA8 \uCD9C\uAE08 \uACC4\uC88C\uB97C \uCC3E\uC9C0 \uBABB\uD588\uC2B5\uB2C8\uB2E4.');
                                    return;
                                }

                                setParentAccountId(resolvedParentAccountId);
                            }

                            await api.post(`/challenges/spending/${challenge.challengeId}/reward-transfer`, {
                                parentAccountId: resolvedParentAccountId,
                                childAccountId,
                            });
                            Alert.alert('\uC1A1\uAE08 \uC644\uB8CC', '\uBCF4\uC0C1 \uC1A1\uAE08\uC744 \uC644\uB8CC\uD588\uC2B5\uB2C8\uB2E4.');
                            fetchChallenges();
                        } catch (error) {
                            console.error('Reward transfer error', error);
                            Alert.alert('\uC624\uB958', error.response?.data?.message || '\uBCF4\uC0C1 \uC1A1\uAE08\uC5D0 \uC2E4\uD328\uD588\uC2B5\uB2C8\uB2E4.');
                        } finally {
                            setTransferringId(null);
                        }
                    }
                }
            ]
        );
    };

    const renderCard = (item) => {
        const canReview = activeTab === TABS.REQUESTED && item.status === 'PENDING';
        const canTransfer = activeTab === TABS.REWARD_REQUESTED && item.status === 'REWARD_REQUESTED';

        return (
            <TouchableOpacity
                key={item.challengeId}
                style={styles.card}
                onPress={() => {
                    navigation.navigate('ChallengeDetail', {
                        challengeId: item.challengeId,
                        childName,
                        hideParentMessage: true,
                        hideDday: activeTab === TABS.REWARD_REQUESTED,
                    });
                }}
                activeOpacity={0.82}
            >
                <View style={styles.cardHeader}>
                    <CustomText style={styles.cardTitle} numberOfLines={2}>
                        {`${item.category} ${Number(item.targetSpending || 0).toLocaleString()}\uC6D0 \uC774\uD558\uB85C \uC18C\uBE44\uD558\uAE30`}
                    </CustomText>
                    {renderStatusBadge(item.status)}
                </View>

                <View style={styles.infoSection}>
                    <View style={styles.infoRow}>
                        <CustomText style={styles.infoLabel}>{'\uBAA9\uD45C \uAE08\uC561'}</CustomText>
                        <CustomText style={styles.infoValue}>
                            {`${Number(item.targetSpending || 0).toLocaleString()}\uC6D0`}
                        </CustomText>
                    </View>
                    <View style={styles.infoRow}>
                        <CustomText style={styles.infoLabel}>{'\uBCF4\uC0C1 \uAE08\uC561'}</CustomText>
                        <CustomText style={styles.infoValue}>
                            {`${Number(item.rewardAmount || 0).toLocaleString()}\uC6D0`}
                        </CustomText>
                    </View>
                    {item.startDate && item.endDate ? (
                        <View style={styles.infoRow}>
                            <CustomText style={styles.infoLabel}>{'\uAE30\uAC04'}</CustomText>
                            <CustomText style={styles.infoValue}>{`${item.startDate} ~ ${item.endDate}`}</CustomText>
                        </View>
                    ) : null}
                </View>

                {item.parentMessage && activeTab === TABS.REQUESTED ? (
                    <View style={styles.memoBox}>
                        <CustomText style={styles.memoLabel}>{'\uBA54\uBAA8'}</CustomText>
                        <CustomText style={styles.memoText}>{item.parentMessage}</CustomText>
                    </View>
                ) : null}

                {canReview ? (
                    <View style={styles.cardActionRow}>
                        <TouchableOpacity
                            style={styles.reviewButton}
                            onPress={() => navigation.navigate('MissionApprovalScreen', { challenge: item, childName })}
                        >
                            <CustomText style={styles.reviewButtonText}>{'\uAC80\uD1A0\uD558\uAE30'}</CustomText>
                        </TouchableOpacity>
                    </View>
                ) : null}

                {canTransfer ? (
                    <View style={styles.transferSection}>
                        <TouchableOpacity
                            style={[styles.transferButton, transferringId === item.challengeId && styles.transferButtonDisabled]}
                            onPress={() => handleRewardTransfer(item)}
                            disabled={transferringId === item.challengeId}
                        >
                            <CustomText style={styles.transferButtonText}>
                                {transferringId === item.challengeId ? '\uC1A1\uAE08 \uC911...' : '\uC1A1\uAE08\uD558\uAE30'}
                            </CustomText>
                        </TouchableOpacity>
                    </View>
                ) : null}
            </TouchableOpacity>
        );
    };

    return (
        <SafeAreaView style={styles.safeArea}>
            <View style={styles.header}>
                <CustomText style={styles.headerTitle}>{'\uC6A9\uB3C8 \uBBF8\uC158 \uAD00\uB9AC'}</CustomText>
            </View>

            <ScrollView contentContainerStyle={styles.container} showsVerticalScrollIndicator={false}>
                <View style={styles.tabContainer}>
                    <TouchableOpacity
                        style={[styles.tabButton, activeTab === TABS.REQUESTED && styles.tabButtonActive]}
                        onPress={() => setActiveTab(TABS.REQUESTED)}
                    >
                        <CustomText style={[styles.tabButtonText, activeTab === TABS.REQUESTED && styles.tabButtonTextActive]}>
                            {'\uC694\uCCAD\uB41C \uCC4C\uB9B0\uC9C0'}
                        </CustomText>
                    </TouchableOpacity>
                    <TouchableOpacity
                        style={[styles.tabButton, activeTab === TABS.THIS_WEEK && styles.tabButtonActive]}
                        onPress={() => setActiveTab(TABS.THIS_WEEK)}
                    >
                        <CustomText style={[styles.tabButtonText, activeTab === TABS.THIS_WEEK && styles.tabButtonTextActive]}>
                            {'\uC774\uBC88 \uC8FC'}
                        </CustomText>
                    </TouchableOpacity>
                    <TouchableOpacity
                        style={[styles.tabButton, activeTab === TABS.REWARD_REQUESTED && styles.tabButtonActive]}
                        onPress={() => setActiveTab(TABS.REWARD_REQUESTED)}
                    >
                        <CustomText style={[styles.tabButtonText, activeTab === TABS.REWARD_REQUESTED && styles.tabButtonTextActive]}>
                            {'\uBCF4\uC0C1 \uC694\uCCAD'}
                        </CustomText>
                    </TouchableOpacity>
                </View>

                {children.length > 1 && (
                    <ScrollView
                        horizontal
                        style={styles.childTabsWrapper}
                        showsHorizontalScrollIndicator={false}
                        contentContainerStyle={styles.childTabs}
                    >
                        {children.map((child) => {
                            const isActive = child.userId === selectedChildId;

                            return (
                                <TouchableOpacity
                                    key={child.userId}
                                    style={[styles.childTab, isActive && styles.childTabActive]}
                                    onPress={() => setSelectedChildId(child.userId)}
                                    activeOpacity={0.85}
                                >
                                    <CustomText style={[styles.childTabText, isActive && styles.childTabTextActive]}>
                                        {child.name}
                                    </CustomText>
                                </TouchableOpacity>
                            );
                        })}
                    </ScrollView>
                )}

                <CustomText style={styles.sectionTitle}>
                    {`${childName}${activeTab === TABS.REQUESTED ? '\uC758 \uC694\uCCAD\uB41C \uCC4C\uB9B0\uC9C0' : activeTab === TABS.THIS_WEEK ? '\uC758 \uC774\uBC88 \uC8FC \uCC4C\uB9B0\uC9C0' : '\uC758 \uBCF4\uC0C1 \uC694\uCCAD \uBAA9\uB85D'}`}
                </CustomText>

                {isLoading ? (
                    <View style={styles.emptyBox}>
                        <CustomText style={styles.emptyText}>{'\uCC4C\uB9B0\uC9C0 \uBAA9\uB85D\uC744 \uBD88\uB7EC\uC624\uB294 \uC911\uC785\uB2C8\uB2E4.'}</CustomText>
                    </View>
                ) : currentChallenges.length === 0 ? (
                    <View style={styles.emptyBox}>
                        <CustomText style={styles.emptyText}>{'\uC870\uD68C\uB41C \uCC4C\uB9B0\uC9C0\uAC00 \uC5C6\uC2B5\uB2C8\uB2E4.'}</CustomText>
                    </View>
                ) : (
                    currentChallenges.map(renderCard)
                )}
            </ScrollView>
        </SafeAreaView>
    );
};

const styles = StyleSheet.create({
    safeArea: { flex: 1, backgroundColor: '#F9FAFB' },
    header: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'center',
        paddingHorizontal: scale(16),
        paddingVertical: verticalScale(16),
        backgroundColor: '#F9FAFB',
    },
    headerTitle: { fontSize: scale(18), fontWeight: 'bold', color: '#111827' },
    container: { flexGrow: 1, paddingHorizontal: scale(16), paddingTop: verticalScale(20), paddingBottom: verticalScale(28) },

    childTabsWrapper: { flexGrow: 0, marginBottom: verticalScale(16) },
    childTabs: { alignItems: 'center' },
    childTab: {
        backgroundColor: '#E5E7EB',
        borderRadius: scale(999),
        paddingHorizontal: scale(14),
        paddingVertical: verticalScale(8),
        marginRight: scale(8),
        alignSelf: 'flex-start',
    },
    childTabActive: { backgroundColor: '#D9F99D' },
    childTabText: { fontSize: scale(13), fontWeight: '600', color: '#4B5563' },
    childTabTextActive: { color: '#365314' },

    tabContainer: {
        flexDirection: 'row',
        backgroundColor: '#FFFFFF',
        borderRadius: scale(14),
        padding: scale(4),
        marginBottom: verticalScale(16),
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 2 },
        shadowOpacity: 0.05,
        shadowRadius: 4,
        elevation: 2,
    },
    tabButton: {
        flex: 1,
        borderRadius: scale(10),
        paddingVertical: verticalScale(10),
        alignItems: 'center',
    },
    tabButtonActive: { backgroundColor: '#ECFCCB' },
    tabButtonText: { fontSize: scale(12), fontWeight: '700', color: '#6B7280' },
    tabButtonTextActive: { color: '#111827' },

    sectionTitle: {
        fontSize: scale(16),
        fontWeight: 'bold',
        color: '#4B5563',
        marginBottom: verticalScale(16),
        marginLeft: scale(4),
    },

    card: {
        backgroundColor: '#FFFFFF',
        borderRadius: scale(16),
        padding: scale(16),
        marginBottom: verticalScale(12),
        shadowColor: '#000',
        shadowOffset: { width: 0, height: verticalScale(2) },
        shadowOpacity: 0.05,
        shadowRadius: scale(8),
        elevation: 2,
    },
    cardHeader: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'flex-start',
        marginBottom: verticalScale(8),
    },
    cardTitle: {
        fontSize: scale(16),
        fontWeight: 'bold',
        color: '#111827',
        flex: 1,
        marginRight: scale(8),
        lineHeight: scale(22),
    },
    statusBadge: {
        backgroundColor: '#E5E7EB',
        paddingHorizontal: scale(8),
        paddingVertical: verticalScale(4),
        borderRadius: scale(8),
    },
    statusText: { fontSize: scale(12), fontWeight: 'bold', color: '#374151' },
    statusBadgePending: { backgroundColor: '#FEF3C7' },
    statusTextPending: { color: '#D97706' },
    statusBadgeSuccess: { backgroundColor: '#DCFCE7' },
    statusTextSuccess: { color: '#15803D' },
    statusBadgeFail: { backgroundColor: '#FEE2E2' },
    statusTextFail: { color: '#B91C1C' },
    statusBadgeApproved: { backgroundColor: '#A3E635' },
    statusTextApproved: { color: '#111827' },

    infoSection: { marginTop: verticalScale(2) },
    infoRow: { flexDirection: 'row', justifyContent: 'space-between', marginBottom: verticalScale(5) },
    infoLabel: { fontSize: scale(13), color: '#6B7280' },
    infoValue: { fontSize: scale(13), fontWeight: 'bold', color: '#111827' },

    memoBox: {
        marginTop: verticalScale(10),
        backgroundColor: '#F9FAFB',
        borderRadius: scale(12),
        padding: scale(12),
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 2 },
        shadowOpacity: 0.05,
        shadowRadius: 4,
        elevation: 2,
    },
    memoLabel: {
        fontSize: scale(11),
        fontWeight: '700',
        color: '#6B7280',
        marginBottom: verticalScale(4),
    },
    memoText: {
        fontSize: scale(13),
        color: '#374151',
        lineHeight: scale(18),
    },

    cardActionRow: {
        marginTop: verticalScale(12),
    },
    reviewButton: {
        backgroundColor: '#111827',
        borderRadius: scale(12),
        paddingVertical: verticalScale(11),
        alignItems: 'center',
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 2 },
        shadowOpacity: 0.05,
        shadowRadius: 4,
        elevation: 2,
    },
    reviewButtonText: {
        fontSize: scale(13),
        color: '#FFFFFF',
        fontWeight: '700',
    },

    transferSection: {
        marginTop: verticalScale(14),
    },
    transferButton: {
        backgroundColor: '#A3E635',
        borderRadius: scale(12),
        paddingVertical: verticalScale(12),
        alignItems: 'center',
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 2 },
        shadowOpacity: 0.05,
        shadowRadius: 4,
        elevation: 2,
    },
    transferButtonDisabled: {
        opacity: 0.6,
    },
    transferButtonText: {
        fontSize: scale(14),
        fontWeight: 'bold',
        color: '#111827',
    },

    emptyBox: {
        backgroundColor: '#FFFFFF',
        borderRadius: scale(16),
        padding: scale(24),
        alignItems: 'center',
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 2 },
        shadowOpacity: 0.05,
        shadowRadius: 4,
        elevation: 2,
    },
    emptyText: { fontSize: scale(14), color: '#6B7280' },
});

export default ParentChallengeManageScreen;
