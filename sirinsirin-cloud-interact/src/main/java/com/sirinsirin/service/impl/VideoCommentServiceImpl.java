package com.sirinsirin.service.impl;

import com.sirinsirin.api.consumer.VideoClient;
import com.sirinsirin.entity.constants.Constants;
import com.sirinsirin.entity.enums.CommentTopTypeEnum;
import com.sirinsirin.entity.enums.PageSize;
import com.sirinsirin.entity.enums.ResponseCodeEnum;
import com.sirinsirin.entity.enums.UserActionTypeEnum;
import com.sirinsirin.entity.po.UserInfo;
import com.sirinsirin.entity.po.VideoComment;
import com.sirinsirin.entity.po.VideoInfo;
import com.sirinsirin.entity.query.SimplePage;
import com.sirinsirin.entity.query.VideoCommentQuery;
import com.sirinsirin.entity.vo.PaginationResultVO;
import com.sirinsirin.exception.BusinessException;
import com.sirinsirin.mappers.VideoCommentMapper;
import com.sirinsirin.service.VideoCommentService;
import com.sirinsirin.utils.StringTools;
import io.seata.spring.annotation.GlobalTransactional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;


/**
 * 评论 业务接口实现
 */
@Service("videoCommentService")
public class VideoCommentServiceImpl implements VideoCommentService {

	@Resource
	private VideoCommentMapper<VideoComment, VideoCommentQuery> videoCommentMapper;

    @Resource
    private VideoClient videoClient;

	/**
	 * 根据条件查询列表
	 */
	@Override
	public List<VideoComment> findListByParam(VideoCommentQuery param) {
		if(param.getLoadChildren() != null && param.getLoadChildren()){
			return this.videoCommentMapper.selectListWithChildren(param);
		}
		List<VideoComment> list = this.videoCommentMapper.selectList(param);
		return this.videoCommentMapper.selectList(param);
	}

	/**
	 * 根据条件查询列表
	 */
	@Override
	public Integer findCountByParam(VideoCommentQuery param) {
		return this.videoCommentMapper.selectCount(param);
	}

	/**
	 * 分页查询方法
	 */
	@Override
	public PaginationResultVO<VideoComment> findListByPage(VideoCommentQuery param) {
		int count = this.findCountByParam(param);
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
		param.setSimplePage(page);
		List<VideoComment> list = this.findListByParam(param);
		PaginationResultVO<VideoComment> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	@Override
	public Integer add(VideoComment bean) {
		return this.videoCommentMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	@Override
	public Integer addBatch(List<VideoComment> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.videoCommentMapper.insertBatch(listBean);
	}

	/**
	 * 批量新增或者修改
	 */
	@Override
	public Integer addOrUpdateBatch(List<VideoComment> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.videoCommentMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 多条件更新
	 */
	@Override
	public Integer updateByParam(VideoComment bean, VideoCommentQuery param) {
		StringTools.checkParam(param);
		return this.videoCommentMapper.updateByParam(bean, param);
	}

	/**
	 * 多条件删除
	 */
	@Override
	public Integer deleteByParam(VideoCommentQuery param) {
		StringTools.checkParam(param);
		return this.videoCommentMapper.deleteByParam(param);
	}

	/**
	 * 根据CommentId获取对象
	 */
	@Override
	public VideoComment getVideoCommentByCommentId(Integer commentId) {
		return this.videoCommentMapper.selectByCommentId(commentId);
	}

	/**
	 * 根据CommentId修改
	 */
	@Override
	public Integer updateVideoCommentByCommentId(VideoComment bean, Integer commentId) {
		return this.videoCommentMapper.updateByCommentId(bean, commentId);
	}

	/**
	 * 根据CommentId删除
	 */
	@Override
	public Integer deleteVideoCommentByCommentId(Integer commentId) {
		return this.videoCommentMapper.deleteByCommentId(commentId);
	}

	@Override
	@GlobalTransactional(rollbackFor = Exception.class)
	public void postComment(VideoComment comment, Integer replyCommentId) {
		VideoInfo videoInfo = videoClient.getVideoInfoByVideoId(comment.getVideoId());
		if(videoInfo == null){
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		//	假设up主后面将评论区关闭，但是某用户没有刷新页面，此时前端页面仍然可以发送评论，但后端会给用户发送“up主已关闭评论区”的提示，
		//	如果这里使用“CODE_600”提示参数错误对于用户而言不太友好
		if(videoInfo.getInteraction() != null && videoInfo.getInteraction().contains(Constants.ZERO.toString())){
			throw new BusinessException("up主已关闭评论区");
		}
		if(replyCommentId != null){
			VideoComment replyComment = getVideoCommentByCommentId(replyCommentId);
			if(replyComment == null || !replyComment.getVideoId().equals(comment.getVideoId())){
				throw new BusinessException(ResponseCodeEnum.CODE_600);
			}
			//	评论只有一级评论和二级评论，即便回复二级评论的评论也视作为二级评论，二级评论的pid全是一级评论的id
			//	若回复的评论为一级评论。则将pid设置为一级评论的id
			if(replyComment.getpCommentId() == 0){
				comment.setpCommentId(replyCommentId);
			}else{
				//	若回复的评论为二级评论。则将pid设置为对应一级评论的id
				comment.setpCommentId(replyComment.getpCommentId());
				comment.setReplyUserId(replyComment.getUserId());
			}
			UserInfo userInfo = videoClient.getUserInfoByUserId(replyComment.getUserId());
			comment.setReplyUserId(userInfo.getUserId());
			comment.setReplyNickName(userInfo.getNickName());
			comment.setReplyAvatar(userInfo.getAvatar());
		}else{
			comment.setpCommentId(0);
		}

		comment.setPostTime(new Date());
		comment.setVideoUserId(videoInfo.getUserId());
		this.videoCommentMapper.insert(comment);
//		if(comment.getpCommentId() == 0){	//	只统计一级评论数
			//	评论数 +1
			this.videoClient.updateCountInfo(comment.getVideoId(), UserActionTypeEnum.VIDEO_COMMENT.getField(), 1);
//		}

		//	该语句触发异常时，插入评论记录会因为@Transactional注解触发回滚；
		//	但修改评论数的语句因为是调用服务b，而完成数据修改操作的也是b，因此不会回滚。致使数据不一致
//			int a = 1 / 0;
	}

	@Override
	//	这里内部调用的方法可以保证在同一个事务内。若该方法执行cancelTopComment()方法时发生异常，会执行回滚
	//	但在cancelTopComment()方法上面添加的@Transaction注解不会生效
	@GlobalTransactional(rollbackFor = Exception.class)
	public void topComment(Integer commentId, String userId) {
		//	先取消置顶当前已置顶的评论
		this.cancelTopComment(commentId, userId);
		//	再置顶选中的评论
		VideoComment videoComment = new VideoComment();
		videoComment.setTopType(CommentTopTypeEnum.TOP.getType());
		videoCommentMapper.updateByCommentId(videoComment, commentId);
	}

	@Override
//	@Transactional(rollbackFor = Exception.class)	//	在topComment()方法调用该方法时，该注解不会生效
	public void cancelTopComment(Integer commentId, String userId) {
		VideoComment dbVideoComment = videoCommentMapper.selectByCommentId(commentId);
		if(dbVideoComment == null){
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		VideoInfo videoInfo = videoClient.getVideoInfoByVideoId(dbVideoComment.getVideoId());
		if(videoInfo == null){
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		if(!videoInfo.getUserId().equals(userId)){
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}

		VideoComment videoComment = new VideoComment();
		videoComment.setTopType(CommentTopTypeEnum.NO_TOP.getType());

		VideoCommentQuery videoCommentQuery = new VideoCommentQuery();
		videoCommentQuery.setVideoId(dbVideoComment.getVideoId());
		videoCommentQuery.setTopType(CommentTopTypeEnum.TOP.getType());
		videoCommentMapper.updateByParam(videoComment, videoCommentQuery);
	}

	@Override
	//	该方法针对删除评论后的视频评论数的减去规则进行过修改，原代码请看/static/img/deleteComment.png
	public void deleteComment(Integer commentId, String userId) {
		VideoComment comment = videoCommentMapper.selectByCommentId(commentId);
		if(comment == null){
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		VideoInfo videoInfo = videoClient.getVideoInfoByVideoId(comment.getVideoId());
		if(videoInfo == null){
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		if(userId != null && !videoInfo.getUserId().equals(userId) && !comment.getUserId().equals(userId)){
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		//	删除选定评论
		videoCommentMapper.deleteByCommentId(commentId);
//		//	评论数 -1，原本没有这个，是我自己加的
//		videoInfoMapper.updateCountInfo(
//				videoInfo.getVideoId(),
//				UserActionTypeEnum.VIDEO_COMMENT.getField(),
//				-1
//		);

		if(comment.getpCommentId() == 0){
//			videoInfoMapper.updateCountInfo(
//					videoInfo.getVideoId(),
//					UserActionTypeEnum.VIDEO_COMMENT.getField(),
//					-1
//			);
//			//	若删除评论为一级评论则将对应二级评论一并删除
//			VideoCommentQuery videoCommentQuery = new VideoCommentQuery();
//			videoCommentQuery.setpCommentId(commentId);
//			videoCommentMapper.deleteByParam(videoCommentQuery);

			//	若删除评论为一级评论则将对应二级评论一并删除
			VideoCommentQuery videoCommentQuery = new VideoCommentQuery();
			videoCommentQuery.setpCommentId(commentId);
			Integer delCount = videoCommentMapper.deleteByParam(videoCommentQuery);
			videoClient.updateCountInfo(
					videoInfo.getVideoId(),
					UserActionTypeEnum.VIDEO_COMMENT.getField(),
					-(delCount + 1)
			);
		}else{	//	若不是一级评论，则评论数直接减1
			videoClient.updateCountInfo(
					videoInfo.getVideoId(),
					UserActionTypeEnum.VIDEO_COMMENT.getField(),
					Constants.MINUS_ONE
			);
		}
	}
}