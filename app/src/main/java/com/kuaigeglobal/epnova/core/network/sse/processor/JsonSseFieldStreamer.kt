package com.kuaigeglobal.epnova.core.network.sse.processor

class JsonSseFieldStreamer(
    private val onTitleUpdate: ((String) -> Unit)? = null,
    private val onMdUpdate: ((String) -> Unit)? = null,
    private val onStoryUpdate: ((String) -> Unit)? = null,
    private val MD_THROTTLE_MS: Long = 80L
) {

    private enum class State {
        SEEK_KEY,
        IN_KEY,
        SEEK_COLON,
        SEEK_VALUE,
        IN_VALUE,
        POTENTIAL_END, // 核心容错状态：疑似结束
        SKIP_VALUE
    }

    private enum class Target { NONE, TITLE, EPISODE_TITLE, MD, STORY }

    private var state = State.SEEK_KEY
    private var target = Target.NONE

    private val keyBuf = StringBuilder(32)
    private val valueBuf = StringBuilder(1024)
    // 暂存疑似结束后的空白字符，防止误判时丢失空格
    private val pendingWs = StringBuilder(16)

    private var escape = false
    private var braceLevel = 0

    private var lastMdEmittedLength = 0
    private var lastMdEmitTime = 0L

    fun onChunk(chunk: String) {
        for (c in chunk) {
            feed(c)
        }
    }

    private fun feed(c: Char) {
        // 维护 JSON 层级 (排除值内部字符，防止误判)
        if (state != State.IN_VALUE && state != State.POTENTIAL_END) {
            when (c) {
                '{' -> braceLevel++
                '}' -> braceLevel--
            }
        }

        when (state) {
            State.SEEK_KEY -> {
                if (c == '"' && braceLevel == 1) { // 仅顶层 Key
                    keyBuf.setLength(0)
                    state = State.IN_KEY
                }
            }

            State.IN_KEY -> {
                when (c) {
                    '"' -> state = State.SEEK_COLON
                    '\\' -> {} // 忽略 Key 中的转义符
                    else -> keyBuf.append(c)
                }
            }

            State.SEEK_COLON -> {
                if (c == ':') state = State.SEEK_VALUE
            }

            State.SEEK_VALUE -> {
                if (c == '"') {
                    target = when (keyBuf.toString()) {
                        "title" -> Target.TITLE
                        "episode_title" -> Target.EPISODE_TITLE
                        "md" -> Target.MD
                        "story_content" -> Target.STORY
                        else -> Target.NONE
                    }
                    valueBuf.setLength(0)
                    lastMdEmittedLength = 0
                    escape = false
                    state = State.IN_VALUE
                } else if (!c.isWhitespace()) {
                    target = Target.NONE
                    state = State.SKIP_VALUE
                }
            }

            State.IN_VALUE -> {
                if (escape) {
                    // 简易转义：物理换行符保留原意，其他保留转义后字符
                    val unescaped = when (c) {
                        'n' -> '\n'
                        'r' -> '\r'
                        't' -> '\t'
                        '\"' -> '\"'
                        '\\' -> '\\'
                        '/' -> '/'
                        else -> c
                    }
                    valueBuf.append(unescaped)
                    escape = false
                    emit(false)
                } else {
                    when (c) {
                        '\\' -> escape = true
                        // [核心容错 1] 遇到引号，进入“疑似结束”状态观察
                        '"' -> state = State.POTENTIAL_END
                        // [核心容错 2] 兼容大模型输出的物理换行
                        '\n', '\r' -> { valueBuf.append('\n'); emit(false) }
                        else -> { valueBuf.append(c); emit(false) }
                    }
                }
            }

            // [核心容错逻辑] 延迟判决机制
            State.POTENTIAL_END -> {
                when {
                    // Case A: 真结束 (后跟逗号或右括号)
                    c == ',' || c == '}' -> {
                        // 确认结束，缓冲区里的空白字符确认为结构性空白（丢弃）
                        pendingWs.setLength(0)
                        emit(true)
                        target = Target.NONE

                        if (c == ',') state = State.SEEK_KEY
                        else {
                            braceLevel--
                            state = State.SEEK_KEY
                        }
                    }

                    // Case B: 空白 (继续观察，存入暂存区)
                    c.isWhitespace() -> {
                        // 归一化换行符
                        pendingWs.append(if (c == '\r') '\n' else c)
                    }

                    // Case C: 假结束 (是内容的一部分，如 "说"快跑")
                    else -> {
                        // 1. 补回刚才扣留的引号 (它其实是内容)
                        valueBuf.append('"')

                        // 2. 补回刚才暂存的空白字符 (如 "Hello" to me)
                        if (pendingWs.isNotEmpty()) {
                            valueBuf.append(pendingWs)
                            pendingWs.setLength(0)
                        }

                        emit(false)

                        // 3. 处理当前字符
                        if (c == '"') {
                            // [关键递归修正] 如果当前字符又是引号 (如 "Hi"")
                            // 说明刚才那个引号是内容，但当前这个引号可能是新的结束符
                            // 保持在 POTENTIAL_END 状态，暂不追加当前引号到 buffer
                        } else {
                            // 普通字符，追加并切回 IN_VALUE
                            valueBuf.append(c)
                            emit(false)
                            state = State.IN_VALUE
                        }
                    }
                }
            }

            State.SKIP_VALUE -> {
                // 保持原版兼容逻辑
                if (c == ',' || c == '}') state = State.SEEK_KEY
            }
        }
    }

    private fun emit(isFinal: Boolean) {
        if (target == Target.NONE) return

        when (target) {
            Target.TITLE -> {
                // Title 保持原样：只在结束时发送一次
                if (isFinal) {
                    onTitleUpdate?.invoke(valueBuf.toString())
                }
            }

            Target.EPISODE_TITLE -> {
                // Title 保持原样：只在结束时发送一次
                if (isFinal) {
                    onTitleUpdate?.invoke(valueBuf.toString())
                }
            }

            Target.MD -> {
                if (!isFinal && valueBuf.isNotEmpty() && valueBuf.last() == '\\') {
                    return
                }

                val now = System.currentTimeMillis()
                // MD 保持原样：节流 + 增量 (Delta)
                if ((isFinal || now - lastMdEmitTime >= MD_THROTTLE_MS) && valueBuf.length > lastMdEmittedLength) {
                    var delta = valueBuf.substring(lastMdEmittedLength)

                    delta = delta.replace("\\n", "\n")
                        .replace("\\r", "")

                    onMdUpdate?.invoke(delta)
                    lastMdEmittedLength = valueBuf.length
                    lastMdEmitTime = now
                }
            }

            Target.STORY -> {
                val now = System.currentTimeMillis()
                // Story 保持原样：节流 + 全量 (Full)
                if (isFinal || now - lastMdEmitTime >= MD_THROTTLE_MS) {
                    val fullText = valueBuf.toString().replace("\\n", "\n").replace("\\r", "")
                    onStoryUpdate?.invoke(fullText)
                    lastMdEmitTime = now
                }
            }

            else -> Unit
        }

        if (isFinal) {
            valueBuf.setLength(0)
        }
    }

    /**
     * 当流被中断或结束时调用此方法。
     * 它可以强制输出缓冲区中剩余的内容，解决 JSON 缺少闭合括号导致数据丢失的问题。
     */
    fun finish() {
        // 如果流结束时还处于“正在读取值”的状态，说明 JSON 被截断了
        // 此时我们应该尽可能把缓冲区里的东西吐出来
        if (state == State.IN_VALUE || state == State.POTENTIAL_END) {

            // 如果是在 POTENTIAL_END 状态，说明最后一个字符是引号
            // 在截断的情况下，我们通常认为这个引号就是结束引号（或者内容的一部分），
            // 但为了保险起见，我们把暂存的空白字符也补回来
            if (state == State.POTENTIAL_END) {
                if (pendingWs.isNotEmpty()) {
                    valueBuf.append(pendingWs)
                    pendingWs.setLength(0)
                }
                // 注意：POTENTIAL_END 状态下，那个触发进入该状态的引号已经在之前被吞掉了
                // 如果是正常结束，那个引号是界定符；如果是异常截断，通常也不需要补回那个引号，
                // 除非你认为 AI 刚好输出了一个作为内容的引号然后挂了。
                // 简单处理：直接视为字段结束。
            }

            // 强制触发一次 Final Emit
            emit(isFinal = true)
        }

        // 重置状态
        reset()
    }

    fun reset() {
        state = State.SEEK_KEY
        target = Target.NONE
        keyBuf.setLength(0)
        valueBuf.setLength(0)
        pendingWs.setLength(0)
        escape = false
        braceLevel = 0
        lastMdEmitTime = 0L
        lastMdEmittedLength = 0
    }
}