import React, { useState, useEffect } from 'react';
import { View, StyleSheet, SafeAreaView, TouchableOpacity, ScrollView, Platform, ActivityIndicator } from 'react-native';
import Svg, { Circle, G, Path, Text as SvgText } from 'react-native-svg';
import { scale, verticalScale } from 'react-native-size-matters';
import CustomText from '../../../components/common/CustomText';
import { getMyWeeklyReport } from '../../../api/reportApi';

const CATEGORY_COLORS = ['#FF8A65', '#64B5F6', '#81C784', '#FFD54F', '#CE93D8', '#80DEEA', '#FFCC80', '#EF9A9A'];
const DAY_KEYS = ['mon', 'tue', 'wed', 'thu', 'fri', 'sat', 'sun'];
const DAY_LABELS = ['월', '화', '수', '목', '금', '토', '일'];

const formatCurrency = (amount) => `${(amount || 0).toLocaleString()}원`;
const formatChartAmount = (amount) => (amount || 0).toLocaleString();

const getMondayDate = (offsetWeeks = 0) => {
    const today = new Date();
    const dayOfWeek = today.getDay();
    const daysToMonday = dayOfWeek === 0 ? -6 : 1 - dayOfWeek;
    const monday = new Date(today);
    monday.setDate(today.getDate() + daysToMonday + offsetWeeks * 7);
    return monday;
};

const formatDateParam = (date) => {
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
};

const formatWeekLabel = (mondayDate) => {
    const sunday = new Date(mondayDate);
    sunday.setDate(mondayDate.getDate() + 6);
    return `${mondayDate.getMonth() + 1}.${mondayDate.getDate()} - ${sunday.getMonth() + 1}.${sunday.getDate()}`;
};

const polarToCartesian = (cx, cy, radius, angle) => {
    const radian = ((angle - 90) * Math.PI) / 180;
    return { x: cx + radius * Math.cos(radian), y: cy + radius * Math.sin(radian) };
};

const createArcPath = (cx, cy, outerRadius, innerRadius, startAngle, endAngle) => {
    const startOuter = polarToCartesian(cx, cy, outerRadius, endAngle);
    const endOuter = polarToCartesian(cx, cy, outerRadius, startAngle);
    const startInner = polarToCartesian(cx, cy, innerRadius, endAngle);
    const endInner = polarToCartesian(cx, cy, innerRadius, startAngle);
    const largeArcFlag = endAngle - startAngle > 180 ? 1 : 0;
    return [
        `M ${startOuter.x} ${startOuter.y}`,
        `A ${outerRadius} ${outerRadius} 0 ${largeArcFlag} 0 ${endOuter.x} ${endOuter.y}`,
        `L ${endInner.x} ${endInner.y}`,
        `A ${innerRadius} ${innerRadius} 0 ${largeArcFlag} 1 ${startInner.x} ${startInner.y}`,
        'Z',
    ].join(' ');
};

const getSliceLabelPosition = (cx, cy, startAngle, endAngle, outerRadius, innerRadius) => {
    const middleAngle = (startAngle + endAngle) / 2;
    const labelRadius = (outerRadius + innerRadius) / 2;
    return polarToCartesian(cx, cy, labelRadius, middleAngle);
};

const bodyFontFamily = Platform.select({
    ios: 'Apple SD Gothic Neo',
    android: 'sans-serif',
    default: undefined,
});

const WeeklyReportScreen = ({ navigation }) => {
    const [weekOffset, setWeekOffset] = useState(0);
    const [reportData, setReportData] = useState(null);
    const [loading, setLoading] = useState(true);
    const [selectedCategoryIndex, setSelectedCategoryIndex] = useState(0);

    const mondayDate = getMondayDate(weekOffset);
    const weekLabel = formatWeekLabel(mondayDate);
    const dateParam = formatDateParam(mondayDate);

    useEffect(() => {
        setLoading(true);
        setSelectedCategoryIndex(0);
        getMyWeeklyReport(dateParam)
            .then(res => setReportData(res.data?.data || null))
            .catch(() => setReportData(null))
            .finally(() => setLoading(false));
    }, [dateParam]);

    const dailyFlowData = DAY_KEYS.map((key, i) => ({
        day: DAY_LABELS[i],
        expense: reportData?.dailySpending?.[key] || 0,
        income: reportData?.dailyIncome?.[key] || 0,
    }));

    const categoryData = (reportData?.categoryRatios || []).map((cat, i) => ({
        id: cat.subCategoryId,
        label: cat.subCategoryName,
        color: CATEGORY_COLORS[i % CATEGORY_COLORS.length],
        total: cat.spendingAmount || 0,
        ratio: cat.ratio || 0,
    }));

    const totalSpending = reportData?.totalSpending || 0;
    const totalIncome = reportData?.totalIncome || 0;
    const selectedCategory = categoryData[selectedCategoryIndex] || null;
    const maxFlowAmount = Math.max(...dailyFlowData.map(item => Math.max(item.expense, item.income)), 1);

    const chartHeights = dailyFlowData.map(item => ({
        ...item,
        incomeHeight: Math.max((item.income / maxFlowAmount) * verticalScale(90), item.income > 0 ? scale(4) : 0),
        expenseHeight: Math.max((item.expense / maxFlowAmount) * verticalScale(90), item.expense > 0 ? scale(4) : 0),
    }));

    const maxIncomeRenderHeight = Math.max(...chartHeights.map(item => item.incomeHeight), verticalScale(10));
    const maxExpenseRenderHeight = Math.max(...chartHeights.map(item => item.expenseHeight), verticalScale(10));

    const incomeTrackHeight = maxIncomeRenderHeight + verticalScale(20);
    const expenseTrackHeight = maxExpenseRenderHeight + verticalScale(20);

    let currentAngle = 0;
    const pieSlices = categoryData.map((category) => {
        const angle = totalSpending > 0 ? (category.total / totalSpending) * 360 : 360 / categoryData.length;
        const slice = { ...category, startAngle: currentAngle, endAngle: currentAngle + angle };
        currentAngle += angle;
        return slice;
    });

    return (
        <SafeAreaView style={styles.safeArea}>
            <View style={styles.header}>
                <TouchableOpacity style={styles.backButton} onPress={() => navigation.goBack()}>
                    <CustomText style={styles.backButtonText}>←</CustomText>
                </TouchableOpacity>
                <CustomText style={styles.headerTitle}>주간 소비 리포트</CustomText>
                <View style={styles.headerSpacer} />
            </View>

            <ScrollView contentContainerStyle={styles.container} showsVerticalScrollIndicator={false}>
                <View style={styles.weekNavigator}>
                    <TouchableOpacity
                        style={styles.weekArrowButton}
                        onPress={() => setWeekOffset(prev => prev - 1)}
                    >
                        <CustomText style={styles.weekArrowText}>‹</CustomText>
                    </TouchableOpacity>
                    <View style={styles.weekLabelBox}>
                        <CustomText style={styles.weekLabel}>{weekLabel}</CustomText>
                    </View>
                    <TouchableOpacity
                        style={styles.weekArrowButton}
                        onPress={() => setWeekOffset(prev => prev + 1)}
                        disabled={weekOffset >= 0}
                    >
                        <CustomText style={[styles.weekArrowText, weekOffset >= 0 && styles.weekArrowDisabled]}>›</CustomText>
                    </TouchableOpacity>
                </View>

                {loading ? (
                    <ActivityIndicator size="large" color="#34D399" style={{ marginTop: verticalScale(40) }} />
                ) : (
                    <>
                        <View style={styles.summaryCard}>
                            <CustomText style={styles.summaryTitle}>이번 주 총 지출</CustomText>
                            <CustomText style={styles.amountText}>{formatCurrency(totalSpending)}</CustomText>
                        </View>

                        <View style={styles.chartCard}>
                            <View style={styles.cardHeaderRow}>
                                <CustomText style={styles.sectionTitle}>요일별 수입·지출 흐름</CustomText>
                            </View>
                            <View style={styles.barChart}>
                                <View style={styles.plotArea}>
                                    <View style={[styles.centerAxisLine, { top: incomeTrackHeight + verticalScale(12) }]} />
                                    <View style={styles.barRow}>
                                        {chartHeights.map((item) => (
                                            <View key={item.day} style={styles.barColumn}>
                                                <View style={[styles.incomeTrack, { height: incomeTrackHeight }]}>
                                                    {item.income > 0 && (
                                                        <CustomText style={styles.barValuePositive} numberOfLines={1}>
                                                            +{formatChartAmount(item.income)}
                                                        </CustomText>
                                                    )}
                                                    {item.income > 0 && <View style={[styles.positiveBar, { height: item.incomeHeight }]} />}
                                                </View>

                                                <View style={{ height: verticalScale(24) }} />

                                                <View style={[styles.expenseTrack, { height: expenseTrackHeight }]}>
                                                    {item.expense > 0 && <View style={[styles.negativeBar, { height: item.expenseHeight }]} />}
                                                    {item.expense > 0 && (
                                                        <CustomText style={styles.barValueNegative} numberOfLines={1}>
                                                            -{formatChartAmount(item.expense)}
                                                        </CustomText>
                                                    )}
                                                </View>

                                                <CustomText style={styles.dayLabel}>{item.day}</CustomText>
                                            </View>
                                        ))}
                                    </View>
                                </View>
                            </View>
                            <View style={styles.chartSummaryRow}>
                                <View style={styles.summaryChip}>
                                    <View style={[styles.legendDot, styles.positiveDot]} />
                                    <CustomText style={styles.summaryChipText}>{`총 수입 ${formatCurrency(totalIncome)}`}</CustomText>
                                </View>
                                <View style={styles.summaryChip}>
                                    <View style={[styles.legendDot, styles.negativeDot]} />
                                    <CustomText style={styles.summaryChipText}>{`총 지출 ${formatCurrency(totalSpending)}`}</CustomText>
                                </View>
                            </View>
                        </View>

                        {!!reportData?.aiSpendingSummary && (
                            <View style={styles.aiReviewCard}>
                                <CustomText style={styles.aiReviewTitle}>AI 소비 분석</CustomText>
                                <CustomText style={[styles.aiReviewText, styles.bodyCopyText]}>
                                    {reportData.aiSpendingSummary}
                                </CustomText>
                            </View>
                        )}

                        {!!reportData?.aiQuizSummary && (
                            <View style={[styles.aiReviewCard, styles.aiQuizCard]}>
                                <CustomText style={[styles.aiReviewTitle, styles.aiQuizTitle]}>AI 퀴즈 분석</CustomText>
                                <CustomText style={[styles.aiReviewText, styles.aiQuizText, styles.bodyCopyText]}>
                                    {reportData.aiQuizSummary}
                                </CustomText>
                            </View>
                        )}

                        {categoryData.length > 0 && (
                            <View style={styles.categoryCard}>
                                <View style={styles.cardHeaderRow}>
                                    <CustomText style={styles.sectionTitle}>카테고리별 지출</CustomText>
                                    <CustomText style={styles.cardCaption}>파이를 누르면 내역 표시</CustomText>
                                </View>

                                <View style={styles.pieSection}>
                                    <View style={styles.pieChartWrap}>
                                        <Svg width={scale(260)} height={scale(260)} viewBox="0 0 260 260">
                                            <Circle cx="130" cy="130" r="86" fill="#F8FAFC" />
                                            {pieSlices.map((slice, i) => {
                                                const isSelected = i === selectedCategoryIndex;
                                                const outerRadius = isSelected ? 104 : 96;
                                                const innerRadius = 56;
                                                const labelPosition = getSliceLabelPosition(
                                                    130, 130,
                                                    slice.startAngle, slice.endAngle,
                                                    outerRadius, innerRadius,
                                                );
                                                return (
                                                    <G key={slice.id ?? i}>
                                                        <Path
                                                            d={createArcPath(130, 130, outerRadius, innerRadius, slice.startAngle, slice.endAngle)}
                                                            fill={slice.color}
                                                            opacity={isSelected ? 1 : 0.9}
                                                            onPress={() => setSelectedCategoryIndex(i)}
                                                        />
                                                        <SvgText
                                                            x={labelPosition.x}
                                                            y={labelPosition.y}
                                                            fontSize={scale(12)}
                                                            fontWeight="700"
                                                            fontFamily="Mulmaru"
                                                            fill="#111827"
                                                            textAnchor="middle"
                                                            alignmentBaseline="middle"
                                                        >
                                                            {slice.label}
                                                        </SvgText>
                                                    </G>
                                                );
                                            })}
                                        </Svg>
                                        <View style={styles.pieCenterLabel} pointerEvents="none">
                                            <CustomText style={styles.pieCenterAmount}>{formatCurrency(selectedCategory?.total)}</CustomText>
                                        </View>
                                    </View>
                                </View>

                                <View style={styles.detailCard}>
                                    <CustomText style={styles.detailTitle}>{`${selectedCategory?.label} 이번 주 지출`}</CustomText>
                                    <View style={styles.detailRow}>
                                        <CustomText style={styles.detailText}>총 지출</CustomText>
                                        <CustomText style={styles.detailAmount}>{formatCurrency(selectedCategory?.total)}</CustomText>
                                    </View>
                                    <View style={[styles.detailRow, styles.detailRowLast]}>
                                        <CustomText style={styles.detailText}>지출 비중</CustomText>
                                        <CustomText style={styles.detailAmount}>
                                            {selectedCategory?.ratio
                                                ? `${(Number(selectedCategory.ratio) * 100).toFixed(1)}%`
                                                : '0%'}
                                        </CustomText>
                                    </View>
                                </View>
                            </View>
                        )}
                    </>
                )}
            </ScrollView>
        </SafeAreaView>
    );
};

const styles = StyleSheet.create({
    safeArea: { flex: 1, backgroundColor: '#ECFCCB' },
    header: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        paddingHorizontal: scale(16),
        paddingVertical: verticalScale(16),
        backgroundColor: '#FFFFFF',
    },
    backButton: { width: scale(32), height: scale(32), justifyContent: 'center' },
    backButtonText: { fontSize: scale(22), fontWeight: 'bold', color: '#111827' },
    headerTitle: { fontSize: scale(18), fontWeight: 'bold', color: '#111827' },
    headerSpacer: { width: scale(32) },
    container: {
        flexGrow: 1,
        paddingHorizontal: scale(16),
        paddingTop: verticalScale(16),
        paddingBottom: verticalScale(40),
    },
    weekNavigator: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'center',
        marginBottom: verticalScale(14),
    },
    weekArrowButton: {
        width: scale(36),
        height: scale(36),
        alignItems: 'center',
        justifyContent: 'center',
    },
    weekArrowText: {
        fontSize: scale(24),
        fontWeight: '700',
        color: '#111827',
    },
    weekArrowDisabled: {
        color: '#CBD5E1',
    },
    weekLabelBox: {
        minWidth: scale(132),
        marginHorizontal: scale(8),
        paddingHorizontal: scale(14),
        paddingVertical: verticalScale(8),
        borderRadius: scale(999),
        backgroundColor: '#FFFFFF',
        alignItems: 'center',
        justifyContent: 'center',
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 2 },
        shadowOpacity: 0.05,
        shadowRadius: 4,
        elevation: 2,
    },
    weekLabel: {
        fontSize: scale(14),
        fontWeight: '700',
        color: '#111827',
    },
    summaryCard: {
        backgroundColor: '#FFFFFF',
        padding: scale(20),
        borderRadius: scale(16),
        alignItems: 'center',
        marginBottom: verticalScale(16),
        shadowColor: '#000',
        shadowOffset: { width: 0, height: verticalScale(2) },
        shadowOpacity: 0.05,
        shadowRadius: scale(4),
        elevation: 2,
    },
    summaryTitle: { fontSize: scale(14), color: '#6B7280', marginBottom: verticalScale(8) },
    amountText: { fontSize: scale(28), fontWeight: '900', color: '#111827', marginBottom: verticalScale(8) },
    chartCard: {
        backgroundColor: '#FFFFFF',
        padding: scale(18),
        borderRadius: scale(16),
        marginBottom: verticalScale(16),
        shadowColor: '#000',
        shadowOffset: { width: 0, height: verticalScale(2) },
        shadowOpacity: 0.05,
        shadowRadius: scale(4),
        elevation: 2,
    },
    cardHeaderRow: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'center',
        marginBottom: verticalScale(14),
    },
    sectionTitle: { fontSize: scale(16), fontWeight: 'bold', color: '#111827' },
    cardCaption: { fontSize: scale(12), color: '#94A3B8' },
    barChart: {
        borderRadius: scale(16),
        backgroundColor: '#F8FAFC',
        paddingHorizontal: scale(6),
        paddingVertical: verticalScale(20),
        overflow: 'hidden',
    },
    plotArea: {
        flex: 1,
        position: 'relative',
    },
    centerAxisLine: {
        position: 'absolute',
        left: scale(7),
        right: scale(7),
        height: 2,
        backgroundColor: '#898e96ff',
        zIndex: 0,
        borderRadius: 1,
    },
    barRow: {
        flex: 1,
        flexDirection: 'row',
        alignItems: 'flex-start',
        justifyContent: 'space-between',
        zIndex: 1,
    },
    barColumn: {
        width: scale(38),
        alignItems: 'center',
    },
    incomeTrack: {
        justifyContent: 'flex-end',
        alignItems: 'center',
        width: '100%',
    },
    expenseTrack: {
        justifyContent: 'flex-start',
        alignItems: 'center',
        width: '100%',
    },
    negativeBar: {
        width: scale(24),
        backgroundColor: '#F87171',
        borderBottomLeftRadius: scale(6),
        borderBottomRightRadius: scale(6),
    },
    positiveBar: {
        width: scale(24),
        backgroundColor: '#34D399',
        borderTopLeftRadius: scale(6),
        borderTopRightRadius: scale(6),
    },
    barValueNegative: {
        fontSize: scale(8.5),
        color: '#DC2626',
        fontWeight: 'bold',
        marginTop: verticalScale(4),
        textAlign: 'center',
    },
    barValuePositive: {
        fontSize: scale(8.5),
        color: '#059669',
        fontWeight: 'bold',
        marginBottom: verticalScale(4),
        textAlign: 'center',
    },
    dayLabel: {
        marginTop: verticalScale(10),
        fontSize: scale(14),
        fontWeight: 'bold',
        color: '#334155',
    },
    chartSummaryRow: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        marginTop: verticalScale(14),
        gap: scale(8),
    },
    summaryChip: {
        flex: 1,
        flexDirection: 'row',
        alignItems: 'center',
        backgroundColor: '#F8FAFC',
        borderRadius: scale(12),
        paddingHorizontal: scale(12),
        paddingVertical: verticalScale(10),
    },
    summaryChipText: {
        fontSize: scale(12),
        color: '#334155',
        fontWeight: '600',
    },
    legendDot: {
        width: scale(10),
        height: scale(10),
        borderRadius: scale(5),
        marginRight: scale(8),
    },
    positiveDot: { backgroundColor: '#34D399' },
    negativeDot: { backgroundColor: '#F87171' },
    aiReviewCard: {
        backgroundColor: '#F0FDF4',
        padding: scale(20),
        borderRadius: scale(16),
        marginBottom: verticalScale(16),
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 2 },
        shadowOpacity: 0.05,
        shadowRadius: 4,
        elevation: 2,
    },
    aiReviewTitle: { fontSize: scale(16), fontWeight: 'bold', color: '#166534', marginBottom: verticalScale(8), fontFamily: 'Mulmaru' },
    aiReviewText: { fontSize: scale(14), color: '#14532D', lineHeight: 26, fontFamily: 'Mulmaru', wordBreak: 'keep-all' },
    aiQuizCard: { backgroundColor: '#EFF6FF' },
    aiQuizTitle: { color: '#1E3A8A', fontFamily: 'Mulmaru' },
    aiQuizText: { color: '#1E40AF', fontFamily: 'Mulmaru', wordBreak: 'keep-all' },
    bodyCopyText: { fontFamily: 'Mulmaru', letterSpacing: 0 },
    categoryCard: {
        backgroundColor: '#FFFFFF',
        padding: scale(20),
        borderRadius: scale(16),
        shadowColor: '#000',
        shadowOffset: { width: 0, height: verticalScale(2) },
        shadowOpacity: 0.05,
        shadowRadius: scale(4),
        elevation: 2,
    },
    pieSection: {
        alignItems: 'center',
        justifyContent: 'center',
        marginBottom: verticalScale(16),
    },
    pieChartWrap: {
        width: scale(260),
        height: scale(260),
        alignItems: 'center',
        justifyContent: 'center',
    },
    pieCenterLabel: {
        position: 'absolute',
        width: scale(110),
        alignItems: 'center',
        justifyContent: 'center',
    },
    pieCenterAmount: {
        fontSize: scale(19),
        fontWeight: '900',
        color: '#64748B',
        textAlign: 'center',
    },
    detailCard: {
        backgroundColor: '#F8FAFC',
        borderRadius: scale(14),
        paddingHorizontal: scale(14),
        paddingVertical: verticalScale(14),
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 2 },
        shadowOpacity: 0.05,
        shadowRadius: 4,
        elevation: 2,
    },
    detailTitle: {
        fontSize: scale(14),
        fontWeight: 'bold',
        color: '#111827',
        marginBottom: verticalScale(8),
    },
    detailRow: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        paddingVertical: verticalScale(10),
        borderBottomWidth: 1,
        borderBottomColor: '#E2E8F0',
    },
    detailRowLast: {
        borderBottomWidth: 0,
        paddingBottom: 0,
    },
    detailText: {
        fontSize: scale(14),
        color: '#334155',
    },
    detailAmount: {
        fontSize: scale(14),
        fontWeight: 'bold',
        color: '#DC2626',
    },
});

export default WeeklyReportScreen;
