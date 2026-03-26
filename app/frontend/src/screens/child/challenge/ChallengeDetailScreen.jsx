import React, { useCallback, useState } from 'react';
import { View, StyleSheet, SafeAreaView, TouchableOpacity, Alert, ScrollView, Image } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { scale, verticalScale } from 'react-native-size-matters';
import CustomText from '../../../components/common/CustomText';
import api from '../../../api/axios';

const CROCO_PARENTS_IMAGE = require('../../../assets/croco/croco_parents.png');
const PROGRESS_AVATAR_IMAGE = require('../../../assets/avatar/face/smile/boy-1-smile.png');

const formatDate = (value) => {
    if (!value) return '-';
    return value.replace(/-/g, '.');
};

const getDetailDdayLabel = (endDateValue) => {
    if (!endDateValue) return null;

    const today = new Date();
    const currentDate = new Date(today.getFullYear(), today.getMonth(), today.getDate());
    const endDate = new Date(`${endDateValue}T00:00:00`);
    const msPerDay = 24 * 60 * 60 * 1000;
    const diffDays = Math.ceil((endDate - currentDate) / msPerDay);

    return diffDays <= 0 ? 'D-DAY' : `D-${diffDays}`;
};

const getProgressRatio = (currentSpending, targetSpending) => {
    if (!targetSpending || targetSpending <= 0) return 0;
    return Math.min(Math.max(currentSpending / targetSpending, 0), 1);
};

const ChallengeDetailScreen = ({ navigation, route }) => {
    const challengeId = route?.params?.challengeId;
    const hideParentMessage = !!route?.params?.hideParentMessage;
    const hideDday = !!route?.params?.hideDday;
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
                        {(() => {
                            const progressRatio = getProgressRatio(detail.currentSpending || 0, detail.targetSpending || 0);
                            const progressPercent = Math.round(progressRatio * 100);
                            const isOverWarning = progressRatio >= 0.8;

                            return (
                                <>
                                    <View style={styles.card}>
                                        <View style={styles.detailHeader}>
                                            <CustomText style={styles.categoryText}>{detail.category}</CustomText>
                                            {!hideDday ? (
                                                <View style={styles.ddayBadge}>
                                                    <CustomText style={styles.ddayText}>{getDetailDdayLabel(detail.endDate)}</CustomText>
                                                </View>
                                            ) : null}
                                        </View>
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

                                    <View style={styles.progressCard}>
                                        <View style={styles.progressHeader}>
                                            <CustomText style={styles.progressTitle}>진행 상황</CustomText>
                                            <CustomText style={styles.progressPercent}>{progressPercent}% 사용</CustomText>
                                        </View>
                                        <View style={styles.progressTrackWrap}>
                                            <View style={styles.progressTrack}>
                                                <View
                                                    style={[
                                                        styles.progressFill,
                                                        isOverWarning ? styles.progressFillDanger : styles.progressFillSafe,
                                                        { width: `${progressRatio * 100}%` },
                                                    ]}
                                                />
                                            </View>
                                            {progressRatio > 0 ? (
                                                hideParentMessage ? (
                                                    <View
                                                        style={[
                                                            styles.progressDot,
                                                            isOverWarning ? styles.progressDotDanger : styles.progressDotSafe,
                                                            { left: `${Math.max(progressRatio * 100, 8)}%` },
                                                        ]}
                                                    />
                                                ) : (
                                                    <Image
                                                        source={PROGRESS_AVATAR_IMAGE}
                                                        style={[
                                                            styles.progressAvatar,
                                                            { left: `${Math.max(progressRatio * 100, 8)}%` },
                                                        ]}
                                                        resizeMode="contain"
                                                    />
                                                )
                                            ) : null}
                                        </View>
                                        <View style={styles.progressMetaRow}>
                                            <CustomText style={styles.progressMetaText}>
                                                {Number(detail.currentSpending || 0).toLocaleString()}원
                                            </CustomText>
                                            <CustomText style={[styles.progressMetaText, styles.progressMetaTextRight]}>
                                                {Number(detail.targetSpending || 0).toLocaleString()}원
                                            </CustomText>
                                        </View>
                                    </View>

                                    {!hideParentMessage && detail.parentMessage ? (
                                        <View style={styles.messageSection}>
                                            <View style={styles.messageBubbleWrap}>
                                                <View style={styles.messageBubbleTail} />
                                                <View style={styles.messageBubble}>
                                                    <CustomText style={styles.messageText}>{detail.parentMessage}</CustomText>
                                                </View>
                                            </View>
                                            <View style={styles.messageCharacterFrame}>
                                                <Image
                                                    source={CROCO_PARENTS_IMAGE}
                                                    style={styles.messageCroco}
                                                    resizeMode="cover"
                                                />
                                            </View>
                                        </View>
                                    ) : null}
                                </>
                            );
                        })()}
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
    },
    detailHeader: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'center',
        marginBottom: verticalScale(16),
    },
    ddayText: {
        fontSize: scale(12),
        color: '#6B7280',
        fontWeight: '700',
    },
    ddayBadge: {
        backgroundColor: '#F3F4F6',
        borderRadius: scale(999),
        paddingHorizontal: scale(10),
        paddingVertical: verticalScale(5),
        borderWidth: 1,
        borderColor: '#E5E7EB',
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
    progressCard: {
        backgroundColor: '#FFFFFF',
        borderRadius: scale(16),
        padding: scale(18),
        marginBottom: verticalScale(14),
    },
    progressHeader: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'center',
        marginBottom: verticalScale(12),
    },
    progressTitle: {
        fontSize: scale(15),
        fontWeight: '700',
        color: '#374151',
    },
    progressPercent: {
        fontSize: scale(13),
        fontWeight: '700',
        color: '#6B7280',
    },
    progressTrackWrap: {
        position: 'relative',
        marginBottom: verticalScale(10),
        paddingTop: verticalScale(10),
    },
    progressTrack: {
        height: verticalScale(10),
        backgroundColor: '#F3F4F6',
        borderRadius: scale(999),
        overflow: 'hidden',
    },
    progressFill: {
        height: '100%',
        borderRadius: scale(999),
    },
    progressFillSafe: {
        backgroundColor: '#84CC16',
    },
    progressFillDanger: {
        backgroundColor: '#EF4444',
    },
    progressAvatar: {
        position: 'absolute',
        top: 0,
        width: scale(66),
        height: scale(66),
        marginLeft: scale(-33),
    },
    progressDot: {
        position: 'absolute',
        top: verticalScale(8),
        width: scale(14),
        height: scale(14),
        borderRadius: scale(999),
        marginLeft: scale(-7),
        borderWidth: 2,
        borderColor: '#FFFFFF',
    },
    progressDotSafe: {
        backgroundColor: '#84CC16',
    },
    progressDotDanger: {
        backgroundColor: '#EF4444',
    },
    progressMetaRow: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        gap: scale(12),
    },
    progressMetaText: {
        flex: 1,
        fontSize: scale(12),
        color: '#6B7280',
    },
    progressMetaTextRight: {
        textAlign: 'right',
    },
    messageSection: {
        position: 'relative',
        marginBottom: verticalScale(14),
        paddingTop: verticalScale(6),
        paddingHorizontal: scale(4),
        minHeight: verticalScale(164),
    },
    messageCharacterFrame: {
        position: 'absolute',
        right: scale(0),
        bottom: verticalScale(-4),
        width: scale(130),
        height: scale(78),
        overflow: 'visible',
        zIndex: 1,
    },
    messageCroco: {
        width: scale(142),
        height: scale(142),
        position: 'absolute',
        left: scale(-10),
        bottom: verticalScale(-40),
    },
    messageBubbleWrap: {
        position: 'absolute',
        top: verticalScale(8),
        left: scale(0),
        right: scale(44),
        zIndex: 2,
    },
    messageBubbleTail: {
        position: 'absolute',
        right: scale(26),
        bottom: scale(-8),
        width: scale(18),
        height: scale(18),
        backgroundColor: '#FFFFFF',
        borderRightWidth: 1,
        borderBottomWidth: 1,
        borderColor: '#E5E7EB',
        borderBottomRightRadius: scale(3),
        transform: [{ rotate: '45deg' }],
    },
    messageBubble: {
        backgroundColor: '#FFFFFF',
        borderRadius: scale(22),
        paddingHorizontal: scale(18),
        paddingVertical: verticalScale(15),
        minHeight: verticalScale(74),
        justifyContent: 'center',
        borderWidth: 1,
        borderColor: '#E5E7EB',
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 2 },
        shadowOpacity: 0.04,
        shadowRadius: 8,
        elevation: 2,
    },
    messageText: {
        fontSize: scale(14),
        color: '#374151',
        lineHeight: scale(20),
        flexShrink: 1,
    },
});

export default ChallengeDetailScreen;
