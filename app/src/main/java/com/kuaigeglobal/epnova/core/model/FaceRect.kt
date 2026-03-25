package com.kuaigeglobal.epnova.core.model

/**
 * 人脸识别框位置信息
 */
class FaceRect(
    val boundingWithPercent: Float,
    val boundingRatio: String,
    val horizontalBias: Float,
    val verticalBias: Float,
)