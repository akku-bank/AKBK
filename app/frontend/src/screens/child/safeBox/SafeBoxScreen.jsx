import React, { useCallback, useRef, useState } from 'react';
import {
    ActivityIndicator,
    Alert,
    Animated,
    Image,
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

    // ⚡️ force metro cache refresh
    let hubImage;
    if (remainJelling <= 100) {
        hubImage = require('../../../assets/hub/hub1.png');
    } else if (remainJelling < 500) {
        hubImage = require('../../../assets/hub/hub2.png');
    } else {
        hubImage = require('../../../assets/hub/hub3.png');
    }

    const formatDescription = (text) => {
        if (!text) return '';
        const words = text.split(' ');
        if (words.length > 4) {
            return words.slice(0, 4).join(' ') + '\n' + words.slice(4).join(' ');
        }
        return text;
    };

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
                    <View style={[styles.balanceHeader, activeCharity && { marginBottom: 0 }]}>
                        <View style={{ flexDirection: 'row', alignItems: 'baseline', marginBottom: RFValue(15), zIndex: 10 }}>
                            <CustomText style={styles.balanceValue}>
                                {remainJelling.toLocaleString()}
                            </CustomText>
                            <CustomText style={{ fontSize: RFValue(18), fontWeight: 'bold', color: '#9CA3AF', marginLeft: RFValue(6) }}>
                                젤링
                            </CustomText>
                        </View>
                        <Image
                            source={hubImage}
                            style={{ width: '100%', height: RFValue(200), transform: [{ scale: 1.8 }], marginTop: RFValue(0) }}
                            resizeMode="contain"
                        />
                    </View>

                    {!activeCharity ? (
                        <View style={styles.charitySelectionBox}>
                            <CustomText style={[styles.sectionTitle, { textAlign: 'center', marginBottom: RFValue(16) }]}>원하는 기부처를 선택해보세요!</CustomText>

                            {charities.map((charity) => (
                                <TouchableOpacity
                                    key={charity.charityId}
                                    style={styles.charityItemCard}
                                    onPress={() => handleSelectCharity(charity)}
                                >
                                    <View style={styles.targetInfo}>
                                        <CustomText style={styles.targetTitle}>
                                            {charity.name}
                                        </CustomText>
                                        <CustomText style={styles.targetDesc}>
                                            {formatDescription(charity.description)}
                                        </CustomText>
                                    </View>
                                </TouchableOpacity>
                            ))}
                        </View>
                    ) : (
                        <View style={[styles.donationTargetBox, { marginTop: RFValue(25) }]}>
                            <CustomText style={[styles.sectionTitle, { textAlign: 'center' }]}>현재 기부 목표</CustomText>

                            <View style={[styles.activeTargetCard, { flexDirection: 'column', marginBottom: RFValue(10), paddingBottom: RFValue(10) }]}>
                                <View style={[styles.targetInfo, { flex: 0, width: '100%', marginBottom: RFValue(0) }]}>
                                    <CustomText style={[styles.targetTitle, { textAlign: 'center' }]}>
                                        {activeCharity.name}
                                    </CustomText>
                                    <CustomText style={[styles.targetDesc, { textAlign: 'center' }]}>
                                        {formatDescription(activeCharity.description)}
                                    </CustomText>
                                </View>

                                <View style={[styles.gaugeContainer, { alignItems: 'center', width: '100%', marginTop: 0, marginBottom: 0 }]}>
                                    <View style={{ width: RFValue(200), height: RFValue(40), justifyContent: 'center' }}>
                                        <Image source={require('../../../assets/gauge.png')} style={{ position: 'absolute', width: '100%', height: '100%' }} resizeMode="contain" />
                                        <View style={{
                                            position: 'absolute',
                                            left: '6%',
                                            top: '37%',
                                            height: '25%',
                                            width: `${Math.min((remainJelling / activeCharity.targetAmount) * 100, 100)}%`,
                                            maxWidth: '90%',
                                            backgroundColor: '#7DF39D',
                                            zIndex: 1
                                        }} />
                                        <CustomText style={{ position: 'absolute', width: '100%', textAlign: 'center', fontSize: RFValue(14), fontWeight: '900', color: '#111', zIndex: 2 }}>
                                            {remainJelling} / {activeCharity.targetAmount}
                                        </CustomText>
                                    </View>
                                </View>
                            </View>

                            <TouchableOpacity
                                style={[
                                    styles.gachaButton,
                                    { backgroundColor: '#A3E635' },
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
                                    가챠!
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
    safeArea: { flex: 1, backgroundColor: '#ECFCCB' },
    flex: { flex: 1 },
    loaderContainer: { justifyContent: 'center', alignItems: 'center' },
    header: { padding: RFValue(16), backgroundColor: '#FFF', alignItems: 'center' },
    headerTitle: { fontSize: RFValue(18), fontWeight: 'bold', color: '#111' },
    container: { padding: RFValue(14) },
    balanceHeader: {
        backgroundColor: 'transparent',
        borderRadius: RFValue(16),
        padding: RFValue(16),
        alignItems: 'center',
        paddingBottom: 0,
        marginBottom: RFValue(40),
        zIndex: 1,
    },
    balanceLabel: {
        fontSize: RFValue(14),
        color: '#6B7280',
        marginBottom: RFValue(8),
    },
    balanceValue: {
        fontSize: RFValue(28),
        fontWeight: 'bold',
        color: '#111111',
    },
    charitySelectionBox: {
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
        padding: RFValue(16),
        borderRadius: RFValue(12),
        marginBottom: RFValue(12),
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 2 },
        shadowOpacity: 0.04,
        shadowRadius: 4,
        elevation: 1,
    },
    donationTargetBox: {
        backgroundColor: '#FFF',
        borderRadius: RFValue(20),
        padding: RFValue(16),
        marginTop: RFValue(20),
        marginBottom: RFValue(12),
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
        padding: RFValue(12),
        borderRadius: RFValue(16),
        marginBottom: RFValue(12),
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 2 },
        shadowOpacity: 0.04,
        shadowRadius: 4,
        elevation: 1,
    },
    targetEmoji: { fontSize: RFValue(36), marginRight: RFValue(16) },
    targetInfo: { width: '100%', alignItems: 'center' },
    targetTitle: {
        fontSize: RFValue(15),
        fontWeight: 'bold',
        color: '#111',
        marginBottom: RFValue(12),
        textAlign: 'center',
    },
    targetDesc: {
        fontSize: RFValue(10.4),
        color: '#6B7280',
        lineHeight: RFValue(16),
        textAlign: 'center',
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
        paddingVertical: RFValue(12),
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
