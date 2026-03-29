import React, { useState, useRef } from 'react';
import { View, StyleSheet, TouchableOpacity, SafeAreaView, Alert, KeyboardAvoidingView, Platform, Keyboard } from 'react-native';
import { scale, verticalScale } from 'react-native-size-matters';
import CustomText from '../../../components/common/CustomText';
import CustomTextInput from '../../../components/common/CustomTextInput';
import api from '../../../api/axios';

const ChildChangePasswordScreen = ({ navigation }) => {
    const [currentPin, setCurrentPin] = useState('');
    const [newPin, setNewPin] = useState('');
    const [confirmPin, setConfirmPin] = useState('');

    const currentPinRef = useRef(null);
    const newPinRef = useRef(null);
    const confirmPinRef = useRef(null);

    const handleSubmit = async () => {
        if (currentPin.length !== 6 || newPin.length !== 6 || confirmPin.length !== 6) {
            Alert.alert('알림', 'PIN 번호는 모두 6자리로 입력해주세요.');
            return;
        }
        if (newPin !== confirmPin) {
            Alert.alert('알림', '새 PIN 번호가 서로 일치하지 않습니다.');
            return;
        }

        try {
            await api.patch('/users/me/pin', {
                oldPin: currentPin,
                newPin
            });
            Alert.alert('변경 완료', '비밀번호가 성공적으로 변경되었습니다!', [
                { text: '확인', onPress: () => navigation.goBack() }
            ]);
        } catch (error) {
            console.error('Change PIN Error:', error);
            // 만약 서버에서 기존 비밀번호 틀림 에러를 주면 여기서 처리
            Alert.alert('오류', '비밀번호 변경에 실패했습니다. 기존 비밀번호를 확인해주세요.');
        }
    };

    return (
        <SafeAreaView style={styles.safeArea}>
            <View style={styles.header}>
                <TouchableOpacity style={styles.backButton} onPress={() => navigation.goBack()}>
                    <CustomText style={styles.backButtonText}>←</CustomText>
                </TouchableOpacity>
                <CustomText style={styles.headerTitle}>비밀번호 변경</CustomText>
                <View style={{ width: scale(32) }} />
            </View>

            <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : undefined} style={{ flex: 1 }}>
                <View style={styles.container}>
                    <CustomText style={styles.description}>
                        간편하게 비밀번호를 변경해보세요.
                    </CustomText>

                    <View style={styles.inputGroup}>
                        <CustomText style={styles.label}>현재 PIN 번호 (6자리)</CustomText>
                        <CustomTextInput
                            ref={currentPinRef}
                            style={styles.input}
                            keyboardType="numeric"
                            maxLength={6}
                            secureTextEntry={true}
                            value={currentPin}
                            onChangeText={(text) => {
                                setCurrentPin(text);
                                if (text.length === 6) newPinRef.current?.focus();
                            }}
                            placeholder="기존 비밀번호 입력"
                            placeholderTextColor="#9CA3AF"
                        />
                    </View>

                    <View style={styles.inputGroup}>
                        <CustomText style={styles.label}>새 PIN 번호 (6자리)</CustomText>
                        <CustomTextInput
                            ref={newPinRef}
                            style={styles.input}
                            keyboardType="numeric"
                            maxLength={6}
                            secureTextEntry={true}
                            value={newPin}
                            onChangeText={(text) => {
                                setNewPin(text);
                                if (text.length === 6) confirmPinRef.current?.focus();
                            }}
                            placeholder="새 비밀번호 입력"
                            placeholderTextColor="#9CA3AF"
                        />
                    </View>

                    <View style={styles.inputGroup}>
                        <CustomText style={styles.label}>새 PIN 번호 확인</CustomText>
                        <CustomTextInput
                            ref={confirmPinRef}
                            style={styles.input}
                            keyboardType="numeric"
                            maxLength={6}
                            secureTextEntry={true}
                            value={confirmPin}
                            onChangeText={(text) => {
                                setConfirmPin(text);
                                if (text.length === 6) Keyboard.dismiss();
                            }}
                            placeholder="새 비밀번호 다시 입력"
                            placeholderTextColor="#9CA3AF"
                        />
                    </View>

                    <TouchableOpacity
                        style={[
                            styles.submitButton,
                            (currentPin.length === 6 && newPin.length === 6 && confirmPin.length === 6)
                                ? styles.submitButtonActive
                                : styles.submitButtonDisabled
                        ]}
                        onPress={handleSubmit}
                        disabled={currentPin.length !== 6 || newPin.length !== 6 || confirmPin.length !== 6}
                    >
                        <CustomText style={styles.submitButtonText}>변경하기</CustomText>
                    </TouchableOpacity>
                </View>
            </KeyboardAvoidingView>
        </SafeAreaView>
    );
};

const styles = StyleSheet.create({
    safeArea: { flex: 1, backgroundColor: '#FFFFFF' },
    header: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingHorizontal: scale(16), paddingVertical: verticalScale(16), backgroundColor: '#FFFFFF' },
    backButton: { width: scale(32), height: scale(32), justifyContent: 'center' },
    backButtonText: { fontSize: scale(22), fontWeight: 'bold', color: '#111' },
    headerTitle: { fontSize: scale(18), fontWeight: 'bold', color: '#111' },

    container: { flex: 1, backgroundColor: '#ECFCCB', paddingHorizontal: scale(20), paddingTop: verticalScale(20) },
    description: { fontSize: scale(14), color: '#6B7280', marginBottom: verticalScale(30), lineHeight: 20 },

    inputGroup: { marginBottom: verticalScale(24) },
    label: { fontSize: scale(14), fontWeight: 'bold', color: '#4B5563', marginBottom: verticalScale(8) },
    input: { backgroundColor: '#FFFFFF', borderRadius: scale(16), padding: scale(16), fontSize: scale(18), letterSpacing: 4, fontWeight: 'bold', color: '#111', shadowColor: '#000', shadowOffset: { width: 0, height: verticalScale(2) }, shadowOpacity: 0.05, shadowRadius: scale(4), elevation: 2 },

    submitButton: { marginTop: verticalScale(20), paddingVertical: verticalScale(16), borderRadius: scale(16), alignItems: 'center', shadowColor: '#000', shadowOffset: { width: 0, height: verticalScale(2) }, shadowOpacity: 0.1, shadowRadius: scale(4), elevation: 3 },
    submitButtonActive: { backgroundColor: '#A3E635' },
    submitButtonDisabled: { backgroundColor: '#E5E7EB' },
    submitButtonText: { fontSize: scale(16), fontWeight: 'bold', color: '#111' },
});

export default ChildChangePasswordScreen;
