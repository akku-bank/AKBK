import React, { useState, useEffect } from 'react';
import { View, StyleSheet, TouchableOpacity, SafeAreaView, ScrollView } from 'react-native';
import { scale, verticalScale } from 'react-native-size-matters';
import CustomText from '../../../components/common/CustomText';
import api from '../../../api/axios';

// 더미 데이터 (숨김 처리 로직 제거, 일괄 마스킹 정책 반영)
const TRANSACTIONS = [
    { id: '1', date: '3월 14일', place: '음식', amount: -2000, time: '14:30' },
    { id: '2', date: '3월 14일', place: '문구', amount: -1500, time: '10:00' },
    { id: '3', date: '3월 13일', place: '쇼핑', amount: -3000, time: '18:20' },
    { id: '4', date: '3월 12일', place: '부모님 송금', amount: 50000, time: '09:00', isIncome: true },
];

const ParentHistoryScreen = ({ navigation, route }) => {
    // route.params.childName 등 자녀 정보를 받아올 수 있음
    const childName = route?.params?.childName || '김싸피';
    const childId = route?.params?.childId || 'temp-child-id';

    const [transactions, setTransactions] = useState([]);
    const [balance, setBalance] = useState(0);

    useEffect(() => {
        const fetchHistory = async () => {
            try {
                const res = await api.get(`/bank/transactions/${childId}`);
                if (res.data?.data) {
                    setTransactions(res.data.data.transactions || []);
                    if (res.data.data.balance !== undefined) setBalance(res.data.data.balance);
                }
            } catch (err) {
                console.error('Fetch Parent History Error', err);
            }
        };
        fetchHistory();
    }, [childId]);

    return (
        <SafeAreaView style={styles.safeArea}>
            <View style={styles.header}>
                <TouchableOpacity style={styles.backButton} onPress={() => navigation.goBack()}>
                    <CustomText style={styles.backButtonText}>←</CustomText>
                </TouchableOpacity>
                <CustomText style={styles.headerTitle}>{childName}의 계좌 내역</CustomText>
                <View style={{ width: scale(32) }} />
            </View>

            <ScrollView contentContainerStyle={styles.container}>
                <View style={styles.balanceCard}>
                    <CustomText style={styles.balanceLabel}>현재 잔액</CustomText>
                    <CustomText style={styles.balanceAmount}>{balance.toLocaleString()} <CustomText style={styles.balanceCurrency}>원</CustomText></CustomText>
                </View>

                <View style={styles.listSection}>
                    <CustomText style={styles.listTitle}>최근 거래</CustomText>
                    {transactions.map(tx => (
                        <View key={tx.id} style={styles.txRow}>
                            <View style={styles.txLeft}>
                                <CustomText style={styles.txDate}>{tx.date}</CustomText>
                                {/* 일괄 숨김 정책에 따라 부모 화면에서는 가맹점 상세 대신 카테고리만 노출 */}
                                <CustomText style={styles.txPlace}>{tx.categoryName || tx.place || '결제 내역'}</CustomText>
                            </View>
                            <View style={styles.txRight}>
                                <CustomText style={[styles.txAmount, tx.isIncome && styles.txIncome]}>
                                    {tx.isIncome ? '+' : ''}{tx.amount.toLocaleString()}원
                                </CustomText>
                                <CustomText style={styles.txTime}>{tx.time}</CustomText>
                            </View>
                        </View>
                    ))}
                </View>
            </ScrollView>
        </SafeAreaView>
    );
};

const styles = StyleSheet.create({
    safeArea: { flex: 1, backgroundColor: '#FFFFFF' },
    header: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingHorizontal: scale(16), paddingVertical: verticalScale(16), backgroundColor: '#FFFFFF' },
    backButton: { width: scale(32), height: scale(32), justifyContent: 'center' },
    backButtonText: { fontSize: scale(22), fontWeight: 'bold', color: '#111' },
    headerTitle: { fontSize: scale(18), fontWeight: 'bold', color: '#111' },
    container: { flexGrow: 1 },

    balanceCard: { backgroundColor: '#F3F4F6', margin: scale(16), padding: scale(24), borderRadius: scale(20), alignItems: 'center' },
    balanceLabel: { fontSize: scale(14), color: '#6B7280', marginBottom: verticalScale(8) },
    balanceAmount: { fontSize: scale(32), fontWeight: '900', color: '#111' },
    balanceCurrency: { fontSize: scale(20) },

    listSection: { paddingHorizontal: scale(16), paddingTop: verticalScale(8) },
    listTitle: { fontSize: scale(16), fontWeight: 'bold', color: '#4B5563', marginBottom: verticalScale(16) },

    txRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingVertical: verticalScale(16), borderBottomWidth: 1, borderBottomColor: '#F9FAFB' },
    txLeft: { flex: 1 },
    txDate: { fontSize: scale(12), color: '#9CA3AF', marginBottom: verticalScale(4) },
    txPlace: { fontSize: scale(16), fontWeight: 'bold', color: '#111' },

    txRight: { alignItems: 'flex-end' },
    txAmount: { fontSize: scale(16), fontWeight: 'bold', color: '#111', marginBottom: verticalScale(4) },
    txIncome: { color: '#A3E635' }, // 입금액은 라임 그린
    txTime: { fontSize: scale(12), color: '#9CA3AF' }
});

export default ParentHistoryScreen;
