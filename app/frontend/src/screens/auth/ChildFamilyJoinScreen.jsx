import React, { useState } from 'react';
import { View, StyleSheet, TouchableOpacity, SafeAreaView, Platform, ActivityIndicator, TextInput, Alert } from 'react-native';
import CustomText from '../../components/common/CustomText';
import { RFValue } from 'react-native-responsive-fontsize';
import api from '../../api/axios';

const ChildFamilyJoinScreen = ({ navigation, route }) => {
    const { tempToken, role, familyCode } = route.params || {};
    const [isLoading, setIsLoading] = useState(false);
    const [childName, setChildName] = useState('');
    const [childBirth, setChildBirth] = useState('');

    const handleJoin = async () => {
        if (!childName.trim() || !childBirth.trim()) {
            Alert.alert('알림', '이름과 생년월일(YYYY-MM-DD)을 모두 입력해주세요.');
            return;
        }

        try {
            setIsLoading(true);

            // 1. 자녀 본인 이름, 생일, 역할 등록 (백엔드 필수 검증)
            const response = await api.post('/auth/signup',
                { role: 'CHILD', name: childName.trim(), birthDate: childBirth.trim() },
                { headers: { Authorization: `Bearer ${tempToken}` } }
            );

            const payload = response.data?.data || response.data || {};
            const resolvedToken = payload.signupToken || payload.accessToken || payload.token || tempToken;

            // 2. 가족 합류 API 전송 (이름 + 생일 완벽 일치 매칭 로직)
            if (familyCode && familyCode !== 'mock-family-code') {
                try {
                    await api.post('/families/join',
                        { scannedQrCode: familyCode },
                        { headers: { Authorization: `Bearer ${resolvedToken}` } }
                    );
                    console.log('가족 합류 API 전송 성공');
                } catch (e) {
                    console.error('가족 합류 에러:', e.response?.data || e.message);
                    setIsLoading(false);
                    Alert.alert('연동 실패', '부모님이 등록하신 정보와 일치하지 않거나 코드가 다릅니다.');
                    return;
                }
            }

            navigation.replace('PinNumberSetup', {
                tempToken: resolvedToken,
                role: 'CHILD',
                name: childName.trim()
            });

        } catch (error) {
            console.error('가입 에러:', error.response?.data || error.message);
            setIsLoading(false);
            Alert.alert('가입 실패', error.response?.data?.message || '에러가 발생했습니다.');
        }
    };

    return (
        <SafeAreaView style={styles.safeArea}>
            <View style={styles.container}>
                <View style={styles.headerSection}>
                    <CustomText style={styles.title}>부모님이 등록하신{'\n'}내 정보를 똑같이 입력해주세요!</CustomText>
                </View>

                <View style={styles.inputSection}>
                    <TextInput
                        style={styles.textInput}
                        placeholder="이름 (예: 김싸피)"
                        placeholderTextColor="#9CA3AF"
                        value={childName}
                        onChangeText={setChildName}
                    />
                    <TextInput
                        style={[styles.textInput, { marginTop: 12 }]}
                        placeholder="생년월일 8자리 (예: 2013-05-05)"
                        placeholderTextColor="#9CA3AF"
                        value={childBirth}
                        onChangeText={setChildBirth}
                    />
                </View>

                <View style={styles.buttonSection}>
                    <TouchableOpacity
                        style={[styles.submitButton, (!childName.trim() || !childBirth.trim()) && styles.submitButtonDisabled]}
                        onPress={handleJoin}
                        disabled={!childName.trim() || !childBirth.trim() || isLoading}
                        activeOpacity={0.8}
                    >
                        {isLoading ? (
                            <ActivityIndicator color="#FFFFFF" />
                        ) : (
                            <CustomText style={styles.submitButtonText}>완벽하게 연동하기</CustomText>
                        )}
                    </TouchableOpacity>
                </View>
            </View>
        </SafeAreaView >
    );
};

const styles = StyleSheet.create({
    safeArea: { flex: 1, backgroundColor: '#FFFFFF' },
    container: { flex: 1, justifyContent: 'space-between', paddingHorizontal: RFValue(24), paddingTop: RFValue(50), paddingBottom: Platform.OS === 'ios' ? RFValue(20) : RFValue(16) },
    headerSection: { marginBottom: RFValue(20) },
    title: { fontSize: RFValue(26), fontWeight: 'bold', color: '#111', marginBottom: RFValue(12) },
    subtitle: { fontSize: RFValue(15), color: '#EF4444', lineHeight: RFValue(22), fontWeight: 'bold' },
    inputSection: { flex: 1, justifyContent: 'center' },
    textInput: { width: '100%', height: RFValue(56), backgroundColor: '#F9FAFB', borderRadius: RFValue(12), paddingHorizontal: RFValue(16), fontSize: RFValue(16), borderWidth: 1, borderColor: '#D1D5DB', color: '#111' },
    buttonSection: { paddingTop: RFValue(12) },
    submitButton: { width: '100%', backgroundColor: '#A3E635', height: RFValue(54), borderRadius: RFValue(12), justifyContent: 'center', alignItems: 'center' },
    submitButtonDisabled: { backgroundColor: '#E5E7EB' },
    submitButtonText: { color: '#FFFFFF', fontSize: RFValue(16), fontWeight: 'bold' }
});

export default ChildFamilyJoinScreen;
