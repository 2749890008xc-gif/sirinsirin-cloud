package com.sirinsirin.controller;

import com.sirinsirin.api.consumer.CategoryClient;
import com.sirinsirin.entity.vo.ResponseVO;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/category")
public class CategoryController extends ABaseController{

    @Resource
    private CategoryClient categoryClient;

    @RequestMapping("/loadAllCategory")
    public ResponseVO loadAllCategory(){
        return getSuccessResponseVO(categoryClient.loadAllCategory());
    }
}
