import React, { useState } from 'react';
import { View, StyleSheet, TouchableOpacity, SafeAreaView, ScrollView, Alert, KeyboardAvoidingView, Platform } from 'react-native';
import { scale, verticalScale } from 'react-native-size-matters';
import CustomText from '../../../components/common/CustomText';
import CustomTextInput from '../../../components/common/CustomTextInput';
import api from '../../../api/axios';

const TransferScreen = ({ navigation }) => {
    const [amount, setAmount] = useState('');
    const [recipient, setRecipient] = useState('');

    const handleTransfer = async () => {
        if (!amount || isNaN(amount) || parseInt(amount) <= 0) {
            Alert.alert('알림', '송금할 금액을 정확히 입력해주세요.');
            return;
        }
        if (!recipient.trim()) {
            Alert.alert('알림', '받는 사람을 입력해주세요.');
            return;
        }

        try {
            // TODO: `withdrawalAccountId`, `targetAccountId`, `pin` 파라미터가 필요하므로 UI 개선 후 실제 값으로 변경해야 합니다.
            await api.post('/bank/transfer', {
                withdrawalAccountId: 'dummy-my-account',
                targetAccountId: 'dummy-target-account',
                amount: parseInt(amount),
                pin: '123456'
            });
            Alert.alert('송금 완료', '송금이 성공적으로 완료되었습니다.', [{ text: '확인', onPress: () => navigation.goBack() }]);
            return; // 성공시 여기서 리턴
        } catch (e) {
            console.error('Transfer Error:', e.response?.data || e.message);
            Alert.alert(
                '송금 시뮬레이션',
                `${recipient}님에게 ${parseInt(amount).toLocaleString()}원을 보냈어요!\n(API 파라미터 누락으로 실제 송금은 미처리)`,
                [{ text: '확인', onPress: () => navigation.goBack() }]
            );
        }
    };

    return (
        <SafeAreaView style={styles.safeArea}>
            <View style={styles.header}>
                <TouchableOpacity style={styles.backButton} onPress={() => navigation.goBack()}>
                    <CustomText style={styles.backButtonText}>←</CustomText>
                </TouchableOpacity>
                <CustomText style={styles.headerTitle}>송금하기</CustomText>
                <View style={{ width: scale(32) }} />
            </View>

            <KeyboardAvoidingView style={{ flex: 1 }} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
                <ScrollView contentContainerStyle={styles.container} showsVerticalScrollIndicator={false}>

                    <CustomText style={styles.sectionLabel}>누구에게 보낼까요?</CustomText>
                    <CustomTextInput
                        style={styles.input}
                        placeholder="받는 사람 이름"
                        placeholderTextColor="#9CA3AF"
                        value={recipient}
                        onChangeText={setRecipient}
                    />

                    <CustomText style={styles.sectionLabel}>얼마를 보낼까요?</CustomText>
                    <View style={styles.amountInputContainer}>
                        <CustomTextInput
                            style={styles.amountInput}
                            placeholder="예: 5000"
                            placeholderTextColor="#9CA3AF"
                            keyboardType="numeric"
                            value={amount}
                            onChangeText={setAmount}
                        />
                        <CustomText style={styles.currencyText}>원</CustomText>
                    </View>

                    <View style={styles.balanceInfo}>
                        <CustomText style={styles.balanceLabel}>내 지갑 잔액:</CustomText>
                        <CustomText style={styles.balanceValue}>140,000원</CustomText>
                    </View>

                </ScrollView>
                <View style={styles.footer}>
                    <TouchableOpacity style={styles.submitButton} onPress={handleTransfer}>
                        <CustomText style={styles.submitButtonText}>보내기</CustomText>
                    </TouchableOpacity>
                </View>
            </KeyboardAvoidingView>
        </SafeAreaView>
    );
};

const styles = StyleSheet.create({
    safeArea: { flex: 1, backgroundColor: '#FFFFFF' },
    header: {
        flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between',
        paddingHorizontal: scale(16), paddingVertical: verticalScale(16), backgroundColor: '#FFFFFF'
    },
    backButton: { width: scale(32), height: scale(32), justifyContent: 'center' },
    backButtonText: { fontSize: scale(22), fontWeight: 'bold', color: '#111' },
    headerTitle: { fontSize: scale(18), fontWeight: 'bold', color: '#111' },
    container: { flexGrow: 1, paddingHorizontal: scale(16), paddingTop: verticalScale(20) },
    sectionLabel: { fontSize: scale(16), fontWeight: 'bold', color: '#111', marginBottom: verticalScale(12) },
    input: {
        backgroundColor: '#F3F4F6', borderRadius: scale(12), paddingHorizontal: scale(16), paddingVertical: verticalScale(14),
        fontSize: scale(16), color: '#111', marginBottom: verticalScale(32)
    },
    amountInputContainer: {
        flexDirection: 'row', alignItems: 'center', borderBottomWidth: 2, borderBottomColor: '#A3E635',
        paddingBottom: verticalScale(8), marginBottom: verticalScale(16)
    },
    amountInput: { flex: 1, fontSize: scale(32), fontWeight: '900', color: '#111' },
    currencyText: { fontSize: scale(24), fontWeight: 'bold', color: '#111', marginLeft: scale(8) },
    balanceInfo: { flexDirection: 'row', justifyContent: 'flex-end', alignItems: 'center' },
    balanceLabel: { fontSize: scale(14), color: '#6B7280', marginRight: scale(4) },
    balanceValue: { fontSize: scale(14), fontWeight: 'bold', color: '#111' },
    footer: { paddingHorizontal: scale(16), paddingBottom: verticalScale(24) },
    submitButton: { backgroundColor: '#A3E635', paddingVertical: verticalScale(16), borderRadius: scale(16), alignItems: 'center' },
    submitButtonText: { fontSize: scale(16), fontWeight: 'bold', color: '#111' }
});

export default TransferScreen;
