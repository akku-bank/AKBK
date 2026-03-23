import React, { useState } from 'react';
import { View, StyleSheet, TouchableOpacity, SafeAreaView, Alert, KeyboardAvoidingView, Platform } from 'react-native';
import { scale, verticalScale } from 'react-native-size-matters';
import CustomText from '../../../components/common/CustomText';
import CustomTextInput from '../../../components/common/CustomTextInput';
import api from '../../../api/axios';

const ParentChildEditScreen = ({ navigation, route }) => {
    const { child } = route?.params || {};
    const [newName, setNewName] = useState(child?.name || '');

    const handleSave = async () => {
        if (!newName.trim()) {
            Alert.alert('알림', '변경할 이름을 입력해주세요.');
            return;
        }

        try {
            await api.patch(`/families/members/${child?.id}`, { name: newName });
            Alert.alert('변경 완료', '자녀 프로필 이름이 성공적으로 변경되었습니다.', [
                { text: '확인', onPress: () => navigation.goBack() }
            ]);
        } catch (e) {
            console.error('Child Edit Error:', e);
            Alert.alert('오류', '이름 변경에 실패했습니다.');
        }
    };

    return (
        <SafeAreaView style={styles.safeArea}>
            <View style={styles.header}>
                <TouchableOpacity style={styles.backButton} onPress={() => navigation.goBack()}>
                    <CustomText style={styles.backButtonText}>←</CustomText>
                </TouchableOpacity>
                <CustomText style={styles.headerTitle}>자녀 정보 수정</CustomText>
                <View style={{ width: scale(32) }} />
            </View>

            <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : undefined} style={{ flex: 1 }}>
                <View style={styles.container}>
                    <CustomText style={styles.description}>
                        가족 그룹 내에서 보여질 자녀의 이름을 변경할 수 있습니다.
                    </CustomText>

                    <View style={styles.inputGroup}>
                        <CustomText style={styles.label}>앱 내 노출 이름 (닉네임)</CustomText>
                        <CustomTextInput
                            style={styles.input}
                            value={newName}
                            onChangeText={setNewName}
                            placeholder="변경할 이름을 입력하세요"
                            placeholderTextColor="#9CA3AF"
                            maxLength={10}
                        />
                    </View>

                    <TouchableOpacity
                        style={[styles.submitButton, newName.trim() ? styles.submitButtonActive : styles.submitButtonDisabled]}
                        onPress={handleSave}
                        disabled={!newName.trim()}
                    >
                        <CustomText style={styles.submitButtonText}>저장하기</CustomText>
                    </TouchableOpacity>
                </View>
            </KeyboardAvoidingView>
        </SafeAreaView>
    );
};

const styles = StyleSheet.create({
    safeArea: { flex: 1, backgroundColor: '#FFFFFF' },
    header: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingHorizontal: scale(16), paddingVertical: verticalScale(16) },
    backButton: { width: scale(32), height: scale(32), justifyContent: 'center' },
    backButtonText: { fontSize: scale(22), fontWeight: 'bold', color: '#111' },
    headerTitle: { fontSize: scale(18), fontWeight: 'bold', color: '#111' },

    container: { flex: 1, paddingHorizontal: scale(20), paddingTop: verticalScale(20) },
    description: { fontSize: scale(14), color: '#6B7280', marginBottom: verticalScale(30), lineHeight: 20 },

    inputGroup: { marginBottom: verticalScale(24) },
    label: { fontSize: scale(14), fontWeight: 'bold', color: '#4B5563', marginBottom: verticalScale(8) },
    input: { backgroundColor: '#F3F4F6', borderRadius: scale(12), padding: scale(16), fontSize: scale(16), fontWeight: 'bold', color: '#111' },

    submitButton: { marginTop: verticalScale(20), paddingVertical: verticalScale(16), borderRadius: scale(12), alignItems: 'center' },
    submitButtonActive: { backgroundColor: '#A3E635' },
    submitButtonDisabled: { backgroundColor: '#E5E7EB' },
    submitButtonText: { fontSize: scale(16), fontWeight: 'bold', color: '#111' },
});

export default ParentChildEditScreen;
