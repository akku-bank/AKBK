import React from 'react';
import { View, StyleSheet, SafeAreaView, TouchableOpacity, Image } from 'react-native';
import { scale, verticalScale } from 'react-native-size-matters';
import CustomText from '../../../components/common/CustomText';

const FriendAlreadyScreen = ({ navigation }) => {
    return (
        <SafeAreaView style={styles.safeArea}>
            <View style={styles.container}>
                <Image source={require('../../../assets/croco/croco_face.png')} style={styles.image} resizeMode="contain" />

                <CustomText style={styles.title}>아앗! 잠깐만요!</CustomText>

                <View style={styles.card}>
                    <CustomText style={styles.emoji}>👫</CustomText>
                    <CustomText style={styles.message}>이미 친구 등록이 되어있어요!</CustomText>
                    <CustomText style={styles.subMessage}>
                        친구 목록에서 친구의 타운에 방문하거나{'\n'}오른쪽 송금 버튼을 눌러 용돈을 보내보세요.
                    </CustomText>
                </View>

            </View>
            <View style={styles.footer}>
                <TouchableOpacity style={styles.mainButton} onPress={() => navigation.navigate('FriendList')}>
                    <CustomText style={styles.mainButtonText}>내 친구 목록 보러가기</CustomText>
                </TouchableOpacity>
            </View>
        </SafeAreaView>
    );
};

const styles = StyleSheet.create({
    safeArea: { flex: 1, backgroundColor: '#F3F4F6' },
    container: {
        flex: 1,
        alignItems: 'center',
        justifyContent: 'center',
        paddingHorizontal: scale(20),
    },
    image: {
        width: scale(120),
        height: scale(120),
        marginBottom: verticalScale(20),
    },
    title: {
        fontSize: scale(26),
        fontWeight: '900',
        color: '#111',
        marginBottom: verticalScale(24),
    },
    card: {
        backgroundColor: '#FFFFFF',
        borderRadius: scale(20),
        padding: scale(24),
        alignItems: 'center',
        width: '100%',
        shadowColor: '#000',
        shadowOffset: { width: 0, height: verticalScale(4) },
        shadowOpacity: 0.05,
        shadowRadius: scale(12),
        elevation: 3,
    },
    emoji: {
        fontSize: scale(48),
        marginBottom: verticalScale(16),
    },
    message: {
        fontSize: scale(18),
        fontWeight: 'bold',
        color: '#111',
        marginBottom: verticalScale(12),
        textAlign: 'center',
    },
    subMessage: {
        fontSize: scale(14),
        color: '#6B7280',
        lineHeight: 22,
        textAlign: 'center',
    },
    footer: {
        padding: scale(20),
        backgroundColor: '#F3F4F6',
    },
    mainButton: {
        backgroundColor: '#A3E635',
        paddingVertical: verticalScale(16),
        borderRadius: scale(16),
        alignItems: 'center',
    },
    mainButtonText: {
        fontSize: scale(16),
        fontWeight: 'bold',
        color: '#111',
    }
});

export default FriendAlreadyScreen;
