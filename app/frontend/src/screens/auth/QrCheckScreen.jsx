import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity, SafeAreaView, Image, Alert } from 'react-native';
import CustomText from '../../components/common/CustomText';
import { RFValue } from 'react-native-responsive-fontsize';

const QrCheckScreen = ({ navigation, route }) => {
    const { tempToken, role } = route.params || {};

    const handleHasQr = () => {
        // QR 있다 ? -> 스캔 화면으로
        navigation.navigate('QRScan', { tempToken, role });
    };

    const handleNoQr = () => {
        if (role === 'CHILD') {
            Alert.alert(
                '앗! 안타깝게도',
                '자녀 가입은 부모님이 먼저 가입하신 후\n초대 QR 코드를 스캔해야만 가능해요.\n\n부모님께 아꾸뱅꾸를 만들어달라고 조르러 가볼까요?',
                [{ text: '확인', style: 'default' }]
            );
        } else {
            // 부모는 QR이 없으면 새 가족 그룹 생성 단계로 이동
            navigation.navigate('SignUp', { tempToken, role });
        }
    };

    return (
        <SafeAreaView style={styles.safeArea}>
            <View style={styles.container}>
                <View style={styles.titleSection}>
                    <CustomText style={styles.title}>가족 연결하기</CustomText>
                    <CustomText style={styles.subtitle}>
                        {role === 'CHILD'
                            ? '부모님이 공유해주신\n초대 QR 코드가 있나요?'
                            : '배우자가 공유한\n초대 QR 코드가 있나요?'}
                    </CustomText>
                </View>

                <View style={styles.cardSection}>
                    {/* QR O */}
                    <TouchableOpacity
                        style={styles.card}
                        activeOpacity={0.8}
                        onPress={handleHasQr}
                    >
                        <View style={styles.iconWrapper}>
                            <CustomText style={styles.iconText}>📷</CustomText>
                        </View>
                        <View style={styles.cardTextContainer}>
                            <CustomText style={styles.cardTitle}>네, QR 코드가 있어요</CustomText>
                            <CustomText style={styles.cardDesc}>카메라폰으로 바로 스캔할게요.</CustomText>
                        </View>
                    </TouchableOpacity>

                    {/* QR X */}
                    <TouchableOpacity
                        style={styles.card}
                        activeOpacity={0.8}
                        onPress={handleNoQr}
                    >
                        <View style={[styles.iconWrapper, { backgroundColor: '#F3F4F6' }]}>
                            <Image source={require('../../assets/croco/croco_parents.png')} style={styles.cardIconImage} resizeMode="contain" />
                        </View>
                        <View style={styles.cardTextContainer}>
                            <CustomText style={styles.cardTitle}>아니요, 아직 없어요</CustomText>
                            <CustomText style={styles.cardDesc}>
                                {role === 'CHILD'
                                    ? '부모님이 먼저 가입하셔야 해요.'
                                    : '새로운 가족 그룹을 만들게요.'}
                            </CustomText>
                        </View>
                    </TouchableOpacity>
                </View>
            </View>
        </SafeAreaView>
    );
};

const styles = StyleSheet.create({
    safeArea: { flex: 1, backgroundColor: '#FFFFFF' },
    container: { flex: 1, paddingHorizontal: RFValue(24), paddingVertical: RFValue(40) },
    titleSection: { marginBottom: RFValue(40), marginTop: RFValue(20) },
    title: { fontSize: RFValue(26), fontWeight: 'bold', color: '#111', marginBottom: RFValue(12) },
    subtitle: { fontSize: RFValue(15), color: '#6B7280', fontWeight: '500', lineHeight: RFValue(22) },
    cardSection: { flex: 1, gap: RFValue(16) },
    card: { flexDirection: 'row', alignItems: 'center', backgroundColor: '#F9FAFB', paddingVertical: RFValue(24), paddingHorizontal: RFValue(20), borderRadius: RFValue(16), borderWidth: 1, borderColor: '#F3F4F6' },
    iconWrapper: { width: RFValue(60), height: RFValue(60), borderRadius: RFValue(30), backgroundColor: '#EFF6FF', justifyContent: 'center', alignItems: 'center', marginRight: RFValue(16) },
    iconText: { fontSize: RFValue(28) },
    cardIconImage: { width: '70%', height: '70%' },
    cardTextContainer: { flex: 1 },
    cardTitle: { fontSize: RFValue(16), fontWeight: 'bold', color: '#111', marginBottom: RFValue(4) },
    cardDesc: { fontSize: RFValue(13), color: '#6B7280' }
});

export default QrCheckScreen;
