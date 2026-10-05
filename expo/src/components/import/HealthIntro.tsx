import { Linking, StyleSheet, Text } from 'react-native';
import { PrimaryButton, TextButton } from '@/components/ui';
import { HealthStatus } from '@/health/types';
import { K, Type } from '@/theme';

/**
 * What stands between the import and the data (ImportScreen.kt): no store, an outdated one, or reading not yet
 * allowed. One action each: get or update the store, or ask to read.
 */
export function HealthIntro({
  name,
  status,
  storeUrl,
  onAllow,
  onCheckAgain,
}: {
  /** "Health Connect" or "Apple Health". */
  name: string;
  status: HealthStatus;
  storeUrl: string | null;
  onAllow: () => void;
  onCheckAgain: () => void;
}) {
  const openStore = () => {
    if (storeUrl) void Linking.openURL(storeUrl).catch(() => undefined);
  };
  switch (status) {
    case 'notInstalled':
      return (
        <>
          <Text style={styles.text}>{`${name} is not on this phone. The band's app writes workouts there, and this app reads them.`}</Text>
          <PrimaryButton text={`Get ${name}`} onPress={openStore} style={styles.action} />
          <TextButton text="Check again" onPress={onCheckAgain} style={styles.again} />
        </>
      );
    case 'updateRequired':
      return (
        <>
          <Text style={styles.text}>{`${name} needs an update before it can share data.`}</Text>
          <PrimaryButton text={`Update ${name}`} onPress={openStore} style={styles.action} />
          <TextButton text="Check again" onPress={onCheckAgain} style={styles.again} />
        </>
      );
    case 'unsupported':
      return <Text style={styles.text}>This device keeps no workout data to import.</Text>;
    case 'available':
      return (
        <>
          <Text style={styles.text}>{`Allow this app to read workouts, heart rate, and distance from ${name}. Nothing is written back.`}</Text>
          <PrimaryButton text="Allow" onPress={onAllow} style={styles.action} />
        </>
      );
  }
}

const styles = StyleSheet.create({
  text: { ...Type.subheadline, color: K.Muted },
  action: { marginTop: 16 },
  again: { alignSelf: 'flex-start', marginTop: 12 },
});
