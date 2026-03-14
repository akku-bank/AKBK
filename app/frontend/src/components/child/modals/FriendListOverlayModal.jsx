import React from 'react';
import { View, Text, StyleSheet, Modal, TouchableOpacity } from 'react-native';

const FriendListOverlayModal = ({ visible, onClose }) => {
    return (
        <Modal visible={visible} transparent={true} animationType="fade" onRequestClose={onClose}>
            <View style={styles.overlay}>
                <View style={styles.modalContainer}>
                    <Text style={styles.title}>친구 목록</Text>
                    <Text style={styles.emptyText}>아직 친구가 없어욤</Text>

                    <TouchableOpacity style={styles.closeButton} onPress={onClose}>
                        <Text style={styles.closeButtonText}>닫기</Text>
                    </TouchableOpacity>
                </View>
            </View>
        </Modal>
    );
};

const styles = StyleSheet.create({
    overlay: {
        flex: 1,
        justifyContent: 'center',
        alignItems: 'center',
        backgroundColor: 'rgba(0,0,0,0.5)',
    },
    modalContainer: {
        width: '80%',
        backgroundColor: '#FFFFFF',
        borderRadius: 24,
        padding: 24,
        alignItems: 'center',
        boxShadow: '0px 4px 10px rgba(0, 0, 0, 0.1)',
        elevation: 5,
    },
    title: {
        fontSize: 18,
        fontWeight: 'bold',
        color: '#111',
        marginBottom: 16,
    },
    emptyText: {
        fontSize: 14,
        color: '#6B7280',
        marginBottom: 24,
    },
    closeButton: {
        backgroundColor: '#F3F4F6',
        paddingVertical: 14,
        width: '100%',
        borderRadius: 12,
        alignItems: 'center',
    },
    closeButtonText: {
        fontSize: 16,
        fontWeight: 'bold',
        color: '#374151',
    }
});

export default FriendListOverlayModal;
