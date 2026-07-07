package com.sirinsirin.api.consumer;

import com.sirinsirin.entity.constants.Constants;
import com.sirinsirin.entity.vo.PaginationResultVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = Constants.SERVER_NAME_INTERACT)
public interface InteractClient {

    @RequestMapping(Constants.INNER_API_PREFIX + "/comment/admin/loadComment")
    PaginationResultVO loadComment(@RequestParam Integer pageNo, @RequestParam String videoNameFuzzy);

    @RequestMapping(Constants.INNER_API_PREFIX + "/comment/admin/delComment")
    void delComment(@RequestParam Integer commentId);

    @RequestMapping(Constants.INNER_API_PREFIX + "/danmu/admin/loadDanmu")
    PaginationResultVO loadDanmu(@RequestParam Integer pageNo, @RequestParam String videoNameFuzzy);

    @RequestMapping(Constants.INNER_API_PREFIX + "/danmu/admin/delDanmu")
    void delDanmu(@RequestParam Integer danmuId);
}
