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
        let faceImg = null;
        if (equipState.face && AVATAR_ASSETS.face[equipState.face]) {
            faceImg = AVATAR_ASSETS.face[equipState.face];
        }

        // 터치 시 웃기
        if (isSmiling) {
            faceImg = AVATAR_ASSETS.face.base_smile;
        } else if (isBlinking) {
            faceImg = AVATAR_ASSETS.face.base_closed;
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
            {/* 호흡 시 날개도 상체 따라 움직이기 */}
            <View style={[styles.layer, { transform: [{ translateY: bodyOffsetY }] }]}>
                {equipState.wing !== 'none' && AVATAR_ASSETS.acc[equipState.wing] && (
                    <Image source={AVATAR_ASSETS.acc[equipState.wing]} style={styles.layer} />
                )}
            </View>

            {/* 2. 하체 고정 */}
            <Image source={AVATAR_ASSETS.body_lower} style={styles.layer} />
            {equipState.lower !== 'none' && AVATAR_ASSETS.lower[equipState.lower] && (
                <Image source={AVATAR_ASSETS.lower[equipState.lower]} style={styles.layer} />
            )}
            {equipState.shoe !== 'none' && AVATAR_ASSETS.acc[equipState.shoe] && (
                <Image source={AVATAR_ASSETS.acc[equipState.shoe]} style={styles.layer} />
            )}

            {/* 3. 상체 이동 */}
            <View style={[styles.layer, { transform: [{ translateY: bodyOffsetY }] }]}>
                <Image source={AVATAR_ASSETS.body_upper} style={styles.layer} />
                {equipState.upper !== 'none' && AVATAR_ASSETS.upper[equipState.upper] && (
                    <Image source={AVATAR_ASSETS.upper[equipState.upper]} style={styles.layer} />
                )}
            </View>

            {/* 4. 얼굴 */}
            {renderFace()}

            {/* 5. 헤어 및 모자 -> 모자 장착 시 헤어 X */}
            <View style={[styles.layer, { transform: [{ translateY: faceOffsetY }] }]}>
                {equipState.hair !== 'none' && equipState.hat === 'none' && AVATAR_ASSETS.hair[equipState.hair] && (
                    <Image source={AVATAR_ASSETS.hair[equipState.hair]} style={styles.layer} />
                )}
                {equipState.hat !== 'none' && AVATAR_ASSETS.acc[equipState.hat] && (
                    <Image source={AVATAR_ASSETS.acc[equipState.hat]} style={styles.layer} />
                )}
            </View>
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