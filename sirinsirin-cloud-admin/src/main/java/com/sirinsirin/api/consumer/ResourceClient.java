package com.sirinsirin.api.consumer;

import com.sirinsirin.entity.constants.Constants;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import feign.Response;

@FeignClient(name = Constants.SERVER_NAME_RESOURCE)
public interface ResourceClient {

    @RequestMapping(
            value = Constants.INNER_API_PREFIX + "/file/uploadImage",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    String uploadImage(@RequestPart MultipartFile file, @RequestParam Boolean createThumbnail);

    @RequestMapping(value = Constants.INNER_API_PREFIX + "/file/getResource")
    Response getResource(@RequestParam String sourceName);

    @RequestMapping(value = Constants.INNER_API_PREFIX + "/file/videoResource/{fileId}")
    Response videoResource(@PathVariable String fileId);

    @RequestMapping(value = Constants.INNER_API_PREFIX + "/file/videoResource/{fileId}/{ts}")
    Response getVideoResrouceTs(@PathVariable String fileId, @PathVariable String ts);
}
