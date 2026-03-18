import React, { useState } from 'react';
import { View, StyleSheet, SafeAreaView, TouchableOpacity, ScrollView, Image } from 'react-native';
import { scale, verticalScale } from 'react-native-size-matters';
import CustomText from '../../../components/common/CustomText';

// 더미 데이터 (유저 소비 레벨)
const MOCK_USER_LEVEL = 3;

const INVENTORY_ITEMS = {
    '모자': [
        { id: 'h1', name: '빨간 캡모자', requiredLevel: 1, image: require('../../../assets/avatar/acc/hat.png'), isOwned: true },
        { id: 'h2', name: '노란 캡모자', requiredLevel: 4, image: require('../../../assets/avatar/acc/hat.png'), isOwned: false },
        { id: 'h3', name: '파란 캡모자', requiredLevel: 6, image: require('../../../assets/avatar/acc/hat.png'), isOwned: false },
    ],
    '상의': [
        { id: 't1', name: '기본 티셔츠', requiredLevel: 1, image: require('../../../assets/avatar/upper/upper_1.png'), isOwned: true },
        { id: 't2', name: '줄무늬 티셔츠', requiredLevel: 3, image: require('../../../assets/avatar/upper/upper_1.png'), isOwned: true },
        { id: 't3', name: '빨간 티셔츠', requiredLevel: 6, image: require('../../../assets/avatar/upper/upper_1.png'), isOwned: false },
    ],
    '하의': [
        { id: 'b1', name: '청바지', requiredLevel: 2, image: require('../../../assets/avatar/lower/lower_1.png'), isOwned: true },
        { id: 'b2', name: '반바지', requiredLevel: 5, image: require('../../../assets/avatar/lower/lower_1.png'), isOwned: false },
    ]
};

const CATEGORIES = ['모자', '상의', '하의'];

const ItemShopScreen = ({ navigation }) => {
    const [selectedCategory, setSelectedCategory] = useState(CATEGORIES[0]);

    return (
        <SafeAreaView style={styles.safeArea}>
            <View style={styles.header}>
                <TouchableOpacity style={styles.backButton} onPress={() => navigation.goBack()}>
                    <CustomText style={styles.backButtonText}>←</CustomText>
                </TouchableOpacity>
                <CustomText style={styles.headerTitle}>아이템 도감</CustomText>
                <View style={styles.levelBox}>
                    <CustomText style={styles.levelText}>Lv.{MOCK_USER_LEVEL}</CustomText>
                </View>
            </View>

            <View style={styles.categoryRow}>
                {CATEGORIES.map(cat => (
                    <TouchableOpacity
                        key={cat}
                        style={[styles.categoryBtn, selectedCategory === cat && styles.categoryBtnActive]}
                        onPress={() => setSelectedCategory(cat)}
                    >
                        <CustomText style={[styles.categoryText, selectedCategory === cat && styles.categoryTextActive]}>{cat}</CustomText>
                    </TouchableOpacity>
                ))}
            </View>

            <ScrollView contentContainerStyle={styles.gridContainer} showsVerticalScrollIndicator={false}>
                <View style={styles.grid}>
                    {INVENTORY_ITEMS[selectedCategory].map(item => {
                        const isGachaOnly = item.requiredLevel > 5;
                        const isLocked = !item.isOwned && item.requiredLevel > MOCK_USER_LEVEL;

                        return (
                            <View key={item.id} style={[styles.itemCard, isLocked && styles.lockedCard]}>
                                <View style={styles.imageBox}>
                                    <Image
                                        source={item.image}
                                        style={[styles.itemImage, isLocked && styles.lockedImage]}
                                        resizeMode="contain"
                                    />
                                    {isLocked && (
                                        <View style={styles.lockOverlay}>
                                            <CustomText style={styles.lockIcon}>🔒</CustomText>
                                        </View>
                                    )}
                                </View>
                                <CustomText style={[styles.itemName, isLocked && styles.lockedText]}>{item.name}</CustomText>

                                {item.isOwned ? (
                                    <View style={[styles.statusButton, styles.ownedButton]}>
                                        <CustomText style={styles.ownedButtonText}>획득 완료</CustomText>
                                    </View>
                                ) : isGachaOnly ? (
                                    <View style={[styles.statusButton, styles.gachaButton]}>
                                        <CustomText style={styles.gachaButtonText}>기부 보상 전용</CustomText>
                                    </View>
                                ) : (
                                    <View style={[styles.statusButton, styles.lockedButton]}>
                                        <CustomText style={styles.lockedButtonText}>Lv.{item.requiredLevel} 해금</CustomText>
                                    </View>
                                )}
                            </View>
                        );
                    })}
                </View>
            </ScrollView>
        </SafeAreaView>
    );
};

const styles = StyleSheet.create({
    safeArea: { flex: 1, backgroundColor: '#F3F4F6' },
    header: {
        flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between',
        paddingHorizontal: scale(16), paddingVertical: verticalScale(16), backgroundColor: '#FFFFFF'
    },
    backButton: { width: scale(32), height: scale(32), justifyContent: 'center' },
    backButtonText: { fontSize: scale(22), fontWeight: 'bold', color: '#111' },
    headerTitle: { fontSize: scale(18), fontWeight: 'bold', color: '#111' },
    levelBox: { backgroundColor: '#F3E8FF', paddingHorizontal: scale(12), paddingVertical: verticalScale(6), borderRadius: scale(12) },
    levelText: { fontSize: scale(14), fontWeight: 'bold', color: '#7E22CE' },

    categoryRow: { flexDirection: 'row', paddingHorizontal: scale(16), paddingVertical: verticalScale(12), backgroundColor: '#FFFFFF', borderBottomWidth: 1, borderBottomColor: '#F3F4F6' },
    categoryBtn: { paddingHorizontal: scale(16), paddingVertical: verticalScale(8), marginRight: scale(8), borderRadius: scale(20), backgroundColor: '#F9FAFB' },
    categoryBtnActive: { backgroundColor: '#A3E635' },
    categoryText: { fontSize: scale(14), fontWeight: 'bold', color: '#6B7280' },
    categoryTextActive: { color: '#111' },

    gridContainer: { paddingHorizontal: scale(16), paddingTop: verticalScale(16), paddingBottom: verticalScale(40) },
    grid: { flexDirection: 'row', flexWrap: 'wrap', justifyContent: 'space-between' },
    itemCard: {
        width: '48%', backgroundColor: '#FFFFFF', borderRadius: scale(16), padding: scale(12), marginBottom: verticalScale(16),
        shadowColor: '#000', shadowOffset: { width: 0, height: verticalScale(2) }, shadowOpacity: 0.05, shadowRadius: scale(4), elevation: 2
    },
    lockedCard: { backgroundColor: '#F9FAFB', elevation: 0 },
    imageBox: { height: scale(100), backgroundColor: '#F9FAFB', borderRadius: scale(12), justifyContent: 'center', alignItems: 'center', marginBottom: verticalScale(8), position: 'relative' },
    itemImage: { width: '80%', height: '80%' },
    lockedImage: { opacity: 0.3 },
    lockOverlay: { position: 'absolute', justifyContent: 'center', alignItems: 'center' },
    lockIcon: { fontSize: scale(24) },
    itemName: { fontSize: scale(14), fontWeight: 'bold', color: '#111', marginBottom: verticalScale(8), textAlign: 'center' },
    lockedText: { color: '#9CA3AF' },
    statusButton: { paddingVertical: verticalScale(8), borderRadius: scale(8), alignItems: 'center' },
    ownedButton: { backgroundColor: '#F3F4F6' },
    ownedButtonText: { fontSize: scale(14), fontWeight: 'bold', color: '#4B5563' },
    gachaButton: { backgroundColor: '#FEF3C7' },
    gachaButtonText: { fontSize: scale(14), fontWeight: 'bold', color: '#D97706' },
    lockedButton: { backgroundColor: '#F3F4F6' },
    lockedButtonText: { fontSize: scale(14), fontWeight: 'bold', color: '#9CA3AF' }
});

export default ItemShopScreen;
