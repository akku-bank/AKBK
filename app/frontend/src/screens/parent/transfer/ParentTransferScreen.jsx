import React, { useState } from 'react';
import { View, StyleSheet, SafeAreaView, TouchableOpacity, Alert } from 'react-native';
import { scale, verticalScale } from 'react-native-size-matters';
import CustomText from '../../../components/common/CustomText';
import CustomTextInput from '../../../components/common/CustomTextInput';
import api from '../../../api/axios';

const ParentTransferScreen = ({ navigation, route }) => {
    const child = route.params?.child || { name: '자녀' };
    const [amount, setAmount] = useState('');

    const handleTransfer = () => {
        if (!amount || amount === '0') {
            Alert.alert('알림', '보낼 금액을 입력해주세요.');
            return;
        }

        Alert.alert(
            '송금 확인',
            `${child.name}에게 ${parseInt(amount).toLocaleString()}원을 보낼까요?`,
            [
                { text: '취소', style: 'cancel' },
                {
                    text: '보내기',
                    onPress: async () => {
                        /* ==========================================
                           [진짜 자녀 용돈 송금 API]
                           ========================================== 
                        try {
                            // await api.post('/transactions/transfer', { amount: parseInt(amount), targetAccountName: child.name });
                            // Alert.alert('송금 완료!', `${child.name}의 계좌로 입금되었습니다.`, [{ text: '확인', onPress: () => navigation.goBack() }]);
                            // return;
                        } catch(e) { console.error('Transfer Error:', e); }
                        ========================================== */

                        // --- 실제 연동 시 아래 임시 로직 삭제 ---
                        Alert.alert('송금 완료!', `${child.name}의 계좌로 입금되었습니다.`, [{ text: '확인', onPress: () => navigation.goBack() }]);
                        // ------------------------------------
                    }
                }
            ]
        );
    };

    return (
        <SafeAreaView style={styles.safeArea}>
            <View style={styles.header}>
                <TouchableOpacity style={styles.backButton} onPress={() => navigation.goBack()}>
                    <CustomText style={styles.backButtonText}>←</CustomText>
                </TouchableOpacity>
                <CustomText style={styles.headerTitle}>용돈 송금</CustomText>
                <View style={{ width: scale(32) }} />
            </View>

            <View style={styles.container}>
                <CustomText style={styles.promptText}>{child.name}에게</CustomText>
                <CustomText style={styles.promptText}>얼마를 보낼까요?</CustomText>

                <View style={styles.inputContainer}>
                    <CustomTextInput
                        style={styles.amountInput}
                        keyboardType="number-pad"
                        value={amount}
                        onChangeText={setAmount}
                        placeholder="0"
                        placeholderTextColor="#D1D5DB"
                        autoFocus
                    />
                    <CustomText style={styles.currencyText}>원</CustomText>
                </View>

                <View style={styles.quickAmountRow}>
                    {[10000, 30000, 50000].map(val => (
                        <TouchableOpacity key={val} style={styles.quickAmountBtn} onPress={() => setAmount((parseInt(amount || 0) + val).toString())}>
                            <CustomText style={styles.quickAmountText}>+{val.toLocaleString()}원</CustomText>
                        </TouchableOpacity>
                    ))}
                </View>

            </View>

            <View style={styles.footer}>
                <TouchableOpacity style={[styles.mainButton, (!amount || amount === '0') && styles.disabledButton]} onPress={handleTransfer}>
                    <CustomText style={styles.mainButtonText}>보내기</CustomText>
                </TouchableOpacity>
            </View>
        </SafeAreaView>
    );
};

const styles = StyleSheet.create({
    safeArea: { flex: 1, backgroundColor: '#FFFFFF' },
    header: {
        flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between',
        paddingHorizontal: scale(16), paddingVertical: verticalScale(16)
    },
    backButton: { width: scale(32), height: scale(32), justifyContent: 'center' },
    backButtonText: { fontSize: scale(22), fontWeight: 'bold', color: '#111' },
    headerTitle: { fontSize: scale(18), fontWeight: 'bold', color: '#111' },

    container: { flex: 1, paddingHorizontal: scale(24), paddingTop: verticalScale(40) },

    promptText: { fontSize: scale(24), fontWeight: '900', color: '#111', marginBottom: verticalScale(8) },

    inputContainer: { flexDirection: 'row', alignItems: 'center', borderBottomWidth: 2, borderBottomColor: '#A3E635', paddingVertical: verticalScale(12), marginTop: verticalScale(24), marginBottom: verticalScale(24) },
    amountInput: { flex: 1, fontSize: scale(36), fontWeight: 'bold', color: '#111', padding: 0 },
    currencyText: { fontSize: scale(24), fontWeight: 'bold', color: '#111', marginLeft: scale(8) },

    quickAmountRow: { flexDirection: 'row', gap: scale(8) },
    quickAmountBtn: { backgroundColor: '#F3F4F6', paddingHorizontal: scale(16), paddingVertical: verticalScale(10), borderRadius: scale(20) },
    quickAmountText: { fontSize: scale(14), fontWeight: 'bold', color: '#4B5563' },

    footer: { padding: scale(16), backgroundColor: '#FFFFFF' },
    mainButton: { backgroundColor: '#A3E635', paddingVertical: verticalScale(16), borderRadius: scale(16), alignItems: 'center' },
    disabledButton: { backgroundColor: '#D1D5DB' },
    mainButtonText: { fontSize: scale(16), fontWeight: 'bold', color: '#111' }
});

export default ParentTransferScreen;
