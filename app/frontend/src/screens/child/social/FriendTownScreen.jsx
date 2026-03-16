import React from 'react';
import { View, StyleSheet, TouchableOpacity, SafeAreaView, ScrollView } from 'react-native';
import { scale, verticalScale } from 'react-native-size-matters';
import ChildAvatar from '../../../components/child/avatar/ChildAvatar';
import CustomText from '../../../components/common/CustomText';

const FriendTownScreen = ({ route, navigation }) => {
    // 파라미터 방어 코드
    const friendName = route?.params?.friendName || '친구';

    // 임시 뱃지 데이터
    const MOCK_BADGES = [
        { id: 1, name: '지구 수호자', icon: '🌍' },
        { id: 2, name: '동물 사랑', icon: '🐶' }
    ];

    return (
        <SafeAreaView style={styles.safeArea}>
            <View style={styles.header}>
                <TouchableOpacity style={styles.backButton} onPress={() => navigation.goBack()}>
                    <CustomText style={styles.backButtonText}>←</CustomText>
                </TouchableOpacity>
                <CustomText style={styles.headerTitle}>{friendName}의 타운</CustomText>
                <View style={{ width: scale(32) }} />
            </View>

            <ScrollView contentContainerStyle={styles.container} showsVerticalScrollIndicator={false}>

                <View style={styles.townScene}>
                    <CustomText style={styles.townGreeting}>"안녕! 내 타운에 온 걸 환영해!"</CustomText>
                    <ChildAvatar
                        size={200}
                        equipState={{
                            hair: 'none',
                            face: 'base_smile',
                            upper: 'none',
                            lower: 'none',
                            shoe: 'none',
                            hat: 'none',
                            wing: 'none'
                        }}
                    />
                    <View style={styles.platform} />
                </View>

                <View style={styles.infoCard}>
                    <CustomText style={styles.infoTitle}>자랑스러운 뱃지 컬렉션</CustomText>
                    <CustomText style={styles.infoSubtitle}>{friendName}가 세상을 따뜻하게 만든 기록이에요.</CustomText>

                    <View style={styles.badgeRow}>
                        {MOCK_BADGES.map(badge => (
                            <View key={badge.id} style={styles.badgeItem}>
                                <View style={styles.badgeIconBox}>
                                    <CustomText style={styles.badgeIcon}>{badge.icon}</CustomText>
                                </View>
                                <CustomText style={styles.badgeName}>{badge.name}</CustomText>
                            </View>
                        ))}
                    </View>
                </View>

                <View style={styles.actionRow}>
                    <TouchableOpacity style={[styles.actionButton, styles.giftButton]} activeOpacity={0.8}>
                        <CustomText style={styles.actionButtonText}>선물 보내기 🎁</CustomText>
                    </TouchableOpacity>
                    <TouchableOpacity style={[styles.actionButton, styles.pokeButton]} activeOpacity={0.8}>
                        <CustomText style={[styles.actionButtonText, { color: '#A3E635' }]}>콕 찌르기 👉</CustomText>
                    </TouchableOpacity>
                </View>

            </ScrollView>
        </SafeAreaView>
    );
};

const styles = StyleSheet.create({
    safeArea: {
        flex: 1,
        backgroundColor: '#F3F4F6',
    },
    header: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        paddingHorizontal: scale(16),
        paddingVertical: verticalScale(16),
        backgroundColor: '#F3F4F6',
    },
    backButton: {
        width: scale(32),
        height: scale(32),
        justifyContent: 'center',
    },
    backButtonText: {
        fontSize: scale(22),
        fontWeight: 'bold',
        color: '#111',
    },
    headerTitle: {
        fontSize: scale(18),
        fontWeight: 'bold',
        color: '#111',
    },
    container: {
        flexGrow: 1,
        paddingHorizontal: scale(16),
        paddingBottom: verticalScale(40),
    },
    townScene: {
        backgroundColor: '#ECFCCB', // 라임 배경색
        borderRadius: scale(24),
        height: verticalScale(350),
        alignItems: 'center',
        justifyContent: 'flex-end',
        paddingBottom: verticalScale(40),
        marginBottom: verticalScale(20),
        position: 'relative',
        overflow: 'hidden',
    },
    townGreeting: {
        position: 'absolute',
        top: verticalScale(30),
        backgroundColor: 'rgba(255, 255, 255, 0.9)',
        paddingHorizontal: scale(16),
        paddingVertical: verticalScale(8),
        borderRadius: scale(20),
        fontSize: scale(14),
        fontWeight: 'bold',
        color: '#4D7C0F', // 진한 라임 텍스트
        overflow: 'hidden',
    },
    avatarWrapper: {
        zIndex: 2,
    },
    platform: {
        position: 'absolute',
        bottom: verticalScale(-40),
        width: scale(300),
        height: verticalScale(120),
        backgroundColor: '#D9F99D', // 라임 배경
        borderRadius: scale(150),
        transform: [{ scaleY: 0.5 }],
        zIndex: 1,
    },
    infoCard: {
        backgroundColor: '#FFFFFF',
        borderRadius: scale(20),
        padding: scale(20),
        marginBottom: verticalScale(20),
        shadowColor: '#000',
        shadowOffset: { width: 0, height: verticalScale(2) },
        shadowOpacity: 0.05,
        shadowRadius: scale(8),
        elevation: 2,
    },
    infoTitle: {
        fontSize: scale(16),
        fontWeight: 'bold',
        color: '#111',
        marginBottom: verticalScale(4),
    },
    infoSubtitle: {
        fontSize: scale(13),
        color: '#6B7280',
        marginBottom: verticalScale(16),
    },
    badgeRow: {
        flexDirection: 'row',
        gap: scale(16),
    },
    badgeItem: {
        alignItems: 'center',
    },
    badgeIconBox: {
        width: scale(56),
        height: scale(56),
        borderRadius: scale(28),
        backgroundColor: '#FEF3C7', // 노란색 배경
        justifyContent: 'center',
        alignItems: 'center',
        marginBottom: verticalScale(8),
    },
    badgeIcon: {
        fontSize: scale(28),
    },
    badgeName: {
        fontSize: scale(12),
        fontWeight: '600',
        color: '#4B5563',
    },
    actionRow: {
        flexDirection: 'row',
        gap: scale(12),
    },
    actionButton: {
        flex: 1,
        paddingVertical: verticalScale(14),
        borderRadius: scale(16),
        alignItems: 'center',
        justifyContent: 'center',
    },
    giftButton: {
        backgroundColor: '#111',
    },
    pokeButton: {
        backgroundColor: '#ECFCCB',
    },
    actionButtonText: {
        fontSize: scale(15),
        fontWeight: 'bold',
        color: '#FFFFFF',
    }
});

export default FriendTownScreen;
