import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity, SafeAreaView, Platform, Image } from 'react-native';
import CustomText from '../../components/common/CustomText';
import { RFValue } from 'react-native-responsive-fontsize';

const RoleSelectScreen = ({ navigation, route }) => {
    const { tempToken } = route.params || {};

    const handleSelectRole = (role) => {
        navigation.navigate('QrCheck', { tempToken, role });
    };

    return (
        <SafeAreaView style={styles.safeArea}>
            <View style={styles.container}>
                <View style={styles.titleSection}>
                    <CustomText style={styles.title}>처음 오셨군요!</CustomText>
                    <CustomText style={styles.subtitle}>어떤 역할로 서비스를 시작할까요?</CustomText>
                </View>

                <View style={styles.cardSection}>
                    {/* 부모 */}
                    <TouchableOpacity
                        style={styles.card}
                        activeOpacity={0.8}
                        onPress={() => handleSelectRole('PARENT')}
                    >
                        <View style={styles.cardImageWrapper}>
                            <Image source={require('../../assets/croco/croco_parents.png')} style={styles.cardImageParent} />
                        </View>
                        <View style={styles.cardTextContainer}>
                            <CustomText style={styles.cardTitle}>부모</CustomText>
                            <CustomText style={styles.cardDesc}>아이 용돈과 소비 습관을 관리하고 싶어요.</CustomText>
                        </View>
                    </TouchableOpacity>

                    {/* 자녀 */}
                    <TouchableOpacity
                        style={styles.card}
                        activeOpacity={0.8}
                        onPress={() => handleSelectRole('CHILD')}
                    >
                        <View style={styles.cardImageWrapper}>
                            <Image source={require('../../assets/croco/croco_face.png')} style={styles.cardImageChild} />
                        </View>
                        <View style={styles.cardTextContainer}>
                            <CustomText style={styles.cardTitle}>아이</CustomText>
                            <CustomText style={styles.cardDesc}>용돈을 받고 모으는 재미를 알고 싶어요.</CustomText>
                        </View>
                    </TouchableOpacity>
                </View>
            </View>
        </SafeAreaView>
    );
};

const styles = StyleSheet.create({
    safeArea: {
        flex: 1,
        backgroundColor: '#FFFFFF',
    },
    container: {
        flex: 1,
        paddingHorizontal: RFValue(24),
        paddingVertical: RFValue(40),
    },
    titleSection: {
        marginBottom: RFValue(40),
        marginTop: RFValue(20),
    },
    title: {
        fontSize: RFValue(26),
        fontWeight: 'bold',
        color: '#111',
        marginBottom: RFValue(12),
    },
    subtitle: {
        fontSize: RFValue(15),
        color: '#6B7280',
        fontWeight: '500',
    },
    cardSection: {
        flex: 1,
        gap: RFValue(16),
    },
    card: {
        flexDirection: 'row',
        alignItems: 'center',
        backgroundColor: '#F9FAFB',
        paddingVertical: RFValue(24),
        paddingHorizontal: RFValue(20),
        borderRadius: RFValue(16),
        borderWidth: 1,
        borderColor: '#F3F4F6',
    },
    cardImageWrapper: {
        width: RFValue(80),
        height: RFValue(80),
        marginRight: RFValue(16),
        overflow: 'hidden',
        justifyContent: 'center',
        alignItems: 'center',
    },
    cardImageParent: {
        width: '125%',
        height: '125%',
        resizeMode: 'contain',
    },
    cardImageChild: {
        width: '140%',
        height: '140%',
        resizeMode: 'cover',
        transform: [{ translateY: RFValue(18) }],
    },
    cardTextContainer: {
        flex: 1,
    },
    cardTitle: {
        fontSize: RFValue(18),
        fontWeight: 'bold',
        color: '#111',
        marginBottom: RFValue(4),
    },
    cardDesc: {
        fontSize: RFValue(13),
        color: '#6B7280',
    }
});

export default RoleSelectScreen;
