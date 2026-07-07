package com.sirinsirin.api.consumer;

import com.sirinsirin.entity.constants.Constants;
import com.sirinsirin.entity.po.StatisticsInfo;
import com.sirinsirin.entity.po.VideoInfoFilePost;
import com.sirinsirin.entity.query.UserInfoQuery;
import com.sirinsirin.entity.query.VideoInfoPostQuery;
import com.sirinsirin.entity.query.VideoInfoQuery;
import com.sirinsirin.entity.vo.PaginationResultVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name = Constants.SERVER_NAME_WEB)
public interface WebClient {

    //  数据统计
    @RequestMapping(Constants.INNER_API_PREFIX + "/statistics/admin/getActualTimeStatisticsInfo")
    Map getActualTimeStatisticsInfo();

    @RequestMapping(Constants.INNER_API_PREFIX + "/statistics/admin/getWeekStatisticsInfo")
    List<StatisticsInfo> getWeekStatisticsInfo(@RequestParam Integer dataType);

    //  视频相关
    @RequestMapping(Constants.INNER_API_PREFIX + "/video/admin/loadVideoList")
    PaginationResultVO loadVideoList(@RequestBody VideoInfoPostQuery videoInfoPostQuery);

    @RequestMapping(Constants.INNER_API_PREFIX + "/video/admin/auditVideo")
    void auditVideo(@RequestParam String videoId, @RequestParam Integer status, @RequestParam String reason);

    @RequestMapping(Constants.INNER_API_PREFIX + "/video/admin/recommendVideo")
    void recommendVideo(@RequestParam String videoId);

    @RequestMapping(Constants.INNER_API_PREFIX + "/video/admin/deleteVideo")
    void deleteVideo(@RequestParam String videoId);

    @RequestMapping(Constants.INNER_API_PREFIX + "/video/admin/loadVideoPList")
    List<VideoInfoFilePost> loadVideoPList(@RequestParam String videoId);

    @RequestMapping(Constants.INNER_API_PREFIX + "/user/loadUser")
    PaginationResultVO loadUser(@RequestBody UserInfoQuery userInfoQuery);

    @RequestMapping(Constants.INNER_API_PREFIX + "/user/changeStatus")
    void changeStatus(@RequestParam String userId, @RequestParam Integer status);

    @RequestMapping(Constants.INNER_API_PREFIX + "/video/getVideoCount")
    Integer getVideoCount(@RequestBody VideoInfoQuery videoInfoQuery);
}
