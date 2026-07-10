package com.kuaigeglobal.epnova.feature.creator

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.viewmodel.AppViewModel
import com.kuaigeglobal.epnova.core.network.apis.entity.UnusedPackageInfo
import com.nhsd.basic.ext.getParcelableArrayListValue
import com.nhsd.basic.ext.getResString

class ShortDramaCreationPackageViewModel : AppViewModel() {

    val firstPackage = ObservableField<UnusedPackageInfo>()
    val secondPackage = ObservableField<UnusedPackageInfo>()
    val selectedPackage = ObservableField<UnusedPackageInfo>()
    val firstPackageSelected = ObservableBoolean(false)
    val secondPackageSelected = ObservableBoolean(false)
    val hasMultiplePackages = ObservableBoolean(false)

    private var packageOptions = emptyList<UnusedPackageInfo>()
    val displaySubtitle by lazy { mBundle.getString(ShortDramaCreationPackageDialog.ARG_DISPLAY_SUBTITLE) }

    override fun onCreate() {
        super.onCreate()
        packageOptions = normalizePackages(
            mBundle.getParcelableArrayListValue(
                ShortDramaCreationPackageDialog.ARG_PACKAGES,
                UnusedPackageInfo::class.java
            ).orEmpty()
        )
        firstPackage.set(packageOptions.getOrNull(0))
        secondPackage.set(packageOptions.getOrNull(1))
        hasMultiplePackages.set(packageOptions.size >= 2)
        setSelectedPackage(resolveSelectedPackage())
    }

    fun hasPackages(): Boolean {
        return packageOptions.isNotEmpty()
    }

    fun selectedPackageInfo(): UnusedPackageInfo? {
        return selectedPackage.get()
    }

    fun selectFirstPackage(): UnusedPackageInfo? {
        return selectPackage(firstPackage.get())
    }

    fun selectSecondPackage(): UnusedPackageInfo? {
        return selectPackage(secondPackage.get())
    }

    fun titleText(packageInfo: UnusedPackageInfo?): String {
        return packageInfo?.displayTitle.takeIfNotBlank()
            ?: R.string.creation_package_unused_title.getResString()
    }

    fun subtitleText(packageInfo: UnusedPackageInfo?): String {
        return packageInfo?.displayDesc.takeIfNotBlank()
            ?: displaySubtitle.takeIfNotBlank()
            ?: if (hasMultiplePackages.get()) {
                    R.string.creation_package_select_desc
                } else {
                    R.string.creation_package_single_desc
                }.getResString()
    }

    fun isPackageSelected(packageInfo: UnusedPackageInfo?): Boolean {
        return isSamePackage(packageInfo, selectedPackage.get())
    }

    private fun resolveSelectedPackage(): UnusedPackageInfo? {
        val selectedInstanceId = mBundle.getString(ShortDramaCreationPackageDialog.ARG_SELECTED_PACKAGE_INSTANCE_ID)
            .takeIfNotBlank()
        return packageOptions.firstOrNull { it.packageInstanceId == selectedInstanceId }
            ?: packageOptions.firstOrNull { it.isDefaultSelected() }
            ?: packageOptions.firstOrNull()
    }

    private fun normalizePackages(packages: List<UnusedPackageInfo>): List<UnusedPackageInfo> {
        return packages.filter { it.hasValidInstance() }
    }

    private fun selectPackage(packageInfo: UnusedPackageInfo?): UnusedPackageInfo? {
        if (packageInfo == null || isPackageSelected(packageInfo)) return null
        setSelectedPackage(packageInfo)
        return packageInfo
    }

    private fun setSelectedPackage(packageInfo: UnusedPackageInfo?) {
        selectedPackage.set(packageInfo)
        firstPackageSelected.set(isSamePackage(firstPackage.get(), packageInfo))
        secondPackageSelected.set(isSamePackage(secondPackage.get(), packageInfo))
    }

    private fun isSamePackage(left: UnusedPackageInfo?, right: UnusedPackageInfo?): Boolean {
        val selectedInstanceId = right?.packageInstanceId ?: return false
        return left?.packageInstanceId == selectedInstanceId
    }

    private fun String?.takeIfNotBlank(): String? {
        return this?.takeIf { it.isNotBlank() }
    }
}
