import React, { useState, useEffect } from 'react';
import { View, StyleSheet, SafeAreaView, TouchableOpacity, Image, ActivityIndicator } from 'react-native';
import QRCode from 'react-native-qrcode-svg';
import { scale, verticalScale } from 'react-native-size-matters';
import CustomText from '../../../components/common/CustomText';
import api from '../../../api/axios';

const FamilyQrGeneratorScreen = ({ navigation }) => {
    const [timeLeft, setTimeLeft] = useState(300); // 5분

    const [qrData, setQrData] = useState(null);

    useEffect(() => {
        const fetchQr = async () => {
            try {
                const res = await api.get('/families/qr');
                const qrRes = res.data?.data;
                if (!qrRes) return;
                setQrData(qrRes.qrCode);
                setTimeLeft(qrRes.expiresIn || 300);
            } catch (e) { console.error('QR Fetch Error:', e); }
        };
        fetchQr();
    }, []);

    useEffect(() => {
        const timer = setInterval(() => {
            setTimeLeft(prev => prev > 0 ? prev - 1 : 0);
        }, 1000);
        return () => clearInterval(timer);
    }, []);

    const formatTime = (seconds) => {
        const m = Math.floor(seconds / 60);
        const s = seconds % 60;
        return `${m}:${s < 10 ? '0' : ''}${s}`;
    };

    return (
        <SafeAreaView style={styles.safeArea}>
            <View style={styles.header}>
                <TouchableOpacity style={styles.backButton} onPress={() => navigation.goBack()}>
                    <CustomText style={styles.backButtonText}>←</CustomText>
                </TouchableOpacity>
                <CustomText style={styles.headerTitle}>초대용 QR코드</CustomText>
                <View style={{ width: scale(32) }} />
            </View>

            <View style={styles.container}>
                <CustomText style={styles.title}>자녀의 휴대폰에서{`\n`}QR 코드를 스캔해주세요!</CustomText>
                <CustomText style={styles.subtitle}>스캔 시 자동으로 가족 등록이 완료되며,{`\n`}임시 코드를 잃을 위험이 없습니다.</CustomText>

                <View style={styles.qrCard}>
                    <View style={styles.qrImageBox}>
                        {/* 더미 QR 이미지. 실제 구현시 react-native-qrcode-svg 등 사용 */}
                        <Image source={require('../../../assets/qr.png')} style={styles.qrImage} resizeMode="contain" />
                    </View>
                    <CustomText style={styles.timerText}>인증 유효시간 {formatTime(timeLeft)}</CustomText>
                </View>

                <TouchableOpacity style={styles.refreshBtn} onPress={async () => {
                    try {
                        const res = await api.post('/families/qr/reissue');
                        const qrRes = res.data?.data;
                        if (!qrRes) return;
                        setQrData(qrRes.qrCode);
                        setTimeLeft(qrRes.expiresIn || 300);
                    } catch (e) { console.error('QR Reissue Error', e); }
                }}>
                    <CustomText style={styles.refreshBtnText}>🔄 QR 코드 갱신</CustomText>
                </TouchableOpacity>
            </View>
        </SafeAreaView>
    );
};

const styles = StyleSheet.create({
    safeArea: { flex: 1, backgroundColor: '#FFFFFF' },
    header: {
        flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between',
        paddingHorizontal: scale(16), paddingVertical: verticalScale(16)
    },
    backButton: { width: scale(32), height: scale(32), justifyContent: 'center' },
    backButtonText: { fontSize: scale(22), fontWeight: 'bold', color: '#111' },
    headerTitle: { fontSize: scale(18), fontWeight: 'bold', color: '#111' },

    container: { flex: 1, paddingHorizontal: scale(24), paddingTop: verticalScale(30), alignItems: 'center' },

    title: { fontSize: scale(20), fontWeight: '900', color: '#111', lineHeight: 30, textAlign: 'center', marginBottom: verticalScale(12) },
    subtitle: { fontSize: scale(14), color: '#6B7280', textAlign: 'center', lineHeight: 20, marginBottom: verticalScale(40) },

    qrCard: { backgroundColor: '#F9FAFB', padding: scale(32), borderRadius: scale(24), alignItems: 'center', shadowColor: '#000', shadowOffset: { width: 0, height: verticalScale(4) }, shadowOpacity: 0.1, shadowRadius: scale(8), elevation: 4 },
    qrImageBox: { width: scale(180), height: scale(180), backgroundColor: '#FFFFFF', borderRadius: scale(16), padding: scale(16), marginBottom: verticalScale(20), justifyContent: 'center', alignItems: 'center' },
    qrImage: { width: '100%', height: '100%' },
    timerText: { fontSize: scale(16), fontWeight: 'bold', color: '#EF4444' },

    refreshBtn: { marginTop: verticalScale(32), padding: scale(12), borderRadius: scale(12), backgroundColor: '#F3F4F6' },
    refreshBtnText: { fontSize: scale(14), fontWeight: 'bold', color: '#4B5563' }
});

export default FamilyQrGeneratorScreen;
