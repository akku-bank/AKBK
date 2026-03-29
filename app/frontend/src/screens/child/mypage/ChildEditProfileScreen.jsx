import React, { useState, useEffect } from 'react';
import { View, StyleSheet, SafeAreaView, TouchableOpacity, Alert, ScrollView, Image } from 'react-native';
import { scale, verticalScale } from 'react-native-size-matters';
import CustomText from '../../../components/common/CustomText';
import CustomTextInput from '../../../components/common/CustomTextInput';
import api from '../../../api/axios';
import useAuthStore from '../../../store/useAuthStore';

const ChildEditProfileScreen = ({ navigation }) => {
    const { user, setUser } = useAuthStore();
    const [nickname, setNickname] = useState(user?.name || '');
    const [statusMessage, setStatusMessage] = useState('돈을 아끼자!');

    useEffect(() => {
        if (user?.name) {
            setNickname(user.name);
        }
    }, [user?.name]);

    useEffect(() => {
        // 기타 마이페이지 데이터가 있다면 여기서 페칭
    }, []);

    const handleSave = async () => {
        if (!nickname.trim()) {
            Alert.alert('알림', '닉네임을 입력해주세요.');
            return;
        }

        try {
            await api.patch('/users/me', { name: nickname });
            if (user) {
                setUser({ ...user, name: nickname });
            }
            Alert.alert('저장 완료', '프로필이 수정되었습니다.', [{ text: '확인', onPress: () => navigation.goBack() }]);
        } catch (error) {
            Alert.alert('오류', '프로필 수정에 실패했습니다.');
        }
    };

    return (
        <SafeAreaView style={styles.safeArea}>
            <View style={styles.header}>
                <TouchableOpacity style={styles.backButton} onPress={() => navigation.goBack()}>
                    <CustomText style={styles.backButtonText}>←</CustomText>
                </TouchableOpacity>
                <CustomText style={styles.headerTitle}>프로필 수정</CustomText>
                <View style={{ width: scale(32) }} />
            </View>

            <ScrollView style={{ backgroundColor: '#ECFCCB' }} contentContainerStyle={styles.container}>
                <View style={styles.avatarSection}>
                    <View style={styles.avatarCircle}>
                        <Image source={require('../../../assets/croco/croco_face.png')} style={styles.avatarImage} resizeMode="contain" />
                    </View>
                </View>

                <View style={styles.inputSection}>
                    <CustomText style={styles.label}>닉네임</CustomText>
                    <CustomTextInput
                        style={styles.input}
                        value={nickname}
                        onChangeText={setNickname}
                        maxLength={10}
                        placeholder="닉네임을 입력하세요"
                        placeholderTextColor="#9CA3AF"
                    />

                    <CustomText style={styles.label}>상태 메시지</CustomText>
                    <CustomTextInput
                        style={styles.input}
                        value={statusMessage}
                        onChangeText={setStatusMessage}
                        maxLength={20}
                        placeholder="상태를 입력하세요"
                        placeholderTextColor="#9CA3AF"
                    />
                </View>

            </ScrollView>

            <View style={styles.footer}>
                <TouchableOpacity style={styles.saveButton} onPress={handleSave}>
                    <CustomText style={styles.saveButtonText}>저장하기</CustomText>
                </TouchableOpacity>
            </View>
        </SafeAreaView>
    );
};

const styles = StyleSheet.create({
    safeArea: { flex: 1, backgroundColor: '#ECFCCB' },
    header: {
        flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between',
        paddingHorizontal: scale(16), paddingVertical: verticalScale(16), backgroundColor: '#FFFFFF'
    },
    backButton: { width: scale(32), height: scale(32), justifyContent: 'center' },
    backButtonText: { fontSize: scale(22), fontWeight: 'bold', color: '#111' },
    headerTitle: { fontSize: scale(18), fontWeight: 'bold', color: '#111' },

    container: { flexGrow: 1, paddingHorizontal: scale(24), paddingTop: verticalScale(20) },

    avatarSection: { alignItems: 'center', marginBottom: verticalScale(40) },
    avatarCircle: { width: scale(100), height: scale(100), borderRadius: scale(50), backgroundColor: '#FFFFFF', justifyContent: 'center', alignItems: 'center', marginBottom: verticalScale(16), shadowColor: '#000', shadowOffset: { width: 0, height: verticalScale(4) }, shadowOpacity: 0.1, shadowRadius: scale(8), elevation: 4 },
    avatarImage: { width: '110%', height: '110%', marginTop: verticalScale(40), marginLeft: verticalScale(10) },
    avatarEditBtn: { paddingVertical: verticalScale(8), paddingHorizontal: scale(16), backgroundColor: '#F9FAFB', borderRadius: scale(20), shadowColor: '#000', shadowOffset: { width: 0, height: 2 }, shadowOpacity: 0.05, shadowRadius: 4, elevation: 1, },
    avatarEditBtnText: { fontSize: scale(13), fontWeight: 'bold', color: '#4B5563' },

    inputSection: { marginBottom: verticalScale(30) },
    label: { fontSize: scale(14), fontWeight: 'bold', color: '#4B5563', marginBottom: verticalScale(8) },
    input: { backgroundColor: '#FFFFFF', borderRadius: scale(16), paddingHorizontal: scale(16), paddingVertical: verticalScale(14), fontSize: scale(16), color: '#111', marginBottom: verticalScale(24), shadowColor: '#000', shadowOffset: { width: 0, height: verticalScale(2) }, shadowOpacity: 0.05, shadowRadius: scale(4), elevation: 2 },

    footer: { padding: scale(16), backgroundColor: '#ECFCCB', paddingBottom: verticalScale(20) },
    saveButton: { backgroundColor: '#A3E635', paddingVertical: verticalScale(16), borderRadius: scale(16), alignItems: 'center', shadowColor: '#000', shadowOffset: { width: 0, height: verticalScale(2) }, shadowOpacity: 0.1, shadowRadius: scale(4), elevation: 3 },
    saveButtonText: { fontSize: scale(16), fontWeight: 'bold', color: '#111' }
});

export default ChildEditProfileScreen;
