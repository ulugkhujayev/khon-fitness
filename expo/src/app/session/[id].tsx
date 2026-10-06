import { useState } from 'react';
import { StyleSheet, View } from 'react-native';
import { useLocalSearchParams } from 'expo-router';
import { KeyboardAwareScrollView } from 'react-native-keyboard-controller';
import { SafeAreaView, useSafeAreaInsets } from 'react-native-safe-area-context';
import { NavBar, useLeaveWhenGone } from '@/components/layout';
import { BlockCard } from '@/components/session/BlockCard';
import { DeleteSessionSheet } from '@/components/session/DeleteSessionSheet';
import { SessionDetails } from '@/components/session/SessionDetails';
import { SessionHeader } from '@/components/session/SessionHeader';
import { SetRow } from '@/components/session/SetRow';
import { SummarySheet } from '@/components/session/SummarySheet';
import { SwapSheet, SwapState } from '@/components/session/SwapSheet';
import { useBlockFlow } from '@/components/session/useBlockFlow';
import { useFieldFocus } from '@/components/session/useFieldFocus';
import { Capsule, PrimaryButton, TintedButton } from '@/components/ui';
import { setKey } from '@/domain/session';
import { Kind } from '@/domain/types';
import { blockTitle, doneCount, isBlockDone, Summary, summarize } from '@/domain/workout';
import { usePreviousLogs, useSession } from '@/db/hooks';
import * as repo from '@/db/repository';
import { K, Space } from '@/theme';

/**
 * A program session (SessionScreen.kt): live logging while unfinished, read-only with an Edit toggle after.
 * Every tap saves. One tap on the circle logs a set as shown, then focus moves on (useBlockFlow).
 */
export default function SessionScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const insets = useSafeAreaInsets();
  const { data } = useSession(id);
  const previous = usePreviousLogs(id);
  const fields = useFieldFocus();
  const logs = data?.logs ?? [];
  const { blocks, expanded, openBlock, toggleSet, scrollRef, onBlockLayout } = useBlockFlow(logs, fields.focus);
  const [editing, setEditing] = useState(false);
  const [swap, setSwap] = useState<SwapState | null>(null);
  const [swapOpen, setSwapOpen] = useState(false);
  const [summary, setSummary] = useState<Summary | null>(null);
  const [summaryOpen, setSummaryOpen] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const leave = useLeaveWhenGone(data != null && data.session == null);

  const s = data?.session;
  if (!s) return <View style={styles.safe} />;
  const finished = s.finishedAt != null;
  const live = !finished || editing;

  const openSummary = async () => {
    setSummary(summarize(logs, await repo.allSetLogs(), s.id));
    setSummaryOpen(true);
  };

  return (
    <SafeAreaView style={styles.safe} edges={['top']}>
      {/* The header stays put while the sets scroll: Finish and the progress are always in reach. */}
      {finished ? (
        <NavBar title={s.programName ?? 'Session'} trailing={<Capsule text={editing ? 'Done' : 'Edit'} onPress={() => setEditing((e) => !e)} />} />
      ) : (
        <SessionHeader title={s.programName ?? 'Session'} done={doneCount(logs)} total={logs.length} onFinish={openSummary} />
      )}
      <KeyboardAwareScrollView
        ref={scrollRef}
        bottomOffset={24}
        keyboardShouldPersistTaps="handled"
        showsVerticalScrollIndicator={false}
        contentContainerStyle={[styles.content, { paddingBottom: insets.bottom + 24 }]}>
        {blocks.map((blockLogs, bi) => (
          <BlockCard
            key={blockLogs[0].blockIndex}
            index={bi}
            title={blockTitle(blockLogs)}
            doneSets={doneCount(blockLogs)}
            setCount={blockLogs.length}
            done={isBlockDone(blockLogs)}
            open={expanded === bi}
            onToggle={() => openBlock(expanded === bi ? null : bi)}
            onLayout={(e) => onBlockLayout(bi, e)}>
            {blockLogs.map((log) => (
              <SetRow
                key={log.id}
                log={log}
                prev={previous.get(setKey(log))}
                live={live}
                inputRef={fields.register(log.id)}
                onToggle={() => toggleSet(log)}
                onWeight={(weightKg) => repo.patchSetLog(log.id, { weightKg })}
                onReps={(r) => repo.patchSetLog(log.id, { reps: r == null ? null : Math.trunc(r) })}
                onSwap={() => {
                  setSwap({ log, query: '', to: null });
                  setSwapOpen(true);
                }}
              />
            ))}
          </BlockCard>
        ))}
        <View style={styles.gap} />
        <SessionDetails session={s} live={live} onChange={(patch) => repo.patchSession(s.id, patch)} />
        <View style={styles.gap} />
        {!finished ? <PrimaryButton text="Finish workout" onPress={openSummary} /> : null}
        <TintedButton text={finished ? 'Delete session' : 'Discard workout'} color={K.Red} onPress={() => setConfirmDelete(true)} style={styles.delete} />
      </KeyboardAwareScrollView>

      <SwapSheet
        visible={swapOpen}
        state={swap}
        exercises={(data?.exercises ?? []).filter((e) => e.kind === Kind.STRENGTH && !e.archived)}
        onChange={setSwap}
        onPick={(to, updateProgram) => {
          if (swap) repo.swapExerciseInSession(s.id, swap.log.exerciseId, to, updateProgram, s.programId);
          setSwapOpen(false);
        }}
        onDismiss={() => setSwapOpen(false)}
      />
      <DeleteSessionSheet
        visible={confirmDelete}
        finished={finished}
        onDismiss={() => setConfirmDelete(false)}
        onConfirm={async () => {
          setConfirmDelete(false);
          await repo.deleteSession(s.id);
          leave();
        }}
      />
      <SummarySheet
        visible={summaryOpen}
        summary={summary}
        onDismiss={() => setSummaryOpen(false)}
        onSave={async () => {
          setSummaryOpen(false);
          await repo.finishSession(s.id);
          leave();
        }}
      />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: K.Bg },
  content: { paddingHorizontal: Space.margin },
  gap: { height: 20 },
  delete: { marginTop: 12 },
});
