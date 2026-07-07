package com.sirinsirin.api.provider;

import com.sirinsirin.entity.constants.Constants;
import com.sirinsirin.entity.po.UserAction;
import com.sirinsirin.entity.query.UserActionQuery;
import com.sirinsirin.service.UserActionService;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping(Constants.INNER_API_PREFIX + "/userAction")
public class UserActionApi {

    @Resource
    private UserActionService userActionService;

    @RequestMapping("/getUserActionList")
    public List<UserAction> getUserActionList(@RequestBody UserActionQuery userActionQuery) {
        return userActionService.findListByParam(userActionQuery);
    }
}