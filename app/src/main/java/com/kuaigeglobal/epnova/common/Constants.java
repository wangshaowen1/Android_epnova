package com.kuaigeglobal.epnova.common;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by lu on 2016/6/16.
 */
public class Constants {
    /*环境baseUrl*/
//    public static final String LOCAL_HTTP_BASIC_URL = "http://192.168.0.113:8286";//王富强


    public static final String BUGLY_APP_ID = "f2a68e3399";

    @NotNull
    public static final String LIKED = "-1";
    @NotNull
    public static final String OTHER_USERS_LIKED = "-2";
    @NotNull
    public static final String DEFAULT_BIRTHDAY = "2001-01-01";

    public static final String APP_SCHEME = "epnovascheme";

    public static final String DEFAULT_CHANNEL_ID = "1";

    public static final String SERVICE_URL = "https://file.epnovai.com/data/protocol/terms.html";
    public static final String PRIVACY_URL = "https://file.epnovai.com/data/protocol/privacy.html";
    public static final String DEFAULT_USER_VIP_PROTOCOL_URL = "https://file.jinguomanju.com/data/protocol/hyxy.html";

    public static final String DEFAULT_ALGORITHM_ANNOUNCEMENT = "https://file.jinguomanju.com/data/protocol/tgLLM-algorithm.html";
    public static final String THIRD_PARTY_INFORMATION_SHARING_LIST_HTML_URL = "https://file.kuaichupian.cn/data/protocol/sharing.html";


    public static final String KEY_UNLIMITED = "不限";

    public static final String DEFAULT_CITY = "深圳";
    public static final String DEFAULT_CITY_CODE = "440300";//0755
    public static final boolean ORIGINAL_ENABLE = true;
    public static final boolean DETECT_EMULATOR = false;
    public static final String ID_CREATE_TOPIC = "ID_CREATE_TOPIC";
    public static final int SMS_CODE_LENGTH = 4;
    public static final long PAGE_BEFORE_DELAY_MS = 0;
    public static final long PAGE_AFTER_DELAY_MS = 0;
    public static final int INTERNATIONALIZATION_RATIO = 5;
    public static final String DEFAULT_COUNTRY_CODE = "US";
    /**
     * 视频单张图片时长
     */
    public static final long IMAGE_DURATION_MS = 3000;
    /**
     * 分享mv时长
     */
    public static final long SHARE_MV_DURATION_MS = 60_000;
    /**
     * 测试环境下 自己输入的ip，供后台测试用
     */
    public static final String BASE_URL_CUSTOM = "BASE_URL_CUSTOM";

    public static final String BLANK = "{}";

    //聊天类型 1-开场话题 2-邀约教程 3-恋爱导师 4-如何表白 5-恋爱秘籍 6-夸夸助手 7-体验键盘
    public interface Scene {

        //ai-回复
        String AI_REPLY = "9";
    }

    public interface Model {
        /**
         * 动态漫
         */
        String STORYBOARD_DYNAMIC = "storyboard_dynamic";
        /**
         * 解说漫
         */
        String STORYBOARD_NARRATION = "storyboard_narration";
    }

    public interface Extra {
        String USER_TOKEN = "userToken";
        String USER_NAME = "userName";
        String URL = "url";
        String TITLE = "title";
        String OUTPUT_PATH = "outputPath";
        String COVER = "cover";
        String VIDEO_COVER = "videoCover";
        String VIDEO_TIME = "videoTime";
        String DATA = "data";
        String COST = "COST";
        String ID = "ID";
        String UNLOCK_CHECK = "UNLOCK_CHECK";
        String POSITION = "position";
        String PUSH = "push";
        String CURRENT = "CURRENT";
        String DRAMA_NAME = "dramaName";
        String DRAMA_INTRO = "dramaIntro";
        String DRAMA = "drama";
        String STATUS = "status";

        String STYLE_ID = "STYLE_Id";
        String VIDEO_RATIO = "VIDEO_RATIO";
        String CONTENT = "content";
        String EXTRA = "EXTRA";
        String WORK_TYPE = "WORK_TYPE";
        //举报场景  1-音乐播放界面举报
        String REPORT_SCENE = "reportScene";
        //举报关联ID，举报音乐则是音乐ID
        String REPORT_ID = "reportId";
        //举报的用户id
        String REPORT_USER_ID = "reportUserId";
        //歌单名字
        String PLAYLIST_NAME = "playlistName";
        //新手流程生成完歌曲后进入播放页面
        String SHOW_CREATION_TIPS = "isNoviceProcess";
        String RESULT = "result";
        String LAST_EPISODE_ID = "lastEpisodeId";
        //上一集的剧集id，续写的时候需要
        String PREVIOUS_STORY_ID = "previousStoryId";
        //当前章节的序号
        String CHAPTER_INDEX = "chapterIndex";
        String MV_ID = "mvId";
        String TRACK_NAME = "trackName";
        String TRACKS = "tracks";
        String PAGE = "page";

        String FROM_PAGE = "fromPage";
        String MODE = "mode";

        String TYPE = "type";
        String PREVIEW = "preview";
        String NAME = "name";
        String TAB = "tab";
        String TAB_ID = "tabId";

        String INTERCEPT_BACK = "interceptBack";
        String PLAY_MODE = "playMode";
        String IN_HOME = "inHome";
        String COINS = "coins";
        String MESSAGE = "message";
        String CONFIG_ID = "CONFIG_ID";
        String REWARD_TYPE = "rewardType";
        String SELECT_MODE = "SELECT_MODE";
        String INDEX = "INDEX";
        String COUNT = "COUNT";
        String SHOW_SHARE_DIALOG = "showShareDialog";
        String EXTRA_ID = "EXTRA_ID";
        String TEMPLATE = "template";
        String BROWSERS = "browsers";
        String FRAME_ACTIVITY = "frameActivity";
        String DOWNLOAD_ACCOMPANY = "downloadAccompany";
        String FROM_NEW_USER_WRITE = "fromNewUserWrite";
        String ROOM_ID = "roomId";
        String MUSIC_ROOM = "musicRoom";
        String OPEN_RECOMMEND = "openRecommend";
        String DARK_STYLE = "darkStyle";
        String INSPIRATION = "inspiration";
        String INSPIRATION_TEXT = "inspirationText";
        String CHAT_SESSION_ID = "CHAT_SESSION_ID";
        String MUSIC_NAME = "MUSIC_NAME";
        String MUSIC_LYRICS = "MUSIC_LYRICS";
        String OFFICIAL_PICKS = "officialPicks";
        String DRAMAS_ID = "dramasId";
        String IMAGE_ID = "imageId";
        String UNRELEASED = "unreleased";
        String WORK_ID = "workId";
        String STORY_ID = "storyId";
        String TASK_ID = "taskId";
        String PLAYER_IN_SERIES = "playerInSeries";
        String NEWLY_PUBLISH = "newlyPublish";
        String RESTORE = "restore";
        String ASSET_ID = "ASSET_ID";
        String ASSET_IMAGE_ID = "ASSET_IMAGE_ID";
        String ROLE_IMAGE = "ROLE_IMAGE";
        String APPEARANCE_ID = "APPEARANCE_ID";
    }

}

