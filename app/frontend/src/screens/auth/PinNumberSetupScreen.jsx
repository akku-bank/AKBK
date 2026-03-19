import React, { useState } from 'react';
import { View, Text, StyleSheet, TouchableOpacity, SafeAreaView, Platform, Alert } from 'react-native';
import { RFValue } from 'react-native-responsive-fontsize';
import useAuthStore from '../../store/useAuthStore';
import api from '../../api/axios';

const PinNumberSetupScreen = ({ navigation, route }) => {
    const { tempToken, role, name } = route.params || {};
    const [pin, setPin] = useState('');
    const [isSuccess, setIsSuccess] = useState(false);
    const { setAuthInfo } = useAuthStore();

    const handleKeyPress = (num) => {
        if (pin.length < 6) {
            const newPin = pin + num;
            setPin(newPin);
            if (newPin.length === 6) {
                handlePinSubmit(newPin);
            }
        }
    };

    const handleDelete = () => {
        setPin(pin.slice(0, -1));
    };

    const handlePinSubmit = async (finalPin) => {
        try {
            /* ==========================================
               [진짜 간편 비밀번호 설정 API 연동 코드]
               ========================================== 
            await api.post('/auth/signup/pin', { pin: finalPin }, { headers: { Authorization: `Bearer ${tempToken}` } });
            
            await setAuthInfo(tempToken, role, name);
            setIsSuccess(true);
            setTimeout(() => {
                if (role === 'PARENT') {
                    navigation.replace('ParentMain');
                } else {
                    navigation.replace('ChildMain');
                }
            }, 1000);
            return;
            ========================================== */

            // --- 실제 연동 시 아래 블록 전체 삭제 ---
            if (tempToken?.includes("dev-bypass")) {
                await setAuthInfo(tempToken, role, name);
                setIsSuccess(true);
                setTimeout(() => {
                    if (role === 'PARENT') {
                        navigation.replace('ParentMain');
                    } else {
                        navigation.replace('ChildMain');
                    }
                }, 1000);
                return;
            }

            await setAuthInfo(tempToken, role, name);
            setIsSuccess(true);
            setTimeout(() => {
                if (role === 'PARENT') {
                    navigation.replace('ParentMain');
                } else {
                    navigation.replace('ChildMain');
                }
            }, 1000);
        } catch (error) {
            console.error('PIN Setup Error:', error);
            Alert.alert('오류', '간편 비밀번호 설정 중 문제가 발생했습니다.');
            setPin('');
        }
    };

    const renderKeypad = () => {
        const rows = [
            ['1', '2', '3'],
            ['4', '5', '6'],
            ['7', '8', '9'],
            ['', '0', 'delete']
        ];

        return (
            <View style={styles.keypadContainer}>
                {rows.map((row, rowIndex) => (
                    <View key={rowIndex} style={styles.keypadRow}>
                        {row.map((key, keyIndex) => {
                            if (key === '') return <View key={keyIndex} style={styles.keypadKey} />;
                            if (key === 'delete') {
                                return (
                                    <TouchableOpacity key={keyIndex} style={styles.keypadKey} onPress={handleDelete}>
                                        <Text style={styles.keypadText}>{'<'}</Text>
                                    </TouchableOpacity>
                                );
                            }
                            return (
                                <TouchableOpacity key={keyIndex} style={styles.keypadKey} onPress={() => handleKeyPress(key)}>
                                    <Text style={styles.keypadText}>{key}</Text>
                                </TouchableOpacity>
                            );
                        })}
                    </View>
                ))}
            </View>
        );
    };

    return (
        <SafeAreaView style={styles.safeArea}>
            <View style={styles.container}>
                <View style={styles.headerSection}>
                    <Text style={styles.title}>간편 비밀번호 설정</Text>
                    <Text style={styles.subtitle}>앞으로 로그인에 사용할{'\n'}6자리 비밀번호를 입력해주세요.</Text>
                </View>

                <View style={styles.dotsContainer}>
                    {[...Array(6)].map((_, index) => (
                        <View
                            key={index}
                            style={[
                                styles.dot,
                                pin.length > index ? styles.dotFilled : styles.dotEmpty
                            ]}
                        />
                    ))}
                </View>

                {isSuccess ? (
                    <View style={styles.successContainer}>
                        <Text style={styles.successIcon}>🎉</Text>
                        <Text style={styles.successText}>가입을 환영합니다!</Text>
                    </View>
                ) : (
                    renderKeypad()
                )}
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
        justifyContent: 'space-between',
        paddingHorizontal: RFValue(24),
        paddingTop: RFValue(60),
        paddingBottom: Platform.OS === 'ios' ? RFValue(40) : RFValue(20),
    },
    headerSection: {
        alignItems: 'center',
    },
    title: {
        fontSize: RFValue(24),
        fontWeight: 'bold',
        color: '#111',
        marginBottom: RFValue(12),
        textAlign: 'center',
    },
    subtitle: {
        fontSize: RFValue(15),
        color: '#6B7280',
        textAlign: 'center',
        lineHeight: RFValue(22),
    },
    dotsContainer: {
        flexDirection: 'row',
        justifyContent: 'center',
        alignItems: 'center',
        marginVertical: RFValue(40),
        gap: RFValue(12),
    },
    dot: {
        width: RFValue(14),
        height: RFValue(14),
        borderRadius: RFValue(7),
    },
    dotEmpty: {
        backgroundColor: '#E5E7EB',
    },
    dotFilled: {
        backgroundColor: '#A3E635',
    },
    keypadContainer: {
        width: '100%',
        paddingHorizontal: RFValue(10),
    },
    keypadRow: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        marginBottom: RFValue(16),
    },
    keypadKey: {
        width: '30%',
        aspectRatio: 1.5,
        justifyContent: 'center',
        alignItems: 'center',
        borderRadius: RFValue(12),
    },
    keypadText: {
        fontSize: RFValue(28),
        fontWeight: '600',
        color: '#111',
    },
    successContainer: {
        flex: 1,
        justifyContent: 'center',
        alignItems: 'center',
    },
    successIcon: {
        fontSize: RFValue(60),
        marginBottom: RFValue(16),
    },
    successText: {
        fontSize: RFValue(20),
        fontWeight: 'bold',
        color: '#A3E635',
    }
});

export default PinNumberSetupScreen;
