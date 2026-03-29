import React, { useState, useEffect, useCallback } from 'react';
import { View, StyleSheet, TouchableOpacity, ScrollView, SafeAreaView, DeviceEventEmitter } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { scale, verticalScale } from 'react-native-size-matters';
import useTransactionStore from '../../../store/transactionStore';
import CustomText from '../../../components/common/CustomText';
import api from '../../../api/axios';

// 임시 달력/내역 데이터 (키: YYYY-MM-DD)
const MOCK_DAYS = ['일', '월', '화', '수', '목', '금', '토'];

// 빈 객체로 시작 (API 연동)

const TransactionCalendarScreen = ({ navigation }) => {
    const today = new Date();
    const [currentDate, setCurrentDate] = useState(today);
    const [selectedDate, setSelectedDate] = useState(today.getDate());
    const [transactionsByDate, setTransactionsByDate] = useState({});


    const year = currentDate.getFullYear();
    const month = currentDate.getMonth();

    const fetchMonthlyTransactions = useCallback(async () => {
        try {
            const res = await api.get(`/bank/transactions?year=${year}&month=${month + 1}`);
            const txList = res.data?.data?.transactions || [];

            const grouped = {};
            txList.forEach(tx => {
                const dateStr = tx.date;
                if (!dateStr || dateStr.length < 8) return;

                const dateKey = `${dateStr.substring(0, 4)}-${dateStr.substring(4, 6)}-${dateStr.substring(6, 8)}`;
                const timeStr = dateStr.length >= 12
                    ? `${dateStr.substring(8, 10)}:${dateStr.substring(10, 12)}`
                    : '';

                if (!grouped[dateKey]) grouped[dateKey] = [];

                grouped[dateKey].push({
                    id: tx.id || Math.random().toString(),
                    time: timeStr,
                    title: tx.place || tx.merchantName || '결제 내역',
                    amount: tx.amount,
                    type: tx.isIncome ? 'DEPOSIT' : 'PAYMENT',
                    isHidden: tx.isHidden
                });
            });
            setTransactionsByDate(grouped);
        } catch (e) { console.error('Transaction Fetch Error:', e); }
    }, [year, month]);

    // 포커스 될 때 & 알림 올 때 갱신
    useFocusEffect(
        useCallback(() => {
            fetchMonthlyTransactions();
        }, [fetchMonthlyTransactions])
    );

    useEffect(() => {
        const subscription = DeviceEventEmitter.addListener('refresh_transactions', fetchMonthlyTransactions);
        return () => subscription.remove();
    }, [fetchMonthlyTransactions]);


    const prevMonth = () => {
        setCurrentDate(new Date(year, month - 1, 1));
        setSelectedDate(1);
    };

    const nextMonth = () => {
        setCurrentDate(new Date(year, month + 1, 1));
        setSelectedDate(1);
    };

    // YYYY-MM-DD 포맷 변환
    const formatDateKey = (y, m, d) => `${y}-${String(m + 1).padStart(2, '0')}-${String(d).padStart(2, '0')}`;

    // 실제 달력 계산 로직
    const renderCalendarGrid = () => {
        const firstDayOfMonth = new Date(year, month, 1).getDay();
        const daysInMonth = new Date(year, month + 1, 0).getDate();

        let days = [];

        // 빈 칸 (시작 요일 맞추기)
        for (let i = 0; i < firstDayOfMonth; i++) {
            days.push(<View key={`empty-${i}`} style={styles.dayCell} />);
        }

        for (let i = 1; i <= daysInMonth; i++) {
            const isSelected = selectedDate === i;
            const dateKey = formatDateKey(year, month, i);
            const hasHistory = !!transactionsByDate[dateKey];

            days.push(
                <TouchableOpacity
                    key={i}
                    style={[styles.dayCell, isSelected && styles.selectedDayCell]}
                    onPress={() => setSelectedDate(i)}
                >
                    <CustomText style={[styles.dayText, isSelected && styles.selectedDayText]}>{i}</CustomText>
                    {hasHistory && !isSelected && <View style={styles.historyDot} />}
                </TouchableOpacity>
            );
        }

        return days;
    };

    const renderTransactionItem = (item) => {
        const isDeposit = item.amount > 0;
        const isHidden = item.isHidden;

        return (
            <TouchableOpacity
                key={item.id}
                style={styles.transactionItem}
                activeOpacity={0.7}
                onPress={() => navigation.navigate('TransactionDetail', { transaction: item })}
            >
                <View style={[styles.transactionIconBox, isHidden && { opacity: 0.6 }]}>
                    <CustomText style={styles.transactionIcon}>{isDeposit ? '💰' : '🏪'}</CustomText>
                </View>
                <View style={styles.transactionInfo}>
                    <View style={{ flexDirection: 'row', alignItems: 'center' }}>
                        <CustomText style={styles.transactionTitle}>
                            {item.title}
                        </CustomText>
                        {isHidden && (
                            <CustomText style={{ fontSize: scale(10), marginLeft: scale(4), color: '#9CA3AF' }}>
                                (비공개 🔒)
                            </CustomText>
                        )}
                    </View>
                    <CustomText style={styles.transactionTime}>{item.time}</CustomText>
                </View>
                <View style={styles.transactionAmountWrapper}>
                    <CustomText style={[styles.transactionAmount, isDeposit ? styles.depositColor : styles.withdrawColor]}>
                        {isDeposit ? '+' : ''}{item.amount.toLocaleString()}원
                    </CustomText>
                </View>
            </TouchableOpacity>
        );
    };

    return (
        <SafeAreaView style={styles.safeArea}>
            {/* 상단 헤더 */}
            <View style={styles.header}>
                <TouchableOpacity style={styles.backButton} onPress={() => navigation.goBack()}>
                    <CustomText style={styles.backButtonText}>←</CustomText>
                </TouchableOpacity>
                <CustomText style={styles.headerTitle}>거래 내역</CustomText>
                <View style={{ width: 40 }} />
            </View>

            <ScrollView contentContainerStyle={styles.container} showsVerticalScrollIndicator={false}>

                {/* 달력 영역 */}
                <View style={styles.calendarCard}>
                    <View style={styles.monthSelector}>
                        <TouchableOpacity onPress={prevMonth} style={styles.monthNavButton}>
                            <CustomText style={styles.monthNavText}>{'<'}</CustomText>
                        </TouchableOpacity>
                        <CustomText style={styles.monthText}>{year}년 {month + 1}월</CustomText>
                        <TouchableOpacity onPress={nextMonth} style={styles.monthNavButton}>
                            <CustomText style={styles.monthNavText}>{'>'}</CustomText>
                        </TouchableOpacity>
                    </View>
                    <View style={styles.weekRow}>
                        {MOCK_DAYS.map((day, index) => (
                            <View key={index} style={styles.dayHeaderCell}>
                                <CustomText style={[styles.dayHeaderText, index === 0 && styles.sundayText]}>{day}</CustomText>
                            </View>
                        ))}
                    </View>
                    <View style={styles.calendarGrid}>
                        {renderCalendarGrid()}
                    </View>
                </View>

                {/* 선택된 날짜의 거래 내역 */}
                <View style={styles.dailyDetailSection}>
                    <CustomText style={styles.detailDateTitle}>{month + 1}월 {selectedDate}일 소비</CustomText>
                    <View style={styles.transactionList}>
                        {(() => {
                            const dateKey = formatDateKey(year, month, selectedDate);
                            const dailyTransactions = transactionsByDate[dateKey];

                            if (dailyTransactions && dailyTransactions.length > 0) {
                                return dailyTransactions.map(renderTransactionItem);
                            } else {
                                return (
                                    <View style={styles.emptyState}>
                                        <CustomText style={styles.emptyStateText}>이 날은 거래 내역이 없어요.</CustomText>
                                    </View>
                                );
                            }
                        })()}
                    </View>
                </View>
            </ScrollView>
        </SafeAreaView >
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
        flexGrow: 1,
        paddingHorizontal: scale(16),
        paddingBottom: verticalScale(40),
    },
    calendarCard: {
        backgroundColor: '#FFFFFF',
        borderRadius: scale(20),
        paddingHorizontal: scale(16),
        paddingVertical: verticalScale(20),
        marginBottom: verticalScale(20),
        shadowColor: '#000',
        shadowOffset: { width: 0, height: verticalScale(2) },
        shadowOpacity: 0.05,
        shadowRadius: scale(8),
        elevation: 2,
    },
    monthSelector: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'center',
        marginBottom: verticalScale(16),
        gap: scale(16),
    },
    monthNavButton: {
        paddingHorizontal: scale(12),
        paddingVertical: verticalScale(4),
    },
    monthNavText: {
        fontSize: scale(18),
        color: '#9CA3AF',
        fontWeight: 'bold',
    },
    monthText: {
        fontSize: scale(18),
        fontWeight: 'bold',
        color: '#111',
        minWidth: scale(90),
        textAlign: 'center',
    },
    weekRow: {
        flexDirection: 'row',
        marginBottom: verticalScale(8),
    },
    dayHeaderCell: {
        flex: 1,
        alignItems: 'center',
    },
    dayHeaderText: {
        fontSize: scale(13),
        fontWeight: '600',
        color: '#9CA3AF',
    },
    sundayText: {
        color: '#EF4444', // 일요일 빨간색
    },
    calendarGrid: {
        flexDirection: 'row',
        flexWrap: 'wrap',
    },
    dayCell: {
        width: '14.28%', // 7등분
        aspectRatio: 1,
        justifyContent: 'center',
        alignItems: 'center',
        marginBottom: verticalScale(4),
    },
    selectedDayCell: {
        backgroundColor: '#111',
        borderRadius: scale(20), // 둥근 원형 선택 표시
    },
    dayText: {
        fontSize: scale(15),
        fontWeight: '600',
        color: '#4B5563',
    },
    selectedDayText: {
        color: '#FFFFFF',
    },
    historyDot: {
        width: scale(4),
        height: scale(4),
        borderRadius: scale(2),
        backgroundColor: '#84CC16',
        position: 'absolute',
        bottom: scale(6),
    },
    dailyDetailSection: {
        flex: 1,
    },
    detailDateTitle: {
        fontSize: scale(16),
        fontWeight: 'bold',
        color: '#111',
        marginBottom: verticalScale(12),
        paddingHorizontal: scale(4),
    },
    transactionList: {
        backgroundColor: '#FFFFFF',
        borderRadius: scale(20),
        padding: scale(16),
        shadowColor: '#000',
        shadowOffset: { width: 0, height: verticalScale(2) },
        shadowOpacity: 0.05,
        shadowRadius: scale(8),
        elevation: 2,
        minHeight: verticalScale(150),
    },
    emptyState: {
        flex: 1,
        justifyContent: 'center',
        alignItems: 'center',
    },
    emptyStateText: {
        color: '#9CA3AF',
        fontSize: scale(14),
    },
    transactionItem: {
        flexDirection: 'row',
        alignItems: 'center',
        paddingVertical: verticalScale(14),
        borderBottomWidth: 1,
        borderBottomColor: '#F3F4F6',
    },
    transactionIconBox: {
        width: scale(40),
        height: scale(40),
        borderRadius: scale(20),
        backgroundColor: '#F3F4F6',
        justifyContent: 'center',
        alignItems: 'center',
        marginRight: scale(12),
    },
    transactionIcon: {
        fontSize: scale(20),
    },
    transactionInfo: {
        flex: 1,
    },
    transactionTitle: {
        fontSize: scale(15),
        fontWeight: '600',
        color: '#111',
        marginBottom: verticalScale(2),
    },
    transactionTime: {
        fontSize: scale(12),
        color: '#9CA3AF',
    },
    transactionAmountWrapper: {
        alignItems: 'flex-end',
    },
    transactionAmount: {
        fontSize: scale(16),
        fontWeight: 'bold',
    },
    depositColor: {
        color: '#84CC16',
    },
    withdrawColor: {
        color: '#111',
    },
    hiddenText: {
        color: '#9CA3AF',
        fontStyle: 'italic',
    }
});

export default TransactionCalendarScreen;
