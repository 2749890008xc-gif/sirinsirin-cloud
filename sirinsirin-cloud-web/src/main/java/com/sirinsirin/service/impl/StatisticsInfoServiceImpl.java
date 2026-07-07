package com.sirinsirin.service.impl;

import com.sirinsirin.component.RedisComponent;
import com.sirinsirin.entity.constants.Constants;
import com.sirinsirin.entity.enums.PageSize;
import com.sirinsirin.entity.enums.StatisticsTypeEnum;
import com.sirinsirin.entity.enums.UserActionTypeEnum;
import com.sirinsirin.entity.po.StatisticsInfo;
import com.sirinsirin.entity.po.VideoInfo;
import com.sirinsirin.entity.query.SimplePage;
import com.sirinsirin.entity.query.StatisticsInfoQuery;
import com.sirinsirin.entity.query.UserInfoQuery;
import com.sirinsirin.entity.query.VideoInfoQuery;
import com.sirinsirin.entity.vo.PaginationResultVO;
import com.sirinsirin.mappers.StatisticsInfoMapper;
import com.sirinsirin.mappers.UserFocusMapper;
import com.sirinsirin.mappers.UserInfoMapper;
import com.sirinsirin.mappers.VideoInfoMapper;
import com.sirinsirin.service.StatisticsInfoService;
import com.sirinsirin.utils.DateUtil;
import com.sirinsirin.utils.StringTools;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * 数据统计 业务接口实现
 */
@Service("statisticsInfoService")
public class StatisticsInfoServiceImpl implements StatisticsInfoService {

	@Resource
	private StatisticsInfoMapper<StatisticsInfo, StatisticsInfoQuery> statisticsInfoMapper;

	@Resource
	private RedisComponent redisComponent;

	@Resource
	private VideoInfoMapper videoInfoMapper;

	@Resource
	private UserInfoMapper userInfoMapper;

	@Resource
	private UserFocusMapper userFocusMapper;

	/**
	 * 根据条件查询列表
	 */
	@Override
	public List<StatisticsInfo> findListByParam(StatisticsInfoQuery param) {
		return this.statisticsInfoMapper.selectList(param);
	}

	/**
	 * 根据条件查询列表
	 */
	@Override
	public Integer findCountByParam(StatisticsInfoQuery param) {
		return this.statisticsInfoMapper.selectCount(param);
	}

	/**
	 * 分页查询方法
	 */
	@Override
	public PaginationResultVO<StatisticsInfo> findListByPage(StatisticsInfoQuery param) {
		int count = this.findCountByParam(param);
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
		param.setSimplePage(page);
		List<StatisticsInfo> list = this.findListByParam(param);
		PaginationResultVO<StatisticsInfo> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	@Override
	public Integer add(StatisticsInfo bean) {
		return this.statisticsInfoMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	@Override
	public Integer addBatch(List<StatisticsInfo> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.statisticsInfoMapper.insertBatch(listBean);
	}

	/**
	 * 批量新增或者修改
	 */
	@Override
	public Integer addOrUpdateBatch(List<StatisticsInfo> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.statisticsInfoMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 多条件更新
	 */
	@Override
	public Integer updateByParam(StatisticsInfo bean, StatisticsInfoQuery param) {
		StringTools.checkParam(param);
		return this.statisticsInfoMapper.updateByParam(bean, param);
	}

	/**
	 * 多条件删除
	 */
	@Override
	public Integer deleteByParam(StatisticsInfoQuery param) {
		StringTools.checkParam(param);
		return this.statisticsInfoMapper.deleteByParam(param);
	}

	/**
	 * 根据StatisticsDateAndUserIdAndDataType获取对象
	 */
	@Override
	public StatisticsInfo getStatisticsInfoByStatisticsDateAndUserIdAndDataType(String statisticsDate, String userId, Integer dataType) {
		return this.statisticsInfoMapper.selectByStatisticsDateAndUserIdAndDataType(statisticsDate, userId, dataType);
	}

	/**
	 * 根据StatisticsDateAndUserIdAndDataType修改
	 */
	@Override
	public Integer updateStatisticsInfoByStatisticsDateAndUserIdAndDataType(StatisticsInfo bean, String statisticsDate, String userId, Integer dataType) {
		return this.statisticsInfoMapper.updateByStatisticsDateAndUserIdAndDataType(bean, statisticsDate, userId, dataType);
	}

	/**
	 * 根据StatisticsDateAndUserIdAndDataType删除
	 */
	@Override
	public Integer deleteStatisticsInfoByStatisticsDateAndUserIdAndDataType(String statisticsDate, String userId, Integer dataType) {
		return this.statisticsInfoMapper.deleteByStatisticsDateAndUserIdAndDataType(statisticsDate, userId, dataType);
	}

	@Override
	public void statisticsData() {
		List<StatisticsInfo> statisticsInfoList = new ArrayList<>();
		final String statisticDate = DateUtil.getBeforeDayDate(1);

		//统计播放量
		Map<String, Integer> videoPlayCountMap = redisComponent.getVideoPlayCount(statisticDate);

		List<String> playVideoKeys = new ArrayList<>(videoPlayCountMap.keySet());
		playVideoKeys = playVideoKeys.stream().map(item -> item.substring(item.lastIndexOf(":") + 1)).collect(Collectors.toList());

		VideoInfoQuery videoInfoQuery = new VideoInfoQuery();
		videoInfoQuery.setVideoIdArray(playVideoKeys.toArray(new String[playVideoKeys.size()]));
		List<VideoInfo> videoInfoList = videoInfoMapper.selectList(videoInfoQuery);

		Map<String, Integer> videoCountMap = videoInfoList.stream().collect(Collectors.groupingBy(VideoInfo::getUserId,
				Collectors.summingInt(
						item -> videoPlayCountMap.get(
								Constants.REDIS_KEY_VIDEO_PLAY_COUNT + statisticDate + ":" + item.getVideoId()
						)
				)
		));

		videoCountMap.forEach((k, v) -> {
			StatisticsInfo statisticsInfo = new StatisticsInfo();
			statisticsInfo.setStatisticsDate(statisticDate);
			statisticsInfo.setUserId(k);
			statisticsInfo.setDataType(StatisticsTypeEnum.PLAY.getType());
			statisticsInfo.setStatisticsCount(v);
			statisticsInfoList.add(statisticsInfo);
		});

		//	统计粉丝数
		List<StatisticsInfo> fansDataList = this.statisticsInfoMapper.selectStatisticsFans(statisticDate);
		for (StatisticsInfo statisticsInfo : fansDataList) {
			statisticsInfo.setStatisticsDate(statisticDate);
			statisticsInfo.setDataType(StatisticsTypeEnum.FANS.getType());
		}
		statisticsInfoList.addAll(fansDataList);

		//	统计评论
		List<StatisticsInfo> commentDataList = this.statisticsInfoMapper.selectStatisticsComment(statisticDate);
		for (StatisticsInfo statisticsInfo : commentDataList) {
			statisticsInfo.setStatisticsDate(statisticDate);
			statisticsInfo.setDataType(StatisticsTypeEnum.COMMENT.getType());
		}
		statisticsInfoList.addAll(commentDataList);

		//	统计点赞、收藏、投币
		List<StatisticsInfo> statisticsOthers = this.statisticsInfoMapper.selectStatisticsInfo(statisticDate, new Integer[]{
				UserActionTypeEnum.VIDEO_LIKE.getType(), UserActionTypeEnum.VIDEO_COIN.getType(), UserActionTypeEnum.VIDEO_COLLECT.getType()
		});
		for (StatisticsInfo statisticsInfo : statisticsOthers) {
			statisticsInfo.setStatisticsDate(statisticDate);
			if (UserActionTypeEnum.VIDEO_LIKE.getType().equals(statisticsInfo.getDataType())) {
				statisticsInfo.setDataType(StatisticsTypeEnum.LIKE.getType());
			} else if (UserActionTypeEnum.VIDEO_COLLECT.getType().equals(statisticsInfo.getDataType())) {
				statisticsInfo.setDataType(StatisticsTypeEnum.COLLECTION.getType());
			} else if (UserActionTypeEnum.VIDEO_COIN.getType().equals(statisticsInfo.getDataType())) {
				statisticsInfo.setDataType(StatisticsTypeEnum.COIN.getType());
			}
		}
		statisticsInfoList.addAll(statisticsOthers);

		//	统计弹幕
		List<StatisticsInfo> danmuDataList = this.statisticsInfoMapper.selectStatisticsDanmu(statisticDate);
		for (StatisticsInfo statisticsInfo : danmuDataList) {
			statisticsInfo.setStatisticsDate(statisticDate);
			statisticsInfo.setDataType(StatisticsTypeEnum.DANMU.getType());
		}
		statisticsInfoList.addAll(danmuDataList);
		this.statisticsInfoMapper.insertOrUpdateBatch(statisticsInfoList);
	}

	@Override
	public Map<String, Integer> getStatisticsInfoActualTime(String userId) {
		Map<String, Integer> result = statisticsInfoMapper.selectTotalCountInfo(userId);
		if (!StringTools.isEmpty(userId)) {
			result.put("userCount", userFocusMapper.selectFansCount(userId));
		} else {
			result.put("userCount", userInfoMapper.selectCount(new UserInfoQuery()));
		}
		return result;
	}

	@Override
	public List<StatisticsInfo> findListTotalInfoByParam(StatisticsInfoQuery query) {
		return this.statisticsInfoMapper.selectListTotalInfoByParam(query);
	}

	@Override
	public List<StatisticsInfo> findUserCountTotalInfoByParam(StatisticsInfoQuery query) {

		return statisticsInfoMapper.selectUserCountTotalInfoByParam(query);
	}
}