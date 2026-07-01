---
name: decisions
description: 版本决策、踩坑与验证结论（追加式情节记忆，按需手动维护）
metadata:
  type: project
---

> 情节记忆：记录「发生了什么」——版本决策、踩坑、验证结论。**只追加，不删除、不改写历史条目。**
> 由对话中按需判断后手动追加（用户说“本轮有没有需要更新的 memory？”时触发）。

## 条目格式

```
## v<版本或"本轮"> <变更主题>

### 变更了什么
<字段 / 文件> → <新增 / 删除 / 修改>：<一句话>

### 为什么
<具体症状：哪个版本、哪一处、出现了什么问题>
<根因分析 + 决策推导>

### 验证结论
<验证方式 + 结果，含具体数据> 或 PENDING（说明待验证项）
```

---

## 本轮 agent 工作流本地化迁移

### 变更了什么
旧本地化资料 → `.agents/` 新结构：业务专项文档迁入 `.agents/project/`，代码接口扫描摘要迁入 `.agents/code-interface-scan-summary.md`，场景 skill 示例迁入 `.agents/skills/*/references/`。

### 为什么
`.agents/` 来自新工作流结构，但包含通用占位和错误 token 假设；旧本地化资料保留了 Epnova 的真实业务资料、资源 token 和可复用组件线索。为避免 agent 继续按模板生成不存在的包名、色值或样式，规则和记忆必须以当前分支代码事实为准。

### 验证结论
PENDING：已静态核实 `com.kuaigeglobal.epnova`、`EpNova`、`epnovascheme`、`epnova.onelink.me`、`themeColor1/textColor1/bgColor1/fillColor1`、`Text28/Text16` 等当前事实；旧本地化目录删除需等待用户确认清单后执行。

## 本轮 Figma/XML UI 还原资源回溯

### 变更了什么
Android Figma/XML UI 还原口径 → 补充执行记忆：纯文字区域默认自适应高度；新增颜色和纯背景资源按项目资源规则落地并引用；项目没有字体资源时不写 `fontFamily` / `Typeface` 模拟设计字体。

### 为什么
同源 Android 项目 UI 还原中出现过一次执行偏差：过度按 Figma 固定 frame 硬还原，导致纯文字在 Android 实际渲染中存在被遮挡风险；同时先用了业务语义色名、局部裸色值和字体引用，未第一时间按项目 XML/资源/命名规则收敛。
后续确认的决策是：Figma 固定尺寸不等于 Android 文本固定高度；纯文字用 `wrap_content`，选择卡片可用 `wrap_content` / `minHeight` 保持同内容自然同高；新增颜色进 `colors.xml` 后通过 `@color` / `R.color` 引用；纯背景、描边、渐变 drawable 优先按色值/形态命名。

### 验证结论
SYNCED：该条为跨项目 agent 执行记忆同步，用于 EPnova 后续 Figma/XML UI 还原前置检查；具体页面实现仍以 EPnova 当前分支代码、资源和 `.agents` 规则为准。

## 本轮 Google Play 图片权限拒审迁移

### 变更了什么
选图入口 → 从申请媒体读取权限后打开 Matisse，迁移为 Android Photo Picker；反馈、举报、WebView 文件上传、作品封面编辑均不再通过 `requestImages2VideosPermissions` 进入相册。

### 为什么
Google Play 因照片和视频权限政策拒审，指出应用对 `READ_MEDIA_IMAGES` / `READ_MEDIA_VIDEO` 的使用与核心用途无直接关系。当前业务仅是用户主动选择少量图片，符合系统 Photo Picker 的一次性授权模型，不应申请持续媒体库读取权限。

### 验证结论
STATIC：已静态确认业务调用点不再调用 `requestImages2VideosPermissions` 或 Matisse；主 Manifest 对 Android 13+ 媒体读取权限使用 `tools:node="remove"`，旧权限工具在 Android 13+ 不再返回媒体读取权限。未跑 Gradle，需以实际构建后的 merged manifest 再确认权限最终移除。

## 本轮 Google Play 图片权限迁移修正

### 变更了什么
权限工具类 → 保持原有媒体权限封装能力，不在工具层废弃或清空 Android 13+ 图片权限；本次过审策略只落在业务入口迁移 Photo Picker 和当前 App Manifest 移除媒体读取权限。

### 为什么
`common/permissions` 和 Matisse 相关权限代码后续可能被其他项目复用，也可能在业务确实需要广泛媒体访问时重新放开。把审核策略写死到工具层会降低复用性。

### 验证结论
STATIC：已回退 `PermissionsExt.kt`、`PermissionsUtils.kt`、Matisse 内部权限辅助和扩展授权逻辑的本轮改动；业务调用点仍不再调用 `requestImages2VideosPermissions` 或 Matisse 选图入口。未跑 Gradle。

## 本轮 AppImagePicker 双选择器适配

### 变更了什么
图片选择入口 → 新增 App 层 `AppImagePicker` 统一封装：Android 13+ 使用系统 Photo Picker，Android 12 及以下沿用 `requestImages2VideosPermissions` + Matisse；业务页面只接收统一 `List<Uri>` 回调。

### 为什么
直接在每个页面写 Android 版本判断、权限申请、Photo Picker 回调和 Matisse `onActivityResult` 会造成重复逻辑；把分支收敛到 App 层适配器，既保留 Android 12 及以下原 Matisse UI，又避免修改权限工具类和 Matisse 内部职责。

### 验证结论
STATIC：反馈、举报、WebView 文件上传、作品封面编辑均接入 `AppImagePicker`；业务 feature 中不再直接调用 `Matisse.from` / `Matisse.obtain` 或直接注册 Photo Picker。未跑 Gradle，需后续构建验证。

## 本轮 Photo Picker 多选覆盖语义

### 变更了什么
Android 13+ Photo Picker 多选结果 → 不再与页面已有 `Uri` 合并，直接以本次新选择结果覆盖当前列表；Android 12 及以下 Matisse 仍可通过 `selectedUris` 做旧 UI 预选回显。

### 为什么
系统 Photo Picker 不支持把已选 `Uri` 传入选择器做原生回显，强行在回调后合并旧数据会造成用户以为只选了新增图片、实际列表却保留旧图片的认知差异。覆盖语义更符合系统选择器行为。

### 验证结论
STATIC：`AppImagePicker` 的 Photo Picker 和 Matisse 回调都统一返回本次结果，`selectedUris` 仅用于 legacy Matisse `setSelectedUris`。
