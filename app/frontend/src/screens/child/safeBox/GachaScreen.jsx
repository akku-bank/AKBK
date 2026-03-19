import React, { useState, useEffect, useRef } from 'react';
import { View, StyleSheet, TouchableOpacity, SafeAreaView, Dimensions, ActivityIndicator, Animated, Easing, Platform } from 'react-native';
import { scale, verticalScale } from 'react-native-size-matters';
import CustomText from '../../../components/common/CustomText';
import api from '../../../api/axios';

const { width, height } = Dimensions.get('window');

const GachaScreen = ({ navigation, route }) => {
    const { itemName = '지구 지키기 캠페인 기부', boxType = '지구' } = route?.params || {};

    const [step, setStep] = useState('OPENING'); // 개봉중 -> 결과확인 -> 완료
    const [reward, setReward] = useState(null);

    // 애니메이션 값
    const translateY = useRef(new Animated.Value(-verticalScale(300))).current; // 위에서 떨어짐
    const translateX = useRef(new Animated.Value(-scale(100))).current; // 왼쪽에서 굴러옴
    const rotate = useRef(new Animated.Value(0)).current; // 회전각
    const scaleAnim = useRef(new Animated.Value(0.5)).current; // 크기 커짐

    useEffect(() => {
        // 1. 캡슐 굴러나오는 애니메이션 시작
        Animated.sequence([
            // 1-1. 떨어지면서 구르기
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
                    toValue: 1, // 1 = 360도
                    duration: 1200,
                    useNativeDriver: Platform.OS !== 'web',
                }),
                Animated.timing(scaleAnim, {
                    toValue: 1,
                    duration: 1000,
                    useNativeDriver: Platform.OS !== 'web',
                })
            ]),
            // 1-2. 흔들거림 (열리기 직전 긴장감)
            Animated.sequence([
                Animated.timing(rotate, { toValue: 1.1, duration: 100, useNativeDriver: Platform.OS !== 'web' }),
                Animated.timing(rotate, { toValue: 0.9, duration: 100, useNativeDriver: Platform.OS !== 'web' }),
                Animated.timing(rotate, { toValue: 1.1, duration: 100, useNativeDriver: Platform.OS !== 'web' }),
                Animated.timing(rotate, { toValue: 1.0, duration: 100, useNativeDriver: Platform.OS !== 'web' }),
            ])
        ]).start(() => {
            // 애니메이션 종료 직후 결과 세팅 (약 2.5초 뒤)
            /* ==========================================
               [진짜 랜덤 가챠 뽑기 로직 API 연동]
               ========================================== 
            try {
                // 뽑기 결과 API 호출 (ex. 기부 완료 보상 또는 일반 뽑기)
                // const res = await api.post('/gacha/draw', { type: boxType });
                // setReward(res.data.data.reward);
            } catch(e) { console.error('Gacha Draw Error', e); }
            ========================================== */

            // --- 실제 연동 시 아래 임시 로직 삭제 ---
            const mockRewards = [
                { type: 'PET', name: '꼬마 북극곰', emoji: '🐻‍❄️', desc: '지구를 아끼는 멋진 마음이에요!' },
                { type: 'JELLING', name: '보너스 50 젤링', emoji: '🍬', desc: '기부 천사에게 주는 작은 선물!' },
                { type: 'FRAME', name: '에코 나무 액자', emoji: '🖼️', desc: '새로운 액자로 아바타를 꾸며보세요!' },
                { type: 'ITEM', name: '스페셜 왕관', emoji: '👑', desc: '아바타를 멋지게 꾸며보세요!' },
                { type: 'DUPLICATE', name: '앗! 꽝이에요', emoji: '😥', desc: '아쉽지만 이미 보유한 아이템이에요. 꽝!' }
            ];
            const randomPick = mockRewards[Math.floor(Math.random() * mockRewards.length)];
            setReward(randomPick);
            // ------------------------------------

            setStep('REVEAL');
        });
    }, []);

    // 회전 값을 각도로 변환
    const spin = rotate.interpolate({
        inputRange: [0, 0.9, 1, 1.1],
        outputRange: ['0deg', '324deg', '360deg', '396deg']
    });

    const handleConfirm = () => {
        // 원래 세이프박스 화면으로 복귀
        navigation.goBack();
    };

    return (
        <SafeAreaView style={styles.safeArea}>
            {step === 'OPENING' ? (
                <View style={styles.centerContainer}>
                    {/* 데구르르 굴러오는 캡슐 애니메이션 */}
                    <Animated.View style={[
                        styles.capsuleWrapper,
                        {
                            transform: [
                                { translateY: translateY },
                                { translateX: translateX },
                                { rotate: spin },
                                { scale: scaleAnim }
                            ]
                        }
                    ]}>
                        <CustomText style={styles.boxEmoji}>🥚</CustomText>
                    </Animated.View>

                    <CustomText style={styles.capsuleText}>
                        데구르르.. 캡슐이 나오고 있어요!
                    </CustomText>
                </View>
            ) : (
                <View style={styles.revealContainer}>
                    <CustomText style={styles.tadaEmoji}>🎉</CustomText>
                    <CustomText style={styles.titleText}>짜잔! 선물이 도착했어요</CustomText>

                    <View style={styles.rewardCard}>
                        <CustomText style={styles.rewardEmoji}>{reward?.emoji}</CustomText>
                        <CustomText style={styles.rewardName}>{reward?.name}</CustomText>
                        <CustomText style={styles.rewardDesc}>{reward?.desc}</CustomText>
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
        backgroundColor: '#F3F4F6', // 좀 더 부드러운 토스 스타일 배경
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
        paddingVertical: verticalScale(40),
        paddingHorizontal: scale(20),
        alignItems: 'center',
        marginBottom: verticalScale(40),
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 4 },
        shadowOpacity: 0.05,
        shadowRadius: 12,
        elevation: 3,
    },
    rewardEmoji: {
        fontSize: scale(70),
        marginBottom: verticalScale(16),
    },
    rewardName: {
        fontSize: scale(22),
        fontWeight: 'bold',
        color: '#3B82F6',
        marginBottom: verticalScale(8),
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
    }
});

export default GachaScreen;
