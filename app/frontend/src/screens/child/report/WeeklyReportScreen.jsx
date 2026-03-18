import React, { useState } from 'react';
import { View, StyleSheet, SafeAreaView, TouchableOpacity, ScrollView, Platform } from 'react-native';
import Svg, { Circle, G, Path, Text as SvgText } from 'react-native-svg';
import { scale, verticalScale } from 'react-native-size-matters';
import CustomText from '../../../components/common/CustomText';

const WEEKLY_FLOW_DATA = [
    { day: '월', income: 12000, expense: 18000 },
    { day: '화', income: 8000, expense: 14000 },
    { day: '수', income: 21000, expense: 9000 },
    { day: '목', income: 6000, expense: 17000 },
    { day: '금', income: 28000, expense: 22000 },
    { day: '토', income: 17000, expense: 26000 },
    { day: '일', income: 9000, expense: 7000 },
];

const CATEGORY_DATA = [
    {
        id: 'snack',
        label: '간식',
        color: '#FF8A65',
        total: 18200,
        transactions: [
            { day: '월', title: '편의점 젤리', amount: 2500 },
            { day: '수', title: '분식집 떡볶이', amount: 4800 },
            { day: '금', title: '카페 음료', amount: 5900 },
            { day: '토', title: '아이스크림', amount: 5000 },
        ],
    },
    {
        id: 'play',
        label: '오락',
        color: '#64B5F6',
        total: 12400,
        transactions: [
            { day: '화', title: '문구점 뽑기', amount: 2400 },
            { day: '목', title: '게임 아이템', amount: 5000 },
            { day: '토', title: '코인노래방', amount: 5000 },
        ],
    },
    {
        id: 'transport',
        label: '이동',
        color: '#81C784',
        total: 7600,
        transactions: [
            { day: '수', title: '버스 충전', amount: 3800 },
            { day: '일', title: '지하철 이용', amount: 3800 },
        ],
    },
    {
        id: 'study',
        label: '학습',
        color: '#FFD54F',
        total: 5400,
        transactions: [
            { day: '월', title: '연습장 구매', amount: 1800 },
            { day: '금', title: '독서실 간식', amount: 3600 },
        ],
    },
];

const WEEK_RANGES = ['3.1-3.9', '3.10-3.16', '3.17-3.23', '3.24-3.31'];

const formatCurrency = (amount) => `${amount.toLocaleString()}원`;
const formatChartAmount = (amount) => amount.toLocaleString();

const polarToCartesian = (cx, cy, radius, angle) => {
    const radian = ((angle - 90) * Math.PI) / 180;

    return {
        x: cx + radius * Math.cos(radian),
        y: cy + radius * Math.sin(radian),
    };
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
    const [weekIndex, setWeekIndex] = useState(1);
    const [selectedCategoryId, setSelectedCategoryId] = useState(CATEGORY_DATA[0].id);

    const totalExpense = CATEGORY_DATA.reduce((sum, category) => sum + category.total, 0);
    const totalIncome = WEEKLY_FLOW_DATA.reduce((sum, item) => sum + item.income, 0);
    const selectedCategory = CATEGORY_DATA.find((category) => category.id === selectedCategoryId) || CATEGORY_DATA[0];
    const maxFlowAmount = Math.max(...WEEKLY_FLOW_DATA.flatMap((item) => [item.income, item.expense]));

    let currentAngle = 0;
    const pieSlices = CATEGORY_DATA.map((category) => {
        const angle = (category.total / totalExpense) * 360;
        const slice = {
            ...category,
            startAngle: currentAngle,
            endAngle: currentAngle + angle,
        };
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
                        onPress={() => setWeekIndex((prev) => Math.max(prev - 1, 0))}
                        disabled={weekIndex === 0}
                    >
                        <CustomText style={[styles.weekArrowText, weekIndex === 0 && styles.weekArrowDisabled]}>‹</CustomText>
                    </TouchableOpacity>
                    <View style={styles.weekLabelBox}>
                        <CustomText style={styles.weekLabel}>{WEEK_RANGES[weekIndex]}</CustomText>
                    </View>
                    <TouchableOpacity
                        style={styles.weekArrowButton}
                        onPress={() => setWeekIndex((prev) => Math.min(prev + 1, WEEK_RANGES.length - 1))}
                        disabled={weekIndex === WEEK_RANGES.length - 1}
                    >
                        <CustomText
                            style={[
                                styles.weekArrowText,
                                weekIndex === WEEK_RANGES.length - 1 && styles.weekArrowDisabled,
                            ]}
                        >
                            ›
                        </CustomText>
                    </TouchableOpacity>
                </View>

                <View style={styles.summaryCard}>
                    <CustomText style={styles.summaryTitle}>이번 주 총 지출</CustomText>
                    <CustomText style={styles.amountText}>{formatCurrency(totalExpense)}</CustomText>
                    <CustomText style={styles.comparisonText}>
                        저번 주보다 <CustomText style={styles.highlightRed}>3,000원</CustomText> 더 썼어요
                    </CustomText>
                </View>

                <View style={styles.chartCard}>
                    <View style={styles.cardHeaderRow}>
                        <CustomText style={styles.sectionTitle}>요일별 수입/지출</CustomText>
                        <CustomText style={styles.cardCaption}>더미 데이터</CustomText>
                    </View>
                    <View style={styles.barChart}>
                        <View style={styles.plotArea}>
                            <View style={styles.chartMidLine} />
                            <View style={styles.barRow}>
                                {WEEKLY_FLOW_DATA.map((item) => {
                                    const incomeHeight = Math.max((item.income / maxFlowAmount) * verticalScale(70), scale(8));
                                    const expenseHeight = Math.max((item.expense / maxFlowAmount) * verticalScale(70), scale(8));

                                    return (
                                        <View key={item.day} style={styles.barColumn}>
                                            <View style={styles.barTrack}>
                                                <View style={styles.positiveZone}>
                                                    <CustomText style={styles.barValuePositive}>{`+${formatChartAmount(item.income)}`}</CustomText>
                                                    <View style={[styles.positiveBar, { height: incomeHeight }]} />
                                                </View>
                                                <View style={styles.negativeZone}>
                                                    <View style={[styles.negativeBar, { height: expenseHeight }]} />
                                                    <CustomText style={styles.barValueNegative}>{`-${formatChartAmount(item.expense)}`}</CustomText>
                                                </View>
                                            </View>
                                            <CustomText style={styles.dayLabel}>{item.day}</CustomText>
                                        </View>
                                    );
                                })}
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
                            <CustomText style={styles.summaryChipText}>{`총 지출 ${formatCurrency(totalExpense)}`}</CustomText>
                        </View>
                    </View>
                </View>

                <View style={styles.aiReviewCard}>
                    <CustomText style={styles.aiReviewTitle}>AI 소비 분석</CustomText>
                    <CustomText style={[styles.aiReviewText, styles.bodyCopyText]}>
                        이번 주는 금요일과 토요일 지출이 크게 늘었어요. 간식과 오락 비중이 높아서 주중에 예산을 먼저 정해두면
                        잔액 관리가 더 쉬워질 거예요.
                    </CustomText>
                </View>

                <View style={styles.categoryCard}>
                    <View style={styles.cardHeaderRow}>
                        <CustomText style={styles.sectionTitle}>카테고리별 지출</CustomText>
                        <CustomText style={styles.cardCaption}>파이를 누르면 내역 표시</CustomText>
                    </View>

                    <View style={styles.pieSection}>
                        <View style={styles.pieChartWrap}>
                            <Svg width={scale(260)} height={scale(260)} viewBox="0 0 260 260">
                                <Circle cx="130" cy="130" r="86" fill="#F8FAFC" />
                                {pieSlices.map((slice) => {
                                    const isSelected = slice.id === selectedCategoryId;
                                    const outerRadius = isSelected ? 104 : 96;
                                    const innerRadius = 56;
                                    const labelPosition = getSliceLabelPosition(
                                        130,
                                        130,
                                        slice.startAngle,
                                        slice.endAngle,
                                        outerRadius,
                                        innerRadius,
                                    );

                                    return (
                                        <G key={slice.id}>
                                            <Path
                                                d={createArcPath(130, 130, outerRadius, innerRadius, slice.startAngle, slice.endAngle)}
                                                fill={slice.color}
                                                opacity={isSelected ? 1 : 0.9}
                                                onPress={() => setSelectedCategoryId(slice.id)}
                                            />
                                            <SvgText
                                                x={labelPosition.x}
                                                y={labelPosition.y}
                                                fontSize={scale(12)}
                                                fontWeight="700"
                                                fill="#FFFFFF"
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
                                <CustomText style={styles.pieCenterAmount}>{formatCurrency(selectedCategory.total)}</CustomText>
                            </View>
                        </View>
                    </View>

                    <View style={styles.detailCard}>
                        <CustomText style={styles.detailTitle}>{`${selectedCategory.label} 일주일 내역`}</CustomText>
                        {selectedCategory.transactions.map((transaction, index) => (
                            <View
                                key={`${selectedCategory.id}-${transaction.day}-${index}`}
                                style={[
                                    styles.detailRow,
                                    index === selectedCategory.transactions.length - 1 && styles.detailRowLast,
                                ]}
                            >
                                <View>
                                    <CustomText style={styles.detailDay}>{transaction.day}</CustomText>
                                    <CustomText style={styles.detailText}>{transaction.title}</CustomText>
                                </View>
                                <CustomText style={styles.detailAmount}>{`-${formatCurrency(transaction.amount)}`}</CustomText>
                            </View>
                        ))}
                    </View>
                </View>
            </ScrollView>
        </SafeAreaView>
    );
};

const styles = StyleSheet.create({
    safeArea: { flex: 1, backgroundColor: '#F3F4F6' },
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
    comparisonText: { fontSize: scale(14), color: '#4B5563' },
    highlightRed: { color: '#EF4444', fontWeight: 'bold' },
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
        height: verticalScale(280),
        borderRadius: scale(16),
        backgroundColor: '#F8FAFC',
        paddingHorizontal: scale(10),
        paddingTop: verticalScale(16),
        paddingBottom: verticalScale(18),
        overflow: 'hidden',
    },
    plotArea: {
        flex: 1,
        position: 'relative',
    },
    barRow: {
        flex: 1,
        flexDirection: 'row',
        alignItems: 'flex-start',
        justifyContent: 'space-between',
    },
    chartMidLine: {
        position: 'absolute',
        left: 0,
        right: 0,
        top: verticalScale(100),
        borderTopWidth: 1.5,
        borderTopColor: '#94A3B8',
    },
    barColumn: {
        width: scale(36),
        alignItems: 'center',
    },
    barValuePositive: {
        fontSize: scale(8),
        color: '#0F766E',
        fontWeight: 'bold',
        marginBottom: verticalScale(6),
        textAlign: 'center',
    },
    barTrack: {
        width: scale(36),
        height: verticalScale(200),
        alignItems: 'center',
        justifyContent: 'space-between',
    },
    positiveZone: {
        width: '100%',
        height: '50%',
        alignItems: 'center',
        justifyContent: 'flex-end',
        paddingBottom: verticalScale(10),
    },
    negativeZone: {
        width: '100%',
        height: '50%',
        alignItems: 'center',
        justifyContent: 'flex-start',
        paddingTop: verticalScale(10),
    },
    positiveBar: {
        width: scale(28),
        backgroundColor: '#34D399',
        borderTopLeftRadius: scale(8),
        borderTopRightRadius: scale(8),
    },
    negativeBar: {
        width: scale(28),
        backgroundColor: '#F87171',
        borderBottomLeftRadius: scale(8),
        borderBottomRightRadius: scale(8),
    },
    barValueNegative: {
        fontSize: scale(8),
        color: '#DC2626',
        fontWeight: 'bold',
        marginTop: verticalScale(6),
        textAlign: 'center',
    },
    dayLabel: {
        marginTop: verticalScale(6),
        fontSize: scale(13),
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
    },
    aiReviewTitle: { fontSize: scale(16), fontWeight: 'bold', color: '#166534', marginBottom: verticalScale(8) },
    aiReviewText: { fontSize: scale(14), color: '#14532D', lineHeight: 22 },
    bodyCopyText: { fontFamily: bodyFontFamily, letterSpacing: 0 },
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
    detailDay: {
        fontSize: scale(12),
        color: '#94A3B8',
        marginBottom: verticalScale(2),
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
