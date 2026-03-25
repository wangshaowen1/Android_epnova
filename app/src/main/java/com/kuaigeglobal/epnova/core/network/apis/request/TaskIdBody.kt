package com.kuaigeglobal.epnova.core.network.apis.request

/**
 * @param taskId 任务 ID
 * @param bizType 查询单个资产必须要入参业务类型 查询全部资产、分镜描述、分镜视频不用入参业务类型枚举 [41=角色图片 51=道具图片 61=场景图片]70=分镜拆分/文本描述 71=分镜图片 72=分镜视频
 */
data class TaskIdBody(
    /**
     * 任务 ID
     */
    val taskId: String?,
    /**
     * 查询单个资产必须要入参业务类型 查询全部资产、分镜描述、分镜视频不用入参业务类型枚举 [41=角色图片 51=道具图片 61=场景图片]70=分镜拆分/文本描述 71=分镜图片 72=分镜视频
     */
    val bizType: Int?
)