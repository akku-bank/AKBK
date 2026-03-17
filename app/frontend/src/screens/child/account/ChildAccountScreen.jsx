```javascript
import React, { useState, useEffect } from 'react';
import { View, StyleSheet, SafeAreaView, ScrollView, TouchableOpacity } from 'react-native';
import { RFValue } from 'react-native-responsive-fontsize';
import CustomText from '../../../components/common/CustomText';
import api from '../../../api/axios';
};

const styles = StyleSheet.create({
    safeArea: { flex: 1, backgroundColor: '#F3F4F6' },
    container: { padding: RFValue(20) },
    balanceCard: { backgroundColor: '#FFF', borderRadius: RFValue(16), padding: RFValue(24), marginBottom: RFValue(30) },
    balanceLabel: { fontSize: RFValue(15), color: '#6B7280', marginBottom: RFValue(8) },
    balanceValue: { fontSize: RFValue(32), fontWeight: 'bold', color: '#111', marginBottom: RFValue(20) },
    actionRow: { flexDirection: 'row', gap: RFValue(12) },
    actionBtn: { flex: 1, backgroundColor: '#F3F4F6', padding: RFValue(14), borderRadius: RFValue(12), alignItems: 'center' },
    actionBtnText: { fontSize: RFValue(15), fontWeight: 'bold', color: '#111' },
    sectionTitle: { fontSize: RFValue(18), fontWeight: 'bold', color: '#111', marginBottom: RFValue(16) },
    historyCard: { backgroundColor: '#FFF', borderRadius: RFValue(16), padding: RFValue(20), marginBottom: RFValue(20) },
    historyRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingVertical: RFValue(12), borderBottomWidth: 1, borderBottomColor: '#F3F4F6' },
    historyTitle: { fontSize: RFValue(16), fontWeight: 'bold', color: '#111', marginBottom: RFValue(4) },
    historyDate: { fontSize: RFValue(13), color: '#9CA3AF' },
    historyAmount: { fontSize: RFValue(16), fontWeight: 'bold', color: '#111' },
    payButton: { backgroundColor: '#3B82F6', padding: RFValue(16), borderRadius: RFValue(12), alignItems: 'center' },
    payButtonText: { color: '#FFF', fontWeight: 'bold', fontSize: RFValue(16) }
});

export default ChildAccountScreen;
