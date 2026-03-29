import React, { useCallback } from 'react';
import { View, StyleSheet, TouchableOpacity, ScrollView, SafeAreaView, Switch, Alert, Image } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { scale, verticalScale } from 'react-native-size-matters';
import CustomText from '../../../components/common/CustomText';
import useAuthStore from '../../../store/useAuthStore';
import api from '../../../api/axios';

const ChildMyPageScreen = ({ navigation }) => {
    const { user, setUser, logout } = useAuthStore();

    useFocusEffect(
        useCallback(() => {
            const fetchProfile = async () => {
                try {
                    const res = await api.get('/users/me');
                    const profile = res.data?.data;
                    if (profile) {
                        // user 객체 동기화 (기존 필드 유지하며 덮어쓰기)
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
        Alert.alert('회원 탈퇴', '정말 탈퇴하시겠습니까? 데이터가 모두 삭제됩니다.', [
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
                <CustomText style={styles.headerTitle}>내 정보</CustomText>
            </View>

            <ScrollView contentContainerStyle={styles.container} showsVerticalScrollIndicator={false}>

                {/* 프로필 요약 카드 */}
                <View style={styles.profileCard}>
                    <View style={styles.profileAvatarBox}>
                        <Image source={require('../../../assets/croco/croco_face.png')} style={styles.profileAvatarImage} resizeMode="contain" />
                    </View>
                    <View style={styles.profileInfo}>
                        <CustomText style={styles.profileName}>{user?.name || '...'}</CustomText>
                        <CustomText style={styles.profileCode}>
                            {user?.familyId ? '가족 연동 완료' : '가족 아이디 연동 중'}
                        </CustomText>
                    </View>
                    <TouchableOpacity style={styles.editButton} onPress={() => navigation.navigate('ChildEditProfile')}>
                        <CustomText style={styles.editButtonText}>수정</CustomText>
                    </TouchableOpacity>
                </View>

                <TouchableOpacity style={styles.reportGroup} onPress={() => navigation.navigate('WeeklyReport')} activeOpacity={0.8}>
                    <View style={styles.reportTextContent}>
                        <CustomText style={styles.reportGroupTitle}>주간 소비 리포트</CustomText>
                        <CustomText style={styles.reportGroupSubtitle}>이번 주 나는 어떻게 소비했지?</CustomText>
                    </View>
                    <Image source={require('../../../assets/croco/akku-curious.png')} style={styles.reportImage} resizeMode="contain" />
                </TouchableOpacity>

                {/* 메뉴 리스트 */}
                <View style={styles.menuGroup}>
                    <CustomText style={styles.menuGroupTitle}>설정</CustomText>

                    <View style={styles.menuItem}>
                        <View style={styles.menuItemLeft}>
                            <CustomText style={styles.menuIcon}>🔔</CustomText>
                            <CustomText style={styles.menuText}>푸시 알림</CustomText>
                        </View>
                        <Switch
                            value={true}
                            trackColor={{ false: '#D1D5DB', true: '#A3E635' }}
                            thumbColor={'#FFFFFF'}
                        />
                    </View>

                    <TouchableOpacity style={styles.menuItem} onPress={() => navigation.navigate('ChildChangePassword')}>
                        <View style={styles.menuItemLeft}>
                            <CustomText style={styles.menuIcon}>🔒</CustomText>
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
    safeArea: {
        flex: 1,
        backgroundColor: '#ECFCCB',
    },
    header: {
        paddingHorizontal: scale(20),
        paddingVertical: verticalScale(16),
        backgroundColor: '#FFFFFF',
        alignItems: 'center',
    },
    headerTitle: {
        fontSize: scale(20),
        fontWeight: 'bold',
        color: '#111',
    },
    container: {
        flexGrow: 1,
        paddingHorizontal: scale(16),
        paddingBottom: verticalScale(100),
    },
    profileCard: {
        flexDirection: 'row',
        alignItems: 'center',
        backgroundColor: '#FFFFFF',
        padding: scale(20),
        borderRadius: scale(20),
        marginTop: verticalScale(16),
        marginBottom: verticalScale(24),
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 2 },
        shadowOpacity: 0.03,
        shadowRadius: 8,
        elevation: 1,
    },
    profileAvatarBox: {
        width: scale(56),
        height: scale(56),
        borderRadius: scale(28),
        backgroundColor: '#FFFFFF',
        justifyContent: 'center',
        alignItems: 'center',
        marginRight: scale(16),
        overflow: 'hidden'
    },
    profileAvatarImage: {
        width: '110%',
        height: '110%',
        marginTop: verticalScale(22),
        marginLeft: verticalScale(5),
    },
    profileInfo: {
        flex: 1,
    },
    profileName: {
        fontSize: scale(18),
        fontWeight: 'bold',
        color: '#111',
        marginBottom: verticalScale(4),
    },
    profileCode: {
        fontSize: scale(13),
        color: '#6B7280',
    },
    editButton: {
        backgroundColor: '#ECFCCB',
        paddingVertical: verticalScale(6),
        paddingHorizontal: scale(12),
        borderRadius: scale(8),
    },
    editButtonText: {
        fontSize: scale(13),
        fontWeight: 'bold',
        color: '#111827',
    },
    menuGroup: {
        backgroundColor: '#FFFFFF',
        borderRadius: scale(20),
        paddingVertical: verticalScale(8),
        paddingHorizontal: scale(16),
        marginBottom: verticalScale(16),
    },
    menuGroupTitle: {
        fontSize: scale(13),
        fontWeight: 'bold',
        color: '#9CA3AF',
        marginTop: verticalScale(8),
        marginBottom: verticalScale(4),
        marginLeft: scale(4),
    },
    reportGroup: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        backgroundColor: '#FFFFFF',
        borderRadius: scale(20),
        paddingVertical: verticalScale(20),
        paddingHorizontal: scale(20),
        marginBottom: verticalScale(16),
        shadowColor: '#000',
        shadowOffset: { width: 0, height: verticalScale(2) },
        shadowOpacity: 0.05,
        shadowRadius: scale(8),
        elevation: 2,
    },
    reportTextContent: {
        flex: 1,
        justifyContent: 'center',
    },
    reportGroupTitle: {
        fontSize: scale(18),
        fontWeight: '900',
        color: '#111827',
        marginBottom: verticalScale(6),
    },
    reportGroupSubtitle: {
        fontSize: scale(13),
        color: '#6B7280',
        lineHeight: scale(18),
    },
    reportImage: {
        width: scale(80),
        height: scale(80),
        marginLeft: scale(12),
    },
    menuItem: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        paddingVertical: verticalScale(14),
        paddingHorizontal: scale(4),
        borderBottomWidth: 1,
        borderBottomColor: '#F9FAFB',
    },
    menuItemLeft: {
        flexDirection: 'row',
        alignItems: 'center',
    },
    menuIcon: {
        fontSize: scale(18),
        marginRight: scale(12),
    },
    menuText: {
        fontSize: scale(15),
        color: '#111',
        fontWeight: '500',
    },
    chevron: {
        fontSize: scale(20),
        color: '#D1D5DB',
    },
    utilitySection: {
        flexDirection: 'row',
        justifyContent: 'center',
        alignItems: 'center',
        marginTop: verticalScale(8),
        marginBottom: verticalScale(16),
    },
    logoutText: {
        fontSize: scale(13),
        color: '#6B7280',
        fontWeight: '500',
    },
    divider: {
        width: 1,
        height: verticalScale(12),
        backgroundColor: '#D1D5DB',
        marginHorizontal: scale(12),
    },
    withdrawalText: {
        fontSize: scale(13),
        color: '#EF4444',
        fontWeight: '500',
    },
    versionText: {
        textAlign: 'center',
        fontSize: scale(12),
        color: '#D1D5DB',
        marginBottom: verticalScale(24),
    }
});

export default ChildMyPageScreen;
