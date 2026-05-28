# Design Scripts

本目录存放可执行的静态检查和索引生成脚本。脚本只辅助检查，不替代当前代码同类实现和 `AGENTS.md` / `design/*` 规范判断。

## check_android_xml_tokens.ps1

检查 Android XML 中常见的 UI 规范风险：

- 裸 `#RRGGBB` / `#AARRGGBB` 色值。
- 可映射到 `spacing_*` 的常见间距值：`4dp`、`8dp`、`12dp`、`16dp`、`24dp`。
- 可映射到 `radius_*` 的常见圆角值：`4dp`、`8dp`、`12dp`、`16dp`。
- `sans-serif`、`sans-serif-medium`、`PingFang SC` 等字体兜底。
- 局部 `letterSpacing` / `lineHeight`。
- 简单 drawable 的泛名或非值命名。

常用命令：

```powershell
powershell -ExecutionPolicy Bypass -File design/scripts/check_android_xml_tokens.ps1 -ChangedOnly
powershell -ExecutionPolicy Bypass -File design/scripts/check_android_xml_tokens.ps1 -ChangedLinesOnly
powershell -ExecutionPolicy Bypass -File design/scripts/check_android_xml_tokens.ps1 -Path app/src/main/res/layout/activity_series.xml
powershell -ExecutionPolicy Bypass -File design/scripts/check_android_xml_tokens.ps1 -MaxIssues 50
```

默认扫描 `app/src/main/res/layout*` 和 `app/src/main/res/drawable*` 下的 XML。发现问题默认只输出报告，不让脚本失败；需要 CI 风格失败时传 `-FailOnIssue`。

- `-ChangedOnly`：扫描当前 Git 变更中的 XML 文件，适合局部检查。
- `-ChangedLinesOnly`：只报告当前 diff 新增/修改行上的问题，适合避免历史 XML 问题干扰本次小改。
- `-Path`：指定一个文件或目录检查；可与 `-ChangedLinesOnly` 组合，只在该路径内报告 diff 行问题。

## scan_code_interface.ps1

按当前分支重新扫描主工程源码，生成代码接口索引摘要。默认输出到终端，不写文件；需要写入文件时显式传 `-WriteSummary`。

常用命令：

```powershell
powershell -ExecutionPolicy Bypass -File design/scripts/scan_code_interface.ps1
powershell -ExecutionPolicy Bypass -File design/scripts/scan_code_interface.ps1 -WriteSummary
powershell -ExecutionPolicy Bypass -File design/scripts/scan_code_interface.ps1 -WriteSummary -OutputPath design/code-interface-scan-summary.generated.md
powershell -ExecutionPolicy Bypass -File design/scripts/scan_code_interface.ps1 -ComparePath design/code-interface-scan-summary.md
```

生成结果仍是快照。触达 API、SDK、Room、Manifest、Gradle、路由或语言资源前，必须再用 `rg` 或直接读取当前文件确认。

- `-ComparePath`：把当前扫描结果和指定快照做文本差异对比，默认忽略生成时间行。
- `-FailOnDiff`：有差异时返回失败码，适合后续 CI 或人工刷新前检查。
