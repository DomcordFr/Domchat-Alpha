#!/usr/bin/env python3
"""
DomChat V2 Termux Pre-Flight Gatekeeper
Runs in <2s with zero Android SDK. Blocks syntax truncation, missing imports,
modifier inversions, 3-Tier Backdrop DAG violations, legacy LastChat imports,
duplicate Scaffolds, and accidental feature deletions ("Silent Feature Eraser").
"""
import subprocess
import sys
import re
from pathlib import Path

MAX_FILES_PER_COMMIT = 3
MAX_DELETED_LINES_WITHOUT_OVERRIDE = 18

REQUIRED_IMPORTS = {
    r'\bpositionInWindow\s*\(': 'androidx.compose.ui.layout.positionInWindow',
    r'\bpositionInParent\s*\(': 'androidx.compose.ui.layout.positionInParent',
    r'\bonGloballyPositioned\b': 'androidx.compose.ui.layout.onGloballyPositioned',
    r'\bIntOffset\s*\(': 'androidx.compose.ui.unit.IntOffset',
    r'\brememberSaveable\b': 'androidx.compose.runtime.saveable.rememberSaveable',
    r'\bliquidGlassSurface\s*\(': 'me.lucid.domchat.ui.theme.liquidGlassSurface',
    r'\brememberLayerBackdrop\s*\(': 'me.lucid.domchat.ui.component.backdrop.backdrops.rememberLayerBackdrop',
    r'\bderivedStateOf\b': 'androidx.compose.runtime.derivedStateOf',
    r'\.border\s*\(': 'androidx.compose.foundation.border',
    r'\bCircleShape\b': 'androidx.compose.foundation.shape.CircleShape',
}

PROTECTED_FEATURE_TOKENS = [
    "HapticPattern",
    "DomChatInWindowBottomSheet",
    "DisposableEffect",
    "creatorNotes",
    "geometryChanged",
    "chatListReady",
]

REPO_ROOT = Path(__file__).resolve().parent.parent

def run_git(args: list[str]) -> str:
    try:
        return subprocess.check_output(["git", "-C", str(REPO_ROOT)] + args, stderr=subprocess.DEVNULL).decode("utf-8", errors="replace")
    except Exception:
        return ""

def get_changed_kt_files() -> list[str]:
    out = run_git(["diff", "--cached", "--name-only", "--diff-filter=ACM"]).splitlines()
    if not out:
        out = run_git(["diff", "--name-only", "--diff-filter=ACM"]).splitlines()
    return [f for f in out if f.endswith(".kt") and (REPO_ROOT / f).exists()]

def strip_comments_and_strings(code: str) -> str:
    pattern = r'//[^\n]*|/\*[\s\S]*?\*/|"""[\s\S]*?"""|"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\''
    def repl(m):
        s = m.group(0)
        if (s.startswith('/') or s.startswith('"""')) and '\n' in s:
            return '\n' * s.count('\n')
        elif s.startswith('//') or s.startswith('/*'):
            return ''
        else:
            return '""'
    return re.sub(pattern, repl, code)

def check_balanced_delimiters(filepath: str, clean: str) -> list[str]:
    stack = []
    pairs = {'{': '}', '(': ')', '[': ']'}
    closing = set(pairs.values())
    for idx, line in enumerate(clean.splitlines(), 1):
        for ch in line:
            if ch in pairs:
                stack.append((ch, idx))
            elif ch in closing:
                if not stack:
                    return [f"[SYNTAX_TRUNCATION] {filepath}:{idx} -> Extra closing '{ch}'."]
                op, op_line = stack.pop()
                if pairs[op] != ch:
                    return [f"[SYNTAX_TRUNCATION] {filepath}:{idx} -> Mismatched '{ch}' for '{op}' opened at line {op_line}."]
    if stack:
        op, op_line = stack[-1]
        return [f"[SYNTAX_TRUNCATION] {filepath}:{op_line} -> Unclosed '{op}' (LLM output truncated)."]
    return []

def check_missing_imports(filepath: str, raw: str, clean: str) -> list[str]:
    errors = []
    pkg_m = re.search(r'^package\s+([\w.]+)', clean, re.MULTILINE)
    curr_pkg = pkg_m.group(1) if pkg_m else ""
    for pat, imp in REQUIRED_IMPORTS.items():
        if re.search(pat, clean):
            target_pkg, sym = imp.rsplit('.', 1)
            if curr_pkg == target_pkg:
                continue
            if (f"import {imp}" not in raw) and (f"import {target_pkg}.*" not in raw):
                if not re.search(rf'\bfun\s+(?:[\w.<>]+\.)?{sym}\b', clean):
                    errors.append(f"[MISSING_IMPORT] {filepath} uses '{sym}' without 'import {imp}'.")
    return errors

def check_domchat_architecture(filepath: str, raw: str, clean: str) -> list[str]:
    errors = []
    # 1. Legacy LastChat Package Blocker
    if "me.rerere.rikkahub" in raw:
        errors.append(f"[LEGACY_BAGGAGE] {filepath} references extinct package 'me.rerere.rikkahub'. Use 'me.lucid.domchat'.")

    # 2. Banned OS-Window ModalBottomSheet
    if "androidx.compose.material3.ModalBottomSheet" in raw:
        errors.append(f"[SHEET_VIOLATION] {filepath} imports Material3 ModalBottomSheet (breaks Liquid Glass). Use DomChatInWindowBottomSheet.")

    # 3. Duplicate Nested Scaffold in ChatPage
    if filepath.endswith("ChatPage.kt") and len(re.findall(r'\bScaffold\s*\(', clean)) > 1:
        errors.append(f"[INSET_COLLISION] {filepath} contains multiple nested 'Scaffold(' declarations!")

    # 4. Modifier Order Invariant
    bad_mod = re.compile(
        r'\.padding\s*\([^)]+\)\s*\n?\s*\.(then\s*\(\s*surfaceModifier\s*\)|liquidGlassSurface|clip|clickable|combinedClickable)\b',
        re.MULTILINE
    )
    for m in bad_mod.finditer(clean):
        line_no = clean[:m.start()].count('\n') + 1
        errors.append(f"[MODIFIER_ORDER] {filepath}:{line_no} -> '.padding()' placed BEFORE '.{m.group(1)}'. Move inner padding after clickable/surface.")

    # 5. Backdrop Detach Wipe & Mirroring Loop
    if "DrawBackdrop" in filepath and re.search(r'fun\s+onDetach\s*\([^)]*\)\s*\{[^}]*layerCoordinates\s*=\s*null', clean, re.DOTALL):
        errors.append(f"[BACKDROP_DAG] {filepath} -> DrawBackdropNode.onDetach() nullifies shared layerCoordinates!")
    if filepath.endswith(("ChatMessageV2.kt", "GroupedMessageBubble.kt")) and "overlayBackdrop" in clean:
        errors.append(f"[BACKDROP_DAG] {filepath} binds message bubble to 'overlayBackdrop' (causes recursive mirroring). Use 'wallpaperBackdrop'.")

    # 6. Drawer Scroll Gradient Ban
    if filepath.endswith("ConversationList.kt") and re.search(r'canScrollBackward\s*\)\s*\{[^}]*verticalGradient', clean, re.DOTALL):
        errors.append(f"[AMOLED_DESIGN] {filepath} re-introduced banned scroll gradient bar.")

    return errors

def check_diff_hazards(allow_override: bool) -> list[str]:
    errors = []
    diff = run_git(["diff", "--cached", "-U3"]) or run_git(["diff", "-U3"])
    if not diff:
        return errors
    removed_lines = [l[1:] for l in diff.splitlines() if l.startswith("-") and not l.startswith("---")]
    added_lines = [l[1:] for l in diff.splitlines() if l.startswith("+") and not l.startswith("+++")]

    # 1. Anti-Eraser Check: Did the worker delete protected feature tokens without re-adding them?
    removed_blob = "\n".join(removed_lines)
    added_blob = "\n".join(added_lines)
    for token in PROTECTED_FEATURE_TOKENS:
        if token in removed_blob and token not in added_blob and not allow_override:
            errors.append(
                f"[SILENT_FEATURE_ERASER] Diff deletes protected feature token '{token}'! "
                f"Restore the feature or pass --force-delete if intentional."
            )

    # 2. Large Net Deletion Check
    net_deleted = len(removed_lines) - len(added_lines)
    if net_deleted > MAX_DELETED_LINES_WITHOUT_OVERRIDE and not allow_override:
        errors.append(
            f"[SILENT_FEATURE_ERASER] Diff removes {net_deleted} net lines of code. "
            f"Verify no existing features were wiped out by the worker."
        )

    # 3. ChatListRtlTest Default Argument Guard
    if "ChatList.kt" in diff:
        for line in added_lines:
            if re.search(r'^\s+\w+\s*:\s*[\w.<>?]+\s*,\s*$', line) and "=" not in line:
                errors.append(f"[TEST_BREAKAGE] ChatList.kt parameter '{line.strip()}' lacks a default value (will break ChatListRtlTest).")

    return errors

def main():
    allow_override = "--force-delete" in sys.argv or "--allow-wide" in sys.argv
    files = get_changed_kt_files()
    if not files:
        print("✓ DomChat Pre-Flight: Working tree clean.")
        sys.exit(0)

    all_errors = []
    if len(files) > MAX_FILES_PER_COMMIT and not allow_override:
        all_errors.append(
            f"[BATCH_HALLUCINATION_GUARD] {len(files)} files modified at once (Max allowed is {MAX_FILES_PER_COMMIT}). "
            f"Fix and verify 1 issue group at a time, or pass --allow-wide."
        )

    for f in files:
        raw = (REPO_ROOT / f).read_text(encoding="utf-8")
        clean = strip_comments_and_strings(raw)
        all_errors.extend(check_balanced_delimiters(f, clean))
        all_errors.extend(check_missing_imports(f, raw, clean))
        all_errors.extend(check_domchat_architecture(f, raw, clean))

    all_errors.extend(check_diff_hazards(allow_override))

    if all_errors:
        print("\n" + "=" * 70)
        print("✗ DOMCHAT PRE-FLIGHT GATE REJECTED THE WORKER'S CHANGES:")
        print("=" * 70)
        for e in all_errors:
            print(f"  • {e}")
        print("=" * 70)
        sys.exit(1)

    print(f"✓ DomChat Pre-Flight PASSED ({len(files)} file(s) verified in <2s).")

if __name__ == "__main__":
    main()
