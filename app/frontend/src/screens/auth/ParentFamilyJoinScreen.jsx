import React, { useState, useEffect } from 'react';
import { View, Text, StyleSheet, TouchableOpacity, SafeAreaView, Platform, ActivityIndicator } from 'react-native';
import CustomText from '../../components/common/CustomText';
import { RFValue } from 'react-native-responsive-fontsize';
import api from '../../api/axios';

const ParentFamilyJoinScreen = ({ navigation, route }) => {
    // QRScanScreen에서 넘어온 정보
    const { tempToken, role, familyCode } = route.params || {};
    const [isLoading, setIsLoading] = useState(true);
    const [familyMembers, setFamilyMembers] = useState([]);
    const [selectedMemberId, setSelectedMemberId] = useState(null);

    useEffect(() => {
        const fetchFamilyMembers = async () => {
            try {
                if (!familyCode) {
                    setIsLoading(false);
                    return;
                }
                const response = await api.get(`/families/members?familyCode=${familyCode}`);
                const members = response.data?.data || [];
                const parents = members.filter(m => m.role === 'PARENT');
                setFamilyMembers(parents);
                setIsLoading(false);
            } catch (error) {
                console.error('Family Join Preview Error:', error);
                setIsLoading(false);
            }
        };

        fetchFamilyMembers();
    }, [familyCode]);

    const handleJoin = async () => {
        if (!selectedMemberId) return;
        const selectedMember = familyMembers.find(m => m.id === selectedMemberId);

        try {
            setIsLoading(true);

            // 실제 회원가입 API 호출 (배우자의 역할과 이름 등록, 실제 토큰 발급)
            const response = await api.post('/auth/signup',
                { role: 'PARENT', name: selectedMember.name },
                { headers: { Authorization: `Bearer ${tempToken}` } }
            );

            const payload = response.data?.data || response.data || {};
            const resolvedToken = payload.signupToken || payload.accessToken || payload.token || tempToken;

            // 방금 받은 토큰으로 배우자의 방에 합류 (QR코드 스캔 결과값)
            if (familyCode && familyCode !== 'mock-family-code') {
                try {
                    await api.post('/families/join',
                        { scannedQrCode: familyCode },
                        { headers: { Authorization: `Bearer ${resolvedToken}` } }
                    );
                    console.log('Spouse Joined Family Successfully');
                } catch (e) {
                    console.error('Spouse Join API Error:', e.response?.data || e.message);
                }
            } else {
                console.log('Skipping API join due to mock-family-code');
            }

            await new Promise(resolve => setTimeout(resolve, 500));

            // 배우자 합류 성공 -> 은행 계좌연결 생략하고 바로 간편 비밀번호 설정으로
            navigation.replace('PinNumberSetup', {
                tempToken: resolvedToken,
                role: 'PARENT',
                name: selectedMember.name
            });
        } catch (error) {
            console.error('Family Join Error:', error.response?.data || error.message);
            setIsLoading(false);
        }
    };

    return (
        <SafeAreaView style={styles.safeArea}>
            <View style={styles.container}>
                <View style={styles.headerSection}>
                    <CustomText style={styles.title}>환영합니다!</CustomText>
                    <CustomText style={styles.subtitle}>
                        배우자가 등록한{'\n'}내 이름을 선택해주세요.
                    </CustomText>
                </View>

                <View style={styles.listSection}>
                    {isLoading && familyMembers.length === 0 ? (
                        <ActivityIndicator size="large" color="#A3E635" style={{ marginTop: 40 }} />
                    ) : (
                        familyMembers.map((member) => (
                            <TouchableOpacity
                                key={member.id}
                                style={[
                                    styles.memberCard,
                                    selectedMemberId === member.id && styles.memberCardSelected
                                ]}
                                onPress={() => setSelectedMemberId(member.id)}
                                activeOpacity={0.8}
                            >
                                <CustomText style={styles.memberEmoji}>👩‍❤️‍👨</CustomText>
                                <CustomText
                                    style={[
                                        styles.memberName,
                                        selectedMemberId === member.id && styles.memberNameSelected
                                    ]}
                                >
                                    {member.name}
                                </CustomText>
                            </TouchableOpacity>
                        ))
                    )}
                </View>

                <View style={styles.buttonSection}>
                    <TouchableOpacity
                        style={[
                            styles.submitButton,
                            !selectedMemberId && styles.submitButtonDisabled
                        ]}
                        onPress={handleJoin}
                        disabled={!selectedMemberId || isLoading}
                        activeOpacity={0.8}
                    >
                        {isLoading && selectedMemberId ? (
                            <ActivityIndicator color="#FFFFFF" />
                        ) : (
                            <CustomText style={styles.submitButtonText}>선택 완료</CustomText>
                        )}
                    </TouchableOpacity>
                </View>
            </View>
        </SafeAreaView>
    );
};

const styles = StyleSheet.create({
    safeArea: { flex: 1, backgroundColor: '#FFFFFF' },
    container: { flex: 1, justifyContent: 'space-between', paddingHorizontal: RFValue(24), paddingTop: RFValue(50), paddingBottom: Platform.OS === 'ios' ? RFValue(20) : RFValue(16) },
    headerSection: { marginBottom: RFValue(20) },
    title: { fontSize: RFValue(26), fontWeight: 'bold', color: '#111', marginBottom: RFValue(12) },
    subtitle: { fontSize: RFValue(15), color: '#6B7280', lineHeight: RFValue(22) },
    listSection: { flex: 1, gap: RFValue(12), marginTop: RFValue(20) },
    memberCard: { flexDirection: 'row', alignItems: 'center', paddingVertical: RFValue(20), paddingHorizontal: RFValue(16), backgroundColor: '#F9FAFB', borderRadius: RFValue(16), borderWidth: 1, borderColor: '#E5E7EB' },
    memberCardSelected: { backgroundColor: '#EFF6FF', borderColor: '#A3E635' },
    memberEmoji: { fontSize: RFValue(24), marginRight: RFValue(16) },
    memberName: { fontSize: RFValue(18), fontWeight: 'bold', color: '#374151' },
    memberNameSelected: { color: '#A3E635' },
    buttonSection: { paddingTop: RFValue(12) },
    submitButton: { width: '100%', backgroundColor: '#A3E635', height: RFValue(54), borderRadius: RFValue(12), justifyContent: 'center', alignItems: 'center' },
    submitButtonDisabled: { backgroundColor: '#E5E7EB' },
    submitButtonText: { color: '#FFFFFF', fontSize: RFValue(16), fontWeight: 'bold' }
});

export default ParentFamilyJoinScreen;
