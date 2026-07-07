package com.sirinsirin.controller;

import com.sirinsirin.annotation.RecordUserMessage;
import com.sirinsirin.api.consumer.WebClient;
import com.sirinsirin.entity.enums.MessageTypeEnum;
import com.sirinsirin.entity.po.VideoInfoFilePost;
import com.sirinsirin.entity.query.VideoInfoFilePostQuery;
import com.sirinsirin.entity.query.VideoInfoPostQuery;
import com.sirinsirin.entity.vo.PaginationResultVO;
import com.sirinsirin.entity.vo.ResponseVO;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@RestController
@RequestMapping("/videoInfo")
@Validated
public class VideoInfoController extends ABaseController {
    @Resource
    private WebClient webClient;

    @RequestMapping("/loadVideoList")
    public ResponseVO loadVideoPost(VideoInfoPostQuery videoInfoPostQuery) {
        return getSuccessResponseVO(webClient.loadVideoList(videoInfoPostQuery));
    }

    @RequestMapping("/auditVideo")
    @RecordUserMessage(messageType = MessageTypeEnum.SYS)
    public ResponseVO auditVideo(@NotEmpty String videoId, @NotNull Integer status, String reason) {
        webClient.auditVideo(videoId, status, reason);
        return getSuccessResponseVO(null);
    }

    @RequestMapping("/recommendVideo")
    public ResponseVO recommendVideo(@NotEmpty String videoId) {
        webClient.recommendVideo(videoId);
        return getSuccessResponseVO("调用recommendVideo接口成功！");
    }

    @RequestMapping("/deleteVideo")
    public ResponseVO deleteVideo(@NotEmpty String videoId) {
        webClient.deleteVideo(videoId);
        return getSuccessResponseVO("调用deleteVideo接口成功！");
    }

    @RequestMapping("/loadVideoPList")
    public ResponseVO loadVideoPList(@NotEmpty String videoId){
        return getSuccessResponseVO(webClient.loadVideoPList(videoId));
    }
}
