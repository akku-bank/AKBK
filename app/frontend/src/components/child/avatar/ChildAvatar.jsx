import React, { useState, useEffect } from 'react';
import { View, Image, StyleSheet, TouchableWithoutFeedback } from 'react-native';
import { RFValue } from 'react-native-responsive-fontsize';
import { AVATAR_ASSETS } from './AvatarAssets';

const ChildAvatar = ({
    equipState,
    size = 250
}) => {
    const responsiveSize = RFValue(size);
    const [isSmiling, setIsSmiling] = useState(false);
    const [isBlinking, setIsBlinking] = useState(false);
    const [breathFrame, setBreathFrame] = useState(0);

    // 숨쉬기 모션
    useEffect(() => {
        const breathInterval = setInterval(() => {
            setBreathFrame(prev => (prev + 1) % 4);
        }, 600);
        return () => clearInterval(breathInterval);
    }, []);

    let bodyOffsetY = 0;
    let faceOffsetY = 0;

    if (breathFrame === 1 || breathFrame === 3) {
        bodyOffsetY = 2;
        faceOffsetY = 2;
    } else if (breathFrame === 2) {
        bodyOffsetY = 2;
        faceOffsetY = 4;
    }

    // 눈 깜빡
    useEffect(() => {
        const blinkInterval = setInterval(() => {
            setIsBlinking(true);
            setTimeout(() => setIsBlinking(false), 200);
        }, 5000);
        return () => clearInterval(blinkInterval);
    }, []);

    useEffect(() => {
        let smileTimeout;
        if (isSmiling) {
            smileTimeout = setTimeout(() => {
                setIsSmiling(false);
            }, 3000);
        }
        return () => clearTimeout(smileTimeout);
    }, [isSmiling]);

    const renderFace = () => {
        let faceImg = AVATAR_ASSETS.face.base.boy1; // Default fallback
        if (equipState.face && AVATAR_ASSETS.face.base[equipState.face]) {
            faceImg = AVATAR_ASSETS.face.base[equipState.face];
        }

        // 터치 시 웃기 (기본값인 boy1 기준으로만 임시 매핑 - 표정 분기 나중에 확장 가능)
        if (isSmiling) {
            faceImg = AVATAR_ASSETS.face.smile[equipState.face] || AVATAR_ASSETS.face.smile.boy1;
        } else if (isBlinking) {
            faceImg = AVATAR_ASSETS.face.closed[equipState.face] || AVATAR_ASSETS.face.closed.boy1;
        }

        if (!faceImg) return null;

        return (
            <View style={[styles.layer, { transform: [{ translateY: faceOffsetY }] }]}>
                <Image source={faceImg} style={styles.layer} />
            </View>
        );
    };

    const renderDefaultAvatar = () => (
        <View style={[styles.canvasContainer, { width: responsiveSize, height: responsiveSize }]}>
            {/* 1. 호흡 시 날개도 상체 따라 움직이기 (자율 등 장식 - 바보는 제외) */}
            <View style={[styles.layer, { transform: [{ translateY: bodyOffsetY }] }]}>
                {equipState.back !== 'none' && equipState.back !== 'babo' && AVATAR_ASSETS.decoration[equipState.back] && (
                    <Image source={AVATAR_ASSETS.decoration[equipState.back]} style={styles.layer} />
                )}
            </View>

            {/* 3. 상체 이동 (상의가 하의 밑에 깔리도록 먼저 렌더링) */}
            <View style={[styles.layer, { transform: [{ translateY: bodyOffsetY }] }]}>
                <Image source={AVATAR_ASSETS.body.upper_base} style={styles.layer} />

                {/* 상의 장착 (한벌옷이 있으면 한벌옷 상의 우선 렌더링) */}
                {equipState.outfit && equipState.outfit !== 'none' && AVATAR_ASSETS.outfit[equipState.outfit] ? (
                    <Image source={AVATAR_ASSETS.outfit[equipState.outfit].upper} style={styles.layer} />
                ) : (
                    equipState.upper !== 'none' && AVATAR_ASSETS.upper[equipState.upper] && (
                        <Image source={AVATAR_ASSETS.upper[equipState.upper]} style={styles.layer} />
                    )
                )}
            </View>

            {/* 하체 고정 (상의 위에 렌더링되도록 뒤로 배치) */}
            <Image source={AVATAR_ASSETS.body.lower_base} style={styles.layer} />

            {/* 하의 장착 (한벌옷이 있으면 한벌옷 하의 우선 렌더링) */}
            {equipState.outfit && equipState.outfit !== 'none' && AVATAR_ASSETS.outfit[equipState.outfit] ? (
                <Image source={AVATAR_ASSETS.outfit[equipState.outfit].lower} style={styles.layer} />
            ) : (
                equipState.lower !== 'none' && AVATAR_ASSETS.lower[equipState.lower] && (
                    <Image source={AVATAR_ASSETS.lower[equipState.lower]} style={styles.layer} />
                )
            )}

            {/* 신발 장착 (한벌옷이 있으면 한벌옷 신발 우선 렌더링) */}
            {equipState.outfit && equipState.outfit !== 'none' && AVATAR_ASSETS.outfit[equipState.outfit] && AVATAR_ASSETS.outfit[equipState.outfit].shoe ? (
                <Image source={AVATAR_ASSETS.outfit[equipState.outfit].shoe} style={styles.layer} />
            ) : (
                equipState.shoe !== 'none' && AVATAR_ASSETS.shoe[equipState.shoe] && (
                    <Image source={AVATAR_ASSETS.shoe[equipState.shoe]} style={styles.layer} />
                )
            )}


            {/* 4. 얼굴 */}
            {renderFace()}

            {/* 5. 헤어 및 모자 -> 모자 장착 시 다시 헤어 가리기 (User Request) */}
            <View style={[styles.layer, { transform: [{ translateY: faceOffsetY }] }]}>
                {equipState.hair !== 'none' && equipState.hat === 'none' && AVATAR_ASSETS.hair[equipState.hair] && (
                    <Image source={AVATAR_ASSETS.hair[equipState.hair]} style={styles.layer} />
                )}
                {equipState.hat !== 'none' && AVATAR_ASSETS.hat[equipState.hat] && (
                    <Image source={AVATAR_ASSETS.hat[equipState.hat]} style={styles.layer} />
                )}
            </View>

            {/* 6. 바보 딱지 (상체에 바짝 붙은 가장 앞 레이어) */}
            {equipState.back === 'babo' && AVATAR_ASSETS.decoration.babo && (
                <View style={[styles.layer, { transform: [{ translateY: bodyOffsetY }] }]}>
                    <Image source={AVATAR_ASSETS.decoration.babo} style={styles.layer} />
                </View>
            )}
        </View>
    );

    const handlePress = () => {
        if (!isSmiling) {
            setIsSmiling(true);
        }
    };

    return (
        <TouchableWithoutFeedback onPress={handlePress}>
            <View style={[styles.container, { width: responsiveSize, height: responsiveSize }]}>
                {renderDefaultAvatar()}
            </View>
        </TouchableWithoutFeedback>
    );
};

const styles = StyleSheet.create({
    container: {
        alignItems: 'center',
        justifyContent: 'center',
    },
    canvasContainer: {
        position: 'relative',
    },
    layer: {
        position: 'absolute',
        width: '100%',
        height: '100%',
        top: 0,
        left: 0,
        resizeMode: 'contain',
    }
});

export default ChildAvatar;