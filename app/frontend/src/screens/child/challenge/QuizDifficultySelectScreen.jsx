import React from 'react';
import { Image, SafeAreaView, StyleSheet, TouchableOpacity, View } from 'react-native';
import { scale, verticalScale } from 'react-native-size-matters';
import CustomText from '../../../components/common/CustomText';

const DIFFICULTIES = [
    {
        label: '\uD558',
        value: 'easy',
        title: '\uC26C\uC6B4 \uB09C\uC774\uB3C4',
        description: '\uAE30\uCD08 \uAE08\uC735 \uAC1C\uB150\uBD80\uD130 \uCC28\uADFC\uCC28\uADFC \uD480\uC5B4\uC694.',
        image: require('../../../assets/croco/kids_akku.png'),
        badgeBackgroundColor: '#DCFCE7',
        badgeTextColor: '#15803D',
    },
    {
        label: '\uC911',
        value: 'medium',
        title: '\uBCF4\uD1B5 \uB09C\uC774\uB3C4',
        description: '\uC870\uAE08 \uB354 \uC0DD\uAC01\uC774 \uD544\uC694\uD55C \uBB38\uC81C\uB97C \uD480\uC5B4\uC694.',
        image: require('../../../assets/croco/students_akku.png'),
        badgeBackgroundColor: '#FEF3C7',
        badgeTextColor: '#B45309',
    },
    {
        label: '\uC0C1',
        value: 'hard',
        title: '\uC5B4\uB824\uC6B4 \uB09C\uC774\uB3C4',
        description: '\uB3C4\uC804\uC801\uC778 \uAE08\uC735 \uD034\uC988\uC5D0 \uB3C4\uC804\uD574\uC694.',
        image: require('../../../assets/croco/adult_akku.png'),
        badgeBackgroundColor: '#FEE2E2',
        badgeTextColor: '#B91C1C',
    },
];

const QuizDifficultySelectScreen = ({ navigation }) => {
    const handleSelectDifficulty = (difficulty) => {
        navigation.navigate('QuizScreen', { difficulty });
    };

    return (
        <SafeAreaView style={styles.safeArea}>
            <View style={styles.header}>
                <TouchableOpacity style={styles.backButton} onPress={() => navigation.goBack()}>
                    <CustomText style={styles.backButtonText}>{'<'}</CustomText>
                </TouchableOpacity>
                <CustomText style={styles.headerTitle}>{'\uD034\uC988 \uB09C\uC774\uB3C4 \uC120\uD0DD'}</CustomText>
                <View style={styles.headerSpacer} />
            </View>

            <View style={styles.container}>
                <View style={styles.titleBox}>
                    <CustomText style={styles.title}>{'\uC624\uB298\uC758 \uAE08\uC735 \uD034\uC988'}</CustomText>
                    <CustomText style={styles.subtitle}>
                        {'\uC6D0\uD558\uB294 \uB09C\uC774\uB3C4\uB97C \uC120\uD0DD\uD558\uACE0 \uD034\uC988\uB97C \uC2DC\uC791\uD558\uC138\uC694.'}
                    </CustomText>
                </View>

                {DIFFICULTIES.map((item) => (
                    <TouchableOpacity
                        key={item.value}
                        style={styles.card}
                        onPress={() => handleSelectDifficulty(item.value)}
                        activeOpacity={0.85}
                    >
                        <View style={styles.cardTopRow}>
                            <View style={styles.cardTextArea}>
                                <View style={styles.cardTitleRow}>
                                    <View style={[styles.badge, { backgroundColor: item.badgeBackgroundColor }]}>
                                        <CustomText style={[styles.badgeText, { color: item.badgeTextColor }]}>{item.label}</CustomText>
                                    </View>
                                    <CustomText style={styles.cardTitle}>{item.title}</CustomText>
                                </View>
                                <CustomText style={styles.cardDescription}>
                                    {item.description}
                                </CustomText>
                            </View>
                            <Image source={item.image} style={styles.cardImage} resizeMode="contain" />
                        </View>
                    </TouchableOpacity>
                ))}
            </View>
        </SafeAreaView>
    );
};

const styles = StyleSheet.create({
    safeArea: { flex: 1, backgroundColor: '#FFFFFF' },
    header: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        paddingHorizontal: scale(16),
        paddingVertical: verticalScale(16),
        backgroundColor: '#FFFFFF',
        borderBottomWidth: 1,
        borderBottomColor: '#DADDE3',
    },
    backButton: { width: scale(32), height: scale(32), justifyContent: 'center' },
    backButtonText: { fontSize: scale(22), fontWeight: 'bold', color: '#111' },
    headerTitle: { fontSize: scale(18), fontWeight: 'bold', color: '#111' },
    headerSpacer: { width: scale(32) },
    container: { flex: 1, paddingHorizontal: scale(20), paddingTop: verticalScale(28), backgroundColor: '#FFFFFF' },
    titleBox: { marginBottom: verticalScale(24) },
    title: { fontSize: scale(26), fontWeight: 'bold', color: '#1F2937', marginBottom: verticalScale(8) },
    subtitle: { fontSize: scale(14), color: '#6B7280', lineHeight: scale(22) },
    card: {
        backgroundColor: '#F8F9FB',
        borderWidth: 1,
        borderColor: '#D1D5DB',
        borderRadius: scale(24),
        paddingHorizontal: scale(20),
        paddingVertical: verticalScale(18),
        marginBottom: verticalScale(16),
        shadowColor: '#9CA3AF',
        shadowOffset: { width: 0, height: verticalScale(4) },
        shadowOpacity: 0.08,
        shadowRadius: scale(10),
        elevation: 3,
    },
    cardTopRow: { flexDirection: 'row', alignItems: 'center' },
    cardTextArea: { flex: 1, paddingRight: scale(12) },
    cardTitleRow: { flexDirection: 'row', alignItems: 'center', marginBottom: verticalScale(10) },
    badge: {
        width: scale(34),
        height: scale(34),
        borderRadius: scale(17),
        justifyContent: 'center',
        alignItems: 'center',
        marginRight: scale(10),
    },
    badgeText: { fontSize: scale(16), fontWeight: 'bold' },
    cardTitle: { fontSize: scale(20), fontWeight: 'bold', color: '#111827' },
    cardDescription: { fontSize: scale(14), lineHeight: scale(22), color: '#6B7280' },
    cardImage: {
        width: scale(88),
        height: scale(88),
    },
});

export default QuizDifficultySelectScreen;
