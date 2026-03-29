import React, { useState, useEffect, useCallback } from 'react';
import { View, StyleSheet, TouchableOpacity, SafeAreaView, ScrollView, Alert, Modal, Image, Dimensions, Animated, Platform, DeviceEventEmitter } from 'react-native';
import { scale, verticalScale } from 'react-native-size-matters';
import CustomText from '../../../components/common/CustomText';
import CustomTextInput from '../../../components/common/CustomTextInput';
import api from '../../../api/axios';
import { useFocusEffect } from '@react-navigation/native';

const { width, height } = Dimensions.get('window');

const BANKS = [
    { code: '999', name: '싸피은행', color: '#A3E635' },
    { code: '001', name: '한국은행', color: '#3B82F6' },
    { code: '004', name: '국민은행', color: '#FFD700' },
    { code: '020', name: '우리은행', color: '#0057A0' },
    { code: '088', name: '신한은행', color: '#0046FF' },
    { code: '011', name: '농협은행', color: '#00A651' },
    { code: '090', name: '카카오뱅크', color: '#FEE500' },
];

const TransferScreen = ({ navigation }) => {
    // 스테이트 관리
    const [step, setStep] = useState('RECIPIENT'); // RECIPIENT, AMOUNT, PIN
    const [isConfirmVisible, setIsConfirmVisible] = useState(false);

    const [targetAccountNumber, setTargetAccountNumber] = useState('');
    const [targetBank, setTargetBank] = useState(BANKS[0]);
    const [targetName, setTargetName] = useState('');
    const [amount, setAmount] = useState('');
    const [pin, setPin] = useState('');

    const [myAccount, setMyAccount] = useState(null);
    const [isBankModalVisible, setIsBankModalVisible] = useState(false);

    // 내 계좌 정보 로드
    const fetchMyAccount = useCallback(() => {
        api.get('/bank/accounts/me').then(res => {
            if (res.data?.data) {
                const accs = res.data.data.accounts || [];
                if (accs.length > 0) setMyAccount(accs[0]);
                else setMyAccount(res.data.data);
            }
        }).catch(err => console.error('Account Load Error', err));
    }, []);

    useFocusEffect(
        useCallback(() => {
            fetchMyAccount();
        }, [fetchMyAccount])
    );

    useEffect(() => {
        const subscription = DeviceEventEmitter.addListener('refresh_transactions', fetchMyAccount);
        return () => subscription.remove();
    }, [fetchMyAccount]);

    const handleNextStep = async () => {
        if (step === 'RECIPIENT') {
            if (!targetAccountNumber.trim()) {
                Alert.alert('알림', '계좌번호를 입력해주세요.');
                return;
            }

            try {
                // 계좌 실명 조회 API 호출
                const res = await api.get('/bank/accounts/holder-name', {
                    params: {
                        bankCode: targetBank.code,
                        accountNumber: targetAccountNumber
                    }
                });

                if (res.data?.success) {
                    setTargetName(res.data.data.userName);
                    setStep('AMOUNT');
                } else {
                    Alert.alert('조회 실패', '입력하신 계좌 정보를 찾을 수 없습니다.');
                }
            } catch (e) {
                console.error('Account Verify Error', e);
                Alert.alert('조회 실패', '계좌 정보를 확인할 수 없습니다. 다시 시도해주세요.');
            }
        } else if (step === 'AMOUNT') {
            if (!amount || parseInt(amount) <= 0) {
                Alert.alert('알림', '송금할 금액을 입력해주세요.');
                return;
            }
            if (myAccount && parseInt(amount) > myAccount.balance) {
                Alert.alert('잔액 부족', '내 지갑의 잔액이 부족합니다.');
                return;
            }
            setIsConfirmVisible(true);
        }
    };

    const handleConfirmTransfer = () => {
        setIsConfirmVisible(false);
        setStep('PIN');
    };

    const handlePinPress = (val) => {
        if (val === 'delete') {
            setPin(prev => prev.slice(0, -1));
        } else if (pin.length < 6) {
            const newPin = pin + val;
            setPin(newPin);
            if (newPin.length === 6) {
                submitTransfer(newPin);
            }
        }
    };

    const submitTransfer = async (finalPin) => {
        try {
            await api.post('/bank/accounts/transfers', {
                withdrawalAccountId: myAccount.accountId,
                targetBankCode: targetBank.code,
                targetAccountNumber: targetAccountNumber,
                targetName: targetName,
                amount: parseInt(amount),
                pin: finalPin
            });

            // 송금 성공 후 즉시 내역 갱신 이벤트 발행
            DeviceEventEmitter.emit('refresh_transactions');

            Alert.alert('송금 완료', `${targetName}님께 ${parseInt(amount).toLocaleString()}원을 보냈어요!`, [
                { text: '확인', onPress: () => navigation.goBack() }
            ]);
        } catch (e) {
            console.error('Transfer Error', e.response?.data || e.message);
            Alert.alert('송금 실패', e.response?.data?.message || '비밀번호가 틀렸거나 오류가 발생했습니다.');
            setPin('');
        }
    };

    // 렌더링 헬퍼
    const renderKeypad = () => {
        const keys = ['1', '2', '3', '4', '5', '6', '7', '8', '9', '', '0', 'delete'];
        return (
            <View style={styles.keypad}>
                {keys.map((key, i) => (
                    <TouchableOpacity
                        key={i}
                        style={styles.key}
                        onPress={() => key !== '' && handlePinPress(key)}
                        disabled={key === ''}
                    >
                        {key === 'delete' ? (
                            <CustomText style={styles.keyText}>←</CustomText>
                        ) : (
                            <CustomText style={styles.keyText}>{key}</CustomText>
                        )}
                    </TouchableOpacity>
                ))}
            </View>
        );
    };

    const handleBack = () => {
        if (step === 'AMOUNT') setStep('RECIPIENT');
        else if (step === 'PIN') setStep('AMOUNT');
        else navigation.goBack();
    };

    return (
        <SafeAreaView style={styles.safeArea}>
            {/* 헤더 */}
            <View style={styles.header}>
                <TouchableOpacity onPress={handleBack} style={styles.backButton}>
                    <CustomText style={styles.backButtonText}>
                        {step === 'RECIPIENT' ? '✕' : '←'}
                    </CustomText>
                </TouchableOpacity>
                <CustomText style={styles.headerTitle}>송금</CustomText>
                <View style={{ width: 40 }} />
            </View>

            {step !== 'PIN' && (
                <ScrollView contentContainerStyle={styles.container}>
                    {step === 'RECIPIENT' && (
                        <View style={styles.stepContent}>
                            <CustomText style={styles.stepTitle}>어디로 보낼까요?</CustomText>

                            <TouchableOpacity
                                style={styles.bankSelect}
                                onPress={() => setIsBankModalVisible(true)}
                            >
                                <View style={[styles.bankCircle, { backgroundColor: targetBank.color }]} />
                                <CustomText style={styles.bankName}>{targetBank.name}</CustomText>
                                <CustomText style={styles.chevron}>▽</CustomText>
                            </TouchableOpacity>

                            <CustomTextInput
                                style={styles.accountInput}
                                placeholder="계좌번호 입력"
                                placeholderTextColor="#9CA3AF"
                                keyboardType="numeric"
                                value={targetAccountNumber}
                                onChangeText={setTargetAccountNumber}
                            />
                        </View>
                    )}

                    {step === 'AMOUNT' && (
                        <View style={styles.stepContent}>
                            <CustomText style={styles.recipientBadge}>
                                {targetBank.name} {targetAccountNumber}
                            </CustomText>
                            <CustomText style={styles.stepTitle}>{targetName}님에게{'\n'}얼마나 보낼까요?</CustomText>

                            <View style={styles.amountContainer}>
                                <CustomTextInput
                                    style={styles.amountInput}
                                    placeholder="0"
                                    placeholderTextColor="#D1D5DB"
                                    keyboardType="numeric"
                                    autoFocus
                                    value={amount}
                                    onChangeText={setAmount}
                                />
                                <CustomText style={styles.currencyText}>원</CustomText>
                            </View>

                            <View style={styles.balanceInfo}>
                                <CustomText style={styles.balanceLabel}>내 지갑 잔액:</CustomText>
                                <CustomText style={styles.balanceValue}>
                                    {myAccount?.balance?.toLocaleString() || 0}원
                                </CustomText>
                            </View>
                        </View>
                    )}

                    {/* 하단 버튼을 ScrollView 안으로 이동 */}
                    <TouchableOpacity style={[styles.nextButton, { marginTop: 20 }]} onPress={handleNextStep}>
                        <CustomText style={styles.nextButtonText}>다음</CustomText>
                    </TouchableOpacity>
                </ScrollView>
            )}

            {step === 'PIN' && (
                <View style={styles.pinStep}>
                    <View style={styles.pinHeader}>
                        <CustomText style={styles.pinTitle}>결제 비밀번호를 입력해주세요</CustomText>
                        <View style={styles.pinDots}>
                            {[...Array(6)].map((_, i) => (
                                <View key={i} style={[styles.dot, pin.length > i && styles.dotFilled]} />
                            ))}
                        </View>
                    </View>
                    {renderKeypad()}
                </View>
            )}

            {/* 은행 선택 모달 */}
            <Modal visible={isBankModalVisible} transparent animationType="slide">
                <View style={styles.modalOverlay}>
                    <View style={styles.bankModal}>
                        <View style={styles.modalHeader}>
                            <CustomText style={styles.modalTitle}>은행 선택</CustomText>
                            <TouchableOpacity onPress={() => setIsBankModalVisible(false)}>
                                <CustomText style={styles.modalClose}>✕</CustomText>
                            </TouchableOpacity>
                        </View>
                        <ScrollView contentContainerStyle={styles.bankGrid}>
                            {BANKS.map((b) => (
                                <TouchableOpacity
                                    key={b.code}
                                    style={styles.bankItem}
                                    onPress={() => {
                                        setTargetBank(b);
                                        setIsBankModalVisible(false);
                                    }}
                                >
                                    <View style={[styles.bankItemIcon, { backgroundColor: b.color }]} />
                                    <CustomText style={styles.bankItemName}>{b.name}</CustomText>
                                </TouchableOpacity>
                            ))}
                        </ScrollView>
                    </View>
                </View>
            </Modal>

            {/* 확인 모달 */}
            <Modal visible={isConfirmVisible} transparent animationType="fade">
                <View style={styles.modalOverlay}>
                    <View style={styles.confirmModal}>
                        <CustomText style={styles.confirmTitle}>송금 확인</CustomText>
                        <CustomText style={styles.confirmText}>
                            <CustomText style={{ fontWeight: 'bold', color: '#111' }}>{targetName}</CustomText>님에게{'\n'}
                            <CustomText style={{ fontWeight: 'bold', color: '#A3E635' }}>{parseInt(amount).toLocaleString()}원</CustomText>을 보낼까요?
                        </CustomText>
                        <View style={styles.confirmButtons}>
                            <TouchableOpacity
                                style={[styles.confirmBtn, styles.cancelBtn]}
                                onPress={() => setIsConfirmVisible(false)}
                            >
                                <CustomText style={styles.cancelBtnText}>취소</CustomText>
                            </TouchableOpacity>
                            <TouchableOpacity
                                style={[styles.confirmBtn, styles.okBtn]}
                                onPress={handleConfirmTransfer}
                            >
                                <CustomText style={styles.okBtnText}>보내기</CustomText>
                            </TouchableOpacity>
                        </View>
                    </View>
                </View>
            </Modal>
        </SafeAreaView>
    );
};

const styles = StyleSheet.create({
    safeArea: { flex: 1, backgroundColor: '#FFFFFF' },
    header: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingHorizontal: 20, height: 60, backgroundColor: '#FFFFFF' },
    backButton: { width: 40, height: 40, justifyContent: 'center' },
    backButtonText: { fontSize: 24, color: '#111' },
    headerTitle: { fontSize: 18, fontWeight: 'bold' },

    container: { flexGrow: 1, backgroundColor: '#ECFCCB', paddingHorizontal: 24, paddingTop: 20, paddingBottom: 40 },
    stepContent: { backgroundColor: '#FFFFFF', borderRadius: 24, padding: 24, marginBottom: 0, shadowColor: '#000', shadowOffset: { width: 0, height: 4 }, shadowOpacity: 0.08, shadowRadius: 12, elevation: 4 },
    stepTitle: { fontSize: 26, fontWeight: '900', color: '#111', lineHeight: 34, marginBottom: 30 },

    // Step 1
    bankSelect: { flexDirection: 'row', alignItems: 'center', backgroundColor: '#FFFFFF', padding: 16, borderRadius: 16, marginBottom: 16, shadowColor: '#000', shadowOffset: { width: 0, height: 2 }, shadowOpacity: 0.08, shadowRadius: 8, elevation: 3 },
    bankCircle: { width: 24, height: 24, borderRadius: 12, marginRight: 12 },
    bankName: { flex: 1, fontSize: 16, fontWeight: '600', color: '#111' },
    chevron: { fontSize: 12, color: '#9CA3AF' },
    accountInput: { backgroundColor: '#FFFFFF', padding: 16, borderRadius: 16, fontSize: 18, fontWeight: 'bold', color: '#111', marginBottom: 16, shadowColor: '#000', shadowOffset: { width: 0, height: 2 }, shadowOpacity: 0.08, shadowRadius: 8, elevation: 3 },
    nameInput: { backgroundColor: '#FFFFFF', padding: 16, borderRadius: 16, fontSize: 16, color: '#111', shadowColor: '#000', shadowOffset: { width: 0, height: 2 }, shadowOpacity: 0.08, shadowRadius: 8, elevation: 3 },

    // Step 2
    recipientBadge: { backgroundColor: '#F9FAFB', paddingHorizontal: 12, paddingVertical: 6, borderRadius: 20, fontSize: 13, color: '#6B7280', alignSelf: 'flex-start', marginBottom: 12 },
    amountContainer: { flexDirection: 'row', alignItems: 'center', borderBottomWidth: 3, borderBottomColor: '#A3E635', paddingBottom: 10, marginBottom: 20 },
    amountInput: { flex: 1, fontSize: 40, fontWeight: '900', color: '#111' },
    currencyText: { fontSize: 24, fontWeight: 'bold', color: '#111' },
    balanceInfo: { flexDirection: 'row', alignItems: 'center', justifyContent: 'flex-end' },
    balanceLabel: { fontSize: 14, color: '#9CA3AF', marginRight: 4 },
    balanceValue: { fontSize: 14, fontWeight: 'bold', color: '#6B7280' },

    // PIN Step
    pinStep: { flex: 1, backgroundColor: '#FFFFFF', justifyContent: 'space-between', paddingBottom: 40 },
    pinHeader: { alignItems: 'center', marginTop: 80 },
    pinTitle: { fontSize: 20, fontWeight: 'bold', color: '#111', marginBottom: 30 },
    pinDots: { flexDirection: 'row', gap: 15 },
    dot: { width: 16, height: 16, borderRadius: 8, backgroundColor: '#E5E7EB' },
    dotFilled: { backgroundColor: '#A3E635' },
    keypad: { flexDirection: 'row', flexWrap: 'wrap', paddingHorizontal: 30 },
    key: { width: '33.33%', height: 80, justifyContent: 'center', alignItems: 'center' },
    keyText: { fontSize: 28, fontWeight: '600', color: '#111' },

    footer: { padding: 20 },
    nextButton: { backgroundColor: '#A3E635', height: 60, borderRadius: 20, justifyContent: 'center', alignItems: 'center', elevation: 2, shadowColor: '#000', shadowOffset: { width: 0, height: 4 }, shadowOpacity: 0.1, shadowRadius: 8 },
    nextButtonText: { fontSize: 18, fontWeight: 'bold', color: '#FFFFFF' },

    // Modals
    modalOverlay: { flex: 1, backgroundColor: 'rgba(0,0,0,0.5)', justifyContent: 'flex-end' },
    bankModal: { backgroundColor: '#FFFFFF', borderTopLeftRadius: 32, borderTopRightRadius: 32, paddingBottom: 40, height: height * 0.6 },
    modalHeader: { flexDirection: 'row', justifyContent: 'space-between', padding: 24, borderBottomWidth: 1, borderBottomColor: '#F9FAFB' },
    modalTitle: { fontSize: 18, fontWeight: 'bold' },
    modalClose: { fontSize: 20, color: '#9CA3AF' },
    bankGrid: { flexDirection: 'row', flexWrap: 'wrap', padding: 12 },
    bankItem: { width: '33.33%', alignItems: 'center', paddingVertical: 20 },
    bankItemIcon: { width: 48, height: 48, borderRadius: 24, marginBottom: 8 },
    bankItemName: { fontSize: 13, color: '#374151', fontWeight: '500' },

    confirmModal: { backgroundColor: '#FFFFFF', margin: 24, borderRadius: 24, padding: 24, alignItems: 'center' },
    confirmTitle: { fontSize: 15, fontWeight: 'bold', color: '#9CA3AF', marginBottom: 16 },
    confirmText: { fontSize: 22, color: '#4B5563', textAlign: 'center', lineHeight: 32, marginBottom: 32 },
    confirmButtons: { flexDirection: 'row', gap: 12 },
    confirmBtn: { flex: 1, height: 56, borderRadius: 16, justifyContent: 'center', alignItems: 'center' },
    cancelBtn: { backgroundColor: '#F9FAFB' },
    cancelBtnText: { color: '#6B7280', fontSize: 16, fontWeight: 'bold' },
    okBtn: { backgroundColor: '#111' },
    okBtnText: { color: '#FFFFFF', fontSize: 16, fontWeight: 'bold' }
});

export default TransferScreen;
