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

        /* ==========================================
           !! 카카오 로그인 연동 코드 !!
           네이티브 앱 키(iOS/Android) 세팅 후 주석 풀면 완료 !
           ========================================== 
        try {
            const token = await kakaoLogin();
            console.log('Kakao Token:', token);
            
            // 백엔드로 카카오 액세스 토큰 전송
            // const response = await api.post('/auth/social/kakao', { accessToken: token.accessToken });
            // const { jwt, isRegistered, role, name } = response.data;
            
            // await setAuthInfo(jwt, role, name);
            // if (isRegistered) {
            //     navigation.replace('PinNumberLogin');
            // } else {
            //     navigation.replace('RoleSelect', { tempToken: jwt });
            // }
        } catch (error) {
            console.error('Kakao Login Error:', error);
            Alert.alert('로그인 에러', '카카오 로그인에 실패했습니다.');
        } finally {
            setIsLoading(false);
        }
        ========================================== */

        // (테스트용 임시  - 실제 연동 전까지만)
        setTimeout(async () => {
            setIsLoading(false);

            if (Platform.OS === 'web') {
                const isExistingUser = window.confirm(
                    '(임시) \n\n[확인]을 누르면 "기존 유저 로그인"으로 진행\n[취소]를 누르면 "신규 가입"으로 진행'
                );

                if (isExistingUser) {
                    const mockToken = "dev-bypass-existing-token";
                    const isParent = window.confirm('(임시) 부모 계정으로 진입할까요?\n[확인]=부모, [취소]=자녀');
                    const role = isParent ? 'PARENT' : 'CHILD';
                    const name = isParent ? '부모님' : '아이짱';

                    await setAuthInfo(mockToken, role, name);
                    navigation.replace('PinNumberLogin');
                } else {
                    const mockToken = "dev-bypass-new-token";
                    await setAuthInfo(mockToken, null, null);
                    navigation.replace('RoleSelect', { tempToken: mockToken });
                }
            } else {
                // (임시) 카카오 토큰 -> 백엔드로 보냈다치고, 백엔드의 응답(가입 여부)을 팝업으로 선택하게
                Alert.alert(
                    '(임시) 백엔드 응답 시뮬레이션',
                    '실제로는 이 단계에서 서버가 isRegistered 값을 내려줍니다. 테스트할 흐름을 선택하세요.',
                    [
                        {
                            text: '기존 유저 (부모)',
                            onPress: async () => {
                                const mockToken = "dev-bypass-existing-token";
                                await setAuthInfo(mockToken, 'PARENT', '아이부모');
                                navigation.replace('PinNumberLogin');
                            }
                        },
                        {
                            text: '기존 유저 (자녀)',
                            onPress: async () => {
                                const mockToken = "dev-bypass-existing-token";
                                await setAuthInfo(mockToken, 'CHILD', '아이짱');
                                navigation.replace('PinNumberLogin');
                            }
                        },
                        {
                            text: '신규 가입',
                            onPress: async () => {
                                const mockToken = "dev-bypass-new-token";
                                await setAuthInfo(mockToken, null, null);
                                navigation.replace('RoleSelect', { tempToken: mockToken });
                            }
                        }
                    ],
                    { cancelable: true }
                );
            }
        }, 500);
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
