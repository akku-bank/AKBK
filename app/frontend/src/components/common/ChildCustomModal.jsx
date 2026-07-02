import React from 'react';
import { Modal, View, StyleSheet, TouchableWithoutFeedback, Animated } from 'react-native';
import { scale, verticalScale } from 'react-native-size-matters';

const ChildCustomModal = ({ 
    visible, 
    onClose, 
    children, 
    dismissable = true 
}) => {
    return (
        <Modal
            transparent={true}
            visible={visible}
            animationType="fade"
            onRequestClose={() => {
                if (dismissable && onClose) onClose();
            }}
        >
            <TouchableWithoutFeedback onPress={() => {
                if (dismissable && onClose) onClose();
            }}>
                <View style={styles.modalOverlay}>
                    <TouchableWithoutFeedback>
                        <View style={styles.modalCard}>
                            {children}
                        </View>
                    </TouchableWithoutFeedback>
                </View>
            </TouchableWithoutFeedback>
        </Modal>
    );
};

const styles = StyleSheet.create({
    modalOverlay: {
        flex: 1,
        backgroundColor: 'rgba(17, 24, 39, 0.38)',
        justifyContent: 'center',
        alignItems: 'center',
        paddingHorizontal: scale(20)
    },
    modalCard: {
        width: '100%',
        backgroundColor: '#FFFFFF',
        borderRadius: scale(28),
        padding: scale(24),
        shadowColor: '#000',
        shadowOffset: { width: 0, height: verticalScale(4) },
        shadowOpacity: 0.1,
        shadowRadius: scale(12),
        elevation: 5,
        alignItems: 'center'
    }
});

export default ChildCustomModal;
