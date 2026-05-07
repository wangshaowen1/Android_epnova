@file:JvmName("DataUtils")

package com.kuaigeglobal.epnova.core.ext

import android.app.Activity
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.EncryptUtils
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.OssHelp
import com.kuaigeglobal.epnova.common.ext.showConfirmPrompt
import com.kuaigeglobal.epnova.common.utils.SensitiveHighlighter
import com.kuaigeglobal.epnova.common.utils.WXUtils
import com.kuaigeglobal.epnova.core.Config
import com.kuaigeglobal.epnova.core.Global
import com.kuaigeglobal.epnova.core.User
import com.kuaigeglobal.epnova.core.manager.AppEvent
import com.kuaigeglobal.epnova.core.manager.EventManager
import com.kuaigeglobal.epnova.core.model.PhotoItem
import com.kuaigeglobal.epnova.core.network.apis.ApiFactory
import com.kuaigeglobal.epnova.core.network.apis.entity.SimpleStoryboardInfo
import com.kuaigeglobal.epnova.core.network.apis.request.ContentBody
import com.kuaigeglobal.epnova.core.network.apis.response.UserHomeData
import com.kuaigeglobal.epnova.feature.recharge.PowerRechargeDialog
import com.kuaigeglobal.epnova.feature.splash.newer.FirstRechargeDialog
import com.kuaigeglobal.epnova.feature.web.SimpleBrowserActivity
import com.nhsd.basic.ext.getResColor
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.showToast
import com.nhsd.basic.ext.showToastDelay
import com.nhsd.basic.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jcodec.containers.mp4.boxes.MetaValue
import org.jcodec.movtool.MetadataEditor
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder


fun UserHomeData?.loginToast() {
    val newRegister = this?.isNewRegistration() == true
    Global.needCompleteProfile = newRegister
    if (!newRegister) {
    }
    //R.string.login_success_start_creating.getResString().showToastDelay(100)
}

/**
 *
 * @param trackId String
 * @param commentsId String? 评论id  如果是通过消息列表跳转的就会传这个值
 * @param repliesId String?  回复id  如果是通过消息列表跳转的就会传这个值
 * @param downloadAccompany Boolean 是否下载伴奏
 */
fun startMusic(
    trackId: String?,

    ) {

}

/**
 * 上报异常至bugly
 */
fun Throwable.report() {
    //CrashReport.postCatchedException(this)
    printStackTrace()
}


fun Activity.openTheCustomerServiceCenter() {
    Config.get().appConfig.customerServiceUrl?.let { customerCenterH5Url ->
        SimpleBrowserActivity.launch(
            this@openTheCustomerServiceCenter,
            customerCenterH5Url,
        )
    }
}

//fun ShortDramaInfo?.shortDramaVideoShareContent(): ShareContent {
//    val title = "快来看看我的AI短剧${this?.dramasName ?: ""}"
//    val content =
//        "我用AI做了个短剧${this?.dramasName ?: ""}，还挺有意思的，快来看看！AI一键生成短剧太厉害了#快出片"
//    return ShareContent(title, content)
//}

/**
 * 打开微信并到客服聊天
 */
fun Activity.openTheCustomerServiceChat() {
    Config.get().appConfig.customerServiceInfo?.apply {
        showConfirmPrompt("确认跳转到微信打开客服？", confirm = {
            customerServiceTransfer()
        })
    }
}

fun Activity.customerServiceTransfer() {
    Config.get().appConfig.customerServiceInfo?.apply {
        WXUtils.customerServiceChat(
            this@customerServiceTransfer,
            customerServiceJmpUrl,
            enterpriseId
        )
    }
}


suspend fun File.putAIGCMetadata(shortDrama: SimpleStoryboardInfo): File {
    withContext(Dispatchers.IO) {
        runCatching {
            val contentProducer = AppUtils.getAppName().let { URLEncoder.encode(it) }
            val produceID = shortDrama.storyboardId
            val reservedCode1 = EncryptUtils.encryptMD5ToString("${contentProducer}${produceID}")
            val contentPropagator = User.get().nickName?.let { URLEncoder.encode(it) }
            val propagateID = User.get().userUid
            val reservedCode2 = EncryptUtils.encryptMD5ToString("${contentPropagator}${propagateID}")
            val aigc = JSONObject()
                .put("Label", "3")
                .put("ContentProducer", contentProducer)
                .put("ProduceID", produceID)
                .put("ReservedCode1", reservedCode1)
                .put("ContentPropagator", contentPropagator)
                .put("PropagateID", propagateID)
                .put("ReservedCode2", reservedCode2)
                .toString()
            val metadataEditor = MetadataEditor.createFrom(this@putAIGCMetadata)
            metadataEditor.keyedMeta?.put("AIGC", MetaValue.createString(aigc))
            metadataEditor.save(true)
        }.onFailure {
            Logger.e("putAIGCMetadata", it.message)
            it.printStackTrace()
        }
    }
    return this
}


fun toPhotoItem(list: MutableList<String?>?): ArrayList<PhotoItem?>? {
    if (list == null || list.size <= 0) {
        return null
    }
    val photoItems: ArrayList<PhotoItem?> = ArrayList<PhotoItem?>(list.size)
    if (list.size == 1) {
        val str = list.get(0)
        val thumbnail = OssHelp.getOssHighQualityThumbnail(str)
        photoItems.add(PhotoItem(thumbnail, str))
    } else {
        for (str in list) {
            val thumbnail = OssHelp.getOssThumbnail(str)
            photoItems.add(PhotoItem(thumbnail, str))
        }
    }
    return photoItems
}


suspend fun sensitiveWordCheck(content: String?, passBlock: () -> Unit, notPassBlock: (CharSequence?) -> Unit, needHighlight: Boolean? = true) {
    passBlock()
//    val pair = sensitiveWordCheck(content, needHighlight)
//    withContext(Dispatchers.Main) {
//        if (pair.first) {
//            notPassBlock(content)
//            passBlock()
//        } else {
//            notPassBlock(pair.second)
//        }
//    }
}

/**
 * 敏感词校验
 * @return 校验结果 Pair<是否通过校验, 高亮未通过文字>
 */
suspend fun sensitiveWordCheck(content: String?, needHighlight: Boolean? = true): Pair<Boolean, CharSequence?> {
    if (content.isNullOrBlank()) {
        return true to null
    }
    val checkRes = withContext(Dispatchers.IO) { ApiFactory.apiService.sensitiveWordCheck(ContentBody(content)) }
    val checkData = checkRes.data
    if (checkRes.isSuccess && checkData != null) {
        if (checkData.isSensitive()) {
            checkData.content?.showToast()
            val review = Config.get().isInReview
            if (needHighlight == false || review) {
                return false to content
            }
            val sp = SensitiveHighlighter.highlight(
                text = content,
                keywords = checkData.hitWords,
                color = R.color.func_failure_dark.getResColor()
            )
            return false to sp
        }
    } else {
        (checkRes.msg ?: R.string.sensitive_word_check_failed.getResString()).showToast()
        return false to null
    }
    return true to null
}

suspend fun showRechargeDialog(showToast: Boolean = false) {
        PowerRechargeDialog.show(0,showToast)
//    if (!showFirstRecharge(showToast)) {
//    }
}

/**
 * 显示首充弹窗
 * @return 是否显示成功
 */
suspend fun showFirstRecharge(showToast: Boolean): Boolean {
    val canShowFirstRecharge = !User.get().hasPaid() && !Config.get().isFirstRechargeShown(FirstRechargeDialog.SCENE_OTHER)
    if (!canShowFirstRecharge) return false
    val firstRechargeDialogData = withContext(Dispatchers.IO) {
        ApiFactory.apiService.getFirstRechargeDialogInfo()
    }.data
    if (firstRechargeDialogData == null) return false
    EventManager.onAppTrack(AppEvent.SF_VIEW_CREATE_NEWCOMER_GIFTPACK_PAGE)
    val dialog = FirstRechargeDialog.show(FirstRechargeDialog.SCENE_OTHER, firstRechargeDialogData)?.apply {
        onClose = {
            PowerRechargeDialog.show(0,showToast)
        }
    }
    Config.get().markFirstRechargeShown(FirstRechargeDialog.SCENE_OTHER)
    return dialog != null
}