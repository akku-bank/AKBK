import React, { useCallback, useState } from 'react';
import { View, StyleSheet, TouchableOpacity, ScrollView, SafeAreaView, Alert } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { scale, verticalScale } from 'react-native-size-matters';
import CustomText from '../../../components/common/CustomText';
import api from '../../../api/axios';

const STATUS_META = {
    PENDING: { label: '대기중', type: 'pending' },
    APPROVED: { label: '승인됨', type: 'default' },
    IN_PROGRESS: { label: '진행중', type: 'default' },
    SUCCESS: { label: '성공', type: 'success' },
    FAIL: { label: '실패', type: 'fail' },
    REJECTED: { label: '반려', type: 'fail' },
    REWARD_REQUESTED: { label: '보상 요청', type: 'default' },
    REWARDED: { label: '보상 완료', type: 'success' },
};

const ParentChallengeManageScreen = ({ navigation, route }) => {
    const routeChild = route?.params?.child;
    const routeChildId = route?.params?.childId || routeChild?.childId;
    const routeChildName = route?.params?.childName || routeChild?.name;

    const [challenges, setChallenges] = useState([]);
    const [childName, setChildName] = useState(routeChildName || '자녀');
    const [isLoading, setIsLoading] = useState(true);

    useFocusEffect(
        useCallback(() => {
            const fetchChallenges = async () => {
                setIsLoading(true);

                try {
                    let resolvedChildId = routeChildId;
                    let resolvedChildName = routeChildName;

                    if (!resolvedChildId) {
                        const familyRes = await api.get('/families/members');
                        const firstChild = (familyRes.data?.data?.members || []).find(
                            member => member.role === 'CHILD' && member.userId
                        );

                        if (!firstChild) {
                            setChallenges([]);
                            setChildName('자녀');
                            return;
                        }

                        resolvedChildId = firstChild.userId;
                        resolvedChildName = firstChild.name;
                    }

                    const res = await api.get('/challenges/spending', {
                        params: { childId: resolvedChildId }
                    });

                    setChallenges(res.data?.data?.challenges || []);
                    setChildName(resolvedChildName || '자녀');
                } catch (e) {
                    console.error('Challenges Fetch Error', e);
                    setChallenges([]);
                    Alert.alert('오류', '자녀 챌린지 목록을 불러오지 못했습니다.');
                } finally {
                    setIsLoading(false);
                }
            };

            fetchChallenges();
        }, [routeChildId, routeChildName])
    );

    const renderStatusBadge = (status) => {
        const meta = STATUS_META[status] || { label: status, type: 'default' };

        return (
            <View
                style={[
                    styles.statusBadge,
                    meta.type === 'pending' && styles.statusBadgePending,
                    meta.type === 'fail' && styles.statusBadgeFail,
                    meta.type === 'success' && styles.statusBadgeSuccess,
                ]}
            >
                <CustomText
                    style={[
                        styles.statusText,
                        meta.type === 'pending' && styles.statusTextPending,
                        meta.type === 'fail' && styles.statusTextFail,
                        meta.type === 'success' && styles.statusTextSuccess,
                    ]}
                >
                    {meta.label}
                </CustomText>
            </View>
        );
    };

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
                <CustomText style={styles.sectionTitle}>{childName}의 소비 챌린지</CustomText>

                {isLoading ? (
                    <View style={styles.emptyBox}>
                        <CustomText style={styles.emptyText}>챌린지 목록을 불러오는 중입니다.</CustomText>
                    </View>
                ) : challenges.length === 0 ? (
                    <View style={styles.emptyBox}>
                        <CustomText style={styles.emptyText}>조회된 소비 챌린지가 없습니다.</CustomText>
                    </View>
                ) : (
                    challenges.map(item => (
                        <TouchableOpacity
                            key={item.challengeId}
                            style={styles.card}
                            onPress={() => item.status === 'PENDING'
                                ? navigation.navigate('MissionApprovalScreen', { challenge: item, childName })
                                : null}
                            activeOpacity={item.status === 'PENDING' ? 0.7 : 1}
                        >
                            <View style={styles.cardHeader}>
                                <CustomText style={styles.cardTitle}>
                                    {item.category} {Number(item.targetSpending || 0).toLocaleString()}원 아끼기
                                </CustomText>
                                {renderStatusBadge(item.status)}
                            </View>
                            <CustomText style={styles.childName}>도전자: {childName}</CustomText>

                            <View style={styles.infoSection}>
                                <View style={styles.infoRow}>
                                    <CustomText style={styles.infoLabel}>목표 금액</CustomText>
                                    <CustomText style={styles.infoValue}>
                                        {Number(item.targetSpending || 0).toLocaleString()}원
                                    </CustomText>
                                </View>
                                <View style={styles.infoRow}>
                                    <CustomText style={styles.infoLabel}>보상 금액</CustomText>
                                    <CustomText style={styles.infoValue}>
                                        {Number(item.rewardAmount || 0).toLocaleString()}원
                                    </CustomText>
                                </View>
                            </View>
                        </TouchableOpacity>
                    ))
                )}
            </ScrollView>
        </SafeAreaView>
    );
};

const styles = StyleSheet.create({
    safeArea: { flex: 1, backgroundColor: '#F3F4F6' },
    header: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        paddingHorizontal: scale(16),
        paddingVertical: verticalScale(16),
        backgroundColor: '#F3F4F6'
    },
    backButton: { width: scale(32), height: scale(32), justifyContent: 'center' },
    backButtonText: { fontSize: scale(22), fontWeight: 'bold', color: '#111' },
    headerTitle: { fontSize: scale(18), fontWeight: 'bold', color: '#111' },
    container: { flexGrow: 1, paddingHorizontal: scale(16), paddingTop: verticalScale(20) },

    sectionTitle: { fontSize: scale(16), fontWeight: 'bold', color: '#4B5563', marginBottom: verticalScale(16) },

    card: {
        backgroundColor: '#FFFFFF',
        borderRadius: scale(16),
        padding: scale(20),
        marginBottom: verticalScale(16),
        shadowColor: '#000',
        shadowOffset: { width: 0, height: verticalScale(2) },
        shadowOpacity: 0.05,
        shadowRadius: scale(8),
        elevation: 2
    },
    cardHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: verticalScale(8) },
    cardTitle: { fontSize: scale(16), fontWeight: 'bold', color: '#111', flex: 1, marginRight: scale(8) },

    statusBadge: { backgroundColor: '#E5E7EB', paddingHorizontal: scale(8), paddingVertical: verticalScale(4), borderRadius: scale(8) },
    statusText: { fontSize: scale(12), fontWeight: 'bold', color: '#374151' },
    statusBadgePending: { backgroundColor: '#FEF3C7' },
    statusTextPending: { color: '#D97706' },
    statusBadgeSuccess: { backgroundColor: '#DCFCE7' },
    statusTextSuccess: { color: '#15803D' },
    statusBadgeFail: { backgroundColor: '#FEE2E2' },
    statusTextFail: { color: '#B91C1C' },

    childName: { fontSize: scale(13), color: '#6B7280', marginBottom: verticalScale(16) },

    infoSection: { marginTop: verticalScale(4) },
    infoRow: { flexDirection: 'row', justifyContent: 'space-between', marginBottom: verticalScale(8) },
    infoLabel: { fontSize: scale(13), color: '#6B7280' },
    infoValue: { fontSize: scale(13), fontWeight: 'bold', color: '#111' },
    emptyBox: {
        backgroundColor: '#FFFFFF',
        borderRadius: scale(16),
        padding: scale(24),
        alignItems: 'center'
    },
    emptyText: { fontSize: scale(14), color: '#6B7280' }
});

export default ParentChallengeManageScreen;
