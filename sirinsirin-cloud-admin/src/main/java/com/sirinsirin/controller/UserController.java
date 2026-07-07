package com.sirinsirin.controller;

import com.sirinsirin.api.consumer.WebClient;
import com.sirinsirin.component.RedisComponent;
import com.sirinsirin.entity.dto.SysSettingDto;
import com.sirinsirin.entity.query.UserInfoQuery;
import com.sirinsirin.entity.vo.ResponseVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/user")
@Validated
@Slf4j
public class UserController extends ABaseController {

    @Resource
    private WebClient webClient;

    @RequestMapping("/loadUser")
    public ResponseVO loadUser(UserInfoQuery userInfoQuery) {
        return getSuccessResponseVO(webClient.loadUser(userInfoQuery));
    }

    @RequestMapping("/changeStatus")
    public ResponseVO changeStatus(String userId, Integer status) {
        webClient.changeStatus(userId, status);
        return getSuccessResponseVO("调用changeStatus接口成功！");
    }
}