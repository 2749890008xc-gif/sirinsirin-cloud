package com.sirinsirin.service.impl;

import com.sirinsirin.entity.enums.PageSize;
import com.sirinsirin.entity.enums.ResponseCodeEnum;
import com.sirinsirin.entity.po.UserVideoSeries;
import com.sirinsirin.entity.po.UserVideoSeriesVideo;
import com.sirinsirin.entity.po.VideoInfo;
import com.sirinsirin.entity.query.SimplePage;
import com.sirinsirin.entity.query.UserVideoSeriesQuery;
import com.sirinsirin.entity.query.UserVideoSeriesVideoQuery;
import com.sirinsirin.entity.query.VideoInfoQuery;
import com.sirinsirin.entity.vo.PaginationResultVO;
import com.sirinsirin.exception.BusinessException;
import com.sirinsirin.mappers.UserVideoSeriesMapper;
import com.sirinsirin.mappers.UserVideoSeriesVideoMapper;
import com.sirinsirin.mappers.VideoInfoMapper;
import com.sirinsirin.service.UserVideoSeriesService;
import com.sirinsirin.utils.StringTools;
import io.seata.spring.annotation.GlobalTransactional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * 用户视频序列归档 业务接口实现
 */
@Service("userVideoSeriesService")
public class UserVideoSeriesServiceImpl implements UserVideoSeriesService {

	@Resource
	private UserVideoSeriesMapper<UserVideoSeries, UserVideoSeriesQuery> userVideoSeriesMapper;

	@Resource
	private VideoInfoMapper<VideoInfo, VideoInfoQuery> videoInfoMapper;

	@Resource
	private UserVideoSeriesVideoMapper<UserVideoSeriesVideo, UserVideoSeriesVideoQuery> userVideoSeriesVideoMapper;

	/**
	 * 根据条件查询列表
	 */
	@Override
	public List<UserVideoSeries> findListByParam(UserVideoSeriesQuery param) {
		return this.userVideoSeriesMapper.selectList(param);
	}

	/**
	 * 根据条件查询列表
	 */
	@Override
	public Integer findCountByParam(UserVideoSeriesQuery param) {
		return this.userVideoSeriesMapper.selectCount(param);
	}

	/**
	 * 分页查询方法
	 */
	@Override
	public PaginationResultVO<UserVideoSeries> findListByPage(UserVideoSeriesQuery param) {
		int count = this.findCountByParam(param);
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
		param.setSimplePage(page);
		List<UserVideoSeries> list = this.findListByParam(param);
		PaginationResultVO<UserVideoSeries> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	@Override
	public Integer add(UserVideoSeries bean) {
		return this.userVideoSeriesMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	@Override
	public Integer addBatch(List<UserVideoSeries> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.userVideoSeriesMapper.insertBatch(listBean);
	}

	/**
	 * 批量新增或者修改
	 */
	@Override
	public Integer addOrUpdateBatch(List<UserVideoSeries> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.userVideoSeriesMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 多条件更新
	 */
	@Override
	public Integer updateByParam(UserVideoSeries bean, UserVideoSeriesQuery param) {
		StringTools.checkParam(param);
		return this.userVideoSeriesMapper.updateByParam(bean, param);
	}

	/**
	 * 多条件删除
	 */
	@Override
	public Integer deleteByParam(UserVideoSeriesQuery param) {
		StringTools.checkParam(param);
		return this.userVideoSeriesMapper.deleteByParam(param);
	}

	/**
	 * 根据SeriesId获取对象
	 */
	@Override
	public UserVideoSeries getUserVideoSeriesBySeriesId(Integer seriesId) {
		return this.userVideoSeriesMapper.selectBySeriesId(seriesId);
	}

	/**
	 * 根据SeriesId修改
	 */
	@Override
	public Integer updateUserVideoSeriesBySeriesId(UserVideoSeries bean, Integer seriesId) {
		return this.userVideoSeriesMapper.updateBySeriesId(bean, seriesId);
	}

	/**
	 * 根据SeriesId删除
	 */
	@Override
	public Integer deleteUserVideoSeriesBySeriesId(Integer seriesId) {
		return this.userVideoSeriesMapper.deleteBySeriesId(seriesId);
	}

	@Override
	public List<UserVideoSeries> getUserAllSeries(String userId) {
		return userVideoSeriesMapper.selectUserAllSeries(userId);
	}

	@Override
	@GlobalTransactional(rollbackFor = Exception.class)
	public void saveUserVideoSeries(UserVideoSeries bean, String videoIds){
		if(bean.getUserId() == null && StringTools.isEmpty(videoIds)){
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}

		if(bean.getSeriesId() == null){
			//	该分支表示新增视频列表，新增时必须指定视频
			checkVideoIds(bean.getUserId(), videoIds);
			bean.setUpdateTime(new Date());
			bean.setSort(this.userVideoSeriesMapper.selectMaxSort(bean.getUserId()) + 1);
			this.userVideoSeriesMapper.insert(bean);

			this.saveSeriesVideo(bean.getUserId(), bean.getSeriesId(), videoIds);
		}else{
			//	该分支表示修改，且修改时只能修改视频列表的信息，而不能对其下的视频进行修改
			UserVideoSeriesQuery seriesVideoQuery = new UserVideoSeriesQuery();
			seriesVideoQuery.setUserId(bean.getUserId());
			seriesVideoQuery.setSeriesId(bean.getSeriesId());
			this.userVideoSeriesMapper.updateByParam(bean, seriesVideoQuery);
		}
	}

	private void checkVideoIds(String userId, String videoIds){
		String[] videoIdArray = videoIds.split(",");
		VideoInfoQuery videoInfoQuery = new VideoInfoQuery();
		videoInfoQuery.setVideoIdArray(videoIdArray);
		videoInfoQuery.setUserId(userId);
		Integer count = videoInfoMapper.selectCount(videoInfoQuery);
		if(videoIdArray.length != count){
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
	}

	@Override
	public void saveSeriesVideo(String userId, Integer seriesId, String videoIds) {
		UserVideoSeries userVideoSeries = getUserVideoSeriesBySeriesId(seriesId);
		if(userVideoSeries == null || !userVideoSeries.getUserId().equals(userId)){
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		this.checkVideoIds(userId, videoIds);

		String[] videoIdArray = videoIds.split(",");
		Integer sort = this.userVideoSeriesVideoMapper.selectMaxSort(seriesId);

		List<UserVideoSeriesVideo> seriesVideoList = new ArrayList<>();
		for(String videoId : videoIdArray){
			UserVideoSeriesVideo videoSeriesVideo = new UserVideoSeriesVideo();
			videoSeriesVideo.setVideoId(videoId);
			//	TODO ++sort会导致每次更换排序，其排序号会越来越高，可以试着解决一下
			videoSeriesVideo.setSort(++sort);
			videoSeriesVideo.setSeriesId(seriesId);
			videoSeriesVideo.setUserId(userId);
			seriesVideoList.add(videoSeriesVideo);
		}
		this.userVideoSeriesVideoMapper.insertOrUpdateBatch(seriesVideoList);

	}

	@Override
	public void delSeriesVideo(String userId, Integer seriesId, String videoId) {
		UserVideoSeriesVideoQuery videoSeriesVideoQuery = new UserVideoSeriesVideoQuery();
		videoSeriesVideoQuery.setUserId(userId);
		videoSeriesVideoQuery.setSeriesId(seriesId);
		videoSeriesVideoQuery.setVideoId(videoId);
		this.userVideoSeriesVideoMapper.deleteByParam(videoSeriesVideoQuery);
	}

	@Override
	@GlobalTransactional(rollbackFor = Exception.class)
	public void delVideoSeries(String userId, Integer seriesId) {
		UserVideoSeriesQuery seriesQuery = new UserVideoSeriesQuery();
		seriesQuery.setUserId(userId);
		seriesQuery.setSeriesId(seriesId);
		Integer count = userVideoSeriesMapper.deleteByParam(seriesQuery);
		if(count == 0){
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}

		UserVideoSeriesVideoQuery videoSeriesVideoQuery = new UserVideoSeriesVideoQuery();
		videoSeriesVideoQuery.setSeriesId(seriesId);
		videoSeriesVideoQuery.setUserId(userId);
		userVideoSeriesVideoMapper.deleteByParam(videoSeriesVideoQuery);
	}

	@Override
	public void changeVideoSeriesSort(String userId, String seriesIds) {
		String[] seriesIdArray = seriesIds.split(",");
		List<UserVideoSeries> videoSeriesList = new ArrayList<>();
		int sort = 0;
		for(String seriesId : seriesIdArray){
			UserVideoSeries videoSeries = new UserVideoSeries();
			videoSeries.setUserId(userId);
			videoSeries.setSeriesId(Integer.parseInt(seriesId));
			//	TODO ++sort会导致每次更换排序，其排序号会越来越高，可以试着解决一下
			videoSeries.setSort(++sort);
			videoSeriesList.add(videoSeries);
		}
		userVideoSeriesMapper.changeSort(videoSeriesList);
	}

	@Override
	public List<UserVideoSeries> findListWithVideoList(UserVideoSeriesQuery seriesQuery) {
		return userVideoSeriesMapper.selectListWithVideo(seriesQuery);
	}
}