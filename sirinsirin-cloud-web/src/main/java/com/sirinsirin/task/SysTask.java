package com.sirinsirin.task;

import com.sirinsirin.service.StatisticsInfoService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class SysTask {

    @Resource
    private StatisticsInfoService statisticsInfoService;

    @Scheduled(cron = " 0 0 0 * * ?")
    public void statisticsData(){
        statisticsInfoService.statisticsData();
    }
}
