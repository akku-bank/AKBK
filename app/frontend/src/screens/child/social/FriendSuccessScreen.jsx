import React from 'react';
import { View, StyleSheet, SafeAreaView, TouchableOpacity } from 'react-native';
import { scale, verticalScale } from 'react-native-size-matters';
import CustomText from '../../../components/common/CustomText';

const FriendSuccessScreen = ({ navigation }) => {
    return (
        <SafeAreaView style={styles.safeArea}>
            <View style={styles.centerContainer}>
                <CustomText style={styles.emoji}>🎉 🎊 🎇</CustomText>
                <CustomText style={styles.title}>친구 추가 완료!</CustomText>
                <CustomText style={styles.subtitle}>새로운 친구와 함께 타운을 구경해보세요.</CustomText>
            </View>
            <TouchableOpacity style={styles.button} onPress={() => navigation.navigate('FriendList')}>
                <CustomText style={styles.buttonText}>친구 목록으로 돌아가기</CustomText>
            </TouchableOpacity>
        </SafeAreaView>
    );
};

const styles = StyleSheet.create({
    safeArea: { flex: 1, backgroundColor: '#A3E635' }, // 라임색 폭죽 배경
    centerContainer: { flex: 1, justifyContent: 'center', alignItems: 'center', paddingHorizontal: scale(24) },
    emoji: { fontSize: scale(64), marginBottom: verticalScale(24) },
    title: { fontSize: scale(28), fontWeight: '900', color: '#111', marginBottom: verticalScale(8) },
    subtitle: { fontSize: scale(16), fontWeight: 'bold', color: '#4D7C0F', textAlign: 'center' },
    button: { backgroundColor: '#111', margin: scale(24), paddingVertical: verticalScale(16), borderRadius: scale(16), alignItems: 'center' },
    buttonText: { color: '#FFF', fontSize: scale(16), fontWeight: 'bold' }
});

export default FriendSuccessScreen;
