package com.sirinsirin.task;

import com.sirinsirin.component.EsSearchComponent;
import com.sirinsirin.component.RedisComponent;
import com.sirinsirin.entity.constants.Constants;
import com.sirinsirin.entity.dto.VideoPlayInfoDto;
import com.sirinsirin.entity.enums.SearchOrderTypeEnum;
import com.sirinsirin.entity.po.VideoInfoFilePost;
import com.sirinsirin.service.VideoInfoPostService;
import com.sirinsirin.service.VideoInfoService;
import com.sirinsirin.service.VideoPlayHistoryService;
import com.sirinsirin.utils.StringTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@Slf4j
public class ExecuteQueueTask {
    private ExecutorService executorService = Executors.newFixedThreadPool(Constants.LENGTH_2);

    @Resource
    private RedisComponent redisComponent;

    @Resource
    private VideoInfoService videoInfoService;

    @Resource
    private EsSearchComponent esSearchComponent;

    @Resource
    private VideoPlayHistoryService videoPlayHistoryService;

    @PostConstruct
    public void consumVideoPlayQueue() {
        executorService.execute(() -> {
            while (true) {
                try {
                    VideoPlayInfoDto videoPlayInfoDto = redisComponent.getVideoPlayFromVideoPlayQueue();
                    if (videoPlayInfoDto == null) {
                        Thread.sleep(1500);
                        continue;
                    }
                    //  更新播放数
                    videoInfoService.addReadCount(videoPlayInfoDto.getVideoId());

                    if(!StringTools.isEmpty(videoPlayInfoDto.getUserId())){
                        videoPlayHistoryService.saveHistory(videoPlayInfoDto.getUserId(), videoPlayInfoDto.getVideoId(), videoPlayInfoDto.getFileIndex());
                    }
                    //  按天记录视频播放量
                    redisComponent.recordVideoPlayCount(videoPlayInfoDto.getVideoId());

                    //  更新es播放数量
                    esSearchComponent.updateDocCount(videoPlayInfoDto.getVideoId(), SearchOrderTypeEnum.VIDEO_PLAY.getField(), 1);
                } catch (Exception e) {
                    log.error("获取视频播放文件队列消息失败", e);
                }
            }
        });
    }

}
