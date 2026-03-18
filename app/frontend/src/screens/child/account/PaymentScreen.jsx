import React, { useState } from 'react';
import { View, StyleSheet, TouchableOpacity, SafeAreaView, Image } from 'react-native';
import { scale, verticalScale } from 'react-native-size-matters';
import CustomText from '../../../components/common/CustomText';

const PaymentScreen = ({ navigation }) => {
    // 혜택 시뮬레이션: 보유 젤링 및결제 시 예상 적립 젤링
    const [currentJellings, setCurrentJellings] = useState(1500);
    const expectedCashback = 50;

    return (
        <SafeAreaView style={styles.safeArea}>
            <View style={styles.header}>
                <TouchableOpacity style={styles.backButton} onPress={() => navigation.goBack()}>
                    <CustomText style={styles.backButtonText}>✕</CustomText>
                </TouchableOpacity>
                <CustomText style={styles.headerTitle}>결제하기</CustomText>
                <View style={{ width: 40 }} />
            </View>

            <View style={styles.container}>
                <View style={styles.jellingBadgeSection}>
                    <CustomText style={styles.jellingBadgeText}>
                        보유 젤링 <CustomText style={styles.jellingAmount}>🍬 {currentJellings}</CustomText>
                    </CustomText>
                </View>

                {/* 메인 결제 카드 (바코드/QR) */}
                <View style={styles.paymentCard}>
                    <CustomText style={styles.paymentTitle}>매장 결제</CustomText>
                    <CustomText style={styles.paymentSubtitle}>가맹점에서 바코드나 QR을 보여주세요</CustomText>

                    <View style={styles.barcodeBox}>
                        {/* 더미 바코드 이미지 영역 */}
                        <View style={styles.dummyBarcode} />
                        <CustomText style={styles.dummyBarcodeText}>|| |||| | ||| | || |||| | |</CustomText>
                        <CustomText style={styles.barcodeNumber}>1234  5678  9012</CustomText>
                    </View>

                    <View style={styles.qrBox}>
                        <View style={styles.dummyQr} />
                    </View>
                </View>

                {/* 하단 혜택 안내 */}
                <View style={styles.benefitCard}>
                    <View style={styles.benefitIconBox}>
                        <CustomText style={styles.benefitIcon}>🎉</CustomText>
                    </View>
                    <View style={styles.benefitTextContent}>
                        <CustomText style={styles.benefitTitle}>결제하면 젤링이 쌓여요!</CustomText>
                        <CustomText style={styles.benefitSubtitle}>
                            이번 결제로 <CustomText style={styles.highlightText}>🍬 {expectedCashback}개</CustomText>를 귀여운 아바타 젤링으로 돌려받아요.
                        </CustomText>
                    </View>
                </View>

            </View>
        </SafeAreaView>
    );
};

const styles = StyleSheet.create({
    safeArea: {
        flex: 1,
        backgroundColor: '#111827', // 결제 화면은 보통 다크 모드 느낌으로 몰입감 제공
    },
    header: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        paddingHorizontal: scale(16),
        paddingVertical: verticalScale(16),
        backgroundColor: '#111827',
    },
    backButton: {
        width: scale(32),
        height: scale(32),
        justifyContent: 'center',
    },
    backButtonText: {
        fontSize: scale(22),
        fontWeight: 'bold',
        color: '#FFFFFF',
    },
    headerTitle: {
        fontSize: scale(18),
        fontWeight: 'bold',
        color: '#FFFFFF',
    },
    container: {
        flex: 1,
        paddingHorizontal: scale(20),
        alignItems: 'center',
        paddingTop: verticalScale(20),
    },
    jellingBadgeSection: {
        backgroundColor: '#374151',
        paddingVertical: verticalScale(8),
        paddingHorizontal: scale(16),
        borderRadius: scale(20),
        marginBottom: verticalScale(24),
    },
    jellingBadgeText: {
        fontSize: scale(14),
        color: '#D1D5DB',
        fontWeight: '600',
    },
    jellingAmount: {
        color: '#F9A8D4', // 핑크색 포인트
        fontWeight: 'bold',
    },
    paymentCard: {
        width: '100%',
        backgroundColor: '#FFFFFF',
        borderRadius: scale(24),
        paddingVertical: verticalScale(32),
        paddingHorizontal: scale(20),
        alignItems: 'center',
        marginBottom: verticalScale(24),
    },
    paymentTitle: {
        fontSize: scale(20),
        fontWeight: 'bold',
        color: '#111',
        marginBottom: verticalScale(8),
    },
    paymentSubtitle: {
        fontSize: scale(13),
        color: '#6B7280',
        marginBottom: verticalScale(32),
    },
    barcodeBox: {
        width: '100%',
        alignItems: 'center',
        marginBottom: verticalScale(24),
    },
    dummyBarcode: {
        width: '80%',
        height: verticalScale(60),
        backgroundColor: '#111',
        marginBottom: verticalScale(8),
    },
    dummyBarcodeText: {
        fontSize: scale(24),
        fontWeight: '900',
        color: '#111',
        letterSpacing: scale(2),
        marginBottom: verticalScale(8),
    },
    barcodeNumber: {
        fontSize: scale(16),
        fontWeight: 'bold',
        color: '#374151',
        letterSpacing: scale(4),
    },
    qrBox: {
        width: scale(120),
        height: scale(120),
        borderWidth: 2,
        borderColor: '#E5E7EB',
        borderRadius: scale(16),
        justifyContent: 'center',
        alignItems: 'center',
    },
    dummyQr: {
        width: '80%',
        height: '80%',
        backgroundColor: '#111',
    },
    benefitCard: {
        width: '100%',
        flexDirection: 'row',
        backgroundColor: '#1F2937',
        borderRadius: scale(16),
        padding: scale(20),
        alignItems: 'center',
    },
    benefitIconBox: {
        width: scale(40),
        height: scale(40),
        borderRadius: scale(20),
        backgroundColor: '#374151',
        justifyContent: 'center',
        alignItems: 'center',
        marginRight: scale(16),
    },
    benefitIcon: {
        fontSize: scale(20),
    },
    benefitTextContent: {
        flex: 1,
    },
    benefitTitle: {
        fontSize: scale(15),
        fontWeight: 'bold',
        color: '#FFFFFF',
        marginBottom: verticalScale(4),
    },
    benefitSubtitle: {
        fontSize: scale(13),
        color: '#9CA3AF',
        lineHeight: scale(18),
    },
    highlightText: {
        color: '#F9A8D4',
        fontWeight: 'bold',
    }
});

export default PaymentScreen;
