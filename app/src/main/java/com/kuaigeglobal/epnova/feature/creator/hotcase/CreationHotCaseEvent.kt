package com.kuaigeglobal.epnova.feature.creator.hotcase

sealed class CreationHotCaseEvent {
    class CreateSame(val item: ItemCreationHotCaseVM) : CreationHotCaseEvent()
}
