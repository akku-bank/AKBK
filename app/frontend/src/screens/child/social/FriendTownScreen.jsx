import React, { useEffect, useState } from 'react';
import { View, StyleSheet, TouchableOpacity, SafeAreaView, ScrollView, Alert, ActivityIndicator } from 'react-native';
import { scale, verticalScale } from 'react-native-size-matters';
import ChildAvatar from '../../../components/child/avatar/ChildAvatar';
import CustomText from '../../../components/common/CustomText';
import api from '../../../api/axios';

const DEFAULT_EQUIP_STATE = {
    hair: 'hair_boy',
    face: 'base_boy',
    upper: 'upper_base',
    lower: 'lower_base',
    shoe: 'none',
    hat: 'none',
    wing: 'none'
};

const ASSET_KEY_BY_FILE = {
    'hair2.png': { category: 'hair', value: 'hair2' },
    'hair_boy.png': { category: 'hair', value: 'hair_boy' },
    'hair_girl.png': { category: 'hair', value: 'hair_girl' },
    'upper_1.png': { category: 'upper', value: 'upper_1' },
    'upper_base.png': { category: 'upper', value: 'upper_base' },
    'lower_1.png': { category: 'lower', value: 'lower_1' },
    'lower_base.png': { category: 'lower', value: 'lower_base' },
    'hat.png': { category: 'hat', value: 'hat' },
    'shoe.png': { category: 'shoe', value: 'shoe' },
    'wing.png': { category: 'wing', value: 'wing' },
};

const mapAvatarUrlsToEquipState = (equippedItems = []) => {
    const nextState = { ...DEFAULT_EQUIP_STATE };

    equippedItems.forEach(url => {
        const fileName = url?.split('/').pop();
        const mapped = ASSET_KEY_BY_FILE[fileName];
        if (mapped) {
            nextState[mapped.category] = mapped.value;
        }
    });

    return nextState;
};

const FriendTownScreen = ({ route, navigation }) => {
    const friendId = route?.params?.friendId;
    const fallbackFriendName = route?.params?.friendName || '친구';

    const [isLoading, setIsLoading] = useState(false);
    const [friendTownInfo, setFriendTownInfo] = useState(null);
    const [equipState, setEquipState] = useState(DEFAULT_EQUIP_STATE);

    useEffect(() => {
        const fetchFriendTown = async () => {
            if (!friendId) return;

            try {
                setIsLoading(true);
                const res = await api.get(`/social/town/${friendId}`);
                const data = res?.data?.data;

                setFriendTownInfo(data);
                setEquipState(mapAvatarUrlsToEquipState(data?.avatar?.equippedItems || []));
            } catch (e) {
                console.error('Friend Town Fetch Error', e);
                Alert.alert('오류', e.response?.data?.message || '친구 타운 정보를 불러오지 못했습니다.');
            } finally {
                setIsLoading(false);
            }
        };

        fetchFriendTown();
    }, [friendId]);

    const friendName = friendTownInfo?.friendName || fallbackFriendName;
    const recentCharity = friendTownInfo?.recentCharity;

    const handleDeleteFriend = () => {
        Alert.alert(
            '안내',
            `${friendName}을(를) 삭제할까요?`,
            [
                { text: '취소', style: 'cancel' },
                {
                    text: '삭제하기',
                    style: 'destructive',
                    onPress: async () => {
                        try {
                            await api.delete(`/social/friends/${friendId}`);
                            Alert.alert('완료', '친구가 삭제되었습니다.', [
                                {
                                    text: '확인',
                                    onPress: () => navigation.goBack(),
                                },
                            ]);
                        } catch (e) {
                            console.error('Friend Delete Error', e);
                            Alert.alert('오류', e.response?.data?.message || '친구 삭제에 실패했습니다.');
                        }
                    },
                },
            ]
        );
    };

    return (
        <SafeAreaView style={styles.safeArea}>
            <View style={styles.header}>
                <TouchableOpacity style={styles.backButton} onPress={() => navigation.goBack()}>
                    <CustomText style={styles.backButtonText}>←</CustomText>
                </TouchableOpacity>
                <CustomText style={styles.headerTitle}>{friendName}의 타운</CustomText>
                <TouchableOpacity style={styles.deleteButton} onPress={handleDeleteFriend}>
                    <CustomText style={styles.deleteButtonText}>삭제</CustomText>
                </TouchableOpacity>
            </View>

            <ScrollView contentContainerStyle={styles.container} showsVerticalScrollIndicator={false}>
                <View style={styles.townScene}>
                    <CustomText style={styles.townGreeting}>"안녕! 내 타운에 온 걸 환영해!"</CustomText>
                    {isLoading ? (
                        <View style={styles.loadingBox}>
                            <ActivityIndicator color="#4D7C0F" />
                        </View>
                    ) : (
                        <ChildAvatar size={200} equipState={equipState} />
                    )}
                    <View style={styles.platform} />
                </View>

                <View style={styles.infoCard}>
                    <CustomText style={styles.infoTitle}>최근 기부 활동</CustomText>
                    <CustomText style={styles.infoSubtitle}>
                        {recentCharity
                            ? `${friendName}의 최근 관심 기부처예요.`
                            : `${friendName}의 최근 기부 정보가 아직 없어요.`}
                    </CustomText>

                    <View style={styles.recentCard}>
                        <CustomText style={styles.recentLabel}>최근 기부처</CustomText>
                        <CustomText style={styles.recentValue}>
                            {recentCharity || '표시할 기부 정보가 없어요'}
                        </CustomText>
                    </View>
                </View>

                <View style={styles.actionRow}>
                    <TouchableOpacity
                        style={[styles.actionButton, styles.giftButton]}
                        activeOpacity={0.8}
                        onPress={() => Alert.alert('안내', '선물 보내기 API는 아직 연결되지 않았습니다.')}
                    >
                        <CustomText style={styles.actionButtonText}>선물 보내기 🎁</CustomText>
                    </TouchableOpacity>
                    <TouchableOpacity
                        style={[styles.actionButton, styles.pokeButton]}
                        activeOpacity={0.8}
                        onPress={() => Alert.alert('안내', '콕 찌르기 API는 아직 연결되지 않았습니다.')}
                    >
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
    deleteButton: {
        backgroundColor: '#FEE2E2',
        paddingHorizontal: scale(12),
        paddingVertical: verticalScale(6),
        borderRadius: scale(12),
        alignItems: 'center',
        justifyContent: 'center',
        minWidth: scale(56),
    },
    deleteButtonText: {
        fontSize: scale(13),
        fontWeight: 'bold',
        color: '#DC2626',
    },
    container: {
        flexGrow: 1,
        paddingHorizontal: scale(16),
        paddingBottom: verticalScale(40),
    },
    townScene: {
        backgroundColor: '#ECFCCB',
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
        color: '#4D7C0F',
        overflow: 'hidden',
    },
    loadingBox: {
        width: scale(200),
        height: scale(200),
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 2,
    },
    platform: {
        position: 'absolute',
        bottom: verticalScale(-40),
        width: scale(300),
        height: verticalScale(120),
        backgroundColor: '#D9F99D',
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
    recentCard: {
        backgroundColor: '#F8FAFC',
        borderRadius: scale(16),
        paddingHorizontal: scale(16),
        paddingVertical: verticalScale(14),
    },
    recentLabel: {
        fontSize: scale(12),
        color: '#6B7280',
        marginBottom: verticalScale(6),
    },
    recentValue: {
        fontSize: scale(15),
        fontWeight: 'bold',
        color: '#111',
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
