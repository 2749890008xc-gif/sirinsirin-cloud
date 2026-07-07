package com.sirinsirin.api.consumer;

import com.sirinsirin.entity.constants.Constants;
import com.sirinsirin.entity.po.VideoInfoFile;
import com.sirinsirin.entity.po.VideoInfoFilePost;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = Constants.SERVER_NAME_WEB)
public interface VideoClient {

    @RequestMapping(Constants.INNER_API_PREFIX + "/video/getVideoInfoFileByFileId")
    //  通过openFeign实现参数传递时，参数前面必须加上 @RequestParam 注解
    VideoInfoFile getVideoInfoFileByFileId(@RequestParam String fileId);

    @RequestMapping(Constants.INNER_API_PREFIX + "/video/transferVideoFile4Db")
    VideoInfoFile transferVideoFile4Db(
            @RequestParam String videoId,
            @RequestParam String uploadId,
            @RequestParam String userId,
            @RequestBody VideoInfoFilePost uploadFilePost
    );
}
