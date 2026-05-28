# Android 快速检查

本文件用于“检查一下”“不走 Gradle”“局部确认”类任务。只有发现问题、任务跨模块或用户要求完整审查时，才读取 `android-codegen-checklist.md`。

## 通用流程

1. 用 `rg -n` 定位目标符号、id、资源名和旧逻辑残留。
2. 只读取命中附近片段，不整文件读取大文件。
3. 检查直接引用是否存在。
4. 按用户要求决定是否解析 XML；不默认跑 Gradle。
5. 输出只说结论、验证范围和风险。

## Kotlin/逻辑

- 新状态字段、方法、点击分支是否接上。
- 旧手动逻辑是否残留。
- 关键 import、常量、BindingAdapter、回调方法是否存在。
- 是否误引入 Hilt、UseCase、AppResult/AppError、完整 MVI。
- 是否直接在 Activity/Fragment 发网络请求或在 Adapter 写业务请求。
- 新增复杂/风险逻辑是否有必要中文原因注释，且没有堆模板化废话注释。
- Fragment 中 `onPrepareView()` 注册的手动回调是否在 `onDestroy()` 解除；除非同类或基类明确如此，不要用 `onDestroyView()` 清理。

## XML/资源

- 目标 XML 能否解析。
- 本次触达的 `@drawable/@string/@color/@dimen/@style` 直接引用是否存在。
- DataBinding 变量是否为项目习惯的 `layout` / `item`。
- 本次触达行是否违反 token 规则：可映射间距/圆角、`Text1` 到 `?attr/textColor1`、裸色值、字体兜底、无必要 `letterSpacing/lineHeight`。
- 新增 drawable 是否先查等价资源，并按值命名。
- 需要批量或脚本化检查 XML token 时，可运行 `design/scripts/check_android_xml_tokens.ps1`；默认优先检查本次触达文件或 `-ChangedLinesOnly`，不要为了局部任务全量清理历史问题。

## Figma/XML

- 有 `node-id` 时是否只读目标节点。
- 目标 XML/同类 XML 是否定位到。
- 只检查本次触达区域的结构、资源映射和直接资源引用。
- 视觉额外发现可以列出，但不要拖慢主检查。

## 输出

- 没问题：直接说明“未发现阻断问题”，列出已检查范围。
- 有问题：按严重程度列文件、行号、问题、影响、建议。
- 未跑 Gradle：明确说明“按要求未跑 Gradle，仅做静态检查”。

### 输出模板

未发现问题时：

```text
未发现阻断问题。

检查范围：
- 目标 Kotlin/XML/资源文件。
- 关键调用点、直接资源引用和旧逻辑残留。

未验证：
- 按要求未跑 Gradle，仅做静态检查。
```

发现问题时：

```text
问题：
1. [严重级别] 文件路径:行号 - 问题描述。
   影响：说明可能导致的行为、构建或维护风险。
   建议：给出最小修改建议。

检查范围：
- 本次实际检查过的文件、符号、资源或 XML。

未验证：
- 未跑 Gradle、未实机验证、接口字段待确认等。
```

快速修改完成时：

```text
已完成。

改动：
- 修改了什么行为或资源。

检查：
- 静态确认了哪些调用点、XML 引用或资源存在性。

风险：
- 只列本次仍需人工确认的点。
```
