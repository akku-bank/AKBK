import React, { useState } from 'react';
import { View, StyleSheet, SafeAreaView, TouchableOpacity, ScrollView, Alert } from 'react-native';
import { scale, verticalScale } from 'react-native-size-matters';
import CustomText from '../../../components/common/CustomText';

// 임시 로컬 상태 (API 연동시 zustand나 서버 상태로 교체)
const MOCK_PROFILES = [
    { id: 101, name: '김싸피', status: '연동 대기중' },
    { id: 102, name: '이싸피', status: '연동 완료' },
];

const FamilyManagementScreen = ({ navigation }) => {
    const [profiles, setProfiles] = useState(MOCK_PROFILES);

    const handleCreateProfile = () => {
        // 원래는 ParentAccountCreateScreen 이나 다른 생성 화면으로 이동
        // 여기서는 간단히 프롬프트 대신 Mock 으로 추가
        const newProfile = { id: Date.now(), name: `아이 ${profiles.length + 1}`, status: '연동 대기중' };
        setProfiles([...profiles, newProfile]);
        Alert.alert('프로필 생성 완료', `${newProfile.name}의 프로필이 생성되었습니다.`);
    };

    return (
        <SafeAreaView style={styles.safeArea}>
            <View style={styles.header}>
                <TouchableOpacity style={styles.backButton} onPress={() => navigation.goBack()}>
                    <CustomText style={styles.backButtonText}>←</CustomText>
                </TouchableOpacity>
                <CustomText style={styles.headerTitle}>가족 관리 센터</CustomText>
                <View style={{ width: scale(32) }} />
            </View>

            <ScrollView contentContainerStyle={styles.container}>
                <View style={styles.sectionHeader}>
                    <CustomText style={styles.sectionTitle}>내 아이 프로필</CustomText>
                    <TouchableOpacity onPress={handleCreateProfile}>
                        <CustomText style={styles.addText}>+ 프로필 추가</CustomText>
                    </TouchableOpacity>
                </View>

                {profiles.map(profile => (
                    <View key={profile.id} style={styles.profileCard}>
                        <View>
                            <CustomText style={styles.profileName}>{profile.name}</CustomText>
                            <CustomText style={[styles.profileStatus, profile.status === '연동 완료' ? styles.statusActive : styles.statusPending]}>
                                {profile.status}
                            </CustomText>
                        </View>
                        {profile.status === '연동 대기중' && (
                            <TouchableOpacity
                                style={styles.qrButton}
                                onPress={() => navigation.navigate('FamilyQrGenerator', { childId: profile.id })}
                            >
                                <CustomText style={styles.qrButtonText}>QR 생성</CustomText>
                            </TouchableOpacity>
                        )}
                        {profile.status === '연동 완료' && (
                            <TouchableOpacity
                                style={styles.detailButton}
                                onPress={() => navigation.navigate('ParentHistoryScreen', { childName: profile.name })}
                            >
                                <CustomText style={styles.detailButtonText}>내역 보기</CustomText>
                            </TouchableOpacity>
                        )}
                    </View>
                ))}

                <View style={styles.infoBox}>
                    <CustomText style={styles.infoTitle}>💡 가족 계정 연동이란?</CustomText>
                    <CustomText style={styles.infoText}>아이의 프로필을 생성한 후 QR 코드를 생성하세요. 아이의 기기에서 해당 QR을 스캔하면 계정이 안전하게 연결됩니다.</CustomText>
                </View>

            </ScrollView>
        </SafeAreaView>
    );
};

const styles = StyleSheet.create({
    safeArea: { flex: 1, backgroundColor: '#F3F4F6' },
    header: {
        flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between',
        paddingHorizontal: scale(16), paddingVertical: verticalScale(16),
        backgroundColor: '#FFFFFF',
    },
    backButton: { width: scale(32), height: scale(32), justifyContent: 'center' },
    backButtonText: { fontSize: scale(22), fontWeight: 'bold', color: '#111' },
    headerTitle: { fontSize: scale(18), fontWeight: 'bold', color: '#111' },

    container: { flexGrow: 1, padding: scale(16) },

    sectionHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: verticalScale(16) },
    sectionTitle: { fontSize: scale(18), fontWeight: 'bold', color: '#111' },
    addText: { fontSize: scale(14), fontWeight: 'bold', color: '#3B82F6' },

    profileCard: {
        flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center',
        backgroundColor: '#FFFFFF', padding: scale(20), borderRadius: scale(16),
        marginBottom: verticalScale(12),
        elevation: 2, shadowColor: '#000', shadowOffset: { width: 0, height: verticalScale(2) }, shadowOpacity: 0.05, shadowRadius: scale(8),
    },
    profileName: { fontSize: scale(16), fontWeight: 'bold', color: '#111', marginBottom: verticalScale(4) },
    profileStatus: { fontSize: scale(13), fontWeight: '600' },
    statusPending: { color: '#F59E0B' }, // 주황색
    statusActive: { color: '#10B981' }, // 초록색

    qrButton: { backgroundColor: '#F3F4F6', paddingHorizontal: scale(16), paddingVertical: verticalScale(8), borderRadius: scale(12) },
    qrButtonText: { fontSize: scale(14), fontWeight: 'bold', color: '#4B5563' },

    detailButton: { backgroundColor: '#EFF6FF', paddingHorizontal: scale(16), paddingVertical: verticalScale(8), borderRadius: scale(12) },
    detailButtonText: { fontSize: scale(14), fontWeight: 'bold', color: '#3B82F6' },

    infoBox: { marginTop: verticalScale(32), backgroundColor: '#EFF6FF', padding: scale(16), borderRadius: scale(12) },
    infoTitle: { fontSize: scale(14), fontWeight: 'bold', color: '#1E3A8A', marginBottom: verticalScale(8) },
    infoText: { fontSize: scale(13), color: '#1E40AF', lineHeight: 20 },
});

export default FamilyManagementScreen;
