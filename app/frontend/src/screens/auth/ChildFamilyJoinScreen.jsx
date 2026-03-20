import React, { useState, useEffect } from 'react';
import { View, Text, StyleSheet, TouchableOpacity, SafeAreaView, Platform, ActivityIndicator } from 'react-native';
import CustomText from '../../components/common/CustomText';
import { RFValue } from 'react-native-responsive-fontsize';
import api from '../../api/axios';

const ChildFamilyJoinScreen = ({ navigation, route }) => {
    // QRScanScreen에서 넘어온 정보
    const { tempToken, role, familyCode } = route.params || {};
    const [isLoading, setIsLoading] = useState(true);
    const [familyMembers, setFamilyMembers] = useState([]);
    const [selectedMemberId, setSelectedMemberId] = useState(null);

    useEffect(() => {
        // 웹 개발용 임시 로직 !
        const fetchFamilyMembers = async () => {
            try {
                /* ==========================================
                   [진짜 가족 합류 명단 조회 API]
                   ========================================== 
                // 해당 코드를 통해 스캔한 가족 그룹에 미리 등록된 자녀 명단을 불러옵니다.
                // const response = await api.get(`/families/members?familyCode=${familyCode}`, { headers: { Authorization: `Bearer ${tempToken}` }});
                // setFamilyMembers(response.data.members || []);
                ========================================== */

                // --- 실제 연동 시 아래 블록 전체 삭제 ---
                setTimeout(() => {
                    setFamilyMembers([
                        { id: 101, name: '사스케' },
                        { id: 102, name: '나루토' },
                    ]);
                    setIsLoading(false);
                }, 800);
                // ------------------------------------
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

            
            // 실제 회원가입 API 호출 (자녀의 역할과 이름 등록, 실제 토큰 발급)
            const response = await api.post('/auth/signup',
                { role: 'CHILD', name: selectedMember.name },
                { headers: { Authorization: `Bearer ${tempToken}` } }
            );

            const payload = response.data?.data || response.data || {};
            const resolvedToken = payload.signupToken || payload.accessToken || payload.token || tempToken;

            // 방금 받은 토큰(혹은 임시토큰)으로 부모의 그룹에 합류 (QR코드 스캔 결과값)
            if (familyCode && familyCode !== 'mock-family-code') {
                try {
                    await api.post('/families/join', 
                        { scannedQrCode: familyCode },
                        { headers: { Authorization: `Bearer ${resolvedToken}` } }
                    );
                    console.log('Family Join API Success');
                } catch(e) {
                    console.error('Family Join API Error (It might be okay if already joined or testing):', e.response?.data || e.message);
                }
            } else {
                 console.log('Skipping API join due to mock-family-code');
            }


            // 가족 합류 로직은 모의(Mock)로 유지해달라는 요청 반영
            await new Promise(resolve => setTimeout(resolve, 500));

            navigation.replace('PinNumberSetup', {
                tempToken: resolvedToken,
                role: 'CHILD',
                name: selectedMember.name
            });
        } catch (error) {
            console.error('Family Join Error:', error.response?.data || error.message);
            setIsLoading(false);
            // 에러 나도 진행할지 여부 결정 (나중에 필요시 핸들링)
        }
    };

    return (
        <SafeAreaView style={styles.safeArea}>
            <View style={styles.container}>
                <View style={styles.headerSection}>
                    <CustomText style={styles.title}>거의 다 왔어요!</CustomText>
                    <CustomText style={styles.subtitle}>
                        {role === 'CHILD'
                            ? '부모님이 등록해두신 내 이름을\n선택해주세요.'
                            : '배우자가 등록해두신 내 이름을\n선택해주세요.'}
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
                                <CustomText style={styles.memberEmoji}></CustomText>
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
    safeArea: {
        flex: 1,
        backgroundColor: '#FFFFFF',
    },
    container: {
        flex: 1,
        justifyContent: 'space-between',
        paddingHorizontal: RFValue(24),
        paddingTop: RFValue(50),
        paddingBottom: Platform.OS === 'ios' ? RFValue(20) : RFValue(16),
    },
    headerSection: {
        marginBottom: RFValue(20),
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
        lineHeight: RFValue(22),
    },
    listSection: {
        flex: 1,
        gap: RFValue(12),
        marginTop: RFValue(20),
    },
    memberCard: {
        flexDirection: 'row',
        alignItems: 'center',
        paddingVertical: RFValue(20),
        paddingHorizontal: RFValue(16),
        backgroundColor: '#F9FAFB',
        borderRadius: RFValue(16),
        borderWidth: 1,
        borderColor: '#E5E7EB',
    },
    memberCardSelected: {
        backgroundColor: '#EFF6FF',
        borderColor: '#A3E635',
    },
    memberEmoji: {
        fontSize: RFValue(24),
        marginRight: RFValue(16),
    },
    memberName: {
        fontSize: RFValue(18),
        fontWeight: 'bold',
        color: '#374151',
    },
    memberNameSelected: {
        color: '#A3E635',
    },
    buttonSection: {
        paddingTop: RFValue(12),
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

export default ChildFamilyJoinScreen;
