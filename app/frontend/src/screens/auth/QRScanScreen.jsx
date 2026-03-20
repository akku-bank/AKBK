import React, { useState, useEffect } from 'react';
import { View, Text, StyleSheet, TouchableOpacity, SafeAreaView, Platform, ActivityIndicator, Alert } from 'react-native';
import CustomText from '../../components/common/CustomText';
import { RFValue } from 'react-native-responsive-fontsize';
import { CameraView, useCameraPermissions } from 'expo-camera';

const QRScanScreen = ({ navigation, route }) => {
    const { tempToken, role } = route.params || {};
    const [scanned, setScanned] = useState(false);
    const [permission, requestPermission] = useCameraPermissions();

    useEffect(() => {
        if (!permission?.granted && permission?.canAskAgain) {
            requestPermission();
        }
    }, [permission]);

    const handleBarCodeScanned = ({ type, data }) => {
        setScanned(true);
        // 실제 API 연동 시 QR 정보 전송 필요 -> 여기 어떻게 테스트할까요 ..
        // (임시) 일단 다음 화면으로
        if (role === 'PARENT') {
            navigation.replace('ParentFamilyJoin', { tempToken, role, familyCode: data || "mock-family-code" });
        } else {
            navigation.replace('ChildFamilyJoin', { tempToken, role, familyCode: data || "mock-family-code" });
        }
    };

    const handleMockScan = () => {
        setScanned(true);
        setTimeout(() => {
            if (role === 'PARENT') {
                navigation.replace('ParentFamilyJoin', { tempToken, role, familyCode: "mock-family-code" });
            } else {
                navigation.replace('ChildFamilyJoin', { tempToken, role, familyCode: "mock-family-code" });
            }
        }, 1500);
    };

    if (!permission) {
        return (
            <SafeAreaView style={styles.safeArea}>
                <View style={styles.centeredContainer}>
                    <ActivityIndicator size="large" color="#FFFFFF" />
                </View>
            </SafeAreaView>
        );
    }

    if (!permission.granted) {
        return (
            <SafeAreaView style={styles.safeArea}>
                <View style={styles.centeredContainer}>
                    <CustomText style={styles.title}>카메라 권한이 필요합니다.</CustomText>
                    <TouchableOpacity style={styles.mockScanButton} onPress={requestPermission}>
                        <CustomText style={styles.mockScanButtonText}>권한 요청하기</CustomText>
                    </TouchableOpacity>
                    <TouchableOpacity style={[styles.mockScanButton, { marginTop: RFValue(12), backgroundColor: '#4B5563' }]} onPress={handleMockScan}>
                        <CustomText style={styles.mockScanButtonText}>건너뛰기(임시)</CustomText>
                    </TouchableOpacity>
                </View>
            </SafeAreaView>
        );
    }

    //부모용 QR 스캔은 다른 스크린에서 관리
    return (
        <SafeAreaView style={styles.safeArea}>
            <View style={styles.container}>
                <View style={styles.headerSection}>
                    <CustomText style={styles.title}>가족 QR 스캔</CustomText>
                    <CustomText style={styles.subtitle}>부모님의 화면에 있는 QR코드를{'\n'}스캔해주세요.</CustomText>
                </View>

                <View style={styles.cameraPlaceholder}>
                    <CameraView
                        style={styles.camera}
                        facing="back"
                        onBarcodeScanned={scanned ? undefined : handleBarCodeScanned}
                        barcodeScannerSettings={{
                            barcodeTypes: ["qr"],
                        }}
                    >
                        <View style={styles.scanTarget}>
                            <View style={styles.cornerTopLeft} />
                            <View style={styles.cornerTopRight} />
                            <View style={styles.cornerBottomLeft} />
                            <View style={styles.cornerBottomRight} />
                        </View>
                    </CameraView>
                </View>

                {/* 개발용 우회 버튼 */}
                <View style={styles.buttonSection}>
                    <TouchableOpacity
                        style={styles.mockScanButton}
                        onPress={handleMockScan}
                        disabled={scanned}
                        activeOpacity={0.8}
                    >
                        <CustomText style={styles.mockScanButtonText}>(임시) 스캔 완료</CustomText>
                    </TouchableOpacity>
                </View>
            </View>
        </SafeAreaView>
    );
};

const styles = StyleSheet.create({
    safeArea: {
        flex: 1,
        backgroundColor: '#111',
    },
    centeredContainer: {
        flex: 1,
        justifyContent: 'center',
        alignItems: 'center',
        paddingHorizontal: RFValue(24),
    },
    container: {
        flex: 1,
        justifyContent: 'space-between',
        paddingHorizontal: RFValue(24),
        paddingTop: RFValue(50),
        paddingBottom: Platform.OS === 'ios' ? RFValue(20) : RFValue(16),
    },
    headerSection: {
        alignItems: 'center',
        marginTop: RFValue(20),
    },
    title: {
        fontSize: RFValue(24),
        fontWeight: 'bold',
        color: '#FFFFFF',
        marginBottom: RFValue(12),
        textAlign: 'center',
    },
    subtitle: {
        fontSize: RFValue(15),
        color: '#D1D5DB',
        lineHeight: RFValue(22),
        textAlign: 'center',
    },
    cameraPlaceholder: {
        flex: 1,
        marginVertical: RFValue(40),
        borderRadius: RFValue(16),
        overflow: 'hidden',
    },
    camera: {
        flex: 1,
        justifyContent: 'center',
        alignItems: 'center',
    },
    scanTarget: {
        width: RFValue(220),
        height: RFValue(220),
        position: 'relative',
    },
    cornerTopLeft: {
        position: 'absolute',
        top: 0,
        left: 0,
        width: RFValue(40),
        height: RFValue(40),
        borderTopWidth: 4,
        borderLeftWidth: 4,
        borderColor: '#A3E635',
        borderTopLeftRadius: RFValue(12),
    },
    cornerTopRight: {
        position: 'absolute',
        top: 0,
        right: 0,
        width: RFValue(40),
        height: RFValue(40),
        borderTopWidth: 4,
        borderRightWidth: 4,
        borderColor: '#A3E635',
        borderTopRightRadius: RFValue(12),
    },
    cornerBottomLeft: {
        position: 'absolute',
        bottom: 0,
        left: 0,
        width: RFValue(40),
        height: RFValue(40),
        borderBottomWidth: 4,
        borderLeftWidth: 4,
        borderColor: '#A3E635',
        borderBottomLeftRadius: RFValue(12),
    },
    cornerBottomRight: {
        position: 'absolute',
        bottom: 0,
        right: 0,
        width: RFValue(40),
        height: RFValue(40),
        borderBottomWidth: 4,
        borderRightWidth: 4,
        borderColor: '#A3E635',
        borderBottomRightRadius: RFValue(12),
    },
    buttonSection: {
        gap: RFValue(12),
    },
    mockScanButton: {
        width: '100%',
        height: RFValue(54),
        borderRadius: RFValue(12),
        backgroundColor: '#A3E635',
        justifyContent: 'center',
        alignItems: 'center',
    },
    mockScanButtonText: {
        fontSize: RFValue(16),
        fontWeight: 'bold',
        color: '#FFFFFF',
    }
});

export default QRScanScreen;
