@file:JvmName("InstanceExt")

package com.kuaigeglobal.epnova.common.ext

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.plus
import okhttp3.OkHttpClient

val ioScope: CoroutineScope by lazy { CoroutineScope(Dispatchers.IO) + SupervisorJob() }

val client by lazy { OkHttpClient() }