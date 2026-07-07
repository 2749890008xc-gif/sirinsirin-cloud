package com.sirinsirin.api.provider;

import com.sirinsirin.annotation.GlobalInterceptor;
import com.sirinsirin.controller.FileController;
import com.sirinsirin.entity.constants.Constants;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.IOException;

@RestController
@RequestMapping(Constants.INNER_API_PREFIX + "/file")
public class ResourceApi {
    @Resource
    private FileController fileController;

    @RequestMapping("/uploadImage")
    @GlobalInterceptor(checkLogin = true)
    public String uploadImage(@NotNull MultipartFile file, @NotNull Boolean createThumbnail) throws IOException {
        return fileController.uploadImageInner(file, createThumbnail);
    }

    @RequestMapping("/getResource")
    @GlobalInterceptor(checkLogin = true)
    public void getResource(HttpServletResponse response, @NotEmpty String sourceName) {
        fileController.getResource(response, sourceName);
    }

    @RequestMapping("/videoResource/{fileId}")
    public void videoResource(HttpServletResponse response, @PathVariable @NotEmpty String fileId) {
        fileController.getVideoResource(response, fileId);
    }

    @RequestMapping("/videoResource/{fileId}/{ts}")
    public void videoResourceTs(HttpServletResponse response, @PathVariable @NotEmpty String fileId, @PathVariable @NotEmpty String ts) {
        fileController.getVideoResourceTs(response, fileId, ts);
    }
}
