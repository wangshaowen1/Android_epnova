# 项目导航与事件索引

仅在导航 Skill 已命中且目标附近无法确认真实入口或协议时读取。

| 要确认的能力 | 搜索入口 |
| --- | --- |
| App 内路由 | `AppRouter`、`AppNavigator`、`navigateTo` |
| scheme/deep link | Manifest intent-filter、scheme/host 分发 |
| 页面参数 | `start(`、`newInstance(`、`mBundle`、`Constants.Extra` |
| 登录拦截 | 当前登录状态、登录页结果与 pending action |
| 全局事件 | `Events`、`FlowEventBus`、`observeEvents` |
| 主 Tab 切换 | 当前 Main 容器和已有 tab 事件，不假设固定常量 |

```powershell
rg -n "AppRouter|AppNavigator|navigateTo|jumpUri|intent-filter|scheme|host" app/src/main
rg -n "FlowEventBus|observeEvents|sealed class Events|object Events" app/src/main/java
rg -n "fun (start|newInstance)\(|mBundle|Constants\.Extra" app/src/main/java
```

协议值、主 Tab 编号和登录行为必须来自当前分支；不要从来源项目或示例名称推断。
