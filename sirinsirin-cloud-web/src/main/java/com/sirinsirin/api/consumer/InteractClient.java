package com.sirinsirin.api.consumer;


import com.sirinsirin.entity.constants.Constants;
import com.sirinsirin.entity.po.UserAction;
import com.sirinsirin.entity.query.UserActionQuery;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = Constants.SERVER_NAME_INTERACT)
public interface InteractClient {
    @RequestMapping(Constants.INNER_API_PREFIX + "/userAction/getUserActionList")
    List<UserAction> getUserActionList(@RequestBody UserActionQuery actionQuery);

    @RequestMapping(Constants.INNER_API_PREFIX + "/comment/delCommentByVideoId")
    void delCommentByVideoId(@RequestParam String videoId);

    @RequestMapping(Constants.INNER_API_PREFIX + "/danmu/delDanmuByVideoId")
    void delDanmuByVideoId(@RequestParam String videoId);
}