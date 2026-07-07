package com.sirinsirin.controller;

import com.sirinsirin.annotation.GlobalInterceptor;
import com.sirinsirin.annotation.RecordUserMessage;
import com.sirinsirin.api.consumer.VideoClient;
import com.sirinsirin.entity.constants.Constants;
import com.sirinsirin.entity.dto.TokenUserInfoDto;
import com.sirinsirin.entity.enums.CommentTopTypeEnum;
import com.sirinsirin.entity.enums.MessageTypeEnum;
import com.sirinsirin.entity.enums.PageSize;
import com.sirinsirin.entity.enums.UserActionTypeEnum;
import com.sirinsirin.entity.po.UserAction;
import com.sirinsirin.entity.po.VideoComment;
import com.sirinsirin.entity.po.VideoInfo;
import com.sirinsirin.entity.query.UserActionQuery;
import com.sirinsirin.entity.query.VideoCommentQuery;
import com.sirinsirin.entity.vo.PaginationResultVO;
import com.sirinsirin.entity.vo.ResponseVO;
import com.sirinsirin.entity.vo.VideoCommentResultVO;
import com.sirinsirin.service.UserActionService;
import com.sirinsirin.service.VideoCommentService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/comment")
@Validated
public class VideoCommentController extends ABaseController {

    @Resource
    private VideoCommentService videoCommentService;

    @Resource
    private UserActionService userActionService;

    @Resource
    private VideoClient videoClient;

    @RequestMapping("/postComment")
    @GlobalInterceptor(checkLogin = true)
    @RecordUserMessage(messageType = MessageTypeEnum.COMMENT)
    public ResponseVO postComment(@NotEmpty String videoId,
                                  @NotEmpty @Size(max = 500) String content,
                                  Integer replyCommentId,
                                  @Size(max = 50) String imgPath){
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
        VideoComment comment = new VideoComment();
        comment.setUserId(tokenUserInfoDto.getUserId());
        comment.setAvatar(tokenUserInfoDto.getAvatar());
        comment.setNickName(tokenUserInfoDto.getNickName());
        comment.setVideoId(videoId);
        comment.setContent(content);
        comment.setImgPath(imgPath);
        videoCommentService.postComment(comment, replyCommentId);

        return getSuccessResponseVO(comment);
    }

    @RequestMapping("/loadComment")
    public ResponseVO loadComment(@NotEmpty String videoId,
                                  Integer pageNo,
                                  Integer orderType){
        VideoInfo videoInfo = videoClient.getVideoInfoByVideoId(videoId);
        if(videoInfo.getInteraction() != null && videoInfo.getInteraction().contains(Constants.ZERO.toString())){
            return getSuccessResponseVO(new ArrayList<>());
        }
        VideoCommentQuery commentQuery = new VideoCommentQuery();
        commentQuery.setVideoId(videoId);
        commentQuery.setLoadChildren(true);
        commentQuery.setPageNo(pageNo);
        commentQuery.setPageSize(PageSize.SIZE15.getSize());
        commentQuery.setpCommentId(0);
        String orderBy = orderType == null || orderType == 0 ? "like_count desc, comment_id desc" : "comment_id desc";
        commentQuery.setOrderBy(orderBy);
        PaginationResultVO<VideoComment> commentData = videoCommentService.findListByPage(commentQuery);

        //  这里采用的是单独查询出置顶的评论和其他评论，将其他评论排好序后，直接将置顶评论插在其他评论前面。这种方式比起增加order by 排序条件的方式更块、更节省效率
        //  只在第一页的时候查询，其他时候不用查询
        //  原来是pageNo == null
        if(pageNo == null || pageNo == 1){
            List<VideoComment> topCommentList = topComment(videoId);
            //  如果查询到置顶的评论，则从已经查询好的其他评论列表中将该评论排除掉，因为置顶评论只有一条，所以使用 " get(0) " 即可
            if(!topCommentList.isEmpty()){
                List<VideoComment> commentList = commentData.getList().stream().filter(
                        item -> !item.getCommentId().equals( topCommentList.get(0).getCommentId() )
                ).collect( Collectors.toList() );
                commentList.addAll(0, topCommentList);
                commentData.setList(commentList);
            }
        }

        VideoCommentResultVO resultVO = new VideoCommentResultVO();
        resultVO.setCommentData(commentData);

        List<UserAction> userActionList = new ArrayList<>();
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
        if(tokenUserInfoDto != null){
            UserActionQuery actionQuery = new UserActionQuery();
            actionQuery.setUserId(tokenUserInfoDto.getUserId());
            actionQuery.setVideoId(videoId);
            actionQuery.setActionTypeArray(new Integer[]{
                    UserActionTypeEnum.COMMENT_LIKE.getType(),
                    UserActionTypeEnum.COMMENT_HATE.getType(),
            });
            userActionList = userActionService.findListByParam(actionQuery);
        }
        resultVO.setUserActionList(userActionList);
        return getSuccessResponseVO(resultVO);
    }

    //  查找该视频的置顶评论
    private List<VideoComment> topComment(String videoId){
        VideoCommentQuery commentQuery = new VideoCommentQuery();
        commentQuery.setVideoId(videoId);
        commentQuery.setTopType(CommentTopTypeEnum.TOP.getType());
        commentQuery.setLoadChildren(true);
        List<VideoComment> videoCommentList = videoCommentService.findListByParam(commentQuery);
        return videoCommentList;
    }

    @RequestMapping("/topComment")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO topComment(@NotNull Integer commentId){
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
        videoCommentService.topComment(commentId, tokenUserInfoDto.getUserId());

        return getSuccessResponseVO("调用topComment接口成功！");
    }

    @RequestMapping("/cancelTopComment")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO cancelTopComment(@NotNull Integer commentId){
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
        videoCommentService.cancelTopComment(commentId, tokenUserInfoDto.getUserId());

        return getSuccessResponseVO("调用cancelTopComment接口成功！");
    }

    @RequestMapping("/userDelComment")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO userDelComment(@NotNull Integer commentId){
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
        videoCommentService.deleteComment(commentId, tokenUserInfoDto.getUserId());

        return getSuccessResponseVO("调用userDelComment接口成功！");
    }
}