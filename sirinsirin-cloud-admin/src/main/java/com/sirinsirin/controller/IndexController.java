package com.sirinsirin.controller;

import com.sirinsirin.api.consumer.WebClient;
import com.sirinsirin.entity.vo.ResponseVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/index")
@Slf4j
@Validated
public class IndexController extends ABaseController{

    @Resource
    private WebClient webClient;

    @RequestMapping("/getActualTimeStatisticsInfo")
    public ResponseVO getActualTimeStatisticsInfo() {
        return getSuccessResponseVO(webClient.getActualTimeStatisticsInfo());
    }

    @RequestMapping("/getWeekStatisticsInfo")
    public ResponseVO getWeekStatisticsInfo(Integer dataType) {
        return getSuccessResponseVO(webClient.getWeekStatisticsInfo(dataType));
    }
}
