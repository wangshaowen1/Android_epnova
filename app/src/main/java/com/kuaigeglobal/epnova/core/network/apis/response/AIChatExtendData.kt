package com.kuaigeglobal.epnova.core.network.apis.response

/**
 *  {
 *             "id": "主键ID",
 *             "userId": "用户ID",
 *             "sessionName": "会话名称（第一条聊天会话消息记录）",
 *             "modeType": "模式类型 1-填词成歌 2-灵感写歌 3-歌曲爆改",
 *             "inspirationLibraryCategoryId": "灵感库分类表ID",
 *             "inspirationLibraryWordId": "灵感库灵感词表ID 11-新用户_填词成歌(取灵感库中填词成歌分类) 14-新用户_灵感库_(取灵感库中某个分类) 17-老用户_灵感库 使用",
 *             "musicRemixExampleSongId": "歌曲爆改示例歌曲表ID",
 *             "musicRemixExampleSongCategoryId": "歌曲爆改示例歌曲分类表ID 12-新用户_歌曲爆改、15-老用户_歌曲爆改 使用",
 *             "recordStatus": "记录状态 0-无效 1-有效",
 *             "createTime": "创建时间",
 *             "modifyTime": "修改时间"
 *         }
 */
data class AIChatExtendData(
    val createTime: Long?,
    val id: String?,
    val inspirationLibraryCategoryId: String?,
    val inspirationLibraryWordId: String?,
    val modeType: Int?,
    val modifyTime: Long?,
    val musicRemixExampleSongCategoryId: String?,
    val musicRemixExampleSongId: String?,
    val recordStatus: Int?,
    val sessionName: String?,
    val remixMusicName: String?,//爆改歌名
    val remixMusicLyric: String?,//爆改歌词
    val userId: String?
)