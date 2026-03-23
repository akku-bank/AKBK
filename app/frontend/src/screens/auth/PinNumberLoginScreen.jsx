import React, { useState } from 'react';
import { View, Text, StyleSheet, TouchableOpacity, SafeAreaView, Platform, Alert } from 'react-native';
import CustomText from '../../components/common/CustomText';
import { RFValue } from 'react-native-responsive-fontsize';
import useAuthStore from '../../store/useAuthStore';
import api from '../../api/axios';

const PinNumberLoginScreen = ({ navigation }) => {
    const [pin, setPin] = useState('');
    const { user, token, setAuthInfo } = useAuthStore();
    const name = user?.name;
    const role = user?.role;

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
            const response = await api.post('/auth/login', { userId: user?.id || user?.userKey || token, pin: finalPin });
            const payload = response.data?.data || response.data || {};
            const jwt = payload.token || payload.tempToken || payload.accessToken;

            await setAuthInfo(jwt, role, name);

            if (role === 'PARENT') {
                navigation.replace('ParentMain');
            } else {
                navigation.replace('ChildMain');
            }
        } catch (error) {
            console.error('PIN Login Error:', error);
            Alert.alert('오류', '비밀번호가 일치하지 않습니다.');
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
                                        <CustomText style={styles.keypadText}>{'<'}</CustomText>
                                    </TouchableOpacity>
                                );
                            }
                            return (
                                <TouchableOpacity key={keyIndex} style={styles.keypadKey} onPress={() => handleKeyPress(key)}>
                                    <CustomText style={styles.keypadText}>{key}</CustomText>
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
                    <CustomText style={styles.title}>비밀번호를 입력해주세요</CustomText>
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

                {renderKeypad()}
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
    }
});

export default PinNumberLoginScreen;
