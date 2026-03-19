import React, { useState, useEffect } from 'react';
import { View, Text, StyleSheet, TouchableOpacity, SafeAreaView, Platform, ActivityIndicator } from 'react-native';
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
                // 실제 API
                // const response = await api.post('/families/join/preview', { familyCode });
                // setFamilyMembers(response.data.data); // [{id:1, name:'자녀1'}, {id:2, name:'자녀2'}]

                // 임시 데이터
                setTimeout(() => {
                    setFamilyMembers([
                        { id: 101, name: '사스케' },
                        { id: 102, name: '나루토' },
                    ]);
                    setIsLoading(false);
                }, 800);
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
            // 실제 가족 합류 API 연동은 여기서 ++ 아니면 SignUp 플로우 전체와 엮기
            // await api.post('/families/join', { familyCode, memberId: selectedMemberId });

            // 이름 들고 핀 번호 설정으로 넘어가기 -> 가입 마지막 단계
            navigation.replace('PinNumberSetup', {
                tempToken,
                role,
                name: selectedMember.name
            });
        } catch (error) {
            console.error('Family Join Error:', error);
            setIsLoading(false);
        }
    };

    return (
        <SafeAreaView style={styles.safeArea}>
            <View style={styles.container}>
                <View style={styles.headerSection}>
                    <Text style={styles.title}>거의 다 왔어요!</Text>
                    <Text style={styles.subtitle}>부모님이 등록해두신 내 이름을{'\n'}선택해주세요.</Text>
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
                                <Text style={styles.memberEmoji}></Text>
                                <Text
                                    style={[
                                        styles.memberName,
                                        selectedMemberId === member.id && styles.memberNameSelected
                                    ]}
                                >
                                    {member.name}
                                </Text>
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
                            <Text style={styles.submitButtonText}>선택 완료</Text>
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
