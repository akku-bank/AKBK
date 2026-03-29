import React, { useContext, useState, useRef } from 'react';
import { View, StyleSheet, TouchableOpacity, ScrollView, Image, Dimensions, Alert, ImageBackground } from 'react-native';
import { AvatarContext } from '../../../components/child/avatar/AvatarContext';
import { AVATAR_ITEMS, AVATAR_ASSETS } from '../../../components/child/avatar/AvatarAssets';
import ChildAvatar from '../../../components/child/avatar/ChildAvatar';
import Pet from '../../../components/child/avatar/Pet';
import { scale, verticalScale } from 'react-native-size-matters';
import CustomText from '../../../components/common/CustomText';
import api from '../../../api/axios';

const CATEGORIES = [
    { id: 'gender', label: '성별' },
    { id: 'face', label: '얼굴' },
    { id: 'hair', label: '헤어' },
    { id: 'outfit', label: '한벌옷' },
    { id: 'upper', label: '상의' },
    { id: 'lower', label: '하의' },
    { id: 'hat', label: '모자' },
    { id: 'shoe', label: '신발' },
    { id: 'back', label: '등' },
    { id: 'pet', label: '펫' },
    { id: 'art1', label: '미술품' },
    { id: 'art2', label: '나무' },
];

const { width } = Dimensions.get('window');

const AvatarCustomScreen = ({ navigation }) => {
    const { equipState, updateEquip, setEquipState } = useContext(AvatarContext);
    const [selectedCategory, setSelectedCategory] = useState('hair');
    const [ownedItemIds, setOwnedItemIds] = useState({});
    const [frontendToUuidMap, setFrontendToUuidMap] = useState({});
    const [userLevel, setUserLevel] = useState(1);
    const initialEquipState = React.useRef(null);

    // 컴포넌트 마운트 시 최초 장착 상태를 백업하여 취소/뒤로가기 시 복원용으로 사용
    React.useEffect(() => {
        if (!initialEquipState.current) {
            initialEquipState.current = { ...equipState };
        }
    }, []);

    // 백엔드 아이템 도감 페칭 및 레벨 조회
    React.useEffect(() => {
        const fetchItemsAndLevel = async () => {
            try {
                // 상단 아이템 조회를 위해 사용자 레벨부터 가져옴
                const homeRes = await api.get('/home');
                setUserLevel(homeRes.data?.data?.level || 1);

                const res = await api.get('/avatars/items');
                const items = res.data?.data?.items || [];

                const owned = {};
                const mapF2U = {};

                items.forEach(backendItem => {
                    for (const cat in AVATAR_ITEMS) {
                        const matchingFrontendItem = AVATAR_ITEMS[cat].find(i => i.name === backendItem.name);
                        if (matchingFrontendItem) {
                            if (backendItem.isOwned) {
                                owned[matchingFrontendItem.id] = true;
                            }
                            mapF2U[matchingFrontendItem.id] = backendItem.itemId;
                            break;
                        }
                    }
                });
                setOwnedItemIds(owned);
                setFrontendToUuidMap(mapF2U);
            } catch (e) { console.error('Avatar Items Fetch Error', e); }
        };
        fetchItemsAndLevel();
    }, []);

    const handleGenderChange = (gender) => {
        setEquipState(prev => ({
            ...prev,
            gender: gender,
            face: gender === 'boy' ? 'boy1' : 'girl1',
            hair: gender === 'boy' ? 'boy1' : 'girl1',
        }));
    };

    const renderGenderTab = () => (
        <View style={styles.genderContainer}>
            <TouchableOpacity
                style={[styles.genderButton, equipState.gender === 'boy' && styles.selectedGender]}
                onPress={() => handleGenderChange('boy')}
            >
                <CustomText style={styles.genderText}>남자</CustomText>
            </TouchableOpacity>
            <TouchableOpacity
                style={[styles.genderButton, equipState.gender === 'girl' && styles.selectedGender]}
                onPress={() => handleGenderChange('girl')}
            >
                <CustomText style={styles.genderText}>여자</CustomText>
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
        // 보유 중인 아이템만 필터링 (기본템/none/획득아이템 + 자신의 레벨로 자동 해금된 아이템)
        items = items.filter(item => {
            const isEquipCat = ['upper', 'lower', 'hat', 'shoe', 'back', 'outfit', 'pet', 'art1', 'art2'].includes(selectedCategory);
            const isBaseOrNone = item.id.includes('base') || item.id === 'none' || item.level === 1;
            const isLevelUnlocked = item.level <= userLevel && item.level !== 99;
            return !isEquipCat || isBaseOrNone || ownedItemIds[item.id] || isLevelUnlocked;
        });

        return (
            <ScrollView contentContainerStyle={styles.gridContainer}>
                {items.map((item) => {
                    const isSelected = equipState[selectedCategory] === item.id;

                    const isOwned = true; // 필터링을 거쳤으므로 전부 owned

                    let imageSize = item.id.includes('akku') ? '75%' : '90%';
                    if (selectedCategory === 'pet') {
                        imageSize = item.id.includes('akku') ? '240%' : '180%';
                    }

                    return (
                        <TouchableOpacity
                            key={item.id}
                            onPress={() => {
                                updateEquip(selectedCategory, item.id);
                            }}
                        >
                            <ImageBackground
                                source={require('../../../assets/box.png')}
                                style={[
                                    styles.itemCard,
                                    isSelected && styles.selectedItemCard
                                ]}
                                resizeMode="stretch"
                            >
                                <View style={styles.itemImageContainer}>
                                    {Array.isArray(item.img) ? (
                                        <View style={{ width: imageSize, height: imageSize, position: 'relative' }}>
                                            {item.img.map((imgSrc, idx) => (
                                                <Image key={idx} source={imgSrc} style={[{ width: '100%', height: '100%' }, { position: 'absolute' }]} resizeMode="contain" />
                                            ))}
                                        </View>
                                    ) : item.img ? (
                                        <Image source={item.img} style={{ width: imageSize, height: imageSize }} resizeMode="contain" />
                                    ) : (
                                        <CustomText style={styles.noneText}>X</CustomText>
                                    )}
                                </View>
                                <CustomText style={styles.itemName} numberOfLines={1}>{item.name}</CustomText>
                            </ImageBackground>
                        </TouchableOpacity>
                    );
                })}
            </ScrollView>
        );
    };

    return (
        <View style={styles.fullscreen}>
            {/* 상단 헤더 */}
            <View style={styles.header}>
                {/* 좌측: 순수 뒤로가기 (저장 안 된 변경사항 롤백) */}
                <TouchableOpacity style={styles.backButton} onPress={() => {
                    if (initialEquipState.current) {
                        setEquipState(initialEquipState.current); // 원본 상태로 강제 복구
                    }
                    navigation.goBack();
                }}>
                    <CustomText style={styles.backButtonText}>←</CustomText>
                </TouchableOpacity>

                {/* 중앙 타이틀 */}
                <CustomText style={styles.headerTitle}>아바타 꾸미기</CustomText>

                {/* 우측: 순수 저장 버튼 */}
                <TouchableOpacity style={styles.saveHeaderButton} onPress={async () => {
                    // 백엔드에 아바타 장착 옷차림 저장
                    try {
                        const equippedUUIDs = [];
                        ['hat', 'upper', 'lower', 'shoe', 'back', 'outfit', 'pet'].forEach(cat => {
                            if (equipState[cat] && equipState[cat] !== 'none' && frontendToUuidMap[equipState[cat]]) {
                                equippedUUIDs.push(frontendToUuidMap[equipState[cat]]);
                            }
                        });

                        await api.patch('/avatars/me/equipment', { equippedItemIds: equippedUUIDs });
                        initialEquipState.current = { ...equipState }; // 저장이 성공하면 기준점을 현재 상태로 업데이트!
                        Alert.alert('저장 완료', '저장되었습니다!');
                    } catch (e) {
                        console.error('Avatar Save Error', e);
                        Alert.alert('저장 실패', '저장 중 오류가 발생했습니다.');
                    }
                }}>
                    <CustomText style={styles.saveHeaderText}>저장</CustomText>
                </TouchableOpacity>
            </View>

            {/* 아바타 영역 */}
            <ImageBackground
                source={require('../../../assets/closet.jpg')}
                style={[styles.previewSection, { width: '100%' }]}
                resizeMode="cover"
            >
                <View style={styles.avatarWrapper}>
                    <ChildAvatar equipState={equipState} size={280} />

                    {/* 꾸미기 전용 펫 미리보기 */}
                    {equipState.pet && equipState.pet !== 'none' && (
                        <View style={{ position: 'absolute', right: scale(-210), bottom: verticalScale(-155) }} pointerEvents="none">
                            <Pet petType={equipState.pet} size={scale(400)} />
                        </View>
                    )}

                    {/* 꾸미기 전용 나무(ART_2) 미리보기 */}
                    {equipState.art2 && equipState.art2 !== 'none' && (() => {
                        const treeItem = AVATAR_ITEMS.art2.find(a => a.id === equipState.art2);
                        if (!treeItem || !treeItem.img) return null;
                        return (
                            <View style={{ position: 'absolute', right: scale(180), bottom: verticalScale(-35) }} pointerEvents="none">
                                <Image source={treeItem.img} style={{ width: scale(80), height: scale(110) }} resizeMode="contain" />
                            </View>
                        );
                    })()}

                </View>
            </ImageBackground>

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
                            <CustomText style={[styles.tabText, selectedCategory === cat.id && styles.activeTabText]}>
                                {cat.label}
                            </CustomText>
                        </TouchableOpacity>
                    ))}
                </ScrollView>

                {/* 그리드 영역 */}
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
        backgroundColor: '#ECFCCB',
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
        width: 32,
        height: 32,
        justifyContent: 'center',
    },
    backButtonText: {
        fontSize: 22,
        fontWeight: 'bold',
        color: '#111',
    },
    headerTitle: {
        fontSize: 18,
        fontWeight: 'bold',
        color: '#111',
    },
    saveHeaderButton: {
        width: 60,
        paddingVertical: 6,
        alignItems: 'flex-end',
        justifyContent: 'center',
    },
    saveHeaderText: {
        fontSize: 16,
        fontWeight: 'bold',
        color: '#111827',
    },
    previewSection: {
        flex: 1,
        justifyContent: 'center',
        alignItems: 'center',
        position: 'relative',
    },
    avatarWrapper: {
        position: 'relative',
        width: 280,
        height: 280,
        alignItems: 'center',
        justifyContent: 'center',
    },

    paintLaunchButton: {
        position: 'absolute',
        top: 20,
        right: 20,
        backgroundColor: '#FFF',
        paddingVertical: 10,
        paddingHorizontal: 16,
        borderRadius: 20,
        alignItems: 'center',
        justifyContent: 'center',
        shadowColor: '#000', shadowOffset: { width: 0, height: 2 }, shadowOpacity: 0.1, shadowRadius: 4, elevation: 2
    },
    paintClearButton: {
        position: 'absolute',
        top: 85,
        right: 20,
        backgroundColor: '#FEE2E2',
        paddingVertical: 10,
        paddingHorizontal: 16,
        borderRadius: 20,
        alignItems: 'center',
        justifyContent: 'center',
        shadowColor: '#000', shadowOffset: { width: 0, height: 2 }, shadowOpacity: 0.1, shadowRadius: 4, elevation: 2
    },
    paintLaunchIcon: {
        fontSize: 24,
        marginBottom: 4,
    },
    paintLaunchText: {
        fontSize: 12,
        fontWeight: 'bold',
        color: '#4B5563',
    },
    podium: {
        display: 'none',
    },
    inventorySection: {
        flex: 1,
        backgroundColor: '#ECFCCB',
        shadowColor: '#000',
        shadowOffset: { width: 0, height: -4 },
        shadowOpacity: 0.05,
        shadowRadius: 10,
        elevation: 10,
    },
    tabScroll: {
        flexGrow: 0,
        height: 65,
        backgroundColor: '#FFFFFF',
        borderBottomWidth: 1,
        borderBottomColor: '#E5E7EB',
    },
    tabContainer: {
        paddingHorizontal: 15,
        paddingTop: 12,
        paddingBottom: 0,
        alignItems: 'center',
    },
    tabButton: {
        paddingVertical: 14,
        paddingHorizontal: 16,
        marginHorizontal: 4,
        borderBottomWidth: 2,
        borderBottomColor: 'transparent',
    },
    activeTabButton: {
        borderBottomColor: '#A3E635',
    },
    tabText: {
        fontSize: 15,
        fontWeight: 'bold',
        color: '#9CA3AF',
    },
    activeTabText: {
        color: '#111',
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
        aspectRatio: 1670 / 2187,
        backgroundColor: 'transparent',
        alignItems: 'center',
        justifyContent: 'flex-start',
        marginHorizontal: 5,
        marginBottom: 15,
        paddingTop: 15,
        paddingBottom: 25,
    },
    selectedItemCard: {
        // 픽셀 아트 모서리 규격 불일치로 인한 테두리 삭제 (투명도 조절 등 대체재 생략)
    },
    itemImageContainer: {
        width: '100%',
        flex: 1,
        alignItems: 'center',
        justifyContent: 'center',
        position: 'relative',
        marginBottom: 4,
    },
    itemThumbnail: {
        width: '80%',
        height: '80%',
    },
    noneText: {
        fontSize: 24,
        color: '#111827',
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
        backgroundColor: '#FFFFFF',
        width: '40%',
        aspectRatio: 1,
        borderRadius: 24,
    },
    selectedGender: {
        backgroundColor: '#F7FEE7',
        borderWidth: 2,
        borderColor: '#A3E635',
    },
    genderText: {
        fontSize: 20,
        fontWeight: 'bold',
        color: '#1f2937',
    }
});

export default AvatarCustomScreen;
