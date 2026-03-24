import React, { useState } from 'react';
import { View, StyleSheet, SafeAreaView, TouchableOpacity, Alert, ScrollView } from 'react-native';
import { scale, verticalScale } from 'react-native-size-matters';
import CustomText from '../../../components/common/CustomText';
import CustomTextInput from '../../../components/common/CustomTextInput';
import api from '../../../api/axios';

const ParentAccountCreateScreen = ({ navigation }) => {
    const [childName, setChildName] = useState('');
    const [childPhone, setChildPhone] = useState('');
    const [isLoading, setIsLoading] = useState(false);

    const handleCreate = () => {
        if (!childName) {
            Alert.alert('알림', '자녀 이름을 입력해주세요.');
            return;
        }

        Alert.alert(
            '계좌 개설',
            `${childName}의 계좌를 개설하시겠습니까?`,
            [
                { text: '취소', style: 'cancel' },
                {
                    text: '개설하기',
                    onPress: async () => {
                        setIsLoading(true);
                        try {
                            const res = await api.get('/home/parent');
                            const children = res.data?.data?.children || [];

                            const targetChild = children.find(c => c.name === childName);
                            if (!targetChild) {
                                Alert.alert('오류', '가족 목록에 해당 이름의 자녀가 없습니다. 먼저 QR 등록을 진행해주세요.');
                                setIsLoading(false);
                                return;
                            }

                            if (!targetChild.childId) {
                                Alert.alert('자녀 연동 필요', '자녀 계좌를 생성하려면 먼저 자녀 계정을 가족에 연동해주세요.');
                                setIsLoading(false);
                                return;
                            }

                            await api.post('/bank/accounts', {
                                childId: targetChild.childId,
                                accountType: 'CASH'
                            });

                            Alert.alert('완료', '계좌가 성공적으로 개설되었습니다!', [{ text: '확인', onPress: () => navigation.goBack() }]);
                        } catch (error) {
                            console.error('Account Create Error', error);
                            const message =
                                error?.response?.data?.message ||
                                error?.response?.data?.errorCode ||
                                '계좌 개설 중 문제가 발생했습니다.';
                            Alert.alert('오류', message);
                        } finally {
                            setIsLoading(false);
                        }
                    }
                }
            ]
        );
    };

    return (
        <SafeAreaView style={styles.safeArea}>
            <View style={styles.header}>
                <TouchableOpacity style={styles.backButton} onPress={() => navigation.goBack()}>
                    <CustomText style={styles.backButtonText}>←</CustomText>
                </TouchableOpacity>
                <CustomText style={styles.headerTitle}>자녀 계좌 개설</CustomText>
                <View style={{ width: scale(32) }} />
            </View>

            <ScrollView contentContainerStyle={styles.container}>
                <CustomText style={styles.pageTitle}>우리아이 첫 은행 계좌,{`\n`}안전하게 만들어주세요.</CustomText>

                <View style={styles.inputSection}>
                    <CustomText style={styles.label}>자녀 이름</CustomText>
                    <CustomTextInput
                        style={styles.input}
                        value={childName}
                        onChangeText={setChildName}
                        placeholder="실명을 입력하세요"
                        placeholderTextColor="#9CA3AF"
                    />

                    <CustomText style={styles.label}>자녀 휴대폰 번호 (선택)</CustomText>
                    <CustomTextInput
                        style={styles.input}
                        value={childPhone}
                        onChangeText={setChildPhone}
                        placeholder="010-0000-0000"
                        keyboardType="phone-pad"
                        placeholderTextColor="#9CA3AF"
                    />
                </View>

                <View style={styles.infoBox}>
                    <CustomText style={styles.infoTitle}>💡 부모님 동의가 필요해요</CustomText>
                    <CustomText style={styles.infoText}>미성년자 자녀의 계좌 개설을 위해 법정대리인의 동의 절차와 신분증 확인이 이후 단계에서 진행됩니다.</CustomText>
                </View>
            </ScrollView>

            <View style={styles.footer}>
                <TouchableOpacity
                    style={[styles.mainButton, (!childName || isLoading) && styles.disabledButton]}
                    onPress={handleCreate}
                    disabled={!childName || isLoading}
                >
                    <CustomText style={styles.mainButtonText}>{isLoading ? '개설 중...' : '다음 단계로'}</CustomText>
                </TouchableOpacity>
            </View>
        </SafeAreaView>
    );
};

const styles = StyleSheet.create({
    safeArea: { flex: 1, backgroundColor: '#FFFFFF' },
    header: {
        flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between',
        paddingHorizontal: scale(16), paddingVertical: verticalScale(16)
    },
    backButton: { width: scale(32), height: scale(32), justifyContent: 'center' },
    backButtonText: { fontSize: scale(22), fontWeight: 'bold', color: '#111' },
    headerTitle: { fontSize: scale(18), fontWeight: 'bold', color: '#111' },

    container: { flexGrow: 1, paddingHorizontal: scale(24), paddingTop: verticalScale(20) },

    pageTitle: { fontSize: scale(22), fontWeight: '900', color: '#111', lineHeight: 32, marginBottom: verticalScale(40) },

    inputSection: { marginBottom: verticalScale(30) },
    label: { fontSize: scale(14), fontWeight: 'bold', color: '#4B5563', marginBottom: verticalScale(8) },
    input: { backgroundColor: '#F9FAFB', borderWidth: 1, borderColor: '#E5E7EB', borderRadius: scale(12), paddingHorizontal: scale(16), paddingVertical: verticalScale(14), fontSize: scale(16), color: '#111', marginBottom: verticalScale(24) },

    infoBox: { backgroundColor: '#F0FDF4', padding: scale(16), borderRadius: scale(12) },
    infoTitle: { fontSize: scale(14), fontWeight: 'bold', color: '#166534', marginBottom: verticalScale(8) },
    infoText: { fontSize: scale(13), color: '#14532D', lineHeight: 20 },

    footer: { padding: scale(16), backgroundColor: '#FFFFFF', borderTopWidth: 1, borderTopColor: '#F3F4F6' },
    mainButton: { backgroundColor: '#A3E635', paddingVertical: verticalScale(16), borderRadius: scale(16), alignItems: 'center' },
    disabledButton: { backgroundColor: '#D1D5DB' },
    mainButtonText: { fontSize: scale(16), fontWeight: 'bold', color: '#111' }
});

export default ParentAccountCreateScreen;
