import React, { useEffect } from 'react';
import { View, StyleSheet, SafeAreaView, TouchableOpacity, ScrollView, Image, Alert } from 'react-native';
import { scale, verticalScale } from 'react-native-size-matters';
import CustomText from '../../../components/common/CustomText';
import api from '../../../api/axios';

// 더미 친구 목록
const MOCK_FRIENDS = [
    { id: '1', name: '이싸피', level: 12, townName: '이싸피의 방' },
    { id: '2', name: '박싸피', level: 8, townName: '박싸피의 방' },
    { id: '3', name: '최싸피', level: 20, townName: '최싸피의 방' },
];

const FriendListScreen = ({ navigation, route }) => {
    /* ==========================================
       [진짜 친구 목록 조회 API]
       ========================================== 
    const [friends, setFriends] = useState([]);
    useEffect(() => {
        const fetchFriends = async () => {
            try {
                // 내 친구 목록 데이터 조회
                // const res = await api.get('/friends');
                // setFriends(res.data.data);
            } catch(e) { console.error('Friends Fetch Error', e); }
        };
        fetchFriends();
    }, []);
    ========================================== */

    // 딥링크/QR 스캔 결과 라우팅 처리
    useEffect(() => {
        if (route?.params?.inviteStatus) {
            if (route.params.inviteStatus === 'success') {
                navigation.navigate('FriendSuccess'); // 가입 폭죽 화면으로 라우팅 분기
            } else if (route.params.inviteStatus === 'already_exists') {
                navigation.navigate('FriendAlready'); // 전용 프론트 페이지로 분기
            } else {
                Alert.alert('오류', '유효하지 않은 초대 링크입니다.');
            }
        }
    }, [route?.params?.inviteStatus, navigation]);

    // UI/UX 테스트용 임시 딥링크 트리거 함수
    const handleMockInvite = () => {
        Alert.alert(
            '링크 테스트 시뮬레이션',
            '초대 링크로 진입한 상황을 테스트합니다.',
            [
                { text: '성공', onPress: () => navigation.setParams({ inviteStatus: 'success' }) },
                { text: '이미 친구', onPress: () => navigation.setParams({ inviteStatus: 'already_exists' }) },
                { text: '취소', style: 'cancel' }
            ]
        );
    };

    return (
        <SafeAreaView style={styles.safeArea}>
            <View style={styles.header}>
                <TouchableOpacity style={styles.backButton} onPress={() => navigation.goBack()}>
                    <CustomText style={styles.backButtonText}>←</CustomText>
                </TouchableOpacity>
                <CustomText style={styles.headerTitle}>친구 목록</CustomText>
                <TouchableOpacity style={styles.addButton} onPress={handleMockInvite}>
                    <CustomText style={styles.addButtonText}>+ 시뮬레이션</CustomText>
                </TouchableOpacity>
            </View>

            <ScrollView contentContainerStyle={styles.container}>
                <View style={styles.searchBox}>
                    <CustomText style={styles.searchText}>🔍 닉네임으로 친구 검색</CustomText>
                </View>

                <CustomText style={styles.sectionTitle}>내 친구 ({MOCK_FRIENDS.length}명)</CustomText>

                {MOCK_FRIENDS.map(friend => (
                    <View key={friend.id} style={styles.friendRow}>
                        <View style={styles.avatarCircle}>
                            <Image source={require('../../../assets/croco/croco_face.png')} style={styles.avatarImage} resizeMode="contain" />
                        </View>
                        <View style={styles.friendInfo}>
                            <CustomText style={styles.friendName}>{friend.name} <CustomText style={styles.levelText}>LV.{friend.level}</CustomText></CustomText>
                            <CustomText style={styles.townName}>{friend.townName}</CustomText>
                        </View>
                        <TouchableOpacity style={styles.visitButton} onPress={() => navigation.navigate('FriendTown', { friend })}>
                            <CustomText style={styles.visitButtonText}>타운 방문</CustomText>
                        </TouchableOpacity>
                    </View>
                ))}
            </ScrollView>
        </SafeAreaView>
    );
};

const styles = StyleSheet.create({
    safeArea: { flex: 1, backgroundColor: '#FFFFFF' },
    header: {
        flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between',
        paddingHorizontal: scale(16), paddingVertical: verticalScale(16), backgroundColor: '#FFFFFF'
    },
    backButton: { width: scale(32), height: scale(32), justifyContent: 'center' },
    backButtonText: { fontSize: scale(22), fontWeight: 'bold', color: '#111' },
    headerTitle: { fontSize: scale(18), fontWeight: 'bold', color: '#111' },
    addButton: { backgroundColor: '#F3F4F6', paddingHorizontal: scale(12), paddingVertical: verticalScale(6), borderRadius: scale(12) },
    addButtonText: { fontSize: scale(14), fontWeight: 'bold', color: '#4B5563' },

    container: { flexGrow: 1, paddingHorizontal: scale(16), paddingTop: verticalScale(12) },

    searchBox: { backgroundColor: '#F3F4F6', paddingHorizontal: scale(16), paddingVertical: verticalScale(12), borderRadius: scale(12), marginBottom: verticalScale(24) },
    searchText: { fontSize: scale(14), color: '#9CA3AF' },

    sectionTitle: { fontSize: scale(14), fontWeight: 'bold', color: '#6B7280', marginBottom: verticalScale(16) },

    friendRow: { flexDirection: 'row', alignItems: 'center', marginBottom: verticalScale(16), paddingVertical: verticalScale(8), borderBottomWidth: 1, borderBottomColor: '#F9FAFB' },
    avatarCircle: { width: scale(48), height: scale(48), borderRadius: scale(24), backgroundColor: '#E5E7EB', justifyContent: 'center', alignItems: 'center', marginRight: scale(16), overflow: 'hidden' },
    avatarImage: { width: '80%', height: '80%' },
    friendInfo: { flex: 1 },
    friendName: { fontSize: scale(16), fontWeight: 'bold', color: '#111', marginBottom: verticalScale(4) },
    levelText: { fontSize: scale(12), color: '#A3E635', fontWeight: 'bold' },
    townName: { fontSize: scale(12), color: '#6B7280' },
    visitButton: { backgroundColor: '#A3E635', paddingHorizontal: scale(12), paddingVertical: verticalScale(8), borderRadius: scale(8) },
    visitButtonText: { fontSize: scale(12), fontWeight: 'bold', color: '#111' }
});

export default FriendListScreen;
