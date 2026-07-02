import React, { useState, useEffect } from 'react';
import { View, StyleSheet, SafeAreaView, TouchableOpacity, Alert, ScrollView } from 'react-native';
import { scale, verticalScale } from 'react-native-size-matters';
import CustomText from '../../../components/common/CustomText';
import CustomTextInput from '../../../components/common/CustomTextInput';
import useAuthStore from '../../../store/useAuthStore';
import api from '../../../api/axios';

const ParentEditProfileScreen = ({ navigation }) => {
    const { logout, user, setUser } = useAuthStore();
    const [nickname, setNickname] = useState(user?.name || '');
    const [phone, setPhone] = useState('010-0000-0000');

    useEffect(() => {
        if (user?.name) {
            setNickname(user.name);
        }
    }, [user?.name]);

    useEffect(() => {
        // API 캐싱 방지를 위해 timestamp 추가
        api.get(`/users/me?t=${new Date().getTime()}`)
            .then(res => {
                if (res.data?.data) {
                    setPhone(res.data.data.username || '비공개 번호');
                }
            })
            .catch(err => console.error('Profile fetch error', err));
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
            Alert.alert('저장 완료', '부모님 정보가 수정되었습니다.', [{ text: '확인', onPress: () => navigation.goBack() }]);
        } catch (error) {
            console.error('Profile Save Error', error);
            Alert.alert('오류', '프로필 수정에 실패했습니다.');
        }
    };

    const handleLogout = () => {
        Alert.alert('로그아웃', '정말 로그아웃 하시겠습니까?', [
            { text: '취소', style: 'cancel' },
            {
                text: '로그아웃',
                style: 'destructive',
                onPress: async () => {
                    try {
                        await api.post('/auth/logout');
                    } catch (e) {
                        console.error('Logout error', e);
                    }
                    logout();
                    navigation.reset({ index: 0, routes: [{ name: 'SocialLogin' }] });
                }
            }
        ]);
    };

    const handleWithdraw = () => {
        Alert.alert('회원 탈퇴', '정말 탈퇴하시겠습니까? 연동된 가족 정보 및 데이터가 삭제될 수 있습니다.', [
            { text: '취소', style: 'cancel' },
            {
                text: '탈퇴하기',
                style: 'destructive',
                onPress: async () => {
                    try {
                        await api.delete('/users/me');
                        logout();
                        navigation.reset({ index: 0, routes: [{ name: 'SocialLogin' }] });
                    } catch (e) {
                        console.error('Withdraw error', e);
                        Alert.alert('오류', '탈퇴 처리 중 문제가 발생했습니다.');
                    }
                }
            }
        ]);
    };

    return (
        <SafeAreaView style={styles.safeArea}>
            <View style={styles.header}>
                <TouchableOpacity style={styles.backButton} onPress={() => navigation.goBack()}>
                    <CustomText style={styles.backButtonText}>←</CustomText>
                </TouchableOpacity>
                <CustomText style={styles.headerTitle}>내 정보 수정</CustomText>
                <View style={{ width: scale(32) }} />
            </View>

            <ScrollView contentContainerStyle={styles.container}>
                <View style={styles.inputSection}>
                    <CustomText style={styles.label}>닉네임</CustomText>
                    <CustomTextInput
                        style={styles.input}
                        value={nickname}
                        onChangeText={setNickname}
                        maxLength={10}
                        placeholder="앱에서 사용할 호칭을 적어주세요"
                        placeholderTextColor="#9CA3AF"
                    />

                    <CustomText style={styles.label}>휴대폰 번호</CustomText>
                    <CustomTextInput
                        style={[styles.input, styles.disabledInput]}
                        value={phone}
                        editable={false}
                    />
                </View>
            </ScrollView>

            <View style={styles.footer}>
                <TouchableOpacity style={styles.saveButton} onPress={handleSave}>
                    <CustomText style={styles.saveButtonText}>수정 완료</CustomText>
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

    container: { flexGrow: 1, paddingHorizontal: scale(24), paddingTop: verticalScale(30) },

    inputSection: { marginBottom: verticalScale(30) },
    label: { fontSize: scale(14), fontWeight: 'bold', color: '#4B5563', marginBottom: verticalScale(8) },
    input: { backgroundColor: '#FFFFFF', borderRadius: scale(16), paddingHorizontal: scale(16), paddingVertical: verticalScale(14), fontSize: scale(16), color: '#111', marginBottom: verticalScale(16), shadowColor: '#000', shadowOffset: { width: 0, height: verticalScale(2) }, shadowOpacity: 0.05, shadowRadius: scale(4), elevation: 2 },
    disabledInput: { backgroundColor: '#F9FAFB', color: '#6B7280' },

    footer: { padding: scale(16), backgroundColor: '#FFFFFF', borderTopWidth: 1, borderTopColor: '#F9FAFB' },
    saveButton: { backgroundColor: '#A3E635', paddingVertical: verticalScale(16), borderRadius: scale(16), alignItems: 'center', shadowColor: '#000', shadowOffset: { width: 0, height: 2 }, shadowOpacity: 0.05, shadowRadius: 4, elevation: 2, },
    saveButtonText: { fontSize: scale(16), fontWeight: 'bold', color: '#111' },

    utilitySection: { flexDirection: 'row', justifyContent: 'center', alignItems: 'center', marginTop: verticalScale(40), marginBottom: verticalScale(16) },
    logoutText: { fontSize: scale(13), color: '#6B7280', fontWeight: '500' },
    divider: { width: 1, height: verticalScale(12), backgroundColor: '#D1D5DB', marginHorizontal: scale(12) },
    withdrawalText: { fontSize: scale(13), color: '#EF4444', fontWeight: '500' }
});

export default ParentEditProfileScreen;
