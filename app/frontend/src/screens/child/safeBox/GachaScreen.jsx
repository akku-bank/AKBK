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
    'app/frontend/src/assets/pet/akku.png': require('../../../assets/pet/akku.png'),
    'app/frontend/src/assets/pet/cat.png': require('../../../assets/pet/cat.png'),
    'app/frontend/src/assets/pet/kdh.png': require('../../../assets/pet/kdh.png'),
    'app/frontend/src/assets/pet/kdh_special.png': require('../../../assets/pet/kdh_special.png'),
    'app/frontend/src/assets/pet/shiba.png': require('../../../assets/pet/shiba.png'),
    'app/frontend/src/assets/pet/akku-base.png': require('../../../assets/pet/akku-base.png'),
};

const CATEGORY_LABEL_MAP = {
    art: '문화 예술 보상',
    art1: '문화 예술 보상',
    art2: '나무심기 보상',
    tree: '나무심기 보상',
    pet: '유기동물 지원 보상',
};

const CATEGORY_EMOJI_MAP = {
    art: '🎨',
    art1: '🎨',
    art2: '🌳',
    tree: '🌳',
    pet: '🐶',
};

const GachaScreen = ({ navigation, route }) => {
    const reward = route?.params?.reward ?? null;
    const [step, setStep] = useState('OPENING');

    const translateX = useRef(new Animated.Value(-scale(300))).current; // Start off-screen left
    const rotate = useRef(new Animated.Value(0)).current;

    useEffect(() => {
        if (step !== 'OPENING') return;

        Animated.sequence([
            // Roll in from left to center
            Animated.parallel([
                Animated.timing(translateX, {
                    toValue: 0,
                    duration: 1500,
                    easing: Easing.out(Easing.cubic),
                    useNativeDriver: Platform.OS !== 'web',
                }),
                Animated.timing(rotate, {
                    toValue: 2, // 2 full rotations
                    duration: 1500,
                    easing: Easing.out(Easing.cubic),
                    useNativeDriver: Platform.OS !== 'web',
                }),
            ]),
            // Wiggle sequence
            Animated.sequence([
                // First Wiggle
                Animated.timing(rotate, { toValue: 2.1, duration: 200, useNativeDriver: Platform.OS !== 'web' }),
                Animated.timing(rotate, { toValue: 1.9, duration: 200, useNativeDriver: Platform.OS !== 'web' }),
                Animated.timing(rotate, { toValue: 2.0, duration: 200, useNativeDriver: Platform.OS !== 'web' }),

                // Stop and wait in center
                Animated.delay(400),

                // Second Wiggle
                Animated.timing(rotate, { toValue: 2.1, duration: 200, useNativeDriver: Platform.OS !== 'web' }),
                Animated.timing(rotate, { toValue: 1.9, duration: 200, useNativeDriver: Platform.OS !== 'web' }),
                Animated.timing(rotate, { toValue: 2.0, duration: 200, useNativeDriver: Platform.OS !== 'web' }),
            ]),
        ]).start(() => {
            setStep('EGG2');
            setTimeout(() => {
                setStep('EGG3');
                setTimeout(() => {
                    setStep('REVEAL');
                }, 1500);
            }, 1500);
        });
    }, [rotate, translateX, step]);

    const spin = rotate.interpolate({
        inputRange: [0, 1, 1.9, 2, 2.1],
        outputRange: ['0deg', '360deg', '684deg', '720deg', '756deg'],
    });

    let rewardImage = null;
    if (reward?.resourceUrl) {
        const urlStr = reward.resourceUrl;
        const fileName = urlStr.includes('/') ? urlStr.split('/').pop() : urlStr;
        if (fileName === 'akku.png' || fileName === 'akku') {
            rewardImage = [require('../../../assets/pet/akku-body.png'), require('../../../assets/pet/akku-base.png')];
        } else {
            const foundKey = Object.keys(REWARD_IMAGE_MAP).find(key => key.includes(fileName));
            if (foundKey) rewardImage = REWARD_IMAGE_MAP[foundKey];
        }
    }

    const rewardCategoryLabel = reward?.category
        ? CATEGORY_LABEL_MAP[String(reward.category).toLowerCase()] || '기부 보상'
        : '기부 보상';
    const rewardCategoryEmoji = reward?.category
        ? CATEGORY_EMOJI_MAP[String(reward.category).toLowerCase()] || '🎁'
        : '🎁';
    const isFallback = !reward?.name && !reward?.rewardItemName;
    const rewardNameStr = isFallback ? '준비된 아이템이 없습니다.' : (reward?.name || reward?.rewardItemName);
    const donationCompleteText = reward?.isDuplicate ? '아쉽게도 중복이에요!' : '기부가 완료되었어요!';

    if (!rewardImage && !isFallback && rewardNameStr) {
        if (rewardNameStr.includes('아꾸') || rewardNameStr.includes('akku')) rewardImage = [require('../../../assets/pet/akku-body.png'), require('../../../assets/pet/akku-base.png')];
        else if (rewardNameStr.includes('시바견') || rewardNameStr.includes('shiba')) rewardImage = require('../../../assets/pet/shiba.png');
        else if (rewardNameStr.includes('냥이') || rewardNameStr.includes('고양이') || rewardNameStr.includes('cat')) rewardImage = require('../../../assets/pet/cat.png');
        else if (rewardNameStr.includes('진주') || rewardNameStr.includes('pearl')) rewardImage = require('../../../assets/art/pearl.png');
        else if (rewardNameStr.includes('만찬') || rewardNameStr.includes('lastmeal')) rewardImage = require('../../../assets/art/lastmeal.png');
        else if (rewardNameStr.includes('모나리자') || rewardNameStr.includes('monalisa')) rewardImage = require('../../../assets/art/monalisa.png');
        else if (rewardNameStr.includes('절규') || rewardNameStr.includes('scream')) rewardImage = require('../../../assets/art/scream.png');
        else if (rewardNameStr.includes('별이') || rewardNameStr.includes('starnight')) rewardImage = require('../../../assets/art/starnight.png');
        else if (rewardNameStr.includes('사과') || rewardNameStr.includes('apple')) rewardImage = require('../../../assets/tree/apple.png');
        else if (rewardNameStr.includes('대나무') || rewardNameStr.includes('bamboo')) rewardImage = require('../../../assets/tree/bamboo.png');
        else if (rewardNameStr.includes('벚꽃') || rewardNameStr.includes('blossom')) rewardImage = require('../../../assets/tree/blossom.png');
        else if (rewardNameStr.includes('버드나무') || rewardNameStr.includes('buddle')) rewardImage = require('../../../assets/tree/buddle.png');
        else if (rewardNameStr.includes('단풍') || rewardNameStr.includes('maple')) rewardImage = require('../../../assets/tree/maple.png');
        else if (rewardNameStr.includes('야자') || rewardNameStr.includes('palm')) rewardImage = require('../../../assets/tree/palm.png');
        else if (rewardNameStr.includes('나무') || rewardNameStr.includes('tree')) rewardImage = require('../../../assets/tree/tree.png');
        else rewardImage = require('../../../assets/pet/akku-base.png');
    }

    const handleConfirm = () => {
        navigation.goBack();
    };

    return (
        <SafeAreaView style={styles.safeArea}>
            {step === 'OPENING' || step === 'EGG2' || step === 'EGG3' ? (
                <View style={styles.centerContainer}>
                    {step === 'OPENING' ? (
                        <Animated.View
                            style={{
                                transform: [
                                    { translateX },
                                    { rotate: spin },
                                ],
                            }}
                        >
                            <Image
                                source={require('../../../assets/egg.png')}
                                style={{ width: scale(220), height: scale(220) }}
                                resizeMode="contain"
                            />
                        </Animated.View>
                    ) : (
                        <Image
                            source={
                                step === 'EGG3' ? require('../../../assets/egg3.png') :
                                    require('../../../assets/egg2.png')
                            }
                            style={{ width: scale(220), height: scale(220) }}
                            resizeMode="contain"
                            fadeDuration={0}
                        />
                    )}
                </View>
            ) : (
                <View style={[styles.revealContainer, { justifyContent: 'center', paddingTop: 0 }]}>
                    <View style={styles.rewardCard}>
                        <CustomText style={[styles.titleText, { marginTop: verticalScale(10), marginBottom: verticalScale(20), textAlign: 'center' }]}>{donationCompleteText}</CustomText>
                        {rewardImage ? (
                            Array.isArray(rewardImage) ? (
                                <View style={[styles.rewardImage, { position: 'relative', marginTop: verticalScale(20), marginBottom: verticalScale(20) }]}>
                                    {rewardImage.map((img, idx) => (
                                        <Image key={idx} source={img} style={{ width: '100%', height: '100%', position: 'absolute', transform: [{ scale: 2.2 }, { translateY: verticalScale(-5) }, { translateX: scale(5) }] }} resizeMode="contain" />
                                    ))}
                                </View>
                            ) : (
                                <Image source={rewardImage} style={[styles.rewardImage, { transform: [{ scale: String(reward?.category).toLowerCase() === 'pet' ? 2.3 : 1.0 }, { translateY: String(reward?.category).toLowerCase() === 'pet' ? verticalScale(-10) : 0 }, { translateX: String(reward?.category).toLowerCase() === 'pet' ? scale(6) : 0 }] }]} resizeMode="contain" />
                            )
                        ) : null}

                        {!isFallback && <CustomText style={styles.rewardCategory}>{rewardCategoryLabel}</CustomText>}
                        <CustomText numberOfLines={2} adjustsFontSizeToFit={true} style={[styles.rewardName, isFallback && { color: '#111', marginTop: verticalScale(20) }]}>{rewardNameStr}</CustomText>
                    </View>

                    <TouchableOpacity style={[styles.confirmButton, { backgroundColor: '#A3E635' }]} onPress={handleConfirm}>
                        <CustomText style={[styles.confirmButtonText, { color: '#ffffff' }]}>확인</CustomText>
                    </TouchableOpacity>
                </View>
            )}
        </SafeAreaView>
    );
};

const styles = StyleSheet.create({
    safeArea: {
        flex: 1,
        backgroundColor: '#ECFCCB',
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
        marginBottom: verticalScale(16),
    },
    rewardCard: {
        width: '100%',
        backgroundColor: '#FFFFFF',
        borderRadius: scale(24),
        paddingVertical: verticalScale(20),
        paddingHorizontal: scale(20),
        alignItems: 'center',
        justifyContent: 'center',
        marginBottom: verticalScale(20),
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 4 },
        shadowOpacity: 0.05,
        shadowRadius: 12,
        elevation: 3,
    },
    rewardImage: {
        width: scale(220),
        height: scale(220),
        marginBottom: verticalScale(10),
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
        color: '#A3E635',
        textAlign: 'center',
    },
    confirmButton: {
        width: '100%',
        backgroundColor: '#A3E635',
        paddingVertical: verticalScale(16),
        borderRadius: scale(16),
        alignItems: 'center',
        shadowColor: '#000',
        shadowOffset: { width: 0, height: verticalScale(2) },
        shadowOpacity: 0.1,
        shadowRadius: scale(4),
        elevation: 3,
    },
    confirmButtonText: {
        fontSize: scale(18),
        fontWeight: 'bold',
        color: '#FFFFFF',
    },
});

export default GachaScreen;
