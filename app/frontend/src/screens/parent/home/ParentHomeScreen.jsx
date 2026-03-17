import React from 'react';
import { View, StyleSheet, SafeAreaView, TouchableOpacity, ScrollView, Image } from 'react-native';
import { scale, verticalScale } from 'react-native-size-matters';
import CustomText from '../../../components/common/CustomText';
import api from '../../../api/axios';

// 더미 자녀 데이터
const MOCK_CHILDREN = [
    { id: '1', name: '김싸피', balance: 12500, avatar: require('../../../assets/croco/croco_face.png') },
    { id: '2', name: '이싸피', balance: 5000, avatar: require('../../../assets/croco/croco_face.png') }
];

const ParentHomeScreen = ({ navigation }) => {
    /* ==========================================
       [진짜 부모 홈(자녀 목록) 조회 API]
       ========================================== 
    const [childrenData, setChildrenData] = useState([]);
    useEffect(() => {
        const fetchChildren = async () => {
            try {
                // 부모 계정에 연결된 자녀 목록 및 각 자녀의 잔액 조회
                // const res = await api.get('/families/members');
                // setChildrenData(res.data.data);
            } catch(e) { console.error('Parent Home Fetch Error', e); }
        };
        fetchChildren();
    }, []);
    ========================================== */

    return (
        <SafeAreaView style={styles.safeArea}>
            <View style={styles.header}>
                <CustomText style={styles.headerTitle}>김아빠</CustomText>
                <TouchableOpacity onPress={() => navigation.navigate('ParentEditProfile')}>
                    <CustomText style={styles.settingsIcon}>⚙️</CustomText>
                </TouchableOpacity>
            </View>

            <ScrollView contentContainerStyle={styles.container}>
                <View style={styles.sectionHeader}>
                    <CustomText style={styles.sectionTitle}>우리 아이들</CustomText>
                </View>

                {MOCK_CHILDREN.map(child => (
                    <View key={child.id} style={styles.childCard}>
                        <View style={styles.childInfoRow}>
                            <View style={styles.childAvatarCircle}>
                                <Image source={child.avatar} style={styles.childAvatarImage} resizeMode="contain" />
                            </View>
                            <View style={styles.childTextInfo}>
                                <CustomText style={styles.childName}>{child.name}</CustomText>
                                <CustomText style={styles.childBalance}>{child.balance.toLocaleString()}원</CustomText>
                            </View>
                            <TouchableOpacity style={styles.historyBtn} onPress={() => navigation.navigate('ParentHistoryScreen', { child })}>
                                <CustomText style={styles.historyBtnText}>내역조회</CustomText>
                            </TouchableOpacity>
                        </View>

                        <View style={styles.actionRow}>
                            <TouchableOpacity style={styles.actionBtn} onPress={() => navigation.navigate('ParentTransferScreen', { child })}>
                                <CustomText style={styles.actionBtnText}>용돈 송금</CustomText>
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
                    <TouchableOpacity style={styles.reportCard} onPress={() => navigation.navigate('ParentReportScreen')}>
                        <CustomText style={styles.reportEmoji}>📊</CustomText>
                        <View style={{ flex: 1 }}>
                            <CustomText style={styles.reportTitle}>김싸피의 주간 리포트 도착!</CustomText>
                            <CustomText style={styles.reportDesc}>간식 지출이 평소보다 늘었어요.</CustomText>
                        </View>
                        <CustomText style={styles.arrowIcon}>→</CustomText>
                    </TouchableOpacity>
                </View>

            </ScrollView>
        </SafeAreaView>
    );
};

const styles = StyleSheet.create({
    safeArea: { flex: 1, backgroundColor: '#F3F4F6' },
    header: {
        flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between',
        paddingHorizontal: scale(20), paddingVertical: verticalScale(20), backgroundColor: '#FFFFFF',
        borderBottomWidth: 1, borderBottomColor: '#F3F4F6'
    },
    headerTitle: { fontSize: scale(20), fontWeight: '900', color: '#111' },
    settingsIcon: { fontSize: scale(20) },

    container: { flexGrow: 1, paddingHorizontal: scale(16), paddingTop: verticalScale(24), paddingBottom: verticalScale(40) },

    sectionHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: verticalScale(16) },
    sectionTitle: { fontSize: scale(18), fontWeight: 'bold', color: '#111' },
    manageText: { fontSize: scale(14), fontWeight: 'bold', color: '#6B7280' },

    childCard: { backgroundColor: '#FFFFFF', borderRadius: scale(20), padding: scale(20), marginBottom: verticalScale(16), shadowColor: '#000', shadowOffset: { width: 0, height: verticalScale(2) }, shadowOpacity: 0.05, shadowRadius: scale(4), elevation: 2 },
    childInfoRow: { flexDirection: 'row', alignItems: 'center', marginBottom: verticalScale(20) },
    childAvatarCircle: { width: scale(50), height: scale(50), borderRadius: scale(25), backgroundColor: '#E5E7EB', justifyContent: 'center', alignItems: 'center', marginRight: scale(16), overflow: 'hidden' },
    childAvatarImage: { width: '80%', height: '80%' },
    childTextInfo: { flex: 1 },
    childName: { fontSize: scale(16), fontWeight: 'bold', color: '#4B5563', marginBottom: verticalScale(4) },
    childBalance: { fontSize: scale(20), fontWeight: '900', color: '#111' },
    historyBtn: { backgroundColor: '#F3F4F6', paddingHorizontal: scale(12), paddingVertical: verticalScale(8), borderRadius: scale(8) },
    historyBtnText: { fontSize: scale(12), fontWeight: 'bold', color: '#4B5563' },

    actionRow: { flexDirection: 'row', gap: scale(12) },
    actionBtn: { flex: 1, backgroundColor: '#ECFCCB', paddingVertical: verticalScale(12), borderRadius: scale(12), alignItems: 'center' },
    actionBtnText: { fontSize: scale(14), fontWeight: 'bold', color: '#4D7C0F' },

    addBabyCard: { flexDirection: 'row', alignItems: 'center', backgroundColor: '#FFFFFF', borderRadius: scale(16), padding: scale(20), marginBottom: verticalScale(32), borderStyle: 'dashed', borderWidth: 2, borderColor: '#D1D5DB' },
    addBabyIcon: { fontSize: scale(24), fontWeight: 'bold', color: '#9CA3AF', marginRight: scale(16) },
    addBabyText: { fontSize: scale(15), fontWeight: 'bold', color: '#6B7280' },

    reportSection: { marginBottom: verticalScale(20) },
    reportCard: { flexDirection: 'row', alignItems: 'center', backgroundColor: '#FFFFFF', padding: scale(20), borderRadius: scale(16), marginTop: verticalScale(12) },
    reportEmoji: { fontSize: scale(24), marginRight: scale(16) },
    reportTitle: { fontSize: scale(15), fontWeight: 'bold', color: '#111', marginBottom: verticalScale(4) },
    reportDesc: { fontSize: scale(13), color: '#6B7280' },
    arrowIcon: { fontSize: scale(18), color: '#9CA3AF' }
});

export default ParentHomeScreen;
