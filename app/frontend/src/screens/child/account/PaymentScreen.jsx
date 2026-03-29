import React, { useState, useEffect, useCallback } from 'react';
import { View, StyleSheet, TouchableOpacity, SafeAreaView, Alert, ActivityIndicator, DeviceEventEmitter, Image } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { scale, verticalScale } from 'react-native-size-matters';
import CustomText from '../../../components/common/CustomText';
import api from '../../../api/axios';

const PaymentScreen = ({ navigation }) => {
    // 혜택 시뮬레이션: 보유 젤링 및 결제 시 예상 적립 젤링
    const [currentJellings, setCurrentJellings] = useState(0);
    const expectedCashback = 50;

    const [myCardId, setMyCardId] = useState(null);
    const [bankBalance, setBankBalance] = useState(null);
    const [isPaying, setIsPaying] = useState(false);

    useFocusEffect(
        useCallback(() => {
            const fetchData = async () => {
                try {
                    const [cardRes, accRes, hubRes] = await Promise.all([
                        api.get('/bank/cards/my'),
                        api.get('/bank/accounts/me'),
                        api.get('/jelling-hub')
                    ]);

                    if (hubRes.data?.data?.remainJelling !== undefined) {
                        setCurrentJellings(hubRes.data.data.remainJelling);
                    }

                    const cards = cardRes.data?.data || [];
                    if (cards.length > 0) setMyCardId(cards[0].id);

                    const accounts = accRes.data?.data?.accounts || [];
                    if (accounts.length > 0) {
                        const primary = accounts.find(a => a.isPrimary) || accounts[0];
                        setBankBalance(primary.balance);
                    }
                } catch (error) {
                    console.error('Payment Screen Fetch Error:', error);
                }
            };
            fetchData();
        }, [])
    );

    const handleMockPayment = async () => {
        if (!myCardId) {
            Alert.alert('알림', '결제할 수 있는 카드가 존재하지 않습니다. 먼저 카드를 발급받아 주세요!');
            return;
        }

        try {
            setIsPaying(true);
            // 가상 결제 데모 (가게ID 16350번, 금액 1500원)
            await api.post('/bank/cards/payment', {
                cardId: myCardId,
                merchantId: 16350,
                paymentBalance: 1500
            });

            // 결제 성공 후 즉시 내역 갱신 이벤트 발행
            DeviceEventEmitter.emit('refresh_transactions');

            Alert.alert('결제 성공', '1,500원 결제가 완료되었습니다!', [
                { text: '확인', onPress: () => navigation.goBack() }
            ]);
        } catch (error) {
            console.error('Payment Error:', error.response?.data || error.message);
            Alert.alert('결제 실패', error.response?.data?.message || '잔액이 부족하거나 결제 서버 오류입니다.');
        } finally {
            setIsPaying(false);
        }
    };

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
                        보유 잔액 <CustomText style={styles.bankBalanceAmount}>{bankBalance !== null ? `${bankBalance.toLocaleString()}원` : '조회 중...'}</CustomText>
                    </CustomText>
                    <View style={styles.badgeDivider} />
                    <CustomText style={styles.jellingBadgeText}>
                        보유 젤링 <CustomText style={styles.jellingAmount}> {currentJellings}</CustomText>
                    </CustomText>
                </View>

                {/* 메인 결제 카드 (바코드/QR) */}
                <View style={styles.paymentCard}>
                    <CustomText style={styles.paymentTitle}>매장 결제</CustomText>
                    <CustomText style={styles.paymentSubtitle}>가맹점에서 바코드나 QR을 보여주세요</CustomText>

                    <View style={styles.barcodeBox}>
                        {/* 더미 바코드 이미지 영역 */}
                        <View style={styles.barcodePatternRow}>
                            {[4, 1, 5, 2, 3, 6, 2, 1, 4, 5, 1, 4, 2, 6, 2, 1, 3, 4, 2].map((w, i) => (
                                <View key={i} style={{ width: scale(w), height: '100%', backgroundColor: '#111' }} />
                            ))}
                        </View>
                        <CustomText style={styles.barcodeNumber}>1234  5678  9012</CustomText>
                    </View>

                    <View style={styles.qrBox}>
                        <Image source={require('../../../assets/qr-white.png')} style={styles.dummyQrImage} resizeMode="contain" />
                    </View>

                    <CustomText style={styles.qrBenefitText}>결제하면 젤링이 쌓여요!</CustomText>

                    {/* 데모용 바코드 스캔 시뮬레이션 버튼 */}
                    <TouchableOpacity
                        style={[styles.mockPayBtn, isPaying && { opacity: 0.7 }]}
                        onPress={handleMockPayment}
                        disabled={isPaying}
                    >
                        {isPaying ? <ActivityIndicator color="#111" /> : <CustomText style={styles.mockPayBtnText}>결제 스캔</CustomText>}
                    </TouchableOpacity>
                </View>

            </View>
        </SafeAreaView>
    );
};

const styles = StyleSheet.create({
    safeArea: {
        flex: 1,
        backgroundColor: '#111827',
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
        flexDirection: 'row',
        backgroundColor: '#374151',
        paddingVertical: verticalScale(10),
        paddingHorizontal: scale(20),
        borderRadius: scale(25),
        marginBottom: verticalScale(24),
        alignItems: 'center',
        shadowColor: '#000',
        shadowOffset: { width: 0, height: verticalScale(2) },
        shadowOpacity: 0.6,
        shadowRadius: scale(12),
        elevation: 5,
    },
    jellingBadgeText: {
        fontSize: scale(14),
        color: '#D1D5DB',
        fontWeight: '600',
    },
    bankBalanceAmount: {
        color: '#A3E635',
        fontWeight: 'bold',
    },
    jellingAmount: {
        color: '#A3E635',
        fontWeight: 'bold',
    },
    badgeDivider: {
        width: 1,
        height: scale(14),
        backgroundColor: '#4B5563',
        marginHorizontal: scale(12),
    },
    paymentCard: {
        width: '100%',
        backgroundColor: '#FFFFFF',
        borderRadius: scale(24),
        paddingVertical: verticalScale(32),
        paddingHorizontal: scale(20),
        alignItems: 'center',
        marginBottom: verticalScale(24),
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 4 },
        shadowOpacity: 0.6,
        shadowRadius: 10,
        elevation: 5,
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
    barcodePatternRow: {
        width: '80%',
        height: verticalScale(50),
        flexDirection: 'row',
        justifyContent: 'space-between',
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
        backgroundColor: '#FFFFFF',
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 4 },
        shadowOpacity: 0.6,
        shadowRadius: 10,
        elevation: 5,
    },
    dummyQrImage: {
        width: '85%',
        height: '85%',
        borderRadius: scale(8),
    },
    qrBenefitText: {
        fontSize: scale(14),
        fontWeight: 'bold',
        color: '#000000ff',
        marginTop: verticalScale(16),
        marginBottom: verticalScale(2),
    },
    mockPayBtn: {
        marginTop: verticalScale(20),
        width: '100%',
        backgroundColor: '#A3E635',
        paddingVertical: verticalScale(14),
        borderRadius: scale(12),
        alignItems: 'center',
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 4 },
        shadowOpacity: 0.6,
        shadowRadius: 10,
        elevation: 5,
    },
    mockPayBtnText: {
        fontSize: scale(15),
        fontWeight: 'bold',
        color: '#111',
    }
});

export default PaymentScreen;
