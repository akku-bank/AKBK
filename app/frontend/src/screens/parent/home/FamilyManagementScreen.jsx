import React, { useState, useEffect } from 'react';
import { View, StyleSheet, SafeAreaView, TouchableOpacity, ScrollView, Alert, Modal, TextInput, Image } from 'react-native';
import { scale, verticalScale } from 'react-native-size-matters';
import CustomText from '../../../components/common/CustomText';
import CustomTextInput from '../../../components/common/CustomTextInput';
import api from '../../../api/axios';

const FamilyManagementScreen = ({ navigation }) => {
    const [profiles, setProfiles] = useState([]);
    const [editingId, setEditingId] = useState(null);
    const [editName, setEditName] = useState('');

    // Modal states
    const [isAddModalVisible, setIsAddModalVisible] = useState(false);
    const [addName, setAddName] = useState('');
    const [addBirth, setAddBirth] = useState('');
    const [addRole, setAddRole] = useState('CHILD');

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
        setAddName('');
        setAddBirth('');
        setAddRole('CHILD');
        setIsAddModalVisible(true);
    };

    const submitAddProfile = async () => {
        if (!addName.trim() || !addBirth.trim()) {
            Alert.alert('알림', '이름과 생년월일(YYYY-MM-DD)을 모두 입력해주세요.');
            return;
        }
        const dateRegex = /^\d{4}-\d{2}-\d{2}$/;
        if (!dateRegex.test(addBirth)) {
            Alert.alert('알림', '생년월일 형식을 YYYY-MM-DD (예: 2015-05-05)로 맞춰주세요.');
            return;
        }
        try {
            await api.post('/families/members', { name: addName, role: addRole, birthDate: addBirth });
            Alert.alert('프로필 생성 완료', `${addName}의 계정이 추가되었습니다.`);
            setIsAddModalVisible(false);
            fetchProfiles();
        } catch (error) {
            console.error('Profile create error', error);
            Alert.alert('오류', '프로필 추가 중 문제가 발생했습니다.');
        }
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

            <Modal visible={isAddModalVisible} transparent={true} animationType="fade">
                <View style={styles.modalOverlay}>
                    <View style={styles.modalContent}>
                        <CustomText style={styles.modalTitle}>새 가족 추가</CustomText>
                        <TextInput style={styles.modalInput} placeholder="이름 (예: 안싸피)" placeholderTextColor="#9CA3AF" value={addName} onChangeText={setAddName} />
                        <TextInput style={[styles.modalInput, { marginTop: 12 }]} placeholder="생년월일 (예: 2015-05-05)" placeholderTextColor="#9CA3AF" value={addBirth} onChangeText={setAddBirth} />

                        <View style={{ flexDirection: 'row', marginTop: 12, marginBottom: 20, justifyContent: 'space-around' }}>
                            <TouchableOpacity style={[styles.roleBtn, addRole === 'CHILD' && styles.roleBtnActive]} onPress={() => setAddRole('CHILD')}>
                                <CustomText style={addRole === 'CHILD' ? styles.roleTextActive : styles.roleText}>자녀 추가</CustomText>
                            </TouchableOpacity>
                            <TouchableOpacity style={[styles.roleBtn, addRole === 'PARENT' && styles.roleBtnActive]} onPress={() => setAddRole('PARENT')}>
                                <CustomText style={addRole === 'PARENT' ? styles.roleTextActive : styles.roleText}>배우자 추가</CustomText>
                            </TouchableOpacity>
                        </View>

                        <View style={styles.modalActionRow}>
                            <TouchableOpacity style={[styles.modalBtn, { backgroundColor: '#E5E7EB' }]} onPress={() => setIsAddModalVisible(false)}>
                                <CustomText style={{ color: '#4B5563', fontWeight: 'bold' }}>취소</CustomText>
                            </TouchableOpacity>
                            <TouchableOpacity style={[styles.modalBtn, { backgroundColor: '#3B82F6' }]} onPress={submitAddProfile}>
                                <CustomText style={{ color: '#FFF', fontWeight: 'bold' }}>저장하기</CustomText>
                            </TouchableOpacity>
                        </View>
                    </View>
                </View>
            </Modal>

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
    statusPending: { color: '#F59E0B' },
    statusActive: { color: '#10B981' },

    qrButton: { backgroundColor: '#F3F4F6', paddingHorizontal: scale(16), paddingVertical: verticalScale(8), borderRadius: scale(12) },
    qrButtonText: { fontSize: scale(14), fontWeight: 'bold', color: '#4B5563' },

    detailButton: { backgroundColor: '#EFF6FF', paddingHorizontal: scale(16), paddingVertical: verticalScale(8), borderRadius: scale(12) },
    detailButtonText: { fontSize: scale(14), fontWeight: 'bold', color: '#3B82F6' },

    editInput: { flex: 1, backgroundColor: '#F9FAFB', borderWidth: 1, borderColor: '#D1D5DB', borderRadius: scale(8), paddingHorizontal: scale(12), paddingVertical: verticalScale(6), fontSize: scale(14), color: '#111', marginRight: scale(8) },
    actionBtn: { paddingHorizontal: scale(8), paddingVertical: verticalScale(6) },

    infoBox: { marginTop: verticalScale(32), backgroundColor: '#EFF6FF', padding: scale(16), borderRadius: scale(12) },
    infoTitle: { fontSize: scale(14), fontWeight: 'bold', color: '#1E3A8A', marginBottom: verticalScale(8) },
    infoText: { fontSize: scale(13), color: '#1E40AF', lineHeight: 20 },

    modalOverlay: { flex: 1, backgroundColor: 'rgba(0,0,0,0.5)', justifyContent: 'center', alignItems: 'center' },
    modalContent: { width: '85%', backgroundColor: '#FFF', borderRadius: scale(16), padding: scale(20) },
    modalTitle: { fontSize: scale(18), fontWeight: 'bold', color: '#111', marginBottom: verticalScale(16), textAlign: 'center' },
    modalInput: { width: '100%', height: scale(45), backgroundColor: '#F9FAFB', borderWidth: 1, borderColor: '#D1D5DB', borderRadius: scale(8), paddingHorizontal: scale(12), fontSize: scale(14), color: '#111' },
    roleBtn: { paddingVertical: verticalScale(8), paddingHorizontal: scale(16), borderRadius: scale(8), backgroundColor: '#F3F4F6' },
    roleBtnActive: { backgroundColor: '#DBEAFE' },
    roleText: { color: '#6B7280', fontWeight: 'bold' },
    roleTextActive: { color: '#2563EB', fontWeight: 'bold' },
    modalActionRow: { flexDirection: 'row', justifyContent: 'space-between', gap: scale(12) },
    modalBtn: { flex: 1, height: scale(45), justifyContent: 'center', alignItems: 'center', borderRadius: scale(8) }
});

export default FamilyManagementScreen;
