import React, { useState, useEffect } from 'react';
import { View, StyleSheet, SafeAreaView, TouchableOpacity, ScrollView, Image, ImageBackground, Dimensions } from 'react-native';
import { scale, verticalScale } from 'react-native-size-matters';
import CustomText from '../../../components/common/CustomText';
import api from '../../../api/axios';

import { AVATAR_ITEMS } from '../../../components/child/avatar/AvatarAssets';

// 초기 빈 도감 상태
const INITIAL_INVENTORY = {
    '한벌옷': [],
    '모자': [],
    '상의': [],
    '하의': [],
    '신발': [],
    '등': [],
    '펫': []
};

const CATEGORIES = ['한벌옷', '모자', '상의', '하의', '신발', '등', '펫', '미술품', '나무'];

const AvatarDictionaryScreen = ({ navigation }) => {
    const [selectedCategory, setSelectedCategory] = useState(CATEGORIES[0]);

    const [userLevel, setUserLevel] = useState(1);
    const [inventoryItems, setInventoryItems] = useState(INITIAL_INVENTORY);

    useEffect(() => {
        const fetchInventory = async () => {
            try {
                // 상단 레벨 표시를 위해 Home 데이터 조회
                const homeRes = await api.get('/home');
                const level = homeRes.data?.data?.level || 1;
                setUserLevel(level);

                // 사용자가 보유한 아이템과 전체 도감 목록 조회
                const res = await api.get('/avatars/items');
                const items = res.data?.data?.items || [];

                const grouped = {
                    '한벌옷': [],
                    '모자': [],
                    '상의': [],
                    '하의': [],
                    '신발': [],
                    '등': [],
                    '장식품': [],
                    '펫': [],
                    '미술품': [],
                    '나무': []
                };

                // 백엔드에서 획득했다고 알려준 아이템 이름 맵핑
                // 백엔드에서 획득했다고 알려준 아이템들의 이름 목록 저장
                const ownedNames = items.filter(backendItem => backendItem.isOwned).map(i => i.name);

                // 카테고리별 공통 아이템 변환 헬퍼
                const createItemObj = (localItem, isForcedUnlock = false) => {
                    // 유연한 매핑: 백엔드에서 준 이름이 로컬 이름을 포함하거나, 역으로도 허용 (예: "시바견 펫" vs "시바견")
                    const isOwned = ownedNames.some(ownedName =>
                        ownedName.includes(localItem.name) || localItem.name.includes(ownedName)
                    );
                    // 레벨 1 기본템은 무조건 보유 처리 (백엔드에 없어도), 강제 해금(isForcedUnlock) 추가
                    const finalOwned = isOwned || localItem.level === 1 || isForcedUnlock;
                    // 가챠 등 특수(level 99)는 미보유시 잠김. 일반템은 유저레벨이 낮으면 잠김.
                    const isLocked = !finalOwned && (localItem.level > level || localItem.level === 99);

                    return {
                        id: localItem.id,
                        name: localItem.name,
                        requiredLevel: localItem.level,
                        image: localItem.img,
                        isOwned: finalOwned,
                        isLevelLocked: isLocked
                    };
                };

                // 프론트엔드 에셋(AVATAR_ITEMS)을 순회하며 빈 도감을 채움
                (AVATAR_ITEMS.outfit || []).forEach(i => { if (i.id !== 'none') grouped['한벌옷'].push(createItemObj(i)); });
                (AVATAR_ITEMS.hat || []).forEach(i => { if (i.id !== 'none') grouped['모자'].push(createItemObj(i)); });
                (AVATAR_ITEMS.upper || []).forEach(i => { if (i.id !== 'none') grouped['상의'].push(createItemObj(i)); });
                (AVATAR_ITEMS.lower || []).forEach(i => { if (i.id !== 'none') grouped['하의'].push(createItemObj(i)); });
                (AVATAR_ITEMS.shoe || []).forEach(i => { if (i.id !== 'none') grouped['신발'].push(createItemObj(i)); });
                (AVATAR_ITEMS.back || []).forEach(i => { if (i.id !== 'none') grouped['등'].push(createItemObj(i)); });
                (AVATAR_ITEMS.pet || []).forEach(i => { if (i.id !== 'none') grouped['펫'].push(createItemObj(i)); });
                (AVATAR_ITEMS.art1 || []).forEach(i => { if (i.id !== 'none') grouped['미술품'].push(createItemObj(i)); });
                (AVATAR_ITEMS.art2 || []).forEach(i => { if (i.id !== 'none') grouped['나무'].push(createItemObj(i)); });

                setInventoryItems(grouped);
            } catch (e) { console.error('Inventory Fetch Error:', e); }
        };
        fetchInventory();
    }, []);

    return (
        <SafeAreaView style={styles.safeArea}>
            <View style={styles.header}>
                <TouchableOpacity style={styles.backButton} onPress={() => navigation.goBack()}>
                    <CustomText style={styles.backButtonText}>←</CustomText>
                </TouchableOpacity>
                <CustomText style={styles.headerTitle}>아이템 도감</CustomText>
                <View style={styles.levelBox}>
                    <CustomText style={styles.levelText}>Lv.{userLevel}</CustomText>
                </View>
            </View>

            <View style={styles.categoryRowWrapper}>
                <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.categoryRow}>
                    {CATEGORIES.map(cat => (
                        <TouchableOpacity
                            key={cat}
                            style={[styles.categoryBtn, selectedCategory === cat && styles.categoryBtnActive]}
                            onPress={() => setSelectedCategory(cat)}
                        >
                            <CustomText style={[styles.categoryText, selectedCategory === cat && styles.categoryTextActive]}>{cat}</CustomText>
                        </TouchableOpacity>
                    ))}
                </ScrollView>
            </View>

            <ScrollView contentContainerStyle={styles.gridContainer} showsVerticalScrollIndicator={false}>
                <View style={styles.grid}>
                    {(inventoryItems[selectedCategory] || []).map(item => {
                        const isGachaOnly = item.requiredLevel > 5;
                        const isLocked = item.isLevelLocked;

                        return (
                            <ImageBackground key={item.id} source={require('../../../assets/box.png')} style={[styles.itemCard, isLocked && styles.lockedCard]} resizeMode="stretch">
                                <View style={styles.imageBox}>
                                    {Array.isArray(item.image) ? (
                                        <View style={{ width: item.id.includes('akku') ? '80%' : '95%', height: item.id.includes('akku') ? '80%' : '95%', position: 'relative' }}>
                                            {item.image.map((imgSrc, idx) => (
                                                <Image
                                                    key={idx}
                                                    source={imgSrc}
                                                    style={[{ width: '100%', height: '100%', position: 'absolute' }, selectedCategory === '펫' && { transform: [{ scale: 1.8 }] }, isLocked && styles.lockedImage]}
                                                    resizeMode="contain"
                                                />
                                            ))}
                                        </View>
                                    ) : (
                                        <Image
                                            source={item.image}
                                            style={[styles.itemImage, item.id.includes('akku') ? { width: '80%', height: '80%' } : { width: '95%', height: '95%' }, selectedCategory === '펫' && { transform: [{ scale: 1.8 }] }, isLocked && styles.lockedImage]}
                                            resizeMode="contain"
                                        />
                                    )}
                                    {isLocked && (
                                        <View style={styles.lockOverlay}>
                                            <CustomText style={styles.lockIcon}>🔒</CustomText>
                                        </View>
                                    )}
                                </View>
                                <CustomText style={[styles.itemName, isLocked && styles.lockedText]}>{item.name}</CustomText>

                                {(item.isOwned || !isLocked) ? (
                                    <View style={[styles.statusButton, styles.ownedButton]}>
                                        <CustomText style={styles.ownedButtonText}>획득 완료</CustomText>
                                    </View>
                                ) : (
                                    <View style={[styles.statusButton, styles.lockedButton]}>
                                        <CustomText style={styles.lockedButtonText}>잠김</CustomText>
                                    </View>
                                )}
                            </ImageBackground>
                        );
                    })}
                </View>
            </ScrollView>
        </SafeAreaView>
    );
};

const { width } = Dimensions.get('window');

const styles = StyleSheet.create({
    safeArea: { flex: 1, backgroundColor: '#ECFCCB' },
    header: {
        flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between',
        paddingHorizontal: scale(16), paddingVertical: verticalScale(16), backgroundColor: '#FFFFFF'
    },
    backButton: { width: scale(32), height: scale(32), justifyContent: 'center' },
    backButtonText: { fontSize: scale(22), fontWeight: 'bold', color: '#111' },
    headerTitle: { fontSize: scale(18), fontWeight: 'bold', color: '#111' },
    levelBox: { backgroundColor: '#ECFCCB', paddingHorizontal: scale(12), paddingVertical: verticalScale(6), borderRadius: scale(12), shadowColor: '#000', shadowOffset: { width: 0, height: 2 }, shadowOpacity: 0.05, shadowRadius: 4, elevation: 1, },
    levelText: { fontSize: scale(14), fontWeight: 'bold', color: '#4D7C0F' },

    categoryRowWrapper: { backgroundColor: '#FFFFFF', borderBottomWidth: 1, borderBottomColor: '#E5E7EB' },
    categoryRow: { flexDirection: 'row', paddingHorizontal: scale(16) },
    categoryBtn: { paddingHorizontal: scale(16), paddingVertical: verticalScale(14), marginRight: scale(8), borderBottomWidth: 2, borderBottomColor: 'transparent' },
    categoryBtnActive: { borderBottomColor: '#A3E635' },
    categoryText: { fontSize: scale(15), fontWeight: 'bold', color: '#9CA3AF' },
    categoryTextActive: { color: '#111' },

    gridContainer: { paddingHorizontal: scale(16), paddingTop: verticalScale(16), paddingBottom: verticalScale(40) },
    grid: { flexDirection: 'row', flexWrap: 'wrap', justifyContent: 'space-between' },
    itemCard: {
        width: (width - scale(32)) * 0.48,
        aspectRatio: 1670 / 2187,
        backgroundColor: 'transparent',
        paddingLeft: scale(12),
        paddingRight: scale(12),
        paddingTop: scale(20),
        paddingBottom: scale(20),
        marginBottom: verticalScale(16),
        alignItems: 'center',
        justifyContent: 'flex-start',
    },
    lockedCard: { opacity: 0.6 },
    imageBox: { width: '100%', flex: 1, backgroundColor: 'transparent', justifyContent: 'center', alignItems: 'center', position: 'relative' },
    itemImage: { width: '80%', height: '80%' },
    lockedImage: { opacity: 0.3 },
    lockOverlay: { position: 'absolute', justifyContent: 'center', alignItems: 'center' },
    lockIcon: { fontSize: scale(24) },
    itemName: { fontSize: scale(14), fontWeight: 'bold', color: '#111', marginBottom: verticalScale(8), textAlign: 'center' },
    lockedText: { color: '#9CA3AF' },
    statusButton: { paddingVertical: verticalScale(4), alignItems: 'center' },
    ownedButton: { backgroundColor: 'transparent' },
    ownedButtonText: { fontSize: scale(14), fontWeight: 'bold', color: '#4B5563' },
    gachaButton: { backgroundColor: 'transparent' },
    gachaButtonText: { fontSize: scale(14), fontWeight: 'bold', color: '#D97706' },
    lockedButton: { backgroundColor: 'transparent' },
    lockedButtonText: { fontSize: scale(14), fontWeight: 'bold', color: '#9CA3AF' }
});

export default AvatarDictionaryScreen;
