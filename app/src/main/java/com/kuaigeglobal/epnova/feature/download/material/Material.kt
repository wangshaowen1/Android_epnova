package com.kuaigeglobal.epnova.feature.download.material

/**
 * 视频用到的所有素材管理（下载），包括封面、背景、原视频、音乐等
 * @param runningTaskSize 同时运行的下载任务数量
 */
abstract class Material(val runningTaskSize: Int) {
    abstract val items: List<SourceMaterialItem>
    private val preparingItems by lazy { arrayListOf<SourceMaterialItem>() }
    private val waitingItems: ArrayList<SourceMaterialItem> by lazy { ArrayList(items) }

    var listener: Listener? = null

    fun prepare(listener: Listener) {
        this.listener = listener
        prepareNext()
    }

    fun cancel() {
        waitingItems.clear()
        preparingItems.forEach { it.cancel() }
        preparingItems.clear()
    }

    private fun prepareNext() {
        if (preparingItems.size >= runningTaskSize) return
        if (preparingItems.isEmpty() && waitingItems.isEmpty()) {
            listener?.onSuccess()
            return
        }
        waitingItems.removeFirstOrNull()?.let {
            preparingItems.add(it)
            it.prepare(object : SourceMaterialItem.Listener {
                override fun onProgress(item: SourceMaterialItem, progress: Int) {
                    notifyProgress()
                }

                override fun onSuccess(item: SourceMaterialItem) {
                    preparingItems.remove(item)
                    prepareNext()
                }

                override fun onFail(item: SourceMaterialItem, e: Throwable) {
                    preparingItems.remove(item)
                    prepareNext()
                }
            })
            prepareNext()
        }
    }

    private fun notifyProgress() {
        if (items.isEmpty()) {
            listener?.onProgress(100)
        } else {
            listener?.onProgress(items.sumOf { it.progress } / items.size)
        }
    }

    interface Listener {
        fun onProgress(progress: Int)
        fun onSuccess()
        fun onFail(e: Throwable)
    }
}