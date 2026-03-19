import React from 'react';
import { View, StyleSheet, SafeAreaView, ScrollView, TouchableOpacity } from 'react-native';
import { RFValue } from 'react-native-responsive-fontsize';
import CustomText from '../../../components/common/CustomText';

const ChallengeScreen = ({ navigation }) => {
    return (
        <SafeAreaView style={styles.safeArea}>
            <ScrollView contentContainerStyle={styles.container}>
                <CustomText style={styles.title}>내 챌린지</CustomText>

                <View style={styles.card}>
                    <CustomText style={styles.cardTitle}>주간 금융 퀴즈</CustomText>
                    <CustomText style={styles.cardDesc}>퀴즈를 풀고 랜덤 젤링을 받아보세요!</CustomText>
                    <TouchableOpacity style={styles.button} onPress={() => navigation.navigate('QuizScreen')}>
                        <CustomText style={styles.buttonText}>퀴즈 풀기</CustomText>
                    </TouchableOpacity>
                </View>

                <View style={styles.card}>
                    <CustomText style={styles.cardTitle}>용돈 챌린지 제안</CustomText>
                    <CustomText style={styles.cardDesc}>부모님께 새로운 용돈 챌린지를 제안해보세요.</CustomText>
                    <TouchableOpacity style={[styles.button, { backgroundColor: '#A3E635' }]} onPress={() => navigation.navigate('ChallengePropose')}>
                        <CustomText style={styles.buttonText}>제안하기</CustomText>
                    </TouchableOpacity>
                </View>

                <View style={styles.card}>
                    <CustomText style={styles.cardTitle}>출석체크</CustomText>
                    <CustomText style={styles.cardDesc}>매일 꾸준히 들어오면 선물이 !</CustomText>
                    <TouchableOpacity style={[styles.button, { backgroundColor: '#A3E635' }]} onPress={() => navigation.navigate('AttendanceScreen')}>
                        <CustomText style={styles.buttonText}>출석하러 가기</CustomText>
                    </TouchableOpacity>
                </View>
            </ScrollView>
        </SafeAreaView>
    );
};

const styles = StyleSheet.create({
    safeArea: { flex: 1, backgroundColor: '#F3F4F6' },
    container: { padding: RFValue(20) },
    title: { fontSize: RFValue(24), fontWeight: 'bold', marginBottom: RFValue(20), color: '#111' },
    card: { backgroundColor: '#FFF', borderRadius: RFValue(16), padding: RFValue(20), marginBottom: RFValue(16) },
    cardTitle: { fontSize: RFValue(18), fontWeight: 'bold', color: '#111', marginBottom: RFValue(8) },
    cardDesc: { fontSize: RFValue(14), color: '#6B7280', marginBottom: RFValue(16) },
    button: { backgroundColor: '#A3E635', padding: RFValue(12), borderRadius: RFValue(8), alignItems: 'center' },
    buttonText: { fontSize: RFValue(14), fontWeight: 'bold', color: '#FFF' }
});

export default ChallengeScreen;
