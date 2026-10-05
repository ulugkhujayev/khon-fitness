import { Ref, useEffect, useRef, useState } from 'react';
import { Pressable, StyleSheet, Text, TextInput, useWindowDimensions, View } from 'react-native';
import { router, useLocalSearchParams } from 'expo-router';
import { SymbolView } from 'expo-symbols';
import { KeyboardAwareScrollView } from 'react-native-keyboard-controller';
import { SafeAreaView, useSafeAreaInsets } from 'react-native-safe-area-context';
import { SearchField } from '@/components/controls';
import { DateRow } from '@/components/DateRow';
import { NavBar } from '@/components/NavBar';
import { COMPACT_WIDTH, NumberField, NumberRow } from '@/components/NumberField';
import { ChoiceList, ChoiceRow, ConfirmSheet, Sheet } from '@/components/Sheet';
import { Capsule, FieldRow, GroupedList, InlineTextField, ListRow, PrimaryButton, TextButton } from '@/components/ui';
import { shortDate } from '@/domain/dates';
import { fmt, sets } from '@/domain/format';
import { setKey } from '@/domain/session';
import { Exercise, Kind, SetLog, SetStatus } from '@/domain/types';
import { firstOpenBlock, gain, groupBlocks, kgDecimals, nextPending, setTag, Summary, summarize } from '@/domain/workout';
import * as repo from '@/db/repository';
import { TABLES as T } from '@/db/schema';
import { useLive } from '@/db/useLive';
import { Font, K, Space, Type } from '@/theme';

const clamp = (v: number, lo: number, hi: number) => Math.min(hi, Math.max(lo, v));

/**
 * A program session (SessionScreen.kt): live logging while unfinished, read-only with an Edit toggle after.
 * Every tap saves. One tap on the circle logs a set as shown, then focus moves to the next set's kg field.
 */
export default function SessionScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const insets = useSafeAreaInsets();
  const { data } = useLive(
    async () => {
      const [session, logs, exercises] = await Promise.all([repo.session(id), repo.setLogs(id), repo.exercises()]);
      return { session, logs, exercises };
    },
    [T.sessions, T.setLogs, T.exercises],
    [id],
  );
  const [previous, setPrevious] = useState<Map<string, SetLog>>(new Map());
  const [editing, setEditing] = useState(false);
  // Undefined until the user opens or closes a block: then the first block with a pending set is open.
  const [chosen, setChosen] = useState<number | null | undefined>(undefined);
  const [focusId, setFocusId] = useState<string | null>(null);
  const [swap, setSwap] = useState<SwapState | null>(null);
  const [swapOpen, setSwapOpen] = useState(false);
  const [summary, setSummary] = useState<Summary | null>(null);
  const [summaryOpen, setSummaryOpen] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const inputs = useRef(new Map<string, TextInput>());
  const leaving = useRef(false);

  const leave = () => {
    leaving.current = true;
    if (router.canGoBack()) router.back();
    else router.replace('/');
  };

  useEffect(() => {
    repo.previousLogs(id).then(setPrevious);
  }, [id]);

  // The session went away (deleted elsewhere, or a backup replaced everything): nothing to draw here.
  useEffect(() => {
    if (data && !data.session && !leaving.current) leave();
  }, [data]);

  const logs = data?.logs ?? [];
  const blocks = groupBlocks(logs);
  const expanded = chosen !== undefined ? chosen : blocks.length > 0 ? firstOpenBlock(blocks) : null;

  useEffect(() => {
    if (!focusId) return;
    const t = setTimeout(() => {
      inputs.current.get(focusId)?.focus();
      setFocusId(null);
    }, 60);
    return () => clearTimeout(t);
  }, [focusId, expanded]);

  const s = data?.session;
  if (!s) return <View style={styles.safe} />;
  const finished = s.finishedAt != null;
  const live = !finished || editing;
  const done = logs.filter((l) => l.status === SetStatus.DONE).length;

  const toggle = (log: SetLog) => {
    const next = log.status === SetStatus.DONE ? SetStatus.PENDING : SetStatus.DONE;
    repo.patchSetLog(log.id, { status: next });
    if (next === SetStatus.DONE) {
      const n = nextPending(logs, log);
      // Pin the open block, so finishing a block never moves the view on its own.
      setChosen(n ? blocks.findIndex((b) => b[0].blockIndex === n.blockIndex) : expanded);
      if (n) setFocusId(n.id);
    }
  };

  const openSummary = async () => {
    setSummary(summarize(logs, await repo.allSetLogs(), s.id));
    setSummaryOpen(true);
  };

  const openSwap = (log: SetLog) => {
    setSwap({ log, query: '', to: null });
    setSwapOpen(true);
  };

  return (
    <SafeAreaView style={styles.safe} edges={['top']}>
      {finished ? (
        <NavBar
          title={s.programName ?? 'Session'}
          trailing={<Capsule text={editing ? 'Done' : 'Edit'} onPress={() => setEditing((e) => !e)} />}
        />
      ) : null}
      <KeyboardAwareScrollView
        bottomOffset={24}
        keyboardShouldPersistTaps="handled"
        showsVerticalScrollIndicator={false}
        contentContainerStyle={[styles.content, { paddingBottom: insets.bottom + 24 }]}>
        {!finished ? (
          <View>
            <View style={styles.topBar}>
              <BackButton />
              <View style={{ flex: 1 }} />
              <Capsule text="Finish" onPress={openSummary} />
            </View>
            <Text style={[Type.title1, { marginTop: 4 }]} accessibilityRole="header">
              {s.programName ?? 'Session'}
            </Text>
            <View style={styles.metaRow}>
              <Text style={[Type.subheadline, { color: K.Muted, flex: 1 }]}>{shortDate(s.date)}</Text>
              <Text style={[Type.subheadline, { color: K.Muted }]}>{`${done} / ${logs.length} sets`}</Text>
            </View>
            <View style={styles.track} accessibilityLabel={`${done} of ${logs.length} sets done`}>
              <View style={[styles.fill, { width: `${logs.length ? (done / logs.length) * 100 : 0}%` }]} />
            </View>
          </View>
        ) : null}

        {blocks.map((blockLogs, bi) => {
          const open = expanded === bi;
          const names = [...new Map(blockLogs.map((l) => [l.slot, l.exerciseName])).entries()]
            .sort((a, b) => a[0] - b[0])
            .map((e) => e[1])
            .join(' · ');
          const blockDone = blockLogs.every((l) => l.status !== SetStatus.PENDING);
          return (
            <View key={bi} style={[styles.block, { marginTop: bi === 0 ? 6 : 10 }]}>
              <Pressable
                onPress={() => setChosen(open ? null : bi)}
                accessibilityRole="button"
                accessibilityState={{ expanded: open }}
                accessibilityLabel={`Block ${bi + 1}, ${names}, ${sets(blockLogs.length)}${blockDone ? ', done' : ''}`}
                style={({ pressed }) => [styles.blockHeader, pressed && { backgroundColor: K.Surface2 }]}>
                <View style={styles.blockIndex}>
                  <Text style={styles.blockIndexText}>{bi + 1}</Text>
                </View>
                <Text style={[Type.body, styles.blockNames]}>{names}</Text>
                <Text style={[Type.subheadline, { color: K.Muted }]}>{sets(blockLogs.length)}</Text>
                <SymbolView
                  name={
                    blockDone
                      ? { ios: 'checkmark', android: 'check', web: 'check' }
                      : open
                        ? { ios: 'chevron.up', android: 'expand_less', web: 'expand_less' }
                        : { ios: 'chevron.right', android: 'chevron_right', web: 'chevron_right' }
                  }
                  tintColor={blockDone ? K.Green : K.Dim}
                  size={blockDone ? 18 : 14}
                  style={{ marginLeft: 10 }}
                />
              </Pressable>
              {open
                ? blockLogs.map((log) => (
                    <View key={log.id}>
                      <View style={styles.hairline} />
                      <SetRow
                        log={log}
                        prev={previous.get(setKey(log))}
                        live={live}
                        inputRef={(r) => {
                          if (r) inputs.current.set(log.id, r);
                          else inputs.current.delete(log.id);
                        }}
                        onToggle={toggle}
                        onWeight={(w) => repo.patchSetLog(log.id, { weightKg: w })}
                        onReps={(r) => repo.patchSetLog(log.id, { reps: r == null ? null : Math.trunc(r) })}
                        onSwap={openSwap}
                      />
                    </View>
                  ))
                : null}
            </View>
          );
        })}

        <View style={{ height: 20 }} />
        {live ? (
          <GroupedList>
            <DateRow date={s.date} divider={false} onChange={(date) => repo.patchSession(s.id, { date })} />
            <NumberRow
              label="Effort"
              value={s.effort}
              unit="/10"
              step={1}
              decimals={0}
              placeholder="1–10"
              onChange={(v) => repo.patchSession(s.id, { effort: v == null ? null : clamp(Math.trunc(v), 1, 10) })}
            />
            <FieldRow label="Note">
              <InlineTextField value={s.note} onChange={(note) => repo.patchSession(s.id, { note })} placeholder="Optional" label="Note" />
            </FieldRow>
          </GroupedList>
        ) : s.note.trim() || s.effort != null ? (
          <GroupedList>
            {s.effort != null ? (
              <FieldRow label="Effort" divider={false}>
                <Text style={Type.body}>{`${s.effort} / 10`}</Text>
              </FieldRow>
            ) : null}
            {s.note.trim() ? (
              <FieldRow label="Note" divider={s.effort != null}>
                <Text style={[Type.body, { textAlign: 'right', flexShrink: 1 }]}>{s.note}</Text>
              </FieldRow>
            ) : null}
          </GroupedList>
        ) : null}
        <View style={{ height: 20 }} />
        {!finished ? <PrimaryButton text="Finish workout" onPress={openSummary} /> : null}
        <View style={{ height: 20 }} />
        <View style={{ flexDirection: 'row' }}>
          <TextButton text={finished ? 'Delete session' : 'Discard workout'} color={K.Red} onPress={() => setConfirmDelete(true)} />
        </View>
      </KeyboardAwareScrollView>

      <SwapSheet
        open={swapOpen}
        state={swap}
        onChange={setSwap}
        exercises={(data?.exercises ?? []).filter((e) => e.kind === Kind.STRENGTH && !e.archived)}
        onDismiss={() => setSwapOpen(false)}
        onPick={(ex, updateProgram) => {
          if (swap) repo.swapExerciseInSession(s.id, swap.log.exerciseId, ex, updateProgram, s.programId);
          setSwapOpen(false);
        }}
      />
      <ConfirmSheet
        visible={confirmDelete}
        title={finished ? 'Delete this session?' : 'Discard this workout?'}
        body={finished ? 'The sets and the session go away. Charts recompute.' : 'Nothing from today is kept.'}
        action={finished ? 'Delete' : 'Discard'}
        onDismiss={() => setConfirmDelete(false)}
        onConfirm={async () => {
          setConfirmDelete(false);
          leaving.current = true;
          await repo.deleteSession(s.id);
          leave();
        }}
      />
      <Sheet visible={summaryOpen} title="Workout done" onDismiss={() => setSummaryOpen(false)}>
        {summary ? (
          <>
            <GroupedList style={{ backgroundColor: K.Elevated }}>
              <ListRow title="Sets done" value={`${summary.done} of ${summary.total}`} chevron={false} />
              <ListRow title="Volume" value={`${fmt(summary.volume, 0)} kg`} chevron={false} />
              <ListRow title="New bests" value={String(summary.bests)} chevron={false} last />
            </GroupedList>
            <PrimaryButton
              text="Save"
              style={{ marginTop: 16 }}
              onPress={async () => {
                setSummaryOpen(false);
                await repo.finishSession(s.id);
                leave();
              }}
            />
          </>
        ) : null}
      </Sheet>
    </SafeAreaView>
  );
}

function BackButton() {
  return (
    <Pressable
      onPress={() => router.back()}
      accessibilityRole="button"
      accessibilityLabel="Today"
      hitSlop={4}
      style={({ pressed }) => [styles.back, pressed && { opacity: 0.6 }]}>
      <SymbolView name={{ ios: 'chevron.left', android: 'arrow_back_ios_new', web: 'arrow_back_ios_new' }} tintColor={K.Text} size={17} />
    </Pressable>
  );
}

/**
 * Set row (DESIGN.md): check circle, tag, the full exercise name (tap to swap), last time on the right;
 * under it the kg and reps widgets with the gain since last time in green.
 */
function SetRow({
  log,
  prev,
  live,
  inputRef,
  onToggle,
  onWeight,
  onReps,
  onSwap,
}: {
  log: SetLog;
  prev: SetLog | undefined;
  live: boolean;
  inputRef: Ref<TextInput>;
  onToggle: (log: SetLog) => void;
  onWeight: (v: number | null) => void;
  onReps: (v: number | null) => void;
  onSwap: (log: SetLog) => void;
}) {
  const { width } = useWindowDimensions();
  // Two widgets share a row: screen minus margins (32), set padding (26) and the gap (10), halved.
  const compact = (width - 68) / 2 < COMPACT_WIDTH;
  const tag = setTag(log);
  const isDone = log.status === SetStatus.DONE;
  const decimals = kgDecimals(log.stepKg, log.weightKg, prev?.weightKg);
  return (
    <View style={[styles.set, isDone && { backgroundColor: 'rgba(48,209,88,0.05)' }]}>
      <View style={styles.setLine}>
        <Pressable
          onPress={() => onToggle(log)}
          disabled={!live}
          accessibilityRole="checkbox"
          accessibilityState={{ checked: isDone, disabled: !live }}
          accessibilityLabel={`${tag} ${log.exerciseName}`}
          style={styles.checkTarget}>
          <View style={[styles.check, isDone ? { backgroundColor: K.Green } : { borderWidth: 2, borderColor: K.Surface3 }]}>
            {isDone ? <SymbolView name={{ ios: 'checkmark', android: 'check', web: 'check' }} tintColor={K.GreenInk} size={20} weight="bold" /> : null}
          </View>
        </Pressable>
        <Text style={styles.tag}>{tag}</Text>
        <Pressable
          onPress={() => onSwap(log)}
          disabled={!live}
          accessibilityRole={live ? 'button' : undefined}
          accessibilityHint={live ? 'Swap exercise' : undefined}
          style={{ flex: 1 }}>
          <Text style={[Type.body, { fontFamily: Font.semibold }]}>{log.exerciseName}</Text>
          <Text style={styles.range}>{`${log.minReps}–${log.maxReps} reps`}</Text>
        </Pressable>
        {prev ? (
          <Text style={[Type.subheadline, styles.prev]} accessibilityLabel={`Last time ${fmt(prev.weightKg, decimals)} kilograms, ${prev.reps ?? 0} reps`}>
            {`${fmt(prev.weightKg, decimals)} × ${prev.reps ?? 0}`}
          </Text>
        ) : null}
      </View>
      {live ? (
        <View style={styles.widgets}>
          <View style={{ flex: 1 }}>
            <NumberField
              value={log.weightKg}
              onChange={onWeight}
              step={log.stepKg > 0 ? log.stepKg : 1}
              unit="kg"
              decimals={decimals}
              inputRef={inputRef}
              label={`${tag} weight`}
              placeholder={prev ? fmt(prev.weightKg, decimals) : undefined}
              compact={compact}
            />
            <Delta text={gain(prev?.weightKg, log.weightKg, decimals, 'kg')} unit={compact ? 'kg' : null} />
          </View>
          <View style={{ flex: 1 }}>
            <NumberField
              value={log.reps}
              onChange={onReps}
              step={1}
              unit="reps"
              decimals={0}
              label={`${tag} reps`}
              placeholder={String(prev?.reps ?? log.minReps)}
              compact={compact}
            />
            <Delta text={gain(prev?.reps, log.reps, 0, 'reps')} unit={compact ? 'reps' : null} />
          </View>
        </View>
      ) : (
        <Text style={[Type.subheadline, styles.readOnly, log.status === SetStatus.SKIPPED && { color: K.Dim }]}>
          {log.status === SetStatus.SKIPPED ? 'skipped' : `${fmt(log.weightKg, decimals)} kg × ${log.reps ?? 0} reps`}
        </Text>
      )}
    </View>
  );
}

/** Under a widget: the gain since last time in green, else the unit when the widget is too narrow to show it. */
function Delta({ text, unit }: { text: string | null; unit: string | null }) {
  if (text) return <Text style={[styles.delta, { color: K.Green }]}>{text}</Text>;
  if (unit) return <Text style={[styles.delta, { color: K.Dim }]}>{unit}</Text>;
  return null;
}

type SwapState = { log: SetLog; query: string; to: Exercise | null };

/** Pick a replacement from the library, then the scope: this session only, or the program too. */
function SwapSheet({
  open,
  state,
  onChange,
  exercises,
  onDismiss,
  onPick,
}: {
  open: boolean;
  state: SwapState | null;
  onChange: (s: SwapState) => void;
  exercises: Exercise[];
  onDismiss: () => void;
  onPick: (ex: Exercise, updateProgram: boolean) => void;
}) {
  const q = (state?.query ?? '').toLowerCase();
  const list = state ? exercises.filter((e) => e.id !== state.log.exerciseId && e.name.toLowerCase().includes(q)).slice(0, 12) : [];
  const to = state?.to;
  return (
    <Sheet visible={open} title={state ? (to ? `Swap to ${to.name}` : `Swap ${state.log.exerciseName}`) : undefined} onDismiss={onDismiss}>
      {state && to ? (
        <ChoiceList>
          <ChoiceRow title="This session only" selected={false} onPress={() => onPick(to, false)} />
          <ChoiceRow title="Update the program too" selected={false} last onPress={() => onPick(to, true)} />
        </ChoiceList>
      ) : state ? (
        <>
          <SearchField value={state.query} onChange={(query) => onChange({ ...state, query })} elevated />
          <View style={{ height: 12 }} />
          <ChoiceList>
            {list.map((e, i) => (
              <ChoiceRow key={e.id} title={e.name} selected={false} last={i === list.length - 1} onPress={() => onChange({ ...state, to: e })} />
            ))}
          </ChoiceList>
        </>
      ) : null}
    </Sheet>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: K.Bg },
  content: { paddingHorizontal: Space.margin, paddingTop: 6 },
  topBar: { flexDirection: 'row', alignItems: 'center', height: 44, marginBottom: 4 },
  back: { width: 36, height: 36, borderRadius: 18, backgroundColor: K.Surface2, alignItems: 'center', justifyContent: 'center' },
  metaRow: { flexDirection: 'row', alignItems: 'center', marginTop: 4 },
  track: { height: 5, borderRadius: 3, backgroundColor: K.Surface2, marginTop: 8, marginBottom: 6, overflow: 'hidden' },
  fill: { height: 5, borderRadius: 3, backgroundColor: K.Green },
  block: { backgroundColor: K.Surface, borderRadius: Space.groupRadius, overflow: 'hidden' },
  blockHeader: { flexDirection: 'row', alignItems: 'center', minHeight: 56, paddingLeft: 16, paddingRight: 12 },
  blockIndex: { width: 24, height: 24, borderRadius: 12, backgroundColor: K.AccentSoft, alignItems: 'center', justifyContent: 'center' },
  blockIndexText: { fontFamily: Font.bold, fontSize: 13, color: K.Accent },
  blockNames: { flex: 1, fontFamily: Font.semibold, paddingHorizontal: 10, paddingVertical: 12 },
  hairline: { height: StyleSheet.hairlineWidth, backgroundColor: K.Divider },
  set: { paddingLeft: 10, paddingRight: 16, paddingTop: 8, paddingBottom: 12 },
  setLine: { flexDirection: 'row', alignItems: 'center' },
  checkTarget: { width: 44, height: 44, alignItems: 'center', justifyContent: 'center', marginRight: 8 },
  check: { width: 34, height: 34, borderRadius: 17, alignItems: 'center', justifyContent: 'center' },
  tag: { fontFamily: Font.bold, fontSize: 12, color: K.Accent, marginRight: 8 },
  range: { fontFamily: Font.regular, fontSize: 12, color: K.Dim },
  prev: { color: K.Muted, marginLeft: 8, fontVariant: ['tabular-nums'] },
  widgets: { flexDirection: 'row', gap: 10, marginTop: 10 },
  delta: { fontFamily: Font.semibold, fontSize: 12, paddingLeft: 40, marginTop: 3 },
  readOnly: { color: K.Text, paddingLeft: 42, marginTop: 4 },
});
