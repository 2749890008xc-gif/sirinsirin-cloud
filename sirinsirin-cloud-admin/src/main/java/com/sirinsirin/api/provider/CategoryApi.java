package com.sirinsirin.api.provider;

import com.sirinsirin.entity.constants.Constants;
import com.sirinsirin.entity.po.CategoryInfo;
import com.sirinsirin.service.CategoryInfoService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping(Constants.INNER_API_PREFIX)
public class CategoryApi {

    @Resource
    private CategoryInfoService categoryInfoService;


    @RequestMapping("/loadAllCategory")
    public List<CategoryInfo> loadAllCategory() {
        return categoryInfoService.getAllCategoryList();
    }
}