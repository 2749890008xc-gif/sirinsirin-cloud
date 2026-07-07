package com.sirinsirin.controller;

import com.sirinsirin.api.consumer.VideoClient;
import com.sirinsirin.entity.constants.Constants;
import com.sirinsirin.entity.po.VideoDanmu;
import com.sirinsirin.entity.po.VideoInfo;
import com.sirinsirin.entity.query.VideoDanmuQuery;
import com.sirinsirin.entity.vo.ResponseVO;
import com.sirinsirin.service.VideoDanmuService;
import com.sirinsirin.annotation.GlobalInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.Date;

/**
 * 视频弹幕 Controller
 */
@RestController("videoDanmuController")
@RequestMapping("/danmu")
@Validated
@Slf4j
public class VideoDanmuController extends ABaseController{

	@Resource
	private VideoDanmuService videoDanmuService;

	@Resource
	private VideoClient videoClient;

	/**
	 * 发布弹幕
	 * */
	@RequestMapping("/postDanmu")
	@GlobalInterceptor(checkLogin = true)
	public ResponseVO postDanmu(@NotEmpty String fileId,
								@NotEmpty String videoId,
								@NotEmpty @Size(max = 200) String text,
								@NotNull Integer mode,
								@NotEmpty String color,
								@NotNull Integer time) {
		VideoDanmu videoDanmu = new VideoDanmu();
		videoDanmu.setVideoId(videoId);
		videoDanmu.setFileId(fileId);
		videoDanmu.setText(text);
		videoDanmu.setMode(mode);
		videoDanmu.setColor(color);
		videoDanmu.setTime(time);
		videoDanmu.setUserId(getTokenUserInfoDto().getUserId());
		videoDanmu.setPostTime(new Date());

		videoDanmuService.saveVideoDanmu(videoDanmu);
		return getSuccessResponseVO("访问postDanmu接口成功！");
	}

	@RequestMapping("/loadDanmu")
	public ResponseVO loadDanmu(@NotEmpty String fileId, @NotEmpty String videoId) {
		VideoInfo videoInfo = videoClient.getVideoInfoByVideoId(videoId);
		if(videoInfo.getInteraction() != null && videoInfo.getInteraction().contains(Constants.ONE.toString())){
			return getSuccessResponseVO(new ArrayList<>());
		}

		VideoDanmuQuery videoDanmuQuery = new VideoDanmuQuery();
		videoDanmuQuery.setVideoId(videoId);
		videoDanmuQuery.setFileId(fileId);
		videoDanmuQuery.setOrderBy("danmu_id asc");

		return getSuccessResponseVO(videoDanmuService.findListByParam(videoDanmuQuery));
	}
}