package com.sirinsirin.component;

import com.sirinsirin.api.consumer.VideoClient;
import com.sirinsirin.entity.config.AppConfig;
import com.sirinsirin.entity.constants.Constants;
import com.sirinsirin.entity.dto.UploadingFileDto;
import com.sirinsirin.entity.enums.VideoFileTransferResultEnum;
import com.sirinsirin.entity.enums.VideoStatusEnum;
import com.sirinsirin.entity.po.VideoInfoFilePost;
import com.sirinsirin.entity.po.VideoInfoPost;
import com.sirinsirin.entity.query.VideoInfoFilePostQuery;
import com.sirinsirin.exception.BusinessException;
import com.sirinsirin.utils.FFmpegUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.io.RandomAccessFile;

@Slf4j
@Component
public class TransferFileComponent {
    @Resource
    private RedisComponent redisComponent;

    @Resource
    private AppConfig appConfig;

    @Resource
    private FFmpegUtils fFmpegUtils;

    @Resource
    private VideoClient videoClient;

    //  前端发送来的视频分片并不能直接播放，需要先合成为mp4文件，再将其拆分成多个完整的、可播放的ts片段
    public void transferVideoFile(VideoInfoFilePost videoInfoFilePost) {
        VideoInfoFilePost updateFilePost = new VideoInfoFilePost();
        try{
            UploadingFileDto fileDto = redisComponent.getUploadVideoFile(videoInfoFilePost.getUserId(), videoInfoFilePost.getUploadId());

            // ============== 修复代码：Redis数据为空直接返回 ==============
            if (fileDto == null) {
                log.error("转码失败：Redis临时文件不存在，uploadId:{}", videoInfoFilePost.getUploadId());
                return;
            }

            String tempFilePath = appConfig.getProjectFolder() + Constants.FILE_FOLDER + Constants.FILE_FOLDER_TEMP + fileDto.getFilePath();
            File tempFile = new File(tempFilePath);
            String targetFilePath = appConfig.getProjectFolder() + Constants.FILE_FOLDER + Constants.FILE_VIDEO + fileDto.getFilePath();
            File targetFile = new File(targetFilePath);
            if(!targetFile.exists()){
                targetFile.mkdirs();
            }
            FileUtils.copyDirectory(tempFile, targetFile);

            //	删除临时目录
            FileUtils.forceDelete(tempFile);
            //	删除redis的相应记录
            redisComponent.delVideoFileInfo(videoInfoFilePost.getUserId(), videoInfoFilePost.getUploadId());
            // 合并分片，合并后为mp4类型的文件
            String completeVideo = targetFilePath + Constants.TEMP_VIDEO_NAME;
            this.union(targetFilePath, completeVideo, true);
            //	获取播放时长
            Integer duration = fFmpegUtils.getVideoInfoDuration(completeVideo);
            updateFilePost.setDuration(duration);
            updateFilePost.setFileSize(new File(completeVideo).length());
            updateFilePost.setFilePath(Constants.FILE_VIDEO + fileDto.getFilePath());
            updateFilePost.setTransferResult(VideoFileTransferResultEnum.SUCCESS.getStatus());
            //	将mp4转为ts文件
            this.convertVideo2Ts(completeVideo);
        }catch(Exception e){
            log.error("文件转码失败",e);
            updateFilePost.setTransferResult(VideoFileTransferResultEnum.FAIL.getStatus());
        }finally {
            videoClient.transferVideoFile4Db(
                    videoInfoFilePost.getVideoId(),
                    videoInfoFilePost.getUploadId(),
                    videoInfoFilePost.getUserId(),
                    updateFilePost
                    );
        }
    }

    private void convertVideo2Ts(String completeVideo){
        File videoFile = new File(completeVideo);
        File tsFolder = videoFile.getParentFile();
        String codec = fFmpegUtils.getVideoCodec(completeVideo);
        //	如果该视频为hevc编码的文件 -> h264编码（.mp4格式）
//		if(codec.equals(Constants.VIDEO_CODE_HEVC)){
        if(!codec.equals(Constants.VIDEO_CODE_HEVC)){	//	原本的代码为上面的代码，若上传的视频不为h264编码，则强转为h264编码
            //	ffmpeg在对视频转码时无法直接进行覆盖，所以采用复制出新的，再删除旧文件
            String tempFileName = completeVideo + Constants.VIDEO_CODE_TEMP_FILE_SUFFIX;
            new File(completeVideo).renameTo(new File(tempFileName));
            fFmpegUtils.convertHevc2Mp4(tempFileName, completeVideo);
            new File(tempFileName).delete();
        }

        fFmpegUtils.convertVideo2Ts(tsFolder, completeVideo);

        videoFile.delete();
    }

    // 合并分片
    private void union(String dirPath, String toFilePath, Boolean delSource){
        File dir = new File(dirPath);
        if(!dir.exists()){
            throw new BusinessException("目录不存在");
        }
        File fileList[] = dir.listFiles();
        File targetFile = new File(toFilePath);
        try(RandomAccessFile writeFile = new RandomAccessFile(targetFile, "rw")){
            byte[] b = new byte[1024 * 10];
            for(int i = 0; i < fileList.length; i++){
                int len = -1;
                //	创建读块文件的对象
                File chunkFile = new File(dirPath + File.separator + i);
                RandomAccessFile readFile = null;
                try{
                    readFile = new RandomAccessFile(chunkFile, "r");
                    while((len = readFile.read(b)) != -1){
                        writeFile.write(b, 0, len);
                    }
                }catch(Exception e){
                    log.error("合并分片失败", e);
                    throw new BusinessException("合并文件失败");
                }finally {
                    readFile.close();
                }
            }
        }catch(Exception e){
            throw new BusinessException("合并文件" + dirPath + "出错了");
        }finally {
            if(delSource){
                for(int i = 0; i < fileList.length; i++){
                    fileList[i].delete();
                }
            }
        }
    }
}
