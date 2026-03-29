import React, { useState, useEffect } from 'react';
import { View, StyleSheet, TouchableOpacity, SafeAreaView, ScrollView, Alert, ActivityIndicator, Modal, FlatList } from 'react-native';
import { scale, verticalScale } from 'react-native-size-matters';
import CustomText from '../../../components/common/CustomText';
import api from '../../../api/axios';

const CardProductScreen = ({ navigation }) => {
    const [products, setProducts] = useState([]);
    const [loading, setLoading] = useState(true);
    const [isModalVisible, setIsModalVisible] = useState(false);
    const [selectedProduct, setSelectedProduct] = useState(null);
    const [accounts, setAccounts] = useState([]);
    const [selectedAccountNo, setSelectedAccountNo] = useState('');
    const [selectedDate, setSelectedDate] = useState('7'); // 기본 일요일(7)
    const [issuing, setIssuing] = useState(false);

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

    const fetchAccounts = async () => {
        try {
            const res = await api.get('/bank/accounts/me');
            const data = res.data?.data?.accounts || [];
            setAccounts(data);
            if (data.length > 0) {
                const primary = data.find(a => a.isPrimary) || data[0];
                setSelectedAccountNo(primary.accountNumber);
            }
        } catch (err) {
            console.error('Fetch Accounts Error', err);
            Alert.alert('오류', '계좌 정보를 불러오지 못했습니다.');
        }
    };

    const handleOpenModal = (product) => {
        setSelectedProduct(product);
        fetchAccounts();
        setIsModalVisible(true);
    };

    const handleIssueCard = async () => {
        if (!selectedAccountNo) {
            Alert.alert('알림', '출금 계좌를 선택해 주세요.');
            return;
        }

        setIssuing(true);
        try {
            await api.post('/bank/cards', {
                cardProductId: selectedProduct.id || selectedProduct.cardProductId,
                withdrawalAccountNo: selectedAccountNo,
                withdrawalDate: selectedDate
            });
            setIsModalVisible(false);
            Alert.alert('발급 완료', '카드 발급이 완료되었습니다!', [
                { text: '확인', onPress: () => navigation.goBack() }
            ]);
        } catch (err) {
            console.error('Card Issue Error', err);
            const errorMsg = err.response?.data?.message || '카드 발급 중 문제가 발생했습니다.';
            Alert.alert('발급 실패', errorMsg);
        } finally {
            setIssuing(false);
        }
    };

    const dayNames = ['월', '화', '수', '목', '금', '토', '일'];

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
                                <CustomText style={styles.productName}>{item.cardName || 'AKKU 스마일 카드'}</CustomText>
                                <CustomText style={styles.productDesc}>{item.cardDescription || '편의점 10% 적립 혜택!'}</CustomText>
                            </View>
                            <TouchableOpacity style={styles.issueButton} onPress={() => handleOpenModal(item)}>
                                <CustomText style={styles.issueButtonText}>발급</CustomText>
                            </TouchableOpacity>
                        </View>
                    ))
                )}
            </ScrollView>

            <Modal
                transparent={true}
                visible={isModalVisible}
                animationType="slide"
                onRequestClose={() => setIsModalVisible(false)}
            >
                <View style={styles.modalOverlay}>
                    <View style={styles.modalContent}>
                        <TouchableOpacity
                            style={styles.closeBtn}
                            onPress={() => setIsModalVisible(false)}
                        >
                            <CustomText style={styles.closeBtnText}>✕</CustomText>
                        </TouchableOpacity>

                        <CustomText style={styles.modalTitle}>카드 발급 설정</CustomText>

                        <ScrollView showsVerticalScrollIndicator={false} style={styles.modalScroll}>
                            {selectedProduct && (
                                <View style={styles.cardPreview}>
                                    <View style={[styles.cardGraphic, { backgroundColor: '#3B82F6' }]}>
                                        <CustomText style={styles.cardGraphicText}>{selectedProduct.cardName}</CustomText>
                                        <View style={styles.cardChip} />
                                    </View>
                                    <CustomText style={styles.selectedProductName}>{selectedProduct.cardName}</CustomText>
                                    <CustomText style={styles.selectedProductDesc}>{selectedProduct.cardDescription}</CustomText>

                                    {/* 카드 혜택 상세 표시 */}
                                    {selectedProduct.cardBenefitInfo && (
                                        <View style={styles.benefitsBox}>
                                            {(() => {
                                                try {
                                                    const benefits = JSON.parse(selectedProduct.cardBenefitInfo);
                                                    if (Array.isArray(benefits)) {
                                                        return benefits.map((b, i) => (
                                                            <View key={i} style={styles.benefitRow}>
                                                                <CustomText style={styles.benefitName}>
                                                                    {b.categoryName || b.benefitName || Object.values(b)[0]}
                                                                </CustomText>
                                                                <CustomText style={styles.benefitDetail}>
                                                                    {b.discountRate ? `${b.discountRate}% 할인` : (b.benefitType || '')}
                                                                </CustomText>
                                                            </View>
                                                        ));
                                                    }
                                                } catch (e) {
                                                    return <CustomText style={styles.benefitDetail}>{selectedProduct.cardBenefitInfo}</CustomText>;
                                                }
                                                return null;
                                            })()}
                                        </View>
                                    )}
                                </View>
                            )}


                            <View style={styles.configSection}>
                                <CustomText style={styles.sectionLabel}>출금 계좌 선택</CustomText>
                                {accounts.map((item) => (
                                    <TouchableOpacity
                                        key={item.accountNumber}
                                        style={[styles.accountItem, selectedAccountNo === item.accountNumber && styles.accountItemActive]}
                                        onPress={() => setSelectedAccountNo(item.accountNumber)}
                                    >
                                        <View>
                                            <CustomText style={styles.accountName}>{item.accountName}</CustomText>
                                            <CustomText style={styles.accountNo}>{item.accountNumber}</CustomText>
                                        </View>
                                        <CustomText style={styles.accountBalance}>{item.balance?.toLocaleString()}원</CustomText>
                                    </TouchableOpacity>
                                ))}
                            </View>
                        </ScrollView>

                        <TouchableOpacity
                            style={[styles.submitBtn, issuing && { opacity: 0.7 }]}
                            onPress={handleIssueCard}
                            disabled={issuing}
                        >
                            {issuing ? (
                                <ActivityIndicator color="#FFF" />
                            ) : (
                                <CustomText style={styles.submitBtnText}>카드 발급하기</CustomText>
                            )}
                        </TouchableOpacity>
                    </View>
                </View>
            </Modal>
        </SafeAreaView>
    );
};

const styles = StyleSheet.create({
    safeArea: { flex: 1, backgroundColor: '#F9FAFB' },
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
    issueButtonText: { color: '#FFFFFF', fontWeight: 'bold', fontSize: scale(14) },

    // Modal Styles
    modalOverlay: { flex: 1, backgroundColor: 'rgba(0,0,0,0.5)', justifyContent: 'flex-end' },
    modalContent: { backgroundColor: '#FFFFFF', borderTopLeftRadius: scale(32), borderTopRightRadius: scale(32), padding: scale(24), minHeight: '80%' },
    closeBtn: { alignSelf: 'flex-end', padding: scale(8) },
    closeBtnText: { fontSize: scale(20), color: '#9CA3AF' },
    modalTitle: { fontSize: scale(20), fontWeight: '900', color: '#111', marginBottom: verticalScale(24), textAlign: 'center' },
    modalScroll: { flex: 1, marginBottom: verticalScale(16) },

    cardPreview: { alignItems: 'center', marginBottom: verticalScale(24) },
    cardGraphic: { width: scale(180), height: scale(110), borderRadius: scale(12), padding: scale(16), justifyContent: 'flex-end', elevation: 4, shadowColor: '#000', shadowOffset: { width: 0, height: 4 }, shadowOpacity: 0.2, shadowRadius: 8 },
    cardGraphicText: { color: '#FFF', fontSize: scale(14), fontWeight: 'bold' },
    cardChip: { width: scale(28), height: scale(20), backgroundColor: '#FCD34D', borderRadius: scale(4), marginBottom: verticalScale(8) },
    selectedProductName: { fontSize: scale(18), fontWeight: 'bold', color: '#111', marginTop: verticalScale(16) },
    selectedProductDesc: { fontSize: scale(13), color: '#6B7280', marginTop: verticalScale(4) },

    benefitsBox: { width: '100%', backgroundColor: '#F9FAFB', borderRadius: scale(12), padding: scale(16), marginTop: verticalScale(16) },
    benefitRow: { flexDirection: 'row', justifyContent: 'space-between', marginBottom: verticalScale(4) },
    benefitName: { fontSize: scale(13), color: '#374151', fontWeight: 'bold' },
    benefitDetail: { fontSize: scale(13), color: '#3B82F6', fontWeight: 'bold' },

    configSection: { marginTop: verticalScale(24) },
    sectionLabel: { fontSize: scale(15), fontWeight: 'bold', color: '#111', marginBottom: verticalScale(12) },
    daySelector: { flexDirection: 'row', justifyContent: 'space-between' },
    dayItem: { width: scale(40), height: scale(40), borderRadius: scale(12), backgroundColor: '#F9FAFB', alignItems: 'center', justifyContent: 'center' },
    dayItemActive: { backgroundColor: '#3B82F6' },
    dayText: { fontSize: scale(14), fontWeight: 'bold', color: '#6B7280' },
    dayTextActive: { color: '#FFFFFF' },

    accountItem: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', padding: scale(16), borderRadius: scale(16), backgroundColor: '#F9FAFB', marginBottom: verticalScale(8), borderWidth: 2, borderColor: 'transparent' },
    accountItemActive: { borderColor: '#3B82F6', backgroundColor: '#EFF6FF' },
    accountName: { fontSize: scale(15), fontWeight: 'bold', color: '#111' },
    accountNo: { fontSize: scale(12), color: '#6B7280' },
    accountBalance: { fontSize: scale(14), fontWeight: 'bold', color: '#111' },

    submitBtn: { backgroundColor: '#3B82F6', paddingVertical: verticalScale(16), borderRadius: scale(16), alignItems: 'center', marginTop: verticalScale(24) },
    submitBtnText: { color: '#FFFFFF', fontSize: scale(16), fontWeight: 'bold' }
});

export default CardProductScreen;
