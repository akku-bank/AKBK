import React, { useState } from 'react';
import { View, StyleSheet, TouchableOpacity, SafeAreaView, Switch, Alert, Modal, TextInput } from 'react-native';
import { scale, verticalScale } from 'react-native-size-matters';
import CustomText from '../../../components/common/CustomText';
import api from '../../../api/axios';

const FALLBACK_DETAIL = {
    id: '1',
    date: '2024.03.12 14:30',
    title: '다이소 강남점',
    amount: -5000,
    type: 'TRANSFER',
    category: '저축',
    memo: '내 적금통장으로 이체',
    is_self_transfer: true,
};

const TransactionDetailScreen = ({ route, navigation }) => {
    const { transaction } = route?.params || {};
    const detailData = transaction || FALLBACK_DETAIL;

    const [isPrivate, setIsPrivate] = useState(detailData.isHidden || false);
    const [isMemoModalVisible, setMemoModalVisible] = useState(false);
    const [memo, setMemo] = useState(detailData.memo || '');
    const [tempMemo, setTempMemo] = useState('');

    const isOver14 = true;
    const isDeposit = detailData.amount > 0;
    const isSelfTransfer = detailData.is_self_transfer;

    const displayTitle = detailData.place || '결제 내역';
    const displayAmount = isSelfTransfer
        ? `${detailData.amount.toLocaleString()}원`
        : `${isDeposit ? '+' : ''}${detailData.amount.toLocaleString()}원`;

    const handleTogglePrivacy = async (value) => {
        try {
            await api.patch(`/bank/transactions/${detailData.id}/visibility`, { isHidden: value });
            setIsPrivate(value);
        } catch (e) {
            console.error('Privacy Toggle Error:', e);
            Alert.alert('오류', '프라이버시 설정을 변경하지 못했습니다.');
        }
    };

    const handleSaveMemo = async () => {
        try {
            await api.patch(`/bank/transactions/${detailData.id}/memo`, { memo: tempMemo });
            setMemo(tempMemo);
            setMemoModalVisible(false);
            Alert.alert('알림', '메모가 저장되었습니다.');
        } catch (e) {
            console.error('Memo Save Error:', e);
            Alert.alert('오류', '메모를 저장하지 못했습니다.');
        }
    };

    const openMemoModal = () => {
        setTempMemo(memo);
        setMemoModalVisible(true);
    };

    return (
        <SafeAreaView style={styles.safeArea}>
            <View style={styles.header}>
                <TouchableOpacity style={styles.backButton} onPress={() => navigation.goBack()}>
                    <CustomText style={styles.backButtonText}>←</CustomText>
                </TouchableOpacity>
                <CustomText style={styles.headerTitle}>거래 상세</CustomText>
                <View style={{ width: 40 }} />
            </View>

            <View style={styles.container}>
                <View style={[styles.detailCard, isSelfTransfer && styles.selfTransferCard]}>
                    <View style={styles.iconWrapper}>
                        <CustomText style={styles.mainIcon}>{isDeposit ? '💰' : (isSelfTransfer ? '🔄' : '🏪')}</CustomText>
                    </View>

                    <CustomText style={[styles.detailTitle, isSelfTransfer && styles.selfTransferText]}>{displayTitle}</CustomText>
                    <CustomText style={[styles.detailAmount, isDeposit ? styles.depositColor : (isSelfTransfer ? styles.selfTransferColor : styles.withdrawColor)]}>
                        {displayAmount}
                    </CustomText>

                    <View style={styles.divider} />

                    <View style={styles.infoRow}>
                        <CustomText style={styles.infoLabel}>일시</CustomText>
                        <CustomText style={styles.infoValue}>{detailData.date || detailData.time}</CustomText>
                    </View>

                    <View style={styles.infoRow}>
                        <CustomText style={styles.infoLabel}>카테고리</CustomText>
                        <CustomText style={styles.infoValue}>{detailData.category || '기차'}</CustomText>
                    </View>

                    <View style={styles.infoRow}>
                        <CustomText style={styles.infoLabel}>메모</CustomText>
                        <CustomText style={styles.infoValue}>{memo || '-'}</CustomText>
                    </View>
                </View>

                {isOver14 && (
                    <View style={styles.privacyCard}>
                        <View style={styles.privacyTextContent}>
                            <CustomText style={styles.privacyTitle}>부모님께 내역 숨기기</CustomText>
                            <CustomText style={styles.privacySubtitle}>이 결제 내역을 부모님이 볼 수 없게 합니다.</CustomText>
                        </View>
                        <Switch
                            value={isPrivate}
                            onValueChange={handleTogglePrivacy}
                            trackColor={{ false: '#E5E7EB', true: '#10B981' }}
                            thumbColor="#FFFFFF"
                        />
                    </View>
                )}

                <TouchableOpacity style={styles.memoButton} onPress={openMemoModal}>
                    <CustomText style={styles.memoButtonText}>{memo ? '메모 수정하기' : '메모 남기기'}</CustomText>
                </TouchableOpacity>
            </View>

            <Modal
                visible={isMemoModalVisible}
                transparent={true}
                animationType="fade"
                onRequestClose={() => setMemoModalVisible(false)}
            >
                <View style={styles.modalOverlay}>
                    <View style={styles.modalContent}>
                        <CustomText style={styles.modalTitle}>메모 남기기</CustomText>
                        <TextInput
                            style={styles.memoInput}
                            placeholder="이 거래에 대한 메모를 남겨주세요."
                            placeholderTextColor="#9CA3AF"
                            multiline
                            maxLength={255}
                            value={tempMemo}
                            onChangeText={setTempMemo}
                            autoFocus
                        />
                        <View style={styles.modalButtonRow}>
                            <TouchableOpacity style={[styles.modalButton, styles.cancelButton]} onPress={() => setMemoModalVisible(false)}>
                                <CustomText style={styles.cancelButtonText}>취소</CustomText>
                            </TouchableOpacity>
                            <TouchableOpacity style={[styles.modalButton, styles.saveButton]} onPress={handleSaveMemo}>
                                <CustomText style={styles.saveButtonText}>저장</CustomText>
                            </TouchableOpacity>
                        </View>
                    </View>
                </View>
            </Modal>
        </SafeAreaView>
    );
};

const styles = StyleSheet.create({
    safeArea: {
        flex: 1,
        backgroundColor: '#F3F4F6',
    },
    header: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        paddingHorizontal: scale(16),
        paddingVertical: verticalScale(16),
        backgroundColor: '#F3F4F6',
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
    container: {
        flex: 1,
        paddingHorizontal: scale(16),
        paddingTop: verticalScale(20),
    },
    detailCard: {
        backgroundColor: '#FFFFFF',
        borderRadius: scale(20),
        padding: scale(24),
        alignItems: 'center',
        marginBottom: verticalScale(16),
        shadowColor: '#000',
        shadowOffset: { width: 0, height: verticalScale(2) },
        shadowOpacity: 0.05,
        shadowRadius: scale(8),
        elevation: 2,
    },
    selfTransferCard: {
        backgroundColor: '#F9FAFB',
        borderWidth: 1,
        borderColor: '#E5E7EB',
    },
    iconWrapper: {
        width: scale(64),
        height: scale(64),
        borderRadius: scale(32),
        backgroundColor: '#F3F4F6',
        justifyContent: 'center',
        alignItems: 'center',
        marginBottom: verticalScale(16),
    },
    mainIcon: {
        fontSize: scale(32),
    },
    detailTitle: {
        fontSize: scale(18),
        fontWeight: '600',
        color: '#111',
        marginBottom: verticalScale(8),
    },
    selfTransferText: {
        color: '#6B7280',
    },
    detailAmount: {
        fontSize: scale(32),
        fontWeight: 'bold',
        marginBottom: verticalScale(24),
    },
    depositColor: {
        color: '#3B82F6',
    },
    withdrawColor: {
        color: '#111',
    },
    selfTransferColor: {
        color: '#9CA3AF',
    },
    divider: {
        width: '100%',
        height: 1,
        backgroundColor: '#F3F4F6',
        marginBottom: verticalScale(20),
    },
    infoRow: {
        flexDirection: 'row',
        width: '100%',
        justifyContent: 'space-between',
        marginBottom: verticalScale(16),
    },
    infoLabel: {
        fontSize: scale(14),
        color: '#6B7280',
        fontWeight: '500',
    },
    infoValue: {
        fontSize: scale(14),
        color: '#111',
        fontWeight: '600',
    },
    privacyCard: {
        flexDirection: 'row',
        backgroundColor: '#FFFFFF',
        borderRadius: scale(16),
        padding: scale(20),
        alignItems: 'center',
        justifyContent: 'space-between',
        marginBottom: verticalScale(16),
        shadowColor: '#000',
        shadowOffset: { width: 0, height: verticalScale(2) },
        shadowOpacity: 0.05,
        shadowRadius: scale(8),
        elevation: 2,
    },
    privacyTextContent: {
        flex: 1,
    },
    privacyTitle: {
        fontSize: scale(15),
        fontWeight: 'bold',
        color: '#111',
        marginBottom: verticalScale(4),
    },
    privacySubtitle: {
        fontSize: scale(12),
        color: '#6B7280',
    },
    memoButton: {
        backgroundColor: '#E5E7EB',
        borderRadius: scale(12),
        paddingVertical: verticalScale(14),
        alignItems: 'center',
    },
    memoButtonText: {
        fontSize: scale(15),
        fontWeight: 'bold',
        color: '#4B5563',
    },
    // 모달 관련 스타일
    modalOverlay: {
        flex: 1,
        backgroundColor: 'rgba(0,0,0,0.5)',
        justifyContent: 'center',
        alignItems: 'center',
        padding: scale(20),
    },
    modalContent: {
        width: '100%',
        backgroundColor: '#FFFFFF',
        borderRadius: scale(20),
        padding: scale(24),
    },
    modalTitle: {
        fontSize: scale(18),
        fontWeight: 'bold',
        color: '#111',
        marginBottom: verticalScale(16),
    },
    memoInput: {
        backgroundColor: '#F3F4F6',
        borderRadius: scale(12),
        padding: scale(16),
        height: verticalScale(100),
        textAlignVertical: 'top',
        fontSize: scale(14),
        color: '#111',
        marginBottom: verticalScale(20),
    },
    modalButtonRow: {
        flexDirection: 'row',
        justifyContent: 'flex-end',
    },
    modalButton: {
        paddingVertical: verticalScale(10),
        paddingHorizontal: scale(20),
        borderRadius: scale(8),
        marginLeft: scale(10),
    },
    cancelButton: {
        backgroundColor: '#F3F4F6',
    },
    saveButton: {
        backgroundColor: '#A3E635',
    },
    cancelButtonText: {
        color: '#6B7280',
        fontWeight: '600',
    },
    saveButtonText: {
        color: '#FFFFFF',
        fontWeight: 'bold',
    }
});

export default TransactionDetailScreen;
