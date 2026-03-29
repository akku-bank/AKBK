import React, { useState, useContext, useEffect, useCallback } from 'react';
import { View, StyleSheet, TouchableOpacity, SafeAreaView, Dimensions, Image, Modal, Platform, StatusBar, ImageBackground, Alert } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
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
    const { user, cachedLevel, setCachedLevel } = useAuthStore();
    const [homeData, setHomeData] = useState(null);
    const [realBalance, setRealBalance] = useState(null);

    useFocusEffect(
        useCallback(() => {
            const fetchHomeData = async () => {
                try {
                    const [res, accRes] = await Promise.all([
                        api.get('/home'),
                        api.get('/bank/accounts/me').catch(() => null)
                    ]);

                    const homeDataResult = res.data?.data;
                    if (!homeDataResult) return;

                    setHomeData(homeDataResult);

                    const currentLevel = homeDataResult.level || 1;
                    if (cachedLevel !== null && currentLevel > cachedLevel) {
                        setLevelUpModalVisible(true);
                        setCachedLevel(currentLevel);
                    } else if (cachedLevel === null) {
                        setCachedLevel(currentLevel);
                    }

                    if (accRes && accRes.data?.data?.accounts) {
                        const accounts = accRes.data.data.accounts;
                        if (accounts.length > 0) {
                            const primaryAcc = accounts.find(a => a.isPrimary) || accounts[0];
                            setRealBalance(primaryAcc.balance);
                        }
                    }

                    // 아바타 장착 상태 동기화
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
        }, [cachedLevel, setCachedLevel])
    );

    const avatarSize = height > 750 ? 270 : 200;

    return (
        <SafeAreaView style={styles.fullscreen}>
            <View style={styles.container}>

                {/* 상단 헤더: 잔액 및 QR */}
                <View style={styles.headerRow}>
                    <View style={styles.balanceWrapper}>
                        <CustomText style={styles.balanceLabel}>잔액</CustomText>
                        <CustomText style={styles.balanceAmount}>
                            {realBalance !== null ? realBalance.toLocaleString() : (homeData ? homeData.cashBalance.toLocaleString() : '0')}
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

                <ImageBackground
                    source={require('../../../assets/background2.jpg')}
                    style={styles.contentBackground}
                    imageStyle={{ left: -21, width: width + 30 }}
                    resizeMode="cover"
                >
                    {/* 좌측 알림 버튼 / 우측 플로팅 버튼 스택(내 도감, 꾸미기, 친구) */}
                    <View style={[styles.actionRow, { alignItems: 'flex-start' }]}>
                        <TouchableOpacity style={styles.squareIconButton} onPress={() => Alert.alert('알림', '아직 알림 기록이 없습니다.')}>
                            <Image source={require('../../../assets/icon/alert2.png')} style={{ width: scale(28), height: scale(28) }} resizeMode="contain" />
                            <CustomText style={styles.squareIconText}>알림</CustomText>
                            {homeData?.hasUnreadNotification && <View style={styles.redDot} />}
                        </TouchableOpacity>

                        <View style={{ position: 'absolute', right: scale(20), top: 0, gap: verticalScale(8), zIndex: 100 }}>
                            <TouchableOpacity style={styles.squareIconButton} onPress={() => navigation.navigate('AvatarDictionaryScreen')}>
                                <Image source={require('../../../assets/icon/dic.png')} style={{ width: scale(28), height: scale(28) }} resizeMode="contain" />
                                <CustomText style={styles.squareIconText}>도감</CustomText>
                            </TouchableOpacity>

                            <TouchableOpacity style={styles.squareIconButton} onPress={() => navigation.navigate('Wardrobe')}>
                                <Image source={require('../../../assets/icon/closet.png')} style={{ width: scale(28), height: scale(28) }} resizeMode="contain" />
                                <CustomText style={styles.squareIconText}>옷장</CustomText>
                            </TouchableOpacity>

                            <TouchableOpacity style={styles.squareIconButton} onPress={() => navigation.navigate('FriendList')}>
                                <Image source={require('../../../assets/icon/friend.png')} style={{ width: scale(28), height: scale(28) }} resizeMode="contain" />
                                <CustomText style={styles.squareIconText}>친구</CustomText>
                            </TouchableOpacity>
                        </View>
                    </View>

                    {/* ART_1 (벽그림 갤러리) - 구 기부처 카드 자리 */}
                    <View style={{ paddingHorizontal: scale(20), marginBottom: verticalScale(14), height: verticalScale(90), width: '100%', alignItems: 'flex-start', justifyContent: 'center' }}>
                        {equipState.art1 && equipState.art1 !== 'none' && (() => {
                            const artItem = AVATAR_ITEMS.art1.find(a => a.id === equipState.art1);
                            if (!artItem || !artItem.img) return null;
                            return (
                                <Image source={artItem.img} style={{ width: scale(115), height: verticalScale(115), marginTop: verticalScale(45) }} resizeMode="contain" />
                            );
                        })()}
                    </View>

                    {/* 아바타 영역 */}
                    <View style={styles.avatarSection}>
                        <View style={{ marginTop: verticalScale(-2), alignItems: 'center', zIndex: 10 }}>
                            <View style={{ flexDirection: 'row', alignItems: 'center', gap: scale(8), marginBottom: verticalScale(-2), transform: [{ translateY: verticalScale(5) }], zIndex: 10 }}>
                                <CustomText style={styles.levelText}>LV.{homeData ? homeData.level : 1}</CustomText>
                                <CustomText style={styles.nameText}>{user ? user.name : '김싸피'}</CustomText>
                            </View>

                            <View style={{ width: scale(200), height: verticalScale(40), justifyContent: 'center', marginBottom: verticalScale(0), transform: [{ translateY: verticalScale(-6) }] }}>
                                <Image source={require('../../../assets/gauge.png')} style={{ position: 'absolute', width: '100%', height: '100%' }} resizeMode="contain" />

                                <View style={{
                                    position: 'absolute',
                                    left: '6%',
                                    top: '37%',
                                    height: '25%',
                                    width: `${Math.min(100, Math.max(0, (homeData ? homeData.score : 0)))}%`,
                                    maxWidth: '90%',
                                    backgroundColor: '#7DF39D',
                                    zIndex: 1
                                }} />

                                <CustomText style={{ position: 'absolute', width: '100%', textAlign: 'center', fontSize: scale(14), fontWeight: '900', color: '#111', zIndex: 2 }}>
                                    {homeData ? homeData.score : 0} / 100
                                </CustomText>
                            </View>
                        </View>

                        <View style={styles.avatarWrapper}>
                            <ChildAvatar equipState={equipState} size={height > 750 ? 270 : 200} />

                            {equipState.pet && equipState.pet !== 'none' && (
                                <View style={{ position: 'absolute', right: scale(-150), bottom: verticalScale(-110), zIndex: 1 }}>
                                    <Pet petType={equipState.pet} size={scale(400)} />
                                </View>
                            )}

                            {equipState.art2 && equipState.art2 !== 'none' && (() => {
                                const treeItem = AVATAR_ITEMS.art2.find(a => a.id === equipState.art2);
                                if (!treeItem || !treeItem.img) return null;
                                return (
                                    <View style={{ position: 'absolute', left: scale(10), bottom: verticalScale(5), zIndex: 0 }}>
                                        <Image source={treeItem.img} style={{ width: scale(90), height: scale(130) }} resizeMode="contain" />
                                    </View>
                                );
                            })()}
                        </View>
                    </View>
                </ImageBackground>
            </View>

            {/* QR 결제 모달 */}
            <Modal visible={isQrModalVisible} transparent={true} animationType="fade">
                <View style={styles.qrModalBackground}>
                    <TouchableOpacity style={styles.qrModalCloseBtn} onPress={() => setQrModalVisible(false)}>
                        <CustomText style={styles.qrModalCloseText}>✕</CustomText>
                    </TouchableOpacity>
                    <Image source={require('../../../assets/qr-white.png')} style={styles.qrModalImage} />
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
    contentBackground: {
        flex: 1,
        width: '100%',
        paddingTop: verticalScale(16),
    },
    container: {
        flex: 1,
        paddingTop: verticalScale(16),
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
        backgroundColor: '#FFFFFF',
        borderRadius: scale(12),
        shadowColor: '#000',
        shadowOffset: { width: 0, height: verticalScale(2) },
        shadowOpacity: 0.05,
        shadowRadius: scale(8),
        elevation: 2,
    },
    qrImage: {
        width: '100%',
        height: '100%',
        resizeMode: 'cover',
    },
    divider: {
        height: 1,
        backgroundColor: '#F9FAFB',
        width: '100%',
    },
    actionRow: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        paddingHorizontal: scale(20),
        marginBottom: verticalScale(16),
    },
    squareIconButton: {
        width: scale(58),
        height: scale(58),
        backgroundColor: 'rgba(255, 255, 255, 0.95)',
        borderRadius: scale(18),
        justifyContent: 'center',
        alignItems: 'center',
        shadowColor: '#000',
        shadowOffset: { width: 0, height: verticalScale(2) },
        shadowOpacity: 0.05,
        shadowRadius: scale(8),
        elevation: 2,
    },
    squareIconText: {
        fontSize: scale(11),
        fontWeight: 'bold',
        color: '#4B5563',
        marginTop: verticalScale(2),
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
        shadowColor: '#000',
        shadowOffset: { width: 0, height: verticalScale(2) },
        shadowOpacity: 0.05,
        shadowRadius: scale(8),
        elevation: 2,
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
        zIndex: 10,
        elevation: 10,
    },
    avatarActionBtn: {
        backgroundColor: '#F9FAFB',
        paddingVertical: verticalScale(6),
        paddingHorizontal: scale(14),
        borderRadius: scale(16),
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 2 },
        shadowOpacity: 0.05,
        shadowRadius: 4,
        elevation: 2,
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
        paddingBottom: verticalScale(15),
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
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 2 },
        shadowOpacity: 0.05,
        shadowRadius: 4,
        elevation: 2,
    },
    levelUpCloseText: {
        fontSize: scale(16),
        fontWeight: 'bold',
        color: '#111',
    }
});

export default ChildHomeScreen;