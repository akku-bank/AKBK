import React from 'react';
import { View, StyleSheet, TouchableOpacity, SafeAreaView, ScrollView } from 'react-native';
import { scale, verticalScale } from 'react-native-size-matters';
import CustomText from '../../../components/common/CustomText';

// 임시 뱃지 데이터
const MOCK_MAP_DATA = [
    { id: 1, title: '지구 수호대', description: '지구 살리기 캠페인 기부', icon: '🌍', unlocked: true, color: '#D1FAE5' },
    { id: 2, title: '동물 친구들', description: '유기동물 보호소 기부', icon: '🐶', unlocked: true, color: '#FEF3C7' },
    { id: 3, title: '따뜻한 한 끼', description: '결식아동 지원 기부', icon: '🍲', unlocked: false, color: '#F9FAFB' },
    { id: 4, title: '희망의 숲', description: '나무 심기 기부', icon: '🌳', unlocked: false, color: '#F9FAFB' },
];

const BadgeMapScreen = ({ navigation }) => {

    return (
        <SafeAreaView style={styles.safeArea}>
            <View style={styles.header}>
                <TouchableOpacity style={styles.backButton} onPress={() => navigation.goBack()}>
                    <CustomText style={styles.backButtonText}>←</CustomText>
                </TouchableOpacity>
                <CustomText style={styles.headerTitle}>마음 수집 지도</CustomText>
                <View style={{ width: scale(32) }} />
            </View>

            <ScrollView contentContainerStyle={styles.container} showsVerticalScrollIndicator={false}>

                {/* 상단 뱃지 맵 배너 */}
                <View style={styles.banner}>
                    <CustomText style={styles.bannerTitle}>내가 만든 긍정적인 변화들</CustomText>
                    <CustomText style={styles.bannerSubtitle}>기부 도장(뱃지)을 모아 세상을 더 예쁘게 만들어요!</CustomText>
                </View>

                {/* 맵 리스트 UI */}
                <View style={styles.mapContainer}>
                    {MOCK_MAP_DATA.map((badge, index) => {
                        const isLast = index === MOCK_MAP_DATA.length - 1;

                        return (
                            <View key={badge.id} style={styles.mapNodeContainer}>
                                {/* 좌측 타임라인 라인 */}
                                {!isLast && <View style={[styles.timelineLine, badge.unlocked ? styles.timelineLineActive : null]} />}

                                <View style={styles.mapNode}>
                                    {/* 뱃지 아이콘 */}
                                    <View style={[
                                        styles.iconWrapper,
                                        { backgroundColor: badge.unlocked ? badge.color : '#F9FAFB' },
                                        !badge.unlocked && styles.iconWrapperLocked
                                    ]}>
                                        <CustomText style={[styles.nodeIcon, !badge.unlocked && styles.nodeIconLocked]}>
                                            {badge.icon}
                                        </CustomText>
                                    </View>

                                    {/* 정보 영역 */}
                                    <View style={styles.infoWrapper}>
                                        <CustomText style={[styles.nodeTitle, !badge.unlocked && styles.nodeTitleLocked]}>
                                            {badge.title}
                                        </CustomText>
                                        <CustomText style={styles.nodeDescription}>{badge.description}</CustomText>
                                    </View>
                                </View>
                            </View>
                        );
                    })}
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
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        paddingHorizontal: scale(16),
        paddingVertical: verticalScale(16),
        backgroundColor: '#F9FAFB',
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
        paddingVertical: verticalScale(16),
        paddingHorizontal: scale(8),
        marginBottom: verticalScale(16),
    },
    bannerTitle: {
        fontSize: scale(20),
        fontWeight: 'bold',
        color: '#111',
        marginBottom: verticalScale(4),
    },
    bannerSubtitle: {
        fontSize: scale(13),
        color: '#6B7280',
        fontWeight: '500',
    },
    mapContainer: {
        backgroundColor: '#FFFFFF',
        borderRadius: scale(24),
        padding: scale(24),
        shadowColor: '#000',
        shadowOffset: { width: 0, height: verticalScale(4) },
        shadowOpacity: 0.03,
        shadowRadius: scale(12),
        elevation: 2,
    },
    mapNodeContainer: {
        position: 'relative',
        paddingBottom: verticalScale(32),
    },
    timelineLine: {
        position: 'absolute',
        left: scale(28), // 56 / 2
        top: scale(56), // iconWrapper 높이
        bottom: 0,
        width: scale(2),
        backgroundColor: '#E5E7EB',
        zIndex: 0,
    },
    timelineLineActive: {
        backgroundColor: '#3B82F6', // 활성화된 연결선
        opacity: 0.5,
    },
    mapNode: {
        flexDirection: 'row',
        alignItems: 'center',
        zIndex: 1,
    },
    iconWrapper: {
        width: scale(56),
        height: scale(56),
        borderRadius: scale(28),
        justifyContent: 'center',
        alignItems: 'center',
        marginRight: scale(16),
        shadowColor: '#000',
        shadowOffset: { width: 0, height: verticalScale(2) },
        shadowOpacity: 0.1,
        shadowRadius: scale(6),
        elevation: 4,
    },
    iconWrapperLocked: {
        backgroundColor: '#F9FAFB',
        shadowOpacity: 0,
        elevation: 0,
        borderWidth: 1,
        borderColor: '#E5E7EB',
        borderStyle: 'dashed',
    },
    nodeIcon: {
        fontSize: scale(28),
    },
    nodeIconLocked: {
        opacity: 0.3,
    },
    infoWrapper: {
        flex: 1,
    },
    nodeTitle: {
        fontSize: scale(16),
        fontWeight: 'bold',
        color: '#111',
        marginBottom: verticalScale(4),
    },
    nodeTitleLocked: {
        color: '#9CA3AF',
    },
    nodeDescription: {
        fontSize: scale(13),
        color: '#6B7280',
    }
});

export default BadgeMapScreen;
