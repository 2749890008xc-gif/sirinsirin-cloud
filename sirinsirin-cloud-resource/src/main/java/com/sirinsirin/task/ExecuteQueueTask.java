package com.sirinsirin.task;

import com.sirinsirin.component.RedisComponent;
import com.sirinsirin.component.TransferFileComponent;
import com.sirinsirin.entity.constants.Constants;
import com.sirinsirin.entity.dto.UploadingFileDto;
import com.sirinsirin.entity.enums.VideoFileTransferResultEnum;
import com.sirinsirin.entity.enums.VideoStatusEnum;
import com.sirinsirin.entity.po.VideoInfoFilePost;
import com.sirinsirin.entity.po.VideoInfoPost;
import com.sirinsirin.entity.query.VideoInfoFilePostQuery;
import com.sirinsirin.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.File;
import java.io.RandomAccessFile;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@Slf4j
public class ExecuteQueueTask {
    private ExecutorService executorService = Executors.newFixedThreadPool(Constants.LENGTH_2);

    @Resource
    private RedisComponent redisComponent;

    @Resource
    private TransferFileComponent transferFileComponent;

    @PostConstruct
    public void consumTransferFileQueue(){
        executorService.execute(() -> {
            while(true){
                try {
                    VideoInfoFilePost videoInfoFile = redisComponent.getFileFromTransferQueue();
                    if(videoInfoFile == null){
                        Thread.sleep(1500);
                        continue;
                    }
                    transferFileComponent.transferVideoFile(videoInfoFile);
                }catch(Exception e){
                    log.error("获取转码文件队列信息失败", e);
                }
            }
        });
    }
}
