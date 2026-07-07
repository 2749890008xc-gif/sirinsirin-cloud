package com.sirinsirin.controller;

import com.sirinsirin.api.consumer.InteractClient;
import com.sirinsirin.entity.vo.PaginationResultVO;
import com.sirinsirin.entity.vo.ResponseVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;

@RestController
@RequestMapping("/interact")
@Validated
@Slf4j
public class InteractController extends ABaseController{
    @Resource
    private InteractClient interactClient;

    @RequestMapping("/loadComment")
    public ResponseVO loadComment(Integer pageNo, String videoNameFuzzy) {
        return getSuccessResponseVO(interactClient.loadComment(pageNo, videoNameFuzzy));
    }

    @RequestMapping("/delComment")
    public ResponseVO delComment(@NotNull Integer commentId) {
        interactClient.delComment(commentId);
        return getSuccessResponseVO("调用delComment接口成功！");
    }

    @RequestMapping("/loadDanmu")
    public ResponseVO loadDanmu(Integer pageNo, String videoNameFuzzy) {
        return getSuccessResponseVO(interactClient.loadDanmu(pageNo, videoNameFuzzy));
    }

    @RequestMapping("/delDanmu")
    public ResponseVO delDanmu(@NotNull Integer danmuId) {
        interactClient.delDanmu(danmuId);
        return getSuccessResponseVO("调用delDanmu接口成功！");
    }
}
