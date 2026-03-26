import React, { useState, useRef } from 'react';
import { View, Text, StyleSheet, TouchableOpacity, Platform, Image, FlatList, useWindowDimensions } from 'react-native';
import CustomText from '../../components/common/CustomText';
import { SafeAreaView } from 'react-native-safe-area-context';
import { RFValue } from 'react-native-responsive-fontsize';

const TUTORIAL_DATA = [
    {
        id: '1',
        title: '부모님을 위한\n똑똑한 자녀 금융 관리',
        subtitle: '아이의 용돈 목표를 승인하고\n주간 리포트로 소비 습관을 확인하세요.',
        image: require('../../assets/croco/akku-parents_tuto.png'),
    },
    {
        id: '2',
        title: '우리 가족만을 위한\n안전한 금융 그룹',
        subtitle: '가족 그룹을 만들고 아이를 초대해\n안전하고 재미있게 금융을 연습해요.',
        image: require('../../assets/croco/akku-family.png'),
    },
    {
        id: '3',
        title: '아이를 위한 첫 지갑,\n쉽고 빠른 결제',
        subtitle: '어디서나 아꾸뱅꾸를 통해 결제하고,\n스스로 소비 목표를 세워 실천해요.',
        image: require('../../assets/croco/akku-cheers.png'),
        imageStyle: { width: '55%' }, // 이미지 축소용
    },
    {
        id: '4',
        title: '매일 똑똑해지는 퀴즈,\n나만의 아바타 꾸미기',
        subtitle: '재밌게 금융 퀴즈 풀고,\n젤링을 모아 아바타 꾸미고 기부까지!',
        image: require('../../assets/croco/akku-welcome.png'),
    }
];

const OnboardingTutorialScreen = ({ navigation }) => {
    const { width } = useWindowDimensions();
    const [currentIndex, setCurrentIndex] = useState(0);
    const flatListRef = useRef(null);

    const handleSkip = () => {
        navigation.replace('SocialLogin');
    };

    const handleNext = () => {
        if (currentIndex < TUTORIAL_DATA.length - 1) {
            const nextIndex = currentIndex + 1;
            // 웹 등 다양한 환경 호환성을 위해 scrollToOffset 사용
            flatListRef.current?.scrollToOffset({
                offset: nextIndex * width,
                animated: true
            });
            setCurrentIndex(nextIndex);
        } else {
            handleSkip();
        }
    };

    const handleScroll = (e) => {
        const offsetX = e.nativeEvent.contentOffset.x;
        const index = Math.round(offsetX / width);
        if (index >= 0 && index < TUTORIAL_DATA.length && index !== currentIndex) {
            setCurrentIndex(index);
        }
    };

    const renderItem = ({ item }) => (
        <View style={[styles.slide, { width }]}>
            <View style={styles.textSection}>
                <CustomText style={styles.title}>{item.title}</CustomText>
                <CustomText style={styles.subtitle}>{item.subtitle}</CustomText>
            </View>
            <View style={styles.imageSection}>
                <Image
                    source={item.image}
                    style={[styles.heroImage, item.imageStyle]} // 개별 스타일 덮어쓰기 지원
                    resizeMode="contain"
                />
            </View>
        </View>
    );

    return (
        <SafeAreaView style={styles.safeArea}>
            <View style={styles.container}>
                {/* 스킵 버튼 */}
                <View style={styles.headerRow}>
                    {currentIndex < TUTORIAL_DATA.length - 1 ? (
                        <TouchableOpacity style={styles.skipButton} onPress={handleSkip}>
                            <CustomText style={styles.skipText}>건너뛰기</CustomText>
                        </TouchableOpacity>
                    ) : (
                        <View style={styles.skipPlaceholder} />
                    )}
                </View>

                {/* 슬라이드 */}
                <View style={styles.flatListContainer}>
                    <FlatList
                        ref={flatListRef}
                        data={TUTORIAL_DATA}
                        horizontal
                        pagingEnabled
                        showsHorizontalScrollIndicator={false}
                        bounces={false}
                        keyExtractor={(item) => item.id}
                        onScroll={handleScroll}
                        scrollEventThrottle={16}
                        renderItem={renderItem}
                        getItemLayout={(data, index) => ({
                            length: width,
                            offset: width * index,
                            index,
                        })}
                    />
                </View>

                {/* 하단 네비게이션 및 버튼 */}
                <View style={styles.bottomSection}>
                    <View style={styles.dotsContainer}>
                        {TUTORIAL_DATA.map((_, index) => (
                            <View
                                key={index}
                                style={[
                                    styles.dot,
                                    currentIndex === index ? styles.activeDot : styles.inactiveDot
                                ]}
                            />
                        ))}
                    </View>

                    <TouchableOpacity
                        style={styles.actionButton}
                        onPress={handleNext}
                        activeOpacity={0.8}
                    >
                        <CustomText style={styles.actionButtonText}>
                            {currentIndex === TUTORIAL_DATA.length - 1 ? '아꾸뱅꾸 시작하기' : '다음'}
                        </CustomText>
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
        paddingTop: Platform.OS === 'ios' ? RFValue(10) : RFValue(20),
        paddingBottom: Platform.OS === 'web' ? RFValue(120) : (Platform.OS === 'ios' ? RFValue(60) : RFValue(30)),
    },
    headerRow: {
        height: RFValue(40),
        flexDirection: 'row',
        justifyContent: 'flex-end',
        alignItems: 'center',
        paddingHorizontal: RFValue(24),
    },
    skipButton: {
        paddingVertical: RFValue(8),
        paddingLeft: RFValue(16),
    },
    skipText: {
        fontSize: RFValue(14),
        color: '#9CA3AF',
        fontWeight: '600',
    },
    skipPlaceholder: {
        height: RFValue(36),
    },
    flatListContainer: {
        flex: 1,
    },
    slide: {
        flex: 1,
        paddingHorizontal: RFValue(24),
        paddingTop: RFValue(40),
        justifyContent: 'space-between',
    },
    textSection: {
        alignItems: 'flex-start',
    },
    title: {
        fontSize: RFValue(28),
        fontWeight: 'bold',
        color: '#111',
        marginBottom: RFValue(16),
        lineHeight: RFValue(36),
    },
    subtitle: {
        fontSize: RFValue(16),
        color: '#6B7280',
        lineHeight: RFValue(24),
        fontWeight: '500',
    },
    imageSection: {
        flex: 1,
        alignItems: 'center',
        justifyContent: 'center',
        marginTop: RFValue(40),
        marginBottom: RFValue(30),
    },
    heroImage: {
        width: '80%',
        height: '100%',
        maxHeight: RFValue(240),
    },
    bottomSection: {
        width: '100%',
        paddingHorizontal: RFValue(24),
        marginTop: RFValue(10),
        marginBottom: RFValue(0)
    },
    dotsContainer: {
        flexDirection: 'row',
        justifyContent: 'center',
        alignItems: 'center',
        marginBottom: RFValue(24),
        gap: RFValue(8),
    },
    dot: {
        width: RFValue(8),
        height: RFValue(8),
        borderRadius: RFValue(4),
    },
    activeDot: {
        backgroundColor: '#111',
        width: RFValue(20),
    },
    inactiveDot: {
        backgroundColor: '#E5E7EB',
    },
    actionButton: {
        width: '100%',
        height: RFValue(54),
        borderRadius: RFValue(12),
        backgroundColor: '#A3E635',
        justifyContent: 'center',
        alignItems: 'center',
    },
    actionButtonText: {
        fontSize: RFValue(16),
        fontWeight: 'bold',
        color: '#FFFFFF',
    },
    kakaoButton: {
        backgroundColor: '#FEE500',
    },
    kakaoButtonText: {
        color: '#000000',
    }
});

export default OnboardingTutorialScreen;
