import React, { useState, useEffect, useCallback } from 'react';
import { View, StyleSheet, SafeAreaView, ScrollView, TouchableOpacity, Alert } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { scale, verticalScale } from 'react-native-size-matters';
import CustomText from '../../../components/common/CustomText';
import api from '../../../api/axios';

const ChildAccountScreen = ({ navigation }) => {
    const [balance, setBalance] = useState(0);
    const [transactions, setTransactions] = useState([]);

    useFocusEffect(
        useCallback(() => {
            const fetchAccountData = async () => {
                try {
                    // 내 계좌 조회
                    const accRes = await api.get('/bank/accounts/me');
                    const accounts = accRes.data?.data?.accounts;
                    if (accounts && accounts.length > 0) {
                        const primaryAcc = accounts.find(a => a.isPrimary) || accounts[0];
                        setBalance(primaryAcc.balance || 0);
                    }

                    // 거래내역 조회
                    const txRes = await api.get('/bank/transactions');
                    const txList = txRes.data?.data?.transactions || [];
                    setTransactions(txList);
                } catch (e) { console.error('Account Fetch Error', e); }
            };
            fetchAccountData();
        }, [])
    );

    const formatDisplayDate = (dateStr) => {
        if (!dateStr || dateStr.length < 8) return '';
        const month = parseInt(dateStr.substring(4, 6));
        const day = parseInt(dateStr.substring(6, 8));
        return `${month}.${day}`;
    };

    const formatDisplayTime = (dateStr) => {
        if (!dateStr || dateStr.length < 12) return '';
        return `${dateStr.substring(8, 10)}:${dateStr.substring(10, 12)}`;
    };

    let lastDate = '';
    let lastYear = '';

    return (
        <SafeAreaView style={styles.safeArea}>
            <ScrollView contentContainerStyle={styles.container} showsVerticalScrollIndicator={false}>
                {/* 잔액 카드 */}
                <View style={styles.balanceCard}>
                    <CustomText style={styles.balanceLabel}>내 지갑 잔액</CustomText>
                    <CustomText style={styles.balanceValue}>{balance.toLocaleString()}원</CustomText>
                    <View style={styles.actionRow}>
                        <TouchableOpacity style={styles.actionBtn} onPress={() => navigation.navigate('Payment')}>
                            <CustomText style={styles.actionBtnText}>결제</CustomText>
                        </TouchableOpacity>
                        <TouchableOpacity style={styles.actionBtn} onPress={() => navigation.navigate('Transfer')}>
                            <CustomText style={styles.actionBtnText}>송금하기</CustomText>
                        </TouchableOpacity>
                        <TouchableOpacity style={styles.actionBtn} onPress={() => navigation.navigate('CardListScreen')}>
                            <CustomText style={styles.actionBtnText}>내 카드</CustomText>
                        </TouchableOpacity>
                    </View>
                </View>

                {/* 최근 거래내역 요약 (고도화된 리스트) */}
                <View style={styles.historyCard}>
                    <View style={styles.historyHeader}>
                        <CustomText style={styles.sectionTitle}>최근 거래 내역</CustomText>
                        <TouchableOpacity onPress={() => navigation.navigate('TransactionCalendar')}>
                            <CustomText style={styles.seeAllText}>전체 보기 &gt;</CustomText>
                        </TouchableOpacity>
                    </View>

                    <View style={styles.listSection}>
                        {transactions.length === 0 ? (
                            <View style={styles.emptyState}>
                                <CustomText style={styles.emptyText}>거래 내역이 없습니다.</CustomText>
                            </View>
                        ) : (
                            transactions.map((tx) => {
                                const txYear = tx.date.substring(0, 4);
                                const currentDate = tx.date.substring(0, 8);
                                const showDate = currentDate !== lastDate;
                                const showYearDivider = lastYear && txYear !== lastYear;

                                lastDate = currentDate;
                                lastYear = txYear;

                                return (
                                    <React.Fragment key={tx.id}>
                                        {showYearDivider && (
                                            <View style={styles.yearDivider}>
                                                <CustomText style={styles.yearText}>{txYear}년</CustomText>
                                            </View>
                                        )}
                                        <View style={styles.txRow}>
                                            <View style={styles.dateColumn}>
                                                {showDate && (
                                                    <CustomText style={styles.groupDate}>
                                                        {formatDisplayDate(tx.date)}
                                                    </CustomText>
                                                )}
                                            </View>
                                            <TouchableOpacity
                                                style={styles.contentContainer}
                                                onPress={() => navigation.navigate('TransactionDetail', { transaction: tx })}
                                            >
                                                <View style={styles.txMainRow}>
                                                    <View style={styles.txLeft}>
                                                        <CustomText style={styles.txPlace}>{tx.place || '결제 내역'}</CustomText>
                                                        <CustomText style={styles.txTime}>{formatDisplayTime(tx.date)}</CustomText>
                                                    </View>
                                                    <View style={styles.txRight}>
                                                        <CustomText style={[styles.txAmount, tx.isIncome && styles.txIncome]}>
                                                            {tx.isIncome ? '' : '-'}{tx.amount.toLocaleString()}원
                                                        </CustomText>
                                                        <CustomText style={styles.txBalanceAfter}>
                                                            {tx.balanceAfter?.toLocaleString()}원
                                                        </CustomText>
                                                    </View>
                                                </View>
                                            </TouchableOpacity>
                                        </View>
                                    </React.Fragment>
                                );
                            })
                        )}
                    </View>
                </View>
            </ScrollView>
        </SafeAreaView>
    );
};

const styles = StyleSheet.create({
    safeArea: { flex: 1, backgroundColor: '#F9FAFB' },
    container: { padding: scale(20) },
    balanceCard: {
        backgroundColor: '#F9FAFB',
        borderRadius: scale(24),
        padding: scale(24),
        marginBottom: verticalScale(30),
        borderWidth: 1,
        borderColor: '#F9FAFB'
    },
    balanceLabel: { fontSize: scale(15), color: '#6B7280', marginBottom: verticalScale(8), fontWeight: '600' },
    balanceValue: { fontSize: scale(32), fontWeight: '900', color: '#111', marginBottom: verticalScale(20) },
    actionRow: { flexDirection: 'row', gap: scale(10) },
    actionBtn: {
        flex: 1,
        backgroundColor: '#FFFFFF',
        paddingVertical: verticalScale(14),
        borderRadius: scale(16),
        alignItems: 'center',
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 2 },
        shadowOpacity: 0.05,
        shadowRadius: 4,
        elevation: 2
    },
    actionBtnText: { fontSize: scale(14), fontWeight: 'bold', color: '#374151' },

    historyCard: { backgroundColor: '#FFFFFF', flex: 1 },
    historyHeader: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'center',
        marginBottom: verticalScale(24)
    },
    sectionTitle: { fontSize: scale(18), fontWeight: '900', color: '#111' },
    seeAllText: { color: '#84CC16', fontSize: scale(14), fontWeight: 'bold' },

    listSection: { minHeight: verticalScale(200) },
    emptyState: { alignItems: 'center', justifyContent: 'center', paddingTop: verticalScale(40) },
    emptyText: { color: '#9CA3AF', fontSize: scale(15) },

    yearDivider: {
        paddingVertical: verticalScale(16),
        marginBottom: verticalScale(8),
    },
    yearText: {
        fontSize: scale(18),
        fontWeight: '600',
        color: '#9CA3AF',
    },
    txRow: {
        flexDirection: 'row',
        marginBottom: verticalScale(24)
    },
    dateColumn: {
        width: scale(45),
        paddingTop: verticalScale(2)
    },
    groupDate: {
        fontSize: scale(15),
        fontWeight: '600',
        color: '#9CA3AF',
    },
    contentContainer: {
        flex: 1,
    },
    txMainRow: {
        flexDirection: 'row',
        justifyContent: 'space-between',
    },
    txLeft: {
        flex: 1,
    },
    txPlace: {
        fontSize: scale(17),
        fontWeight: '700',
        color: '#374151',
        marginBottom: verticalScale(4)
    },
    txTime: {
        fontSize: scale(13),
        color: '#9CA3AF'
    },
    txRight: {
        alignItems: 'flex-end'
    },
    txAmount: {
        fontSize: scale(17),
        fontWeight: '700',
        color: '#374151',
        marginBottom: verticalScale(4)
    },
    txIncome: {
        color: '#84CC16'
    },
    txBalanceAfter: {
        fontSize: scale(13),
        color: '#9CA3AF'
    }
});

export default ChildAccountScreen;
