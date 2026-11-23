#!/usr/bin/env python3
"""
DomChat V3 Termux Pre-Flight Gatekeeper
Runs in <2s with zero Android SDK. Blocks:
- Syntax truncation / unbalanced delimiters
- Missing imports (with FQN & same-package awareness)
- Newly introduced or orphaned unused imports
- Kotlin type-inference traps (e.g. untyped `buildList { }` inside nullable `if/else`)
- Newly introduced `!!` non-null assertions
- Swallowed `CancellationException` in added/modified `catch` blocks
- Broken cross-file callers when deleting/renaming functions or classes
- Staged `*_FIX_PLAN.md` files
- Liquid Glass & Backdrop invariants (anonymous `object : Backdrop`, double `layerBlock`, `Lens.kt` padding)
- Modifier inversions, 3-Tier Backdrop DAG violations, legacy LastChat imports,
  duplicate Scaffolds, and accidental feature deletions ("Silent Feature Eraser").

Usage:
  python scripts/domchat_preflight.py [--allow-wide] [--force-delete]
  python scripts/domchat_preflight.py --check-commit <sha> [--allow-wide]
"""
import subprocess
import sys
import re
from pathlib import Path

MAX_FILES_PER_COMMIT = 3
MAX_DELETED_LINES_WITHOUT_OVERRIDE = 18

REQUIRED_IMPORTS = {
    r'(?<![.\w])positionInWindow\s*\(': 'androidx.compose.ui.layout.positionInWindow',
    r'(?<![.\w])positionInParent\s*\(': 'androidx.compose.ui.layout.positionInParent',
    r'(?<![.\w])onGloballyPositioned\b': 'androidx.compose.ui.layout.onGloballyPositioned',
    r'(?<![.\w])IntOffset\s*\(': 'androidx.compose.ui.unit.IntOffset',
    r'(?<![.\w])rememberSaveable\b': 'androidx.compose.runtime.saveable.rememberSaveable',
    r'(?<![.\w])liquidGlassSurface\s*\(': 'me.lucid.domchat.ui.theme.liquidGlassSurface',
    r'(?<![.\w])rememberLayerBackdrop\s*\(': 'me.lucid.domchat.ui.component.backdrop.backdrops.rememberLayerBackdrop',
    r'(?<![.\w])CombinedBackdrop\s*\(': 'me.lucid.domchat.ui.component.backdrop.backdrops.CombinedBackdrop',
    r'(?<![.\w])LocalAppBackdrop\b': 'me.lucid.domchat.ui.component.backdrop.LocalAppBackdrop',
    r'(?<![.\w])LocalDarkMode\b': 'me.lucid.domchat.ui.theme.LocalDarkMode',
    r'(?<![.\w])LocalDisplaySetting\b': 'me.lucid.domchat.ui.context.LocalDisplaySetting',
    r'(?<![.\w])derivedStateOf\b': 'androidx.compose.runtime.derivedStateOf',
    r'\.border\s*\(': 'androidx.compose.foundation.border',
    r'(?<![.\w])CircleShape\b': 'androidx.compose.foundation.shape.CircleShape',
    r'(?<![.\w])NonCancellable\b': 'kotlinx.coroutines.NonCancellable',
    r'(?<![.\w])CoroutineStart\b': 'kotlinx.coroutines.CoroutineStart',
    r'(?<![.\w])ensureActive\s*\(': 'kotlinx.coroutines.ensureActive',
    r'(?<![.\w])SupervisorJob\s*\(': 'kotlinx.coroutines.SupervisorJob',
    r'(?<![.\w])withContext\s*\(': 'kotlinx.coroutines.withContext',
}

# Kotlin operator / delegate imports that are invoked implicitly without appearing by name
IMPLICIT_KOTLIN_IMPORTS = {
    "getValue", "setValue", "provideDelegate",
    "plus", "minus", "times", "div", "rem", "inc", "dec",
    "unaryPlus", "unaryMinus", "not", "compareTo", "contains",
    "get", "set", "invoke", "iterator", "rangeTo", "rangeUntil",
    "component1", "component2", "component3", "component4", "component5",
    "component6", "component7", "component8", "component9",
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
        return subprocess.check_output(
            ["git", "-C", str(REPO_ROOT)] + args,
            stderr=subprocess.DEVNULL
        ).decode("utf-8", errors="replace")
    except Exception:
        return ""


def get_diff_and_files(check_commit: str | None) -> tuple[list[str], str, list[str]]:
    """Returns (changed_kt_files, unified_diff, all_staged_or_commit_files)."""
    if check_commit:
        all_files = run_git(["diff-tree", "--no-commit-id", "--name-only", "-r", "--diff-filter=ACM", check_commit]).splitlines()
        diff = run_git(["show", "--format=", "-U5", check_commit])
        kt_files = [f for f in all_files if f.endswith(".kt") and (REPO_ROOT / f).exists()]
        return kt_files, diff, all_files

    staged = run_git(["diff", "--cached", "--name-only", "--diff-filter=ACM"]).splitlines()
    if staged:
        diff = run_git(["diff", "--cached", "-U5"])
        kt_files = [f for f in staged if f.endswith(".kt") and (REPO_ROOT / f).exists()]
        return kt_files, diff, staged

    unstaged = run_git(["diff", "--name-only", "--diff-filter=ACM"]).splitlines()
    diff = run_git(["diff", "-U5"])
    kt_files = [f for f in unstaged if f.endswith(".kt") and (REPO_ROOT / f).exists()]
    return kt_files, diff, unstaged


def parse_file_diffs(diff: str) -> dict[str, tuple[list[tuple[int, str]], list[str]]]:
    """
    Parses unified diff into {filepath: ([(new_line_no, added_line), ...], [removed_line, ...])}.
    """
    per_file: dict[str, tuple[list[tuple[int, str]], list[str]]] = {}
    curr_file = None
    new_line = 0
    for line in diff.splitlines():
        if line.startswith("+++ b/"):
            curr_file = line[6:]
            per_file.setdefault(curr_file, ([], []))
        elif line.startswith("@@ "):
            m = re.search(r'\+(\d+)', line)
            if m:
                new_line = int(m.group(1))
        elif curr_file is not None:
            if line.startswith("+") and not line.startswith("+++"):
                per_file[curr_file][0].append((new_line, line[1:]))
                new_line += 1
            elif line.startswith("-") and not line.startswith("---"):
                per_file[curr_file][1].append(line[1:])
            elif not line.startswith("\\"):
                new_line += 1
    return per_file


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
                # Skip if defined locally in the same file
                if not re.search(rf'\b(?:fun|val|var|class|object|interface)\s+(?:[\w.<>]+\.)?{sym}\b', clean):
                    errors.append(f"[MISSING_IMPORT] {filepath} uses '{sym}' without 'import {imp}'.")
    return errors


def check_kotlin_type_inference(filepath: str, clean: str) -> list[str]:
    """
    Catches Kotlin type-inference failures like:
      val x = if (cond) { buildList { ... } } else null
    where `buildList`/`buildMap`/`buildSet` has no explicit `<T>` and the `val` has no explicit type annotation.
    """
    errors = []
    # Pattern: val/var <name> = if (...) { buildList/buildSet/buildMap { ... } } else null
    # (no `: Type` on val, no `<...>` on builder)
    pat = re.compile(
        r'\b(?:val|var)\s+(\w+)\s*=\s*if\s*\([^)]*\)\s*\{?\s*(buildList|buildSet|buildMap)\s*\{',
        re.MULTILINE
    )
    for m in pat.finditer(clean):
        var_name = m.group(1)
        builder = m.group(2)
        # Look ahead up to 400 chars for `else null` or `else\n null`
        tail = clean[m.end():m.end() + 500]
        if re.search(r'\belse\s+null\b', tail):
            line_no = clean[:m.start()].count('\n') + 1
            errors.append(
                f"[KOTLIN_TYPE_INFERENCE] {filepath}:{line_no} -> '{var_name}' uses untyped '{builder} {{' "
                f"in a nullable if/else branch. Specify explicit type args (e.g. '{builder}<YourType> {{') "
                f"or annotate 'val {var_name}: List<YourType>? = ...'."
            )
    return errors


def check_unused_imports_in_diff(
    filepath: str,
    raw: str,
    clean: str,
    added_lines: list[tuple[int, str]],
    removed_lines: list[str]
) -> list[str]:
    """
    Flags any import that was:
      (a) newly added in this diff but unused in the file, OR
      (b) already in the file, whose symbol was removed in this diff and now has 0 references left.
    Never fails on unrelated pre-existing unused imports elsewhere in the file.
    """
    errors = []
    body_without_imports = re.sub(r'^\s*import\s+[^\n]+', '', clean, flags=re.MULTILINE)
    removed_blob = "\n".join(removed_lines)

    added_import_lines = {ln for ln, _ in added_lines}
    for idx, line in enumerate(raw.splitlines(), 1):
        m = re.match(r'^\s*import\s+([\w.]+)(?:\s+as\s+(\w+))?\s*$', line)
        if not m:
            continue
        fqn = m.group(1)
        alias = m.group(2)
        if fqn.endswith(".*"):
            continue
        sym = alias or fqn.rsplit(".", 1)[-1]
        if sym in IMPLICIT_KOTLIN_IMPORTS:
            continue

        is_newly_added = idx in added_import_lines
        was_removed_from_body = bool(re.search(rf'\b{re.escape(sym)}\b', removed_blob))
        if not (is_newly_added or was_removed_from_body):
            continue

        if not re.search(rf'\b{re.escape(sym)}\b', body_without_imports):
            reason = "newly added" if is_newly_added else "orphaned by this diff"
            errors.append(f"[UNUSED_IMPORT] {filepath}:{idx} -> Import '{sym}' ({reason}) is unused.")
    return errors


def check_added_line_hazards(
    filepath: str,
    clean: str,
    added_lines: list[tuple[int, str]]
) -> list[str]:
    """Checks newly added lines for `!!`, swallowed CancellationException, and anonymous Backdrop."""
    errors = []
    clean_lines = clean.splitlines()

    for line_no, _raw_added in added_lines:
        if line_no < 1 or line_no > len(clean_lines):
            continue
        cline = clean_lines[line_no - 1]

        # 1. Newly introduced `!!`
        if "!!" in cline:
            errors.append(f"[UNSAFE_NON_NULL] {filepath}:{line_no} -> Newly added '!!' non-null assertion is forbidden.")

        # 2. Anonymous `object : Backdrop` (breaks DrawBackdropModifier's LayerBackdrop/CombinedBackdrop cast)
        if re.search(r'\bobject\s*:\s*Backdrop\b', cline):
            errors.append(
                f"[GLASS_INVARIANT] {filepath}:{line_no} -> Anonymous 'object : Backdrop' will NOT render in "
                f"DrawBackdropModifier (currentBackdropOffset returns null). Use 'LayerBackdrop' or 'CombinedBackdrop'."
            )

        # 3. Added `catch (e: Exception)` / `catch (e: Throwable)` inside suspend/coroutine contexts
        cm = re.search(r'\bcatch\s*\(\s*(\w+)\s*:\s*(?:Exception|Throwable)\s*\)', cline)
        if cm:
            var_name = cm.group(1)
            # Check surrounding 15 lines after catch for rethrow or CancellationException handling
            window_after = "\n".join(clean_lines[line_no - 1:min(len(clean_lines), line_no + 20)])
            window_before = "\n".join(clean_lines[max(0, line_no - 35):line_no])
            in_coroutine = bool(re.search(r'\b(suspend\s+fun|launch|async|withContext|flow\s*\{)\b', window_before))
            if in_coroutine:
                has_cancel_guard = (
                    "CancellationException" in window_after
                    or re.search(rf'\bthrow\s+{re.escape(var_name)}\b', window_after) is not None
                )
                if not has_cancel_guard:
                    errors.append(
                        f"[SWALLOWED_CANCELLATION] {filepath}:{line_no} -> Added 'catch ({var_name}: ...)' "
                        f"inside coroutine/suspend context without rethrowing CancellationException."
                    )

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

    # 5. Backdrop Detach Wipe, Double layerBlock & Mirroring Loop
    if "DrawBackdrop" in filepath:
        if re.search(r'fun\s+onDetach\s*\([^)]*\)\s*\{[^}]*layerCoordinates\s*=\s*null', clean, re.DOTALL):
            errors.append(f"[BACKDROP_DAG] {filepath} -> DrawBackdropNode.onDetach() nullifies shared layerCoordinates!")
        if re.search(r'layoutLayerBlock\s*:\s*GraphicsLayerScope\.\(\)\s*->\s*Unit\s*=\s*\{[^}]*layerBlock\?\.invoke', clean, re.DOTALL):
            errors.append(f"[GLASS_INVARIANT] {filepath} -> layoutLayerBlock invokes layerBlock a second time (squares scale & alpha).")

    if filepath.endswith("Lens.kt"):
        if "coerceAtLeast" not in clean and "maxOf" not in clean:
            errors.append(f"[GLASS_INVARIANT] {filepath} -> Missing anti-clipping 'neededPadding' (refractionAmount.coerceAtLeast(refractionHeight)).")

    if filepath.endswith(("ChatMessageV2.kt", "GroupedMessageBubble.kt")) and "overlayBackdrop" in clean:
        errors.append(f"[BACKDROP_DAG] {filepath} binds message bubble to 'overlayBackdrop' (causes recursive mirroring). Use 'wallpaperBackdrop'.")

    # 6. Drawer Scroll Gradient Ban
    if filepath.endswith("ConversationList.kt") and re.search(r'canScrollBackward\s*\)\s*\{[^}]*verticalGradient', clean, re.DOTALL):
        errors.append(f"[AMOLED_DESIGN] {filepath} re-introduced banned scroll gradient bar.")

    return errors


def check_broken_deleted_symbols(per_file: dict[str, tuple[list[tuple[int, str]], list[str]]]) -> list[str]:
    """
    If a non-private fun/class/object was removed in this diff and not re-added in the same file,
    grep app/src/ to ensure no dangling callers remain in other files.
    """
    errors = []
    decl_pat = re.compile(r'^\s*(?:suspend\s+|inline\s+|internal\s+|public\s+)?(fun|class|object)\s+([A-Za-z_]\w*)\s*[\(<{:]')
    for filepath, (added, removed) in per_file.items():
        if not filepath.endswith(".kt"):
            continue
        added_blob = "\n".join(line for _, line in added)
        for rline in removed:
            if re.match(r'^\s*private\b', rline):
                continue
            m = decl_pat.match(rline)
            if not m:
                continue
            kind, sym = m.group(1), m.group(2)
            if len(sym) < 5:
                continue
            # Skip if re-declared in added lines or still present in the file
            if re.search(rf'\b(?:fun|class|object)\s+{re.escape(sym)}\b', added_blob):
                continue
            target_path = REPO_ROOT / filepath
            if target_path.exists() and re.search(rf'\b(?:fun|class|object)\s+{re.escape(sym)}\b', target_path.read_text(encoding="utf-8")):
                continue
            # For functions, search for actual call sites `sym(` or `::sym` (avoiding named params `sym =`)
            if kind == "fun":
                hits = run_git(["grep", "-n", "-E", rf'(\b{sym}\s*\(|::{sym}\b)', "--", "app/src/main/"]).splitlines()
            else:
                hits = run_git(["grep", "-n", "-w", sym, "--", "app/src/main/"]).splitlines()
            external_hits = [h for h in hits if not h.startswith(filepath + ":")]
            if external_hits:
                sample = external_hits[0].split(":", 2)
                loc = f"{sample[0]}:{sample[1]}" if len(sample) >= 2 else external_hits[0]
                errors.append(
                    f"[BROKEN_CALLER] {filepath} removed '{sym}', but {len(external_hits)} reference(s) remain (e.g. {loc})."
                )
    return errors


def check_diff_hazards(diff: str, all_files: list[str], allow_override: bool, check_commit: str | None) -> list[str]:
    errors = []

    # 0. Plan Markdown Blocker
    for f in all_files:
        if re.search(r'(?:FIX_PLAN|PRODUCTION_FIX_PLAN).*\.md$', f, re.IGNORECASE):
            errors.append(f"[PLAN_FILE_STAGED] '{f}' is a plan markdown file and MUST remain untracked (use 'git add -u').")

    if not diff:
        return errors

    removed_lines = [l[1:] for l in diff.splitlines() if l.startswith("-") and not l.startswith("---")]
    added_lines = [l[1:] for l in diff.splitlines() if l.startswith("+") and not l.startswith("+++")]

    # 1. Anti-Eraser Check
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
    check_commit = None
    if "--check-commit" in sys.argv:
        idx = sys.argv.index("--check-commit")
        if idx + 1 < len(sys.argv):
            check_commit = sys.argv[idx + 1]

    files, diff, all_files = get_diff_and_files(check_commit)
    if not files and not all_files:
        print("✓ DomChat Pre-Flight: Working tree clean.")
        sys.exit(0)

    all_errors = []
    if len(files) > MAX_FILES_PER_COMMIT and not allow_override:
        all_errors.append(
            f"[BATCH_HALLUCINATION_GUARD] {len(files)} files modified at once (Max allowed is {MAX_FILES_PER_COMMIT}). "
            f"Fix and verify 1 issue group at a time, or pass --allow-wide."
        )

    per_file = parse_file_diffs(diff)

    for f in files:
        raw = (REPO_ROOT / f).read_text(encoding="utf-8")
        clean = strip_comments_and_strings(raw)
        added, removed = per_file.get(f, ([], []))

        all_errors.extend(check_balanced_delimiters(f, clean))
        all_errors.extend(check_missing_imports(f, raw, clean))
        all_errors.extend(check_kotlin_type_inference(f, clean))
        all_errors.extend(check_unused_imports_in_diff(f, raw, clean, added, removed))
        all_errors.extend(check_added_line_hazards(f, clean, added))
        all_errors.extend(check_domchat_architecture(f, raw, clean))

    all_errors.extend(check_broken_deleted_symbols(per_file))
    all_errors.extend(check_diff_hazards(diff, all_files, allow_override, check_commit))

    if all_errors:
        print("\n" + "=" * 70)
        print("✗ DOMCHAT PRE-FLIGHT GATE REJECTED THE WORKER'S CHANGES:")
        print("=" * 70)
        for e in all_errors:
            print(f"  • {e}")
        print("=" * 70)
        sys.exit(1)

    print(f"✓ DomChat Pre-Flight V3 PASSED ({len(files)} file(s) verified in <2s).")


if __name__ == "__main__":
    main()
