import React, { useEffect, useRef } from 'react';
import { View, Animated, StyleSheet } from 'react-native';
import { scale } from 'react-native-size-matters';
import { AVATAR_ASSETS } from './AvatarAssets';

const Pet = ({ petType = 'shiba', size = 100 }) => {
    // 0: (가만히)
    // 1: (머리, 몸통 1px 아래로)
    // 2: (머리만 1px 더 아래로)
    // 3: (머리 1px 위로 = 1상태표시와 동일)
    const frameAnim = useRef(new Animated.Value(0)).current;

    useEffect(() => {
        // 아바타 숨쉬기랑 유사하게
        const loop = Animated.loop(
            Animated.sequence([
                Animated.delay(500), // 대기
                Animated.timing(frameAnim, { toValue: 1, duration: 0, useNativeDriver: true }),
                Animated.delay(300), // 머리/몸통 1단계 내려간 상태 유지
                Animated.timing(frameAnim, { toValue: 2, duration: 0, useNativeDriver: true }),
                Animated.delay(400), // 머리만 한단계 더 내려간 찐 숨쉬기 상태 유지
                Animated.timing(frameAnim, { toValue: 3, duration: 0, useNativeDriver: true }),
                Animated.delay(300), // 머리만 다시 올라옴
                Animated.timing(frameAnim, { toValue: 0, duration: 0, useNativeDriver: true }),
            ])
        );
        loop.start();

        return () => loop.stop();
    }, [frameAnim]);

    const stepSize = scale(1.5);

    const bodyTranslateY = frameAnim.interpolate({
        inputRange: [0, 1, 2, 3],
        outputRange: [0, stepSize, stepSize, stepSize] // 몸통은 1단계까지
    });

    const headTranslateY = frameAnim.interpolate({
        inputRange: [0, 1, 2, 3],
        outputRange: [0, stepSize, stepSize * 2, stepSize] // 머리는 2단계
    });

    // 펫 타입에 해당하는 이미지 가져오기
    const assets = AVATAR_ASSETS.pet[petType];
    if (!assets) return null;

    return (
        <View style={{ width: size, height: size, alignItems: 'center', justifyContent: 'flex-end' }}>
            {/* 1. 다리 (바닥에 고정, 아꾸는 다리가 없음) */}
            {assets.leg && (
                <View style={[styles.absoluteImage, { width: size, height: size }]}>
                    <Animated.Image
                        source={assets.leg}
                        style={{ width: '100%', height: '100%', resizeMode: 'contain' }}
                    />
                </View>
            )}

            {/* 2. 몸통 (프레임 1,2,3에서 1단계 아래로 이동, 단 아꾸는 바닥 고정) */}
            {assets.body && (
                <Animated.View style={[styles.absoluteImage, { width: size, height: size, transform: [{ translateY: petType === 'akku' ? 0 : bodyTranslateY }] }]}>
                    <Animated.Image
                        source={assets.body}
                        style={{ width: '100%', height: '100%', resizeMode: 'contain' }}
                    />
                </Animated.View>
            )}

            {/* 3. 머리상단(base) (프레임 단위로 1단계 -> 2단계 -> 1단계 내려감, 아꾸는 머리만 1단계 이동) */}
            {assets.base && (
                <Animated.View style={[styles.absoluteImage, { width: size, height: size, transform: [{ translateY: petType === 'akku' ? bodyTranslateY : headTranslateY }] }]}>
                    <Animated.Image
                        source={assets.base}
                        style={{ width: '100%', height: '100%', resizeMode: 'contain' }}
                    />
                </Animated.View>
            )}
        </View>
    );
};

const styles = StyleSheet.create({
    absoluteImage: {
        position: 'absolute',
        bottom: 0,
    }
});

export default Pet;
