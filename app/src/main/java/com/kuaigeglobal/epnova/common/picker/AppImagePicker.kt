package com.kuaigeglobal.epnova.common.picker

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Build
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.common.ext.applyEpnovaMatisseUi
import com.kuaigeglobal.epnova.common.permissions.requestImages2VideosPermissions
import com.nhsd.basic.manager.FileManager
import com.nhsd.mattise.Matisse
import com.nhsd.mattise.MimeType
import com.nhsd.mattise.engine.impl.GlideEngine
import com.nhsd.mattise.internal.entity.CaptureStrategy

class AppImagePicker private constructor(
    private val activity: FragmentActivity?,
    private val fragment: Fragment?,
    private val requestCode: Int,
    private val maxSelectable: Int,
    private val capture: Boolean,
    private val useEmbeddedCamera: Boolean,
    private val onResult: (List<Uri>) -> Unit,
    private val onCancel: () -> Unit
) {
    constructor(
        activity: FragmentActivity,
        requestCode: Int,
        maxSelectable: Int = 1,
        capture: Boolean = true,
        useEmbeddedCamera: Boolean = false,
        onResult: (List<Uri>) -> Unit,
        onCancel: () -> Unit = {}
    ) : this(
        activity = activity,
        fragment = null,
        requestCode = requestCode,
        maxSelectable = maxSelectable,
        capture = capture,
        useEmbeddedCamera = useEmbeddedCamera,
        onResult = onResult,
        onCancel = onCancel
    )

    constructor(
        fragment: Fragment,
        requestCode: Int,
        maxSelectable: Int = 1,
        capture: Boolean = true,
        useEmbeddedCamera: Boolean = false,
        onResult: (List<Uri>) -> Unit,
        onCancel: () -> Unit = {}
    ) : this(
        activity = null,
        fragment = fragment,
        requestCode = requestCode,
        maxSelectable = maxSelectable,
        capture = capture,
        useEmbeddedCamera = useEmbeddedCamera,
        onResult = onResult,
        onCancel = onCancel
    )

    private val caller: ActivityResultCaller = fragment ?: requireNotNull(activity)
    private var selectedUris: List<Uri>? = null

    private val singlePickerLauncher = caller.registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        dispatchResult(uri?.let { listOf(it) }.orEmpty())
    }

    private val multiplePickerLauncher = caller.registerForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxOf(maxSelectable, 2))
    ) { uris ->
        if (uris.isEmpty()) {
            onCancel()
        } else {
            dispatchResult(uris)
        }
    }

    fun pick(selectedUris: List<Uri>? = null) {
        this.selectedUris = selectedUris
        val request = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        if (usePhotoPicker()) {
            if (maxSelectable > 1) {
                multiplePickerLauncher.launch(request)
            } else {
                singlePickerLauncher.launch(request)
            }
        } else {
            requestLegacyPermission()
        }
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode != this.requestCode) return false
        if (resultCode != Activity.RESULT_OK) {
            onCancel()
            return true
        }
        dispatchResult(Matisse.obtainResult(data).orEmpty().filterNotNull())
        return true
    }

    private fun dispatchResult(uris: List<Uri>) {
        val result = uris.distinct().take(maxSelectable)
        if (result.isEmpty()) {
            onCancel()
        } else {
            onResult(result)
        }
    }

    private fun requestLegacyPermission() {
        val callback: (Boolean) -> Unit = { granted ->
            if (granted) {
                openLegacyMatisse()
            } else {
                onCancel()
            }
        }
        if (fragment != null) {
            fragment.requestImages2VideosPermissions(
                withVideo = false,
                openSelect = true,
                callback = callback
            )
        } else {
            activity?.requestImages2VideosPermissions(
                withVideo = false,
                openSelect = true,
                callback = callback
            )
        }
    }

    private fun openLegacyMatisse() {
        val matisse = fragment?.let { Matisse.from(it) } ?: activity?.let { Matisse.from(it) } ?: return
        matisse
            .choose(MimeType.ofImage(), false)
            .applyEpnovaMatisseUi()
            .countable(false)
            .capture(capture)
            .useEmbeddedCamera(useEmbeddedCamera)
            .spanCount(4)
            .captureStrategy(CaptureStrategy(true, FileManager.getInstance().fileProvider, "album"))
            .maxSelectable(maxSelectable)
            .setSelectedUris(selectedUris)
            .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
            .thumbnailScale(0.85f)
            .imageEngine(GlideEngine())
            .showSingleMediaType(true)
            .originalEnable(Constants.ORIGINAL_ENABLE)
            .autoHideToolbarOnSingleTap(true)
            .forResult(requestCode)
    }

    companion object {
        fun usePhotoPicker(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    }
}
