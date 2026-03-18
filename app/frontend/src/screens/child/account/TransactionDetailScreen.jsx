import React from 'react';
import { View, StyleSheet, TouchableOpacity, SafeAreaView, Switch } from 'react-native';
import { scale, verticalScale } from 'react-native-size-matters';
import useTransactionStore from '../../../store/transactionStore';
import CustomText from '../../../components/common/CustomText';

// 임시 단건 결제 내역 데이터 (나중에는 route.params로 전달받음)
const FALLBACK_DETAIL = {
    id: '1',
    date: '2024.03.12 14:30',
    title: '다이소 강남점',
    amount: -5000,
    type: 'PAYMENT',
    category: '쇼핑',
    memo: '엄마 생일 선물 구매',
};

const TransactionDetailScreen = ({ route, navigation }) => {
    const { transaction } = route?.params || {};
    const detailData = transaction || FALLBACK_DETAIL;

    const hiddenTransactionIds = useTransactionStore(state => state.hiddenTransactionIds);
    const toggleHideTransaction = useTransactionStore(state => state.toggleHideTransaction);

    const isPrivate = hiddenTransactionIds.includes(detailData.id);
    const isOver14 = true; // 서버 연동 시 유저 나이로 판단

    const isDeposit = detailData.amount > 0;

    return (
        <SafeAreaView style={styles.safeArea}>
            {/* 상단 헤더 */}
            <View style={styles.header}>
                <TouchableOpacity style={styles.backButton} onPress={() => navigation.goBack()}>
                    <CustomText style={styles.backButtonText}>←</CustomText>
                </TouchableOpacity>
                <CustomText style={styles.headerTitle}>거래 상세</CustomText>
                <View style={{ width: 40 }} />
            </View>

            <View style={styles.container}>
                {/* 메인 상세 카드 */}
                <View style={styles.detailCard}>
                    <View style={styles.iconWrapper}>
                        <CustomText style={styles.mainIcon}>{isDeposit ? '💰' : '🏪'}</CustomText>
                    </View>

                    <CustomText style={styles.detailTitle}>{detailData.title}</CustomText>
                    <CustomText style={[styles.detailAmount, isDeposit ? styles.depositColor : styles.withdrawColor]}>
                        {isDeposit ? '+' : ''}{detailData.amount.toLocaleString()}원
                    </CustomText>

                    <View style={styles.divider} />

                    <View style={styles.infoRow}>
                        <CustomText style={styles.infoLabel}>일시</CustomText>
                        <CustomText style={styles.infoValue}>{detailData.date || detailData.time}</CustomText>
                    </View>

                    <View style={styles.infoRow}>
                        <CustomText style={styles.infoLabel}>카테고리</CustomText>
                        <CustomText style={styles.infoValue}>{detailData.category || '기타'}</CustomText>
                    </View>

                    <View style={styles.infoRow}>
                        <CustomText style={styles.infoLabel}>메모</CustomText>
                        <CustomText style={styles.infoValue}>{detailData.memo || '-'}</CustomText>
                    </View>
                </View>

                {/* 14세 이상 프라이버시 설정 카드 */}
                {isOver14 && (
                    <View style={styles.privacyCard}>
                        <View style={styles.privacyTextContent}>
                            <CustomText style={styles.privacyTitle}>부모님께 내역 숨기기</CustomText>
                            <CustomText style={styles.privacySubtitle}>14세 이상은 거래 내역을 숨길 수 있어요.</CustomText>
                        </View>
                        <Switch
                            trackColor={{ false: '#D1D5DB', true: '#3B82F6' }}
                            thumbColor={'#FFFFFF'}
                            ios_backgroundColor="#D1D5DB"
                            onValueChange={() => toggleHideTransaction(detailData.id)}
                            value={isPrivate}
                        />
                    </View>
                )}

                {/* 추가 액션 버튼 (명세 표기용) */}
                <TouchableOpacity style={styles.memoButton}>
                    <CustomText style={styles.memoButtonText}>메모 남기기</CustomText>
                </TouchableOpacity>

            </View>
        </SafeAreaView>
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
        flex: 1,
        paddingHorizontal: scale(16),
        paddingTop: verticalScale(20),
    },
    detailCard: {
        backgroundColor: '#FFFFFF',
        borderRadius: scale(20),
        padding: scale(24),
        alignItems: 'center',
        marginBottom: verticalScale(16),
        shadowColor: '#000',
        shadowOffset: { width: 0, height: verticalScale(2) },
        shadowOpacity: 0.05,
        shadowRadius: scale(8),
        elevation: 2,
    },
    iconWrapper: {
        width: scale(64),
        height: scale(64),
        borderRadius: scale(32),
        backgroundColor: '#F3F4F6',
        justifyContent: 'center',
        alignItems: 'center',
        marginBottom: verticalScale(16),
    },
    mainIcon: {
        fontSize: scale(32),
    },
    detailTitle: {
        fontSize: scale(18),
        fontWeight: '600',
        color: '#111',
        marginBottom: verticalScale(8),
    },
    detailAmount: {
        fontSize: scale(32),
        fontWeight: 'bold',
        marginBottom: verticalScale(24),
    },
    depositColor: {
        color: '#3B82F6',
    },
    withdrawColor: {
        color: '#111',
    },
    divider: {
        width: '100%',
        height: 1,
        backgroundColor: '#F3F4F6',
        marginBottom: verticalScale(20),
    },
    infoRow: {
        flexDirection: 'row',
        width: '100%',
        justifyContent: 'space-between',
        marginBottom: verticalScale(16),
    },
    infoLabel: {
        fontSize: scale(14),
        color: '#6B7280',
        fontWeight: '500',
    },
    infoValue: {
        fontSize: scale(14),
        color: '#111',
        fontWeight: '600',
    },
    privacyCard: {
        flexDirection: 'row',
        backgroundColor: '#FFFFFF',
        borderRadius: scale(16),
        padding: scale(20),
        alignItems: 'center',
        justifyContent: 'space-between',
        marginBottom: verticalScale(16),
        shadowColor: '#000',
        shadowOffset: { width: 0, height: verticalScale(2) },
        shadowOpacity: 0.05,
        shadowRadius: scale(8),
        elevation: 2,
    },
    privacyTextContent: {
        flex: 1,
    },
    privacyTitle: {
        fontSize: scale(15),
        fontWeight: 'bold',
        color: '#111',
        marginBottom: verticalScale(4),
    },
    privacySubtitle: {
        fontSize: scale(12),
        color: '#6B7280',
    },
    memoButton: {
        backgroundColor: '#E5E7EB',
        borderRadius: scale(12),
        paddingVertical: verticalScale(14),
        alignItems: 'center',
    },
    memoButtonText: {
        fontSize: scale(15),
        fontWeight: 'bold',
        color: '#4B5563',
    }
});

export default TransactionDetailScreen;
