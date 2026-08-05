# 项目接口索引

仅在 `SKILL.md` 已命中且附近同类实现不足时读取。本文件提供搜索起点，不代表目标分支一定保留同名业务接口。

## 固定入口

| 要确认的能力 | 优先搜索 |
| --- | --- |
| 路径和 Retrofit 声明 | `ApiConstants`、`ApiService` |
| 请求/响应封装 | `ApiResponse`、`Response.kt`、`ViewModelExt.kt` |
| Repository 基类与注入 | `AppRepository`、`CommonRepository`、`mRepo.api` |
| 模型目录 | `core/network/apis/request`、`response`、`entity` |
| 分页协议 | `PagedBody`、`PagedList`、`PagingSource` |
| SSE 声明 | `SSEvent`、`text/event-stream` |

## 定位命令

```powershell
rg -n "接口路径|业务关键词|方法名|常量名" app/src/main/java -g "!**/OssManager.kt"
rg -n "fun (request|requestWithLoading|response)|class ApiResponse|class AppRepository" app/src/main/java
rg -n "@(GET|POST|PUT|DELETE|PATCH)|ApiResponse<" app/src/main/java
```

只读取目标接口和一个最近的完整同类链路；不要为了找示例把整个网络层或所有业务页面加入上下文。
