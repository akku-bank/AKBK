import React, { useState, useRef } from 'react';
import { View, StyleSheet, TouchableOpacity, Modal, PanResponder, SafeAreaView, Dimensions, Alert } from 'react-native';
import { scale, verticalScale } from 'react-native-size-matters';
import Svg, { Path } from 'react-native-svg';
import ChildAvatar from '../avatar/ChildAvatar';
import CustomText from '../../../components/common/CustomText';

const { width } = Dimensions.get('window');
export const FACE_PAINT_CANVAS_SIZE = width - scale(40);

const FacePaintingModal = ({ visible, onClose, onSave, initialPaths, equipState }) => {
    const [paths, setPaths] = useState(initialPaths || []);
    const [currentPath, setCurrentPath] = useState(null);
    const [color, setColor] = useState('#EF4444'); // 기본 빨간색 설정
    const [isEraser, setIsEraser] = useState(false);

    const COLORS = ['#EF4444', '#3B82F6', '#F59E0B', '#10B981', '#8B5CF6', '#EC4899', '#111827', '#FFFFFF'];

    const handlePanResponderGrant = (evt) => {
        const { locationX, locationY } = evt.nativeEvent;
        setCurrentPath({
            path: `M${locationX},${locationY}`,
            color: isEraser ? 'transparent' : color,
            strokeWidth: isEraser ? 20 : 5,
            isEraser
        });
    };

    const handlePanResponderMove = (evt) => {
        const { locationX, locationY } = evt.nativeEvent;
        setCurrentPath(prev => {
            if (!prev) return prev;
            return {
                ...prev,
                path: `${prev.path} L${locationX},${locationY}`
            };
        });
    };

    const handlePanResponderRelease = () => {
        if (currentPath) {
            setPaths(prev => [...prev, currentPath]);
            setCurrentPath(null);
        }
    };

    const panResponder = useRef(
        PanResponder.create({
            onStartShouldSetPanResponder: () => true,
            onMoveShouldSetPanResponder: () => true,
            onPanResponderGrant: handlePanResponderGrant,
            onPanResponderMove: handlePanResponderMove,
            onPanResponderRelease: handlePanResponderRelease,
        })
    ).current;

    const clearCanvas = () => {
        Alert.alert('초기화', '정말 그림을 모두 지우시겠어요?', [
            { text: '취소', style: 'cancel' },
            { text: '지우기', onPress: () => setPaths([]), style: 'destructive' }
        ]);
    };

    const handleSave = () => {
        // 지우개(transparent) 패스는 다른 패스를 덮어서 지우는 효과를 줌
        // 그림 결과물을 부모 화면에 전달
        onSave(paths);
        onClose();
    };

    return (
        <Modal visible={visible} animationType="slide" transparent={false}>
            <SafeAreaView style={styles.safeArea}>
                <View style={styles.header}>
                    <TouchableOpacity onPress={onClose} style={styles.headerBtn}>
                        <CustomText style={styles.headerBtnText}>← 뒤로</CustomText>
                    </TouchableOpacity>
                    <CustomText style={styles.title}>페이스 페인팅</CustomText>
                    <TouchableOpacity onPress={handleSave} style={styles.headerBtn}>
                        <CustomText style={[styles.headerBtnText, { color: '#3B82F6' }]}>저장</CustomText>
                    </TouchableOpacity>
                </View>

                {/* 그리기 영역 (얼굴 가이드라인) */}
                <View style={styles.canvasContainer}>
                    <View style={styles.guideWrapper}>
                        <ChildAvatar equipState={equipState} size={width - scale(40)} />
                    </View>
                    <View style={styles.drawingArea} {...panResponder.panHandlers}>
                        <Svg width="100%" height="100%">
                            {paths.map((p, index) => (
                                <Path
                                    key={`path-${index}`}
                                    d={p.path}
                                    stroke={p.color}
                                    strokeWidth={p.strokeWidth}
                                    strokeLinecap="round"
                                    strokeLinejoin="round"
                                    fill="none"
                                />
                            ))}
                            {currentPath && (
                                <Path
                                    d={currentPath.path}
                                    stroke={currentPath.color}
                                    strokeWidth={currentPath.strokeWidth}
                                    strokeLinecap="round"
                                    strokeLinejoin="round"
                                    fill="none"
                                />
                            )}
                        </Svg>
                    </View>
                </View>

                {/* 색상 팔레트 및 도구 */}
                <View style={styles.toolsContainer}>
                    <View style={styles.colorPalette}>
                        {COLORS.map(c => (
                            <TouchableOpacity
                                key={c}
                                style={[styles.colorBtn, { backgroundColor: c }, color === c && !isEraser && styles.selectedColorBtn]}
                                onPress={() => { setColor(c); setIsEraser(false); }}
                            />
                        ))}
                    </View>
                    <View style={styles.actionRow}>
                        <TouchableOpacity style={[styles.toolBtn, isEraser && styles.activeTool]} onPress={() => setIsEraser(true)}>
                            <CustomText style={styles.toolIcon}>🧽</CustomText>
                        </TouchableOpacity>
                        <TouchableOpacity style={styles.toolBtn} onPress={clearCanvas}>
                            <CustomText style={styles.toolIcon}>🗑️</CustomText>
                        </TouchableOpacity>
                    </View>
                </View>
            </SafeAreaView>
        </Modal>
    );
};

const styles = StyleSheet.create({
    safeArea: { flex: 1, backgroundColor: '#F9FAFB' },
    header: { flexDirection: 'row', justifyContent: 'space-between', padding: scale(16), backgroundColor: '#FFF', alignItems: 'center' },
    headerBtn: { padding: scale(8) },
    headerBtnText: { fontSize: scale(16), fontWeight: 'bold', color: '#111' },
    title: { fontSize: scale(18), fontWeight: 'bold', color: '#111' },
    canvasContainer: {
        width: width - scale(40),
        height: width - scale(40), // 정사각형 캔버스
        alignSelf: 'center',
        marginTop: verticalScale(40),
        backgroundColor: '#FFF',
        borderRadius: scale(24),
        overflow: 'hidden',
        position: 'relative',
        shadowColor: '#000', shadowOffset: { width: 0, height: 4 }, shadowOpacity: 0.1, shadowRadius: 10, elevation: 5
    },
    guideWrapper: {
        ...StyleSheet.absoluteFillObject,
        justifyContent: 'center',
        alignItems: 'center',
        backgroundColor: '#F9FAFB',
    },
    drawingArea: {
        ...StyleSheet.absoluteFillObject,
        zIndex: 10,
    },
    toolsContainer: {
        marginTop: verticalScale(40),
        paddingHorizontal: scale(20),
        alignItems: 'center',
    },
    colorPalette: {
        flexDirection: 'row',
        flexWrap: 'wrap',
        justifyContent: 'center',
        gap: scale(12),
        marginBottom: verticalScale(20),
    },
    colorBtn: {
        width: scale(36),
        height: scale(36),
        borderRadius: scale(18),
        borderWidth: 2,
        borderColor: '#E5E7EB',
    },
    selectedColorBtn: {
        borderColor: '#111',
        transform: [{ scale: 1.1 }]
    },
    actionRow: {
        flexDirection: 'row',
        gap: scale(20)
    },
    toolBtn: {
        width: scale(56),
        height: scale(56),
        borderRadius: scale(28),
        backgroundColor: '#FFF',
        justifyContent: 'center',
        alignItems: 'center',
        shadowColor: '#000', shadowOffset: { width: 0, height: 2 }, shadowOpacity: 0.1, shadowRadius: 4, elevation: 2
    },
    activeTool: {
        backgroundColor: '#E5E7EB',
        borderWidth: 2,
        borderColor: '#111'
    },
    toolIcon: {
        fontSize: scale(24)
    }
});

export default FacePaintingModal;
