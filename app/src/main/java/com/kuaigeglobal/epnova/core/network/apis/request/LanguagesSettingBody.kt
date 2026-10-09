package com.kuaigeglobal.epnova.core.network.apis.request

/** 
 * @createtime 2026年10月09日 11:04
 * @author TooWhiteT
 */

data class LanguagesSettingBody(
    /**
     * App 展示语言，支持 9 种语言短码及带地区的语言码；服务端保存归一化后的短码。
     *
     * zh 中文
     * th 泰语
     * en 英语
     * id 印尼语
     * fr 法语
     * de 德语
     * pt 葡萄牙语
     * es 西班牙语
     * ja 日语
     */
    val languageCode: String
)
