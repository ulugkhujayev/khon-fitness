# LOAD01-load-year R4 verification

**Verdict.** Merge for the reviewed scope at `36b9c41`.

**F2. Read.** At the chart limit, adjacent doubles are 0.125 apart, so the flat-series padding of 1 still changes both bounds. The minimum grid step of 0.5 is exactly representable. Each larger step is smaller than the span that selects it. The chart filters both series before it finds the bounds. All vertical positions use the same fraction. The new test checks ordered bounds, tick count and point fractions. [Axis and filter](/Users/mirzohidkhon/.config/agents/state/snapshots/LOAD01-load-year-09f5567c-R4/src/android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/components/LineChart.kt:53), [Draw path](/Users/mirzohidkhon/.config/agents/state/snapshots/LOAD01-load-year-09f5567c-R4/src/android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/components/LineChart.kt:149), [Test](/Users/mirzohidkhon/.config/agents/state/snapshots/LOAD01-load-year-09f5567c-R4/src/android/app/src/test/java/dev/mirzohidkhon/khonfitness/ui/components/LineChartStepTest.kt:52), [Design](/Users/mirzohidkhon/.config/agents/state/snapshots/LOAD01-load-year-09f5567c-R4/src/DESIGN.md:659).

**Ran.** Inline `python3` checked 15 boundary cases and 300,000 sampled axes. All had ordered finite bounds, finite ticks and finite point fractions. The 0 to 100 percent domain kept finite fractions for points outside that domain. The 39 step checks through span 3000 matched the old table. The maximum position change was `0.0 px` across 30,000 ordinary chart cases. The same arithmetic reproduced the R3 one-tick and collapsed-bound cases.

**Other. P3. Read and ran arithmetic.** The limit comment says every sum and difference is exact. At `1e15`, subtracting `1e-9` gives the same double. The flat-series branch handles that pair. Describe the bound and step guarantees in the comment. [LineChart.kt:53](/Users/mirzohidkhon/.config/agents/state/snapshots/LOAD01-load-year-09f5567c-R4/src/android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/components/LineChart.kt:53).

**Witness. Read.** The step 6 fallback applies because `witness run` writes records outside the sandbox. The lead's record says `fixed` at `36b9c41c7eb0`. Its Gradle log says `BUILD SUCCESSFUL in 11s`. The lead also reports zero failures in the focused suite. My checks covered source and arithmetic. [Witness record](/Users/mirzohidkhon/.config/agents/state/witness/LOAD01-load-year/F2-8.json:1), [Gradle receipt](/Users/mirzohidkhon/.config/agents/state/receipts/r-20261127014345-03fa02.log).

F2=closed
