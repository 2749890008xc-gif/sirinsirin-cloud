package com.sirinsirin.api.provider;

import com.sirinsirin.entity.constants.Constants;
import com.sirinsirin.entity.query.VideoCommentQuery;
import com.sirinsirin.entity.vo.PaginationResultVO;
import com.sirinsirin.service.VideoCommentService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@RestController
@RequestMapping(Constants.INNER_API_PREFIX + "/comment")
public class VideoCommentApi {

    @Resource
    private VideoCommentService videoCommentService;

    @RequestMapping("/admin/loadComment")
    public PaginationResultVO loadComment(Integer pageNo, String videoNameFuzzy) {
        VideoCommentQuery videoCommentQuery = new VideoCommentQuery();
        videoCommentQuery.setOrderBy("comment_id desc");
        videoCommentQuery.setPageNo(pageNo);
        videoCommentQuery.setQueryVideoInfo(true);
        videoCommentQuery.setVideoNameFuzzy(videoNameFuzzy);
        PaginationResultVO resultVO = videoCommentService.findListByPage(videoCommentQuery);
        return resultVO;
    }

    @RequestMapping("/admin/delComment")
    public void delComment(@NotNull Integer commentId) {
        videoCommentService.deleteComment(commentId, null);
    }

    @RequestMapping("/delCommentByVideoId")
    public void delCommentByVideoId(@NotEmpty String videoId) {
        VideoCommentQuery videoCommentQuery = new VideoCommentQuery();
        videoCommentQuery.setVideoId(videoId);
        videoCommentService.deleteByParam(videoCommentQuery);
    }
}