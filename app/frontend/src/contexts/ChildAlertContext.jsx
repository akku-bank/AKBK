import React, { createContext, useContext, useState } from 'react';
import { View, StyleSheet, TouchableOpacity, Modal } from 'react-native';
import { scale, verticalScale } from 'react-native-size-matters';
import CustomText from '../components/common/CustomText';

const ChildAlertContext = createContext({
    showAlert: () => {},
});

export const useChildAlert = () => useContext(ChildAlertContext);

export const ChildAlertProvider = ({ children }) => {
    const [alertConfig, setAlertConfig] = useState(null);

    const showAlert = ({
        title,
        message,
        confirmText = '확인',
        onConfirm,
        cancelText,
        onCancel,
        showCancel = false
    }) => {
        setAlertConfig({
            title,
            message,
            confirmText,
            onConfirm,
            cancelText,
            onCancel,
            showCancel
        });
    };

    const hideAlert = () => {
        setAlertConfig(null);
    };

    const handleConfirm = () => {
        if (alertConfig?.onConfirm) {
            alertConfig.onConfirm();
        }
        hideAlert();
    };

    const handleCancel = () => {
        if (alertConfig?.onCancel) {
            alertConfig.onCancel();
        }
        hideAlert();
    };

    return (
        <ChildAlertContext.Provider value={{ showAlert }}>
            {children}
            
            <Modal
                transparent={true}
                visible={!!alertConfig}
                animationType="fade"
                onRequestClose={hideAlert}
            >
                <View style={styles.modalOverlay}>
                    <View style={styles.modalCard}>
                        {alertConfig?.title && (
                            <CustomText style={styles.modalTitle}>{alertConfig.title}</CustomText>
                        )}
                        {alertConfig?.message && (
                            <CustomText style={styles.modalMessage}>{alertConfig.message}</CustomText>
                        )}

                        <View style={styles.buttonContainer}>
                            {alertConfig?.showCancel || alertConfig?.cancelText ? (
                                <TouchableOpacity 
                                    style={[styles.actionButton, styles.cancelButton]} 
                                    onPress={handleCancel}
                                >
                                    <CustomText style={styles.cancelButtonText}>
                                        {alertConfig.cancelText || '취소'}
                                    </CustomText>
                                </TouchableOpacity>
                            ) : null}
                            <TouchableOpacity 
                                style={[styles.actionButton, styles.confirmButton]} 
                                onPress={handleConfirm}
                            >
                                <CustomText style={styles.confirmButtonText}>
                                    {alertConfig?.confirmText || '확인'}
                                </CustomText>
                            </TouchableOpacity>
                        </View>
                    </View>
                </View>
            </Modal>
        </ChildAlertContext.Provider>
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
        width: '90%',
        backgroundColor: '#FFFFFF',
        borderRadius: scale(28),
        paddingHorizontal: scale(24),
        paddingTop: verticalScale(32),
        paddingBottom: verticalScale(24),
        alignItems: 'center',
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 4 },
        shadowOpacity: 0.1,
        shadowRadius: 10,
        elevation: 5
    },
    modalTitle: {
        fontSize: scale(20),
        fontWeight: '900',
        color: '#111',
        marginBottom: verticalScale(12),
        textAlign: 'center'
    },
    modalMessage: {
        fontSize: scale(15),
        color: '#4B5563',
        textAlign: 'center',
        marginBottom: verticalScale(28),
        lineHeight: scale(22)
    },
    buttonContainer: {
        flexDirection: 'row',
        width: '100%',
        justifyContent: 'center',
        gap: scale(12)
    },
    actionButton: {
        paddingVertical: verticalScale(14),
        borderRadius: scale(999), /* fully rounded */
        justifyContent: 'center',
        alignItems: 'center',
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 2 },
        shadowOpacity: 0.1,
        shadowRadius: 4,
        elevation: 2,
    },
    confirmButton: {
        flex: 1,
        backgroundColor: '#A3E635' /* Lime Green Theme */
    },
    confirmButtonText: {
        fontSize: scale(16),
        fontWeight: 'bold',
        color: '#111'
    },
    cancelButton: {
        flex: 0.8,
        backgroundColor: '#F3F4F6'
    },
    cancelButtonText: {
        fontSize: scale(15),
        fontWeight: 'bold',
        color: '#4B5563'
    }
});
