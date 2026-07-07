package com.sirinsirin.controller;

import com.sirinsirin.component.RedisComponent;
import com.sirinsirin.entity.vo.ResponseVO;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.constraints.NotEmpty;

@RestController
@RequestMapping("/online")
@Validated
public class OnlineController extends ABaseController {

    @Resource
    private RedisComponent redisComponent;

    @RequestMapping("/reportVideoPlayOnline")
    public ResponseVO reportVideoPlayOnline(@NotEmpty String fileId, @NotEmpty String deviceId) {
        return getSuccessResponseVO(redisComponent.reportVideoPlayOnline(fileId, deviceId));
    }
}
