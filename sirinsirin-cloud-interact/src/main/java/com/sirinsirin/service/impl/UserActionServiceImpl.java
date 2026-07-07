package com.sirinsirin.service.impl;

import com.sirinsirin.api.consumer.VideoClient;
import com.sirinsirin.entity.constants.Constants;
import com.sirinsirin.entity.enums.PageSize;
import com.sirinsirin.entity.enums.ResponseCodeEnum;
import com.sirinsirin.entity.enums.SearchOrderTypeEnum;
import com.sirinsirin.entity.enums.UserActionTypeEnum;
import com.sirinsirin.entity.po.UserAction;
import com.sirinsirin.entity.po.UserInfo;
import com.sirinsirin.entity.po.VideoComment;
import com.sirinsirin.entity.po.VideoInfo;
import com.sirinsirin.entity.query.*;
import com.sirinsirin.entity.vo.PaginationResultVO;
import com.sirinsirin.exception.BusinessException;
import com.sirinsirin.mappers.UserActionMapper;
import com.sirinsirin.mappers.VideoCommentMapper;
import com.sirinsirin.service.UserActionService;
import com.sirinsirin.utils.StringTools;
import io.seata.spring.annotation.GlobalTransactional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;


/**
 * 用户行为 点赞、评论 业务接口实现
 */
@Service("userActionService")
public class UserActionServiceImpl implements UserActionService {

	@Resource
	private UserActionMapper<UserAction, UserActionQuery> userActionMapper;

	@Resource
	private VideoCommentMapper<VideoComment, VideoCommentQuery> videoCommentMapper;

	@Resource
	private VideoClient videoClient;

	/**
	 * 根据条件查询列表
	 */
	@Override
	public List<UserAction> findListByParam(UserActionQuery param) {
		return this.userActionMapper.selectList(param);
	}

	/**
	 * 根据条件查询列表
	 */
	@Override
	public Integer findCountByParam(UserActionQuery param) {
		return this.userActionMapper.selectCount(param);
	}

	/**
	 * 分页查询方法
	 */
	@Override
	public PaginationResultVO<UserAction> findListByPage(UserActionQuery param) {
		int count = this.findCountByParam(param);
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
		param.setSimplePage(page);
		List<UserAction> list = this.findListByParam(param);
		PaginationResultVO<UserAction> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	@Override
	public Integer add(UserAction bean) {
		return this.userActionMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	@Override
	public Integer addBatch(List<UserAction> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.userActionMapper.insertBatch(listBean);
	}

	/**
	 * 批量新增或者修改
	 */
	@Override
	public Integer addOrUpdateBatch(List<UserAction> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.userActionMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 多条件更新
	 */
	@Override
	public Integer updateByParam(UserAction bean, UserActionQuery param) {
		StringTools.checkParam(param);
		return this.userActionMapper.updateByParam(bean, param);
	}

	/**
	 * 多条件删除
	 */
	@Override
	public Integer deleteByParam(UserActionQuery param) {
		StringTools.checkParam(param);
		return this.userActionMapper.deleteByParam(param);
	}

	/**
	 * 根据ActionId获取对象
	 */
	@Override
	public UserAction getUserActionByActionId(Integer actionId) {
		return this.userActionMapper.selectByActionId(actionId);
	}

	/**
	 * 根据ActionId修改
	 */
	@Override
	public Integer updateUserActionByActionId(UserAction bean, Integer actionId) {
		return this.userActionMapper.updateByActionId(bean, actionId);
	}

	/**
	 * 根据ActionId删除
	 */
	@Override
	public Integer deleteUserActionByActionId(Integer actionId) {
		return this.userActionMapper.deleteByActionId(actionId);
	}

	/**
	 * 根据VideoIdAndCommentIdAndActionTypeAndUserId获取对象
	 */
	@Override
	public UserAction getUserActionByVideoIdAndCommentIdAndActionTypeAndUserId(String videoId, Integer commentId, Integer actionType, String userId) {
		return this.userActionMapper.selectByVideoIdAndCommentIdAndActionTypeAndUserId(videoId, commentId, actionType, userId);
	}

	/**
	 * 根据VideoIdAndCommentIdAndActionTypeAndUserId修改
	 */
	@Override
	public Integer updateUserActionByVideoIdAndCommentIdAndActionTypeAndUserId(UserAction bean, String videoId, Integer commentId, Integer actionType, String userId) {
		return this.userActionMapper.updateByVideoIdAndCommentIdAndActionTypeAndUserId(bean, videoId, commentId, actionType, userId);
	}

	/**
	 * 根据VideoIdAndCommentIdAndActionTypeAndUserId删除
	 */
	@Override
	public Integer deleteUserActionByVideoIdAndCommentIdAndActionTypeAndUserId(String videoId, Integer commentId, Integer actionType, String userId) {
		return this.userActionMapper.deleteByVideoIdAndCommentIdAndActionTypeAndUserId(videoId, commentId, actionType, userId);
	}

	@Override
	@GlobalTransactional(rollbackFor = Exception.class)
	public void saveAction(UserAction userAction) {
		VideoInfo videoInfo = videoClient.getVideoInfoByVideoId(userAction.getVideoId());
		if(videoInfo == null){
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		userAction.setVideoUserId(videoInfo.getUserId());

		UserActionTypeEnum actionTypeEnum = UserActionTypeEnum.getByType(userAction.getActionType());
		if(actionTypeEnum == null){
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}

		UserAction dbAction = userActionMapper.selectByVideoIdAndCommentIdAndActionTypeAndUserId(
				userAction.getVideoId(),
				userAction.getCommentId(),
				userAction.getActionType(),
				userAction.getUserId());
		userAction.setActionTime(new Date());

		switch (actionTypeEnum){
			case VIDEO_LIKE:
			case VIDEO_COLLECT:
				//	从数据库中查找，如果有记录则表示取消（点赞、收藏等），如果没有记录则表示进行（点赞、收藏等）行为
				if(dbAction != null){
					userActionMapper.deleteByActionId(dbAction.getActionId());
				}else{
					userActionMapper.insert(userAction);
				}
				Integer changeCount = dbAction == null ? Constants.ONE : -Constants.ONE;
				videoClient.updateCountInfo(userAction.getVideoId(), actionTypeEnum.getField(), changeCount);
				if(actionTypeEnum == UserActionTypeEnum.VIDEO_COLLECT){
					videoClient.updateDocCount(videoInfo.getVideoId(), SearchOrderTypeEnum.VIDEO_COLLECT, changeCount);
				}
				break;
			case VIDEO_COIN:
				//	如果相等则意味着投币者和作者为同一人
				if(videoInfo.getUserId().equals(userAction.getUserId())){
					throw new BusinessException("up主不能给自己投币");
				}
				if(dbAction != null){
					throw new BusinessException("对本稿件的投币次数已用完");
				}
				//	减少自己的硬币
				//	不执行查询操作，直接执行修改操作，这样可以保证不会读脏数据，不会出现并发所带来的数据库问题
				Integer updateCount = videoClient.updateCoinCountInfo(userAction.getUserId(), -userAction.getActionCount());
				//	如果为0，则表示sql语句的修改操作受影响行数为0，即：硬币数量不够
				if(updateCount == 0){
					throw new BusinessException("硬币不足");
				}

				//	给up主加币
				updateCount = videoClient.updateCoinCountInfo(videoInfo.getUserId(), userAction.getActionCount());
				if(updateCount == 0){
					throw new BusinessException("投币失败");
				}
				userActionMapper.insert(userAction);
				videoClient.updateCountInfo(userAction.getVideoId(), actionTypeEnum.getField(), userAction.getActionCount());
				break;
			case COMMENT_LIKE:
			case COMMENT_HATE:
				UserActionTypeEnum opposeTypeEnum = UserActionTypeEnum.COMMENT_LIKE ==
						actionTypeEnum ? UserActionTypeEnum.COMMENT_HATE : UserActionTypeEnum.COMMENT_LIKE;

				UserAction opposeAction = userActionMapper.selectByVideoIdAndCommentIdAndActionTypeAndUserId(
						userAction.getVideoId(),
						userAction.getCommentId(),
						opposeTypeEnum.getType(),
						userAction.getUserId()
				);
				//	当进行评论点赞或者评论讨厌操作时，先查看其对立面是否存在，若存在则删除对立面操作的记录
				if(opposeAction != null){
					userActionMapper.deleteByActionId(opposeAction.getActionId());
				}
				//	当对已经点过赞或讨厌的评论再次执行相同操作时，删除该操作的记录
				if(dbAction != null){
					userActionMapper.deleteByActionId(dbAction.getActionId());
				}else{
					userActionMapper.insert(userAction);
				}
				changeCount = dbAction == null ? Constants.ONE : -Constants.ONE;
				Integer opposeChangeCount = -changeCount;
				videoCommentMapper.updateCountInfo(
						userAction.getCommentId(),
						actionTypeEnum.getField(),
						changeCount,
						opposeAction == null ? null : opposeTypeEnum.getField(),
						opposeChangeCount
				);
				break;
		}
	}
}