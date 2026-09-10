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

## 本轮 Google Play 强制更新迁移

### 变更了什么
生产版强制更新 → 从自定义不可取消弹窗及历史 APK 自安装链路，迁移为 Google Play Core `IMMEDIATE` In-app Update；普通建议更新继续使用现有可取消弹窗并跳转 Google Play。

### 为什么
Google Play 禁止通过 Play 分发的应用使用 Play 更新机制之外的方法更新自身；官方 `IMMEDIATE` 流程会先验证当前用户是否存在可用且允许的更新，并由 Play 完成下载、安装和重启。服务端标记强更但 Play 尚未覆盖当前用户时，降级为建议更新可避免灰度或兼容性差异造成不可恢复的锁死。

### 验证结论
STATIC：已确认强更入口使用 `AppUpdateManager`、前台恢复处理中断的 Immediate 流程，升级模块不再引用 APK 下载或安装逻辑；未跑 Gradle，需通过 Google Play 内部测试轨道验证更新可用、取消重试、安装重启和无可用更新降级。

## 本轮会员订阅进程恢复归属

### 变更了什么
会员订阅进程重启恢复 → 由 `MemberSubscriptionRecoveryManager` 作为 Application 生命周期的唯一后台编排者：首页启动及重新前台化时仅在当前账号存在恢复标记时查询 Google SUBS，严格匹配 productId 与 obfuscatedAccountId，PURCHASED 调幂等 `google/sync`，PENDING/未知结果保留标记；会员页继续负责持有 prepare 上下文的正常 confirm/release。

### 为什么
原页面级恢复只能在用户重新进入会员页后执行，进程重启停留在首页时无法主动收敛已成交交易；如果首页和会员页同时恢复，又可能重复查询和 sync。恢复任务需要跨 Activity 重建存活、限制单实例与有界重试，同时不能持久化 purchaseToken、prepare token 或完整 Purchase。

### 验证结论
STATIC：已核对恢复标记账号隔离、商品与混淆账号双重匹配、后台/页面互斥、Billing 使用计数成对释放、最多一次查询重试，以及成功/无有效交易/稳定业务错误与 PENDING/网络未知/300185 的清理边界；未跑 Gradle，仍需 License Tester 实机验证进程杀死后恢复、PENDING 转 PURCHASED 和重复前台化。

## 本轮分镜合成混合音频采样率

### 变更了什么
`VideoStoryboardResource` 在每段转码时统一音频为 AAC、128kbps、48kHz、双声道；`VideoSynthesisTransform` 复用相同编码参数，分镜缓存名增加版本 v2。

### 为什么
用户确认出现音画不同步、人声变速的素材混有 32kHz 与 44.1kHz 音轨。旧预处理仅统一视频规格，音轨采样率仍随源文件变化，不满足 concat demuxer 对各段流参数与时间基准一致的要求。必须在逐段处理时重采样；仅调整最终合成的输出采样率无法修复拼接输入阶段的参数错用。底层 Resource 会直接复用非空文件，因此修改转码规格必须同时更新分镜缓存版本。

### 验证结论
STATIC：已检查逐段转码先于 concat 执行、两阶段共用音频编码参数、新缓存名避开旧处理结果，`git diff --check` 通过；未跑 Gradle或媒体转码，仍需用实际 32kHz/44.1kHz 素材验证人声速度和分镜切点同步。

## 本轮 Google Play 一次性商品凭证兜底

### 变更了什么
金币与算力预下单后持久化账号、业务类型、orderNo 和 productId；Google 返回 PURCHASED 后先用 Android Keystore + AES-GCM 加密持久化 purchaseToken，再分别调用 `/trade/pay/google/coin/verifyReceipt` 与 `/trade/pay/google/payCallBack`。启动、回到前台及进入充值页都会触发同一应用级恢复任务。

### 为什么
页面级 orderId 会随进程退出丢失，旧链路还会把接口业务失败误计为已验单。服务端已保证成功 token 重复提交幂等，并负责 consume，因此客户端只负责保留并重试真实凭证，不调用 `consumeAsync` 或 `acknowledgePurchase`；只有服务端明确验单成功后才删除含 token 的本地记录。

### 验证结论
STATIC：已核对两类商品接口路由、回调落盘先于网络、账号与 orderNo/productId 交叉匹配、网络/业务失败保留凭证、生命周期恢复入口及无客户端 consume/ack 调用；`git diff --check` 通过。按项目规则未跑 Gradle，仍需使用 License Tester 验证杀进程恢复、接口超时重提、PENDING 转 PURCHASED 和服务端已发货 token 幂等响应。

## 2026-09-10 同步会话控件优化

### 变更与兼容取舍
以 Template 的 c239de3c 为来源，将 27 个既有控件、按压缩放辅助类及小数输入 BindingAdapter 优化同步到 EpNova。保留项目专属 BindingAdapter；GradientTextView 保留不透明文字效果，改用 ColorStateList.withAlpha 保留颜色状态，并删除不必要的 setText 延迟回调；AutoScrollRecyclerView 分离临时 detach 暂停与显式 stop，重新挂载可恢复已启动滚动。TransitionImageView 默认仍整组预加载，缓存窗口仅显式开启。StickyScrollView 的 onAttachedToWindow 保持 public，以兼容当前 AndroidX 父类可见性。

### 验证范围
28 个控件及辅助文件通过本地缓存依赖下的 Kotlin/Java 离线类型检查；33 个 Kotlin/Java 文件语法检查通过，109 个生产控件资源引用可解析，debug XML 与 diff 空白检查通过。同步 34 条仪器回归用例并补充 EpNova 颜色状态、滚动重新挂载 2 条用例，配置 AndroidJUnitRunner。未运行 Gradle、仪器测试或实机交互；上述结果仅说明静态检查未发现阻断问题。保留原有 gradle.properties 工作区修改。

## 2026-09-10 补齐 DragContainer 的遗漏同步

上一轮同步只覆盖 Template c239de3c，遗漏了用户此前已更新的 4fa9bcc6。现补齐 DragContainer 的 Kotlin 替换和属性定义，移除旧 Java 实现及六个无外部引用的专属类；DimenUtil 仍被 SpanExtTagHandler 使用，保留。新版要求提示 View + RecyclerView 两个直接子 View；已确认当前 EpNova 没有业务页面使用旧接口或旧布局。

源码与 Template/Povie 一致；离线 Kotlin 类型检查、语法、XML 契约与旧符号残留检查通过。新增四条仪器用例覆盖取消、触发保持、detach 复位、禁用监听器；未运行 Gradle或设备测试。跨项目同步应同时核对本会话各次控件提交，不能仅凭后一笔优化提交的文件清单声明全部对齐。
