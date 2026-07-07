package com.sirinsirin.controller;

import com.sirinsirin.entity.po.CategoryInfo;
import com.sirinsirin.entity.query.CategoryInfoQuery;
import com.sirinsirin.entity.vo.ResponseVO;
import com.sirinsirin.service.CategoryInfoService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/category")
public class CategoryController extends ABaseController{

    @Resource
    private CategoryInfoService categoryInfoService;

    @RequestMapping("/loadCategory")
    public ResponseVO loadCategory(CategoryInfoQuery query){
        query.setOrderBy("sort asc");
        query.setConvert2Tree(true);
        List<CategoryInfo> categoryInfoList = categoryInfoService.findListByParam(query);
        return getSuccessResponseVO(categoryInfoList);
    }

    @RequestMapping("/saveCategory")
    public ResponseVO saveCategory(@NotNull Integer pCategoryId,
                                   Integer categoryId,
                                   @NotEmpty String categoryCode,
                                   @NotEmpty String categoryName,
                                   String icon,
                                   String background) throws IOException {
        CategoryInfo categoryInfo = new CategoryInfo();
        categoryInfo.setCategoryId(categoryId);
        categoryInfo.setpCategoryId(pCategoryId);
        categoryInfo.setCategoryCode(categoryCode);
        categoryInfo.setCategoryName(categoryName);
        categoryInfo.setIcon(icon);
        categoryInfo.setBackground(background);

        categoryInfoService.saveCategory(categoryInfo);
        if(categoryId == null)
            return getSuccessResponseVO("新增分类成功！");
        else
            return getSuccessResponseVO("修改分类成功！");

    }

    @RequestMapping("/delCategory")
    public ResponseVO delCategory(@NotNull Integer categoryId){

        categoryInfoService.delCategory(categoryId);
        return getSuccessResponseVO("删除分类成功！");
    }

    @RequestMapping("/changeSort")
    public ResponseVO changeSort(@NotNull Integer pCategoryId,
                                 @NotEmpty String categoryIds){
        categoryInfoService.changeSort(pCategoryId, categoryIds);
        return getSuccessResponseVO("删除分类成功！");
    }
}
