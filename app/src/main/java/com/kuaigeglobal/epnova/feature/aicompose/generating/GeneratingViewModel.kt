package com.kuaigeglobal.epnova.feature.aicompose.generating

import android.os.Handler
import android.os.Looper
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.MutableLiveData
import com.kuaigeglobal.epnova.common.SP
import com.kuaigeglobal.epnova.core.User
import com.kuaigeglobal.epnova.core.network.apis.ApiFactory
import com.kuaigeglobal.epnova.core.network.apis.request.UserTokenBody
import com.kuaigeglobal.epnova.core.network.apis.response.PictureBook
import com.nhsd.basic.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 首页-绘本生成中浮层控制
 */
object GeneratingViewModel {

    var visible: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                if (value) {
                    loop()
                } else {
                    handler.removeCallbacks(runnable)
                    show.set(false)
                }
            }
        }
    val show by lazy { ObservableBoolean(false) }


    val expand by lazy { ObservableBoolean(false) }
    val firstItem by lazy { ObservableField<ItemGeneratingVM>() }
    val generatingItems by lazy { MutableLiveData<Map<String?, PictureBook?>>() }
    val size by lazy { ObservableInt(0) }
    var generating = true
        set(value) {
            if (field != value) {
                field = value
                if (!value) {
                    Logger.i("sender________set false. removeCallbacks")

                    handler.removeCallbacks(runnable)
                    show.set(false)
                } else if (visible) {
                    //loop()
                }
            }
        }

    private val handler by lazy { Handler(Looper.getMainLooper()) }
    private val runnable by lazy { Runnable { loop() } }

    /**
     * 本地生成中假状态，与生成中弹窗状态相同
     * @param bookId 歌曲ID
     * @return 0 正在为你解析歌词 1 正在为你编曲 2 试听版即将生成
     */
    fun getGenerateStatus(bookId: String?) =
        SP.getDefaultSP().getInt("generateStatus_$bookId", -1)

    /**
     * 本地生成中假状态，与生成中弹窗状态相同
     * @param bookId 歌曲ID
     * @param status 0 正在为你解析歌词 1 正在为你编曲 2 试听版即将生成
     */
    fun setGenerateStatus(bookId: String?, status: Int) {
        SP.getDefaultSP().putInt("generateStatus_$bookId", status)
        if (bookId.generateTime <= 0) {
            bookId.generateTime = System.currentTimeMillis()
        }
    }

    var String?.generateTime: Long
        private set(value) {
            SP.getDefaultSP().putLong("generateTime_$this", value)
        }
        get() {
            val now = System.currentTimeMillis()
            return SP.getDefaultSP().getLong("generateTime_$this", -1).let {
                if (it <= 0) {
                    SP.getDefaultSP().putLong("generateTime_$this", now)
                    now
                } else {
                    it
                }
            }
        }

    fun collapse() {
        expand.set(false)
    }

    fun expand() {
        expand.set(true)
    }

    private fun loop() {
        if (generating) {
            if (User.get().isVisitor) {
                generating = false
                return
            }
            MainScope().launch {
                runCatching {
                    val response = withContext(Dispatchers.IO) {
                        ApiFactory.apiService.generatingBookList(UserTokenBody())
                    }
                    if (response.isSuccess) {
                        val items = response.data?.items
                        generatingItems.value = items?.associate { it.pictureBookId to it }
                        val first = items?.firstOrNull()
                        if (first == null) {
                            generating = false
                            firstItem.set(null)
                        } else {
                            show.set(true)
                            size.set(items.size)
                            if (firstItem.get() != null) {
                                firstItem.get()?.update(first)
                            } else {
                                firstItem.set(ItemGeneratingVM(first))
                            }
                        }
                    }
                }
                if (generating) {
                    handler.postDelayed(runnable, 4000)
                }
            }
        }
    }
}
