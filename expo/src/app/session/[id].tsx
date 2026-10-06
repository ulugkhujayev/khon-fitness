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
import { useFieldFocus } from '@/components/session/useFieldFocus';
import { Capsule, PrimaryButton, TextButton } from '@/components/ui';
import { setKey } from '@/domain/session';
import { Kind, SetLog, SetStatus } from '@/domain/types';
import { blockTitle, doneCount, firstOpenBlock, groupBlocks, isBlockDone, nextPending, Summary, summarize } from '@/domain/workout';
import { usePreviousLogs, useSession } from '@/db/hooks';
import * as repo from '@/db/repository';
import { K, Space } from '@/theme';

/**
 * A program session (SessionScreen.kt): live logging while unfinished, read-only with an Edit toggle after.
 * Every tap saves. One tap on the circle logs a set as shown, then focus moves to the next set's kg field.
 */
export default function SessionScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const insets = useSafeAreaInsets();
  const { data } = useSession(id);
  const previous = usePreviousLogs(id);
  const fields = useFieldFocus();
  const [editing, setEditing] = useState(false);
  // Undefined until the user opens or closes a block: then the first block with a pending set is open.
  const [chosen, setChosen] = useState<number | null | undefined>(undefined);
  const [swap, setSwap] = useState<SwapState | null>(null);
  const [swapOpen, setSwapOpen] = useState(false);
  const [summary, setSummary] = useState<Summary | null>(null);
  const [summaryOpen, setSummaryOpen] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const leave = useLeaveWhenGone(data != null && data.session == null);

  const logs = data?.logs ?? [];
  const blocks = groupBlocks(logs);
  const expanded = chosen !== undefined ? chosen : blocks.length > 0 ? firstOpenBlock(blocks) : null;

  const s = data?.session;
  if (!s) return <View style={styles.safe} />;
  const finished = s.finishedAt != null;
  const live = !finished || editing;

  const toggle = (log: SetLog) => {
    const next = log.status === SetStatus.DONE ? SetStatus.PENDING : SetStatus.DONE;
    repo.patchSetLog(log.id, { status: next });
    if (next !== SetStatus.DONE) return;
    const n = nextPending(logs, log);
    // Pin the open block, so finishing a block never moves the view on its own.
    setChosen(n ? blocks.findIndex((b) => b[0].blockIndex === n.blockIndex) : expanded);
    if (n) fields.focus(n.id);
  };

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
            onToggle={() => setChosen(expanded === bi ? null : bi)}>
            {blockLogs.map((log) => (
              <SetRow
                key={log.id}
                log={log}
                prev={previous.get(setKey(log))}
                live={live}
                inputRef={fields.register(log.id)}
                onToggle={() => toggle(log)}
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
        <TextButton text={finished ? 'Delete session' : 'Discard workout'} color={K.Red} onPress={() => setConfirmDelete(true)} style={styles.delete} />
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
  delete: { alignSelf: 'flex-start', marginTop: 20 },
});
