package com.sirinsirin.controller;

import com.sirinsirin.annotation.RecordUserMessage;
import com.sirinsirin.entity.constants.Constants;
import com.sirinsirin.entity.enums.MessageTypeEnum;
import com.sirinsirin.entity.po.UserAction;
import com.sirinsirin.entity.vo.ResponseVO;
import com.sirinsirin.service.UserActionService;
import com.sirinsirin.annotation.GlobalInterceptor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@RestController
@RequestMapping("/userAction")
@Validated
public class UserActionController extends ABaseController{
    @Resource
    private UserActionService userActionService;

    @RequestMapping("/doAction")
    @GlobalInterceptor(checkLogin = true)
    @RecordUserMessage(messageType = MessageTypeEnum.LIKE)
    public ResponseVO doAction(@NotEmpty String videoId,
                               @NotNull Integer actionType,
                               @Max(2) @Min(1) Integer actionCount,
                               Integer commentId){
        UserAction userAction = new UserAction();
        userAction.setVideoId(videoId);
        userAction.setUserId(getTokenUserInfoDto().getUserId());
        userAction.setActionType(actionType);
        actionCount = actionCount == null ? Constants.ONE : actionCount;
        //  如果没有commentId，则给0，如果不加的话，后面sql无法正确查询出相关记录
        commentId = commentId == null ? Constants.ZERO : commentId;
        userAction.setActionCount(actionCount);
        userAction.setCommentId(commentId);
        userActionService.saveAction(userAction);
        return getSuccessResponseVO("调用doAction接口返回成功！");
    }
}
