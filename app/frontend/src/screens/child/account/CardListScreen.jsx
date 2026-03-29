import React, { useState, useCallback } from 'react';
import { View, StyleSheet, TouchableOpacity, SafeAreaView, ScrollView, Alert } from 'react-native';
import { scale, verticalScale } from 'react-native-size-matters';
import { useFocusEffect } from '@react-navigation/native';
import CustomText from '../../../components/common/CustomText';
import api from '../../../api/axios';

const CardListScreen = ({ navigation }) => {
    const [myCards, setMyCards] = useState([]);
    const [loading, setLoading] = useState(true);

    useFocusEffect(
        useCallback(() => {
            fetchMyCards();
        }, [])
    );

    const fetchMyCards = async () => {
        try {
            const res = await api.get('/bank/cards/my');
            const data = res.data?.data;
            if (data) {
                if (data.cards) {
                    setMyCards(data.cards);
                } else if (Array.isArray(data)) {
                    setMyCards(data);
                }
            }
        } catch (err) {
            console.error('Fetch My Cards Error', err);
        } finally {
            setLoading(false);
        }
    };

    return (
        <SafeAreaView style={styles.safeArea}>
            <View style={styles.header}>
                <TouchableOpacity style={styles.backButton} onPress={() => navigation.goBack()}>
                    <CustomText style={styles.backButtonText}>←</CustomText>
                </TouchableOpacity>
                <CustomText style={styles.headerTitle}>내 카드 목록</CustomText>
                <View style={{ width: scale(32) }} />
            </View>

            <ScrollView contentContainerStyle={styles.container}>
                <View style={styles.topSection}>
                    <CustomText style={styles.pageTitle}>내 지갑 속 카드</CustomText>
                    <CustomText style={styles.pageSubtitle}>내가 보유한 카드를 확인해보세요.</CustomText>
                </View>

                {loading ? (
                    <CustomText style={styles.emptyText}>불러오는 중...</CustomText>
                ) : myCards.length === 0 ? (
                    <View style={styles.emptyState}>
                        <CustomText style={styles.emptyText}>아직 발급받은 카드가 없어요!</CustomText>
                        <CustomText style={styles.emptySubText}>새로운 카드를 발급받아 보세요.</CustomText>
                    </View>
                ) : (
                    myCards.map((card, idx) => (
                        <View key={idx} style={styles.cardItem}>
                            <View style={[styles.cardGraphic, { backgroundColor: card.color || '#3B82F6' }]}>
                                <CustomText style={styles.cardGraphicText}>{card.cardName || 'AKKU 카드'}</CustomText>
                                <CustomText style={styles.cardNumber}>**** **** **** {card.cardNo?.slice(-4) || '1234'}</CustomText>
                            </View>
                            <View style={styles.cardInfo}>
                                <CustomText style={styles.cardName}>{card.cardName || '기본 카드'}</CustomText>
                                <CustomText style={styles.cardDesc}>결제 계좌: {card.withdrawalAccountNo || '연동됨'}</CustomText>
                            </View>
                        </View>
                    ))
                )}
            </ScrollView>

            <View style={styles.footer}>
                <TouchableOpacity style={styles.mainButton} onPress={() => navigation.navigate('CardProductScreen')}>
                    <CustomText style={styles.mainButtonText}>카드 새로 발급받기</CustomText>
                </TouchableOpacity>
            </View>
        </SafeAreaView>
    );
};

const styles = StyleSheet.create({
    safeArea: { flex: 1, backgroundColor: '#F9FAFB' },
    header: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingHorizontal: scale(16), paddingVertical: verticalScale(16) },
    backButton: { width: scale(32), height: scale(32), justifyContent: 'center' },
    backButtonText: { fontSize: scale(22), fontWeight: 'bold', color: '#111' },
    headerTitle: { fontSize: scale(18), fontWeight: 'bold', color: '#111' },
    container: { flexGrow: 1, paddingHorizontal: scale(24), paddingTop: verticalScale(20) },
    topSection: { marginBottom: verticalScale(30) },
    pageTitle: { fontSize: scale(22), fontWeight: '900', color: '#111', marginBottom: verticalScale(8) },
    pageSubtitle: { fontSize: scale(14), color: '#6B7280' },
    emptyState: { alignItems: 'center', marginTop: verticalScale(40) },
    emptyText: { fontSize: scale(16), fontWeight: 'bold', color: '#4B5563', marginBottom: verticalScale(8) },
    emptySubText: { fontSize: scale(14), color: '#9CA3AF' },
    cardItem: { backgroundColor: '#F9FAFB', borderRadius: scale(16), padding: scale(16), marginBottom: verticalScale(16), borderWidth: 1, borderColor: '#F9FAFB' },
    cardGraphic: { height: verticalScale(120), borderRadius: scale(12), padding: scale(16), justifyContent: 'space-between', marginBottom: verticalScale(12) },
    cardGraphicText: { fontSize: scale(16), fontWeight: 'bold', color: '#FFF' },
    cardNumber: { fontSize: scale(14), color: '#FFF', letterSpacing: 2 },
    cardInfo: { paddingHorizontal: scale(4) },
    cardName: { fontSize: scale(16), fontWeight: 'bold', color: '#111', marginBottom: verticalScale(4) },
    cardDesc: { fontSize: scale(13), color: '#6B7280' },
    footer: { padding: scale(16), backgroundColor: '#FFFFFF', borderTopWidth: 1, borderTopColor: '#F9FAFB' },
    mainButton: { backgroundColor: '#3B82F6', paddingVertical: verticalScale(16), borderRadius: scale(16), alignItems: 'center' },
    mainButtonText: { fontSize: scale(16), fontWeight: 'bold', color: '#FFF' }
});

export default CardListScreen;
