import React, { useState, useContext, useEffect } from 'react';
import { View, StyleSheet, TouchableOpacity, SafeAreaView, Dimensions, Image, Modal, Platform, StatusBar } from 'react-native';
import { scale, verticalScale } from 'react-native-size-matters';
import ChildAvatar from '../../../components/child/avatar/ChildAvatar';
import { AvatarContext } from '../../../components/child/avatar/AvatarContext';
import { AVATAR_ITEMS } from '../../../components/child/avatar/AvatarAssets';
import CustomText from '../../../components/common/CustomText';
import Pet from '../../../components/child/avatar/Pet';
import api from '../../../api/axios';
import useAuthStore from '../../../store/useAuthStore';

const { width, height } = Dimensions.get('window');

const ChildHomeScreen = ({ navigation }) => {
    const [isQrModalVisible, setQrModalVisible] = useState(false);
    const [isLevelUpModalVisible, setLevelUpModalVisible] = useState(false);
    const { equipState, setEquipState } = useContext(AvatarContext);
    const { user } = useAuthStore(); // get name from store since it's not in response
    const [homeData, setHomeData] = useState(null);

    useEffect(() => {
        const fetchHomeData = async () => {
            try {
                const res = await api.get('/home');
                const homeDataResult = res.data?.data;
                if (!homeDataResult) return;

                setHomeData(homeDataResult);
                if (homeDataResult.hasLevelChanged) {
                    setLevelUpModalVisible(true);
                }

                // 백엔드 아바타 장착 상태 동기화
                if (homeDataResult.avatar && homeDataResult.avatar.equippedItems) {
                    try {
                        const dictRes = await api.get('/avatars/items');
                        const backendItems = dictRes.data?.data?.items || [];
                        const equippedDTOs = homeDataResult.avatar.equippedItems;

                        let newEquip = null;

                        equippedDTOs.forEach(eq => {
                            const dictItem = backendItems.find(i => i.itemId === eq.itemId);
                            if (!dictItem) return;

                            let frontendCat = null;
                            if (eq.category === 'HAT') frontendCat = 'hat';
                            else if (eq.category === 'TOP') frontendCat = 'upper';
                            else if (eq.category === 'BOTTOM') frontendCat = 'lower';

                            if (frontendCat) {
                                const assetItem = AVATAR_ITEMS[frontendCat]?.find(a => a.name === dictItem.name);
                                if (assetItem) {
                                    if (!newEquip) newEquip = { ...equipState };
                                    newEquip[frontendCat] = assetItem.id;
                                }
                            }
                        });

                        if (newEquip) {
                            setEquipState(newEquip);
                        }
                    } catch (dictErr) { console.error('Dictionary Fetch Error on Home', dictErr); }
                }

            } catch (e) {
                console.error('Child Home Fetch Error', e);
            }
        };
        fetchHomeData();
    }, []);

    const avatarSize = height > 750 ? 270 : 200;

    return (
        <SafeAreaView style={styles.fullscreen}>
            <View style={styles.container}>

                {/* 상단 헤더: 잔액 및 QR */}
                <View style={styles.headerRow}>
                    <View style={styles.balanceWrapper}>
                        <CustomText style={styles.balanceLabel}>잔액</CustomText>
                        <CustomText style={styles.balanceAmount}>
                            {homeData ? homeData.cashBalance.toLocaleString() : '0'}
                        </CustomText>
                        <CustomText style={styles.balanceCurrency}>원</CustomText>
                    </View>
                    <View style={{ flexDirection: 'row', gap: 12, alignItems: 'center' }}>
                        <TouchableOpacity style={styles.qrButton} onPress={() => setQrModalVisible(true)}>
                            <Image source={require('../../../assets/qr.png')} style={styles.qrImage} />
                        </TouchableOpacity>
                    </View>
                </View>

                <View style={styles.divider} />

                {/* 친구, 알림 버튼 */}
                <View style={styles.actionRow}>
                    <TouchableOpacity style={styles.pillButton} onPress={() => navigation.navigate('FriendList')}>
                        <CustomText style={styles.pillButtonText}>친구</CustomText>
                    </TouchableOpacity>
                    <TouchableOpacity style={styles.pillButton}>
                        <CustomText style={styles.pillButtonText}>알림</CustomText>
                        {homeData?.hasUnreadNotification && <View style={styles.redDot} />}
                    </TouchableOpacity>
                </View>

                {/* 내가 기부한 장소 카드 */}
                <TouchableOpacity style={styles.donationCard} activeOpacity={0.9} onPress={() => navigation.navigate('BadgeMap')}>
                    <View style={styles.donationBadge}>
                        <CustomText style={styles.donationBadgeText}>내가 기부한 장소</CustomText>
                    </View>
                    <CustomText style={styles.donationContentText}>준비 중</CustomText>
                </TouchableOpacity>

                {/* 아바타 영역 */}
                <View style={styles.avatarSection}>
                    <CustomText style={styles.levelText}>LV.{homeData ? homeData.level : 1} | 소비점수 {homeData ? homeData.score : 0}점</CustomText>
                    <CustomText style={styles.nameText}>{user ? user.name : '김싸피'}</CustomText>

                    <View style={styles.avatarActionRow}>
                        <TouchableOpacity style={styles.avatarActionBtn} onPress={() => navigation.navigate('AvatarDictionaryScreen')}>
                            <CustomText style={styles.avatarActionText}>내 도감</CustomText>
                        </TouchableOpacity>
                        <TouchableOpacity style={styles.avatarActionBtn} onPress={() => navigation.navigate('Wardrobe')}>
                            <CustomText style={styles.avatarActionText}>꾸미기</CustomText>
                        </TouchableOpacity>
                    </View>

                    <View style={styles.avatarWrapper}>
                        <ChildAvatar equipState={equipState} size={avatarSize} />

                        {/* 펫 배치 (백엔드 펫 데이터가 있을 때만 렌더링되도록 사전 준비) */}
                        {homeData?.pet && (
                            <View style={{ position: 'absolute', right: scale(-120), bottom: verticalScale(-115) }}>
                                <Pet petType={homeData.pet.type || 'shiba'} size={scale(350)} />
                            </View>
                        )}
                    </View>
                </View>

            </View>

            {/* QR 결제 모달 */}
            <Modal visible={isQrModalVisible} transparent={true} animationType="fade">
                <View style={styles.qrModalBackground}>
                    <TouchableOpacity style={styles.qrModalCloseBtn} onPress={() => setQrModalVisible(false)}>
                        <CustomText style={styles.qrModalCloseText}>✕</CustomText>
                    </TouchableOpacity>
                    <Image source={require('../../../assets/qr.png')} style={styles.qrModalImage} />
                </View>
            </Modal>

            {/* 레벨업 축하 모달 */}
            <Modal visible={isLevelUpModalVisible} transparent={true} animationType="fade">
                <View style={styles.levelUpModalBackground}>
                    <View style={styles.levelUpModalCard}>
                        <CustomText style={styles.levelUpEmoji}>🎉</CustomText>
                        <CustomText style={styles.levelUpTitle}>축하해요!</CustomText>
                        <CustomText style={styles.levelUpDesc}>레벨이 올랐어요.{'\n'}새로운 아이템이 해금되었습니다!</CustomText>
                        <TouchableOpacity style={styles.levelUpCloseBtn} onPress={() => setLevelUpModalVisible(false)}>
                            <CustomText style={styles.levelUpCloseText}>확인</CustomText>
                        </TouchableOpacity>
                    </View>
                </View>
            </Modal>
        </SafeAreaView>
    );
};

const styles = StyleSheet.create({
    fullscreen: {
        flex: 1,
        backgroundColor: '#FFFFFF',
    },
    container: {
        flex: 1,
        paddingTop: verticalScale(16),
        paddingBottom: verticalScale(10),
    },
    headerRow: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'center',
        paddingHorizontal: scale(20),
        marginBottom: verticalScale(16),
    },
    balanceWrapper: {
        flexDirection: 'row',
        alignItems: 'baseline',
    },
    balanceLabel: {
        fontSize: scale(16),
        fontWeight: '900',
        color: '#2A303C',
        marginRight: scale(6),
    },
    balanceAmount: {
        fontSize: scale(32),
        fontWeight: '900',
        color: '#111',
        letterSpacing: -1,
    },
    balanceCurrency: {
        fontSize: scale(18),
        fontWeight: '900',
        color: '#111',
        marginLeft: scale(4),
    },
    qrButton: {
        width: scale(40),
        height: scale(40),
        justifyContent: 'center',
        alignItems: 'center',
        backgroundColor: '#F3F4F6',
        borderRadius: scale(8),
        overflow: 'hidden',
    },
    qrImage: {
        width: '100%',
        height: '100%',
        resizeMode: 'cover',
    },
    divider: {
        height: 1,
        backgroundColor: '#F3F4F6',
        width: '100%',
        marginBottom: verticalScale(20),
    },
    actionRow: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        paddingHorizontal: scale(20),
        marginBottom: verticalScale(20),
    },
    pillButton: {
        backgroundColor: '#F3F4F6',
        paddingVertical: verticalScale(8),
        paddingHorizontal: scale(24),
        borderRadius: scale(20),
    },
    pillButtonText: {
        fontSize: scale(15),
        fontWeight: '900',
        color: '#374151',
    },
    redDot: {
        position: 'absolute',
        top: scale(4),
        right: scale(8),
        width: scale(6),
        height: scale(6),
        borderRadius: scale(3),
        backgroundColor: '#EF4444',
    },
    donationCard: {
        backgroundColor: '#E5E7EB',
        marginHorizontal: scale(20),
        borderRadius: scale(20),
        padding: scale(16),
        height: verticalScale(90),
        marginBottom: verticalScale(20),
        position: 'relative',
        justifyContent: 'center',
        alignItems: 'center',
    },
    donationBadge: {
        position: 'absolute',
        top: scale(16),
        left: scale(16),
        backgroundColor: '#FFFFFF',
        paddingVertical: verticalScale(6),
        paddingHorizontal: scale(12),
        borderRadius: scale(16),
    },
    donationBadgeText: {
        fontSize: scale(13),
        fontWeight: '900',
        color: '#111',
    },
    donationContentText: {
        fontSize: scale(14),
        fontWeight: '700',
        color: '#6B7280',
        marginTop: verticalScale(10),
    },
    avatarSection: {
        flex: 1,
        alignItems: 'center',
        justifyContent: 'flex-start',
    },
    levelText: {
        fontSize: scale(13),
        fontWeight: '900',
        color: '#6B7280',
        marginBottom: verticalScale(2),
    },
    nameText: {
        fontSize: scale(22),
        fontWeight: '900',
        color: '#111',
        marginBottom: verticalScale(6),
    },
    avatarActionRow: {
        flexDirection: 'row',
        gap: scale(10),
        marginBottom: verticalScale(10),
    },
    avatarActionBtn: {
        backgroundColor: '#F3F4F6',
        paddingVertical: verticalScale(6),
        paddingHorizontal: scale(14),
        borderRadius: scale(16),
    },
    avatarActionText: {
        fontSize: scale(13),
        fontWeight: '900',
        color: '#374151',
    },
    avatarWrapper: {
        flex: 1,
        width: '100%',
        alignItems: 'center',
        justifyContent: 'flex-end',
    },
    qrModalBackground: {
        flex: 1,
        backgroundColor: 'rgba(0, 0, 0, 0.9)',
        justifyContent: 'center',
        alignItems: 'center',
    },
    qrModalCloseBtn: {
        position: 'absolute',
        top: verticalScale(50),
        right: scale(20),
        padding: scale(10),
        zIndex: 100,
    },
    qrModalCloseText: {
        fontSize: scale(30),
        color: '#FFFFFF',
        fontWeight: 'bold',
    },
    qrModalImage: {
        width: '80%',
        height: '80%',
        resizeMode: 'contain',
    },
    levelUpModalBackground: {
        flex: 1,
        backgroundColor: 'rgba(0, 0, 0, 0.6)',
        justifyContent: 'center',
        alignItems: 'center',
        paddingHorizontal: scale(20),
    },
    levelUpModalCard: {
        backgroundColor: '#FFFFFF',
        borderRadius: scale(24),
        padding: scale(24),
        alignItems: 'center',
        width: '100%',
        shadowColor: '#000',
        shadowOffset: { width: 0, height: verticalScale(4) },
        shadowOpacity: 0.1,
        shadowRadius: scale(12),
        elevation: 5,
    },
    levelUpEmoji: {
        fontSize: scale(48),
        marginBottom: verticalScale(16),
    },
    levelUpTitle: {
        fontSize: scale(22),
        fontWeight: '900',
        color: '#111',
        marginBottom: verticalScale(8),
    },
    levelUpDesc: {
        fontSize: scale(14),
        color: '#4B5563',
        textAlign: 'center',
        lineHeight: 22,
        marginBottom: verticalScale(24),
    },
    levelUpCloseBtn: {
        backgroundColor: '#A3E635',
        paddingVertical: verticalScale(14),
        paddingHorizontal: scale(32),
        borderRadius: scale(16),
        width: '100%',
        alignItems: 'center',
    },
    levelUpCloseText: {
        fontSize: scale(16),
        fontWeight: 'bold',
        color: '#111',
    }
});

export default ChildHomeScreen;