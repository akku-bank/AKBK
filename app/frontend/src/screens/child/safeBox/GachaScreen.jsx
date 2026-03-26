import React, { useEffect, useRef, useState } from 'react';
import {
    Animated,
    Easing,
    Image,
    Platform,
    SafeAreaView,
    StyleSheet,
    TouchableOpacity,
    View,
} from 'react-native';
import { scale, verticalScale } from 'react-native-size-matters';
import CustomText from '../../../components/common/CustomText';

const REWARD_IMAGE_MAP = {
    'app/frontend/src/assets/art/lastmeal.png': require('../../../assets/art/lastmeal.png'),
    'app/frontend/src/assets/art/monalisa.png': require('../../../assets/art/monalisa.png'),
    'app/frontend/src/assets/art/pearl.png': require('../../../assets/art/pearl.png'),
    'app/frontend/src/assets/art/scream.png': require('../../../assets/art/scream.png'),
    'app/frontend/src/assets/art/starnight.png': require('../../../assets/art/starnight.png'),
    'app/frontend/src/assets/tree/apple.png': require('../../../assets/tree/apple.png'),
    'app/frontend/src/assets/tree/bamboo.png': require('../../../assets/tree/bamboo.png'),
    'app/frontend/src/assets/tree/blossom.png': require('../../../assets/tree/blossom.png'),
    'app/frontend/src/assets/tree/buddle.png': require('../../../assets/tree/buddle.png'),
    'app/frontend/src/assets/tree/maple.png': require('../../../assets/tree/maple.png'),
    'app/frontend/src/assets/tree/palm.png': require('../../../assets/tree/palm.png'),
    'app/frontend/src/assets/tree/tree.png': require('../../../assets/tree/tree.png'),
    'app/frontend/src/assets/pet/akku-base.png': require('../../../assets/pet/akku-base.png'),
    'app/frontend/src/assets/pet/akku-body.png': require('../../../assets/pet/akku-body.png'),
    'app/frontend/src/assets/pet/cat-base.png': require('../../../assets/pet/cat-base.png'),
    'app/frontend/src/assets/pet/cat-body.png': require('../../../assets/pet/cat-body.png'),
    'app/frontend/src/assets/pet/cat-leg.png': require('../../../assets/pet/cat-leg.png'),
    'app/frontend/src/assets/pet/kdh.png': require('../../../assets/pet/kdh.png'),
    'app/frontend/src/assets/pet/kdh_special.png': require('../../../assets/pet/kdh_special.png'),
    'app/frontend/src/assets/pet/shiba-base.png': require('../../../assets/pet/shiba-base.png'),
    'app/frontend/src/assets/pet/shiba-body.png': require('../../../assets/pet/shiba-body.png'),
    'app/frontend/src/assets/pet/shiba-leg.png': require('../../../assets/pet/shiba-leg.png'),
};

const CATEGORY_LABEL_MAP = {
    art: '문화 예술 보상',
    tree: '나무심기 보상',
    pet: '유기동물 보상',
};

const CATEGORY_EMOJI_MAP = {
    art: '🎨',
    tree: '🌳',
    pet: '🐶',
};

const GachaScreen = ({ navigation, route }) => {
    const reward = route?.params?.reward ?? null;
    const [step, setStep] = useState('OPENING');

    const translateY = useRef(new Animated.Value(-verticalScale(300))).current;
    const translateX = useRef(new Animated.Value(-scale(100))).current;
    const rotate = useRef(new Animated.Value(0)).current;
    const scaleAnim = useRef(new Animated.Value(0.5)).current;

    useEffect(() => {
        Animated.sequence([
            Animated.parallel([
                Animated.spring(translateY, {
                    toValue: 0,
                    friction: 4,
                    tension: 20,
                    useNativeDriver: Platform.OS !== 'web',
                }),
                Animated.timing(translateX, {
                    toValue: 0,
                    duration: 1200,
                    easing: Easing.out(Easing.cubic),
                    useNativeDriver: Platform.OS !== 'web',
                }),
                Animated.timing(rotate, {
                    toValue: 1,
                    duration: 1200,
                    useNativeDriver: Platform.OS !== 'web',
                }),
                Animated.timing(scaleAnim, {
                    toValue: 1,
                    duration: 1000,
                    useNativeDriver: Platform.OS !== 'web',
                }),
            ]),
            Animated.sequence([
                Animated.timing(rotate, { toValue: 1.1, duration: 100, useNativeDriver: Platform.OS !== 'web' }),
                Animated.timing(rotate, { toValue: 0.9, duration: 100, useNativeDriver: Platform.OS !== 'web' }),
                Animated.timing(rotate, { toValue: 1.1, duration: 100, useNativeDriver: Platform.OS !== 'web' }),
                Animated.timing(rotate, { toValue: 1.0, duration: 100, useNativeDriver: Platform.OS !== 'web' }),
            ]),
        ]).start(() => {
            setStep('REVEAL');
        });
    }, [rotate, scaleAnim, translateX, translateY]);

    const spin = rotate.interpolate({
        inputRange: [0, 0.9, 1, 1.1],
        outputRange: ['0deg', '324deg', '360deg', '396deg'],
    });

    const rewardImage = reward?.resourceUrl ? REWARD_IMAGE_MAP[reward.resourceUrl] : null;
    const rewardCategoryLabel = reward?.category
        ? CATEGORY_LABEL_MAP[String(reward.category).toLowerCase()] || '기부 보상'
        : '기부 보상';
    const rewardCategoryEmoji = reward?.category
        ? CATEGORY_EMOJI_MAP[String(reward.category).toLowerCase()] || '🎁'
        : '🎁';
    const rewardDescription = reward?.isDuplicate
        ? '이미 가지고 있는 아이템이에요.'
        : '새 보상을 획득했어요.';

    const handleConfirm = () => {
        navigation.goBack();
    };

    return (
        <SafeAreaView style={styles.safeArea}>
            {step === 'OPENING' ? (
                <View style={styles.centerContainer}>
                    <Animated.View
                        style={[
                            styles.capsuleWrapper,
                            {
                                transform: [
                                    { translateY },
                                    { translateX },
                                    { rotate: spin },
                                    { scale: scaleAnim },
                                ],
                            },
                        ]}
                    >
                        <CustomText style={styles.boxEmoji}>🎁</CustomText>
                    </Animated.View>

                    <CustomText style={styles.capsuleText}>
                        보상을 준비하고 있어요
                    </CustomText>
                </View>
            ) : (
                <View style={styles.revealContainer}>
                    <CustomText style={styles.tadaEmoji}>{rewardCategoryEmoji}</CustomText>
                    <CustomText style={styles.titleText}>보상을 획득했어요</CustomText>

                    <View style={styles.rewardCard}>
                        {rewardImage ? (
                            <Image source={rewardImage} style={styles.rewardImage} resizeMode="contain" />
                        ) : (
                            <CustomText style={styles.rewardFallbackEmoji}>{rewardCategoryEmoji}</CustomText>
                        )}
                        <CustomText style={styles.rewardCategory}>{rewardCategoryLabel}</CustomText>
                        <CustomText style={styles.rewardName}>
                            {reward?.rewardItemName || '알 수 없는 보상'}
                        </CustomText>
                        <CustomText style={styles.rewardDesc}>{rewardDescription}</CustomText>
                    </View>

                    <TouchableOpacity style={styles.confirmButton} onPress={handleConfirm}>
                        <CustomText style={styles.confirmButtonText}>확인</CustomText>
                    </TouchableOpacity>
                </View>
            )}
        </SafeAreaView>
    );
};

const styles = StyleSheet.create({
    safeArea: {
        flex: 1,
        backgroundColor: '#F3F4F6',
    },
    centerContainer: {
        flex: 1,
        justifyContent: 'center',
        alignItems: 'center',
        paddingHorizontal: scale(20),
    },
    capsuleWrapper: {
        width: scale(120),
        height: scale(120),
        justifyContent: 'center',
        alignItems: 'center',
        marginBottom: verticalScale(40),
    },
    boxEmoji: {
        fontSize: scale(100),
    },
    capsuleText: {
        fontSize: scale(18),
        fontWeight: 'bold',
        color: '#4B5563',
        marginTop: verticalScale(20),
    },
    revealContainer: {
        flex: 1,
        justifyContent: 'center',
        alignItems: 'center',
        paddingHorizontal: scale(20),
    },
    tadaEmoji: {
        fontSize: scale(60),
        marginBottom: verticalScale(16),
    },
    titleText: {
        fontSize: scale(24),
        fontWeight: '900',
        color: '#111',
        marginBottom: verticalScale(32),
    },
    rewardCard: {
        width: '100%',
        backgroundColor: '#FFFFFF',
        borderRadius: scale(24),
        paddingVertical: verticalScale(32),
        paddingHorizontal: scale(20),
        alignItems: 'center',
        marginBottom: verticalScale(40),
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 4 },
        shadowOpacity: 0.05,
        shadowRadius: 12,
        elevation: 3,
    },
    rewardImage: {
        width: scale(140),
        height: scale(140),
        marginBottom: verticalScale(16),
    },
    rewardFallbackEmoji: {
        fontSize: scale(70),
        marginBottom: verticalScale(16),
    },
    rewardCategory: {
        fontSize: scale(14),
        color: '#6B7280',
        marginBottom: verticalScale(8),
    },
    rewardName: {
        fontSize: scale(22),
        fontWeight: 'bold',
        color: '#3B82F6',
        marginBottom: verticalScale(8),
        textAlign: 'center',
    },
    rewardDesc: {
        fontSize: scale(14),
        color: '#6B7280',
        textAlign: 'center',
    },
    confirmButton: {
        width: '100%',
        backgroundColor: '#3B82F6',
        paddingVertical: verticalScale(16),
        borderRadius: scale(16),
        alignItems: 'center',
    },
    confirmButtonText: {
        fontSize: scale(18),
        fontWeight: 'bold',
        color: '#FFFFFF',
    },
});

export default GachaScreen;
