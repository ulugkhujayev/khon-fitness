import { Fragment, useState } from 'react';
import { StyleSheet, View } from 'react-native';
import { useLocalSearchParams } from 'expo-router';
import { KeyboardAwareScrollView } from 'react-native-keyboard-controller';
import Animated from 'react-native-reanimated';
import { SafeAreaView, useSafeAreaInsets } from 'react-native-safe-area-context';
import { NavBar, useLeaveWhenGone } from '@/components/layout';
import { BlockBody } from '@/components/session/BlockBody';
import { BlockHeader } from '@/components/session/BlockHeader';
import { DeleteSessionSheet } from '@/components/session/DeleteSessionSheet';
import { SessionFooter } from '@/components/session/SessionFooter';
import { SessionHeader } from '@/components/session/SessionHeader';
import { SetRow } from '@/components/session/SetRow';
import { SummarySheet } from '@/components/session/SummarySheet';
import { SwapSheet, SwapState } from '@/components/session/SwapSheet';
import { useBlockFlow } from '@/components/session/useBlockFlow';
import { useFieldFocus } from '@/components/session/useFieldFocus';
import { Capsule } from '@/components/ui';
import { setKey } from '@/domain/session';
import { Kind } from '@/domain/types';
import { doneCount, Summary, summarize } from '@/domain/workout';
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
  const { blocks, open, toggleBlock, toggleSet, scrollRef, onScroll, pinned, pinStyle, onHeaderLayout, onBodyLayout } = useBlockFlow(logs, fields.focus);
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

  // A block's header: in the list, or (`pinnedCopy`) the copy held at the top while its sets scroll.
  const header = (bi: number, pinnedCopy = false) => (
    <BlockHeader index={bi} logs={blocks[bi]} open={open[bi]} onToggle={() => toggleBlock(bi)} onLayout={pinnedCopy ? undefined : (e) => onHeaderLayout(bi, e)} />
  );

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
      <View style={styles.list}>
        <KeyboardAwareScrollView
          ref={scrollRef}
          bottomOffset={24}
          keyboardShouldPersistTaps="handled"
          showsVerticalScrollIndicator={false}
          onScroll={onScroll}
          contentContainerStyle={[styles.content, { paddingBottom: insets.bottom + 24 }]}>
          {blocks.map((blockLogs, bi) => (
            <Fragment key={blockLogs[0].blockIndex}>
              {header(bi)}
              {open[bi] ? (
                <BlockBody onLayout={(e) => onBodyLayout(bi, e)}>
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
                </BlockBody>
              ) : null}
            </Fragment>
          ))}
          <SessionFooter session={s} live={live} onChange={(patch) => repo.patchSession(s.id, patch)} onFinish={openSummary} onDelete={() => setConfirmDelete(true)} />
        </KeyboardAwareScrollView>
        {/* The list's header serves screen readers; the pinned copy is for the eye and the thumb. */}
        {pinned != null ? (
          <Animated.View style={[styles.pin, pinStyle]} importantForAccessibility="no-hide-descendants" accessibilityElementsHidden>
            {header(pinned, true)}
          </Animated.View>
        ) : null}
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
  pin: { position: 'absolute', top: 0, left: Space.margin, right: Space.margin },
});
