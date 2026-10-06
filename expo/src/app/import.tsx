import { useState } from 'react';
import { StyleSheet } from 'react-native';
import { DraftList } from '@/components/import/DraftList';
import { DraftSheet } from '@/components/import/DraftSheet';
import { HealthIntro } from '@/components/import/HealthIntro';
import { EditorScreen } from '@/components/layout';
import { ContentEmpty, GroupedList, GroupFooter, IconTile, ListRow, TextButton } from '@/components/ui';
import { defaultExercise, draftDate, ImportDraft, importDrafts } from '@/domain/health';
import { itemFor } from '@/domain/plan';
import { useImportData } from '@/db/hooks';
import * as repo from '@/db/repository';
import { healthSource } from '@/health/source';
import { useHealthImport } from '@/health/useHealthImport';
import { K } from '@/theme';

/**
 * Import (ImportScreen.kt): band and watch workouts from Health Connect or Apple Health become cardio sessions,
 * one tap each or all matched at once. Imported ones never show again. The source on top, then the waiting
 * sessions, or a card saying it is reading, all caught up, or what access is missing.
 */
export default function ImportScreen() {
  const { data } = useImportData();
  const health = useHealthImport();
  // The picked draft stays set while its sheet slides out.
  const [picked, setPicked] = useState<ImportDraft | null>(null);
  const [picking, setPicking] = useState(false);

  if (!data || !health.status) return <EditorScreen title="Import" />;
  const exerciseFor = (d: ImportDraft) => defaultExercise(d.modalityId, itemFor(data.plan, draftDate(d)).cardio, data.cardio);
  const drafts = health.workouts ? importDrafts(health.workouts, data.imported) : null;
  const list = drafts?.filter((d) => !d.strength) ?? null;
  const strength = (drafts?.length ?? 0) - (list?.length ?? 0);

  return (
    <EditorScreen title="Import">
      {health.status !== 'available' || !health.granted ? (
        <HealthIntro
          name={healthSource.name}
          status={health.status}
          storeUrl={healthSource.storeUrl(health.status)}
          onAllow={health.allow}
          onCheckAgain={health.refresh}
        />
      ) : (
        <>
          <GroupedList>
            <ListRow
              leading={<IconTile icon={{ ios: 'heart.fill', android: 'favorite', web: 'favorite' }} color={K.Red} />}
              title={healthSource.name}
              value="Last 60 days"
              chevron={false}
              last
            />
          </GroupedList>
          {health.error ? <GroupFooter text={health.error} error /> : null}
          {!list ? (
            <GroupedList style={styles.card}>
              <ContentEmpty icon={{ ios: 'arrow.down.circle', android: 'download', web: 'download' }} title="Reading workouts…" text={`From ${healthSource.name}.`} />
            </GroupedList>
          ) : list.length === 0 ? (
            <GroupedList style={styles.card}>
              <ContentEmpty
                icon={{ ios: 'checkmark.circle.fill', android: 'check_circle', web: 'check_circle' }}
                color={K.Green}
                title="All caught up"
                text="Nothing new in the last 60 days. Sessions already imported do not show again."
              />
            </GroupedList>
          ) : (
            <DraftList
              drafts={list}
              exerciseFor={exerciseFor}
              modalities={data.modalities}
              onPick={(d) => {
                setPicked(d);
                setPicking(true);
              }}
              onImportAll={(ready) => ready.forEach(([d, ex]) => repo.importDraft(d, ex))}
            />
          )}
          {strength > 0 ? (
            <GroupFooter text={`${strength} strength ${strength === 1 ? 'workout' : 'workouts'} from the watch not shown: gym sessions are logged in the app.`} />
          ) : null}
          {health.writeTestRun ? <TextButton text="Debug: write a test run" onPress={health.writeTestRun} style={styles.debug} /> : null}
        </>
      )}
      <DraftSheet
        visible={picking}
        draft={picked}
        current={picked ? exerciseFor(picked) : null}
        cardio={data.cardio}
        modalities={data.modalities}
        onPick={(ex) => {
          if (picked) repo.importDraft(picked, ex);
          setPicking(false);
        }}
        onDismiss={() => setPicking(false)}
      />
    </EditorScreen>
  );
}

const styles = StyleSheet.create({
  card: { marginTop: 24 },
  debug: { alignSelf: 'flex-start', marginTop: 32 },
});
