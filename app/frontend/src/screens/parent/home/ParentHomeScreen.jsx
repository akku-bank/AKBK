import React, { useState, useCallback } from 'react';
import { View, StyleSheet, SafeAreaView, TouchableOpacity, ScrollView, Image, Alert } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { scale, verticalScale } from 'react-native-size-matters';
import CustomText from '../../../components/common/CustomText';
import useAuthStore from '../../../store/useAuthStore';
import api from '../../../api/axios';

const ParentHomeScreen = ({ navigation }) => {
    const { user } = useAuthStore();
    const [childrenData, setChildrenData] = useState([]);
    const [parentBalance, setParentBalance] = useState(0);

    useFocusEffect(
        useCallback(() => {
            const fetchChildren = async () => {
                try {
                    const [homeRes, familyRes] = await Promise.all([
                        api.get('/home/parent'),
                        api.get('/families/members')
                    ]);

                    const homeChildren = homeRes.data?.data?.children || [];
                    const familyMembers = familyRes.data?.data?.members || [];
                    const pBalance = homeRes.data?.data?.parentBalance || 0;

                    const visibleMembersByUserId = new Map(
                        familyMembers
                            .filter(member =>
                                member.role === 'CHILD' &&
                                member.userId
                            )
                            .map(member => [
                                String(member.userId),
                                {
                                    profileId: member.profileId,
                                    accountId: (member.accountIds && member.accountIds.length > 0) ? member.accountIds[0] : null
                                }
                            ])
                    );

                    const mappedChildren = homeChildren
                        .map(child => {
                            const matchedMember = visibleMembersByUserId.get(String(child.childId));
                            if (!matchedMember) {
                                return null;
                            }

                            const result = {
                                id: matchedMember.profileId,
                                childId: child.childId,
                                accountId: matchedMember.accountId,
                                name: child.name,
                                balance: matchedMember.accountId ? (child.balance || 0) : null,
                                bankCode: child.bankCode,
                                accountNumber: child.accountNumber,
                                avatar: require('../../../assets/croco/croco_face.png')
                            };
                            console.log('[ParentHomeScreen] Mapped child:', result);
                            return result;
                        })
                        .filter(Boolean);

                    setChildrenData(mappedChildren);
                    setParentBalance(pBalance);
                } catch (e) {
                    console.error('Parent Home Fetch Error', e);
                    setChildrenData([]);
                }
            };

            fetchChildren();
        }, [])
    );

    const handleDeleteMember = (child) => {
        Alert.alert('가족 분리', `${child.name} 자녀를 가족 그룹에서 제거하시겠습니까?`, [
            { text: '취소', style: 'cancel' },
            {
                text: '제거',
                style: 'destructive',
                onPress: async () => {
                    try {
                        await api.delete(`/families/members/${child.id}`);
                        Alert.alert('완료', '가족 목록에서 제거되었습니다.');
                        setChildrenData(prev => prev.filter(c => c.id !== child.id));
                    } catch (e) {
                        console.error('Delete Member Error', e);
                        Alert.alert('오류', '가족 분리에 실패했습니다.');
                    }
                }
            }
        ]);
    };

    return (
        <SafeAreaView style={styles.safeArea}>
            <View style={styles.header}>
                <CustomText style={styles.headerTitle}>{user?.name || '부모님'}</CustomText>
                <TouchableOpacity onPress={() => navigation.navigate('ParentEditProfile')}>
                    <CustomText style={styles.settingsIcon}>설정</CustomText>
                </TouchableOpacity>
            </View>

            <ScrollView contentContainerStyle={styles.container}>
                <View style={styles.parentBalanceCard}>
                    <CustomText style={styles.parentBalanceLabel}>내 지갑(연동 계좌) 잔액</CustomText>
                    <CustomText style={styles.parentBalanceValue}>{parentBalance.toLocaleString()}원</CustomText>
                </View>

                <View style={styles.sectionHeader}>
                    <CustomText style={styles.sectionTitle}>우리 아이들</CustomText>
                </View>

                {childrenData.length === 0 ? (
                    <CustomText style={styles.emptyText}>연동된 자녀 계좌가 없습니다.</CustomText>
                ) : childrenData.map(child => (
                    <View key={String(child.id)} style={styles.childCard}>
                        <View style={styles.childInfoRow}>
                            <View style={styles.childAvatarCircle}>
                                <Image source={child.avatar} style={styles.childAvatarImage} resizeMode="contain" />
                            </View>
                            <View style={styles.childTextInfo}>
                                <CustomText style={styles.childName}>{child.name}</CustomText>
                                {child.accountId ? (
                                    <CustomText style={styles.childBalance}>{child.balance.toLocaleString()}원</CustomText>
                                ) : (
                                    <CustomText style={styles.unlinkedText}>계좌 연동을 완료해 주세요</CustomText>
                                )}
                            </View>
                            {child.accountId && (
                                <TouchableOpacity style={styles.historyBtn} onPress={() => navigation.navigate('ParentHistoryScreen', { child })}>
                                    <CustomText style={styles.historyBtnText}>내역조회</CustomText>
                                </TouchableOpacity>
                            )}
                        </View>

                        <View style={styles.actionRow}>
                            {child.accountId ? (
                                <TouchableOpacity style={styles.actionBtn} onPress={() => navigation.navigate('ParentTransferScreen', { child })}>
                                    <CustomText style={styles.actionBtnText}>용돈 송금</CustomText>
                                </TouchableOpacity>
                            ) : (
                                <TouchableOpacity style={styles.actionBtnPrimary} onPress={() => navigation.navigate('ParentAccountCreate')}>
                                    <CustomText style={styles.actionBtnPrimaryText}>계좌 개설</CustomText>
                                </TouchableOpacity>
                            )}
                            <TouchableOpacity style={styles.actionBtnOutline} onPress={() => navigation.navigate('ParentChildEdit', { child })}>
                                <CustomText style={styles.actionBtnOutlineText}>정보 수정</CustomText>
                            </TouchableOpacity>
                            <TouchableOpacity style={styles.actionBtnDanger} onPress={() => handleDeleteMember(child)}>
                                <CustomText style={styles.actionBtnDangerText}>가족 분리</CustomText>
                            </TouchableOpacity>
                        </View>
                    </View>
                ))}

                <TouchableOpacity style={styles.addBabyCard} onPress={() => navigation.navigate('ParentAccountCreate')}>
                    <CustomText style={styles.addBabyIcon}>+</CustomText>
                    <CustomText style={styles.addBabyText}>자녀 계좌 새로 만들기</CustomText>
                </TouchableOpacity>

                <View style={styles.reportSection}>
                    <CustomText style={styles.sectionTitle}>이번 주 소비 리포트 요약</CustomText>
                    <TouchableOpacity style={styles.reportCard} onPress={() => navigation.navigate('ParentReportScreen', { childId: childrenData[0]?.childId, childName: childrenData[0]?.name })}>
                        <CustomText style={styles.reportEmoji}>리포트</CustomText>
                        <View style={{ flex: 1 }}>
                            <CustomText style={styles.reportTitle}>
                                {childrenData.length > 0 ? `${childrenData[0].name}의` : '자녀의'} 주간 리포트 도착!
                            </CustomText>
                            <CustomText style={styles.reportDesc}>소비 내역과 변화 추이를 확인해보세요.</CustomText>
                        </View>
                        <CustomText style={styles.arrowIcon}>{'>'}</CustomText>
                    </TouchableOpacity>
                </View>
            </ScrollView>
        </SafeAreaView>
    );
};

const styles = StyleSheet.create({
    safeArea: { flex: 1, backgroundColor: '#F3F4F6' },
    header: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        paddingHorizontal: scale(20),
        paddingVertical: verticalScale(20),
        backgroundColor: '#FFFFFF',
        borderBottomWidth: 1,
        borderBottomColor: '#F3F4F6'
    },
    headerTitle: { fontSize: scale(20), fontWeight: '900', color: '#111' },
    settingsIcon: { fontSize: scale(16), fontWeight: '700', color: '#4B5563' },

    container: { flexGrow: 1, paddingHorizontal: scale(16), paddingTop: verticalScale(24), paddingBottom: verticalScale(40) },

    sectionHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: verticalScale(16) },
    sectionTitle: { fontSize: scale(18), fontWeight: 'bold', color: '#111' },

    parentBalanceCard: {
        backgroundColor: '#3B82F6',
        borderRadius: scale(20),
        padding: scale(20),
        marginBottom: verticalScale(32),
        shadowColor: '#3B82F6',
        shadowOffset: { width: 0, height: 4 },
        shadowOpacity: 0.3,
        shadowRadius: scale(8),
        elevation: 6
    },
    parentBalanceLabel: { fontSize: scale(14), color: '#E0F2FE', marginBottom: verticalScale(8) },
    parentBalanceValue: { fontSize: scale(28), fontWeight: '900', color: '#FFFFFF' },

    emptyText: { textAlign: 'center', marginTop: 20, color: '#9CA3AF' },

    childCard: {
        backgroundColor: '#FFFFFF',
        borderRadius: scale(20),
        padding: scale(20),
        marginBottom: verticalScale(16),
        shadowColor: '#000',
        shadowOffset: { width: 0, height: verticalScale(2) },
        shadowOpacity: 0.05,
        shadowRadius: scale(4),
        elevation: 2
    },
    childInfoRow: { flexDirection: 'row', alignItems: 'center', marginBottom: verticalScale(20) },
    childAvatarCircle: {
        width: scale(50),
        height: scale(50),
        borderRadius: scale(25),
        backgroundColor: '#E5E7EB',
        justifyContent: 'center',
        alignItems: 'center',
        marginRight: scale(16),
        overflow: 'hidden'
    },
    childAvatarImage: { width: '80%', height: '80%' },
    childTextInfo: { flex: 1 },
    childName: { fontSize: scale(16), fontWeight: 'bold', color: '#4B5563', marginBottom: verticalScale(4) },
    childBalance: { fontSize: scale(20), fontWeight: '900', color: '#111' },
    historyBtn: { backgroundColor: '#F3F4F6', paddingHorizontal: scale(12), paddingVertical: verticalScale(8), borderRadius: scale(8) },
    historyBtnText: { fontSize: scale(12), fontWeight: 'bold', color: '#4B5563' },

    actionRow: { flexDirection: 'row', gap: scale(8), marginTop: verticalScale(4) },
    actionBtn: { flex: 1.5, backgroundColor: '#ECFCCB', paddingVertical: verticalScale(10), borderRadius: scale(8), alignItems: 'center' },
    actionBtnText: { fontSize: scale(13), fontWeight: 'bold', color: '#4D7C0F' },
    actionBtnOutline: { flex: 1, backgroundColor: '#F3F4F6', paddingVertical: verticalScale(10), borderRadius: scale(8), alignItems: 'center' },
    actionBtnOutlineText: { fontSize: scale(13), fontWeight: 'bold', color: '#4B5563' },
    actionBtnDanger: { flex: 1, backgroundColor: '#FEE2E2', paddingVertical: verticalScale(10), borderRadius: scale(8), alignItems: 'center' },
    actionBtnDangerText: { fontSize: scale(13), fontWeight: 'bold', color: '#DC2626' },
    unlinkedText: { fontSize: scale(13), color: '#EF4444', fontWeight: 'bold' },
    actionBtnPrimary: { flex: 1.5, backgroundColor: '#3B82F6', paddingVertical: verticalScale(10), borderRadius: scale(8), alignItems: 'center' },
    actionBtnPrimaryText: { fontSize: scale(13), fontWeight: 'bold', color: '#FFFFFF' },

    addBabyCard: {
        flexDirection: 'row',
        alignItems: 'center',
        backgroundColor: '#FFFFFF',
        borderRadius: scale(16),
        padding: scale(20),
        marginBottom: verticalScale(32),
        borderStyle: 'dashed',
        borderWidth: 2,
        borderColor: '#D1D5DB'
    },
    addBabyIcon: { fontSize: scale(24), fontWeight: 'bold', color: '#9CA3AF', marginRight: scale(16) },
    addBabyText: { fontSize: scale(15), fontWeight: 'bold', color: '#6B7280' },

    reportSection: { marginBottom: verticalScale(20) },
    reportCard: { flexDirection: 'row', alignItems: 'center', backgroundColor: '#FFFFFF', padding: scale(20), borderRadius: scale(16), marginTop: verticalScale(12) },
    reportEmoji: { fontSize: scale(16), fontWeight: '700', color: '#4B5563', marginRight: scale(16) },
    reportTitle: { fontSize: scale(15), fontWeight: 'bold', color: '#111', marginBottom: verticalScale(4) },
    reportDesc: { fontSize: scale(13), color: '#6B7280' },
    arrowIcon: { fontSize: scale(18), color: '#9CA3AF' }
});

export default ParentHomeScreen;
