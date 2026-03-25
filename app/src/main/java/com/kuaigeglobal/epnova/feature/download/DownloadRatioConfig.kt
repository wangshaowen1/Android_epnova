package com.kuaigeglobal.epnova.feature.download

/**
 * 下载视频比例配置
 * @param vw 视频宽度
 * @param vh 视频高度
 * @param subtitleLineMaxLength 每行字幕最大长度
 */
enum class DownloadRatioConfig(val vw: Int, val vh: Int, val subtitleTextSize: Int, val subtitleLineMaxLength: Int, val subtitleMarginBottom: Int) {
    RATIO_16_9(1280, 720, 40, 30, 80),
    RATIO_9_16(720, 1280, 40, 15, 120),
    RATIO_4_3(960, 720, 40, 30, 60),
    RATIO_3_4(720, 960, 40, 15, 45);

    override fun toString(): String {
        return "DownloadRatioConfig(vw=$vw, vh=$vh, subtitleTextSize=$subtitleTextSize, subtitleLineMaxLength=$subtitleLineMaxLength, subtitleMarginBottom=$subtitleMarginBottom)"
    }
}