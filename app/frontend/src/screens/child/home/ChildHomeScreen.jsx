import React, { useState } from 'react';
import { View, Text, StyleSheet, TouchableOpacity, ScrollView, Platform, Image } from 'react-native';
import { RFValue } from 'react-native-responsive-fontsize';

const MapView = null;
const Marker = null;

import ChildAvatar from '../../../components/child/avatar/ChildAvatar';
import FriendListOverlayModal from '../../../components/child/modals/FriendListOverlayModal';
import { AvatarContext } from '../../../components/child/avatar/AvatarContext';
import { useContext } from 'react';

const ChildHomeScreen = ({ navigation }) => {
    const [isFriendModalVisible, setFriendModalVisible] = useState(false);
    const { equipState } = useContext(AvatarContext);
    return (
        <View style={styles.fullscreen}>
            <ScrollView contentContainerStyle={styles.container} showsVerticalScrollIndicator={false}>
                {/* 헤더 */}
                <View style={styles.headerContainer}>
                    <View style={styles.headerTopRow}>
                        <View style={styles.balanceWrapper}>
                            <Text style={styles.balanceLabel}>잔액</Text>
                            <Text style={styles.balanceAmount}>140,000<Text style={styles.balanceCurrency}> 원</Text></Text>
                        </View>
                        <TouchableOpacity style={styles.qrButton} onPress={() => { }}>
                            <Image
                                source={require('../../../assets/qr.png')}
                                style={styles.qrImage}
                                resizeMode="contain"
                            />
                        </TouchableOpacity>
                    </View>

                    <View style={styles.headerBottomRow}>
                        <TouchableOpacity style={styles.actionButton} onPress={() => setFriendModalVisible(true)}>
                            <Text style={styles.actionButtonText}>친구</Text>
                        </TouchableOpacity>
                        <TouchableOpacity style={styles.actionButton}>
                            <Text style={styles.actionButtonText}>알림</Text>
                        </TouchableOpacity>
                    </View>
                </View>

                {/* 기부 */}
                <TouchableOpacity style={styles.donationBox} activeOpacity={0.8} onPress={() => { navigation.navigate('BadgeMap') }}>
                    <View style={styles.mapContainer} pointerEvents="none">
                        <View style={[styles.map, styles.webMapPlaceholder]}>
                            <br></br>
                            <br></br>
                            <Text style={styles.webMapText}>준비 중</Text>
                        </View>
                    </View>
                    <View style={styles.donationOverlay}>
                        <Text style={styles.donationText}>내가 기부한 장소</Text>
                    </View>
                </TouchableOpacity>

                <View style={[styles.avatarSection]}>
                    <View style={styles.avatarNameHeader}>
                        <Text style={styles.avatarLevelText}>LV.15</Text>
                        <View style={styles.avatarNameRow}>
                            <Text style={styles.avatarNameText}>김싸피</Text>
                        </View>
                    </View>

                    <ChildAvatar equipState={equipState} size={200} />

                    {/* 옷장 */}
                    <TouchableOpacity style={styles.wardrobeButton} onPress={() => navigation.navigate('Wardrobe')}>
                        <Text style={styles.wardrobeButtonText}>옷장</Text>
                    </TouchableOpacity>
                </View>
            </ScrollView>

            {/* 친구 목록 */}
            <FriendListOverlayModal
                visible={isFriendModalVisible}
                onClose={() => setFriendModalVisible(false)}
            />
        </View>
    );
};

const styles = StyleSheet.create({
    fullscreen: {
        flex: 1,
        backgroundColor: '#ffffffff',
        overflow: 'hidden',
    },
    container: {
        flexGrow: 1,
        paddingHorizontal: RFValue(20),
        paddingBottom: RFValue(15),
        paddingTop: RFValue(20),
    },
    headerContainer: {
        marginBottom: RFValue(15),
    },
    headerTopRow: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'center',
        paddingBottom: RFValue(15),
        borderBottomWidth: 1.5,
        borderBottomColor: '#F3F4F6',
        marginBottom: RFValue(15),
        marginHorizontal: -RFValue(20),
        paddingHorizontal: RFValue(20),
    },
    balanceWrapper: {
        flexDirection: 'row',
        alignItems: 'baseline',
    },
    balanceLabel: {
        fontSize: RFValue(16),
        fontWeight: 'bold',
        color: '#4B5563',
        marginRight: RFValue(8),
    },
    balanceAmount: {
        fontSize: RFValue(28),
        fontWeight: 'bold',
        color: '#111',
    },
    balanceCurrency: {
        fontSize: RFValue(20),
        fontWeight: 'bold',
        color: '#111',
    },
    headerBottomRow: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'center',
    },
    actionButton: {
        backgroundColor: '#F3F4F6',
        paddingVertical: RFValue(10),
        paddingHorizontal: RFValue(24),
        borderRadius: RFValue(20),
        minWidth: RFValue(70),
        alignItems: 'center',
    },
    actionButtonText: {
        fontSize: RFValue(14),
        fontWeight: 'bold',
        color: '#4B5563',
    },
    qrButton: {
        width: RFValue(40),
        height: RFValue(40),
        justifyContent: 'center',
        alignItems: 'center',
    },
    qrImage: {
        width: '100%',
        height: '100%',
    },
    avatarSection: {
        flex: 1,
        alignItems: 'center',
        justifyContent: 'center',
        marginVertical: RFValue(5),
        position: 'relative',
        minHeight: RFValue(150),
    },
    avatarNameHeader: {
        alignItems: 'center',
        marginBottom: RFValue(10),
        zIndex: 10,
    },
    avatarLevelText: {
        fontSize: RFValue(13),
        color: '#6B7280',
        fontWeight: 'bold',
    },
    avatarNameRow: {
        flexDirection: 'row',
        alignItems: 'center',
        marginTop: RFValue(2),
    },
    avatarNameText: {
        fontSize: RFValue(18),
        fontWeight: 'bold',
        color: '#111',
    },
    wardrobeButton: {
        position: 'absolute',
        bottom: 0,
        right: '10%',
        backgroundColor: '#FFFFFF',
        paddingVertical: RFValue(10),
        paddingHorizontal: RFValue(16),
        borderRadius: RFValue(20),
        elevation: 4,
        shadowColor: '#000',
        shadowOffset: { width: 0, height: RFValue(2) },
        shadowOpacity: 0.1,
        shadowRadius: RFValue(5),
    },
    wardrobeButtonText: {
        fontSize: RFValue(14),
        fontWeight: 'bold',
        color: '#4B5563',
    },
    donationBox: {
        width: '100%',
        height: RFValue(100),
        borderRadius: RFValue(20),
        marginBottom: RFValue(10),
        shadowColor: '#000',
        shadowOffset: { width: 0, height: RFValue(2) },
        shadowOpacity: 0.1,
        shadowRadius: RFValue(5),
        elevation: 3,
        overflow: 'hidden',
        backgroundColor: '#fff',
    },
    mapContainer: {
        flex: 1,
    },
    map: {
        width: '100%',
        height: '100%',
    },
    webMapPlaceholder: {
        backgroundColor: '#E5E7EB',
        alignItems: 'center',
        justifyContent: 'center',
    },
    webMapText: {
        color: '#6B7280',
        fontSize: RFValue(12),
        fontWeight: 'bold',
    },
    donationOverlay: {
        position: 'absolute',
        top: RFValue(12),
        left: RFValue(15),
        backgroundColor: 'rgba(255, 255, 255, 0.9)',
        paddingVertical: RFValue(6),
        paddingHorizontal: RFValue(12),
        borderRadius: RFValue(12),
    },
    donationText: {
        fontSize: RFValue(14),
        fontWeight: 'bold',
        color: '#111',
    }
});

export default ChildHomeScreen;