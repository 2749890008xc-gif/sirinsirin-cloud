package com.sirinsirin.api.provider;

import com.sirinsirin.annotation.RecordUserMessage;
import com.sirinsirin.component.EsSearchComponent;
import com.sirinsirin.entity.constants.Constants;
import com.sirinsirin.entity.enums.MessageTypeEnum;
import com.sirinsirin.entity.enums.SearchOrderTypeEnum;
import com.sirinsirin.entity.po.VideoInfo;
import com.sirinsirin.entity.po.VideoInfoFile;
import com.sirinsirin.entity.po.VideoInfoFilePost;
import com.sirinsirin.entity.po.VideoInfoPost;
import com.sirinsirin.entity.query.VideoInfoFilePostQuery;
import com.sirinsirin.entity.query.VideoInfoPostQuery;
import com.sirinsirin.entity.query.VideoInfoQuery;
import com.sirinsirin.entity.vo.PaginationResultVO;
import com.sirinsirin.service.VideoInfoFilePostService;
import com.sirinsirin.service.VideoInfoFileService;
import com.sirinsirin.service.VideoInfoPostService;
import com.sirinsirin.service.VideoInfoService;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@RestController
@RequestMapping(Constants.INNER_API_PREFIX + "/video")
@Validated
public class VideoInfoApi {

    @Resource
    private VideoInfoService videoInfoService;

    @Resource
    private VideoInfoFileService videoInfoFileService;

    @Resource
    private VideoInfoPostService videoInfoPostService;

    @Resource
    private VideoInfoFilePostService videoInfoFilePostService;

    @Resource
    private EsSearchComponent esSearchComponent;


    @RequestMapping("/getVideoInfoFileByFileId")
    public VideoInfoFile getVideoInfoFileByFileId(@NotEmpty String fileId) {
        return videoInfoFileService.getVideoInfoFileByFileId(fileId);
    }

    @RequestMapping("/getVideoInfoByVideoId")
    public VideoInfo getVideoInfoByVideoId(@NotEmpty String videoId) {
        return videoInfoService.getVideoInfoByVideoId(videoId);
    }

    @RequestMapping("/updateCountInfo")
    public void updateCountInfo(String videoId, String fileId, Integer changeCount) {
        videoInfoService.updateCountInfo(videoId, fileId, changeCount);
    }

    @RequestMapping("/getVideoInfoPostByVideoId")
    public VideoInfoPost getVideoInfoPostByVideoId(String videoId) {
        return videoInfoPostService.getVideoInfoPostByVideoId(videoId);
    }

    @RequestMapping("/updateDocCount")
    public void updateDocCount(String videoId, SearchOrderTypeEnum searchOrderTypeEnum, Integer changeCount) {
        esSearchComponent.updateDocCount(videoId, searchOrderTypeEnum.getField(), changeCount);
    }

    @RequestMapping("/admin/loadVideoList")
    public PaginationResultVO loadVideoPost(@RequestBody VideoInfoPostQuery videoInfoPostQuery){
        videoInfoPostQuery.setOrderBy("v.last_update_time desc");
        videoInfoPostQuery.setQueryCountInfo(true);
        videoInfoPostQuery.setQueryUserInfo(true);
        PaginationResultVO resultVO = videoInfoPostService.findListByPage(videoInfoPostQuery);
        return resultVO;
    }

    @RequestMapping("/admin/auditVideo")
    @RecordUserMessage(messageType = MessageTypeEnum.SYS)
    public void auditVideo(@NotEmpty String videoId, @NotNull Integer status, String reason){
        videoInfoPostService.auditVideo(videoId, status, reason);
    }

    @RequestMapping("/admin/recommendVideo")
    public void recommendVideo(@NotEmpty String videoId) {
        videoInfoService.recommendVideo(videoId);
    }

    @RequestMapping("/admin/deleteVideo")
    public void deleteVideo(@NotEmpty String videoId){
        videoInfoService.deleteVideo(videoId, null);
    }

    @RequestMapping("/admin/loadVideoPList")
    public List<VideoInfoFilePost> loadVideoPList(@NotEmpty String videoId){
        VideoInfoFilePostQuery postQuery = new VideoInfoFilePostQuery();
        postQuery.setOrderBy("file_index asc");
        postQuery.setVideoId(videoId);
        List<VideoInfoFilePost> videoInfoFilePostList = videoInfoFilePostService.findListByParam(postQuery);

        return videoInfoFilePostList;
    }

    @RequestMapping("/getVideoCount")
    public Integer getVideoCount(@RequestBody VideoInfoQuery videoInfoQuery){
        return videoInfoService.findCountByParam(videoInfoQuery);
    }

    @RequestMapping("/transferVideoFile4Db")
    public void transferVideoFile4Db(
            @RequestParam String videoId,
            @RequestParam String uploadId,
            @RequestParam String userId,
            @RequestBody VideoInfoFilePost uploadFilePost) {
        videoInfoPostService.transferVideoFile4Db(videoId, uploadId, userId, uploadFilePost);
    }
}
