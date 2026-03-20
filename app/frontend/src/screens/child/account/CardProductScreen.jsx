import React, { useState, useEffect } from 'react';
import { View, StyleSheet, TouchableOpacity, SafeAreaView, ScrollView, Alert, ActivityIndicator } from 'react-native';
import { scale, verticalScale } from 'react-native-size-matters';
import CustomText from '../../../components/common/CustomText';
import api from '../../../api/axios';

const CardProductScreen = ({ navigation }) => {
    const [products, setProducts] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        fetchProducts();
    }, []);

    const fetchProducts = async () => {
        try {
            const res = await api.get('/bank/cards');
            const data = res.data?.data;
            if (data && data.products) {
                setProducts(data.products);
            } else if (Array.isArray(data)) {
                setProducts(data);
            }
        } catch (err) {
            console.error('Fetch Card Products Error', err);
        } finally {
            setLoading(false);
        }
    };

    const handleIssueCard = (productId) => {
        Alert.alert(
            '카드 발급',
            '이 카드를 발급하시겠습니까?',
            [
                { text: '취소', style: 'cancel' },
                {
                    text: '발급하기',
                    onPress: async () => {
                        try {
                            await api.post('/bank/cards', { productId });
                            Alert.alert('완료', '카드 발급이 완료되었습니다!', [
                                { text: '확인', onPress: () => navigation.goBack() }
                            ]);
                        } catch (err) {
                            console.error('Card Issue Error', err);
                            Alert.alert('오류', '카드 발급 중 문제가 발생했습니다.');
                        }
                    }
                }
            ]
        );
    };

    return (
        <SafeAreaView style={styles.safeArea}>
            <View style={styles.header}>
                <TouchableOpacity style={styles.backButton} onPress={() => navigation.goBack()}>
                    <CustomText style={styles.backButtonText}>←</CustomText>
                </TouchableOpacity>
                <CustomText style={styles.headerTitle}>카드 상품 안내</CustomText>
                <View style={{ width: scale(32) }} />
            </View>

            <ScrollView contentContainerStyle={styles.container}>
                <View style={styles.topSection}>
                    <CustomText style={styles.pageTitle}>나에게 꼭 맞는 카드</CustomText>
                    <CustomText style={styles.pageSubtitle}>원하는 디자인과 혜택을 선택하세요.</CustomText>
                </View>

                {loading ? (
                    <ActivityIndicator size="large" color="#3B82F6" style={{ marginTop: 40 }} />
                ) : products.length === 0 ? (
                    <CustomText style={styles.emptyText}>현재 발급 가능한 카드가 없습니다.</CustomText>
                ) : (
                    products.map((item, idx) => (
                        <View key={idx} style={styles.productCard}>
                            <View style={styles.productInfo}>
                                <CustomText style={styles.productName}>{item.name || 'AKKU 스마일 카드'}</CustomText>
                                <CustomText style={styles.productDesc}>{item.description || '편의점 10% 적립 혜택!'}</CustomText>
                            </View>
                            <TouchableOpacity style={styles.issueButton} onPress={() => handleIssueCard(item.id || idx)}>
                                <CustomText style={styles.issueButtonText}>발급</CustomText>
                            </TouchableOpacity>
                        </View>
                    ))
                )}
            </ScrollView>
        </SafeAreaView>
    );
};

const styles = StyleSheet.create({
    safeArea: { flex: 1, backgroundColor: '#F3F4F6' },
    header: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingHorizontal: scale(16), paddingVertical: verticalScale(16), backgroundColor: '#FFFFFF' },
    backButton: { width: scale(32), height: scale(32), justifyContent: 'center' },
    backButtonText: { fontSize: scale(22), fontWeight: 'bold', color: '#111' },
    headerTitle: { fontSize: scale(18), fontWeight: 'bold', color: '#111' },
    container: { flexGrow: 1, paddingHorizontal: scale(20), paddingTop: verticalScale(20) },
    topSection: { marginBottom: verticalScale(30) },
    pageTitle: { fontSize: scale(22), fontWeight: '900', color: '#111', marginBottom: verticalScale(8) },
    pageSubtitle: { fontSize: scale(14), color: '#6B7280' },
    emptyText: { textAlign: 'center', fontSize: scale(15), color: '#9CA3AF', marginTop: verticalScale(40) },
    productCard: { flexDirection: 'row', backgroundColor: '#FFFFFF', borderRadius: scale(16), padding: scale(20), marginBottom: verticalScale(16), alignItems: 'center', justifyContent: 'space-between', elevation: 2, shadowColor: '#000', shadowOffset: { width: 0, height: 2 }, shadowOpacity: 0.1, shadowRadius: 8 },
    productInfo: { flex: 1, paddingRight: scale(16) },
    productName: { fontSize: scale(16), fontWeight: 'bold', color: '#111', marginBottom: verticalScale(4) },
    productDesc: { fontSize: scale(13), color: '#6B7280', lineHeight: 18 },
    issueButton: { backgroundColor: '#3B82F6', paddingVertical: verticalScale(8), paddingHorizontal: scale(16), borderRadius: scale(12) },
    issueButtonText: { color: '#FFFFFF', fontWeight: 'bold', fontSize: scale(14) }
});

export default CardProductScreen;
