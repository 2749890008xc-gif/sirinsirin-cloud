package com.sirinsirin.entity.constants;

public class Constants {
    public static final Integer ONE = 1;
    public static final Integer MINUS_ONE = -1;
    public static final Integer ZERO = 0;
    public static final Integer LENGTH_2 = 2;
    public static final Integer LENGTH_10 = 10;
    public static final Integer LENGTH_15 = 15;
    public static final Integer LENGTH_20 = 20;
    public static final Integer LENGTH_30 = 30;
    public static final Long MB_SIZE = 1024 * 1024L;
    public static final Integer HOUR_24 = 24;
    public static final String REGEX_PASSWORD = "(?=.*\\d)(?=.*[a-zA-z])[\\da-zA-Z~!@#$%^&*_]{8,18}";
    public static final Integer REDIS_KEY_EXPIRES_ONE_SECONDS = 1000;
    public static final Integer REDIS_KEY_EXPIRES_ONE_MIN = 60 * 1000;
    public static final String FILE_FOLDER = "file/";
    public static final String FILE_COVER = "cover/";
    public static final String FILE_VIDEO = "video/";
    public static final String FILE_FOLDER_TEMP = "temp/";
    public static final Integer REDIS_KEY_EXPIRES_ONE_DAY = REDIS_KEY_EXPIRES_ONE_MIN * 60 * 24;
    public static final Integer TIME_SECONDS_DAY = REDIS_KEY_EXPIRES_ONE_DAY / 1000;
    public static final String REDIS_KEY_PREFIX = "sirinsirin:";
    public static final String REDIS_KEY_CHECK_CODE = REDIS_KEY_PREFIX + "checkcode:";
    public static final String REDIS_KEY_EMAIL_CHECK_CODE = REDIS_KEY_PREFIX + "email:checkcode:";
    public static final String REDIS_KEY_TOKEN_WEB = REDIS_KEY_PREFIX + "token:web:";
    public static final String REDIS_KEY_TOKEN_ADMIN = REDIS_KEY_PREFIX + "token:admin:";
    public static final String TOKEN_WEB = "token";
    public static final String TOKEN_ADMIN = "adminToken";
    public static final String REDIS_KEY_CATEGORY_LIST = REDIS_KEY_PREFIX + "category:list:";
    public static final String IMAGE_THUMBNAIL_SUFFIX = "_thumbnail.";
    public static final String REDIS_KEY_UPLOADING_FILE = REDIS_KEY_PREFIX + "uploading:";
    //  系统设置
    public static final String REDIS_KEY_SYS_SETTING = REDIS_KEY_PREFIX + "sysSetting:";
    //  删除文件的结合
    public static final String REDIS_KEY_FILE_DEL = REDIS_KEY_PREFIX + "file:list:del:";
    public static final String REDIS_KEY_QUEUE_TRANSFER = REDIS_KEY_PREFIX + "queue:transfer:";
    public static final String REDIS_KEY_QUEUE_VIDEO_PLAY = REDIS_KEY_PREFIX + "queue:video:play:";
    public static final String TEMP_VIDEO_NAME = "/temp.mp4";
    public static final String VIDEO_CODE_HEVC = "hevc";
    public static final String VIDEO_CODE_H264 = "h264";
    public static final String VIDEO_CODE_TEMP_FILE_SUFFIX = "_temp";
    public static final String TS_NAME = "index.ts";
    public static final String M3U8_NAME = "index.m3u8";

    //视频在线
    public static final String REDIS_KEY_VIDEO_PLAY_COUNT_ONLINE_PREFIX = REDIS_KEY_PREFIX + "video:play:online:";
    public static final String REDIS_KEY_VIDEO_PLAY_COUNT_ONLINE = REDIS_KEY_VIDEO_PLAY_COUNT_ONLINE_PREFIX + "count:%s";
    public static final String REDIS_KEY_VIDEO_PLAY_COUNT_USER_PREFIX = "user:";
    public static final String REDIS_KEY_VIDEO_PLAY_COUNT_USER = REDIS_KEY_VIDEO_PLAY_COUNT_ONLINE_PREFIX + REDIS_KEY_VIDEO_PLAY_COUNT_USER_PREFIX + "%s:%s";

    public static final Integer UPDATE_NICK_NAME_COIN = 3;
    public static final String REDIS_KEY_VIDEO_SEARCH_COUNT = REDIS_KEY_PREFIX + "video:search:";
    public static final String REDIS_KEY_VIDEO_PLAY_COUNT = REDIS_KEY_PREFIX + "video:playcount:";

    public static final String INNER_API_PREFIX = "/innerApi";
    public static final String SERVER_NAME_ADMIN = "sirinsirin-cloud-admin";
    public static final String SERVER_NAME_WEB = "sirinsirin-cloud-web";
    public static final String SERVER_NAME_RESOURCE = "sirinsirin-cloud-resource";
    public static final String SERVER_NAME_INTERACT = "sirinsirin-cloud-interact";


}
