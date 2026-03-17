import React from 'react';
import { View, StyleSheet, SafeAreaView, TouchableOpacity, ScrollView } from 'react-native';
import { scale, verticalScale } from 'react-native-size-matters';
import CustomText from '../../../components/common/CustomText';

const ParentReportScreen = ({ navigation, route }) => {
    // 자녀 이름 등은 route.params 로 넘겨받는다고 가정
    const childName = route.params?.childName || '김싸피';

    return (
        <SafeAreaView style={styles.safeArea}>
            <View style={styles.header}>
                <TouchableOpacity style={styles.backButton} onPress={() => navigation.goBack()}>
                    <CustomText style={styles.backButtonText}>←</CustomText>
                </TouchableOpacity>
                <CustomText style={styles.headerTitle}>{childName}의 주간 리포트</CustomText>
                <View style={{ width: scale(32) }} />
            </View>

            <ScrollView contentContainerStyle={styles.container}>
                <View style={styles.summaryCard}>
                    <CustomText style={styles.summaryTitle}>이번 주 총 지출</CustomText>
                    <CustomText style={styles.amountText}>15,000원</CustomText>
                    <CustomText style={styles.comparisonText}>저번 주보다 <CustomText style={styles.highlightRed}>3,000원</CustomText> 더 지출했어요 📈</CustomText>
                </View>

                <View style={styles.aiReviewCard}>
                    <CustomText style={styles.aiReviewTitle}>🤖 AI 부모님 조언 가이드</CustomText>
                    <CustomText style={styles.aiReviewText}>
                        "이번 주는 편의점 간식 지출 비율이 높습니다. 자녀와 함께 일주일 간식 예산을 정해보는 대화를 나눠보시는 것을 추천합니다!"
                    </CustomText>
                </View>

                <View style={styles.categoryCard}>
                    <CustomText style={styles.sectionTitle}>카테고리별 지출 요약</CustomText>
                    <View style={styles.categoryRow}>
                        <CustomText style={styles.categoryName}>🍔 간식류</CustomText>
                        <CustomText style={styles.categoryAmount}>10,000원</CustomText>
                    </View>
                    <View style={styles.categoryRow}>
                        <CustomText style={styles.categoryName}>🎮 오락</CustomText>
                        <CustomText style={styles.categoryAmount}>5,000원</CustomText>
                    </View>
                </View>
            </ScrollView>
        </SafeAreaView>
    );
};

const styles = StyleSheet.create({
    safeArea: { flex: 1, backgroundColor: '#F3F4F6' },
    header: {
        flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between',
        paddingHorizontal: scale(16), paddingVertical: verticalScale(16), backgroundColor: '#FFFFFF'
    },
    backButton: { width: scale(32), height: scale(32), justifyContent: 'center' },
    backButtonText: { fontSize: scale(22), fontWeight: 'bold', color: '#111' },
    headerTitle: { fontSize: scale(18), fontWeight: 'bold', color: '#111' },

    container: { flexGrow: 1, paddingHorizontal: scale(16), paddingTop: verticalScale(16), paddingBottom: verticalScale(40) },

    summaryCard: { backgroundColor: '#FFFFFF', padding: scale(20), borderRadius: scale(16), alignItems: 'center', marginBottom: verticalScale(16), shadowColor: '#000', shadowOffset: { width: 0, height: verticalScale(2) }, shadowOpacity: 0.05, shadowRadius: scale(4), elevation: 2 },
    summaryTitle: { fontSize: scale(14), color: '#6B7280', marginBottom: verticalScale(8) },
    amountText: { fontSize: scale(28), fontWeight: '900', color: '#111', marginBottom: verticalScale(8) },
    comparisonText: { fontSize: scale(14), color: '#4B5563' },
    highlightRed: { color: '#EF4444', fontWeight: 'bold' },

    aiReviewCard: { backgroundColor: '#F8FAFC', padding: scale(20), borderRadius: scale(16), marginBottom: verticalScale(16), borderWidth: 1, borderColor: '#E2E8F0' },
    aiReviewTitle: { fontSize: scale(16), fontWeight: 'bold', color: '#0F172A', marginBottom: verticalScale(8) },
    aiReviewText: { fontSize: scale(14), color: '#334155', lineHeight: 22 },

    categoryCard: { backgroundColor: '#FFFFFF', padding: scale(20), borderRadius: scale(16), shadowColor: '#000', shadowOffset: { width: 0, height: verticalScale(2) }, shadowOpacity: 0.05, shadowRadius: scale(4), elevation: 2 },
    sectionTitle: { fontSize: scale(16), fontWeight: 'bold', color: '#111', marginBottom: verticalScale(16) },
    categoryRow: { flexDirection: 'row', justifyContent: 'space-between', paddingVertical: verticalScale(12), borderBottomWidth: 1, borderBottomColor: '#F3F4F6' },
    categoryName: { fontSize: scale(15), color: '#4B5563' },
    categoryAmount: { fontSize: scale(15), fontWeight: 'bold', color: '#111' }
});

export default ParentReportScreen;
