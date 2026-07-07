package com.sirinsirin.service.impl;

import com.sirinsirin.api.consumer.InteractClient;
import com.sirinsirin.component.EsSearchComponent;
import com.sirinsirin.component.RedisComponent;
import com.sirinsirin.entity.config.AppConfig;
import com.sirinsirin.entity.dto.SysSettingDto;
import com.sirinsirin.entity.enums.PageSize;
import com.sirinsirin.entity.enums.ResponseCodeEnum;
import com.sirinsirin.entity.enums.UserActionTypeEnum;
import com.sirinsirin.entity.enums.VideoRecommendTypeEnum;
import com.sirinsirin.entity.po.*;
import com.sirinsirin.entity.query.*;
import com.sirinsirin.entity.vo.PaginationResultVO;
import com.sirinsirin.exception.BusinessException;
import com.sirinsirin.mappers.*;
import com.sirinsirin.service.VideoInfoService;
import com.sirinsirin.utils.StringTools;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * 视频信息 业务接口实现
 */
@Service("videoInfoService")
@Slf4j
public class VideoInfoServiceImpl implements VideoInfoService {
	//	创建异步线程池
	private static ExecutorService executorService = Executors.newFixedThreadPool(10);

	@Resource
	private VideoInfoMapper<VideoInfo, VideoInfoQuery> videoInfoMapper;

	@Resource
	private VideoInfoPostMapper<VideoInfoPost, VideoInfoPostQuery> videoInfoPostMapper;
	@Resource
	private VideoInfoFileMapper<VideoInfoFile, VideoInfoFileQuery> videoInfoFileMapper;
	@Resource
	private VideoInfoFilePostMapper<VideoInfoFilePost, VideoInfoFilePostQuery> videoInfoFilePostMapper;
	@Resource
	private AppConfig appConfig;
    @Resource
    private RedisComponent redisComponent;
    @Resource
    private EsSearchComponent esSearchComponent;
	@Resource
	private InteractClient interactClient;

	@Resource
	private UserInfoMapper<UserInfo, UserInfoQuery> userInfoMapper;

	/**
	 * 根据条件查询列表
	 */
	@Override
	public List<VideoInfo> findListByParam(VideoInfoQuery param) {
		return this.videoInfoMapper.selectList(param);
	}

	/**
	 * 根据条件查询列表
	 */
	@Override
	public Integer findCountByParam(VideoInfoQuery param) {
		return this.videoInfoMapper.selectCount(param);
	}

	/**
	 * 分页查询方法
	 */
	@Override
	public PaginationResultVO<VideoInfo> findListByPage(VideoInfoQuery param) {
		int count = this.findCountByParam(param);
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
		param.setSimplePage(page);
		List<VideoInfo> list = this.findListByParam(param);
		PaginationResultVO<VideoInfo> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	@Override
	public Integer add(VideoInfo bean) {
		return this.videoInfoMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	@Override
	public Integer addBatch(List<VideoInfo> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.videoInfoMapper.insertBatch(listBean);
	}

	/**
	 * 批量新增或者修改
	 */
	@Override
	public Integer addOrUpdateBatch(List<VideoInfo> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.videoInfoMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 多条件更新
	 */
	@Override
	public Integer updateByParam(VideoInfo bean, VideoInfoQuery param) {
		StringTools.checkParam(param);
		return this.videoInfoMapper.updateByParam(bean, param);
	}

	/**
	 * 多条件删除
	 */
	@Override
	public Integer deleteByParam(VideoInfoQuery param) {
		StringTools.checkParam(param);
		return this.videoInfoMapper.deleteByParam(param);
	}

	/**
	 * 根据VideoId获取对象
	 */
	@Override
	public VideoInfo getVideoInfoByVideoId(String videoId) {
		return this.videoInfoMapper.selectByVideoId(videoId);
	}

	/**
	 * 根据VideoId修改
	 */
	@Override
	public Integer updateVideoInfoByVideoId(VideoInfo bean, String videoId) {
		return this.videoInfoMapper.updateByVideoId(bean, videoId);
	}

	/**
	 * 根据VideoId删除
	 */
	@Override
	public Integer deleteVideoInfoByVideoId(String videoId) {
		return this.videoInfoMapper.deleteByVideoId(videoId);
	}

	@Override
	@GlobalTransactional(rollbackFor = Exception.class)
	public void changeInteraction(String videoId, String userId, String interaction) {
		VideoInfo videoInfo = new VideoInfo();
		videoInfo.setInteraction(interaction);
		VideoInfoQuery videoInfoQuery = new VideoInfoQuery();
		videoInfoQuery.setVideoId(videoId);
		videoInfoQuery.setUserId(userId);
		videoInfoMapper.updateByParam(videoInfo, videoInfoQuery);

		VideoInfoPost videoInfoPost = new VideoInfoPost();
		videoInfoPost.setInteraction(interaction);
		VideoInfoPostQuery videoInfoPostQuery = new VideoInfoPostQuery();
		videoInfoPostQuery.setVideoId(videoId);
		videoInfoPostQuery.setUserId(userId);
		videoInfoPostMapper.updateByParam(videoInfoPost, videoInfoPostQuery);
	}

	@Override
	@GlobalTransactional(rollbackFor = Exception.class)
	public void deleteVideo(String videoId, String userId) {
		VideoInfoPost videoInfoPost = this.videoInfoPostMapper.selectByVideoId(videoId);

		//	如果管理员在后台删除视频时会调用该接口，此时userId为null，因此要加 userId != null 的判断
		if(videoInfoPost == null || userId != null && !userId.equals(videoInfoPost.getUserId())){
			throw new BusinessException(ResponseCodeEnum.CODE_404);
		}

		this.videoInfoMapper.deleteByVideoId(videoId);
		this.videoInfoPostMapper.deleteByVideoId(videoId);
		SysSettingDto sysSettingDto = redisComponent.getSysSettingDto();
		//	减去用户的硬币
		userInfoMapper.updateCoinCountInfo(videoInfoPost.getUserId(), -sysSettingDto.getPostVideoCoinCount());

		//	删除es数据
		esSearchComponent.delDoc(videoId);

		executorService.execute(() -> {
			VideoInfoFileQuery videoInfoFileQuery = new VideoInfoFileQuery();
			videoInfoFileQuery.setVideoId(videoId);

			//删除分P
			videoInfoFileMapper.deleteByParam(videoInfoFileQuery);

			VideoInfoFilePostQuery videoInfoFilePost = new VideoInfoFilePostQuery();
			videoInfoFilePost.setVideoId(videoId);
			videoInfoFilePostMapper.deleteByParam(videoInfoFilePost);

			//	删除评论
			interactClient.delCommentByVideoId(videoId);
			//	删除弹幕
			interactClient.delDanmuByVideoId(videoId);
			List<VideoInfoFile> videoInfoFileList = this.videoInfoFileMapper.selectList(videoInfoFileQuery);
			for (VideoInfoFile item : videoInfoFileList) {
				try {
					FileUtils.deleteDirectory(new File(appConfig.getProjectFolder() + item.getFilePath()));
				} catch (IOException e) {
					log.error("删除文件失败,文件路径:{}", item.getFilePath());
				}
			}
		});
	}

	@Override
	public void addReadCount(String videoId) {
		this.videoInfoMapper.updateCountInfo(videoId, UserActionTypeEnum.VIDEO_PLAY.getField(), 1);
	}

	@Override
	public void recommendVideo(String videoId) {
		VideoInfo videoInfo = videoInfoMapper.selectByVideoId(videoId);
		if (videoInfo == null) {
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		Integer recommendType = null;
		if (VideoRecommendTypeEnum.RECOMMEND.getType().equals(videoInfo.getRecommendType())) {
			recommendType = VideoRecommendTypeEnum.NO_RECOMMEND.getType();
		} else {
			recommendType = VideoRecommendTypeEnum.RECOMMEND.getType();
		}
		VideoInfo updateInfo = new VideoInfo();
		updateInfo.setRecommendType(recommendType);
		videoInfoMapper.updateByVideoId(updateInfo, videoId);
	}

	@Override
	public void updateCountInfo(String videoId, String fileId, Integer changeCount) {
		this.videoInfoMapper.updateCountInfo(videoId, fileId, changeCount);
	}
}