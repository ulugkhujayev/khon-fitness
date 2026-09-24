#!/usr/bin/env bash
# Read-only, compact resume snapshot for the 0.4.7 audit.
set -euo pipefail
cd "$(dirname "$0")/.."
printf 'HEAD: '
git rev-parse --short HEAD
printf '\nWorktree changes:\n'
git status --short
printf '\nDiff check: '
if git diff --check; then printf 'pass\n'; else printf 'FAIL\n'; fi
printf '\nPreview APK: '
if [[ -f dist/khon-fitness-0.4.8-audit-preview.apk ]]; then
  shasum -a 256 dist/khon-fitness-0.4.8-audit-preview.apk
else
  printf 'missing\n'
fi
printf '\nLast unit-test results: '
python3 - <<'PY'
from pathlib import Path
from xml.etree import ElementTree as ET
files = list(Path('android/app/build/test-results/testDebugUnitTest').glob('TEST-*.xml'))
if not files:
    print('none on disk')
else:
    suites = [ET.parse(path).getroot() for path in files]
    print(f"{sum(int(s.attrib['tests']) for s in suites)} tests, "
          f"{sum(int(s.attrib['failures']) + int(s.attrib['errors']) for s in suites)} failures")
PY
printf '\nConnected Android devices:\n'
adb_bin="${ANDROID_HOME:-$HOME/Library/Android/sdk}/platform-tools/adb"
if [[ -x "$adb_bin" ]]; then "$adb_bin" devices; else printf 'adb unavailable\n'; fi
printf '\nNext batch: docs/QA-AUDIT-STATE.md\n'
