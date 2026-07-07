package com.sirinsirin.component;

import com.sirinsirin.entity.config.AppConfig;
import com.sirinsirin.entity.constants.Constants;
import com.sirinsirin.entity.dto.SysSettingDto;
import com.sirinsirin.entity.dto.TokenUserInfoDto;
import com.sirinsirin.entity.dto.UploadingFileDto;
import com.sirinsirin.entity.dto.VideoPlayInfoDto;
import com.sirinsirin.entity.enums.DateTimePatternEnum;
import com.sirinsirin.entity.po.CategoryInfo;
import com.sirinsirin.entity.po.VideoInfoFilePost;
import com.sirinsirin.redis.RedisUtils;
import com.sirinsirin.utils.DateUtil;
import com.sirinsirin.utils.StringTools;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class RedisComponent {

    @Resource
    private RedisUtils redisUtils;

    @Resource
    private AppConfig appConfig;

    public String saveCheckCode(String code){   //  将验证码以key -> value的形式存入redis缓存中
        String checkCodeKey = UUID.randomUUID().toString();
        redisUtils.setex(Constants.REDIS_KEY_CHECK_CODE+checkCodeKey,code, Constants.REDIS_KEY_EXPIRES_ONE_MIN * 10);
        return checkCodeKey;
    }

    public String getCheckCode(String checkCodeKey){
        return (String)redisUtils.get(Constants.REDIS_KEY_CHECK_CODE+checkCodeKey);
    }

    public void cleanCheckCode(String checkCodeKey){    //  将已用过的验证码从redis中清除掉
        redisUtils.delete(Constants.REDIS_KEY_CHECK_CODE+checkCodeKey);
    }

    public String saveEmailCheckCode(String emailCheckCode, String email, String type){
        type += ":";
        redisUtils.setex(Constants.REDIS_KEY_EMAIL_CHECK_CODE + type + email, emailCheckCode, Constants.REDIS_KEY_EXPIRES_ONE_MIN * 10);
        return email;
    }

    public String getEmailCheckCode(String email, String type){
        type += ":";
        return (String)redisUtils.get(Constants.REDIS_KEY_EMAIL_CHECK_CODE + type + email);
    }

    public void cleanEmailCheckCode(String email, String type){
        type += ":";
        redisUtils.delete(Constants.REDIS_KEY_EMAIL_CHECK_CODE + type + email);
    }

    public void savaTokenInfo(TokenUserInfoDto tokenUserInfoDto){   //  将生成的token保存进redis中
        String token = UUID.randomUUID().toString();
        //  设置token的过期时间为7天
        tokenUserInfoDto.setExpireAt(System.currentTimeMillis() + Constants.REDIS_KEY_EXPIRES_ONE_DAY * 7);
        tokenUserInfoDto.setToken(token);
        redisUtils.setex(Constants.REDIS_KEY_TOKEN_WEB + token, tokenUserInfoDto, Constants.REDIS_KEY_EXPIRES_ONE_DAY * 7);
    }

    public void updateTokenInfo(TokenUserInfoDto tokenUserInfoDto){
        redisUtils.setex(Constants.REDIS_KEY_TOKEN_WEB + tokenUserInfoDto.getToken(), tokenUserInfoDto, Constants.REDIS_KEY_EXPIRES_ONE_DAY * 7);
    }

    public void cleanToken(String token){   //  将指定的token删除
         redisUtils.delete(Constants.REDIS_KEY_TOKEN_WEB+token);
    }

    public TokenUserInfoDto getTokenInfo(String token){
        return (TokenUserInfoDto)redisUtils.get(Constants.REDIS_KEY_TOKEN_WEB+token);
    }

    public String savaTokenInfo4Admin(String account){
        String token = UUID.randomUUID().toString();
        redisUtils.setex(Constants.REDIS_KEY_TOKEN_ADMIN + token, account, Constants.REDIS_KEY_EXPIRES_ONE_DAY);
        return token;
    }

    public String getTokenInfo4Admin(String token){
        return (String)redisUtils.get(Constants.REDIS_KEY_TOKEN_ADMIN+token);
    }

    public void cleanToken4Admin(String token){   //  将指定的token删除
        redisUtils.delete(Constants.REDIS_KEY_TOKEN_ADMIN+token);
    }

    public void saveCategoryList(List<CategoryInfo> categoryInfoList){
        redisUtils.set(Constants.REDIS_KEY_CATEGORY_LIST, categoryInfoList);
    }

    public List<CategoryInfo> getCategoryList(){
        return (List<CategoryInfo>)redisUtils.get(Constants.REDIS_KEY_CATEGORY_LIST);
    }

    public String savePreVideoFileInfo(String userId, String fileName, Integer chunks){
        String uploadId = StringTools.getRandomString(Constants.LENGTH_15);
        UploadingFileDto fileDto = new UploadingFileDto();
        fileDto.setChunks(chunks);
        fileDto.setFileName(fileName);
        fileDto.setUploadId(uploadId);
        fileDto.setChunkIndex(0);
        String day = DateUtil.format(new Date(), DateTimePatternEnum.YYYYMMDD.getPattern());
        String filePath = day + "/" + userId + "-" + uploadId;
        String folder = appConfig.getProjectFolder() + Constants.FILE_FOLDER + Constants.FILE_FOLDER_TEMP + filePath;
        File folderFile = new File(folder);
        if(!folderFile.exists()){
            folderFile.mkdirs();
        }
        fileDto.setFilePath(filePath);
        redisUtils.setex(
        Constants.REDIS_KEY_UPLOADING_FILE + userId + "-" + uploadId,
                fileDto,
                Constants.REDIS_KEY_EXPIRES_ONE_DAY
        );
        return uploadId;
    }

    public UploadingFileDto getUploadVideoFile(String userId, String uploadId){
        return (UploadingFileDto)redisUtils.get(Constants.REDIS_KEY_UPLOADING_FILE + userId + "-" + uploadId);
    }

    public SysSettingDto getSysSettingDto(){
        SysSettingDto sysSettingDto = (SysSettingDto) redisUtils.get(Constants.REDIS_KEY_SYS_SETTING);
        if(sysSettingDto == null){
            sysSettingDto = new SysSettingDto();
        }
        return sysSettingDto;
    }

    public void saveSettingDto(SysSettingDto sysSettingDto){
        redisUtils.set(Constants.REDIS_KEY_SYS_SETTING, sysSettingDto);
    }

    //  用于更新Redis缓存
    public void updateVideoFileInfo(String userId, UploadingFileDto fileDto){
        redisUtils.setex(
                Constants.REDIS_KEY_UPLOADING_FILE + userId + "-" + fileDto.getUploadId(),
                fileDto,
                Constants.REDIS_KEY_EXPIRES_ONE_DAY
        );
    }

    public void delVideoFileInfo(String userId, String uploadId){
        redisUtils.delete(Constants.REDIS_KEY_UPLOADING_FILE + userId + "-" + uploadId);
    }

    //  redis可以实现轻量级的消息队列，所以这里没有使用rabbitmq
    public void addFile2DelQueue(String videoId, List<String> filePathList){
        //  视频产生修改 -> 待审核，与此同时进行视频删除操作，若审核不通过，应当保证删除操作和修改操作都不执行。
        //  因此这里将过期时间延长至七天（以保证管理员能够在失效时间内完成审核）
        redisUtils.lpushAll(Constants.REDIS_KEY_FILE_DEL + videoId, filePathList, Constants.REDIS_KEY_EXPIRES_ONE_DAY * 7);
    }

    public List<String> getDelFileList(String videoId){
        return redisUtils.getQueueList(Constants.REDIS_KEY_FILE_DEL + videoId);
    }

    public void cleanDelFileList(String videoId){
        redisUtils.delete(Constants.REDIS_KEY_FILE_DEL + videoId);
    }

    public void addFile2TransferQueue(List<VideoInfoFilePost> addFileList){
        redisUtils.lpushAll(Constants.REDIS_KEY_QUEUE_TRANSFER, addFileList, 0);    //  lpush表示从左边加入队列，这里使用“0”表示永不过期
    }

    public VideoInfoFilePost getFileFromTransferQueue(){
        return (VideoInfoFilePost) redisUtils.rpop(Constants.REDIS_KEY_QUEUE_TRANSFER); //  rpop表示从右边取出
    }

    public Integer reportVideoPlayOnline(String fileId, String deviceId){
        String userPlayOnlineKey = String.format(Constants.REDIS_KEY_VIDEO_PLAY_COUNT_USER, fileId, deviceId);

        //  同一视频下的不同分p之间的在线观看人数是分开的，所以使用fileId
        String playOnlineCountKey = String.format(Constants.REDIS_KEY_VIDEO_PLAY_COUNT_ONLINE, fileId);

        if(!redisUtils.keyExists(userPlayOnlineKey)){
            //  这里的失效时间必须要大于前端发送请求的心跳间隔，心跳间隔为5秒，这里设置失效时间为8秒
            redisUtils.setex(userPlayOnlineKey, fileId, Constants.REDIS_KEY_EXPIRES_ONE_SECONDS * 8);
            //  这里则是要比上面多一点
            return redisUtils.incrementex(playOnlineCountKey, Constants.REDIS_KEY_EXPIRES_ONE_SECONDS * 10).intValue();
        }
        redisUtils.expire(playOnlineCountKey, Constants.REDIS_KEY_EXPIRES_ONE_SECONDS * 10);
        redisUtils.expire(userPlayOnlineKey, Constants.REDIS_KEY_EXPIRES_ONE_SECONDS * 8);
        Integer count = (Integer) redisUtils.get(playOnlineCountKey);

        return count == null ? 1 : count;
    }

    public void decrementPlayOnlineCount(String key){
        redisUtils.decrement(key);
    }

    public void addKeywordCount(String keyword) {
        redisUtils.zaddCount(Constants.REDIS_KEY_VIDEO_SEARCH_COUNT, keyword);
    }

    public List<String> getKeywordTop(Integer top) {
        return redisUtils.getZSetList(Constants.REDIS_KEY_VIDEO_SEARCH_COUNT,top - 1);
    }

    public void addVideoPlay(VideoPlayInfoDto videoPlayInfoDto) {
        redisUtils.lpush(Constants.REDIS_KEY_QUEUE_VIDEO_PLAY, videoPlayInfoDto, null);
    }

    public VideoPlayInfoDto getVideoPlayFromVideoPlayQueue() {
        return (VideoPlayInfoDto) redisUtils.rpop(Constants.REDIS_KEY_QUEUE_VIDEO_PLAY);
    }

    public void recordVideoPlayCount(String videoId) {
        String date = DateUtil.format(new Date(), DateTimePatternEnum.YYYY_MM_DD.getPattern());
        redisUtils.incrementex(Constants.REDIS_KEY_VIDEO_PLAY_COUNT + date + ":" + videoId, Constants.REDIS_KEY_EXPIRES_ONE_DAY * 2L);
    }

    public Map<String, Integer> getVideoPlayCount(String date) {
        Map<String, Integer> videoPlayMap = redisUtils.getBatch(Constants.REDIS_KEY_VIDEO_PLAY_COUNT + date);
        return videoPlayMap;
    }
}
