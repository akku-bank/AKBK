import React, { useState } from 'react';
import { View, StyleSheet, SafeAreaView, TouchableOpacity, Alert, ScrollView, Image } from 'react-native';
import { scale, verticalScale } from 'react-native-size-matters';
import CustomText from '../../../components/common/CustomText';
import CustomTextInput from '../../../components/common/CustomTextInput';

const ChildEditProfileScreen = ({ navigation }) => {
    const [nickname, setNickname] = useState('김싸피');
    const [statusMessage, setStatusMessage] = useState('돈을 아끼자!');

    const handleSave = () => {
        Alert.alert('저장 완료', '프로필이 수정되었습니다.', [{ text: '확인', onPress: () => navigation.goBack() }]);
    };

    return (
        <SafeAreaView style={styles.safeArea}>
            <View style={styles.header}>
                <TouchableOpacity style={styles.backButton} onPress={() => navigation.goBack()}>
                    <CustomText style={styles.backButtonText}>←</CustomText>
                </TouchableOpacity>
                <CustomText style={styles.headerTitle}>프로필 수정</CustomText>
                <View style={{ width: scale(32) }} />
            </View>

            <ScrollView contentContainerStyle={styles.container}>
                <View style={styles.avatarSection}>
                    <View style={styles.avatarCircle}>
                        <Image source={require('../../../assets/croco/croco_face.png')} style={styles.avatarImage} resizeMode="contain" />
                    </View>
                </View>

                <View style={styles.inputSection}>
                    <CustomText style={styles.label}>닉네임</CustomText>
                    <CustomTextInput
                        style={styles.input}
                        value={nickname}
                        onChangeText={setNickname}
                        maxLength={10}
                        placeholder="닉네임을 입력하세요"
                        placeholderTextColor="#9CA3AF"
                    />

                    <CustomText style={styles.label}>상태 메시지</CustomText>
                    <CustomTextInput
                        style={styles.input}
                        value={statusMessage}
                        onChangeText={setStatusMessage}
                        maxLength={20}
                        placeholder="상태를 입력하세요"
                        placeholderTextColor="#9CA3AF"
                    />
                </View>

            </ScrollView>

            <View style={styles.footer}>
                <TouchableOpacity style={styles.saveButton} onPress={handleSave}>
                    <CustomText style={styles.saveButtonText}>저장하기</CustomText>
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

    avatarSection: { alignItems: 'center', marginBottom: verticalScale(40) },
    avatarCircle: { width: scale(100), height: scale(100), borderRadius: scale(50), backgroundColor: '#E5E7EB', justifyContent: 'center', alignItems: 'center', marginBottom: verticalScale(16), overflow: 'hidden' },
    avatarImage: { width: '80%', height: '80%' },
    avatarEditBtn: { paddingVertical: verticalScale(8), paddingHorizontal: scale(16), backgroundColor: '#F3F4F6', borderRadius: scale(20) },
    avatarEditBtnText: { fontSize: scale(13), fontWeight: 'bold', color: '#4B5563' },

    inputSection: { marginBottom: verticalScale(30) },
    label: { fontSize: scale(14), fontWeight: 'bold', color: '#4B5563', marginBottom: verticalScale(8) },
    input: { backgroundColor: '#F9FAFB', borderWidth: 1, borderColor: '#E5E7EB', borderRadius: scale(12), paddingHorizontal: scale(16), paddingVertical: verticalScale(14), fontSize: scale(16), color: '#111', marginBottom: verticalScale(24) },

    footer: { padding: scale(16), backgroundColor: '#FFFFFF', borderTopWidth: 1, borderTopColor: '#F3F4F6' },
    saveButton: { backgroundColor: '#A3E635', paddingVertical: verticalScale(16), borderRadius: scale(16), alignItems: 'center' },
    saveButtonText: { fontSize: scale(16), fontWeight: 'bold', color: '#111' }
});

export default ChildEditProfileScreen;
