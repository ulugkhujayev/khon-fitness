import { useEffect, useRef, useState } from 'react';
import { StyleSheet, TextInput } from 'react-native';
import { K, Type } from '@/theme';

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
      style={styles.input}
    />
  );
}

const styles = StyleSheet.create({
  input: { ...Type.body, flex: 1, textAlign: 'right', paddingVertical: 13 },
});
