import React, { useState } from 'react';
import { View, Text, StyleSheet, TouchableOpacity, SafeAreaView, TextInput, KeyboardAvoidingView, Platform, Alert, ScrollView } from 'react-native';
import CustomText from '../../components/common/CustomText';
import { RFValue } from 'react-native-responsive-fontsize';
import api from '../../api/axios';

const ParentInitialSetupScreen = ({ navigation, route }) => {
    const { tempToken, role, name, familyCode } = route.params || {};

    const [bankAccount, setBankAccount] = useState('');

    const handleNext = async () => {
        if (!bankAccount.trim()) {
            Alert.alert('알림', '연결할 계좌번호를 입력해주세요.');
            return;
        }
        try {
            // 1. 가족 그룹 참가 또는 생성
            if (familyCode && familyCode !== 'mock-family-code') {
                try {
                    await api.post('/families/join', { scannedQrCode: familyCode }, { headers: { Authorization: `Bearer ${tempToken}` } });
                    console.log('Spouse Joined Family Successfully');
                } catch (e) {
                    console.error('Spouse Join API Error:', e.response?.data || e.message);
                }
            } else {
                await api.post('/families', {}, { headers: { Authorization: `Bearer ${tempToken}` } });
            }

            // 2. 부모 기본 계좌 연동 (현재 500 에러 발생 시 부모 가입 차단을 막기 위해 임시 예외처리)
            try {
                await api.post('/bank/accounts/link', { bankCode: '004', accountNumber: bankAccount }, { headers: { Authorization: `Bearer ${tempToken}` } });
            } catch (linkErr) {
                console.warn('Bank linking failed, bypassed:', linkErr);
            }

            navigation.replace('PinNumberSetup', { tempToken, role, name, bankAccount });
        } catch (error) {
            console.error('Initial Setup Error:', error.response?.data || error.message);
            Alert.alert('오류', '가족 정보 등록 중 문제가 발생했습니다.');
        }
    };

    return (
        <SafeAreaView style={styles.safeArea}>
            <KeyboardAvoidingView style={styles.container} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
                <ScrollView contentContainerStyle={styles.scrollContent}>

                    <View style={styles.stepContainer}>
                        <CustomText style={styles.title}>자산 정보 등록</CustomText>
                        <CustomText style={styles.subtitle}>아이들의 용돈을 충전해 줄{'\n'}부모님의 주거래 계좌를 연결할까요?</CustomText>

                        <TextInput
                            style={styles.input}
                            placeholder="계좌번호 입력 (숫자만)"
                            placeholderTextColor="#9CA3AF"
                            value={bankAccount}
                            onChangeText={setBankAccount}
                            keyboardType="number-pad"
                            autoFocus={true}
                        />
                        <View style={styles.infoBox}>
                            <CustomText style={styles.infoText}>🔒 입력하신 정보는 안전하게 암호화되어 보관됩니다.</CustomText>
                        </View>
                    </View>

                </ScrollView>

                <View style={styles.buttonSection}>
                    <TouchableOpacity
                        style={[styles.submitButton, !bankAccount.trim() ? styles.submitButtonDisabled : null]}
                        onPress={handleNext}
                        activeOpacity={0.8}
                    >
                        <CustomText style={styles.submitButtonText}>완료</CustomText>
                    </TouchableOpacity>
                </View>

            </KeyboardAvoidingView>
        </SafeAreaView>
    );
};

const styles = StyleSheet.create({
    safeArea: { flex: 1, backgroundColor: '#FFFFFF' },
    container: { flex: 1 },
    scrollContent: { flexGrow: 1, paddingHorizontal: RFValue(24), paddingTop: RFValue(30) },

    progressRow: { flexDirection: 'row', gap: RFValue(8), marginBottom: RFValue(30) },
    progressDot: { flex: 1, height: RFValue(4), backgroundColor: '#E5E7EB', borderRadius: RFValue(2) },
    progressDotActive: { backgroundColor: '#A3E635' },

    stepContainer: { flex: 1 },
    title: { fontSize: RFValue(26), fontWeight: 'bold', color: '#111', marginBottom: RFValue(12) },
    subtitle: { fontSize: RFValue(15), color: '#6B7280', fontWeight: '500', lineHeight: RFValue(22), marginBottom: RFValue(40) },

    input: { fontSize: RFValue(20), color: '#111', fontWeight: 'bold', borderBottomWidth: 2, borderBottomColor: '#111', paddingVertical: RFValue(10), marginBottom: RFValue(20) },

    infoBox: { backgroundColor: '#F9FAFB', padding: RFValue(16), borderRadius: RFValue(12), marginTop: RFValue(10) },
    infoText: { fontSize: RFValue(13), color: '#4B5563' },

    buttonSection: { paddingHorizontal: RFValue(24), paddingBottom: Platform.OS === 'ios' ? RFValue(20) : RFValue(16) },
    submitButton: { width: '100%', backgroundColor: '#A3E635', height: RFValue(54), borderRadius: RFValue(12), justifyContent: 'center', alignItems: 'center' },
    submitButtonDisabled: { backgroundColor: '#E5E7EB' },
    submitButtonText: { color: '#FFFFFF', fontSize: RFValue(16), fontWeight: 'bold' }
});

export default ParentInitialSetupScreen;
