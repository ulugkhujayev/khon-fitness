import { useEffect, useRef, useState } from 'react';
import { TextInput } from 'react-native';

/**
 * Moves focus to a field by id once it is on screen. Fields register through `register(id)`;
 * `focus(id)` waits a frame, so a block that opens in the same update has mounted its fields.
 */
export function useFieldFocus() {
  const inputs = useRef(new Map<string, TextInput>());
  const [pending, setPending] = useState<string | null>(null);

  useEffect(() => {
    if (!pending) return;
    const timer = setTimeout(() => {
      inputs.current.get(pending)?.focus();
      setPending(null);
    }, 60);
    return () => clearTimeout(timer);
  }, [pending]);

  const register = (id: string) => (input: TextInput | null) => {
    if (input) inputs.current.set(id, input);
    else inputs.current.delete(id);
  };

  return { register, focus: setPending };
}
