package com.kuaigeglobal.epnova.feature.editor

object VideoSynthesisManager {
    private const val runningTaskSize = 2

    /**
     * 缓存key，修改关联Parcelable类，需要修改版本
     */
    private const val CACHE_KEY = "VideoSynthesisManager.taskList.v1"
    private val taskList by lazy { arrayListOf<VideoSynthesisTask>() }

    fun synthesis(task: VideoSynthesisTask) {
        val exist = taskList.firstOrNull { it.workId == task.workId && it.storyId == task.storyId }
        if (exist != null) {
            exist.cancel()
            taskList.remove(exist)
        }
        taskList.add(task)
        synthesisNext()
    }

    fun synthesisNext() {
        val synthesisingCount = taskList.count { it.isSynthesising() }
        if (synthesisingCount < runningTaskSize) {
            taskList.firstOrNull { it.status == VideoSynthesisTask.STATUS_INIT }?.start()
        }
    }

    fun delete(task: VideoSynthesisTask) {
        taskList.remove(task)
        saveCache()
    }

    fun saveCache() {
//        CoroutineScope(Dispatchers.IO).launchNoCrash(block = {
//            CacheDiskUtils.getInstance().put(CACHE_KEY, VideoSynthesisTaskList(taskList.filterNot { it.status == VideoSynthesisTask.STATUS_SUCCESS }.toArrayList()))
//        }, onError = {
//            it.printStackTrace()
//        })
    }

}