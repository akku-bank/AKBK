import React, { useState } from 'react';
import { View, Text, StyleSheet, TouchableOpacity, Image, Alert, ActivityIndicator, Platform, TextInput } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import CustomText from '../../components/common/CustomText';
import { RFValue } from 'react-native-responsive-fontsize';
import { login as kakaoLogin } from '@react-native-seoul/kakao-login';
import api from '../../api/axios';
import useAuthStore from '../../store/useAuthStore';
import * as Device from 'expo-device';
import * as Notifications from 'expo-notifications';

const SocialLoginScreen = ({ navigation }) => {
    const [isLoading, setIsLoading] = useState(false);
    const [testUserId, setTestUserId] = useState('123e4567-e89b-12d3-a456-426614174000');
    const { setAuthInfo } = useAuthStore();

    const handleTestLogin = async () => {
        if (!testUserId) {
            Alert.alert('테스트 에러', 'userId를 입력하세요.');
            return;
        }
        setIsLoading(true);
        try {
            const response = await api.post('/auth/test/login', { userId: testUserId.trim() });
            const payload = response.data?.data || response.data || {};
            const jwt = payload.token || payload.tempToken || payload.jwt || payload.accessToken;

            let userRole = null;
            let userName = null;
            let userId = null;
            let profile = {}; // Declare profile here
            let isAlreadyRegistered = false;

            try {
                // 저장된 토큰 이용해 ROLE 및 NAME 조회해서 현재 등록된 유저인지 파악
                const userRes = await api.get('/users/me', {
                    headers: { Authorization: `Bearer ${jwt}` }
                });
                profile = userRes.data?.data || userRes.data || {}; // Assign to pre-declared profile
                userRole = profile.role || null;
                userName = profile.name || null;
                userId = profile.userId || null;
                if ((userRole === 'PARENT' || userRole === 'CHILD') && userName) {
                    isAlreadyRegistered = true;
                }
            } catch (e) {
                console.log('신규 테스트 유저이거나 프로필 조회 실패:', e.message);
            }

            // 전역 상태에 토큰 및 정보 저장
            await setAuthInfo(jwt, userRole, userName, userId, {
                familyId: profile.familyId,
                level: profile.level
            });
            await handleFcmRegistration(jwt);

            Alert.alert('테스트 로그인 성공', '임시/정식 토큰 발급 성공!', [
                {
                    text: '확인', onPress: () => {
                        if (isAlreadyRegistered) {
                            navigation.replace('PinNumberLogin');
                        } else {
                            navigation.replace('RoleSelect', { tempToken: jwt });
                        }
                    }
                }
            ]);
        } catch (error) {
            console.error('Test Login Error:', error);
            Alert.alert('테스트 로그인 실패', 'API 연동에 실패했습니다.');
        } finally {
            setIsLoading(false);
        }
    };

    // FCM 기기 등록 공통 헬퍼 함수
    const handleFcmRegistration = async (jwt) => {
        try {
            let fcmTokenToUse = 'expo-dummy-token-for-dev-test';

            if (Platform.OS === 'android') {
                await Notifications.setNotificationChannelAsync('default', {
                    name: 'default',
                    importance: Notifications.AndroidImportance.MAX,
                });
            }

            const { status: existingStatus } = await Notifications.getPermissionsAsync();
            let finalStatus = existingStatus;
            if (existingStatus !== 'granted') {
                const { status } = await Notifications.requestPermissionsAsync();
                finalStatus = status;
            }

            if (finalStatus === 'granted') {
                try {
                    // 백엔드가 Spring Boot FCM SDK 이므로 Expo Push Token이 아닌 순수 FCM Device Token 추출
                    const tokenData = await Notifications.getDevicePushTokenAsync();
                    if (tokenData && tokenData.data) {
                        fcmTokenToUse = tokenData.data;
                    }
                } catch (tokenErr) {
                    console.log('기기 토큰 추출 실패 (혹은 에뮬레이터 제한 환경):', tokenErr);
                }
            }

            await api.put('/users/me/fcm-token', { fcmToken: fcmTokenToUse }, {
                headers: { Authorization: `Bearer ${jwt}` }
            });
            console.log('최종 디바이스 기기 토큰 등록 완료:', fcmTokenToUse);
        } catch (fcmErr) {
            console.error('FCM Token Registration Failed:', fcmErr);
        }
    };

    const handleKakaoLogin = async () => {
        setIsLoading(true);

        try {
            const token = await kakaoLogin();
            console.log('카카오 로그인 성공! 네이티브 토큰 발급 완료');
            console.log('카카오 토큰:', token.accessToken);

            // 토큰 발급 완료 테스트 성공 -> 백엔드로 전송
            const response = await api.post('/auth/social/kakao', { socialToken: token.accessToken });
            const payload = response.data?.data || response.data || {};

            // Jackson 직렬화 이슈 대비 (isRegistered -> registered) 및 각종 토큰 변수명 대비
            // Jackson 직렬화 이슈 대비 (isRegistered -> registered) 및 각종 토큰 변수명 대비
            const isRegistered = payload.isRegistered ?? payload.registered ?? !!payload.token;
            const jwt = payload.token || payload.tempToken || payload.jwt || payload.accessToken;

            console.log('JWT 발급 성공!');
            console.log('토큰 키:', jwt);

            let userRole = null;
            let userName = null;
            let userId = null;
            let profile = {};

            if (isRegistered && jwt) {
                try {
                    // 저장된 토큰 이용해 ROLE 및 NAME 조회 위해서 /users/me 호출
                    const userRes = await api.get('/users/me', {
                        headers: { Authorization: `Bearer ${jwt}` }
                    });
                    profile = userRes.data?.data || userRes.data || {};
                    userRole = profile.role || null;
                    userName = profile.name || null;
                    userId = profile.userId || null;
                    console.log(`유저 프로필 로드 완료: 역할=${userRole}, 이름=${userName}`);
                } catch (e) {
                    console.error('사용자 정보 조회 실패:', e);
                }
            }

            // authInfo (zustand) 에 jwt, role, name 함께 업데이트
            await setAuthInfo(jwt, userRole, userName, userId, {
                familyId: profile.familyId,
                level: profile.level
            });

            // 로그인 성공 시 백엔드로 FCM 토큰 전송 시도
            await handleFcmRegistration(jwt);

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
                    <CustomText style={styles.subtitle}>우리 가족 금융 생활은 아꾸뱅꾸로</CustomText>
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
                            <CustomText style={[styles.kakaoButtonText, { color: '#000000' }]}>카카오로 시작하기</CustomText>
                        )}
                    </TouchableOpacity>

                    {/* 개발 연동 테스트 전용 섹션 */}
                    <View style={styles.testLoginBox}>
                        <CustomText style={styles.testLoginLabel}>[테스트용] 우회 계정 UUID</CustomText>
                        <View style={styles.testInputRow}>
                            <TextInput
                                style={styles.testInput}
                                placeholder="[자녀용] UUID"
                                value={testUserId}
                                onChangeText={setTestUserId}
                            />
                            <TouchableOpacity style={styles.testButton} onPress={handleTestLogin}>
                                <CustomText style={styles.testButtonText}>테스트 접속</CustomText>
                            </TouchableOpacity>
                        </View>
                    </View>
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
        paddingTop: Platform.OS === 'ios' ? RFValue(10) : RFValue(20),
        paddingBottom: Platform.OS === 'web' ? RFValue(120) : (Platform.OS === 'ios' ? RFValue(60) : RFValue(30)),
        paddingHorizontal: RFValue(24),
        justifyContent: 'space-between',
        alignItems: 'center',
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
        marginBottom: 0,
    },
    subtitle: {
        fontSize: RFValue(15),
        color: '#6B7280',
        fontWeight: '500',
        marginTop: RFValue(8),
    },
    buttonSection: {
        width: '100%',
        alignItems: 'center',
        marginTop: RFValue(10),
        marginBottom: RFValue(40),
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
    },
    testLoginBox: {
        marginTop: RFValue(20),
        width: '100%',
        padding: RFValue(12),
        backgroundColor: '#F3F4F6',
        borderRadius: RFValue(8),
        borderWidth: 1,
        borderColor: '#D1D5DB',
        borderStyle: 'dashed'
    },
    testLoginLabel: {
        fontSize: RFValue(11),
        color: '#4B5563',
        marginBottom: RFValue(8),
        fontWeight: 'bold',
    },
    testInputRow: {
        flexDirection: 'row',
        gap: RFValue(8),
    },
    testInput: {
        flex: 1,
        backgroundColor: '#FFF',
        borderWidth: 1,
        borderColor: '#D1D5DB',
        borderRadius: RFValue(6),
        paddingHorizontal: RFValue(10),
        height: RFValue(36),
    },
    testButton: {
        backgroundColor: '#111',
        paddingHorizontal: RFValue(16),
        borderRadius: RFValue(6),
        justifyContent: 'center',
        alignItems: 'center',
    },
    testButtonText: {
        color: '#FFF',
        fontSize: RFValue(12),
        fontWeight: 'bold',
    }
});

export default SocialLoginScreen;
