#!/usr/bin/env python3
"""Validate the repository layout after the forum + mall consolidation.

This script focuses on structure-level checks so we can quickly detect path drift
after future refactors without requiring a full Maven build.
"""

from __future__ import annotations

import sys
import xml.etree.ElementTree as element_tree
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
MAVEN_NS = {"m": "http://maven.apache.org/POM/4.0.0"}

# These directories define the consolidated repository skeleton after the merge.
REQUIRED_ROOT_DIRS = (
    "apps",
    "services",
    "shared",
    "platform",
    "docs",
    "deploy",
    "sql",
    "scripts",
)

# Keep the frontend contract small but explicit so proxy and shell entry points stay discoverable.
REQUIRED_FRONTEND_FILES = (
    "apps/frontend/package.json",
    "apps/frontend/vite.config.js",
    "apps/frontend/src/App.vue",
    "apps/frontend/src/router/index.js",
    "apps/frontend/.env.example",
)


def parse_pom(pom_path: Path) -> element_tree.ElementTree:
    """Parse a Maven POM and raise a descriptive error when XML is invalid."""

    try:
        return element_tree.parse(pom_path)
    except element_tree.ParseError as exc:
        raise ValueError(f"invalid XML in {pom_path.relative_to(ROOT)}: {exc}") from exc


def validate_root_layout(issues: list[str]) -> None:
    """Check whether the top-level engineering directories still exist."""

    for directory in REQUIRED_ROOT_DIRS:
        if not (ROOT / directory).exists():
            issues.append(f"missing required root directory: {directory}")

    for file_path in REQUIRED_FRONTEND_FILES:
        if not (ROOT / file_path).exists():
            issues.append(f"missing required frontend file: {file_path}")


def validate_poms(issues: list[str]) -> None:
    """Validate root modules plus every child POM parent/module reference."""

    pom_files = sorted(ROOT.rglob("pom.xml"))
    if not pom_files:
        issues.append("no pom.xml files found under repository root")
        return

    for pom_file in pom_files:
        try:
            pom_tree = parse_pom(pom_file)
        except ValueError as exc:
            issues.append(str(exc))
            continue

        project = pom_tree.getroot()

        parent = project.find("m:parent", MAVEN_NS)
        if parent is not None:
            relative_path = parent.findtext("m:relativePath", default="", namespaces=MAVEN_NS).strip()
            # Spring Boot parents often use <relativePath/> intentionally, so only validate non-empty values.
            if relative_path:
                target = (pom_file.parent / relative_path).resolve()
                if not target.exists():
                    issues.append(
                        f"missing parent relativePath target: {pom_file.relative_to(ROOT)} -> {relative_path}"
                    )

        modules = project.find("m:modules", MAVEN_NS)
        if modules is None:
            continue

        for module in modules.findall("m:module", MAVEN_NS):
            module_path = (pom_file.parent / module.text.strip()).resolve()
            if not module_path.exists():
                issues.append(f"missing module path: {pom_file.relative_to(ROOT)} -> {module.text.strip()}")


def main() -> int:
    """Run all structural checks and print a concise report."""

    issues: list[str] = []
    validate_root_layout(issues)
    validate_poms(issues)

    if issues:
        print("STRUCTURE CHECK FAILED")
        for issue in issues:
            print(f"- {issue}")
        return 1

    pom_count = sum(1 for _ in ROOT.rglob("pom.xml"))
    print("STRUCTURE CHECK PASSED")
    print(f"- repository root: {ROOT}")
    print(f"- validated pom files: {pom_count}")
    print(f"- validated frontend files: {len(REQUIRED_FRONTEND_FILES)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
