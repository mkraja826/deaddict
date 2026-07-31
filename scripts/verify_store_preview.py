#!/usr/bin/env python3
"""Validate that Play screenshot fixtures stay fictional and debug-only."""

from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MAIN_MANIFEST = ROOT / "app/src/main/AndroidManifest.xml"
DEBUG_MANIFEST = ROOT / "app/src/debug/AndroidManifest.xml"
PREVIEW_SOURCE = ROOT / "app/src/debug/java/com/deaddict/app/store/StorePreviewActivity.kt"
CAPTURE_WORKFLOW = ROOT / ".github/workflows/capture-store-screenshots.yml"
CAPTURE_DOC = ROOT / "docs/play/SCREENSHOT_CAPTURE.md"


def require_file(path: Path, minimum_bytes: int = 1) -> str:
    if not path.is_file():
        raise SystemExit(f"Missing required store-preview file: {path.relative_to(ROOT)}")
    if path.stat().st_size < minimum_bytes:
        raise SystemExit(f"Store-preview file is incomplete: {path.relative_to(ROOT)}")
    return path.read_text(encoding="utf-8")


def main() -> None:
    main_manifest = require_file(MAIN_MANIFEST, 300)
    debug_manifest = require_file(DEBUG_MANIFEST, 150)
    source = require_file(PREVIEW_SOURCE, 2_000)
    workflow = require_file(CAPTURE_WORKFLOW, 1_000)
    require_file(CAPTURE_DOC, 1_000)

    if "StorePreviewActivity" in main_manifest:
        raise SystemExit("StorePreviewActivity must never be registered in the production manifest")
    if "StorePreviewActivity" not in debug_manifest:
        raise SystemExit("Debug manifest must register StorePreviewActivity")
    if 'android:exported="false"' not in debug_manifest:
        raise SystemExit("StorePreviewActivity must remain non-exported")

    forbidden_source_tokens = {
        "com.deaddict.database": "production database dependency",
        "io.github.jan.supabase": "Supabase dependency",
        "RookPreferenceStore": "real DataStore preference access",
        "AuthGateway": "real authentication access",
        "PlayBillingManager": "real billing access",
        "UsageStatsManager": "real Android usage access",
        "privateNote =": "private-note fixture",
    }
    for token, description in forbidden_source_tokens.items():
        if token in source:
            raise SystemExit(f"Store preview contains {description}: {token}")

    required_tracks = {"Social media", "Caffeine"}
    missing_tracks = sorted(track for track in required_tracks if track not in source)
    if missing_tracks:
        raise SystemExit(f"Fictional Recovery Tracks are missing: {', '.join(missing_tracks)}")

    required_screens = {"today", "tracks", "rescue", "insights", "you"}
    enum_keys = set(re.findall(r'\("([a-z]+)",\s*"', source))
    if not required_screens.issubset(enum_keys):
        raise SystemExit(
            "Store preview screen keys are incomplete: "
            + ", ".join(sorted(required_screens - enum_keys))
        )
    for screen in required_screens:
        if screen not in workflow:
            raise SystemExit(f"Capture workflow does not include {screen}")

    if "1080" not in workflow or "1920" not in workflow:
        raise SystemExit("Capture workflow must enforce 1080 x 1920 dimensions")
    if "workflow_dispatch" not in workflow:
        raise SystemExit("Store screenshots must be captured through an explicit manual workflow")

    print("Store preview verified: debug-only, non-exported, fictional, and privacy isolated.")


if __name__ == "__main__":
    main()
