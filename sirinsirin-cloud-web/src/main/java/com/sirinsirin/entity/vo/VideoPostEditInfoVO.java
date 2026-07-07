package com.sirinsirin.entity.vo;

import com.sirinsirin.entity.po.VideoInfoFilePost;
import com.sirinsirin.entity.po.VideoInfoPost;

import java.util.List;

public class VideoPostEditInfoVO {
    private VideoInfoPost videoInfo;
    private List<VideoInfoFilePost> videoInfoFileList;

    public VideoInfoPost getVideoInfo() {
        return videoInfo;
    }

    public void setVideoInfo(VideoInfoPost videoInfo) {
        this.videoInfo = videoInfo;
    }

    public List<VideoInfoFilePost> getVideoInfoFileList() {
        return videoInfoFileList;
    }

    public void setVideoInfoFileList(List<VideoInfoFilePost> videoInfoFileList) {
        this.videoInfoFileList = videoInfoFileList;
    }
}
