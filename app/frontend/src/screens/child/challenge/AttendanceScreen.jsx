import React, { useState } from 'react';
import { View, StyleSheet, TouchableOpacity, SafeAreaView, ScrollView, Alert } from 'react-native';
import { scale, verticalScale } from 'react-native-size-matters';
import CustomText from '../../../components/common/CustomText';
import api from '../../../api/axios';

// 임시 출석 데이터 (7일 연속 출석)
const MOCK_ATTENDANCE = [
    { day: 1, label: '월', status: 'ATTENDED', reward: '🍬 10' },
    { day: 2, label: '화', status: 'ATTENDED', reward: '🍬 10' },
    { day: 3, label: '수', status: 'TODAY', reward: '🍬 20' },
    { day: 4, label: '목', status: 'UPCOMING', reward: '🍬 10' },
    { day: 5, label: '금', status: 'UPCOMING', reward: '🍬 10' },
    { day: 6, label: '토', status: 'UPCOMING', reward: '🍬 30' },
    { day: 7, label: '일', status: 'UPCOMING', reward: '🎁 박스' },
];

const AttendanceScreen = ({ navigation }) => {
    const [isCheckedIn, setIsCheckedIn] = useState(false);

    /* ==========================================
       [진짜 출석 현황 조회 API]
       ========================================== 
    const [attendanceData, setAttendanceData] = useState([]);
    useEffect(() => {
        const fetchAttendance = async () => {
            try {
                // 이번 주 출석 현황 조회
                // const res = await api.get('/attendance');
                // setAttendanceData(res.data.data);
            } catch(e) { console.error('Attendance Fetch Error', e); }
        };
        fetchAttendance();
    }, []);
    ========================================== */

    const handleCheckIn = () => {
        /* ==========================================
           [진짜 출석 체크 처리 API]
           ========================================== 
        try {
            // await api.post('/attendance/checkin');
            // 출석 성공 후 현황 재조회 또는 로컬 상태 갱신
        } catch(e) { console.error('Attendance CheckIn Error', e); }
        ========================================== */

        setIsCheckedIn(true);
        Alert.alert('출석 완료!', '오늘의 출석 도장을 찍었습니다.\n(7일 연속 달성 시 팝업 연동 필요)');
    };

    const handleBoxClick = (dayItem) => {
        if (dayItem.day === 7) {
            Alert.alert(
                '스페셜 랜덤 박스',
                '7일 연속 출석 달성!\n랜덤 박스를 열어보시겠어요? (가챠 연동 필요)',
                [
                    { text: '나중에', style: 'cancel' },
                    { text: '열기', onPress: () => Alert.alert('알림', '가챠 스크린 연동 예정') }
                ]
            );
        }
    };

    const renderDayCard = (item) => {
        let cardStyle = styles.dayCardUpcoming;
        let textStyle = styles.dayTextUpcoming;
        let iconStyle = styles.iconUpcoming;
        let icon = '🎁';

        if (item.status === 'ATTENDED' || (item.status === 'TODAY' && isCheckedIn)) {
            cardStyle = styles.dayCardAttended;
            textStyle = styles.dayTextAttended;
            iconStyle = styles.iconAttended;
            icon = '✅';
        } else if (item.status === 'TODAY') {
            cardStyle = styles.dayCardToday;
            textStyle = styles.dayTextToday;
            iconStyle = styles.iconToday;
            icon = '🔒';
        }

        return (
            <TouchableOpacity
                key={item.day}
                style={[styles.dayCard, cardStyle]}
                activeOpacity={item.day === 7 ? 0.7 : 1}
                onPress={() => handleBoxClick(item)}
            >
                <CustomText style={[styles.dayLabel, textStyle]}>{item.label}</CustomText>
                <View style={styles.iconCircle}>
                    <CustomText style={[styles.iconText, iconStyle]}>{icon}</CustomText>
                </View>
                <CustomText style={[styles.rewardText, textStyle]}>{item.reward}</CustomText>
            </TouchableOpacity>
        );
    };

    return (
        <SafeAreaView style={styles.safeArea}>
            <View style={styles.header}>
                <TouchableOpacity style={styles.backButton} onPress={() => navigation.goBack()}>
                    <CustomText style={styles.backButtonText}>←</CustomText>
                </TouchableOpacity>
                <CustomText style={styles.headerTitle}>출석 체크</CustomText>
                <View style={{ width: scale(32) }} />
            </View>

            <ScrollView contentContainerStyle={styles.container} showsVerticalScrollIndicator={false}>

                {/* 상단 배너 */}
                <View style={styles.banner}>
                    <CustomText style={styles.bannerTitle}>매일매일 접속하고{`\n`}선물을 받아가세요!</CustomText>
                    <CustomText style={styles.bannerSubtitle}>7일 연속 출석 시 스페셜 랜덤 박스 무조건 지급 🎉</CustomText>
                </View>

                {/* 출석 현황판 */}
                <View style={styles.boardCard}>
                    <View style={styles.daysGrid}>
                        {MOCK_ATTENDANCE.slice(0, 4).map(renderDayCard)}
                    </View>
                    <View style={styles.daysGrid}>
                        {MOCK_ATTENDANCE.slice(4, 7).map(renderDayCard)}
                        {/* 7일차 옆의 빈 공간 채우기 용도 */}
                        <View style={[styles.dayCard, { backgroundColor: 'transparent', borderWidth: 0 }]} />
                    </View>
                </View>

                {/* 출석 버튼 */}
                <TouchableOpacity
                    style={[styles.checkInButton, isCheckedIn && styles.checkInButtonDisabled]}
                    activeOpacity={0.8}
                    onPress={handleCheckIn}
                    disabled={isCheckedIn}
                >
                    <CustomText style={[styles.checkInButtonText, isCheckedIn && styles.checkInButtonTextDisabled]}>
                        {isCheckedIn ? '오늘 출석 완료!' : '오늘의 출석 도장 찍기'}
                    </CustomText>
                </TouchableOpacity>

            </ScrollView>
        </SafeAreaView>
    );
};

const styles = StyleSheet.create({
    safeArea: {
        flex: 1,
        backgroundColor: '#F3F4F6',
    },
    header: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        paddingHorizontal: scale(16),
        paddingVertical: verticalScale(16),
        backgroundColor: '#F3F4F6',
    },
    backButton: {
        width: scale(32),
        height: scale(32),
        justifyContent: 'center',
    },
    backButtonText: {
        fontSize: scale(22),
        fontWeight: 'bold',
        color: '#111',
    },
    headerTitle: {
        fontSize: scale(18),
        fontWeight: 'bold',
        color: '#111',
    },
    container: {
        flexGrow: 1,
        paddingHorizontal: scale(16),
        paddingBottom: verticalScale(40),
    },
    banner: {
        marginBottom: verticalScale(24),
        paddingHorizontal: scale(8),
    },
    bannerTitle: {
        fontSize: scale(22),
        fontWeight: '900',
        color: '#111',
        marginBottom: verticalScale(8),
        lineHeight: 30,
    },
    bannerSubtitle: {
        fontSize: scale(14),
        color: '#6B7280',
        fontWeight: '600',
    },
    boardCard: {
        backgroundColor: '#FFFFFF',
        borderRadius: scale(24),
        padding: scale(20),
        shadowColor: '#000',
        shadowOffset: { width: 0, height: verticalScale(4) },
        shadowOpacity: 0.05,
        shadowRadius: scale(12),
        elevation: 3,
        marginBottom: verticalScale(32),
    },
    daysGrid: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        marginBottom: verticalScale(16),
    },
    dayCard: {
        width: '23%', // 4개 배치
        aspectRatio: 0.8,
        borderRadius: scale(16),
        borderWidth: 2,
        alignItems: 'center',
        paddingVertical: verticalScale(12),
        justifyContent: 'center',
    },
    dayCardAttended: {
        borderColor: '#A3E635', // 라임 그린
        backgroundColor: '#ECFCCB',
    },
    dayCardToday: {
        borderColor: '#111',
        backgroundColor: '#FFFFFF',
    },
    dayCardUpcoming: {
        borderColor: '#E5E7EB',
        backgroundColor: '#F9FAFB',
    },
    dayLabel: {
        fontSize: scale(13),
        fontWeight: 'bold',
        marginBottom: verticalScale(8),
    },
    dayTextAttended: {
        color: '#A3E635', // 라임 그린 텍스트
    },
    dayTextToday: {
        color: '#111',
    },
    dayTextUpcoming: {
        color: '#9CA3AF',
    },
    iconCircle: {
        width: scale(32),
        height: scale(32),
        borderRadius: scale(16),
        backgroundColor: '#FFFFFF',
        alignItems: 'center',
        justifyContent: 'center',
        marginBottom: verticalScale(8),
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 2 },
        shadowOpacity: 0.05,
        shadowRadius: 4,
        elevation: 1,
    },
    iconText: {
        fontSize: scale(14),
    },
    rewardText: {
        fontSize: scale(11),
        fontWeight: '600',
    },
    checkInButton: {
        backgroundColor: '#111',
        borderRadius: scale(16),
        paddingVertical: verticalScale(16),
        alignItems: 'center',
        shadowColor: '#111',
        shadowOffset: { width: 0, height: 4 },
        shadowOpacity: 0.2,
        shadowRadius: 8,
        elevation: 4,
    },
    checkInButtonDisabled: {
        backgroundColor: '#E5E7EB',
        shadowOpacity: 0,
        elevation: 0,
    },
    checkInButtonText: {
        color: '#FFFFFF',
        fontSize: scale(16),
        fontWeight: 'bold',
    },
    checkInButtonTextDisabled: {
        color: '#9CA3AF',
    }
});

export default AttendanceScreen;
