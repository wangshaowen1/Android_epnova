#!/usr/bin/env python3
"""按 write-project-docs 规范审计仓库 Markdown 文档。"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path


REQUIRED = {"title", "type", "status", "last_verified"}
ALLOWED_TYPES = {"overview", "architecture", "feature", "contract"}
ALLOWED_STATUS = {"draft", "active", "deprecated"}
ALLOWED_IMPLEMENTATION = {"planned", "in-progress", "complete"}
BUDGETS = {"overview": 180, "architecture": 350, "feature": 250, "contract": 400}
ABSOLUTE_PATH = re.compile(
    r"(?<![A-Za-z0-9_])(?:[A-Za-z]:[\\/]|file://|/(?:Users|home)/[^\s)`]+)"
)
MARKDOWN_LINK = re.compile(r"(?<!!)\[[^\]]+\]\(([^)]+)\)")
SEMVER_SEGMENT = re.compile(r"^v?\d+\.\d+(?:\.\d+)?(?:[-+][0-9A-Za-z.-]+)?$")
KEBAB_NAME = re.compile(r"^[a-z0-9]+(?:-[a-z0-9]+)*$")
DATE_VALUE = re.compile(r"^\d{4}-\d{2}-\d{2}$")


def parse_frontmatter(text: str) -> tuple[dict[str, str], bool]:
    lines = text.splitlines()
    if not lines or lines[0].strip() != "---":
        return {}, False
    metadata: dict[str, str] = {}
    for line in lines[1:]:
        if line.strip() == "---":
            return metadata, True
        if not line.strip() or line.lstrip().startswith("#"):
            continue
        match = re.match(r"^([a-z_]+):\s*(.*?)\s*$", line)
        if match:
            metadata[match.group(1)] = match.group(2).strip('"\'')
    return metadata, False


def discover_markdown(target: Path) -> list[Path]:
    if target.is_file():
        return [target] if target.suffix.lower() == ".md" else []
    docs = target / "docs" if (target / "docs").is_dir() else target
    return sorted(path for path in docs.rglob("*.md") if ".git" not in path.parts)


def audit_file(path: Path, root: Path) -> tuple[list[str], list[str]]:
    errors: list[str] = []
    warnings: list[str] = []
    relative = path.relative_to(root) if path.is_relative_to(root) else path
    text = path.read_text(encoding="utf-8-sig")
    lines = text.splitlines()
    metadata, closed = parse_frontmatter(text)
    in_archive = "archive" in {part.lower() for part in path.parts}
    doc_type = metadata.get("type")

    if not metadata:
        if not in_archive:
            errors.append("缺少 YAML frontmatter")
    elif not closed:
        errors.append("frontmatter 缺少结束标记 ---")
    else:
        missing = sorted(REQUIRED - metadata.keys())
        if missing and not in_archive:
            errors.append(f"缺少元数据：{', '.join(missing)}")

        status = metadata.get("status")
        if doc_type and doc_type not in ALLOWED_TYPES:
            errors.append(f"不支持的 type：{doc_type}")
        if status and status not in ALLOWED_STATUS:
            errors.append(f"不支持的 status：{status}")
        last_verified = metadata.get("last_verified")
        if last_verified and not DATE_VALUE.fullmatch(last_verified):
            errors.append("last_verified 必须使用 YYYY-MM-DD")
        if doc_type == "feature":
            implementation = metadata.get("implementation")
            if implementation not in ALLOWED_IMPLEMENTATION:
                errors.append("feature 必须声明 implementation: planned|in-progress|complete")

    if not in_archive:
        inferred_budget = BUDGETS.get(doc_type)
        if inferred_budget is None:
            inferred_budget = 180 if path.name == "README.md" else 350
            if "features" in {part.lower() for part in path.parts}:
                inferred_budget = 250
        if len(lines) > inferred_budget:
            warnings.append(f"共 {len(lines)} 行，超过 {inferred_budget} 行软预算")

    for line_number, line in enumerate(lines, start=1):
        if ABSOLUTE_PATH.search(line):
            errors.append(f"第 {line_number} 行包含开发机器绝对路径")

    for raw_link in MARKDOWN_LINK.findall(text):
        link = raw_link.strip().split(maxsplit=1)[0].strip("<>")
        if not link or link.startswith(("#", "http://", "https://", "mailto:")):
            continue
        path_part = link.split("#", 1)[0]
        if path_part and not (path.parent / path_part).resolve().exists():
            errors.append(f"相对链接失效：{raw_link}")

    for segment in relative.parts[:-1]:
        if SEMVER_SEGMENT.fullmatch(segment):
            warnings.append(f"使用版本目录层级：{segment}")
    if path.stem != "README" and not in_archive and not KEBAB_NAME.fullmatch(path.stem):
        warnings.append("文件名不是 lower-kebab-case")

    return errors, warnings


def run_audit(target: Path, strict: bool = False) -> int:
    target = target.resolve()
    if not target.exists():
        print(f"错误：目标不存在：{target}")
        return 2
    files = discover_markdown(target)
    if not files:
        print(f"错误：目标下没有 Markdown 文档：{target}")
        return 2

    root = target if target.is_dir() else target.parent
    error_count = 0
    warning_count = 0
    docs_root = target / "docs" if target.is_dir() and (target / "docs").is_dir() else target
    if target.is_dir() and docs_root.is_dir() and not (docs_root / "README.md").exists():
        warning_count += 1
        print(f"警告 {docs_root}：缺少文档索引 README.md")

    for path in files:
        errors, warnings = audit_file(path, root)
        label = path.relative_to(root) if path.is_relative_to(root) else path
        for message in errors:
            error_count += 1
            print(f"错误 {label}：{message}")
        for message in warnings:
            warning_count += 1
            print(f"警告 {label}：{message}")

    print(f"已审计 {len(files)} 份 Markdown：{error_count} 个错误，{warning_count} 个警告")
    return 1 if error_count or (strict and warning_count) else 0


def self_test() -> int:
    import tempfile

    with tempfile.TemporaryDirectory() as temp_dir:
        root = Path(temp_dir)
        valid = root / "docs" / "features" / "sample-feature.md"
        valid.parent.mkdir(parents=True)
        valid.write_text(
            "---\n"
            "title: 示例功能\n"
            "type: feature\n"
            "status: active\n"
            "last_verified: 2026-08-04\n"
            "implementation: complete\n"
            "---\n\n"
            "# 示例功能\n",
            encoding="utf-8",
        )
        errors, warnings = audit_file(valid, root)
        if errors or warnings:
            print(f"自测失败（合法样例）：{errors=} {warnings=}")
            return 1

        contract = root / "docs" / "contracts" / "sample-contract.md"
        contract.parent.mkdir(parents=True)
        contract.write_text(
            "---\n"
            "title: 示例契约\n"
            "type: contract\n"
            "status: active\n"
            "last_verified: 2026-08-04\n"
            "---\n\n"
            "# 示例契约\n",
            encoding="utf-8",
        )
        errors, warnings = audit_file(contract, root)
        if errors or warnings:
            print(f"自测失败（契约样例）：{errors=} {warnings=}")
            return 1

        invalid = root / "docs" / "features" / "bad.md"
        invalid.write_text("# 错误样例\n\n本地路径：C:\\Users\\name\\project\n", encoding="utf-8")
        errors, _ = audit_file(invalid, root)
        if len(errors) < 2:
            print(f"自测失败（非法样例）：{errors=}")
            return 1
    print("自测通过")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("target", nargs="?", help="仓库、docs 目录或 Markdown 文件")
    parser.add_argument("--strict", action="store_true", help="将警告也视为失败")
    parser.add_argument("--self-test", action="store_true", help="运行内置自测")
    args = parser.parse_args()
    if args.self_test:
        return self_test()
    if not args.target:
        parser.error("除 --self-test 外必须提供 target")
    return run_audit(Path(args.target), args.strict)


if __name__ == "__main__":
    sys.exit(main())
