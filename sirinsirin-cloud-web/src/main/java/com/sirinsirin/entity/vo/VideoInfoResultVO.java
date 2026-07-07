package com.sirinsirin.entity.vo;

import com.sirinsirin.entity.po.VideoInfo;

import java.util.List;

public class VideoInfoResultVO {
    private VideoInfo videoInfo;
    private List userActionList;

    public VideoInfoResultVO() {}

    public VideoInfoResultVO(VideoInfo videoInfo, List userActionList) {
        this.userActionList = userActionList;
        this.videoInfo = videoInfo;
    }

    public VideoInfo getVideoInfo() {
        return videoInfo;
    }

    public void setVideoInfo(VideoInfo videoInfo) {
        this.videoInfo = videoInfo;
    }

    public List getUserActionList() {
        return userActionList;
    }

    public void setUserActionList(List userActionList) {
        this.userActionList = userActionList;
    }
}
