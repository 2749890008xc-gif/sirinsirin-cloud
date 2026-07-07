package com.sirinsirin.controller;

import com.sirinsirin.api.consumer.InteractClient;
import com.sirinsirin.component.EsSearchComponent;
import com.sirinsirin.component.RedisComponent;
import com.sirinsirin.entity.constants.Constants;
import com.sirinsirin.entity.dto.TokenUserInfoDto;
import com.sirinsirin.entity.enums.*;
import com.sirinsirin.entity.po.UserAction;
import com.sirinsirin.entity.po.VideoInfo;
import com.sirinsirin.entity.po.VideoInfoFile;
import com.sirinsirin.entity.query.UserActionQuery;
import com.sirinsirin.entity.query.VideoInfoFileQuery;
import com.sirinsirin.entity.query.VideoInfoQuery;
import com.sirinsirin.entity.vo.PaginationResultVO;
import com.sirinsirin.entity.vo.ResponseVO;
import com.sirinsirin.entity.vo.VideoInfoResultVO;
import com.sirinsirin.exception.BusinessException;
//import com.sirinsirin.service.UserActionService;
import com.sirinsirin.service.VideoInfoFileService;
import com.sirinsirin.service.VideoInfoService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/video")
@Validated
public class VideoController extends ABaseController{
    @Resource
    private VideoInfoService videoInfoService;

    @Resource
    private VideoInfoFileService videoInfoFileService;

    @Resource
    private RedisComponent redisComponent;

    @Resource
    private EsSearchComponent esSearchComponent;

    @Resource
    private InteractClient interactClient;

    @RequestMapping("/loadRecommendVideo")
    public ResponseVO loadRecommendVideo(){
        VideoInfoQuery videoInfoQuery = new VideoInfoQuery();
        videoInfoQuery.setQueryUserInfo(true);
        videoInfoQuery.setOrderBy("create_time desc");
        videoInfoQuery.setRecommendType(VideoRecommendTypeEnum.RECOMMEND.getType());
        List<VideoInfo> recommendVideoList = videoInfoService.findListByParam(videoInfoQuery);
        return getSuccessResponseVO(recommendVideoList);
    }

    @RequestMapping("/loadVideo")
    public ResponseVO loadVideo(Integer pCategoryId, Integer categoryId, Integer pageNo){
        VideoInfoQuery videoInfoQuery = new VideoInfoQuery();
        videoInfoQuery.setCategoryId(categoryId);
        videoInfoQuery.setpCategoryId(pCategoryId);
        videoInfoQuery.setPageNo(pageNo);
        videoInfoQuery.setQueryUserInfo(true);
        videoInfoQuery.setOrderBy("create_time desc");
        videoInfoQuery.setRecommendType(VideoRecommendTypeEnum.NO_RECOMMEND.getType());
        PaginationResultVO resultVO = videoInfoService.findListByPage(videoInfoQuery);
        return getSuccessResponseVO(resultVO);
    }

    @RequestMapping("/getVideoInfo")
    public ResponseVO getVideoInfo(@NotEmpty String videoId){
        VideoInfo videoInfo = videoInfoService.getVideoInfoByVideoId(videoId);
        if(videoInfo == null){
            throw new BusinessException(ResponseCodeEnum.CODE_404);
        }

        TokenUserInfoDto userInfoDto = getTokenUserInfoDto();
        List<UserAction> userActionList = new ArrayList<>();
        if(userInfoDto != null){
            UserActionQuery actionQuery = new UserActionQuery();
            actionQuery.setUserId(userInfoDto.getUserId());
            actionQuery.setVideoId(videoId);
            actionQuery.setActionTypeArray(new Integer[]{
                    UserActionTypeEnum.VIDEO_LIKE.getType(),
                    UserActionTypeEnum.VIDEO_COLLECT.getType(),
                    UserActionTypeEnum.VIDEO_COIN.getType(),
            });
            userActionList = interactClient.getUserActionList(actionQuery);
        }

        VideoInfoResultVO resultVO = new VideoInfoResultVO(videoInfo, userActionList);
        return getSuccessResponseVO(resultVO);
    }

    @RequestMapping("/loadVideoPList")
    public ResponseVO loadVideoPList(@NotEmpty String videoId){
        VideoInfoFileQuery videoInfoFileQuery = new VideoInfoFileQuery();
        videoInfoFileQuery.setVideoId(videoId);
        videoInfoFileQuery.setOrderBy("file_index asc");
        List<VideoInfoFile> fileList = videoInfoFileService.findListByParam(videoInfoFileQuery);
        return getSuccessResponseVO(fileList);
    }

    @RequestMapping("/reportVideoPlayOnline")
    public ResponseVO reportVideoPlayOnline(@NotEmpty String fileId, @NotEmpty String deviceId){
        return getSuccessResponseVO(redisComponent.reportVideoPlayOnline(fileId, deviceId));
    }

    @RequestMapping("/getVideoRecommend")
    public ResponseVO getVideoRecommend(@NotEmpty String keyword, String videoId){
        List<VideoInfo> videoInfoList = esSearchComponent.search(
                false, keyword, SearchOrderTypeEnum.VIDEO_PLAY.getType(), 1, PageSize.SIZE10.getSize()
                ).getList();
        videoInfoList = videoInfoList.stream().filter(item -> !item.getVideoId().equals(videoId)).collect(Collectors.toList());
        return getSuccessResponseVO(videoInfoList);
    }

    @RequestMapping("/search")
    public ResponseVO search(@NotEmpty String keyword, Integer orderType, Integer pageNo) {
        redisComponent.addKeywordCount(keyword);

        PaginationResultVO<VideoInfo> resultVO = esSearchComponent.search(
                true, keyword, orderType, pageNo, PageSize.SIZE30.getSize()
        );
        return getSuccessResponseVO(resultVO);
    }

    @RequestMapping("/getSearchKeywordTop")
    public ResponseVO getSearchKeywordTop() {
        List<String> keywordList = redisComponent.getKeywordTop(Constants.LENGTH_10);
        return getSuccessResponseVO(keywordList);
    }

    @RequestMapping("/loadHotVideoList")
    public ResponseVO loadHotVideoList(Integer pageNo) {
        VideoInfoQuery videoInfoQuery = new VideoInfoQuery();
        videoInfoQuery.setPageNo(pageNo);
        videoInfoQuery.setQueryUserInfo(true);
        videoInfoQuery.setOrderBy("play_count desc");
        videoInfoQuery.setLastPlayHour(Constants.HOUR_24);
        PaginationResultVO resultVO = videoInfoService.findListByPage(videoInfoQuery);
        return getSuccessResponseVO(resultVO);
    }
}
