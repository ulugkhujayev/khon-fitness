import { ReactNode, useEffect, useRef, useState } from 'react';
import { Animated, GestureResponderEvent, Modal, Pressable, ScrollView, StyleSheet, Text, useWindowDimensions, View } from 'react-native';
import { KeyboardAvoidingView } from 'react-native-keyboard-controller';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { K, Type } from '@/theme';
import { Check, ListRow, PrimaryButton, TextButton } from './ui';

/**
 * Bottom sheet (DESIGN.md): top radius 24, a 36 x 5 grabber, the sheet on the group surface,
 * groups inside on the elevated fill. Swipe the top down or tap outside to dismiss.
 * It slides out after `visible` turns false, so callers keep the data the content shows until it opens again.
 */
export function Sheet({
  visible,
  title,
  onDismiss,
  children,
}: {
  visible: boolean;
  title?: string;
  onDismiss: () => void;
  children: ReactNode;
}) {
  const insets = useSafeAreaInsets();
  const { height } = useWindowDimensions();
  const [mounted, setMounted] = useState(visible);
  if (visible && !mounted) setMounted(true);
  const [shift] = useState(() => new Animated.Value(height));
  const [fade] = useState(() => new Animated.Value(0));

  useEffect(() => {
    if (visible) {
      shift.setValue(height);
      Animated.parallel([
        Animated.spring(shift, { toValue: 0, useNativeDriver: true, bounciness: 0, speed: 16 }),
        Animated.timing(fade, { toValue: 1, duration: 200, useNativeDriver: true }),
      ]).start();
    } else {
      Animated.parallel([
        Animated.timing(shift, { toValue: height, duration: 220, useNativeDriver: true }),
        Animated.timing(fade, { toValue: 0, duration: 220, useNativeDriver: true }),
      ]).start(({ finished }) => finished && setMounted(false));
    }
  }, [visible, height, shift, fade]);

  // Raw touch events on the header: the responder system hands moves to the screen's scroll view when the
  // sheet is rendered inside one, so a PanResponder here never gets the drag.
  const drag = useRef({ y0: 0, y: 0, t: 0, vy: 0 });
  const onTouchStart = (e: GestureResponderEvent) => {
    const { pageY, timestamp } = e.nativeEvent;
    drag.current = { y0: pageY, y: pageY, t: timestamp, vy: 0 };
  };
  const onTouchMove = (e: GestureResponderEvent) => {
    const { pageY, timestamp } = e.nativeEvent;
    const d = drag.current;
    if (timestamp > d.t) d.vy = (pageY - d.y) / (timestamp - d.t);
    d.y = pageY;
    d.t = timestamp;
    shift.setValue(Math.max(0, pageY - d.y0));
  };
  const onTouchEnd = () => {
    const d = drag.current;
    if (d.y - d.y0 > 80 || d.vy > 0.8) onDismiss();
    else Animated.spring(shift, { toValue: 0, useNativeDriver: true, bounciness: 0 }).start();
  };

  return (
    <Modal
      visible={mounted}
      transparent
      animationType="none"
      statusBarTranslucent
      navigationBarTranslucent
      onRequestClose={onDismiss}>
      <Animated.View style={[StyleSheet.absoluteFill, styles.backdrop, { opacity: fade }]}>
        <Pressable style={StyleSheet.absoluteFill} onPress={onDismiss} accessibilityLabel="Close" />
      </Animated.View>
      {/* The sheet shrinks and scrolls when the keyboard takes room, instead of sliding off the top. */}
      <KeyboardAvoidingView behavior="padding" style={[styles.bottom, { paddingTop: insets.top + 24 }]} pointerEvents="box-none">
        <Animated.View style={[styles.sheet, { paddingBottom: insets.bottom + 16, transform: [{ translateY: shift }] }]}>
          {/* A real native view: a flattened one cannot be a touch target. */}
          <View collapsable={false} onTouchStart={onTouchStart} onTouchMove={onTouchMove} onTouchEnd={onTouchEnd} onTouchCancel={onTouchEnd}>
            <View style={styles.grabber} />
            {title ? (
              <Text style={[Type.title2, styles.title]} accessibilityRole="header">
                {title}
              </Text>
            ) : (
              <View style={{ height: 12 }} />
            )}
          </View>
          <ScrollView
            style={styles.scroll}
            contentContainerStyle={styles.content}
            keyboardShouldPersistTaps="handled"
            showsVerticalScrollIndicator={false}>
            {children}
          </ScrollView>
        </Animated.View>
      </KeyboardAvoidingView>
    </Modal>
  );
}

/** Group header inside a sheet. */
export function SheetGroupTitle({ text }: { text: string }) {
  return <Text style={[Type.footnote, styles.groupTitle]}>{text}</Text>;
}

/** Choice list inside a sheet: rows on the elevated fill. */
export function ChoiceList({ children }: { children: ReactNode }) {
  return <View style={styles.choices}>{children}</View>;
}

/** A choice: optional color dot, the name, a check when selected. */
export function ChoiceRow({
  title,
  selected,
  dot,
  dotFilled = true,
  last = false,
  onPress,
}: {
  title: string;
  selected: boolean;
  dot?: string;
  dotFilled?: boolean;
  last?: boolean;
  onPress: () => void;
}) {
  return (
    <ListRow
      title={title}
      dot={dot}
      dotFilled={dotFilled}
      chevron={false}
      last={last}
      elevated
      selected={selected}
      trailing={selected ? <Check /> : null}
      onPress={onPress}
    />
  );
}

/** Confirm for a destructive action: what goes, a red button, Cancel. */
export function ConfirmSheet({
  visible,
  title,
  body,
  action,
  onDismiss,
  onConfirm,
}: {
  visible: boolean;
  title: string;
  body: string;
  action: string;
  onDismiss: () => void;
  onConfirm: () => void;
}) {
  return (
    <Sheet visible={visible} title={title} onDismiss={onDismiss}>
      <Text style={[Type.subheadline, { color: K.Muted }]}>{body}</Text>
      <PrimaryButton text={action} color={K.Red} onPress={onConfirm} style={{ marginTop: 16 }} />
      <View style={{ alignItems: 'center', marginTop: 8 }}>
        <TextButton text="Cancel" color={K.Muted} onPress={onDismiss} />
      </View>
    </Sheet>
  );
}

const styles = StyleSheet.create({
  backdrop: { backgroundColor: 'rgba(0,0,0,0.55)' },
  bottom: { flex: 1, justifyContent: 'flex-end' },
  sheet: { flexShrink: 1, backgroundColor: K.Surface, borderTopLeftRadius: 24, borderTopRightRadius: 24 },
  scroll: { flexGrow: 0, flexShrink: 1 },
  grabber: { alignSelf: 'center', width: 36, height: 5, borderRadius: 3, backgroundColor: K.Dim, marginTop: 6, marginBottom: 2 },
  title: { paddingHorizontal: 16, paddingTop: 10, paddingBottom: 12 },
  content: { paddingHorizontal: 16, paddingBottom: 8 },
  groupTitle: { color: K.Muted, paddingLeft: 16, paddingTop: 14, paddingBottom: 6 },
  choices: { backgroundColor: K.Elevated, borderRadius: 20, overflow: 'hidden' },
});
