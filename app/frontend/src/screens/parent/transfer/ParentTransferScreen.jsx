import React, { useState } from 'react';
import { View, StyleSheet, SafeAreaView, TouchableOpacity, Alert } from 'react-native';
import { scale, verticalScale } from 'react-native-size-matters';
import CustomText from '../../../components/common/CustomText';
import CustomTextInput from '../../../components/common/CustomTextInput';
import api from '../../../api/axios';

const ParentTransferScreen = ({ navigation, route }) => {
    // 딥링크(푸시 알림)를 통해 진입 시 route.params에 금액(amount)과 자녀 정보(child)가 전달됨
    const child = route.params?.child || { name: '자녀', accountId: '' };
    const initialAmount = route.params?.amount ? String(route.params.amount) : '';

    const [amount, setAmount] = useState(initialAmount);
    const [pin, setPin] = useState('');
    const [myAccount, setMyAccount] = useState(null);

    React.useEffect(() => {
        api.get('/bank/accounts/me').then(res => {
            if (res.data?.data) {
                const accs = res.data.data.accounts || [];
                if (accs.length > 0) setMyAccount(accs[0]);
                else setMyAccount(res.data.data); // in case single object
            }
        }).catch(err => console.error('Parent account load error', err));
    }, []);

    const handleTransfer = () => {
        if (!amount || amount === '0') {
            Alert.alert('알림', '보낼 금액을 입력해주세요.');
            return;
        }
        if (!pin || pin.length < 6) {
            Alert.alert('알림', '6자리 결제 비밀번호를 입력해주세요.');
            return;
        }
        if (!myAccount?.accountId) {
            Alert.alert('알림', '출금할 계좌를 찾지 못했습니다.');
            return;
        }
        if (!child?.accountId) {
            Alert.alert('알림', '자녀의 계좌 번호가 등록되어있지 않습니다.');
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
                        try {
                            await api.post('/bank/accounts/transfers', {
                                withdrawalAccountId: myAccount.accountId,
                                targetAccountId: child.accountId,
                                amount: parseInt(amount),
                                pin: pin
                            });
                            Alert.alert('송금 완료!', `${child.name}의 계좌로 입금되었습니다.`, [{ text: '확인', onPress: () => navigation.goBack() }]);
                            return;
                        } catch (e) {
                            console.error('Transfer Error:', e.response?.data || e.message);
                            Alert.alert('오류', e.response?.data?.message || '송금 처리에 실패했습니다.');
                        }
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

                {myAccount && (
                    <CustomText style={{ fontSize: scale(14), color: '#6B7280', marginTop: verticalScale(4) }}>
                        내 잔액: {myAccount.balance?.toLocaleString() || 0}원
                    </CustomText>
                )}

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

                <CustomText style={[styles.promptText, { fontSize: scale(16), marginTop: verticalScale(24) }]}>결제 비밀번호</CustomText>
                <CustomTextInput
                    style={styles.pinInput}
                    keyboardType="number-pad"
                    value={pin}
                    onChangeText={setPin}
                    placeholder="6자리 PIN 입력"
                    placeholderTextColor="#D1D5DB"
                    secureTextEntry
                    maxLength={6}
                />
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

    pinInput: {
        backgroundColor: '#F3F4F6', borderRadius: scale(12), paddingHorizontal: scale(16), paddingVertical: verticalScale(14),
        fontSize: scale(18), color: '#111', marginTop: verticalScale(12), marginBottom: verticalScale(32), fontWeight: 'bold', letterSpacing: 8
    },

    footer: { padding: scale(16), backgroundColor: '#FFFFFF' },
    mainButton: { backgroundColor: '#A3E635', paddingVertical: verticalScale(16), borderRadius: scale(16), alignItems: 'center' },
    disabledButton: { backgroundColor: '#D1D5DB' },
    mainButtonText: { fontSize: scale(16), fontWeight: 'bold', color: '#111' }
});

export default ParentTransferScreen;
