import React, { useState } from 'react';
import { View, Text, StyleSheet, TextInput, TouchableOpacity, SafeAreaView, KeyboardAvoidingView, Platform, Alert, ActivityIndicator } from 'react-native';
import CustomText from '../../components/common/CustomText';
import { RFValue } from 'react-native-responsive-fontsize';
import api from '../../api/axios';
import useAuthStore from '../../store/useAuthStore';

const SignUpScreen = ({ navigation, route }) => {
    const { tempToken, role, familyCode } = route.params || {};
    const [name, setName] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const { setAuthInfo } = useAuthStore();

    const handleSignup = async () => {
        if (!name.trim()) {
            Alert.alert('알림', '이름을 입력해주세요.');
            return;
        }

        setIsLoading(true);
        try {
            const response = await api.post('/auth/signup',
                { role, name },
                { headers: { Authorization: `Bearer ${tempToken}` } }
            );

            // 발급된 실제 토큰 추출 (백엔드에서 signupToken으로 응답)
            const payload = response.data?.data || response.data || {};
            const resolvedToken = payload.signupToken || payload.accessToken || payload.token || tempToken;

            if (role === 'PARENT') {
                navigation.replace('ParentInitialSetup', { tempToken: resolvedToken, role, name, familyCode });
            } else {
                navigation.replace('PinNumberSetup', { tempToken: resolvedToken, role, name });
            }
        } catch (error) {
            console.error('Signup Error:', error);
            Alert.alert('오류', '회원가입 처리 중 문제가 발생했습니다.');
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <SafeAreaView style={styles.safeArea}>
            <KeyboardAvoidingView
                style={styles.container}
                behavior={Platform.OS === 'ios' ? 'padding' : undefined}
            >
                <View style={styles.content}>
                    <View style={styles.titleSection}>
                        <CustomText style={styles.title}>이름을 알려주세요!</CustomText>
                        <CustomText style={styles.subtitle}>앱에서 사용할 닉네임이나 실명을 적어주세요.</CustomText>
                    </View>

                    <View style={styles.inputSection}>
                        <TextInput
                            style={styles.input}
                            placeholder="이름 입력 (예: 사스케)"
                            placeholderTextColor="#9CA3AF"
                            value={name}
                            onChangeText={setName}
                            autoFocus={true}
                            maxLength={10}
                        />
                    </View>
                </View>

                <View style={styles.buttonSection}>
                    <TouchableOpacity
                        style={[styles.submitButton, !name.trim() && styles.submitButtonDisabled]}
                        onPress={handleSignup}
                        disabled={isLoading || !name.trim()}
                        activeOpacity={0.8}
                    >
                        {isLoading ? (
                            <ActivityIndicator color="#FFFFFF" />
                        ) : (
                            <CustomText style={styles.submitButtonText}>시작하기</CustomText>
                        )}
                    </TouchableOpacity>
                </View>
            </KeyboardAvoidingView>
        </SafeAreaView>
    );
};

const styles = StyleSheet.create({
    safeArea: {
        flex: 1,
        backgroundColor: '#FFFFFF',
    },
    container: {
        flex: 1,
    },
    content: {
        flex: 1,
        paddingHorizontal: RFValue(24),
        paddingTop: RFValue(40),
    },
    titleSection: {
        marginBottom: RFValue(40),
    },
    title: {
        fontSize: RFValue(26),
        fontWeight: 'bold',
        color: '#111',
        marginBottom: RFValue(12),
    },
    subtitle: {
        fontSize: RFValue(15),
        color: '#6B7280',
        fontWeight: '500',
    },
    inputSection: {
        width: '100%',
    },
    input: {
        fontSize: RFValue(22),
        color: '#111',
        fontWeight: 'bold',
        borderBottomWidth: 2,
        borderBottomColor: '#111',
        paddingVertical: RFValue(10),
    },
    buttonSection: {
        paddingHorizontal: RFValue(24),
        paddingBottom: Platform.OS === 'ios' ? RFValue(20) : RFValue(16),
    },
    submitButton: {
        width: '100%',
        backgroundColor: '#A3E635',
        height: RFValue(54),
        borderRadius: RFValue(12),
        justifyContent: 'center',
        alignItems: 'center',
    },
    submitButtonDisabled: {
        backgroundColor: '#E5E7EB',
    },
    submitButtonText: {
        color: '#FFFFFF',
        fontSize: RFValue(16),
        fontWeight: 'bold',
    }
});

export default SignUpScreen;
