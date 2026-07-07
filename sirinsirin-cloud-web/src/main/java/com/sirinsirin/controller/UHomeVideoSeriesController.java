package com.sirinsirin.controller;

import com.sirinsirin.entity.dto.TokenUserInfoDto;
import com.sirinsirin.entity.enums.ResponseCodeEnum;
import com.sirinsirin.entity.po.UserVideoSeries;
import com.sirinsirin.entity.po.UserVideoSeriesVideo;
import com.sirinsirin.entity.po.VideoInfo;
import com.sirinsirin.entity.query.UserVideoSeriesQuery;
import com.sirinsirin.entity.query.UserVideoSeriesVideoQuery;
import com.sirinsirin.entity.query.VideoInfoQuery;
import com.sirinsirin.entity.vo.ResponseVO;
import com.sirinsirin.entity.vo.UserVideoSeriesDetailVO;
import com.sirinsirin.exception.BusinessException;
import com.sirinsirin.service.UserVideoSeriesService;
import com.sirinsirin.service.UserVideoSeriesVideoService;
import com.sirinsirin.service.VideoInfoService;
import com.sirinsirin.annotation.GlobalInterceptor;
import com.sun.istack.internal.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/uhome/series")
@Validated
public class UHomeVideoSeriesController extends ABaseController {

    @Resource
    private VideoInfoService videoInfoService;

    @Resource
    private UserVideoSeriesService userVideoSeriesService;

    @Resource
    private UserVideoSeriesVideoService userVideoSeriesVideoService;

    @RequestMapping("/loadVideoSeries")
    public ResponseVO loadVideoSeries(@NotEmpty String userId) {
        List<UserVideoSeries> videoSeries = userVideoSeriesService.getUserAllSeries(userId);
        return getSuccessResponseVO(videoSeries);
    }

    @RequestMapping("/saveVideoSeries")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO saveVideoSeries(Integer seriesId,
                                      @NotEmpty @Size(max = 100) String seriesName,
                                      @Size(max = 200) String seriesDescription,
                                      String videoIds) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
        UserVideoSeries videoSeries = new UserVideoSeries();
        videoSeries.setUserId(tokenUserInfoDto.getUserId());
        videoSeries.setSeriesId(seriesId);
        videoSeries.setSeriesName(seriesName);
        videoSeries.setSeriesDescription(seriesDescription);

        this.userVideoSeriesService.saveUserVideoSeries(videoSeries, videoIds);
        return getSuccessResponseVO(videoSeries);
    }

    @RequestMapping("/loadAllVideo")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO loadAllVideo(Integer seriesId) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
        VideoInfoQuery infoQuery = new VideoInfoQuery();
        //  如果id不为空，则意味着在已有的视频列表下添加视频。此时要保证加载的视频集合中不能含有列表内已添加的视频
        if(seriesId != null){
            UserVideoSeriesVideoQuery videoSeriesVideoQuery = new UserVideoSeriesVideoQuery();
            videoSeriesVideoQuery.setSeriesId(seriesId);
            videoSeriesVideoQuery.setUserId(tokenUserInfoDto.getUserId());
            List<UserVideoSeriesVideo> seriesVideoList = userVideoSeriesVideoService.findListByParam(videoSeriesVideoQuery);
            List<String> videoIdList = seriesVideoList.stream().map(item->item.getVideoId()).collect(Collectors.toList());
            infoQuery.setExcludeVideoIdArray(videoIdList.toArray(new String[videoIdList.size()]));
        }

        infoQuery.setUserId(tokenUserInfoDto.getUserId());
        List<VideoInfo> videoInfoList = videoInfoService.findListByParam(infoQuery);
        return getSuccessResponseVO(videoInfoList);
    }

    @RequestMapping("/getVideoSeriesDetail")
    public ResponseVO getVideoSeriesDetail(@NotNull Integer seriesId) {
        UserVideoSeries videoSeries = userVideoSeriesService.getUserVideoSeriesBySeriesId(seriesId);
        if(videoSeries == null){
            throw new BusinessException(ResponseCodeEnum.CODE_404);
        }
        UserVideoSeriesVideoQuery videoSeriesVideoQuery = new UserVideoSeriesVideoQuery();
        videoSeriesVideoQuery.setOrderBy("sort asc");
        videoSeriesVideoQuery.setQueryVideoInfo(true);
        videoSeriesVideoQuery.setSeriesId(seriesId);
        List<UserVideoSeriesVideo> seriesVideoList = userVideoSeriesVideoService.findListByParam(videoSeriesVideoQuery);

        return getSuccessResponseVO(new UserVideoSeriesDetailVO(videoSeries, seriesVideoList));
    }

    //  向集合内添加视频
    @RequestMapping("/saveSeriesVideo")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO saveSeriesVideo(@NotNull Integer seriesId, @NotEmpty String videoIds) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
        this.userVideoSeriesService.saveSeriesVideo(tokenUserInfoDto.getUserId(), seriesId, videoIds);
        return getSuccessResponseVO("调用saveSeriesVideo接口成功!");
    }

    //  从集合中删除视频，一次只允许删除一个
    @RequestMapping("/delSeriesVideo")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO delSeriesVideo(@NotNull Integer seriesId, @NotEmpty String videoId) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
        this.userVideoSeriesService.delSeriesVideo(tokenUserInfoDto.getUserId(), seriesId, videoId);
        return getSuccessResponseVO("调用delSeriesVideo接口成功!");
    }

    //  删除视频集合
    @RequestMapping("/delVideoSeries")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO delVideoSeries(@NotNull Integer seriesId) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
        this.userVideoSeriesService.delVideoSeries(tokenUserInfoDto.getUserId(), seriesId);
        return getSuccessResponseVO("调用delVideoSeries接口成功!");
    }

    //  更改视频集合的顺序
    @RequestMapping("/changeVideoSeriesSort")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO changeVideoSeriesSort(@NotEmpty String seriesIds) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
        this.userVideoSeriesService.changeVideoSeriesSort(tokenUserInfoDto.getUserId(), seriesIds);
        return getSuccessResponseVO("调用changeVideoSeriesSort接口成功!");
    }

    @RequestMapping("/loadVideoSeriesWithVideo")
    public ResponseVO loadVideoSeriesWithVideo(@NotEmpty String userId) {
        UserVideoSeriesQuery seriesQuery = new UserVideoSeriesQuery();
        seriesQuery.setUserId(userId);
        seriesQuery.setOrderBy("sort asc");
        List<UserVideoSeries> videoSeries = userVideoSeriesService.findListWithVideoList(seriesQuery);
        return getSuccessResponseVO(videoSeries);
    }
}
