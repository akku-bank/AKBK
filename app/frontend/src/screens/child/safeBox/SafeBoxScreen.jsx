import React, { useCallback, useRef, useState } from 'react';
import {
    ActivityIndicator,
    Alert,
    Animated,
    KeyboardAvoidingView,
    Platform,
    SafeAreaView,
    ScrollView,
    StyleSheet,
    TouchableOpacity,
    View,
} from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { RFValue } from 'react-native-responsive-fontsize';
import CustomText from '../../../components/common/CustomText';
import api from '../../../api/axios';

const CHARITY_EMOJI_MAP = {
    '문화 예술': '🎨',
    나무심기: '🌳',
    '유기동물 보호소': '🐶',
};

const CHARITY_DESCRIPTION_MAP = {
    '문화 예술': '아이들을 위한 공연과 전시를 후원해요.',
    나무심기: '도시 숲 조성과 나무 식재 활동에 기부해요.',
    '유기동물 보호소': '보호소 사료와 치료비를 지원해요.',
};

const SafeBoxScreen = ({ navigation }) => {
    const [isLoading, setIsLoading] = useState(true);
    const [hubInfo, setHubInfo] = useState(null);
    const [charities, setCharities] = useState([]);
    const progressAnim = useRef(new Animated.Value(0)).current;

    const animateProgress = (current, goal) => {
        const value = Math.min(current / (goal || 1), 1);
        Animated.timing(progressAnim, {
            toValue: value,
            duration: 800,
            useNativeDriver: false,
        }).start();
    };

    const fetchCharities = async () => {
        try {
            const res = await api.get('/jelling-hub/charities');
            setCharities(res.data?.data?.charities || []);
        } catch (error) {
            console.error('Fetch Charities Error:', error);
        }
    };

    const fetchHubInfo = async () => {
        setIsLoading(true);
        try {
            const res = await api.get('/jelling-hub');
            const data = res.data?.data;

            if (data) {
                setHubInfo(data);
                if (data.activeCharity) {
                    animateProgress(data.remainJelling, data.activeCharity.targetAmount);
                } else {
                    await fetchCharities();
                }
            }
        } catch (error) {
            console.error('Fetch Hub Error:', error);
            Alert.alert('오류', '젤링 허브 정보를 불러오지 못했습니다.');
        } finally {
            setIsLoading(false);
        }
    };

    useFocusEffect(
        useCallback(() => {
            fetchHubInfo();
        }, [])
    );

    const handleSelectCharity = async (charity) => {
        Alert.alert(
            '기부처를 선택할까요?',
            `${charity.name}을 현재 기부 목표로 설정할까요?`,
            [
                { text: '취소', style: 'cancel' },
                {
                    text: '확인',
                    onPress: async () => {
                        try {
                            setIsLoading(true);
                            await api.post('/jelling-hub/active-charity', {
                                charityId: charity.charityId,
                            });
                            await fetchHubInfo();
                            Alert.alert('성공', '기부 목표가 설정되었습니다.');
                        } catch (error) {
                            console.error('Set Active Charity Error:', error);
                            Alert.alert('오류', '기부 목표 설정에 실패했습니다.');
                            setIsLoading(false);
                        }
                    },
                },
            ]
        );
    };

    const handleReward = async () => {
        try {
            setIsLoading(true);
            const res = await api.post('/jelling-hub/donations/rewards');
            navigation.navigate('GachaScreen', {
                reward: res.data?.data ?? null,
            });
        } catch (error) {
            console.error('Reward Claim Error:', error);
            Alert.alert('오류', '보상 수령에 실패했습니다.');
            setIsLoading(false);
        }
    };

    if (isLoading && !hubInfo) {
        return (
            <SafeAreaView style={[styles.safeArea, styles.loaderContainer]}>
                <ActivityIndicator size="large" color="#10B981" />
            </SafeAreaView>
        );
    }

    const { remainJelling, activeCharity } = hubInfo || {
        remainJelling: 0,
        activeCharity: null,
    };
    const canClaimReward = Boolean(
        activeCharity && remainJelling >= activeCharity.targetAmount
    );

    return (
        <SafeAreaView style={styles.safeArea}>
            <View style={styles.header}>
                <CustomText style={styles.headerTitle}>젤링 주머니</CustomText>
            </View>

            <KeyboardAvoidingView
                style={styles.flex}
                behavior={Platform.OS === 'ios' ? 'padding' : undefined}
            >
                <ScrollView contentContainerStyle={styles.container}>
                    <View style={styles.balanceHeader}>
                        <CustomText style={styles.balanceLabel}>내 젤링 주머니</CustomText>
                        <CustomText style={styles.balanceValue}>
                            {remainJelling.toLocaleString()} 💎
                        </CustomText>
                    </View>

                    {!activeCharity ? (
                        <View style={styles.charitySelectionBox}>
                            <CustomText style={styles.sectionTitle}>어디에 기부해 볼까요?</CustomText>
                            <CustomText style={styles.selectionDescription}>
                                원하는 기부처를 먼저 선택해주세요.
                            </CustomText>

                            {charities.map((charity) => (
                                <TouchableOpacity
                                    key={charity.charityId}
                                    style={styles.charityItemCard}
                                    onPress={() => handleSelectCharity(charity)}
                                >
                                    <CustomText style={styles.targetEmoji}>
                                        {CHARITY_EMOJI_MAP[charity.name] || '🎁'}
                                    </CustomText>
                                    <View style={styles.targetInfo}>
                                        <CustomText style={styles.targetTitle}>
                                            {charity.name}
                                        </CustomText>
                                        <CustomText style={styles.targetDesc}>
                                            {charity.description}
                                        </CustomText>
                                    </View>
                                </TouchableOpacity>
                            ))}
                        </View>
                    ) : (
                        <View style={styles.donationTargetBox}>
                            <CustomText style={styles.sectionTitle}>현재 기부 목표</CustomText>

                            <View style={styles.activeTargetCard}>
                                <CustomText style={styles.targetEmoji}>
                                    {CHARITY_EMOJI_MAP[activeCharity.name] || '🎁'}
                                </CustomText>
                                <View style={styles.targetInfo}>
                                    <CustomText style={styles.targetTitle}>
                                        {activeCharity.name}
                                    </CustomText>
                                    <CustomText style={styles.targetDesc}>
                                        {CHARITY_DESCRIPTION_MAP[activeCharity.name] ||
                                            '기부를 진행해보세요.'}
                                    </CustomText>
                                </View>
                            </View>

                            <View style={styles.gaugeContainer}>
                                <View style={styles.gaugeTexts}>
                                    <CustomText style={styles.gaugeCurrentText}>
                                        {remainJelling} 💎
                                    </CustomText>
                                    <CustomText style={styles.gaugeGoalText}>
                                        / {activeCharity.targetAmount} 💎
                                    </CustomText>
                                </View>
                                <View style={styles.progressBarBg}>
                                    <Animated.View
                                        style={[
                                            styles.progressBarFill,
                                            {
                                                width: progressAnim.interpolate({
                                                    inputRange: [0, 1],
                                                    outputRange: ['0%', '100%'],
                                                }),
                                            },
                                        ]}
                                    />
                                </View>
                            </View>

                            <TouchableOpacity
                                style={[
                                    styles.gachaButton,
                                    !canClaimReward && styles.gachaButtonDisabled,
                                ]}
                                onPress={canClaimReward ? handleReward : undefined}
                                disabled={!canClaimReward}
                                activeOpacity={canClaimReward ? 0.7 : 1}
                            >
                                <CustomText
                                    style={[
                                        styles.gachaButtonText,
                                        !canClaimReward && styles.gachaButtonTextDisabled,
                                    ]}
                                >
                                    보상 획득하기 🎁
                                </CustomText>
                            </TouchableOpacity>
                        </View>
                    )}
                </ScrollView>
            </KeyboardAvoidingView>
        </SafeAreaView>
    );
};

const styles = StyleSheet.create({
    safeArea: { flex: 1, backgroundColor: '#F3F4F6' },
    flex: { flex: 1 },
    loaderContainer: { justifyContent: 'center', alignItems: 'center' },
    header: { padding: RFValue(16), backgroundColor: '#FFF', alignItems: 'center' },
    headerTitle: { fontSize: RFValue(18), fontWeight: 'bold', color: '#111' },
    container: { padding: RFValue(20) },
    balanceHeader: {
        backgroundColor: '#FFF',
        borderRadius: RFValue(16),
        padding: RFValue(24),
        alignItems: 'center',
        marginBottom: RFValue(20),
    },
    balanceLabel: {
        fontSize: RFValue(14),
        color: '#6B7280',
        marginBottom: RFValue(8),
    },
    balanceValue: {
        fontSize: RFValue(28),
        fontWeight: 'bold',
        color: '#D97706',
    },
    charitySelectionBox: {
        backgroundColor: '#FFF',
        borderRadius: RFValue(20),
        padding: RFValue(20),
        marginBottom: RFValue(20),
    },
    sectionTitle: {
        fontSize: RFValue(16),
        fontWeight: 'bold',
        color: '#111',
        marginBottom: RFValue(8),
    },
    selectionDescription: {
        color: '#6B7280',
        marginBottom: RFValue(16),
    },
    charityItemCard: {
        backgroundColor: '#F9FAFB',
        borderWidth: 1,
        borderColor: '#E5E7EB',
        padding: RFValue(16),
        borderRadius: RFValue(12),
        marginBottom: RFValue(12),
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
    },
    donationTargetBox: {
        backgroundColor: '#FFF',
        borderRadius: RFValue(20),
        padding: RFValue(20),
        marginBottom: RFValue(20),
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 4 },
        shadowOpacity: 0.05,
        shadowRadius: 10,
        elevation: 3,
    },
    activeTargetCard: {
        flexDirection: 'row',
        alignItems: 'center',
        backgroundColor: '#F9FAFB',
        padding: RFValue(16),
        borderRadius: RFValue(16),
        marginBottom: RFValue(24),
    },
    targetEmoji: { fontSize: RFValue(36), marginRight: RFValue(16) },
    targetInfo: { flex: 1, paddingRight: RFValue(8) },
    targetTitle: {
        fontSize: RFValue(15),
        fontWeight: 'bold',
        color: '#111',
        marginBottom: RFValue(5),
    },
    targetDesc: {
        fontSize: RFValue(10.4),
        color: '#6B7280',
        lineHeight: RFValue(12.5),
        paddingRight: RFValue(6),
    },
    gaugeContainer: { marginBottom: RFValue(24) },
    gaugeTexts: {
        flexDirection: 'row',
        alignItems: 'baseline',
        marginBottom: RFValue(8),
    },
    gaugeCurrentText: {
        fontSize: RFValue(24),
        fontWeight: '900',
        color: '#10B981',
    },
    gaugeGoalText: {
        fontSize: RFValue(14),
        fontWeight: 'bold',
        color: '#9CA3AF',
        marginLeft: RFValue(4),
    },
    progressBarBg: {
        height: RFValue(20),
        backgroundColor: '#E5E7EB',
        borderRadius: RFValue(10),
        overflow: 'hidden',
    },
    progressBarFill: {
        height: '100%',
        backgroundColor: '#10B981',
        borderRadius: RFValue(10),
    },
    gachaButton: {
        backgroundColor: '#F59E0B',
        paddingVertical: RFValue(14),
        borderRadius: RFValue(12),
        alignItems: 'center',
    },
    gachaButtonText: {
        color: '#FFF',
        fontWeight: 'bold',
        fontSize: RFValue(16),
    },
    gachaButtonDisabled: { backgroundColor: '#D1D5DB' },
    gachaButtonTextDisabled: { color: '#6B7280' },
});

export default SafeBoxScreen;
