import React, { useState, useEffect } from 'react';
import { View, StyleSheet, SafeAreaView, TouchableOpacity, Modal, TextInput, Alert } from 'react-native';
import QRCode from 'react-native-qrcode-svg';
import { scale, verticalScale } from 'react-native-size-matters';
import CustomText from '../../../components/common/CustomText';
import api from '../../../api/axios';

const FamilyQrGeneratorScreen = ({ navigation }) => {
    const [timeLeft, setTimeLeft] = useState(300); // 5분
    const [qrData, setQrData] = useState(null);

    const [isModalVisible, setModalVisible] = useState(false);
    const [memberName, setMemberName] = useState('');
    const [memberBirth, setMemberBirth] = useState('');

    const syncQrState = (qrRes) => {
        if (!qrRes?.qrCode) {
            return;
        }

        setQrData(qrRes.qrCode);

        if (qrRes.qrExpiresAt) {
            const remainingSeconds = Math.max(
                0,
                Math.floor((new Date(qrRes.qrExpiresAt).getTime() - Date.now()) / 1000)
            );
            setTimeLeft(remainingSeconds);
            return;
        }

        setTimeLeft(300);
    };

    const handleAddMember = async () => {
        if (!memberName || !memberBirth) {
            Alert.alert('알림', '이름과 생년월일을 모두 입력해주세요. (YYYY-MM-DD)');
            return;
        }

        // 날짜 형식 검증 (YYYY-MM-DD)
        const dateRegex = /^\d{4}-\d{2}-\d{2}$/;
        if (!dateRegex.test(memberBirth)) {
            Alert.alert('알림', '생년월일 형식을 YYYY-MM-DD (예: 2015-05-05)로 맞춰주세요.');
            return;
        }

        try {
            await api.post('/families/members', {
                name: memberName,
                role: 'CHILD',
                birthDate: memberBirth
            });
            Alert.alert('성공', '가족 구성원이 추가되었습니다! 이제 자녀 폰에서 화면의 공유 코드를 입력해 주세요.');
            setModalVisible(false);
            setMemberName('');
            setMemberBirth('');
        } catch (e) {
            Alert.alert('오류', '구성원 사전 등록에 실패했습니다.');
            console.error('Add Member Error:', e);
        }
    };

    useEffect(() => {
        const fetchQr = async () => {
            try {
                const res = await api.get('/families/qr');
                syncQrState(res.data?.data);
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
                <CustomText style={styles.title}>QR 코드를 스캔해주세요!</CustomText>
                <CustomText style={styles.subtitle}>스캔 시 자동으로 가족 등록이 완료됩니다.</CustomText>
                <CustomText style={styles.title}>QR 코드를 스캔해주세요!</CustomText>
                <CustomText style={styles.subtitle}>스캔 시 자동으로 가족 등록이 완료됩니다.</CustomText>

                <View style={styles.qrCard}>
                    <View style={styles.qrImageBox}>
                        {qrData ? (
                            <QRCode value={qrData} size={scale(148)} />
                        ) : (
                            <CustomText style={styles.qrLoadingText}>QR 코드를 불러오는 중입니다.</CustomText>
                        )}
                    </View>
                    <CustomText style={styles.timerText}>인증 유효시간 {formatTime(timeLeft)}</CustomText>
                    {qrData && (
                        <CustomText style={{ marginTop: 12, color: '#6B7280', fontSize: scale(14), fontWeight: 'bold' }}>
                            [개발용 우회] {qrData}
                        </CustomText>
                    )}
                    {qrData && (
                        <CustomText style={{ marginTop: 12, color: '#6B7280', fontSize: scale(14), fontWeight: 'bold' }}>
                            [개발용 우회] {qrData}
                        </CustomText>
                    )}
                </View>

                <TouchableOpacity style={styles.refreshBtn} onPress={async () => {
                    try {
                        const res = await api.post('/families/qr/reissue');
                        syncQrState(res.data?.data);
                    } catch (e) { console.error('QR Reissue Error', e); }
                }}>
                    <CustomText style={styles.refreshBtnText}>🔄 QR 코드 갱신</CustomText>
                </TouchableOpacity>

                <TouchableOpacity style={[styles.refreshBtn, { backgroundColor: '#3B82F6', marginTop: verticalScale(16), paddingHorizontal: scale(20) }]} onPress={() => setModalVisible(true)}>
                    <CustomText style={[styles.refreshBtnText, { color: '#FFFFFF' }]}>+ 초대받을 사람(자녀) 사전 추가하기</CustomText>
                </TouchableOpacity>

                <TouchableOpacity style={[styles.refreshBtn, { backgroundColor: '#3B82F6', marginTop: verticalScale(16), paddingHorizontal: scale(20) }]} onPress={() => setModalVisible(true)}>
                    <CustomText style={[styles.refreshBtnText, { color: '#FFFFFF' }]}>+ 초대받을 사람(자녀) 사전 추가하기</CustomText>
                </TouchableOpacity>
            </View>

            {/* 자녀 사전 등록 모달 */}
            <Modal visible={isModalVisible} transparent={true} animationType="fade">
                <View style={styles.modalOverlay}>
                    <View style={styles.modalContent}>
                        <CustomText style={styles.modalTitle}>자녀 사전 등록</CustomText>
                        <CustomText style={styles.modalSubtitle}>초대할 자녀의 정보를 등록해주세요.</CustomText>

                        <TextInput
                            style={styles.input}
                            placeholder="이름 (예: 김싸피)"
                            placeholderTextColor="#9CA3AF"
                            value={memberName}
                            onChangeText={setMemberName}
                        />
                        <TextInput
                            style={styles.input}
                            placeholder="생년월일 (예: 2015-05-05)"
                            placeholderTextColor="#9CA3AF"
                            value={memberBirth}
                            onChangeText={setMemberBirth}
                        />

                        <View style={styles.modalBtnRow}>
                            <TouchableOpacity style={[styles.modalBtn, { backgroundColor: '#F3F4F6' }]} onPress={() => setModalVisible(false)}>
                                <CustomText style={{ color: '#4B5563', fontWeight: 'bold' }}>취소</CustomText>
                            </TouchableOpacity>
                            <TouchableOpacity style={[styles.modalBtn, { backgroundColor: '#3B82F6' }]} onPress={handleAddMember}>
                                <CustomText style={{ color: '#FFFFFF', fontWeight: 'bold' }}>명단에 등록하기</CustomText>
                            </TouchableOpacity>
                        </View>
                    </View>
                </View>
            </Modal>

            {/* 자녀 사전 등록 모달 */}
            <Modal visible={isModalVisible} transparent={true} animationType="fade">
                <View style={styles.modalOverlay}>
                    <View style={styles.modalContent}>
                        <CustomText style={styles.modalTitle}>자녀 사전 등록</CustomText>
                        <CustomText style={styles.modalSubtitle}>초대할 자녀의 정보를 등록해주세요.</CustomText>

                        <TextInput
                            style={styles.input}
                            placeholder="이름 (예: 김싸피)"
                            placeholderTextColor="#9CA3AF"
                            value={memberName}
                            onChangeText={setMemberName}
                        />
                        <TextInput
                            style={styles.input}
                            placeholder="생년월일 (예: 2015-05-05)"
                            placeholderTextColor="#9CA3AF"
                            value={memberBirth}
                            onChangeText={setMemberBirth}
                        />

                        <View style={styles.modalBtnRow}>
                            <TouchableOpacity style={[styles.modalBtn, { backgroundColor: '#F3F4F6' }]} onPress={() => setModalVisible(false)}>
                                <CustomText style={{ color: '#4B5563', fontWeight: 'bold' }}>취소</CustomText>
                            </TouchableOpacity>
                            <TouchableOpacity style={[styles.modalBtn, { backgroundColor: '#3B82F6' }]} onPress={handleAddMember}>
                                <CustomText style={{ color: '#FFFFFF', fontWeight: 'bold' }}>명단에 등록하기</CustomText>
                            </TouchableOpacity>
                        </View>
                    </View>
                </View>
            </Modal>
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
    qrLoadingText: { fontSize: scale(14), color: '#6B7280', textAlign: 'center', lineHeight: 20 },
    timerText: { fontSize: scale(16), fontWeight: 'bold', color: '#EF4444' },

    refreshBtn: { marginTop: verticalScale(32), padding: scale(12), borderRadius: scale(12), backgroundColor: '#F3F4F6' },
    refreshBtnText: { fontSize: scale(14), fontWeight: 'bold', color: '#4B5563' },

    modalOverlay: { flex: 1, backgroundColor: 'rgba(0,0,0,0.5)', justifyContent: 'center', alignItems: 'center' },
    modalContent: { width: '85%', backgroundColor: '#FFF', borderRadius: scale(20), padding: scale(24), alignItems: 'center' },
    modalTitle: { fontSize: scale(18), fontWeight: 'bold', color: '#111', marginBottom: verticalScale(8) },
    modalSubtitle: { fontSize: scale(14), color: '#6B7280', marginBottom: verticalScale(20), textAlign: 'center' },
    input: { width: '100%', backgroundColor: '#F9FAFB', borderWidth: 1, borderColor: '#E5E7EB', borderRadius: scale(12), padding: scale(14), fontSize: scale(14), marginBottom: verticalScale(12), color: '#111' },
    modalBtnRow: { flexDirection: 'row', justifyContent: 'space-between', width: '100%', marginTop: verticalScale(16) },
    modalBtn: { flex: 1, paddingVertical: verticalScale(14), borderRadius: scale(12), alignItems: 'center', marginHorizontal: scale(4) }
    refreshBtnText: { fontSize: scale(14), fontWeight: 'bold', color: '#4B5563' },

    modalOverlay: { flex: 1, backgroundColor: 'rgba(0,0,0,0.5)', justifyContent: 'center', alignItems: 'center' },
    modalContent: { width: '85%', backgroundColor: '#FFF', borderRadius: scale(20), padding: scale(24), alignItems: 'center' },
    modalTitle: { fontSize: scale(18), fontWeight: 'bold', color: '#111', marginBottom: verticalScale(8) },
    modalSubtitle: { fontSize: scale(14), color: '#6B7280', marginBottom: verticalScale(20), textAlign: 'center' },
    input: { width: '100%', backgroundColor: '#F9FAFB', borderWidth: 1, borderColor: '#E5E7EB', borderRadius: scale(12), padding: scale(14), fontSize: scale(14), marginBottom: verticalScale(12), color: '#111' },
    modalBtnRow: { flexDirection: 'row', justifyContent: 'space-between', width: '100%', marginTop: verticalScale(16) },
    modalBtn: { flex: 1, paddingVertical: verticalScale(14), borderRadius: scale(12), alignItems: 'center', marginHorizontal: scale(4) }
});

export default FamilyQrGeneratorScreen;
