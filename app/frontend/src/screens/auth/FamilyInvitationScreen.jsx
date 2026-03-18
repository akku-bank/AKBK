import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity, SafeAreaView, Platform, Image } from 'react-native';
import { RFValue } from 'react-native-responsive-fontsize';

const FamilyInvitationScreen = ({ navigation, route }) => {
    const { inviterName = '부모' } = route.params || {};

    const handleAccept = () => {
        // 초대 수락 -> 소셜 로그인으로 ~ (임시 토큰 등 정보 유지 필요)
        navigation.navigate('SocialLogin', { invitationAccepted: true });
    };

    const handleReject = () => {
        // 초대 거절 -> 앱 닫기 or 홈 화면으로
        navigation.navigate('SocialLogin');
    };

    return (
        <SafeAreaView style={styles.safeArea}>
            <View style={styles.container}>
                <View style={styles.contentSection}>
                    <View style={styles.imageWrapper}>
                        <Image
                            source={require('../../assets/croco/croco_parents.png')}
                            style={styles.inviterImage}
                            resizeMode="contain"
                        />
                    </View>

                    <Text style={styles.title}>
                        <Text style={styles.highlight}>{inviterName}</Text>님이{'\n'}
                        가족으로 초대했어요!
                    </Text>
                    <Text style={styles.subtitle}>
                        초대를 수락하고 아꾸뱅꾸에서{'\n'}함께 금융 생활을 시작해볼까요?
                    </Text>
                </View>

                <View style={styles.buttonSection}>
                    <TouchableOpacity
                        style={styles.acceptButton}
                        onPress={handleAccept}
                        activeOpacity={0.8}
                    >
                        <Text style={styles.acceptButtonText}>수락하고 시작하기</Text>
                    </TouchableOpacity>

                    <TouchableOpacity
                        style={styles.rejectButton}
                        onPress={handleReject}
                        activeOpacity={0.8}
                    >
                        <Text style={styles.rejectButtonText}>다음에 할게요</Text>
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
        paddingTop: RFValue(80),
        paddingBottom: Platform.OS === 'ios' ? RFValue(20) : RFValue(16),
    },
    contentSection: {
        alignItems: 'center',
    },
    imageWrapper: {
        width: RFValue(100),
        height: RFValue(100),
        backgroundColor: '#F3F4F6',
        borderRadius: RFValue(50),
        justifyContent: 'center',
        alignItems: 'center',
        marginBottom: RFValue(24),
        overflow: 'hidden',
    },
    inviterImage: {
        width: '120%',
        height: '120%',
    },
    title: {
        fontSize: RFValue(24),
        fontWeight: 'bold',
        color: '#111',
        textAlign: 'center',
        marginBottom: RFValue(16),
        lineHeight: RFValue(34),
    },
    highlight: {
        color: '#A3E635',
    },
    subtitle: {
        fontSize: RFValue(15),
        color: '#6B7280',
        lineHeight: RFValue(24),
        textAlign: 'center',
        fontWeight: '500',
    },
    buttonSection: {
        gap: RFValue(12),
    },
    acceptButton: {
        width: '100%',
        height: RFValue(54),
        borderRadius: RFValue(12),
        backgroundColor: '#A3E635',
        justifyContent: 'center',
        alignItems: 'center',
    },
    acceptButtonText: {
        fontSize: RFValue(16),
        fontWeight: 'bold',
        color: '#FFFFFF',
    },
    rejectButton: {
        width: '100%',
        height: RFValue(54),
        borderRadius: RFValue(12),
        backgroundColor: '#F3F4F6',
        justifyContent: 'center',
        alignItems: 'center',
    },
    rejectButtonText: {
        fontSize: RFValue(15),
        fontWeight: '600',
        color: '#6B7280',
    }
});

export default FamilyInvitationScreen;
