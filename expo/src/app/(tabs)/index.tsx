import { useRef, useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { router } from 'expo-router';
import { LineChart } from '@/components/LineChart';
import { NumberField } from '@/components/NumberField';
import { TabScreen } from '@/components/Screen';
import { ChoiceList, ChoiceRow, Sheet, SheetGroupTitle } from '@/components/Sheet';
import { BarCircleButton, PrimaryButton, ScreenTitle, TextButton } from '@/components/ui';
import { WeekStrip } from '@/components/WeekStrip';
import { exerciseColor } from '@/components/colors';
import { addDays, longDate, today as todayIso } from '@/domain/dates';
import { fmt, preview } from '@/domain/format';
import { itemFor, PlanData } from '@/domain/plan';
import { Intensity, ItemType, Kind, Modality, Session } from '@/domain/types';
import { doneLine } from '@/domain/workout';
import * as repo from '@/db/repository';
import { TABLES as T } from '@/db/schema';
import { useLive } from '@/db/useLive';
import { Font, K, Type } from '@/theme';

/** Opens a running or new session. Cardio sessions get their screen in stage 4. */
function openSession(s: Session) {
  if (s.itemType === ItemType.PROGRAM) router.push(`/session/${s.id}`);
}

/** Port of TodayScreen.kt: week strip, today's item with Start or Resume, bodyweight. */
export default function TodayScreen() {
  const today = todayIso();
  const [planDate, setPlanDate] = useState<string | null>(null);
  const [planOpen, setPlanOpen] = useState(false);
  const [bwDraft, setBwDraft] = useState<number | null>(null);
  const [bwKey, setBwKey] = useState(0);
  const starting = useRef(false);

  const { data } = useLive(
    async () => {
      const [plan, modalities, blocks, blockExercises, bodyweights, unfinished, sessions] = await Promise.all([
        repo.planData(), repo.modalities(), repo.blocks(), repo.blockExercises(), repo.bodyweights(), repo.unfinishedSession(), repo.sessions(),
      ]);
      const item = itemFor(plan, today);
      const done = sessions.find(
        (s) =>
          s.date === today &&
          s.finishedAt != null &&
          ((item.program != null && s.programId === item.program.id) || (item.cardio != null && s.exerciseId === item.cardio.id)),
      );
      return {
        plan, modalities, bodyweights, unfinished, item, done,
        line: preview(item, blocks, blockExercises, modalities),
        doneText: done ? doneLine(done, await repo.setLogs(done.id)) : null,
      };
    },
    [...repo.PLAN_TABLES, T.modalities, T.blocks, T.blockExercises, T.bodyweights, T.sessions, T.setLogs],
    [today],
  );

  const start = async () => {
    if (!data || starting.current) return;
    starting.current = true;
    try {
      const s = await repo.startItem(data.item);
      if (s) openSession(s);
    } finally {
      starting.current = false;
    }
  };

  const latest = data?.bodyweights[data.bodyweights.length - 1];
  const from = addDays(today, -29);
  const recent = (data?.bodyweights ?? []).filter((b) => b.date >= from);

  return (
    <TabScreen>
      <ScreenTitle
        text="Today"
        trailing={<BarCircleButton icon={{ ios: 'gearshape', android: 'settings', web: 'settings' }} label="Settings" onPress={() => router.push('/settings')} />}
      />
      {data ? (
        <>
          <WeekStrip
            today={today}
            plan={data.plan}
            modalities={data.modalities}
            onPress={(date) => {
              setPlanDate(date);
              setPlanOpen(true);
            }}
          />
          <View style={styles.card}>
            {data.unfinished ? (
              <>
                <Text style={Type.title2}>{data.unfinished.programName ?? data.unfinished.exerciseName ?? 'Session'}</Text>
                <Text style={[Type.subheadline, styles.line, { color: K.Green }]}>In progress</Text>
                <PrimaryButton
                  text="Resume"
                  enabled={data.unfinished.itemType === ItemType.PROGRAM}
                  onPress={() => openSession(data.unfinished!)}
                />
              </>
            ) : data.done ? (
              <>
                <Text style={Type.title2}>{data.item.name}</Text>
                <Text style={[Type.subheadline, { color: K.Green, marginTop: 6, marginBottom: 10 }]}>{data.doneText}</Text>
                <View style={{ flexDirection: 'row' }}>
                  <TextButton text="Start again" enabled={data.item.itemType === ItemType.PROGRAM} onPress={start} />
                </View>
              </>
            ) : (
              <>
                <Text style={Type.title2}>{data.item.name}</Text>
                <Text style={[Type.subheadline, styles.line, { color: K.Muted }, data.item.isRest && { marginBottom: 0 }]}>{data.line}</Text>
                {data.item.isRest ? null : (
                  <PrimaryButton text="Start" enabled={data.item.itemType === ItemType.PROGRAM} onPress={start} />
                )}
              </>
            )}
          </View>

          <View style={styles.bwHeader}>
            <Text style={[Type.headline, { flex: 1 }]}>Bodyweight</Text>
            <Text style={styles.bwLatest} accessibilityLabel={latest ? `${fmt(latest.kg)} kg` : 'No entry yet'}>
              {latest ? `${fmt(latest.kg)} kg` : '—'}
            </Text>
          </View>
          {recent.length > 0 ? <LineChart points={recent.map((b) => ({ date: b.date, value: b.kg }))} unit="kg" height={130} style={{ marginTop: 8 }} /> : null}
          <View style={styles.bwAdd}>
            <NumberField
              key={bwKey}
              value={bwDraft}
              onChange={setBwDraft}
              step={0.1}
              unit="kg"
              label="Bodyweight"
              placeholder={latest ? fmt(latest.kg) : '0.0'}
              style={{ flex: 1 }}
            />
            <TextButton
              text="Add"
              enabled={bwDraft != null && bwDraft > 0}
              onPress={() => {
                if (bwDraft == null || bwDraft <= 0) return;
                repo.saveBodyweight(today, bwDraft);
                setBwDraft(null);
                setBwKey((k) => k + 1);
              }}
            />
          </View>

          <PlanSheet date={planDate} open={planOpen} plan={data.plan} modalities={data.modalities} onDismiss={() => setPlanOpen(false)} />
        </>
      ) : null}
    </TabScreen>
  );
}

/** Change what a date holds: a program, a cardio exercise, or rest. Port of PlanSheet. */
function PlanSheet({
  date,
  open,
  plan,
  modalities,
  onDismiss,
}: {
  date: string | null;
  open: boolean;
  plan: PlanData;
  modalities: Modality[];
  onDismiss: () => void;
}) {
  const pick = (type: string, id: string | null) => {
    if (date) repo.saveOverride(date, type, id);
    onDismiss();
  };
  const current = date ? itemFor(plan, date) : null;
  const cardio = plan.exercises.filter((e) => e.kind === Kind.CARDIO && !e.archived);
  return (
    <Sheet visible={open} title={date ? longDate(date) : undefined} onDismiss={onDismiss}>
      {date && current ? (
        <>
          <SheetGroupTitle text="Programs" />
          <ChoiceList>
            {plan.programs.map((p, i) => (
              <ChoiceRow
                key={p.id}
                title={p.name}
                selected={current.program?.id === p.id}
                dot={K.Accent}
                last={i === plan.programs.length - 1}
                onPress={() => pick(ItemType.PROGRAM, p.id)}
              />
            ))}
          </ChoiceList>
          <SheetGroupTitle text="Cardio exercises" />
          <ChoiceList>
            {cardio.map((e, i) => (
              <ChoiceRow
                key={e.id}
                title={e.name}
                selected={current.cardio?.id === e.id}
                dot={exerciseColor(e, modalities)}
                dotFilled={e.intensity === Intensity.HIGH}
                last={i === cardio.length - 1}
                onPress={() => pick(ItemType.CARDIO, e.id)}
              />
            ))}
          </ChoiceList>
          <SheetGroupTitle text="Rest" />
          <ChoiceList>
            <ChoiceRow title="Rest" selected={current.isRest} dot={K.Dim} last onPress={() => pick(ItemType.REST, null)} />
          </ChoiceList>
          {current.overridden ? (
            <View style={{ flexDirection: 'row', marginTop: 12 }}>
              <TextButton
                text="Back to weekly template"
                onPress={() => {
                  repo.clearOverride(date);
                  onDismiss();
                }}
              />
            </View>
          ) : null}
        </>
      ) : null}
    </Sheet>
  );
}

const styles = StyleSheet.create({
  card: { marginTop: 24, backgroundColor: K.Surface, borderRadius: 20, paddingHorizontal: 20, paddingVertical: 18 },
  line: { marginTop: 6, marginBottom: 18 },
  bwHeader: { flexDirection: 'row', alignItems: 'center', marginTop: 28 },
  bwLatest: { fontFamily: Font.bold, fontSize: 22, color: K.Text, fontVariant: ['tabular-nums'] },
  bwAdd: { flexDirection: 'row', alignItems: 'center', gap: 12, marginTop: 12 },
});
