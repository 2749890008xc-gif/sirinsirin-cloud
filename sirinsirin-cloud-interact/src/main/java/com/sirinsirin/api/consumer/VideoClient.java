package com.sirinsirin.api.consumer;

import com.sirinsirin.entity.constants.Constants;
import com.sirinsirin.entity.enums.SearchOrderTypeEnum;
import com.sirinsirin.entity.po.UserInfo;
import com.sirinsirin.entity.po.VideoInfo;
import com.sirinsirin.entity.po.VideoInfoFile;
import com.sirinsirin.entity.po.VideoInfoPost;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = Constants.SERVER_NAME_WEB)
public interface VideoClient {

    @RequestMapping(Constants.INNER_API_PREFIX + "/user/updateCoinCountInfo")
    //  通过openFeign实现参数传递时，参数前面必须加上 @RequestParam 注解
    Integer updateCoinCountInfo(@RequestParam String userId, @RequestParam Integer count);

    @RequestMapping(Constants.INNER_API_PREFIX + "/user/getUserInfoByUserId")
    UserInfo getUserInfoByUserId(@RequestParam String userId);

    @RequestMapping(Constants.INNER_API_PREFIX + "/video/getVideoInfoByVideoId")
    VideoInfo getVideoInfoByVideoId(@RequestParam String videoId);

    @RequestMapping(Constants.INNER_API_PREFIX + "/video/updateCountInfo")
    void updateCountInfo(@RequestParam String videoId, @RequestParam String fileId, @RequestParam Integer changeCount);

    @RequestMapping(Constants.INNER_API_PREFIX + "/video/getVideoInfoPostByVideoId")
    VideoInfoPost getVideoInfoPostByVideoId(@RequestParam String videoId);

    @RequestMapping(Constants.INNER_API_PREFIX + "/video/updateDocCount")
    void updateDocCount(@RequestParam String videoId, @RequestParam SearchOrderTypeEnum searchOrderTypeEnum, @RequestParam Integer changeCount);
}
