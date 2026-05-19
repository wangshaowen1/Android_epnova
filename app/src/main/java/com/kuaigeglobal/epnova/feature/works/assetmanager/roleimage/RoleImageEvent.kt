package com.kuaigeglobal.epnova.feature.works.assetmanager.roleimage


sealed class RoleImageEvent {
    class Click(val item: ItemRoleImageVm) : RoleImageEvent()

    class Select(val item: ItemRoleImageVm?) : RoleImageEvent()
}