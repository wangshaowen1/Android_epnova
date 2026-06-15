# 代码接口目录维护说明

本文件不保存从其他项目复制来的固定代码目录。需要代码接口目录时，应基于当前分支重新扫描生成，避免海外版和中国大陆版之间的包名、SDK、语言目录、支付/登录/推送能力差异误导后续代码生成。

## 扫描原则

- 主业务包路径以当前分支 `app/build.gradle` 的 `namespace`、`AndroidManifest.xml` 的 Application 类和 `app/src/main/java` 实际目录为准。
- 本地模块以 `settings.gradle` 为准。
- SDK 能力以当前分支依赖、Manifest、初始化入口和 `core/manager/` 或已有平台包为准。
- 多语言目录以当前分支 `app/src/main/res/values*/strings.xml` 为准。
- 不查看或编辑 `**/OssManager.kt`，除非用户明确要求。

## 快照使用和刷新规则

- `code-interface-scan-summary.md` 是从旧本地资料迁入的历史快照，只能作为导航索引，不作为当前分支最终事实。
- 触达 API、SDK、Room、主包路径、语言目录、Manifest、Gradle 依赖或路由入口前，必须用 `rg` 或直接读取当前文件重新确认。
- 如果后续存在快照且生成时间距离当前日期超过 14 天，或当前分支已有较大目录/API/SDK 调整，只能使用快照中的目录线索，不能直接引用其中的接口数量、字段语义或方法清单作为结论。
- 发现快照与当前代码不一致时，以当前代码为准；如差异影响后续任务，应在输出中说明快照已过期或局部不适用。
- 需要重扫时，按本文推荐命令重新扫描；如要落盘摘要，先确认仓库已有维护入口或用户明确要求新增文档。

## 推荐扫描命令

```bash
rg --files app/src/main/java
rg -n "class .*ViewModel|class .*Repository|class .*Adapter|class .*Dialog" app/src/main/java -g "!**/OssManager.kt"
rg -n "suspend fun|@GET|@POST|@Body|ApiResponse<" app/src/main/java -g "!**/OssManager.kt"
rg -n "fun start\\(|fun getIntent\\(|fun newInstance\\(|AppRouter|FlowEventBus|Events\\." app/src/main/java -g "!**/OssManager.kt"
rg -n "Billing|Pay|Payment|Wechat|Alipay|Douyin|Firebase|Google|Facebook|Umeng|Bugly|OAID|Push|AppsFlyer|Adjust|Rangers|GrowingIO|OpenInstall" app/build.gradle app/src/main app/src/main/java -g "!**/OssManager.kt"
rg --files app/src/main/res | rg "values.*[/\\\\]strings\\.xml$"
```

当前仓库未维护 `.agents/scripts/scan_code_interface.ps1`，不要按复制来源项目的脚本路径执行。

## 输出建议

重新生成目录时，建议只记录当前分支真实存在的内容：

- 主业务包路径和关键模块。
- Application、网络、数据库、路由、基础 ViewModel、BindingAdapter。
- 页面功能域及代表性 Activity/Fragment/ViewModel/Repository。
- 列表、分页、弹窗、异步任务、下载、SSE、支付/登录/推送/统计等平台能力入口。
- 当前已维护的语言资源目录。
- 需要人工确认的 SDK 差异、合规初始化或历史命名问题。
