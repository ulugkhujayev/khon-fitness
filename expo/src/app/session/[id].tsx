import { useState } from 'react';
import { StyleSheet, View } from 'react-native';
import { useLocalSearchParams } from 'expo-router';
import { KeyboardAwareScrollView } from 'react-native-keyboard-controller';
import { SafeAreaView, useSafeAreaInsets } from 'react-native-safe-area-context';
import { NavBar, useLeaveWhenGone, useScrollY } from '@/components/layout';
import { BlockCard } from '@/components/session/BlockCard';
import { DeleteSessionSheet } from '@/components/session/DeleteSessionSheet';
import { SessionFooter } from '@/components/session/SessionFooter';
import { SESSION_HEADER_FOLDED, SESSION_HEADER_HEIGHT, SessionHeader } from '@/components/session/SessionHeader';
import { SetRow } from '@/components/session/SetRow';
import { FinishSummary, SummarySheet } from '@/components/session/SummarySheet';
import { SwapSheet, SwapState } from '@/components/session/SwapSheet';
import { useBlockFlow } from '@/components/session/useBlockFlow';
import { useFieldFocus } from '@/components/session/useFieldFocus';
import { Capsule } from '@/components/ui';
import { setKey } from '@/domain/session';
import { Kind, SessionDetailsPatch } from '@/domain/types';
import { blockTitle, doneCount, isBlockDone, summarize, workoutTime } from '@/domain/workout';
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
  // A running session's header sits over the list; folded, it still hides the bar's height.
  const running = data?.session != null && data.session.finishedAt == null;
  const { blocks, expanded, openBlock, toggleSet, scrollRef, onBlockLayout } = useBlockFlow(logs, fields.focus, running ? SESSION_HEADER_FOLDED : 0);
  const { scrollY, onScroll } = useScrollY();
  const [editing, setEditing] = useState(false);
  const [swap, setSwap] = useState<SwapState | null>(null);
  const [swapOpen, setSwapOpen] = useState(false);
  const [summary, setSummary] = useState<FinishSummary | null>(null);
  const [summaryOpen, setSummaryOpen] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const leave = useLeaveWhenGone(data != null && data.session == null);

  const s = data?.session;
  if (!s) return <View style={styles.safe} />;
  const finished = s.finishedAt != null;
  const live = !finished || editing;

  const patchDetails = (patch: SessionDetailsPatch) => repo.patchSession(s.id, patch);

  const openSummary = async () => {
    setSummary({ stats: summarize(logs, await repo.allSetLogs(), s.id), time: workoutTime(s.startedAt, Date.now()) });
    setSummaryOpen(true);
  };

  return (
    <SafeAreaView style={styles.safe} edges={['top']}>
      {finished ? (
        <NavBar title={s.programName ?? 'Session'} trailing={<Capsule text={editing ? 'Done' : 'Edit'} onPress={() => setEditing((e) => !e)} />} />
      ) : null}
      <View style={styles.list}>
        <KeyboardAwareScrollView
          ref={scrollRef}
          bottomOffset={24}
          keyboardShouldPersistTaps="handled"
          showsVerticalScrollIndicator={false}
          onScroll={onScroll}
          contentContainerStyle={[styles.content, { paddingTop: finished ? 0 : SESSION_HEADER_HEIGHT, paddingBottom: insets.bottom + 24 }]}>
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
          <SessionFooter session={s} live={live} onChange={patchDetails} onDelete={() => setConfirmDelete(true)} />
        </KeyboardAwareScrollView>
        {/* Over the list: Finish and the progress stay in reach while the sets scroll, and it folds as they do. */}
        {!finished ? <SessionHeader title={s.programName ?? 'Session'} done={doneCount(logs)} total={logs.length} onFinish={openSummary} scrollY={scrollY} /> : null}
      </View>

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
        session={s}
        onChange={patchDetails}
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
  list: { flex: 1 },
  content: { paddingHorizontal: Space.margin },
});
