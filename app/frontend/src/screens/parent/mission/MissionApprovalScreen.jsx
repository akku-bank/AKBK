import React, { useMemo, useState } from 'react';
import { View, StyleSheet, TouchableOpacity, ScrollView, SafeAreaView, Alert } from 'react-native';
import { scale, verticalScale } from 'react-native-size-matters';
import CustomText from '../../../components/common/CustomText';
import CustomTextInput from '../../../components/common/CustomTextInput';
import api from '../../../api/axios';

const MissionApprovalScreen = ({ navigation, route }) => {
    const challenge = route?.params?.challenge;
    const childName = route?.params?.childName || '자녀';
    const [parentComment, setParentComment] = useState('');
    const [isSubmitting, setIsSubmitting] = useState(false);

    const mission = useMemo(() => ({
        challengeId: challenge?.challengeId || '',
        childName,
        category: challenge?.category || '',
        targetSpending: Number(challenge?.targetSpending || 0),
        rewardAmount: Number(challenge?.rewardAmount || 0),
        status: challenge?.status || 'PENDING',
    }), [challenge, childName]);

    const updateStatus = async (status) => {
        if (!mission.challengeId) {
            Alert.alert('오류', '챌린지 정보가 없습니다.');
            return;
        }

        if (!parentComment.trim()) {
            Alert.alert('알림', status === 'APPROVED' ? '승인 메시지를 작성해주세요.' : '거절 사유를 작성해주세요.');
            return;
        }

        try {
            setIsSubmitting(true);

            await api.patch(`/challenges/spending/${mission.challengeId}/status`, {
                status,
                parentMessage: parentComment.trim(),
            });

            Alert.alert(
                status === 'APPROVED' ? '승인 완료' : '거절 완료',
                status === 'APPROVED'
                    ? `${mission.childName}의 챌린지를 승인했습니다.`
                    : '챌린지를 거절했습니다.',
                [{ text: '확인', onPress: () => navigation.goBack() }]
            );
        } catch (e) {
            console.error('Mission status update error', e);
            Alert.alert('오류', status === 'APPROVED' ? '챌린지 승인에 실패했습니다.' : '챌린지 거절에 실패했습니다.');
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <SafeAreaView style={styles.safeArea}>
            <View style={styles.header}>
                <TouchableOpacity style={styles.backButton} onPress={() => navigation.goBack()}>
                    <CustomText style={styles.backButtonText}>←</CustomText>
                </TouchableOpacity>
                <CustomText style={styles.headerTitle}>용돈 미션 검토</CustomText>
                <View style={{ width: scale(32) }} />
            </View>

            <ScrollView contentContainerStyle={styles.container}>
                <View style={styles.card}>
                    <CustomText style={styles.childName}>{mission.childName}의 제안</CustomText>

                    <View style={styles.infoRow}>
                        <CustomText style={styles.infoLabel}>소비 카테고리</CustomText>
                        <View style={styles.categoryBadge}>
                            <CustomText style={styles.categoryText}>{mission.category || '-'}</CustomText>
                        </View>
                    </View>

                    <View style={styles.infoRow}>
                        <CustomText style={styles.infoLabel}>억제 목표 금액</CustomText>
                        <CustomText style={styles.amountText}>{mission.targetSpending.toLocaleString()} 원</CustomText>
                    </View>

                    <View style={styles.infoRow}>
                        <CustomText style={styles.infoLabel}>보상 금액</CustomText>
                        <CustomText style={styles.amountText}>{mission.rewardAmount.toLocaleString()} 원</CustomText>
                    </View>
                </View>

                <View style={styles.commentInputContainer}>
                    <CustomText style={styles.commentInputLabel}>부모님 검토 코멘트 (필수)</CustomText>
                    <CustomTextInput
                        style={styles.commentInput}
                        placeholder="승인 또는 거절 이유를 적어주세요."
                        placeholderTextColor="#9CA3AF"
                        value={parentComment}
                        onChangeText={setParentComment}
                        multiline
                        editable={!isSubmitting}
                    />
                </View>

                <View style={styles.actionRow}>
                    <TouchableOpacity
                        style={[styles.actionBtn, styles.rejectBtn, isSubmitting && styles.disabledBtn]}
                        onPress={() => updateStatus('REJECTED')}
                        disabled={isSubmitting}
                    >
                        <CustomText style={styles.rejectBtnText}>거절하기</CustomText>
                    </TouchableOpacity>
                    <TouchableOpacity
                        style={[styles.actionBtn, styles.approveBtn, isSubmitting && styles.disabledBtn]}
                        onPress={() => updateStatus('APPROVED')}
                        disabled={isSubmitting}
                    >
                        <CustomText style={styles.approveBtnText}>승인하기</CustomText>
                    </TouchableOpacity>
                </View>
            </ScrollView>
        </SafeAreaView>
    );
};

const styles = StyleSheet.create({
    safeArea: { flex: 1, backgroundColor: '#F9FAFB' },
    header: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingHorizontal: scale(16), paddingVertical: verticalScale(16), backgroundColor: '#F9FAFB' },
    backButton: { width: scale(32), height: scale(32), justifyContent: 'center' },
    backButtonText: { fontSize: scale(22), fontWeight: 'bold', color: '#111' },
    headerTitle: { fontSize: scale(18), fontWeight: 'bold', color: '#111' },
    container: { flexGrow: 1, paddingHorizontal: scale(16), paddingTop: verticalScale(20), paddingBottom: verticalScale(40) },

    card: { backgroundColor: '#FFFFFF', borderRadius: scale(16), padding: scale(20), marginBottom: verticalScale(24), shadowColor: '#000', shadowOffset: { width: 0, height: verticalScale(2) }, shadowOpacity: 0.05, shadowRadius: scale(8), elevation: 2 },
    childName: { fontSize: scale(18), fontWeight: 'bold', color: '#111', marginBottom: verticalScale(20) },

    infoRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: verticalScale(16) },
    infoLabel: { fontSize: scale(14), color: '#6B7280' },
    categoryBadge: { backgroundColor: '#A3E635', paddingHorizontal: scale(12), paddingVertical: verticalScale(4), borderRadius: scale(12) },
    categoryText: { fontSize: scale(14), fontWeight: 'bold', color: '#111' },
    amountText: { fontSize: scale(18), fontWeight: 'bold', color: '#111' },

    commentInputContainer: { marginBottom: verticalScale(32) },
    commentInputLabel: { fontSize: scale(14), fontWeight: 'bold', color: '#4B5563', marginBottom: verticalScale(8) },
    commentInput: { backgroundColor: '#FFFFFF', borderWidth: 1, borderColor: '#E5E7EB', borderRadius: scale(12), paddingHorizontal: scale(16), paddingVertical: verticalScale(14), fontSize: scale(14), color: '#111', minHeight: verticalScale(100), textAlignVertical: 'top' },

    actionRow: { flexDirection: 'row', gap: scale(12) },
    actionBtn: { flex: 1, paddingVertical: verticalScale(16), borderRadius: scale(12), alignItems: 'center' },
    rejectBtn: { backgroundColor: '#F9FAFB', borderWidth: 1, borderColor: '#E5E7EB' },
    rejectBtnText: { fontSize: scale(16), fontWeight: 'bold', color: '#4B5563' },
    approveBtn: { backgroundColor: '#A3E635' },
    approveBtnText: { fontSize: scale(16), fontWeight: 'bold', color: '#111' },
    disabledBtn: { opacity: 0.6 }
});

export default MissionApprovalScreen;
