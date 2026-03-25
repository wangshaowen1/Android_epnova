package com.kuaigeglobal.epnova.feature.download.transform

import java.io.File

interface TransformListener {
    fun onTransformStart()
    fun onTransformProgress(progress: Int)
    fun onTransformFail(reason: String)
    fun onTransformSuccess(file: File)
}