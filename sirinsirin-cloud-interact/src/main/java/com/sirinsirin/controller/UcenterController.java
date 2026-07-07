package com.sirinsirin.controller;

import com.sirinsirin.api.consumer.VideoClient;
import com.sirinsirin.entity.dto.TokenUserInfoDto;
import com.sirinsirin.entity.po.VideoInfo;
import com.sirinsirin.entity.query.VideoCommentQuery;
import com.sirinsirin.entity.query.VideoDanmuQuery;
import com.sirinsirin.entity.query.VideoInfoQuery;
import com.sirinsirin.entity.vo.PaginationResultVO;
import com.sirinsirin.entity.vo.ResponseVO;
import com.sirinsirin.service.VideoCommentService;
import com.sirinsirin.service.VideoDanmuService;
import com.sirinsirin.annotation.GlobalInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;
import java.util.List;

@RestController
@RequestMapping("/ucenter")
@Validated
@Slf4j
public class UcenterController extends ABaseController{
    @Resource
    private VideoClient videoClient;

    @Resource
    private VideoCommentService videoCommentService;

    @Resource
    private VideoDanmuService videoDanmuService;

    @RequestMapping("/loadComment")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO loadComment(Integer pageNo, String videoId, Integer pageSize) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();

        VideoCommentQuery videoCommentQuery = new VideoCommentQuery();
        videoCommentQuery.setVideoId(videoId);
        videoCommentQuery.setVideoUserId(tokenUserInfoDto.getUserId());
        videoCommentQuery.setOrderBy("comment_id desc");
        videoCommentQuery.setPageNo(pageNo);
        videoCommentQuery.setPageSize(pageSize);
        videoCommentQuery.setQueryVideoInfo(true);
        PaginationResultVO resultVO = videoCommentService.findListByPage(videoCommentQuery);
        return getSuccessResponseVO(resultVO);
    }

    @RequestMapping("/delComment")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO delComment(@NotNull Integer commentId) {
        videoCommentService.deleteComment(commentId, getTokenUserInfoDto().getUserId());
        return getSuccessResponseVO("调用delComment接口成功！");
    }

    @RequestMapping("/loadDanmu")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO loadDanmu(Integer pageNo, String videoId) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
        VideoDanmuQuery danmuQuery = new VideoDanmuQuery();
        danmuQuery.setVideoId(videoId);
        danmuQuery.setVideoUserId(tokenUserInfoDto.getUserId());
        danmuQuery.setOrderBy("danmu_id desc");
        danmuQuery.setPageNo(pageNo);
        danmuQuery.setQueryVideoInfo(true);
        PaginationResultVO resultVO = videoDanmuService.findListByPage(danmuQuery);
        return getSuccessResponseVO(resultVO);
    }

    @RequestMapping("/delDanmu")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO delDanmu(@NotNull Integer danmuId) {
        videoDanmuService.deleteDanmu(getTokenUserInfoDto().getUserId(), danmuId);
        return getSuccessResponseVO("调用delDanmu接口成功！");
    }
}
