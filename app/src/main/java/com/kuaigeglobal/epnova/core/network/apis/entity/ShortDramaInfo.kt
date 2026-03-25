package com.kuaigeglobal.epnova.core.network.apis.entity

import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.kuaigeglobal.epnova.core.User
import com.kuaigeglobal.epnova.core.db.convert.RoleListTypeConverter
import com.kuaigeglobal.epnova.core.db.convert.SegmentListTypeConverter
import com.kuaigeglobal.epnova.feature.download.DownloadRatioConfig
import kotlinx.parcelize.Parcelize

/**
 * 剧本信息
 */
@Entity("ShortDramaInfo")
@TypeConverters(value = [SegmentListTypeConverter::class, RoleListTypeConverter::class])
@Parcelize
data class ShortDramaInfo(
    /**
     * 短剧id，接受Long和String类型
     */
    @PrimaryKey
    val dramasId: String,
    /**
     * 短剧名称
     */
    var dramasName: String?,
    /**
     * 剧本内容
     */
    val scriptContent: String?,
    /**
     * 剧本简介
     */
    var scriptIntro: String?,
    /**
     * 短剧封面URL
     */
    val dramasCoverUrl: String?,
    /**
     * 短剧风格id
     */
    val styleId: String?,
    /**
     * 短剧风格名称
     */
    val styleName: String?,
    /**
     * 短剧音色id
     */
    val timbreId: String?,
    /**
     * [枚举]视频比例 [1=9:16 2=16:9 3=3:4 4=4:3]
     */
    val videoRatio: Int?,
    /**
     * [枚举]作品状态 [0=预生成中(剧本大纲和分镜在生成中) 1=画面生成中 2=画面全部生成失败 3=画面部分生成失败 4=待选择角色 5=画面生成成功(未合成短剧) 6=已合成短剧 7=预生成成功(剧本大纲和分镜在生成成功)]
     */
    val workStatus: Int?,
    /**
     * 是否为首次生成
     */
    val firstGenStatus: Int?,
    /**
     * 预估时长(秒)
     */
    val estimatedDuration: Int?,
    /**
     * 实际时长(秒)
     */
    val actualDuration: Int?,
    /**
     * 首次生成付费状态 0=未付费 1=已付费
     */
    val firstGenPayStatus: Int?,
    /**
     * 创建时间
     */
    val createTime: Long?,
    /**
     * 字幕音频生成状态 [0=待生成 1=生成中 2=生成成功 3=生成失败]
     */
    val subtitleAudioGenStatus: Int?,
    /**
     * 发布状态 0-未发布 1-已发布
     */
    val publishStatus: Int?,
    /**
     * 作品编号
     */
    val workNo: String?,
    /**
     * 短剧剧集信息(只有已发布才有值)
     */
    @Embedded(prefix = "dramasSeries_") // 嵌套对象的前缀
    val dramasSeries: DramasSeriesInfo?,
    /**
     * 分镜片段集合
     */
    val shotSegmentList: List<ShortDramaSegmentInfo>?,
    /**
     * 短剧角色集合
     */
    val roleList: List<ShortDramaRoleInfo>?,
    /**
     * 短剧视频
     */
    @Embedded(prefix = "dramasVideo_") // 嵌套对象的前缀
    var dramasVideo: ShortDramaVideoInfo?,

    @Embedded(prefix = "artist_") // 嵌套对象的前缀
    val artist: Artist?,
    @Embedded(prefix = "unlockInfo_") // 嵌套对象的前缀
    val unlockInfo: UnlockInfo?,
    @Embedded(prefix = "interaction")
    val interaction: Interaction? = null,
) : Parcelable {

    companion object {
        //[枚举]作品状态 [0=预生成中(剧本大纲和分镜在生成中) 1=画面生成中 2=画面全部生成失败 3=画面部分生成失败 4=待选择角色 5=画面生成成功(未合成短剧) 6=已合成短剧 7=预生成成功(剧本大纲和分镜在生成成功)]
        val STATUS_WAITING = 0
        val STATUS_GENERATING = 1
        val STATUS_GENERATED_FAILED = 2
        val STATUS_GENERATED_PART_FAILED = 3
        val STATUS_WAIT_FOR_CHARACTER_CREATION = 4
        val STATUS_PENDING_VIDEO_GENERATION = 5
        val STATUS_GENERATED_SUCCESS = 6

        val STATUS_PRE_GENERATED_SUCCESS = 7
    }

    fun isPrepareForEditor(): Boolean {
        return workStatus == 1 || workStatus == 2 || workStatus == 3 || workStatus == 5
    }

    val videoRatioValue: String
        get() = when (videoRatio) {
            1 -> "9:16"
            2 -> "16:9"
            3 -> "3:4"
            4 -> "4:3"
            else -> "9:16"
        }

    val downloadRatio: DownloadRatioConfig
        get() = when (videoRatio) {
            1 -> DownloadRatioConfig.RATIO_9_16
            2 -> DownloadRatioConfig.RATIO_16_9
            3 -> DownloadRatioConfig.RATIO_3_4
            4 -> DownloadRatioConfig.RATIO_4_3
            else -> DownloadRatioConfig.RATIO_9_16
        }

    val generated get() = workStatus == 5 || workStatus == 6

    val subtitleAudioGenerating get() = subtitleAudioGenStatus == 0 || subtitleAudioGenStatus == 1

    val isMine get() = User.get().id == artist?.userToken

    fun getDurationMs(): Long {
        return (actualDuration ?: estimatedDuration)?.times(1000L) ?: 0L
    }

    fun getMediaUrls(): List<String>? {
        return shotSegmentList?.mapNotNull { it.mediaUrls() }?.flatten()
    }

    fun isLocked(): Boolean {
        return unlockInfo?.isLocked() == true
    }

    /**
     * 首次创作应该显示锁定状态
     */
    fun firstGenLocked() = firstGenStatus == 1 && firstGenPayStatus != 1

    fun equalsWithMedias(other: ShortDramaInfo?): Boolean {
        if (this != other) return false
        if (dramasId != other.dramasId) return false
        if (getMediaUrls() != other.getMediaUrls()) return false

        return true

    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ShortDramaInfo

        if (workStatus != other.workStatus) return false
        if (firstGenPayStatus != other.firstGenPayStatus) return false
        if (createTime != other.createTime) return false
        if (subtitleAudioGenStatus != other.subtitleAudioGenStatus) return false
        if (publishStatus != other.publishStatus) return false
        if (dramasId != other.dramasId) return false
        if (dramasName != other.dramasName) return false
        if (scriptContent != other.scriptContent) return false
        if (scriptIntro != other.scriptIntro) return false
        if (dramasCoverUrl != other.dramasCoverUrl) return false
        if (timbreId != other.timbreId) return false
        if (unlockInfo != other.unlockInfo) return false
        if (interaction != other.interaction) return false

        return true
    }

    override fun hashCode(): Int {
        var result = workStatus ?: 0
        result = 31 * result + (firstGenPayStatus ?: 0)
        result = 31 * result + (createTime?.hashCode() ?: 0)
        result = 31 * result + (subtitleAudioGenStatus ?: 0)
        result = 31 * result + (publishStatus ?: 0)
        result = 31 * result + dramasId.hashCode()
        result = 31 * result + (dramasName?.hashCode() ?: 0)
        result = 31 * result + (scriptContent?.hashCode() ?: 0)
        result = 31 * result + (scriptIntro?.hashCode() ?: 0)
        result = 31 * result + (dramasCoverUrl?.hashCode() ?: 0)
        result = 31 * result + (timbreId?.hashCode() ?: 0)
        result = 31 * result + (unlockInfo?.hashCode() ?: 0)
        result = 31 * result + (interaction?.hashCode() ?: 0)
        return result
    }


}
