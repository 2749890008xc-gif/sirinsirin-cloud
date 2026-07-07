package com.sirinsirin.controller;

import com.sirinsirin.api.consumer.VideoClient;
import com.sirinsirin.component.RedisComponent;
import com.sirinsirin.entity.config.AppConfig;
import com.sirinsirin.entity.constants.Constants;
import com.sirinsirin.entity.dto.SysSettingDto;
import com.sirinsirin.entity.dto.TokenUserInfoDto;
import com.sirinsirin.entity.dto.UploadingFileDto;
import com.sirinsirin.entity.dto.VideoPlayInfoDto;
import com.sirinsirin.entity.enums.DateTimePatternEnum;
import com.sirinsirin.entity.enums.ResponseCodeEnum;
import com.sirinsirin.entity.po.VideoInfoFile;
import com.sirinsirin.entity.vo.ResponseVO;
import com.sirinsirin.exception.BusinessException;
//import com.sirinsirin.service.impl.VideoInfoFileServiceImpl;
import com.sirinsirin.utils.DateUtil;
import com.sirinsirin.utils.FFmpegUtils;
import com.sirinsirin.utils.StringTools;
import com.sirinsirin.annotation.GlobalInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.*;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@RestController
@Validated
@Slf4j
public class FileController extends ABaseController{
    @Resource
    private AppConfig appConfig;

    @Resource
    private RedisComponent redisComponent;

    @Resource
    private FFmpegUtils ffmpegUtils;

    @Resource
    private VideoClient videoClient;

    @RequestMapping("/getResource")
    public void getResource(HttpServletResponse response, @NotNull String sourceName) {
        if(!StringTools.pathIsOk(sourceName)){
            throw new BusinessException(ResponseCodeEnum.CODE_600 + "，地址不合法！");
        }
        String suffix = StringTools.getFileSuffix(sourceName);
        response.setContentType("image/" + suffix.replace(".", ""));
        response.setHeader("Cache-Control", "max-age=864000"); //  单位是秒
        readFile(response, sourceName);
    }

    protected void readFile(HttpServletResponse response, String filePath){
        File file = new File(appConfig.getProjectFolder() + Constants.FILE_FOLDER + filePath);
        if(!file.exists()){
            return ;
        }
        try(OutputStream out = response.getOutputStream(); FileInputStream in = new FileInputStream(file)){
            byte[] byteData = new byte[1024];
            int len = 0;
            while((len = in.read(byteData)) != -1){
                out.write(byteData, 0, len);
            }
            out.flush();
        }catch(Exception e){
            log.error("读取文件异常", e);
        }
    }

    //  视频预上传接口
    @RequestMapping("/preUploadVideo")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO preUploadVideo(@NotEmpty String fileName, @NotNull Integer chunks){
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
        String uploadId = redisComponent.savePreVideoFileInfo(tokenUserInfoDto.getUserId(), fileName, chunks);
        return getSuccessResponseVO(uploadId);
    }

    //  视频上传接口
    @RequestMapping("/uploadVideo")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO uploadVideo(@NotNull MultipartFile chunkFile, @NotNull Integer chunkIndex, @NotEmpty String uploadId) throws IOException {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
        UploadingFileDto fileDto = redisComponent.getUploadVideoFile(tokenUserInfoDto.getUserId(), uploadId);
        if(fileDto == null){
            throw new BusinessException("文件不存在或已失效，请重新上传");
        }
        SysSettingDto sysSettingDto = redisComponent.getSysSettingDto();
        if(fileDto.getFileSize() > sysSettingDto.getVideoSize() * Constants.MB_SIZE){
            throw new BusinessException("文件超过大小限制，文件大小不能超过" + sysSettingDto.getVideoSize() + "MB");
        }

        //  判断分片，当传完某些分片后又上传了其中的某些分片，或者分片的索引号大于总分片数量
        if((chunkIndex - 1) > fileDto.getChunkIndex() || chunkIndex > fileDto.getChunks() - 1){
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }

        String folder = appConfig.getProjectFolder() + Constants.FILE_FOLDER + Constants.FILE_FOLDER_TEMP + fileDto.getFilePath();
        File targetFile = new File(folder + "/" + chunkIndex);
        chunkFile.transferTo(targetFile);
        fileDto.setChunkIndex(chunkIndex);
        fileDto.setFileSize(fileDto.getFileSize() + chunkFile.getSize());
        redisComponent.updateVideoFileInfo(tokenUserInfoDto.getUserId(), fileDto);
        return getSuccessResponseVO("视频上传成功");
    }

    //  删除预上传的视频
    @RequestMapping("/delUploadVideo")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO delUploadVideo(@NotEmpty String uploadId) throws IOException {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
        UploadingFileDto fileDto = redisComponent.getUploadVideoFile(tokenUserInfoDto.getUserId(), uploadId);
        if(fileDto == null){
            throw new BusinessException("文件不存在，请重新上传");
        }
        redisComponent.delVideoFileInfo(tokenUserInfoDto.getUserId(), uploadId);
        FileUtils.deleteDirectory(
                new File(appConfig.getProjectFolder() +
                Constants.FILE_FOLDER +
                        Constants.FILE_FOLDER_TEMP +
                        fileDto.getFilePath()));
        return getSuccessResponseVO(uploadId);
    }

    //  上传封面
    @RequestMapping("/uploadImage")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO uploadImage(@NotNull MultipartFile file, @NotNull Boolean createThumbnail) throws IOException {

        return getSuccessResponseVO(uploadImageInner(file, createThumbnail));
    }

    public String uploadImageInner(MultipartFile file, Boolean createThumbnail) throws IOException {
        // 允许的图片后缀
        final List<String> ALLOWED_IMAGE_SUFFIX = Arrays.asList(".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp");
        // 图片文件头魔数（二进制校验，绝对无法伪造）
        final List<String> ALLOWED_IMAGE_MAGIC = Arrays.asList(
                "FFD8FF",    // JPG, JPEG
                "89504E47",  // PNG
                "47494638",  // GIF
                "424D"       // BMP
        );

        // 1. 空文件校验
        if (file.isEmpty()) {
            throw new BusinessException("上传的图片文件不能为空");
        }

        String fileName = file.getOriginalFilename();
        String fileSuffix = StringTools.getFileSuffix(fileName).toLowerCase();

        // 2. 后缀校验
        if (!ALLOWED_IMAGE_SUFFIX.contains(fileSuffix)) {
            throw new BusinessException("仅支持上传图片文件：jpg、jpeg、png、gif、bmp");
        }

        // 3. 【核心】文件头魔数校验（拦截MP3/视频/文本等所有非图片文件）
        if (!checkImageMagicNumber(file.getInputStream(), ALLOWED_IMAGE_MAGIC)) {
            throw new BusinessException("非法文件！仅允许上传真实图片（禁止修改后缀冒充）");
        }

        String day = DateUtil.format(new Date(), DateTimePatternEnum.YYYYMMDD.getPattern());
        String folder = appConfig.getProjectFolder() + Constants.FILE_FOLDER + Constants.FILE_COVER + day;
        File folderFile = new File(folder);
        if (!folderFile.exists()) {
            folderFile.mkdirs();
        }
        String realFileName = StringTools.getRandomString(Constants.LENGTH_30) + fileSuffix;
        String filePath = folder + "/" + realFileName;
        file.transferTo(new File(filePath));
        if(createThumbnail != null && createThumbnail){
            ffmpegUtils.createImageThumbnail(filePath);
        }

        return Constants.FILE_COVER + day + "/" + realFileName;
    }

    //  校验图片文件二进制头（终极防护）
    private boolean checkImageMagicNumber(InputStream inputStream, List<String> allowedImage) throws IOException {
        byte[] bytes = new byte[4];
        inputStream.read(bytes, 0, 4);
        String magicNumber = bytesToHex(bytes).toUpperCase();

        // 匹配真实图片魔数
        for (String magic : allowedImage) {
            if (magicNumber.startsWith(magic)) {
                return true;
            }
        }
        return false;
    }

    // 字节转十六进制
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(b & 0xFF);
            if (hex.length() == 1) {
                sb.append("0");
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    @RequestMapping("/videoResource/{fileId}")
    public void getVideoResource(HttpServletResponse response, @PathVariable @NotEmpty String fileId){
        VideoInfoFile videoInfoFile = videoClient.getVideoInfoFileByFileId(fileId);
        String filePath = videoInfoFile.getFilePath();
        readFile(response, filePath + "/" + Constants.M3U8_NAME);

        VideoPlayInfoDto videoPlayInfoDto = new VideoPlayInfoDto();
        videoPlayInfoDto.setVideoId(videoInfoFile.getVideoId());
        videoPlayInfoDto.setFileIndex(videoInfoFile.getFileIndex());

        TokenUserInfoDto tokenUserInfoDto = getTokenInfoFromCookie();
        if (tokenUserInfoDto != null) {
            videoPlayInfoDto.setUserId(tokenUserInfoDto.getUserId());
        }
        redisComponent.addVideoPlay(videoPlayInfoDto);
    }

    @RequestMapping("/videoResource/{fileId}/{ts}")
    public void getVideoResourceTs(HttpServletResponse response, @PathVariable @NotEmpty String fileId, @PathVariable @NotEmpty String ts){
        VideoInfoFile videoInfoFile = videoClient.getVideoInfoFileByFileId(fileId);
        String filePath = videoInfoFile.getFilePath();
        readFile(response, filePath + "/" + ts);
    }
}
