import React, { useState } from 'react';
import { View, Text, StyleSheet, TouchableOpacity, Image, Alert, ActivityIndicator, SafeAreaView, Platform } from 'react-native';
import { RFValue } from 'react-native-responsive-fontsize';
import { login as kakaoLogin } from '@react-native-seoul/kakao-login';
import api from '../../api/axios';
import useAuthStore from '../../store/useAuthStore';

const SocialLoginScreen = ({ navigation }) => {
    const [isLoading, setIsLoading] = useState(false);
    const { setAuthInfo } = useAuthStore();

    const handleKakaoLogin = async () => {
        setIsLoading(true);

        try {
            const token = await kakaoLogin();
            console.log('Kakao Token:', token);

            // 토큰 발급 완료 테스트 성공, 이제 백엔드로 전송합니다.
            const response = await api.post('auth/social/kakao', { socialToken: token.accessToken });
            const { jwt, isRegistered, role, name } = response.data;

            await setAuthInfo(jwt, role, name);
            if (isRegistered) {
                navigation.replace('PinNumberLogin');
            } else {
                navigation.replace('RoleSelect', { tempToken: jwt });
            }
        } catch (error) {
            console.error('Kakao Login Error:', error);
            const errUrl = error.config ? `${error.config.baseURL}${error.config.url}` : 'Unknown URL';
            const errBody = error.response ? JSON.stringify(error.response.data) : error.message;
            Alert.alert(
                '로그인 에러 상세',
                `상태코드: ${error.response?.status}\n요청주소: ${errUrl}\n응답내용: ${errBody}`
            );
        } finally {
            setIsLoading(false);
        }

        /* --- 임시 모의 로직 (삭제) ---
        setTimeout(async () => {
            setIsLoading(false);
        }, 500);
        */
    };

    return (
        <SafeAreaView style={styles.safeArea}>
            <View style={styles.container}>
                {/* 로고 영역 */}
                <View style={styles.logoSection}>
                    <Image
                        source={require('../../assets/croco/logo.png')}
                        style={styles.logoImage}
                        resizeMode="contain"
                    />
                    <Text style={styles.subtitle}>우리 가족 금융 생활은 아꾸뱅꾸로</Text>
                </View>

                {/* 로그인 버튼 영역 */}
                <View style={[styles.buttonSection, { gap: 10 }]}>
                    <TouchableOpacity
                        style={[styles.kakaoButton, { backgroundColor: '#FEE500' }]}
                        onPress={handleKakaoLogin}
                        disabled={isLoading}
                        activeOpacity={0.8}
                    >
                        {isLoading ? (
                            <ActivityIndicator color="#000000" />
                        ) : (
                            <Text style={[styles.kakaoButtonText, { color: '#000000' }]}>카카오로 시작하기</Text>
                        )}
                    </TouchableOpacity>
                </View>
            </View>
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
        paddingHorizontal: RFValue(24),
        justifyContent: 'space-between',
        alignItems: 'center',
        paddingVertical: RFValue(50),
    },
    logoSection: {
        flex: 1,
        justifyContent: 'center',
        alignItems: 'center',
        width: '100%',
    },
    logoImage: {
        width: RFValue(180),
        height: RFValue(180),
        marginBottom: RFValue(24),
    },
    subtitle: {
        fontSize: RFValue(15),
        color: '#6B7280',
        fontWeight: '500',
        marginTop: RFValue(10),
    },
    buttonSection: {
        width: '100%',
        alignItems: 'center',
        paddingBottom: Platform.OS === 'web' ? RFValue(70) : (Platform.OS === 'ios' ? RFValue(20) : RFValue(10)),
    },
    kakaoButton: {
        width: '100%',
        backgroundColor: '#A3E635',
        height: RFValue(54),
        borderRadius: RFValue(12),
        justifyContent: 'center',
        alignItems: 'center',
        flexDirection: 'row',
        elevation: 1,
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 1 },
        shadowOpacity: 0.1,
        shadowRadius: 2,
    },
    kakaoButtonText: {
        color: '#FFFFFF',
        fontSize: RFValue(16),
        fontWeight: 'bold',
    },
    helpText: {
        fontSize: RFValue(11),
        color: '#9CA3AF',
        textAlign: 'center',
        paddingHorizontal: RFValue(20),
        lineHeight: RFValue(16),
    }
});

export default SocialLoginScreen;
