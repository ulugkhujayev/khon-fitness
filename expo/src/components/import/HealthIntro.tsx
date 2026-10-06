import { Linking, StyleSheet } from 'react-native';
import { ContentEmpty, GroupedList, PrimaryButton, TextButton } from '@/components/ui';
import { HealthStatus } from '@/health/types';
import { K } from '@/theme';

/** The health store's symbol, in the red of a heart. */
const HEART = { ios: 'heart.text.square.fill', android: 'monitor_heart', web: 'monitor_heart' } as const;

/**
 * What stands between the import and the data (ImportScreen.kt), on one card: no store, an outdated one, or
 * reading not yet allowed. One action each: get or update the store, or ask to read.
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
  const again = <TextButton text="Check again" onPress={onCheckAgain} style={styles.again} />;
  return (
    <GroupedList>
      {status === 'notInstalled' ? (
        <ContentEmpty icon={HEART} color={K.Red} title={`Get ${name}`} text={`The band's app writes workouts to ${name}, and this app reads them from there.`}>
          <PrimaryButton text={`Get ${name}`} onPress={openStore} />
          {again}
        </ContentEmpty>
      ) : status === 'updateRequired' ? (
        <ContentEmpty icon={HEART} color={K.Red} title={`Update ${name}`} text={`${name} needs an update before it can share data.`}>
          <PrimaryButton text={`Update ${name}`} onPress={openStore} />
          {again}
        </ContentEmpty>
      ) : status === 'unsupported' ? (
        <ContentEmpty icon={HEART} title="No workout data" text="This device keeps no workout data to import." />
      ) : (
        <ContentEmpty icon={HEART} color={K.Red} title="Read your workouts" text={`Allow this app to read workouts, heart rate and distance from ${name}. Nothing is written back.`}>
          <PrimaryButton text="Allow" onPress={onAllow} />
        </ContentEmpty>
      )}
    </GroupedList>
  );
}

const styles = StyleSheet.create({
  again: { alignSelf: 'center' },
});
