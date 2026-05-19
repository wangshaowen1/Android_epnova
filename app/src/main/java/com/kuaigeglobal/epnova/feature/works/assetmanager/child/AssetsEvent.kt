package com.kuaigeglobal.epnova.feature.works.assetmanager.child


sealed class AssetsEvent {
    class Click(val item: ItemAssetsChildVm) : AssetsEvent()

    class Select(val item: ItemAssetsChildVm?) : AssetsEvent()
}