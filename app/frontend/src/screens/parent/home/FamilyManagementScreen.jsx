import React, { useState, useEffect } from 'react';
import { View, StyleSheet, SafeAreaView, TouchableOpacity, ScrollView, Alert } from 'react-native';
import { scale, verticalScale } from 'react-native-size-matters';
import CustomText from '../../../components/common/CustomText';
import CustomTextInput from '../../../components/common/CustomTextInput';
import api from '../../../api/axios';

const FamilyManagementScreen = ({ navigation }) => {
    const [profiles, setProfiles] = useState([]);
    const [editingId, setEditingId] = useState(null);
    const [editName, setEditName] = useState('');

    const fetchProfiles = async () => {
        try {
            const response = await api.get('/families/members');
            const members = response.data?.data?.members || [];
            const mappedProfiles = members
                .map(member => ({
                    id: member.userId || member.profileId,
                    name: member.name,
                    role: member.role,
                    status: member.userId ? '연동 완료' : '연동 대기중',
                    profileId: member.profileId
                }));
            setProfiles(mappedProfiles);
        } catch (error) {
            console.error("Failed to fetch family members:", error);
        }
    };

    useEffect(() => {
        fetchProfiles();
    }, []);

    const handleCreateProfile = () => {
        Alert.alert(
            '프로필 추가',
            '추가할 가족 구성원의 역할을 선택하세요.',
            [
                { text: '취소', style: 'cancel' },
                {
                    text: '자녀 추가',
                    onPress: async () => {
                        try {
                            const newName = `새 자녀 ${profiles.length + 1}`;
                            await api.post('/families/members', { name: newName, role: "CHILD", birthDate: "2015-01-01" });
                            Alert.alert('프로필 생성 완료', `${newName}의 계정이 추가되었습니다.`);
                            fetchProfiles();
                        } catch (error) { console.error('Profile create error', error); }
                    }
                },
                {
                    text: '부모(배우자) 추가',
                    onPress: async () => {
                        try {
                            const newName = `새 부모 ${profiles.length + 1}`;
                            await api.post('/families/members', { name: newName, role: "PARENT", birthDate: "1980-01-01" });
                            Alert.alert('프로필 생성 완료', `${newName}의 계정이 추가되었습니다.`);
                            fetchProfiles();
                        } catch (error) { console.error('Profile create error', error); }
                    }
                }
            ]
        );
    };

    const handleDelete = (memberId, name) => {
        Alert.alert('자녀 연동 해제', `${name}님의 프로필을 삭제하시겠습니까?`, [
            { text: '취소', style: 'cancel' },
            {
                text: '삭제', style: 'destructive',
                onPress: async () => {
                    try {
                        await api.delete(`/families/members/${memberId}`);
                        Alert.alert('삭제 완료', '가족 목록에서 제외되었습니다.');
                        fetchProfiles();
                    } catch (e) {
                        console.error('Delete error', e);
                        Alert.alert('오류', '삭제 중 문제가 발생했습니다.');
                    }
                }
            }
        ]);
    };

    const handleSaveEdit = async (memberId) => {
        if (!editName.trim()) {
            Alert.alert('알림', '이름을 입력해주세요.');
            return;
        }
        try {
            await api.patch(`/families/members/${memberId}`, { name: editName });
            setEditingId(null);
            setEditName('');
            fetchProfiles();
        } catch (e) {
            console.error('Update error', e);
            Alert.alert('오류', '이름 수정 중 문제가 발생했습니다.');
        }
    };

    return (
        <SafeAreaView style={styles.safeArea}>
            <View style={styles.header}>
                <TouchableOpacity style={styles.backButton} onPress={() => navigation.goBack()}>
                    <CustomText style={styles.backButtonText}>←</CustomText>
                </TouchableOpacity>
                <CustomText style={styles.headerTitle}>가족 관리 센터</CustomText>
                <View style={{ width: scale(32) }} />
            </View>

            <ScrollView contentContainerStyle={styles.container}>
                <View style={styles.sectionHeader}>
                    <CustomText style={styles.sectionTitle}>내 아이 프로필</CustomText>
                    <TouchableOpacity onPress={handleCreateProfile}>
                        <CustomText style={styles.addText}>+ 프로필 추가</CustomText>
                    </TouchableOpacity>
                </View>

                {profiles.map(profile => (
                    <View key={profile.id} style={styles.profileCard}>
                        {editingId === profile.id ? (
                            <View style={{ flex: 1, flexDirection: 'row', alignItems: 'center' }}>
                                <CustomTextInput
                                    style={styles.editInput}
                                    value={editName}
                                    onChangeText={setEditName}
                                    autoFocus
                                />
                                <TouchableOpacity style={styles.actionBtn} onPress={() => handleSaveEdit(profile.profileId)}>
                                    <CustomText style={{ color: '#10B981', fontWeight: 'bold' }}>저장</CustomText>
                                </TouchableOpacity>
                                <TouchableOpacity style={styles.actionBtn} onPress={() => setEditingId(null)}>
                                    <CustomText style={{ color: '#6B7280' }}>취소</CustomText>
                                </TouchableOpacity>
                            </View>
                        ) : (
                            <>
                                <View style={{ flex: 1 }}>
                                    <View style={{ flexDirection: 'row', alignItems: 'center', marginBottom: verticalScale(4) }}>
                                        <CustomText style={styles.profileName}>
                                            {profile.name} {profile.role === 'PARENT' ? '(부모)' : ''}
                                        </CustomText>
                                        <TouchableOpacity
                                            style={{ marginLeft: scale(8) }}
                                            onPress={() => {
                                                setEditingId(profile.id);
                                                setEditName(profile.name);
                                            }}
                                        >
                                            <CustomText style={{ fontSize: scale(12), color: '#3B82F6' }}>수정</CustomText>
                                        </TouchableOpacity>
                                        <TouchableOpacity
                                            style={{ marginLeft: scale(8) }}
                                            onPress={() => handleDelete(profile.profileId, profile.name)}
                                        >
                                            <CustomText style={{ fontSize: scale(12), color: '#EF4444' }}>삭제</CustomText>
                                        </TouchableOpacity>
                                    </View>
                                    <CustomText style={[styles.profileStatus, profile.status === '연동 완료' ? styles.statusActive : styles.statusPending]}>
                                        {profile.status}
                                    </CustomText>
                                </View>
                                {profile.status === '연동 대기중' && (
                                    <TouchableOpacity
                                        style={styles.qrButton}
                                        onPress={() => navigation.navigate('FamilyQrGenerator')}
                                    >
                                        <CustomText style={styles.qrButtonText}>QR 생성</CustomText>
                                    </TouchableOpacity>
                                )}
                                {profile.status === '연동 완료' && (
                                    <TouchableOpacity
                                        style={styles.detailButton}
                                        onPress={() => navigation.navigate('ParentHistoryScreen', { childName: profile.name })}
                                    >
                                        <CustomText style={styles.detailButtonText}>내역 보기</CustomText>
                                    </TouchableOpacity>
                                )}
                            </>
                        )}
                    </View>
                ))}

                <View style={styles.infoBox}>
                    <CustomText style={styles.infoTitle}>💡 가족 계정 연동이란?</CustomText>
                    <CustomText style={styles.infoText}>아이의 프로필을 생성한 후 QR 코드를 생성하세요. 아이의 기기에서 해당 QR을 스캔하면 계정이 안전하게 연결됩니다.</CustomText>
                </View>

            </ScrollView>
        </SafeAreaView>
    );
};

const styles = StyleSheet.create({
    safeArea: { flex: 1, backgroundColor: '#F3F4F6' },
    header: {
        flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between',
        paddingHorizontal: scale(16), paddingVertical: verticalScale(16),
        backgroundColor: '#FFFFFF',
    },
    backButton: { width: scale(32), height: scale(32), justifyContent: 'center' },
    backButtonText: { fontSize: scale(22), fontWeight: 'bold', color: '#111' },
    headerTitle: { fontSize: scale(18), fontWeight: 'bold', color: '#111' },

    container: { flexGrow: 1, padding: scale(16) },

    sectionHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: verticalScale(16) },
    sectionTitle: { fontSize: scale(18), fontWeight: 'bold', color: '#111' },
    addText: { fontSize: scale(14), fontWeight: 'bold', color: '#3B82F6' },

    profileCard: {
        flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center',
        backgroundColor: '#FFFFFF', padding: scale(20), borderRadius: scale(16),
        marginBottom: verticalScale(12),
        elevation: 2, shadowColor: '#000', shadowOffset: { width: 0, height: verticalScale(2) }, shadowOpacity: 0.05, shadowRadius: scale(8),
    },
    profileName: { fontSize: scale(16), fontWeight: 'bold', color: '#111', marginBottom: verticalScale(4) },
    profileStatus: { fontSize: scale(13), fontWeight: '600' },
    statusPending: { color: '#F59E0B' }, // 주황색
    statusActive: { color: '#10B981' }, // 초록색

    qrButton: { backgroundColor: '#F3F4F6', paddingHorizontal: scale(16), paddingVertical: verticalScale(8), borderRadius: scale(12) },
    qrButtonText: { fontSize: scale(14), fontWeight: 'bold', color: '#4B5563' },

    detailButton: { backgroundColor: '#EFF6FF', paddingHorizontal: scale(16), paddingVertical: verticalScale(8), borderRadius: scale(12) },
    detailButtonText: { fontSize: scale(14), fontWeight: 'bold', color: '#3B82F6' },

    editInput: { flex: 1, backgroundColor: '#F9FAFB', borderWidth: 1, borderColor: '#D1D5DB', borderRadius: scale(8), paddingHorizontal: scale(12), paddingVertical: verticalScale(6), fontSize: scale(14), color: '#111', marginRight: scale(8) },
    actionBtn: { paddingHorizontal: scale(8), paddingVertical: verticalScale(6) },

    infoBox: { marginTop: verticalScale(32), backgroundColor: '#EFF6FF', padding: scale(16), borderRadius: scale(12) },
    infoTitle: { fontSize: scale(14), fontWeight: 'bold', color: '#1E3A8A', marginBottom: verticalScale(8) },
    infoText: { fontSize: scale(13), color: '#1E40AF', lineHeight: 20 },
});

export default FamilyManagementScreen;
