#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

export NODE_OPTIONS="--max-old-space-size=384"
CMD="${1:-}"
shift || true

case "$CMD" in
  # 1. ZERO-TOKEN LOCAL LOCATOR (Uses local ripgrep in Termux - 0 API calls, 0 IP rate limit)
  locate)
    PATTERN="$1"
    PATH_SCOPE="${2:-app/src/}"
    echo "=== LOCAL RIPGREP MAP (0 API Tokens Used) ==="
    rg -n -C 2 "$PATTERN" "$PATH_SCOPE" | head -n 45 || echo "No matches found for: $PATTERN"
    ;;

  # 2. SURGICAL FILE SLICER (Extracts ONLY package/imports + exact line range)
  slice)
    FILE="$1"
    START="$2"
    END="$3"
    echo "=== TARGET: $FILE (Imports + Lines ${START}..${END}) ==="
    grep -nE "^(package |import )" "$FILE" || true
    echo "--- CODE WINDOW [${START}..${END}] ---"
    nl -ba "$FILE" | sed -n "${START},${END}p"
    ;;

  # 3. RATE-LIMIT-AWARE SURGEON DISPATCHER (Feeds pre-sliced context so Worker finishes in 1-2 turns)
  surgeon)
    MODEL="${1:-kilo/nvidia/nemotron-3.5-super-vl:free}"
    FILE="$2"
    START="$3"
    END="$4"
    INSTRUCTION="$5"

    SLICE_DATA=$("$0" slice "$FILE" "$START" "$END")
    if ! opencode run --model "$MODEL" --standalone <<EOF
Read AGENTS.md rules carefully.
TARGET FILE: $FILE (Lines $START to $END)
PRE-SLICED CONTEXT (DO NOT read the full file; edit ONLY within lines $START to $END):
$SLICE_DATA

TASK: $INSTRUCTION

MANDATORY STEPS:
1. State the Write-Site Root Cause in 2 lines (no symptom fallbacks).
2. Apply the surgical edit to $FILE (preserving all existing features and HapticPatterns).
3. Run: python3 scripts/domchat_preflight.py
EOF
    then
      echo "⚠️ Worker call failed or hit HTTP 429 on $MODEL."
      echo "👉 If using Kilo free tier, toggle Airplane Mode ON/OFF to cycle your mobile IP, or switch to GLM-5.3 (numes/z-ai/glm-5.3)."
      exit 1
    fi
    python3 scripts/domchat_preflight.py
    ;;

  # 4. ZERO-POLLING CI WATCHER (Sleeps silently locally; never burns agent tokens in 20s loops)
  ci-wait)
    INITIAL_SLEEP="${1:-300}" # Default 5 minutes (300s) silent local sleep
    echo "Sleeping silently for ${INITIAL_SLEEP}s locally in Termux (0 tokens used)..."
    sleep "$INITIAL_SLEEP"
    RUN_ID=$(gh run list --limit 1 --json databaseId --jq '.[0].databaseId')
    echo "Attaching to GitHub Action Run #$RUN_ID until completion..."
    if ! gh run watch "$RUN_ID" --interval 30 --exit-status; then
      echo "=== COMPILER / TEST FAILURE EXTRACT ==="
      gh run view "$RUN_ID" --log-failed | grep -E "e: file://|FAILURE:|Unresolved reference|Exception" | tail -n 35
      exit 1
    fi
    echo "✓ Build #$RUN_ID PASSED!"
    ;;

  *)
    echo "Usage:"
    echo "  ./scripts/domchat_rig.sh locate \"<regex_pattern>\" [path]"
    echo "  ./scripts/domchat_rig.sh slice <file> <start_line> <end_line>"
    echo "  ./scripts/domchat_rig.sh surgeon <model> <file> <start> <end> \"<instruction>\""
    echo "  ./scripts/domchat_rig.sh ci-wait [initial_sleep_seconds]"
    exit 1
    ;;
esac
