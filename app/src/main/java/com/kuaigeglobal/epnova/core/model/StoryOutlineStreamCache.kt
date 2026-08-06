package com.kuaigeglobal.epnova.core.model

/**
 * 故事梗概 SSE 未完成时的本地续传缓存，按 workId 存在配置 SP。
 *
 * 只缓存故事梗概是因为梗概页面退出后需要恢复 markdown 流式展示；final 成功后会立即清理。
 * key 使用 workId 维度，避免同一用户多个作品同时生成时互相覆盖。
 */
data class StoryOutlineStreamCache(
    // 最近一次收到的非空 SSE 原生 id；重新进入页面后先带给 session，再由 session 决定是否可回放。
    val lastEventId: String? = null,
    // 后端 chunk 的完整原文，用于 markdownCutKey 尚未命中时继续在隐藏区累积。
    val rawText: String? = null,
    // 当前已经允许展示给用户的文本；有 markdownCutKey 时可能只是 rawText 的后半段。
    val displayText: String? = null,
    // session 下发的展示起点。为空时不截断，直接展示 rawText。
    val markdownCutKey: String? = null,
    // 是否已经命中过 markdownCutKey；恢复后继续追加时不能重新从 rawText 开头展示。
    val markdownCutMatched: Boolean = false
)
