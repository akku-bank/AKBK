import React, { useState, useEffect, useRef } from 'react';
import { View, StyleSheet, TouchableOpacity, SafeAreaView, TextInput, KeyboardAvoidingView, Platform, Alert, ScrollView, Modal, FlatList, Animated } from 'react-native';
import CustomText from '../../components/common/CustomText';
import { RFValue } from 'react-native-responsive-fontsize';
import api from '../../api/axios';

const BANKS = [
    { code: '999', name: '싸피은행' },
    { code: '088', name: '신한은행' },
    { code: '004', name: '국민은행' },
    { code: '020', name: '우리은행' },
    { code: '090', name: '카카오뱅크' },
    { code: '011', name: '농협은행' },
    { code: '003', name: '기업은행' },
    { code: '081', name: 'KEB하나은행' },
    { code: '001', name: '한국은행' },
];

const ParentInitialSetupScreen = ({ navigation, route }) => {
    const { tempToken, role, name, familyCode } = route.params || {};

    const [bankAccount, setBankAccount] = useState('');
    const [selectedBank, setSelectedBank] = useState(BANKS[0]);
    const [isBankModalVisible, setIsBankModalVisible] = useState(false);

    const [isVerificationRequested, setIsVerificationRequested] = useState(false);
    const [verificationCode, setVerificationCode] = useState('');
    const [isVerified, setIsVerified] = useState(false);
    const [isLoading, setIsLoading] = useState(false);

    // 가상 알림(Toast) 관련 상태
    const [showToast, setShowToast] = useState(false);
    const [toastData, setToastData] = useState({ bankName: '', code: '' });
    const toastAnim = useRef(new Animated.Value(-100)).current;

    const triggerToast = (bankName, code) => {
        setToastData({ bankName, code });
        setShowToast(true);
        Animated.sequence([
            Animated.timing(toastAnim, {
                toValue: RFValue(20),
                duration: 500,
                useNativeDriver: true,
            }),
            Animated.delay(5000),
            Animated.timing(toastAnim, {
                toValue: -100,
                duration: 500,
                useNativeDriver: true,
            }),
        ]).start(() => setShowToast(false));
    };

    const handleRequest1Won = async () => {
        if (!bankAccount.trim()) {
            Alert.alert('알림', '계좌번호를 입력해주세요.');
            return;
        }
        setIsLoading(true);
        try {
            const response = await api.post('/bank/accounts/verify/request',
                { bankCode: selectedBank.code, accountNumber: bankAccount },
                { headers: { Authorization: `Bearer ${tempToken}` } }
            );

            const authCode = response.data?.data?.authCode;
            if (authCode) {
                setIsVerificationRequested(true);
                // 가상 알림 트리거
                triggerToast(selectedBank.name, authCode);
            }
        } catch (error) {
            console.error('Verify Request Error:', error.response?.data || error.message);
            Alert.alert('오류', error.response?.data?.message || '1원 송금 요청 중 문제가 발생했습니다.');
        } finally {
            setIsLoading(false);
        }
    };

    const handleConfirmVerification = async () => {
        if (verificationCode.length !== 4) {
            Alert.alert('알림', '인증코드 4자리를 입력해주세요.');
            return;
        }
        setIsLoading(true);
        try {
            await api.post('/bank/accounts/verify/confirm',
                { bankCode: selectedBank.code, accountNumber: bankAccount, authCode: verificationCode },
                { headers: { Authorization: `Bearer ${tempToken}` } }
            );
            setIsVerified(true);
            Alert.alert('성공', '계좌 인증이 완료되었습니다.');
        } catch (error) {
            console.error('Verify Confirm Error:', error.response?.data || error.message);
            Alert.alert('오류', '인증코드가 일치하지 않습니다.');
        } finally {
            setIsLoading(false);
        }
    };

    const handleFinalize = async () => {
        if (!isVerified) {
            Alert.alert('알림', '먼저 계좌 인증을 완료해주세요.');
            return;
        }
        setIsLoading(true);
        try {
            // 1. 가족 그룹 참가 또는 생성
            if (familyCode && familyCode !== 'mock-family-code') {
                try {
                    await api.post('/families/join', { scannedQrCode: familyCode }, { headers: { Authorization: `Bearer ${tempToken}` } });
                } catch (e) {
                    console.warn('Spouse Join API Error (Bypassed if already in family):', e.response?.data || e.message);
                }
            } else {
                try {
                    await api.post('/families', {}, { headers: { Authorization: `Bearer ${tempToken}` } });
                } catch (famErr) {
                    if (famErr.response?.data?.errorCode !== 'FAM_008') {
                        throw famErr;
                    }
                }
            }

            navigation.replace('PinNumberSetup', { tempToken, role, name, bankAccount });
        } catch (error) {
            console.error('Finalize Setup Error:', error.response?.data || error.message);
            Alert.alert('오류', '최종 등록 중 문제가 발생했습니다.');
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <SafeAreaView style={styles.safeArea}>
            {/* 가상 Toast 알림 UI (시스템 알림 스타일) */}
            {showToast && (
                <Animated.View style={[styles.toastContainer, { transform: [{ translateY: toastAnim }] }]}>
                    <View style={styles.toastHeader}>
                        <CustomText style={styles.toastTitle}>{toastData.bankName}</CustomText>
                        <CustomText style={styles.toastTime}>지금</CustomText>
                    </View>
                    <View style={styles.toastBody}>
                        <CustomText style={styles.toastContent}>입금 1원 SSAFY{toastData.code}</CustomText>
                    </View>
                </Animated.View>
            )}

            <KeyboardAvoidingView style={styles.container} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
                <ScrollView contentContainerStyle={styles.scrollContent}>
                    <View style={styles.stepContainer}>
                        <CustomText style={styles.title}>내 계좌 연결</CustomText>
                        <CustomText style={styles.subtitle}>아이들의 용돈을 충전해 줄{'\n'}부모님의 주거래 계좌를 연결해주세요.</CustomText>

                        {/* 은행 선택 */}
                        <TouchableOpacity
                            style={styles.bankSelect}
                            onPress={() => setIsBankModalVisible(true)}
                            disabled={isVerified}
                        >
                            <CustomText style={styles.bankSelectText}>{selectedBank.name}</CustomText>
                            <CustomText style={styles.dropdownIcon}>▼</CustomText>
                        </TouchableOpacity>

                        {/* 계좌 번호 입력 */}
                        <TextInput
                            style={[styles.input, isVerified && styles.disabledInput]}
                            placeholder="계좌번호 입력 (숫자만)"
                            placeholderTextColor="#9CA3AF"
                            value={bankAccount}
                            onChangeText={setBankAccount}
                            keyboardType="number-pad"
                            editable={!isVerified}
                        />

                        {!isVerificationRequested ? (
                            <TouchableOpacity
                                style={styles.verifyRequestButton}
                                onPress={handleRequest1Won}
                                disabled={isLoading || !bankAccount}
                            >
                                <CustomText style={styles.verifyRequestButtonText}>계좌 인증하기</CustomText>
                            </TouchableOpacity>
                        ) : !isVerified ? (
                            <View style={styles.verificationSection}>
                                <CustomText style={styles.verificationLabel}>입금자명 뒤의 숫자 4자리를 입력해주세요.</CustomText>
                                <TextInput
                                    style={styles.codeInput}
                                    placeholder="4자리 숫자"
                                    placeholderTextColor="#9CA3AF"
                                    value={verificationCode}
                                    onChangeText={setVerificationCode}
                                    keyboardType="number-pad"
                                    maxLength={4}
                                />
                                <TouchableOpacity
                                    style={styles.verifyConfirmButton}
                                    onPress={handleConfirmVerification}
                                    disabled={isLoading || verificationCode.length < 4}
                                >
                                    <CustomText style={styles.verifyConfirmButtonText}>인증 확인</CustomText>
                                </TouchableOpacity>
                            </View>
                        ) : (
                            <View style={styles.verifiedBadge}>
                                <CustomText style={styles.verifiedText}>✅ 인증 완료</CustomText>
                            </View>
                        )}

                        <View style={styles.infoBox}>
                            <CustomText style={styles.infoText}>🔒 입력하신 정보는 안전하게 암호화되어 보관됩니다.</CustomText>
                        </View>
                    </View>
                </ScrollView>

                <View style={styles.buttonSection}>
                    {/* 개발자용 강제 패스 버튼 */}
                    <TouchableOpacity
                        style={{ paddingVertical: 10, alignItems: 'center', marginBottom: 5 }}
                        onPress={() => {
                            setBankAccount('000000000000');
                            setIsVerified(true);
                        }}
                        activeOpacity={0.7}
                    >
                        <CustomText style={{ color: '#9CA3AF', fontSize: 13, textDecorationLine: 'underline' }}>
                            [개발자 전용] 실계좌 인증 없이 넘어가기
                        </CustomText>
                    </TouchableOpacity>

                    <TouchableOpacity
                        style={[styles.submitButton, (!isVerified || isLoading) ? styles.submitButtonDisabled : null]}
                        onPress={handleFinalize}
                        disabled={!isVerified || isLoading}
                        activeOpacity={0.8}
                    >
                        <CustomText style={styles.submitButtonText}>{isLoading ? '처리 중...' : '다음 단계로'}</CustomText>
                    </TouchableOpacity>
                </View>
            </KeyboardAvoidingView>

            {/* 은행 선택 모달 */}
            <Modal visible={isBankModalVisible} transparent animationType="slide">
                <View style={styles.modalOverlay}>
                    <View style={styles.modalContent}>
                        <View style={styles.modalHeader}>
                            <CustomText style={styles.modalTitle}>은행 선택</CustomText>
                            <TouchableOpacity onPress={() => setIsBankModalVisible(false)}>
                                <CustomText style={styles.modalClose}>✕</CustomText>
                            </TouchableOpacity>
                        </View>
                        <FlatList
                            data={BANKS}
                            keyExtractor={(item) => item.code}
                            renderItem={({ item }) => (
                                <TouchableOpacity
                                    style={styles.bankItem}
                                    onPress={() => {
                                        setSelectedBank(item);
                                        setIsBankModalVisible(false);
                                    }}
                                >
                                    <CustomText style={styles.bankItemText}>{item.name}</CustomText>
                                </TouchableOpacity>
                            )}
                        />
                    </View>
                </View>
            </Modal>
        </SafeAreaView>
    );
};

const styles = StyleSheet.create({
    safeArea: { flex: 1, backgroundColor: '#FFFFFF' },
    container: { flex: 1 },
    scrollContent: { flexGrow: 1, paddingHorizontal: RFValue(24), paddingTop: RFValue(40) },

    toastContainer: {
        position: 'absolute', top: 0, left: RFValue(12), right: RFValue(12),
        backgroundColor: 'rgba(255, 255, 255, 0.95)', borderRadius: RFValue(18), padding: RFValue(14),
        zIndex: 9999, elevation: 15, shadowColor: '#000', shadowOffset: { width: 0, height: 8 },
        shadowOpacity: 0.15, shadowRadius: 10, borderWidth: 0.5, borderColor: '#E5E7EB',
    },
    toastHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: RFValue(4) },
    toastTitle: { fontSize: RFValue(14), fontWeight: '700', color: '#111827' },
    toastTime: { fontSize: RFValue(11), color: '#6B7280' },
    toastBody: { marginTop: RFValue(2) },
    toastContent: { fontSize: RFValue(13), color: '#374151', fontWeight: '500' },

    stepContainer: { flex: 1 },
    title: { fontSize: RFValue(26), fontWeight: 'bold', color: '#111', marginBottom: RFValue(12) },
    subtitle: { fontSize: RFValue(15), color: '#6B7280', fontWeight: '500', lineHeight: RFValue(22), marginBottom: RFValue(30) },

    bankSelect: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', borderBottomWidth: 2, borderBottomColor: '#111', paddingVertical: RFValue(12), marginBottom: RFValue(10) },
    bankSelectText: { fontSize: RFValue(18), fontWeight: 'bold', color: '#111' },
    dropdownIcon: { fontSize: RFValue(12), color: '#111' },

    input: { fontSize: RFValue(18), color: '#111', fontWeight: 'bold', borderBottomWidth: 2, borderBottomColor: '#111', paddingVertical: RFValue(12), marginBottom: RFValue(20) },
    disabledInput: { color: '#9CA3AF', borderBottomColor: '#E5E7EB' },

    verifyRequestButton: { backgroundColor: '#F9FAFB', paddingVertical: RFValue(12), borderRadius: RFValue(12), alignItems: 'center' },
    verifyRequestButtonText: { color: '#4B5563', fontWeight: 'bold', fontSize: RFValue(14) },

    verificationSection: { marginTop: RFValue(10), backgroundColor: '#F9FAFB', padding: RFValue(20), borderRadius: RFValue(16) },
    verificationLabel: { fontSize: RFValue(13), color: '#4B5563', marginBottom: RFValue(15), textAlign: 'center' },
    codeInput: { backgroundColor: '#FFFFFF', borderWidth: 1, borderColor: '#D1D5DB', borderRadius: RFValue(12), paddingVertical: RFValue(12), fontSize: RFValue(20), fontWeight: 'bold', textAlign: 'center', color: '#111', marginBottom: RFValue(15) },
    verifyConfirmButton: { backgroundColor: '#111', paddingVertical: RFValue(14), borderRadius: RFValue(12), alignItems: 'center' },
    verifyConfirmButtonText: { color: '#FFFFFF', fontWeight: 'bold', fontSize: RFValue(15) },

    verifiedBadge: { backgroundColor: '#F0FDF4', paddingVertical: RFValue(10), borderRadius: RFValue(12), alignItems: 'center' },
    verifiedText: { color: '#166534', fontWeight: 'bold', fontSize: RFValue(14) },

    infoBox: { marginTop: RFValue(20) },
    infoText: { fontSize: RFValue(13), color: '#9CA3AF', textAlign: 'center' },

    buttonSection: { paddingHorizontal: RFValue(24), paddingBottom: Platform.OS === 'ios' ? RFValue(20) : RFValue(16) },
    submitButton: { width: '100%', backgroundColor: '#A3E635', height: RFValue(54), borderRadius: RFValue(12), justifyContent: 'center', alignItems: 'center' },
    submitButtonDisabled: { backgroundColor: '#E5E7EB' },
    submitButtonText: { color: '#111', fontSize: RFValue(16), fontWeight: 'bold' },

    modalOverlay: { flex: 1, backgroundColor: 'rgba(0,0,0,0.5)', justifyContent: 'flex-end' },
    modalContent: { backgroundColor: '#FFFFFF', borderTopLeftRadius: RFValue(24), borderTopRightRadius: RFValue(24), paddingHorizontal: RFValue(24), paddingBottom: RFValue(40), maxHeight: '60%' },
    modalHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingVertical: RFValue(20), borderBottomWidth: 1, borderBottomColor: '#F9FAFB' },
    modalTitle: { fontSize: RFValue(18), fontWeight: 'bold', color: '#111' },
    modalClose: { fontSize: RFValue(20), color: '#9CA3AF' },
    bankItem: { paddingVertical: RFValue(18), borderBottomWidth: 1, borderBottomColor: '#F9FAFB' },
    bankItemText: { fontSize: RFValue(16), color: '#111' }
});

export default ParentInitialSetupScreen;
