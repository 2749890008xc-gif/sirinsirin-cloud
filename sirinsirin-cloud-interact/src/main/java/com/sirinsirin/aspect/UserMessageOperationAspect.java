package com.sirinsirin.aspect;

import com.sirinsirin.annotation.RecordUserMessage;
import com.sirinsirin.component.RedisComponent;
import com.sirinsirin.entity.constants.Constants;
import com.sirinsirin.entity.dto.TokenUserInfoDto;
import com.sirinsirin.entity.enums.MessageTypeEnum;
import com.sirinsirin.entity.enums.UserActionTypeEnum;
import com.sirinsirin.entity.vo.ResponseVO;
import com.sirinsirin.service.UserMessageService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

@Component
@Aspect
@Slf4j
public class UserMessageOperationAspect {

    @Resource
    private RedisComponent redisComponent;

    @Resource
    private UserMessageService userMessageService;

    private static final String PARAMETERS_VIDEO_ID = "videoId";
    private static final String PARAMETERS_ACTION_TYPE = "actionType";
    private static final String PARAMETERS_REPLY_COMMENTID = "replyCommentId";
    private static final String PARAMETERS_CONTENT = "content";
    private static final String PARAMETERS_AUDIT_REJECT_REASON = "reason";

    @Around("@annotation(com.sirinsirin.annotation.RecordUserMessage)")
    public ResponseVO interceptorDo(ProceedingJoinPoint point) throws Throwable {
        ResponseVO responseVO = (ResponseVO) point.proceed();

        Method method = ((MethodSignature) point.getSignature()).getMethod();
        RecordUserMessage recordUserMessage = method.getAnnotation(RecordUserMessage.class);
        if (recordUserMessage != null) {
            saveMessage(recordUserMessage, point.getArgs(), method.getParameters());
        }
        return responseVO;
    }

    /**
     * @param recordUserMessage 注解实例，包含消息类型
     * @param arguments 该数组存放了参数值
     * @param parameters 该数组存放了参数名
     */
    //  每种类型的消息其对应的方法的参数各不相同，因此采用这种方式
    private void saveMessage(RecordUserMessage recordUserMessage, Object[] arguments, Parameter[] parameters) {
        String videoId = null;
        Integer actionType = null;
        Integer replyCommentId = null;
        String content = null;
        for (int i = 0; i < parameters.length; i++) {
            if (PARAMETERS_VIDEO_ID.equals(parameters[i].getName())) {
                videoId = (String) arguments[i];
            } else if (PARAMETERS_ACTION_TYPE.equals(parameters[i].getName())) {
                actionType = (Integer) arguments[i];
            } else if (PARAMETERS_REPLY_COMMENTID.equals(parameters[i].getName())) {
                replyCommentId = (Integer) arguments[i];
            } else if (PARAMETERS_CONTENT.equals(parameters[i].getName())) {
                content = (String) arguments[i];
            } else if (PARAMETERS_AUDIT_REJECT_REASON.equals(parameters[i].getName())) {
                content = (String) arguments[i];
            }
        }

        MessageTypeEnum messageTypeEnum = recordUserMessage.messageType();
        if (UserActionTypeEnum.VIDEO_COLLECT.getType().equals(actionType)) {
            messageTypeEnum = MessageTypeEnum.COLLECTION;
        }

        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
        userMessageService.saveUserMessage(
                videoId, tokenUserInfoDto == null ? null : tokenUserInfoDto.getUserId(), messageTypeEnum, content, replyCommentId
        );
    }

    private TokenUserInfoDto getTokenUserInfoDto() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String token = request.getHeader(Constants.TOKEN_WEB);
        return redisComponent.getTokenInfo(token);
    }
}