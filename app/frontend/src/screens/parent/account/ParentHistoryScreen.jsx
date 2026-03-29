import React, { useState, useEffect, useCallback } from 'react';
import { View, StyleSheet, TouchableOpacity, SafeAreaView, ScrollView, DeviceEventEmitter } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
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
    const child = route?.params?.child;
    const childName = child?.name || route?.params?.childName || '자녀';
    const childId = child?.childId || route?.params?.childId;

    const [transactions, setTransactions] = useState([]);
    const [balance, setBalance] = useState(0);

    const fetchHistory = useCallback(async () => {
        try {
            // childId가 있으면 자녀 내역, 없으면 본인(부모) 내역 조회
            const endpoint = childId ? `/bank/transactions/${childId}` : '/bank/transactions';
            const res = await api.get(endpoint);

            if (res.data?.data) {
                setTransactions(res.data.data.transactions || []);
                if (res.data.data.balance !== undefined) setBalance(res.data.data.balance);
            }
        } catch (err) {
            console.error('Fetch Parent History Error', err);
        }
    }, [childId]);

    // 포커스 될 때 & 알림 올 때 갱신
    useFocusEffect(
        useCallback(() => {
            fetchHistory();
        }, [fetchHistory])
    );

    useEffect(() => {
        const subscription = DeviceEventEmitter.addListener('refresh_transactions', fetchHistory);
        return () => subscription.remove();
    }, [fetchHistory]);

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
            <View style={styles.header}>
                <TouchableOpacity style={styles.backButton} onPress={() => navigation.goBack()}>
                    <CustomText style={styles.backButtonText}>←</CustomText>
                </TouchableOpacity>
                <CustomText style={styles.headerTitle}>전체 ▽</CustomText>
                <View style={{ width: scale(32) }} />
            </View>

            <ScrollView contentContainerStyle={styles.container}>
                <View style={styles.listSection}>
                    {transactions.map((tx, index) => {
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
                                    <View style={styles.contentContainer}>
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
                                    </View>
                                </View>
                            </React.Fragment>
                        );
                    })}
                </View>
            </ScrollView>
        </SafeAreaView>
    );
};

const styles = StyleSheet.create({
    safeArea: { flex: 1, backgroundColor: '#FFFFFF' },
    header: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        paddingHorizontal: scale(20),
        paddingVertical: verticalScale(12),
        backgroundColor: '#FFFFFF'
    },
    backButton: { width: scale(32) },
    backButtonText: { fontSize: scale(22), fontWeight: 'bold', color: '#111' },
    headerTitle: { fontSize: scale(16), fontWeight: '600', color: '#4B5563' },
    searchButton: {
        width: scale(36),
        height: scale(36),
        borderRadius: scale(18),
        backgroundColor: '#F9FAFB',
        justifyContent: 'center',
        alignItems: 'center',
        borderWidth: 1,
        borderColor: '#E5E7EB',
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 2 },
        shadowOpacity: 0.05,
        shadowRadius: 4,
        elevation: 2,
    },
    searchIcon: { fontSize: scale(16) },

    container: { flexGrow: 1, paddingBottom: verticalScale(40) },

    listSection: { paddingHorizontal: scale(20), paddingTop: verticalScale(20) },

    yearDivider: {
        paddingVertical: verticalScale(20),
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
        color: '#3B82F6' // 입금액은 처리 파란색 (이미지 기준)
    },
    txBalanceAfter: {
        fontSize: scale(13),
        color: '#9CA3AF'
    }
});

export default ParentHistoryScreen;
