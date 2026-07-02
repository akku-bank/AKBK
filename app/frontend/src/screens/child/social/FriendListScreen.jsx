import React, { useEffect, useState } from 'react';
import {
    View,
    StyleSheet,
    SafeAreaView,
    TouchableOpacity,
    ScrollView,
    Image,
    Alert,
    Modal,
    TextInput,
    ActivityIndicator,
} from 'react-native';
import { scale, verticalScale } from 'react-native-size-matters';
import CustomText from '../../../components/common/CustomText';
import api from '../../../api/axios';
import ChildCustomModal from '../../../components/common/ChildCustomModal';
import { useChildAlert } from '../../../contexts/ChildAlertContext';

const FriendListScreen = ({ navigation, route }) => {
    const [isCreatingInvite, setIsCreatingInvite] = useState(false);
    const [inviteCode, setInviteCode] = useState('');
    const [friends, setFriends] = useState([]);
    const [isLoadingFriends, setIsLoadingFriends] = useState(false);

    const [isInviteModalVisible, setIsInviteModalVisible] = useState(false);
    const [inviteCodeInput, setInviteCodeInput] = useState('');
    const [isLookingUpInvite, setIsLookingUpInvite] = useState(false);
    const [inviteLookupResult, setInviteLookupResult] = useState(null);
    const { showAlert } = useChildAlert();

    const fetchFriends = async () => {
        try {
            setIsLoadingFriends(true);
            const res = await api.get('/social/friends');
            const nextFriends = res?.data?.data?.friends || [];
            setFriends(nextFriends);
        } catch (e) {
            console.error('Friends Fetch Error', e);
            showAlert({ title: '오류', message: e.response?.data?.message || '친구 목록을 불러오지 못했습니다.' });
        } finally {
            setIsLoadingFriends(false);
        }
    };

    useEffect(() => {
        fetchFriends();
    }, []);

    useEffect(() => {
        if (route?.params?.inviteStatus) {
            if (route.params.inviteStatus === 'success') {
                navigation.navigate('FriendSuccess');
            } else if (route.params.inviteStatus === 'already_exists') {
                navigation.navigate('FriendAlready');
            } else {
                showAlert({ title: '오류', message: '유효하지 않은 초대 링크입니다.' });
            }
        }
    }, [route?.params?.inviteStatus, navigation]);

    const handleCreateInviteLink = async () => {
        if (isCreatingInvite) return;

        try {
            setIsCreatingInvite(true);
            const res = await api.post('/social/friends/invites');
            const nextInviteCode = res?.data?.data?.inviteCode;

            if (!nextInviteCode) {
                showAlert({ title: '오류', message: '초대 코드가 반환되지 않았습니다.' });
                return;
            }

            setInviteCode(nextInviteCode);
        } catch (e) {
            console.error('Create Invite Code Error', e);
            showAlert({ title: '오류', message: e.response?.data?.message || '초대 링크 생성에 실패했습니다.' });
        } finally {
            setIsCreatingInvite(false);
        }
    };

    const openInviteModal = () => {
        setInviteCodeInput('');
        setInviteLookupResult(null);
        setIsInviteModalVisible(true);
    };

    const closeInviteModal = () => {
        setIsInviteModalVisible(false);
        setInviteCodeInput('');
        setInviteLookupResult(null);
    };

    const handleLookupInviteCode = async () => {
        const trimmedCode = inviteCodeInput.trim();
        if (!trimmedCode) {
            showAlert({ title: '안내', message: '초대 코드를 입력해주세요.' });
            return;
        }

        try {
            setIsLookingUpInvite(true);
            setInviteLookupResult(null);
            const res = await api.get(`/social/friends/invites/${encodeURIComponent(trimmedCode)}`);
            const data = res?.data?.data;

            if (!data?.isValid) {
                setInviteLookupResult({ isValid: false });
                return;
            }

            setInviteLookupResult(data);
        } catch (e) {
            console.error('Invite Lookup Error', e);
            showAlert({ title: '오류', message: e.response?.data?.message || '초대 코드 조회에 실패했습니다.' });
        } finally {
            setIsLookingUpInvite(false);
        }
    };

    const handleFriendAddAttempt = async () => {
        const trimmedCode = inviteCodeInput.trim();
        if (!trimmedCode) {
            showAlert({ title: '안내', message: '초대 코드를 먼저 입력해 주세요.' });
            return;
        }

        try {
            await api.post(`/social/friends/invites/${encodeURIComponent(trimmedCode)}/accept`);
            closeInviteModal();
            fetchFriends();
            showAlert({ title: '완료', message: '친구가 추가되었습니다.' });
        } catch (e) {
            console.error('Accept Friend Invite Error', e);
            showAlert({ title: '오류', message: e.response?.data?.message || '친구 추가에 실패했습니다.' });
        }
    };

    return (
        <SafeAreaView style={styles.safeArea}>
            <View style={styles.header}>
                <TouchableOpacity style={styles.backButton} onPress={() => navigation.goBack()}>
                    <CustomText style={styles.backButtonText}>←</CustomText>
                </TouchableOpacity>
                <CustomText style={styles.headerTitle}>친구 목록</CustomText>
                <TouchableOpacity
                    style={[styles.addButton, isCreatingInvite && styles.addButtonDisabled]}
                    onPress={handleCreateInviteLink}
                    disabled={isCreatingInvite}
                >
                    <CustomText style={styles.addButtonText}>
                        {isCreatingInvite ? '생성 중...' : '초대'}
                    </CustomText>
                </TouchableOpacity>
            </View>

            <ScrollView contentContainerStyle={styles.container}>
                {inviteCode ? (
                    <View style={styles.inviteCard}>
                        <CustomText style={styles.inviteTitle}>초대 코드 생성 완료!</CustomText>
                        <View style={styles.inviteCodeRow}>
                            <CustomText style={styles.inviteCodeText} selectable>
                                {inviteCode}
                            </CustomText>
                        </View>
                        <CustomText style={styles.inviteHint}>
                            초대 코드를 길게 눌러 복사할 수 있어요.
                        </CustomText>
                    </View>
                ) : null}

                <View style={styles.sectionHeader}>
                    <CustomText style={styles.sectionTitle}>내 친구</CustomText>
                    <TouchableOpacity style={styles.sectionPlusButton} onPress={openInviteModal}>
                        <CustomText style={styles.sectionPlusText}>+</CustomText>
                    </TouchableOpacity>
                </View>

                {isLoadingFriends ? (
                    <View style={styles.emptyBox}>
                        <ActivityIndicator color="#2563EB" />
                    </View>
                ) : friends.length === 0 ? (
                    <View style={styles.emptyBox}>
                        <CustomText style={styles.emptyText}>아직 친구가 없어요.</CustomText>
                    </View>
                ) : (
                    friends.map(friend => (
                        <View key={friend.friendId} style={styles.friendRow}>
                            <View style={styles.avatarCircle}>
                                <Image
                                    source={require('../../../assets/croco/croco_face.png')}
                                    style={styles.avatarImage}
                                    resizeMode="contain"
                                />
                            </View>
                            <View style={styles.friendInfo}>
                                <CustomText style={styles.friendName}>{friend.name}</CustomText>
                            </View>
                            <TouchableOpacity
                                style={styles.visitButton}
                                onPress={() =>
                                    navigation.navigate('FriendTown', {
                                        friendId: friend.friendId,
                                        friendName: friend.name,
                                    })
                                }
                            >
                                <CustomText style={styles.visitButtonText}>방문하기</CustomText>
                            </TouchableOpacity>
                        </View>
                    ))
                )}
            </ScrollView>

            <ChildCustomModal visible={isInviteModalVisible} onClose={closeInviteModal}>
                <CustomText style={styles.modalTitle}>초대 코드 입력</CustomText>
                <CustomText style={styles.modalSubtitle}>
                    친구에게 받은 코드를 입력해 추가해보세요!
                </CustomText>

                <TextInput
                    value={inviteCodeInput}
                    onChangeText={setInviteCodeInput}
                    placeholder="초대 코드를 입력하세요"
                    style={styles.input}
                    autoCapitalize="none"
                    autoCorrect={false}
                />

                <TouchableOpacity
                    style={[styles.lookupButton, { borderRadius: scale(999) }, isLookingUpInvite && styles.lookupButtonDisabled]}
                    onPress={handleLookupInviteCode}
                    disabled={isLookingUpInvite}
                >
                    <CustomText style={styles.lookupButtonText}>
                        {isLookingUpInvite ? '조회 중...' : '초대 코드 조회'}
                    </CustomText>
                </TouchableOpacity>

                {inviteLookupResult ? (
                    inviteLookupResult.isValid ? (
                        <View style={styles.lookupCard}>
                            <CustomText style={styles.lookupLabel}>초대한 친구</CustomText>
                            <CustomText style={styles.lookupName}>
                                {inviteLookupResult.inviterName}
                            </CustomText>
                            <TouchableOpacity style={[styles.confirmButton, { borderRadius: scale(999) }]} onPress={handleFriendAddAttempt}>
                                <CustomText style={styles.confirmButtonText}>친구 추가</CustomText>
                            </TouchableOpacity>
                        </View>
                    ) : (
                        <View style={styles.lookupCardInvalid}>
                            <CustomText style={styles.lookupInvalidText}>
                                유효하지 않은 초대 코드입니다.
                            </CustomText>
                        </View>
                    )
                ) : null}

                <TouchableOpacity style={{ marginTop: verticalScale(14), width: '100%', alignItems: 'center' }} onPress={closeInviteModal}>
                    <CustomText style={{ fontSize: scale(15), fontWeight: 'bold', color: '#6B7280' }}>닫기</CustomText>
                </TouchableOpacity>
            </ChildCustomModal>
        </SafeAreaView>
    );
};

const styles = StyleSheet.create({
    safeArea: { flex: 1, backgroundColor: '#ECFCCB' },
    header: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        paddingHorizontal: scale(16),
        paddingVertical: verticalScale(16),
        backgroundColor: '#FFFFFF'
    },
    backButton: { width: scale(32), height: scale(32), justifyContent: 'center' },
    backButtonText: { fontSize: scale(22), fontWeight: 'bold', color: '#111' },
    headerTitle: { fontSize: scale(18), fontWeight: 'bold', color: '#111' },
    addButton: {
        backgroundColor: '#ECFCCB',
        paddingHorizontal: scale(12),
        paddingVertical: verticalScale(6),
        borderRadius: scale(12),
        minWidth: scale(56),
        alignItems: 'center'
    },
    addButtonDisabled: { opacity: 0.6 },
    addButtonText: { fontSize: scale(14), fontWeight: 'bold', color: '#4D7C0F' },

    container: {
        flexGrow: 1,
        paddingHorizontal: scale(16),
        paddingTop: verticalScale(12),
        paddingBottom: verticalScale(24)
    },

    inviteCard: {
        backgroundColor: '#F7FEE7',
        borderRadius: scale(18),
        paddingHorizontal: scale(18),
        paddingVertical: verticalScale(18),
        borderWidth: 1,
        borderColor: '#D9F99D',
        marginBottom: verticalScale(16),
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 2 },
        shadowOpacity: 0.05,
        shadowRadius: 4,
        elevation: 2,
    },
    inviteTitle: {
        fontSize: scale(16),
        fontWeight: 'bold',
        color: '#111',
        textAlign: 'center',
        marginBottom: verticalScale(12)
    },
    inviteCodeRow: {
        backgroundColor: '#FFFFFF',
        borderRadius: scale(14),
        paddingHorizontal: scale(14),
        paddingVertical: verticalScale(10)
    },
    inviteCodeText: {
        fontSize: scale(15),
        fontWeight: '700',
        color: '#4D7C0F'
    },
    inviteHint: {
        marginTop: verticalScale(10),
        fontSize: scale(12),
        color: '#6B7280',
        textAlign: 'center'
    },

    searchBox: {
        backgroundColor: '#F9FAFB',
        paddingHorizontal: scale(16),
        paddingVertical: verticalScale(12),
        borderRadius: scale(12),
        marginBottom: verticalScale(20),
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 2 },
        shadowOpacity: 0.05,
        shadowRadius: 4,
        elevation: 2,
    },
    searchText: { fontSize: scale(14), color: '#9CA3AF' },

    sectionHeader: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        marginBottom: verticalScale(16)
    },
    sectionTitle: { fontSize: scale(14), fontWeight: 'bold', color: '#6B7280' },
    sectionPlusButton: {
        width: scale(28),
        height: scale(28),
        borderRadius: scale(14),
        backgroundColor: '#FFFFFF',
        alignItems: 'center',
        justifyContent: 'center'
    },
    sectionPlusText: {
        fontSize: scale(18),
        fontWeight: 'bold',
        color: '#4D7C0F',
        lineHeight: scale(18)
    },

    emptyBox: {
        backgroundColor: '#F9FAFB',
        borderRadius: scale(16),
        paddingVertical: verticalScale(28),
        alignItems: 'center',
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 2 },
        shadowOpacity: 0.05,
        shadowRadius: 4,
        elevation: 2,
    },
    emptyText: {
        fontSize: scale(14),
        color: '#6B7280'
    },

    friendRow: {
        flexDirection: 'row',
        alignItems: 'center',
        marginBottom: verticalScale(16),
        paddingVertical: verticalScale(8),
        borderBottomWidth: 1,
        borderBottomColor: '#F9FAFB'
    },
    avatarCircle: {
        width: scale(48),
        height: scale(48),
        borderRadius: scale(24),
        backgroundColor: '#FFFFFF',
        justifyContent: 'center',
        alignItems: 'center',
        marginRight: scale(16),
        overflow: 'hidden'
    },
    avatarImage: {
        width: '110%',
        height: '110%',
        marginTop: verticalScale(22),
        marginLeft: verticalScale(5),
    },
    friendInfo: { flex: 1 },
    friendName: { fontSize: scale(18), fontWeight: 'bold', color: '#111' },
    visitButton: {
        backgroundColor: '#FFFFFF',
        borderWidth: 1,
        borderColor: '#E5E7EB',
        paddingHorizontal: scale(12),
        paddingVertical: verticalScale(8),
        borderRadius: scale(8)
    },
    visitButtonText: { fontSize: scale(12), fontWeight: 'bold', color: '#111' },

    modalBackdrop: {
        flex: 1,
        backgroundColor: 'rgba(17, 24, 39, 0.45)',
        justifyContent: 'center',
        paddingHorizontal: scale(20)
    },
    modalCard: {
        backgroundColor: '#FFFFFF',
        borderRadius: scale(22),
        paddingHorizontal: scale(20),
        paddingVertical: verticalScale(22),
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 2 },
        shadowOpacity: 0.05,
        shadowRadius: 4,
        elevation: 2,
    },
    modalTitle: {
        fontSize: scale(18),
        fontWeight: 'bold',
        color: '#111',
        textAlign: 'center',
        marginBottom: verticalScale(8)
    },
    modalSubtitle: {
        fontSize: scale(13),
        color: '#6B7280',
        textAlign: 'center',
        marginBottom: verticalScale(16)
    },
    input: {
        width: '100%',
        borderWidth: 1,
        borderColor: '#D1D5DB',
        borderRadius: scale(14),
        paddingHorizontal: scale(14),
        paddingVertical: verticalScale(12),
        fontSize: scale(14),
        color: '#111',
        marginBottom: verticalScale(12),
        fontFamily: 'Mulmaru'
    },
    lookupButton: {
        backgroundColor: '#A3E635',
        borderRadius: scale(14),
        paddingVertical: verticalScale(12),
        alignItems: 'center',
        width: '100%',
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 2 },
        shadowOpacity: 0.1,
        shadowRadius: 4,
        elevation: 2,
    },
    lookupButtonDisabled: {
        opacity: 0.6
    },
    lookupButtonText: {
        fontSize: scale(14),
        fontWeight: 'bold',
        color: '#111'
    },
    lookupCard: {
        backgroundColor: '#F7FEE7',
        borderRadius: scale(16),
        paddingHorizontal: scale(16),
        paddingVertical: verticalScale(16),
        marginTop: verticalScale(14),
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 2 },
        shadowOpacity: 0.05,
        shadowRadius: 4,
        elevation: 2,
    },
    lookupLabel: {
        fontSize: scale(12),
        color: '#6B7280',
        textAlign: 'center',
        marginBottom: verticalScale(6)
    },
    lookupName: {
        fontSize: scale(18),
        fontWeight: 'bold',
        color: '#111',
        textAlign: 'center',
        marginBottom: verticalScale(14)
    },
    confirmButton: {
        backgroundColor: '#A3E635',
        borderRadius: scale(12),
        paddingVertical: verticalScale(11),
        alignItems: 'center',
        width: '100%',
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 2 },
        shadowOpacity: 0.1,
        shadowRadius: 4,
        elevation: 2,
    },
    confirmButtonText: {
        fontSize: scale(14),
        fontWeight: 'bold',
        color: '#111'
    },
    lookupHint: {
        marginTop: verticalScale(10),
        fontSize: scale(12),
        color: '#6B7280',
        textAlign: 'center'
    },
    lookupCardInvalid: {
        backgroundColor: '#FEF2F2',
        borderRadius: scale(16),
        paddingHorizontal: scale(16),
        paddingVertical: verticalScale(16),
        marginTop: verticalScale(14)
    },
    lookupInvalidText: {
        fontSize: scale(14),
        fontWeight: 'bold',
        color: '#DC2626',
        textAlign: 'center'
    },
    closeButton: {
        marginTop: verticalScale(14),
        alignItems: 'center'
    },
    closeButtonText: {
        fontSize: scale(14),
        fontWeight: 'bold',
        color: '#6B7280'
    }
});

export default FriendListScreen;
