package com.sirinsirin.api.consumer;

import com.sirinsirin.entity.constants.Constants;
import com.sirinsirin.entity.po.CategoryInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@FeignClient(name = Constants.SERVER_NAME_ADMIN)
//  该接口没有实现，因此调用提供者的接口
public interface CategoryClient {
    @RequestMapping(Constants.INNER_API_PREFIX + "/loadAllCategory")
    List<CategoryInfo> loadAllCategory();
}
