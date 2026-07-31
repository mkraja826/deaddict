#!/usr/bin/env python3
"""Render and validate the static DeAddict public support site."""

from __future__ import annotations

import argparse
import html
import re
import shutil
from html.parser import HTMLParser
from pathlib import Path
from urllib.parse import urlsplit

REQUIRED_FILES = {
    "index.html",
    "privacy.html",
    "terms.html",
    "support.html",
    "account-deletion.html",
    "styles.css",
}
IDENTITY_MARKERS = ("__DEVELOPER_NAME__", "__SUPPORT_EMAIL__")
EMAIL_PATTERN = re.compile(r"[^\s@]+@[^\s@]+\.[^\s@]+")


class LinkParser(HTMLParser):
    def __init__(self) -> None:
        super().__init__()
        self.links: list[str] = []

    def handle_starttag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        if tag != "a":
            return
        href = dict(attrs).get("href")
        if href:
            self.links.append(href)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", type=Path, default=Path("docs/site"))
    parser.add_argument("--output", type=Path, default=Path("_site"))
    parser.add_argument("--developer-name", required=True)
    parser.add_argument("--support-email", required=True)
    parser.add_argument("--require-public-identity", action="store_true")
    return parser.parse_args()


def validate_identity(developer_name: str, support_email: str, required: bool) -> None:
    if not required:
        return
    if len(developer_name.strip()) < 2 or "preview" in developer_name.lower():
        raise SystemExit("A verified public developer name is required for deployment")
    if not EMAIL_PATTERN.fullmatch(support_email):
        raise SystemExit("A valid public support email is required for deployment")
    if support_email.endswith(".invalid") or "example." in support_email.lower():
        raise SystemExit("A placeholder support email cannot be deployed")


def render_site(
    source: Path,
    output: Path,
    developer_name: str,
    support_email: str,
) -> None:
    missing = sorted(name for name in REQUIRED_FILES if not (source / name).is_file())
    if missing:
        raise SystemExit(f"Missing public-site files: {', '.join(missing)}")

    if output.exists():
        shutil.rmtree(output)
    shutil.copytree(source, output)

    replacements = {
        "__DEVELOPER_NAME__": html.escape(developer_name.strip(), quote=True),
        "__SUPPORT_EMAIL__": html.escape(support_email.strip(), quote=True),
    }
    for page in output.glob("*.html"):
        content = page.read_text(encoding="utf-8")
        for marker, value in replacements.items():
            content = content.replace(marker, value)
        unresolved = [marker for marker in IDENTITY_MARKERS if marker in content]
        if unresolved:
            raise SystemExit(f"Unresolved identity marker in {page}: {', '.join(unresolved)}")
        page.write_text(content, encoding="utf-8")

    (output / ".nojekyll").write_text("", encoding="utf-8")


def validate_links(output: Path) -> None:
    root = output.resolve()
    for page in output.glob("*.html"):
        parser = LinkParser()
        parser.feed(page.read_text(encoding="utf-8"))
        for href in parser.links:
            if href.startswith(("https://", "mailto:", "#")):
                continue
            if href.startswith(("http://", "javascript:", "data:")):
                raise SystemExit(f"Unsafe link in {page}: {href}")
            path = urlsplit(href).path
            if not path:
                continue
            target = (page.parent / path).resolve()
            if root not in target.parents and target != root:
                raise SystemExit(f"Link escapes site root in {page}: {href}")
            if not target.is_file():
                raise SystemExit(f"Broken internal link in {page}: {href}")


def main() -> None:
    args = parse_args()
    developer_name = args.developer_name.strip()
    support_email = args.support_email.strip()
    validate_identity(developer_name, support_email, args.require_public_identity)
    render_site(args.source, args.output, developer_name, support_email)
    validate_links(args.output)
    print(f"Validated {len(list(args.output.glob('*.html')))} public HTML pages")


if __name__ == "__main__":
    main()
