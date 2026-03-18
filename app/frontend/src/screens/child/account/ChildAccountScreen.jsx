import React from 'react';
import { View, StyleSheet, SafeAreaView, ScrollView, TouchableOpacity } from 'react-native';
import { RFValue } from 'react-native-responsive-fontsize';
import CustomText from '../../../components/common/CustomText';

const ChildAccountScreen = ({ navigation }) => {
    return (
        <SafeAreaView style={styles.safeArea}>
            <ScrollView contentContainerStyle={styles.container}>
                <View style={styles.balanceCard}>
                    <CustomText style={styles.balanceLabel}>내 지갑</CustomText>
                    <CustomText style={styles.balanceValue}>140,000 원</CustomText>
                    <View style={styles.actionRow}>
                        <TouchableOpacity style={styles.actionBtn} onPress={() => navigation.navigate('Transfer')}>
                            <CustomText style={styles.actionBtnText}>송금</CustomText>
                        </TouchableOpacity>
                        <TouchableOpacity style={[styles.actionBtn, { backgroundColor: '#111' }]} onPress={() => navigation.navigate('TransactionCalendar')}>
                            <CustomText style={[styles.actionBtnText, { color: '#FFF' }]}>내역보기</CustomText>
                        </TouchableOpacity>
                    </View>
                </View>

                <CustomText style={styles.sectionTitle}>최근 결제 내역</CustomText>

                <View style={styles.historyCard}>
                    <View style={styles.historyRow}>
                        <View>
                            <CustomText style={styles.historyTitle}>CU편의점</CustomText>
                            <CustomText style={styles.historyDate}>오늘 14:30</CustomText>
                        </View>
                        <CustomText style={styles.historyAmount}>-3,500원</CustomText>
                    </View>
                    <View style={styles.historyRow}>
                        <View>
                            <CustomText style={styles.historyTitle}>아빠 용돈</CustomText>
                            <CustomText style={styles.historyDate}>어제 10:00</CustomText>
                        </View>
                        <CustomText style={[styles.historyAmount, { color: '#3B82F6' }]}>+10,000원</CustomText>
                    </View>
                </View>
                <TouchableOpacity style={styles.payButton} onPress={() => navigation.navigate('Payment')}>
                    <CustomText style={styles.payButtonText}>현장결제 (QR)</CustomText>
                </TouchableOpacity>
            </ScrollView>
        </SafeAreaView>
    );
};

const styles = StyleSheet.create({
    safeArea: { flex: 1, backgroundColor: '#F3F4F6' },
    container: { padding: RFValue(20) },
    balanceCard: { backgroundColor: '#FFF', borderRadius: RFValue(16), padding: RFValue(24), marginBottom: RFValue(30) },
    balanceLabel: { fontSize: RFValue(15), color: '#6B7280', marginBottom: RFValue(8) },
    balanceValue: { fontSize: RFValue(32), fontWeight: 'bold', color: '#111', marginBottom: RFValue(20) },
    actionRow: { flexDirection: 'row', gap: RFValue(12) },
    actionBtn: { flex: 1, backgroundColor: '#F3F4F6', padding: RFValue(14), borderRadius: RFValue(12), alignItems: 'center' },
    actionBtnText: { fontSize: RFValue(15), fontWeight: 'bold', color: '#111' },
    sectionTitle: { fontSize: RFValue(18), fontWeight: 'bold', color: '#111', marginBottom: RFValue(16) },
    historyCard: { backgroundColor: '#FFF', borderRadius: RFValue(16), padding: RFValue(20), marginBottom: RFValue(20) },
    historyRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingVertical: RFValue(12), borderBottomWidth: 1, borderBottomColor: '#F3F4F6' },
    historyTitle: { fontSize: RFValue(16), fontWeight: 'bold', color: '#111', marginBottom: RFValue(4) },
    historyDate: { fontSize: RFValue(13), color: '#9CA3AF' },
    historyAmount: { fontSize: RFValue(16), fontWeight: 'bold', color: '#111' },
    payButton: { backgroundColor: '#3B82F6', padding: RFValue(16), borderRadius: RFValue(12), alignItems: 'center' },
    payButtonText: { color: '#FFF', fontWeight: 'bold', fontSize: RFValue(16) }
});

export default ChildAccountScreen;
