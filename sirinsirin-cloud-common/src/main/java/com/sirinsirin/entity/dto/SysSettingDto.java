package com.sirinsirin.entity.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SysSettingDto implements Serializable {
    private static final long serialVersionUID = -90520876542443938L;
    private Integer registerCoinCount = 10; //  注册赠送的积分数
    private Integer postVideoCoinCount = 5; //  发布视频成功后赠送的积分数
    private Integer videoSize = 100;  //  文件大小，单位是mb
    private Integer videoPCount = 5;   //  文件的最多分p数
    private Integer videoCount = 10;    //  文件数
    private Integer commentCount = 20;  //  评论数
    private Integer danmuCount = 20;    //  弹幕数

    public Integer getRegisterCoinCount() {
        return registerCoinCount;
    }

    public void setRegisterCoinCount(Integer registerCoinCount) {
        this.registerCoinCount = registerCoinCount;
    }

    public Integer getPostVideoCoinCount() {
        return postVideoCoinCount;
    }

    public void setPostVideoCoinCount(Integer postVideoCoinCount) {
        this.postVideoCoinCount = postVideoCoinCount;
    }

    public Integer getVideoSize() {
        return videoSize;
    }

    public void setVideoSize(Integer videoSize) {
        this.videoSize = videoSize;
    }

    public Integer getVideoPCount() {
        return videoPCount;
    }

    public void setVideoPCount(Integer videoPCount) {
        this.videoPCount = videoPCount;
    }

    public Integer getVideoCount() {
        return videoCount;
    }

    public void setVideoCount(Integer videoCount) {
        this.videoCount = videoCount;
    }

    public Integer getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(Integer commentCount) {
        this.commentCount = commentCount;
    }

    public Integer getDanmuCount() {
        return danmuCount;
    }

    public void setDanmuCount(Integer danmuCount) {
        this.danmuCount = danmuCount;
    }
}
