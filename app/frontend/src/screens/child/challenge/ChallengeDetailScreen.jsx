import React, { useCallback, useState } from 'react';
import { View, StyleSheet, SafeAreaView, TouchableOpacity, Alert, ScrollView } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { scale, verticalScale } from 'react-native-size-matters';
import CustomText from '../../../components/common/CustomText';
import api from '../../../api/axios';

const STATUS_LABELS = {
    PENDING: '승인 대기',
    APPROVED: '승인 완료',
    IN_PROGRESS: '진행 중',
    SUCCESS: '성공',
    FAIL: '실패',
    REJECTED: '반려',
    REWARD_REQUESTED: '보상 요청 중',
    REWARDED: '보상 완료',
};

const formatDate = (value) => {
    if (!value) return '-';
    return value.replace(/-/g, '.');
};

const ChallengeDetailScreen = ({ navigation, route }) => {
    const challengeId = route?.params?.challengeId;
    const [detail, setDetail] = useState(null);
    const [isLoading, setIsLoading] = useState(true);

    useFocusEffect(
        useCallback(() => {
            const fetchDetail = async () => {
                if (!challengeId) {
                    Alert.alert('오류', '챌린지 정보를 찾을 수 없습니다.');
                    navigation.goBack();
                    return;
                }

                setIsLoading(true);

                try {
                    const res = await api.get(`/challenges/spending/${challengeId}`);
                    setDetail(res.data?.data || null);
                } catch (error) {
                    console.error('Challenge detail fetch error', error);
                    Alert.alert('오류', error.response?.data?.message || '챌린지 상세 정보를 불러오지 못했습니다.');
                    navigation.goBack();
                } finally {
                    setIsLoading(false);
                }
            };

            fetchDetail();
        }, [challengeId, navigation])
    );

    return (
        <SafeAreaView style={styles.safeArea}>
            <View style={styles.header}>
                <TouchableOpacity style={styles.backButton} onPress={() => navigation.goBack()}>
                    <CustomText style={styles.backButtonText}>{'<'}</CustomText>
                </TouchableOpacity>
                <CustomText style={styles.headerTitle}>챌린지 상세</CustomText>
                <View style={styles.headerSpacer} />
            </View>

            <ScrollView contentContainerStyle={styles.container} showsVerticalScrollIndicator={false}>
                {isLoading ? (
                    <View style={styles.card}>
                        <CustomText style={styles.loadingText}>챌린지 정보를 불러오는 중입니다.</CustomText>
                    </View>
                ) : detail ? (
                    <>
                        <View style={styles.card}>
                            <CustomText style={styles.categoryText}>{detail.category}</CustomText>
                            <CustomText style={styles.statusText}>{STATUS_LABELS[detail.status] || detail.status}</CustomText>
                        </View>

                        <View style={styles.card}>
                            <View style={styles.infoRow}>
                                <CustomText style={styles.infoLabel}>목표 금액</CustomText>
                                <CustomText style={styles.infoValue}>{Number(detail.targetSpending || 0).toLocaleString()}원</CustomText>
                            </View>
                            <View style={styles.infoRow}>
                                <CustomText style={styles.infoLabel}>현재 소비 금액</CustomText>
                                <CustomText style={styles.infoValue}>{Number(detail.currentSpending || 0).toLocaleString()}원</CustomText>
                            </View>
                            <View style={styles.infoRow}>
                                <CustomText style={styles.infoLabel}>보상 금액</CustomText>
                                <CustomText style={styles.infoValue}>{Number(detail.rewardAmount || 0).toLocaleString()}원</CustomText>
                            </View>
                            <View style={styles.infoRow}>
                                <CustomText style={styles.infoLabel}>기간</CustomText>
                                <CustomText style={styles.infoValue}>{formatDate(detail.startDate)} - {formatDate(detail.endDate)}</CustomText>
                            </View>
                        </View>

                        {detail.parentMessage ? (
                            <View style={styles.card}>
                                <CustomText style={styles.messageTitle}>부모 메세지</CustomText>
                                <CustomText style={styles.messageText}>{detail.parentMessage}</CustomText>
                            </View>
                        ) : null}
                    </>
                ) : (
                    <View style={styles.card}>
                        <CustomText style={styles.loadingText}>표시할 챌린지 정보가 없습니다.</CustomText>
                    </View>
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
        backgroundColor: '#FFFFFF',
    },
    backButton: {
        width: scale(32),
        height: scale(32),
        justifyContent: 'center',
    },
    backButtonText: {
        fontSize: scale(22),
        fontWeight: 'bold',
        color: '#111',
    },
    headerTitle: {
        fontSize: scale(18),
        fontWeight: 'bold',
        color: '#111',
    },
    headerSpacer: { width: scale(32) },
    container: {
        flexGrow: 1,
        padding: scale(16),
    },
    card: {
        backgroundColor: '#FFFFFF',
        borderRadius: scale(16),
        padding: scale(18),
        marginBottom: verticalScale(14),
    },
    loadingText: {
        fontSize: scale(14),
        color: '#6B7280',
    },
    categoryText: {
        fontSize: scale(20),
        fontWeight: 'bold',
        color: '#111827',
        marginBottom: verticalScale(8),
    },
    statusText: {
        fontSize: scale(14),
        color: '#6B7280',
        fontWeight: '600',
    },
    infoRow: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        marginBottom: verticalScale(12),
    },
    infoLabel: {
        fontSize: scale(14),
        color: '#6B7280',
    },
    infoValue: {
        fontSize: scale(14),
        color: '#111827',
        fontWeight: '700',
    },
    messageTitle: {
        fontSize: scale(14),
        color: '#6B7280',
        marginBottom: verticalScale(8),
    },
    messageText: {
        fontSize: scale(14),
        color: '#374151',
        lineHeight: scale(20),
    },
});

export default ChallengeDetailScreen;
