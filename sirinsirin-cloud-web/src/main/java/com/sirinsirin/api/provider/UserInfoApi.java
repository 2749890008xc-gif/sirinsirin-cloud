package com.sirinsirin.api.provider;

import com.sirinsirin.entity.constants.Constants;
import com.sirinsirin.entity.po.UserInfo;
import com.sirinsirin.entity.query.UserInfoQuery;
import com.sirinsirin.entity.vo.PaginationResultVO;
import com.sirinsirin.service.UserInfoService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@RestController
@RequestMapping(Constants.INNER_API_PREFIX + "/user")
@Validated
public class UserInfoApi {

    @Resource
    private UserInfoService userInfoService;

    @RequestMapping("/updateCoinCountInfo")
    public Integer updateCoinCountInfo(@NotEmpty String userId, @NotNull Integer count) {
        return userInfoService.updateCoinCountInfo(userId, count);
    }

    @RequestMapping("/getUserInfoByUserId")
    public UserInfo getUserInfoByUserId(@NotEmpty String userId) {
        return userInfoService.getUserInfoByUserId(userId);
    }

    @RequestMapping("/loadUser")
    public PaginationResultVO loadUser(UserInfoQuery userInfoQuery) {
        userInfoQuery.setOrderBy("join_time desc");
        return userInfoService.findListByPage(userInfoQuery);
    }

    @RequestMapping("/changeStatus")
    public void changeStatus(String userId, Integer status) {
        userInfoService.changeUserStatus(userId, status);
    }
}
