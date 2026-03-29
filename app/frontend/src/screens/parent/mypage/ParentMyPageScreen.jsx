import React, { useCallback } from 'react';
import { View, StyleSheet, TouchableOpacity, ScrollView, SafeAreaView, Switch, Alert, Image } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { scale, verticalScale } from 'react-native-size-matters';
import CustomText from '../../../components/common/CustomText';
import useAuthStore from '../../../store/useAuthStore';
import api from '../../../api/axios';

const ParentMyPageScreen = ({ navigation }) => {
    const { user, setUser, logout } = useAuthStore();

    useFocusEffect(
        useCallback(() => {
            const fetchProfile = async () => {
                try {
                    const res = await api.get('/users/me');
                    const profile = res.data?.data;
                    if (profile) {
                        setUser({
                            ...user,
                            id: profile.userId,
                            role: profile.role,
                            name: profile.name,
                            familyId: profile.familyId,
                            level: profile.level
                        });
                    }
                } catch (e) {
                    console.error('Profile Fetch Error', e);
                }
            };
            fetchProfile();
        }, [])
    );

    const handleLogout = () => {
        Alert.alert('로그아웃', '정말 로그아웃 하시겠습니까?', [
            { text: '취소', style: 'cancel' },
            {
                text: '로그아웃',
                style: 'destructive',
                onPress: async () => {
                    try {
                        await api.post('/auth/logout');
                    } catch (e) {
                        console.error('Logout error', e);
                    }
                    logout();
                    navigation.reset({ index: 0, routes: [{ name: 'SocialLogin' }] });
                }
            }
        ]);
    };

    const handleWithdraw = () => {
        Alert.alert('회원 탈퇴', '정말 탈퇴하시겠습니까? 연동된 가족 정보 및 데이터가 삭제될 수 있습니다.', [
            { text: '취소', style: 'cancel' },
            {
                text: '탈퇴하기',
                style: 'destructive',
                onPress: async () => {
                    try {
                        await api.delete('/users/me');
                        logout();
                        navigation.reset({ index: 0, routes: [{ name: 'SocialLogin' }] });
                    } catch (e) {
                        console.error('Withdraw error', e);
                        Alert.alert('오류', '탈퇴 처리 중 문제가 발생했습니다.');
                    }
                }
            }
        ]);
    };

    return (
        <SafeAreaView style={styles.safeArea}>
            <View style={styles.header}>
                <CustomText style={styles.headerTitle}>마이페이지</CustomText>
            </View>

            <ScrollView contentContainerStyle={styles.container} showsVerticalScrollIndicator={false}>

                {/* 프로필 요약 카드 */}
                <View style={styles.profileCard}>
                    <View style={styles.profileAvatarBox}>
                        <Image source={require('../../../assets/croco/croco_parents.png')} style={styles.profileAvatarImage} resizeMode="contain" />
                    </View>
                    <View style={styles.profileInfo}>
                        <CustomText style={styles.profileName}>{user?.name || '부모님'}</CustomText>
                        <CustomText style={styles.profileStatus}>
                            {user?.familyId ? '가족 연동 완료' : '가족 연동 필요'}
                        </CustomText>
                    </View>
                    <TouchableOpacity style={styles.editButton} onPress={() => navigation.navigate('ParentEditProfile')}>
                        <CustomText style={styles.editButtonText}>수정</CustomText>
                    </TouchableOpacity>
                </View>

                {/* 가족 관리 섹션 */}
                <View style={styles.menuGroup}>
                    <CustomText style={styles.menuGroupTitle}>가족</CustomText>
                    <TouchableOpacity style={styles.menuItem} onPress={() => navigation.navigate('FamilyManagementScreen')}>
                        <View style={styles.menuItemLeft}>
                            <CustomText style={styles.menuText}>가족/구성원 관리</CustomText>
                        </View>
                        <CustomText style={styles.chevron}>›</CustomText>
                    </TouchableOpacity>
                </View>

                {/* 데이터 섹션 - 리포트/내역 등 */}
                <View style={styles.menuGroup}>
                    <CustomText style={styles.menuGroupTitle}>데이터</CustomText>
                    <TouchableOpacity style={styles.menuItem} onPress={() => navigation.navigate('ParentReportScreen')}>
                        <View style={styles.menuItemLeft}>
                            <CustomText style={styles.menuText}>우리 아이 주간 소비 리포트</CustomText>
                        </View>
                        <CustomText style={styles.chevron}>›</CustomText>
                    </TouchableOpacity>
                    <TouchableOpacity style={styles.menuItem} onPress={() => navigation.navigate('ParentHistoryScreen')}>
                        <View style={styles.menuItemLeft}>
                            <CustomText style={styles.menuText}>소비 내역 상세</CustomText>
                        </View>
                        <CustomText style={styles.chevron}>›</CustomText>
                    </TouchableOpacity>
                </View>

                {/* 설정 메뉴 리스트 */}
                <View style={styles.menuGroup}>
                    <CustomText style={styles.menuGroupTitle}>설정</CustomText>

                    <View style={styles.menuItem}>
                        <View style={styles.menuItemLeft}>
                            <CustomText style={styles.menuText}>푸시 알림</CustomText>
                        </View>
                        <Switch
                            value={true}
                            trackColor={{ false: '#D1D5DB', true: '#A3E635' }}
                            thumbColor={'#FFFFFF'}
                        />
                    </View>

                    <TouchableOpacity style={styles.menuItem} onPress={() => navigation.navigate('ParentChangePassword')}>
                        <View style={styles.menuItemLeft}>
                            <CustomText style={styles.menuText}>비밀번호 변경</CustomText>
                        </View>
                        <CustomText style={styles.chevron}>›</CustomText>
                    </TouchableOpacity>
                </View>

                {/* 하단 유틸리티 */}
                <View style={styles.utilitySection}>
                    <TouchableOpacity onPress={handleLogout}>
                        <CustomText style={styles.logoutText}>로그아웃</CustomText>
                    </TouchableOpacity>
                    <View style={styles.divider} />
                    <TouchableOpacity onPress={handleWithdraw}>
                        <CustomText style={styles.withdrawalText}>회원 탈퇴</CustomText>
                    </TouchableOpacity>
                </View>

            </ScrollView>
        </SafeAreaView>
    );
};

const styles = StyleSheet.create({
    safeArea: { flex: 1, backgroundColor: '#F9FAFB' },
    header: { paddingHorizontal: scale(20), paddingVertical: verticalScale(16), backgroundColor: '#F9FAFB' },
    headerTitle: { fontSize: scale(20), fontWeight: 'bold', color: '#111' },
    container: { flexGrow: 1, paddingHorizontal: scale(16), paddingBottom: verticalScale(100) },

    profileCard: { flexDirection: 'row', alignItems: 'center', backgroundColor: '#FFFFFF', padding: scale(20), borderRadius: scale(20), marginBottom: verticalScale(24), shadowColor: '#000', shadowOffset: { width: 0, height: verticalScale(2) }, shadowOpacity: 0.08, shadowRadius: scale(8), elevation: 3 },
    profileAvatarBox: { width: scale(56), height: scale(56), borderRadius: scale(28), backgroundColor: '#E5E7EB', justifyContent: 'center', alignItems: 'center', marginRight: scale(16), overflow: 'hidden', shadowColor: '#000', shadowOffset: { width: 0, height: 2 }, shadowOpacity: 0.05, shadowRadius: 4, elevation: 2, },
    profileAvatarImage: { width: '80%', height: '80%' },
    profileInfo: { flex: 1 },
    profileName: { fontSize: scale(18), fontWeight: 'bold', color: '#111', marginBottom: verticalScale(2) },
    profileStatus: { fontSize: scale(13), color: '#6B7280' },

    editButton: { backgroundColor: '#F9FAFB', paddingVertical: verticalScale(6), paddingHorizontal: scale(12), borderRadius: scale(8), shadowColor: '#000', shadowOffset: { width: 0, height: 2 }, shadowOpacity: 0.05, shadowRadius: 4, elevation: 2, },
    editButtonText: { fontSize: scale(13), fontWeight: '600', color: '#4B5563' },

    menuGroup: { backgroundColor: '#FFFFFF', borderRadius: scale(20), paddingVertical: verticalScale(8), paddingHorizontal: scale(16), marginBottom: verticalScale(16), shadowColor: '#000', shadowOffset: { width: 0, height: verticalScale(2) }, shadowOpacity: 0.08, shadowRadius: scale(8), elevation: 3 },
    menuGroupTitle: { fontSize: scale(13), fontWeight: 'bold', color: '#9CA3AF', marginTop: verticalScale(8), marginBottom: verticalScale(4), marginLeft: scale(4) },
    menuItem: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingVertical: verticalScale(14), paddingHorizontal: scale(4), borderBottomWidth: 1, borderBottomColor: '#F9FAFB' },
    menuItemLeft: { flexDirection: 'row', alignItems: 'center' },
    menuIcon: { fontSize: scale(18), marginRight: scale(12) },
    menuText: { fontSize: scale(15), color: '#111', fontWeight: '500' },
    chevron: { fontSize: scale(20), color: '#D1D5DB' },

    utilitySection: { flexDirection: 'row', justifyContent: 'center', alignItems: 'center', marginTop: verticalScale(24), marginBottom: verticalScale(16) },
    logoutText: { fontSize: scale(13), color: '#6B7280', fontWeight: '500' },
    divider: { width: 1, height: verticalScale(12), backgroundColor: '#D1D5DB', marginHorizontal: scale(12) },
    withdrawalText: { fontSize: scale(13), color: '#EF4444', fontWeight: '500' }
});

export default ParentMyPageScreen;
