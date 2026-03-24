import React, { useState } from 'react';
import { View, StyleSheet, TouchableOpacity, ScrollView, SafeAreaView, Alert } from 'react-native';
import { scale, verticalScale } from 'react-native-size-matters';
import CustomText from '../../../components/common/CustomText';
import CustomTextInput from '../../../components/common/CustomTextInput';
import api from '../../../api/axios';

const MissionApprovalScreen = ({ navigation }) => {
    // 더미 미션 제안 데이터
    const [mission, setMission] = useState({
        childName: '김싸피',
        category: '간식',
        goalAmount: 3000,
        memo: '이만큼 아껴서 사고 싶은 게 있어요!',
        status: 'PENDING'
    });

    const [parentComment, setParentComment] = useState('');
    const [rewardAmount, setRewardAmount] = useState('');

    const handleApprove = () => {
        if (!parentComment.trim()) {
            Alert.alert('알림', '승인 메시지를 작성해주세요. 아이가 검토 의견을 기다립니다!');
            return;
        }
        if (!rewardAmount || isNaN(rewardAmount)) {
            Alert.alert('알림', '성공 시 지급할 보상 금액을 입력해주세요!');
            return;
        }

        /* ==========================================
           [진짜 미션 승인 API]
           ========================================== 
        try {
            // await api.post(`/challenges/${mission.id}/approve`, { 
            //     comment: parentComment,
            //     rewardAmount: parseInt(rewardAmount)
            // });
            // Alert.alert('승인 완료', `${mission.childName}의 챌린지에 ${rewardAmount}원 보상을 걸고 승인했습니다!`, [{ text: '확인', onPress: () => navigation.goBack() }]);
            // return;
        } catch(e) { console.error('Mission Approve Error', e); }
        ========================================== */

        Alert.alert('승인 완료', `${mission.childName}의 챌린지에 ${rewardAmount.toLocaleString()}원 보상을 걸고 승인했습니다!`, [
            { text: '확인', onPress: () => navigation.goBack() }
        ]);
    };

    const handleReject = () => {
        if (!parentComment.trim()) {
            Alert.alert('알림', '거절 사유를 작성해주세요. 아이가 검토 의견을 기다립니다!');
            return;
        }

        /* ==========================================
           [진짜 미션 거절 API]
           ========================================== 
        try {
            // await api.post(`/challenges/${mission.id}/reject`, { reason: parentComment });
            // Alert.alert('거절 완료', '챌린지를 거절했습니다.', [{ text: '확인', onPress: () => navigation.goBack() }]);
            // return;
        } catch(e) { console.error('Mission Reject Error', e); }
        ========================================== */

        Alert.alert('거절 완료', '챌린지를 거절했습니다.', [
            { text: '확인', onPress: () => navigation.goBack() }
        ]);
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
                            <CustomText style={styles.categoryText}>{mission.category}</CustomText>
                        </View>
                    </View>

                    <View style={styles.infoRow}>
                        <CustomText style={styles.infoLabel}>억제 목표 금액</CustomText>
                        <CustomText style={styles.amountText}>{mission.goalAmount.toLocaleString()} 원</CustomText>
                    </View>

                    <View style={styles.memoBox}>
                        <CustomText style={styles.memoLabel}>아이의 한마디</CustomText>
                        <CustomText style={styles.memoText}>"{mission.memo}"</CustomText>
                    </View>
                </View>

                <View style={styles.rewardInputContainer}>
                    <CustomText style={styles.commentInputLabel}>챌린지 성공 보상금 (필수)</CustomText>
                    <View style={styles.rewardInputWrapper}>
                        <CustomTextInput
                            style={styles.rewardInput}
                            placeholder="예: 3000"
                            placeholderTextColor="#9CA3AF"
                            keyboardType="numeric"
                            value={rewardAmount}
                            onChangeText={setRewardAmount}
                        />
                        <CustomText style={styles.currencyText}>원</CustomText>
                    </View>
                </View>

                <View style={styles.commentInputContainer}>
                    <CustomText style={styles.commentInputLabel}>부모님 검토 코멘트 (필수)</CustomText>
                    <CustomTextInput
                        style={styles.commentInput}
                        placeholder="승인/거절 이유와 함께 아이에게 남길 말을 적어주세요."
                        placeholderTextColor="#9CA3AF"
                        value={parentComment}
                        onChangeText={setParentComment}
                        multiline
                    />
                </View>

                <View style={styles.actionRow}>
                    <TouchableOpacity style={[styles.actionBtn, styles.rejectBtn]} onPress={handleReject}>
                        <CustomText style={styles.rejectBtnText}>거절하기</CustomText>
                    </TouchableOpacity>
                    <TouchableOpacity style={[styles.actionBtn, styles.approveBtn]} onPress={handleApprove}>
                        <CustomText style={styles.approveBtnText}>승인하기</CustomText>
                    </TouchableOpacity>
                </View>

            </ScrollView>
        </SafeAreaView>
    );
};

const styles = StyleSheet.create({
    safeArea: { flex: 1, backgroundColor: '#F3F4F6' },
    header: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingHorizontal: scale(16), paddingVertical: verticalScale(16), backgroundColor: '#F3F4F6' },
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

    memoBox: { backgroundColor: '#F9FAFB', borderRadius: scale(12), padding: scale(16), marginTop: verticalScale(8) },
    memoLabel: { fontSize: scale(12), color: '#9CA3AF', marginBottom: verticalScale(8) },
    memoText: { fontSize: scale(14), color: '#111', fontStyle: 'italic', lineHeight: 20 },

    rewardInputContainer: { marginBottom: verticalScale(20) },
    rewardInputWrapper: { flexDirection: 'row', alignItems: 'center', backgroundColor: '#FFFFFF', borderWidth: 1, borderColor: '#E5E7EB', borderRadius: scale(12), paddingHorizontal: scale(16) },
    rewardInput: { flex: 1, fontSize: scale(16), fontWeight: 'bold', color: '#111', paddingVertical: verticalScale(14) },
    currencyText: { fontSize: scale(16), fontWeight: 'bold', color: '#111' },

    commentInputContainer: { marginBottom: verticalScale(32) },
    commentInputLabel: { fontSize: scale(14), fontWeight: 'bold', color: '#4B5563', marginBottom: verticalScale(8) },
    commentInput: { backgroundColor: '#FFFFFF', borderWidth: 1, borderColor: '#E5E7EB', borderRadius: scale(12), paddingHorizontal: scale(16), paddingVertical: verticalScale(14), fontSize: scale(14), color: '#111', minHeight: verticalScale(80), textAlignVertical: 'top' },

    actionRow: { flexDirection: 'row', gap: scale(12) },
    actionBtn: { flex: 1, paddingVertical: verticalScale(16), borderRadius: scale(12), alignItems: 'center' },
    rejectBtn: { backgroundColor: '#F3F4F6', borderWidth: 1, borderColor: '#E5E7EB' },
    rejectBtnText: { fontSize: scale(16), fontWeight: 'bold', color: '#4B5563' },
    approveBtn: { backgroundColor: '#A3E635' },
    approveBtnText: { fontSize: scale(16), fontWeight: 'bold', color: '#111' }
});

export default MissionApprovalScreen;
