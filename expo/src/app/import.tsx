import { useState } from 'react';
import { StyleSheet, Text } from 'react-native';
import { DraftList } from '@/components/import/DraftList';
import { DraftSheet } from '@/components/import/DraftSheet';
import { HealthIntro } from '@/components/import/HealthIntro';
import { EditorScreen } from '@/components/layout';
import { TextButton } from '@/components/ui';
import { defaultExercise, draftDate, ImportDraft, importDrafts } from '@/domain/health';
import { itemFor } from '@/domain/plan';
import { useImportData } from '@/db/hooks';
import * as repo from '@/db/repository';
import { healthSource } from '@/health/source';
import { useHealthImport } from '@/health/useHealthImport';
import { K, Type } from '@/theme';

/**
 * Import (ImportScreen.kt): band and watch workouts from Health Connect or Apple Health become cardio sessions,
 * one tap each or all matched at once. Imported ones never show again.
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
          {health.error ? <Text style={styles.error}>{health.error}</Text> : null}
          {!list ? (
            <Text style={styles.text}>Reading…</Text>
          ) : list.length === 0 ? (
            <Text style={styles.text}>Nothing new in the last 60 days. Sessions already imported do not show again.</Text>
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
            <Text style={styles.note}>{`${strength} strength ${strength === 1 ? 'workout' : 'workouts'} from the watch not shown. Gym sessions are logged in the app.`}</Text>
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
  text: { ...Type.subheadline, color: K.Muted },
  error: { ...Type.subheadline, color: K.Red, marginBottom: 12 },
  note: { ...Type.footnote, color: K.Dim, marginTop: 16 },
  debug: { alignSelf: 'flex-start', marginTop: 32 },
});
