package com.sirinsirin.service.impl;

import com.sirinsirin.api.consumer.VideoClient;
import com.sirinsirin.entity.constants.Constants;
import com.sirinsirin.entity.enums.PageSize;
import com.sirinsirin.entity.enums.ResponseCodeEnum;
import com.sirinsirin.entity.enums.SearchOrderTypeEnum;
import com.sirinsirin.entity.enums.UserActionTypeEnum;
import com.sirinsirin.entity.po.VideoDanmu;
import com.sirinsirin.entity.po.VideoInfo;
import com.sirinsirin.entity.query.SimplePage;
import com.sirinsirin.entity.query.VideoDanmuQuery;
import com.sirinsirin.entity.query.VideoInfoQuery;
import com.sirinsirin.entity.vo.PaginationResultVO;
import com.sirinsirin.exception.BusinessException;
import com.sirinsirin.mappers.VideoDanmuMapper;
import com.sirinsirin.service.VideoDanmuService;
import com.sirinsirin.utils.StringTools;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;


/**
 * 视频弹幕 业务接口实现
 */
@Service("videoDanmuService")
public class VideoDanmuServiceImpl implements VideoDanmuService {

	@Resource
	private VideoDanmuMapper<VideoDanmu, VideoDanmuQuery> videoDanmuMapper;

	@Resource
	private VideoClient videoClient;

	/**
	 * 根据条件查询列表
	 */
	@Override
	public List<VideoDanmu> findListByParam(VideoDanmuQuery param) {
		List<VideoDanmu> vd = this.videoDanmuMapper.selectList(param);
		return vd;
	}

	/**
	 * 根据条件查询列表
	 */
	@Override
	public Integer findCountByParam(VideoDanmuQuery param) {
		return this.videoDanmuMapper.selectCount(param);
	}

	/**
	 * 分页查询方法
	 */
	@Override
	public PaginationResultVO<VideoDanmu> findListByPage(VideoDanmuQuery param) {
		int count = this.findCountByParam(param);
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
		param.setSimplePage(page);
		List<VideoDanmu> list = this.findListByParam(param);
		PaginationResultVO<VideoDanmu> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	@Override
	public Integer add(VideoDanmu bean) {
		return this.videoDanmuMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	@Override
	public Integer addBatch(List<VideoDanmu> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.videoDanmuMapper.insertBatch(listBean);
	}

	/**
	 * 批量新增或者修改
	 */
	@Override
	public Integer addOrUpdateBatch(List<VideoDanmu> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.videoDanmuMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 多条件更新
	 */
	@Override
	public Integer updateByParam(VideoDanmu bean, VideoDanmuQuery param) {
		StringTools.checkParam(param);
		return this.videoDanmuMapper.updateByParam(bean, param);
	}

	/**
	 * 多条件删除
	 */
	@Override
	public Integer deleteByParam(VideoDanmuQuery param) {
		StringTools.checkParam(param);
		return this.videoDanmuMapper.deleteByParam(param);
	}

	/**
	 * 根据DanmuId获取对象
	 */
	@Override
	public VideoDanmu getVideoDanmuByDanmuId(Integer danmuId) {
		return this.videoDanmuMapper.selectByDanmuId(danmuId);
	}

	/**
	 * 根据DanmuId修改
	 */
	@Override
	public Integer updateVideoDanmuByDanmuId(VideoDanmu bean, Integer danmuId) {
		return this.videoDanmuMapper.updateByDanmuId(bean, danmuId);
	}

	/**
	 * 根据DanmuId删除
	 */
	@Override
	public Integer deleteVideoDanmuByDanmuId(Integer danmuId) {
		return this.videoDanmuMapper.deleteByDanmuId(danmuId);
	}

	@Override
	//	先查询出数据赋值给变量 -> 变量+1 -> 使用update将变量的值放入数据库 这样传统的修改数据方式存在并发问题
	//	方法一，悲观锁：加上下面的注解后 -> 在mapper中的查询语句要加上 “ for update ” 这样一来就会在执行修改操作时，为该条记录加上锁，保证数据的一致性
//	@Transactional(rollbackFor = BusinessException.class)
	//	方法二，乐观锁：该表增加version字段 -> 设置version的值只要在该条数据被操作就自增 -> update语句的where后面要额外增加version = 查询时拿到的值
	// 当第二个人和第一个人拿到相同的version时，因第一人的修改操作完成，version自增1，而” 第二个人的version = 查询出的值 “判断为假，因此不执行修改操作
	//	注意：方法一需要在并发时额外增加等待的效果；而方法二则是在并发时给第二个人弹出“ 增加弹幕失败 ”等信息

	public void saveVideoDanmu(VideoDanmu videoDanmu) {
		VideoInfo videoInfo = videoClient.getVideoInfoByVideoId(videoDanmu.getVideoId());
		if(videoInfo == null){
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		if(videoInfo.getInteraction() != null && videoInfo.getInteraction().contains(Constants.ONE.toString())){
			throw new BusinessException("up主已经关闭弹幕");
		}
		this.videoDanmuMapper.insert(videoDanmu);
		//	方法三：不执行查询操作，直接执行修改操作，这样可以保证不会读脏数据
		this.videoClient.updateCountInfo(videoDanmu.getVideoId(), UserActionTypeEnum.VIDEO_DANMU.getField(), 1);

		videoClient.updateDocCount(videoDanmu.getVideoId(), SearchOrderTypeEnum.VIDEO_DANMU, 1);
	}

	@Override
	public void deleteDanmu(String userId, Integer danmuId) {
		VideoDanmu videoDanmu = videoDanmuMapper.selectByDanmuId(danmuId);
		if (videoDanmu == null) {
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}

		VideoInfo videoInfo = videoClient.getVideoInfoByVideoId(videoDanmu.getVideoId());
		if (videoInfo == null) {
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}

		if (userId != null && !videoInfo.getUserId().equals(userId)) {
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}

		videoDanmuMapper.deleteByDanmuId(danmuId);
		//	减少弹幕数量
		videoClient.updateCountInfo(videoDanmu.getVideoId(), UserActionTypeEnum.VIDEO_DANMU.getField(), Constants.MINUS_ONE);
	}
}