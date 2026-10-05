import { ReactNode, useEffect, useRef, useState } from 'react';
import { Pressable, StyleSheet, Switch, Text, TextInput, useWindowDimensions, View, ViewStyle } from 'react-native';
import { SymbolView, SymbolViewProps } from 'expo-symbols';
import { Font, K, Space, Type } from '@/theme';

export { fmt } from '@/domain/format';

/** Large title at the top of a scrolling tab screen, bar items trailing. */
export function ScreenTitle({ text, trailing }: { text: string; trailing?: ReactNode }) {
  return (
    <View style={styles.titleRow}>
      <Text style={[Type.largeTitle, { flex: 1 }]} accessibilityRole="header">
        {text}
      </Text>
      {trailing}
    </View>
  );
}

/** Grouped section header: Footnote in secondary label, inset 16. */
export function SectionTitle({ text, trailing, style }: { text: string; trailing?: ReactNode; style?: ViewStyle }) {
  return (
    <View style={[styles.sectionRow, style]}>
      <Text style={[Type.footnote, { color: K.Muted, flex: 1 }]}>{text}</Text>
      {trailing}
    </View>
  );
}

export function TextButton({
  text,
  color = K.Accent,
  enabled = true,
  onPress,
  label,
}: {
  text: string;
  color?: string;
  enabled?: boolean;
  onPress: () => void;
  label?: string;
}) {
  return (
    <Pressable
      onPress={onPress}
      disabled={!enabled}
      accessibilityRole="button"
      accessibilityLabel={label ?? text}
      accessibilityState={{ disabled: !enabled }}
      hitSlop={4}
      style={({ pressed }) => [styles.textButton, pressed && { opacity: 0.5 }]}>
      <Text style={[Type.body, { fontFamily: Font.medium, color: enabled ? color : K.Dim }]}>{text}</Text>
    </Pressable>
  );
}

/** Round "+" bar or section action with a 44 pt target. */
export function PlusButton({ label = 'Add', onPress }: { label?: string; onPress: () => void }) {
  return (
    <Pressable
      onPress={onPress}
      accessibilityRole="button"
      accessibilityLabel={label}
      style={({ pressed }) => [styles.plus, pressed && { opacity: 0.5 }]}>
      <SymbolView name={{ ios: 'plus', android: 'add', web: 'add' }} tintColor={K.Accent} size={22} />
    </Pressable>
  );
}

/** 36 pt round bar button on a 44 pt target, a symbol in accent (gear, plus). */
export function BarCircleButton({ icon, label, onPress }: { icon: SymbolViewProps['name']; label: string; onPress: () => void }) {
  return (
    <Pressable onPress={onPress} accessibilityRole="button" accessibilityLabel={label} style={styles.barTarget}>
      {({ pressed }) => (
        <View style={[styles.barCircle, pressed && { opacity: 0.6 }]}>
          <SymbolView name={icon} tintColor={K.Accent} size={18} />
        </View>
      )}
    </Pressable>
  );
}

/** The trailing bar action as a 34 pt accent capsule: Done, Edit, Finish. */
export function Capsule({ text, onPress }: { text: string; onPress: () => void }) {
  return (
    <Pressable
      onPress={onPress}
      accessibilityRole="button"
      hitSlop={6}
      style={({ pressed }) => [styles.capsule, pressed && { opacity: 0.8 }]}>
      <Text style={styles.capsuleText}>{text}</Text>
    </Pressable>
  );
}

/** One prominent button per screen: 50 pt full-width accent capsule. */
export function PrimaryButton({
  text,
  enabled = true,
  onPress,
  style,
  color = K.Accent,
}: {
  text: string;
  enabled?: boolean;
  onPress: () => void;
  style?: ViewStyle;
  /** K.Red for a destructive confirm. */
  color?: string;
}) {
  return (
    <Pressable
      onPress={onPress}
      disabled={!enabled}
      accessibilityRole="button"
      accessibilityState={{ disabled: !enabled }}
      style={({ pressed }) => [
        styles.primary,
        { backgroundColor: enabled ? color : K.Surface2, opacity: pressed ? 0.8 : 1 },
        style,
      ]}>
      <Text style={[Type.headline, { color: enabled ? K.AccentInk : K.Muted }]}>{text}</Text>
    </Pressable>
  );
}

/** iOS-style inset grouped list. Rows draw their own inset separators. */
export function GroupedList({ children, style }: { children: ReactNode; style?: ViewStyle }) {
  return <View style={[styles.group, style]}>{children}</View>;
}

/** A color shown as a filled circle, or a ring for low intensity. */
export function Dot({ color, filled = true, size = 12 }: { color: string; filled?: boolean; size?: number }) {
  return (
    <View
      style={[
        { width: size, height: size, borderRadius: size / 2 },
        filled ? { backgroundColor: color } : { borderWidth: 2, borderColor: color },
      ]}
    />
  );
}

export function Chevron() {
  return (
    <SymbolView
      name={{ ios: 'chevron.right', android: 'chevron_right', web: 'chevron_right' }}
      tintColor={K.Dim}
      size={14}
      style={{ marginLeft: 8 }}
    />
  );
}

/** Accent check on a selected choice row. */
export function Check({ color = K.Accent, size = 18 }: { color?: string; size?: number }) {
  return <SymbolView name={{ ios: 'checkmark', android: 'check', web: 'check' }} tintColor={color} size={size} weight="bold" />;
}

/**
 * List row: optional color dot, primary text, spacer, optional value, optional trailing, optional chevron.
 * Tapping the row opens its editor; no buttons inside rows.
 */
export function ListRow({
  title,
  value,
  dot,
  dotFilled = true,
  chevron = true,
  last = false,
  color = K.Text,
  trailing,
  selected,
  elevated = false,
  onPress,
}: {
  title: string;
  /** Title color; K.Red for destructive rows, K.Accent for plain actions, K.Dim for archived. */
  color?: string;
  value?: string;
  dot?: string;
  /** Filled for an active or high-intensity item, a ring otherwise. */
  dotFilled?: boolean;
  chevron?: boolean;
  last?: boolean;
  trailing?: ReactNode;
  /** Set on choice rows so screen readers hear the state. */
  selected?: boolean;
  /** On the elevated fill inside a sheet. */
  elevated?: boolean;
  onPress?: () => void;
}) {
  return (
    <Pressable
      onPress={onPress}
      disabled={!onPress}
      accessibilityRole={onPress ? (selected !== undefined ? 'radio' : 'button') : undefined}
      accessibilityState={selected !== undefined ? { selected } : undefined}
      style={({ pressed }) => [styles.row, pressed && { backgroundColor: elevated ? K.Surface3 : K.Surface2 }]}>
      {dot ? (
        <View style={styles.dotCol}>
          <Dot color={dot} filled={dotFilled} />
        </View>
      ) : null}
      <View style={[styles.rowInner, !last && styles.separator]}>
        <Text style={[Type.body, { flex: 1, color, paddingVertical: 12 }]} numberOfLines={2}>
          {title}
        </Text>
        {value ? (
          <Text style={[Type.subheadline, { color: K.Muted, marginLeft: 8, flexShrink: 1 }]} numberOfLines={1}>
            {value}
          </Text>
        ) : null}
        {trailing}
        {chevron && onPress ? <Chevron /> : null}
      </View>
    </Pressable>
  );
}

/** Editor field row: label on the left, value or control on the right. A hairline above unless first. */
export function FieldRow({
  label,
  divider = true,
  onPress,
  children,
}: {
  label: string;
  divider?: boolean;
  onPress?: () => void;
  children: ReactNode;
}) {
  const { width } = useWindowDimensions();
  return (
    <Pressable
      onPress={onPress}
      disabled={!onPress}
      accessibilityRole={onPress ? 'button' : undefined}
      accessibilityLabel={onPress ? label : undefined}
      style={({ pressed }) => [pressed && { backgroundColor: K.Surface2 }]}>
      {divider ? <View style={styles.fieldDivider} /> : null}
      <View style={styles.field}>
        <Text style={[Type.body, { width: width < 400 ? 104 : 128 }]}>{label}</Text>
        <View style={styles.fieldValue}>{children}</View>
      </View>
    </Pressable>
  );
}

/** A select's current value in a field row, with a chevron: the row opens a sheet. */
export function FieldValue({ text, color = K.Text }: { text: string; color?: string }) {
  return (
    <>
      <Text style={[Type.body, { fontFamily: Font.semibold, color, flexShrink: 1, textAlign: 'right' }]} numberOfLines={2}>
        {text}
      </Text>
      <Chevron />
    </>
  );
}

/**
 * Text field that edits inline in a field row. Keeps its own text while focused so a database
 * round trip never fights the keyboard; outside changes land only while it is not focused.
 */
export function InlineTextField({
  value,
  onChange,
  placeholder = '',
  label,
}: {
  value: string;
  onChange: (v: string) => void;
  placeholder?: string;
  label?: string;
}) {
  const [text, setText] = useState(value);
  const focused = useRef(false);
  useEffect(() => {
    if (!focused.current) setText(value);
  }, [value]);
  return (
    <TextInput
      value={text}
      onChangeText={(t) => {
        setText(t);
        onChange(t);
      }}
      onFocus={() => (focused.current = true)}
      onBlur={() => (focused.current = false)}
      placeholder={placeholder}
      placeholderTextColor={K.Dim}
      accessibilityLabel={label}
      selectionColor={K.Accent}
      keyboardAppearance="dark"
      style={[Type.body, styles.inlineInput]}
    />
  );
}

/** 51 x 31 toggle: systemGreen on, Surface3 off, white thumb. */
export function Toggle({ value, onChange, label }: { value: boolean; onChange: (v: boolean) => void; label?: string }) {
  return (
    <Switch
      value={value}
      onValueChange={onChange}
      accessibilityLabel={label}
      trackColor={{ false: K.Surface3, true: K.Green }}
      thumbColor="#FFFFFF"
      ios_backgroundColor={K.Surface3}
    />
  );
}

/** Secondary and destructive actions at the bottom of an editor, as text buttons in one row. */
export function BottomActions({ children }: { children: ReactNode }) {
  return <View style={styles.bottomActions}>{children}</View>;
}

/** Empty state: one muted line, no illustration. */
export function Empty({ text }: { text: string }) {
  return <Text style={[Type.subheadline, { color: K.Muted, paddingHorizontal: 16, paddingVertical: 14 }]}>{text}</Text>;
}

const styles = StyleSheet.create({
  titleRow: { flexDirection: 'row', alignItems: 'center', paddingTop: 4, paddingBottom: 12 },
  sectionRow: { flexDirection: 'row', alignItems: 'center', minHeight: 32, paddingLeft: 16, paddingBottom: 6 },
  textButton: { minHeight: Space.tapMin, justifyContent: 'center', paddingHorizontal: 4 },
  plus: { width: 44, height: 44, borderRadius: 22, alignItems: 'center', justifyContent: 'center' },
  barTarget: { width: 44, height: 44, alignItems: 'center', justifyContent: 'center' },
  barCircle: { width: 36, height: 36, borderRadius: 18, backgroundColor: K.Surface2, alignItems: 'center', justifyContent: 'center' },
  capsule: { height: 34, borderRadius: 17, backgroundColor: K.Accent, paddingHorizontal: 14, alignItems: 'center', justifyContent: 'center' },
  capsuleText: { fontFamily: Font.semibold, fontSize: 15, color: K.AccentInk },
  primary: { height: Space.buttonHeight, borderRadius: 25, alignItems: 'center', justifyContent: 'center', alignSelf: 'stretch' },
  group: { backgroundColor: K.Surface, borderRadius: Space.groupRadius, overflow: 'hidden' },
  row: { flexDirection: 'row', alignItems: 'center', paddingLeft: 16 },
  rowInner: { flex: 1, flexDirection: 'row', alignItems: 'center', minHeight: Space.rowMin, paddingRight: 16 },
  separator: { borderBottomWidth: StyleSheet.hairlineWidth, borderBottomColor: K.Divider },
  dotCol: { width: 24 },
  field: { flexDirection: 'row', alignItems: 'center', minHeight: Space.rowMin, paddingHorizontal: 16, gap: 12 },
  fieldValue: { flex: 1, flexDirection: 'row', justifyContent: 'flex-end', alignItems: 'center' },
  fieldDivider: { height: StyleSheet.hairlineWidth, backgroundColor: K.Divider, marginLeft: 16 },
  inlineInput: { flex: 1, textAlign: 'right', paddingVertical: 13, color: K.Text },
  bottomActions: { flexDirection: 'row', flexWrap: 'wrap', alignItems: 'center', columnGap: 22, paddingTop: 28, paddingLeft: 2 },
});
