import React, { useContext, useState } from 'react';
import { View, Text, StyleSheet, TouchableOpacity, ScrollView, Image, Dimensions } from 'react-native';
import { AvatarContext } from '../../../components/child/avatar/AvatarContext';
import { AVATAR_ITEMS } from '../../../components/child/avatar/AvatarAssets';
import ChildAvatar from '../../../components/child/avatar/ChildAvatar';

const CATEGORIES = [
    { id: 'gender', label: '성별' },
    { id: 'face', label: '얼굴' },
    { id: 'hair', label: '헤어' },
    { id: 'upper', label: '상의' },
    { id: 'lower', label: '하의' },
    { id: 'hat', label: '모자' },
    { id: 'shoe', label: '신발' },
    { id: 'wing', label: '날개' },
];

const { width } = Dimensions.get('window');

const AvatarCustomScreen = ({ navigation }) => {
    const { equipState, updateEquip, setEquipState } = useContext(AvatarContext);
    const [selectedCategory, setSelectedCategory] = useState('hair');

    const handleGenderChange = (gender) => {
        setEquipState(prev => ({
            ...prev,
            gender: gender,
            face: `base_${gender}`,
            hair: `hair_${gender}`,
        }));
    };

    const renderGenderTab = () => (
        <View style={styles.genderContainer}>
            <TouchableOpacity
                style={[styles.genderButton, equipState.gender === 'boy' && styles.selectedGender]}
                onPress={() => handleGenderChange('boy')}
            >
                <Text style={styles.genderText}>남자</Text>
            </TouchableOpacity>
            <TouchableOpacity
                style={[styles.genderButton, equipState.gender === 'girl' && styles.selectedGender]}
                onPress={() => handleGenderChange('girl')}
            >
                <Text style={styles.genderText}>여자</Text>
            </TouchableOpacity>
        </View>
    );

    const renderItemGrid = () => {
        if (selectedCategory === 'gender') {
            return renderGenderTab();
        }

        let items = AVATAR_ITEMS[selectedCategory] || [];

        // 성별 따라서 얼굴/헤어 필터링
        if (selectedCategory === 'face' || selectedCategory === 'hair') {
            const currentGenderStr = equipState.gender;
            const oppositeGenderStr = currentGenderStr === 'boy' ? 'girl' : 'boy';

            items = items.filter(item => {
                if (item.id.includes(oppositeGenderStr)) {
                    return false;
                }
                return true;
            });
        }
        return (
            <ScrollView contentContainerStyle={styles.gridContainer}>
                {items.map((item) => {
                    const isSelected = equipState[selectedCategory] === item.id;
                    return (
                        <TouchableOpacity
                            key={item.id}
                            style={[styles.itemCard, isSelected && styles.selectedItemCard]}
                            onPress={() => updateEquip(selectedCategory, item.id)}
                        >
                            <View style={styles.itemImageContainer}>
                                {item.img ? (
                                    <Image source={item.img} style={styles.itemThumbnail} resizeMode="contain" />
                                ) : (
                                    <Text style={styles.noneText}>X</Text>
                                )}
                            </View>
                            <Text style={styles.itemName} numberOfLines={1}>{item.name}</Text>
                        </TouchableOpacity>
                    );
                })}
            </ScrollView>
        );
    };

    return (
        <View style={styles.fullscreen}>
            {/* 헤더 */}
            <View style={styles.header}>
                <TouchableOpacity style={styles.backButton} onPress={() => navigation.goBack()}>
                    <Text style={styles.backButtonText}>뒤로 가기</Text>
                </TouchableOpacity>
                <Text style={styles.headerTitle}>아바타 꾸미기</Text>
                <View style={{ width: 60 }} />
            </View>

            {/* 아바타 영역 */}
            <View style={styles.previewSection}>
                <View style={styles.podium} />
                <ChildAvatar equipState={equipState} size={320} />
            </View>

            {/* 인벤토리 영역 */}
            <View style={styles.inventorySection}>
                {/* 카테고리 */}
                <ScrollView
                    horizontal
                    showsHorizontalScrollIndicator={false}
                    style={styles.tabScroll}
                    contentContainerStyle={styles.tabContainer}
                >
                    {CATEGORIES.map(cat => (
                        <TouchableOpacity
                            key={cat.id}
                            style={[styles.tabButton, selectedCategory === cat.id && styles.activeTabButton]}
                            onPress={() => setSelectedCategory(cat.id)}
                        >
                            <Text style={[styles.tabText, selectedCategory === cat.id && styles.activeTabText]}>
                                {cat.label}
                            </Text>
                        </TouchableOpacity>
                    ))}
                </ScrollView>

                {/* 그리드 */}
                <View style={styles.gridWrapper}>
                    {renderItemGrid()}
                </View>
            </View>
        </View>
    );
};

const styles = StyleSheet.create({
    fullscreen: {
        flex: 1,
        backgroundColor: '#F3F4F6',
    },
    header: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        paddingHorizontal: 20,
        paddingTop: 50,
        paddingBottom: 15,
        backgroundColor: '#FFFFFF',
    },
    backButton: {
        paddingVertical: 5,
        paddingHorizontal: 10,
    },
    backButtonText: {
        fontSize: 16,
        fontWeight: 'bold',
        color: '#4B5563',
    },
    headerTitle: {
        fontSize: 18,
        fontWeight: 'bold',
        color: '#111',
    },
    previewSection: {
        flex: 1,
        justifyContent: 'center',
        alignItems: 'center',
        position: 'relative',
        backgroundColor: '#EDF2F7',
    },
    podium: {
        position: 'absolute',
        bottom: '20%',
        width: 150,
        height: 40,
        backgroundColor: '#E2E8F0',
        borderRadius: 100,
        transform: [{ scaleY: 0.5 }],
    },
    inventorySection: {
        flex: 1,
        backgroundColor: '#FFFFFF',
        borderTopLeftRadius: 30,
        borderTopRightRadius: 30,
        shadowColor: '#000',
        shadowOffset: { width: 0, height: -4 },
        shadowOpacity: 0.05,
        shadowRadius: 10,
        elevation: 10,
    },
    tabScroll: {
        maxHeight: 60,
        borderBottomWidth: 1,
        borderBottomColor: '#E5E7EB',
    },
    tabContainer: {
        paddingHorizontal: 15,
        paddingTop: 15,
        paddingBottom: 10,
        alignItems: 'center',
    },
    tabButton: {
        paddingVertical: 8,
        paddingHorizontal: 16,
        marginHorizontal: 4,
        borderRadius: 20,
        backgroundColor: '#F3F4F6',
    },
    activeTabButton: {
        backgroundColor: '#374151',
    },
    tabText: {
        fontSize: 14,
        fontWeight: '600',
        color: '#6B7280',
    },
    activeTabText: {
        color: '#FFFFFF',
    },
    gridWrapper: {
        flex: 1,
    },
    gridContainer: {
        flexDirection: 'row',
        flexWrap: 'wrap',
        padding: 15,
        justifyContent: 'flex-start',
    },
    itemCard: {
        width: (width - 60) / 3, // 3열 배치 (여백 고려)
        height: 120,
        backgroundColor: '#F9FAFB',
        borderRadius: 16,
        padding: 10,
        alignItems: 'center',
        justifyContent: 'center',
        marginHorizontal: 5,
        marginBottom: 15,
        borderWidth: 2,
        borderColor: 'transparent',
    },
    selectedItemCard: {
        borderColor: '#3B82F6',
        backgroundColor: '#EFF6FF',
    },
    itemImageContainer: {
        flex: 1,
        width: '100%',
        alignItems: 'center',
        justifyContent: 'center',
        marginBottom: 8,
    },
    itemThumbnail: {
        width: 60,
        height: 60,
    },
    noneText: {
        fontSize: 24,
        color: '#9CA3AF',
        fontWeight: 'bold',
    },
    itemName: {
        fontSize: 12,
        fontWeight: '600',
        color: '#4B5563',
    },
    genderContainer: {
        flexDirection: 'row',
        width: '100%',
        justifyContent: 'space-around',
        padding: 20,
    },
    genderButton: {
        alignItems: 'center',
        justifyContent: 'center',
        backgroundColor: '#F3F4F6',
        width: '40%',
        aspectRatio: 1,
        borderRadius: 24,
    },
    selectedGender: {
        backgroundColor: '#DBEAFE',
        borderWidth: 2,
        borderColor: '#3B82F6',
    },
    genderText: {
        fontSize: 20,
        fontWeight: 'bold',
        color: '#1f2937',
    }
});

export default AvatarCustomScreen;
