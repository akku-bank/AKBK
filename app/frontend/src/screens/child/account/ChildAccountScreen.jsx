import React, { useState, useEffect } from 'react';
import { View, StyleSheet, SafeAreaView, ScrollView, TouchableOpacity, Alert, Switch } from 'react-native';
import { RFValue } from 'react-native-responsive-fontsize';
import CustomText from '../../../components/common/CustomText';
import api from '../../../api/axios';

const ChildAccountScreen = ({ navigation }) => {
    // 임시 계좌 데이터 상태
    const [balance, setBalance] = useState(0);
    const [transactions, setTransactions] = useState([
        { id: 1, title: '문구점', amount: -1500, date: '2023.10.25' },
        { id: 2, title: '용돈', amount: 5000, date: '2023.10.24' },
        { id: 3, title: '편의점', amount: -2000, date: '2023.10.22' }
    ]);

    useEffect(() => {
        const fetchAccountData = async () => {
            try {
                // 내 계좌 조회
                const accRes = await api.get('/bank/accounts/me');
                const accounts = accRes.data?.data?.accounts;
                if (accounts && accounts.length > 0) {
                    setBalance(accounts[0].balance || 0);
                }

                // 이번 달 거래내역 조회 (최근 4건 추출)
                const today = new Date();
                const year = today.getFullYear();
                const month = today.getMonth() + 1;
                const txRes = await api.get(`/bank/transactions?year=${year}&month=${month}`);

                const txList = txRes.data?.data?.transactions || [];
                const mappedTx = txList.slice(0, 4).map(tx => {
                    // YYYYMMDDHHmmss -> YYYY.MM.DD
                    const parsedDate = tx.date && tx.date.length >= 8
                        ? `${tx.date.substring(0, 4)}.${tx.date.substring(4, 6)}.${tx.date.substring(6, 8)}`
                        : tx.date || '';
                    return {
                        id: tx.id || Math.random().toString(),
                        title: tx.merchantName,
                        amount: tx.amount,
                        date: parsedDate
                    };
                });
                setTransactions(mappedTx);
            } catch (e) { console.error('Account Fetch Error', e); }
        };
        fetchAccountData();
    }, []);

    return (
        <SafeAreaView style={styles.safeArea}>
            <ScrollView contentContainerStyle={styles.container} showsVerticalScrollIndicator={false}>
                {/* 잔액 카드 */}
                <View style={styles.balanceCard}>
                    <CustomText style={styles.balanceLabel}>내 지갑 잔액</CustomText>
                    <CustomText style={styles.balanceValue}>{balance.toLocaleString()}원</CustomText>
                    <View style={styles.actionRow}>
                        <TouchableOpacity style={styles.actionBtn} onPress={() => navigation.navigate('Payment')}>
                            <CustomText style={styles.actionBtnText}>💸 바코드 결제</CustomText>
                        </TouchableOpacity>
                        <TouchableOpacity style={styles.actionBtn} onPress={() => navigation.navigate('Transfer')}>
                            <CustomText style={styles.actionBtnText}>🤝 송금하기</CustomText>
                        </TouchableOpacity>
                        <TouchableOpacity style={styles.actionBtn} onPress={() => navigation.navigate('CardListScreen')}>
                            <CustomText style={styles.actionBtnText}>💳 내 카드</CustomText>
                        </TouchableOpacity>
                    </View>
                </View>

                {/* 최근 거래내역 요약 */}
                <View style={styles.historyCard}>
                    <View style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: RFValue(16) }}>
                        <CustomText style={styles.sectionTitle}>최근 거래 내역</CustomText>
                        <TouchableOpacity onPress={() => navigation.navigate('TransactionCalendar')}>
                            <CustomText style={{ color: '#3B82F6', fontSize: RFValue(14), fontWeight: 'bold' }}>전체 보기 &gt;</CustomText>
                        </TouchableOpacity>
                    </View>

                    {transactions.map(item => (
                        <TouchableOpacity key={item.id} style={styles.historyRow} onPress={() => navigation.navigate('TransactionDetail', { transaction: item })}>
                            <View>
                                <CustomText style={styles.historyTitle}>{item.title}</CustomText>
                                <CustomText style={styles.historyDate}>{item.date}</CustomText>
                            </View>
                            <CustomText style={[styles.historyAmount, { color: item.amount < 0 ? '#111' : '#3B82F6' }]}>
                                {item.amount > 0 ? '+' : ''}{item.amount.toLocaleString()}원
                            </CustomText>
                        </TouchableOpacity>
                    ))}
                </View>

            </ScrollView>
        </SafeAreaView>
    );
};

const styles = StyleSheet.create({
    safeArea: { flex: 1, backgroundColor: '#F3F4F6' },
    container: { padding: RFValue(20) },
    balanceCard: { backgroundColor: '#FFF', borderRadius: RFValue(16), padding: RFValue(24), marginBottom: RFValue(30) },
    balanceLabel: { fontSize: RFValue(15), color: '#6B7280', marginBottom: RFValue(8) },
    balanceValue: { fontSize: RFValue(32), fontWeight: 'bold', color: '#111', marginBottom: RFValue(20) },
    actionRow: { flexDirection: 'row', gap: RFValue(12) },
    actionBtn: { flex: 1, backgroundColor: '#F3F4F6', padding: RFValue(14), borderRadius: RFValue(12), alignItems: 'center' },
    actionBtnText: { fontSize: RFValue(15), fontWeight: 'bold', color: '#111' },
    hideHistoryCard: { flexDirection: 'row', backgroundColor: '#FFF', borderRadius: RFValue(16), padding: RFValue(20), marginBottom: RFValue(20), justifyContent: 'space-between', alignItems: 'center' },
    hideHistoryLabel: { fontSize: RFValue(15), fontWeight: 'bold', color: '#111' },
    sectionTitle: { fontSize: RFValue(18), fontWeight: 'bold', color: '#111', marginBottom: 0 },
    historyCard: { backgroundColor: '#FFF', borderRadius: RFValue(16), padding: RFValue(20), marginBottom: RFValue(20) },
    historyRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingVertical: RFValue(12), borderBottomWidth: 1, borderBottomColor: '#F3F4F6' },
    historyTitle: { fontSize: RFValue(16), fontWeight: 'bold', color: '#111', marginBottom: RFValue(4) },
    historyDate: { fontSize: RFValue(13), color: '#9CA3AF' },
    historyAmount: { fontSize: RFValue(16), fontWeight: 'bold', color: '#111' },
    payButton: { backgroundColor: '#3B82F6', padding: RFValue(16), borderRadius: RFValue(12), alignItems: 'center' },
    payButtonText: { color: '#FFF', fontWeight: 'bold', fontSize: RFValue(16) }
});

export default ChildAccountScreen;
