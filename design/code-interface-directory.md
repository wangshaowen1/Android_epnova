# 代码接口目录维护说明

本文件不保存从其他项目复制来的固定代码目录。需要代码接口目录时，应基于当前分支重新扫描生成，避免海外版和中国大陆版之间的包名、SDK、语言目录、支付/登录/推送能力差异误导后续代码生成。

## 扫描原则

- 主业务包路径以当前分支 `app/build.gradle` 的 `namespace`、`AndroidManifest.xml` 的 Application 类和 `app/src/main/java` 实际目录为准。
- 本地模块以 `settings.gradle` 为准。
- SDK 能力以当前分支依赖、Manifest、初始化入口和 `core/manager/` 或已有平台包为准。
- 多语言目录以当前分支 `app/src/main/res/values*/strings.xml` 为准。
- 不查看或编辑 `**/OssManager.kt`，除非用户明确要求。

## 推荐扫描命令

```bash
rg --files app/src/main/java
rg -n "class .*ViewModel|class .*Repository|class .*Adapter|class .*Dialog" app/src/main/java -g "!**/OssManager.kt"
rg -n "suspend fun|@GET|@POST|@Body|ApiResponse<" app/src/main/java -g "!**/OssManager.kt"
rg -n "fun start\\(|fun getIntent\\(|fun newInstance\\(|AppRouter|FlowEventBus|Events\\." app/src/main/java -g "!**/OssManager.kt"
rg -n "Billing|Pay|Payment|Wechat|Alipay|Douyin|Firebase|Google|Facebook|Umeng|Bugly|OAID|Push|AppsFlyer|Adjust|Rangers|GrowingIO" app/build.gradle app/src/main app/src/main/java -g "!**/OssManager.kt"
rg --files app/src/main/res | rg "values.*[/\\\\]strings\\.xml$"
```

## 输出建议

重新生成目录时，建议只记录当前分支真实存在的内容：

- 主业务包路径和关键模块。
- Application、网络、数据库、路由、基础 ViewModel、BindingAdapter。
- 页面功能域及代表性 Activity/Fragment/ViewModel/Repository。
- 列表、分页、弹窗、异步任务、下载、SSE、支付/登录/推送/统计等平台能力入口。
- 当前已维护的语言资源目录。
- 需要人工确认的 SDK 差异、合规初始化或历史命名问题。
