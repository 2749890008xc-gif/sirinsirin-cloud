package com.sirinsirin.service.impl;

import com.sirinsirin.component.EsSearchComponent;
import com.sirinsirin.component.RedisComponent;
import com.sirinsirin.entity.config.AppConfig;
import com.sirinsirin.entity.constants.Constants;
import com.sirinsirin.entity.dto.SysSettingDto;
import com.sirinsirin.entity.dto.UploadingFileDto;
import com.sirinsirin.entity.enums.*;
import com.sirinsirin.entity.po.VideoInfo;
import com.sirinsirin.entity.po.VideoInfoFile;
import com.sirinsirin.entity.po.VideoInfoFilePost;
import com.sirinsirin.entity.po.VideoInfoPost;
import com.sirinsirin.entity.query.*;
import com.sirinsirin.entity.vo.PaginationResultVO;
import com.sirinsirin.exception.BusinessException;
import com.sirinsirin.mappers.*;
import com.sirinsirin.service.VideoInfoPostService;
import com.sirinsirin.utils.CopyTools;
import com.sirinsirin.utils.FFmpegUtils;
import com.sirinsirin.utils.StringTools;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * 视频信息，这个表示视频的发布表 业务接口实现
 */
@Service("videoInfoPostService")
@Slf4j
public class VideoInfoPostServiceImpl implements VideoInfoPostService {

	@Resource
	private VideoInfoPostMapper<VideoInfoPost, VideoInfoPostQuery> videoInfoPostMapper;

	@Resource
	private VideoInfoFilePostMapper<VideoInfoFilePost, VideoInfoFilePostQuery> videoInfoFilePostMapper;

	@Resource
	private VideoInfoMapper<VideoInfo, VideoInfoQuery> videoInfoMapper;

	@Resource
	private VideoInfoFileMapper<VideoInfoFile, VideoInfoFileQuery> videoInfoFileMapper;

	@Resource
	private RedisComponent redisComponent;

	@Resource
	private AppConfig appConfig;

	@Resource
	private FFmpegUtils fFmpegUtils;

	@Resource
	private EsSearchComponent esSearchComponent;

    @Resource
    private UserInfoMapper userInfoMapper;

	/**
	 * 根据条件查询列表
	 */
	@Override
	public List<VideoInfoPost> findListByParam(VideoInfoPostQuery param) {
		return this.videoInfoPostMapper.selectList(param);
	}

	/**
	 * 根据条件查询列表
	 */
	@Override
	public Integer findCountByParam(VideoInfoPostQuery param) {
		return this.videoInfoPostMapper.selectCount(param);
	}

	/**
	 * 分页查询方法
	 */
	@Override
	public PaginationResultVO<VideoInfoPost> findListByPage(VideoInfoPostQuery param) {
		int count = this.findCountByParam(param);
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
		param.setSimplePage(page);
		List<VideoInfoPost> list = this.findListByParam(param);
		PaginationResultVO<VideoInfoPost> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	@Override
	public Integer add(VideoInfoPost bean) {
		return this.videoInfoPostMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	@Override
	public Integer addBatch(List<VideoInfoPost> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.videoInfoPostMapper.insertBatch(listBean);
	}

	/**
	 * 批量新增或者修改
	 */
	@Override
	public Integer addOrUpdateBatch(List<VideoInfoPost> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.videoInfoPostMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 多条件更新
	 */
	@Override
	public Integer updateByParam(VideoInfoPost bean, VideoInfoPostQuery param) {
		StringTools.checkParam(param);
		return this.videoInfoPostMapper.updateByParam(bean, param);
	}

	/**
	 * 多条件删除
	 */
	@Override
	public Integer deleteByParam(VideoInfoPostQuery param) {
		StringTools.checkParam(param);
		return this.videoInfoPostMapper.deleteByParam(param);
	}

	/**
	 * 根据VideoId获取对象
	 */
	@Override
	public VideoInfoPost getVideoInfoPostByVideoId(String videoId) {
		return this.videoInfoPostMapper.selectByVideoId(videoId);
	}

	/**
	 * 根据VideoId修改
	 */
	@Override
	public Integer updateVideoInfoPostByVideoId(VideoInfoPost bean, String videoId) {
		return this.videoInfoPostMapper.updateByVideoId(bean, videoId);
	}

	/**
	 * 根据VideoId删除
	 */
	@Override
	public Integer deleteVideoInfoPostByVideoId(String videoId) {
		return this.videoInfoPostMapper.deleteByVideoId(videoId);
	}

	@Override
	//rollbackFor = Exception.class：所有异常都触发回滚
	@GlobalTransactional(rollbackFor = Exception.class)
	public void saveVideoInfo(VideoInfoPost videoInfoPost, List<VideoInfoFilePost> uploadFileList) {
		// 校验文件分p数是否超过系统设定的数量上限
		if(uploadFileList.size() > redisComponent.getSysSettingDto().getVideoPCount()){
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		//	能判断成功说明有videoId，有id意味着修改操作
		if(!StringTools.isEmpty(videoInfoPost.getVideoId())){
			VideoInfoPost videoInfoPostDb = this.videoInfoPostMapper.selectByVideoId(videoInfoPost.getVideoId());
			if(videoInfoPostDb == null){	// 校验数据库中是否存在该视频（防止修改不存在的视频）
				throw new BusinessException(ResponseCodeEnum.CODE_600);
			}
			if(ArrayUtils.contains(	// 校验视频状态（STATUS0/STATUS2不允许修改，比如“转码中”/“待审核”的视频不能改）
					new Integer[]{VideoStatusEnum.STATUS0.getStatus(), VideoStatusEnum.STATUS2.getStatus()},
					videoInfoPostDb.getStatus())){
				throw  new BusinessException(ResponseCodeEnum.CODE_600);
			}
		}

		Date curDate = new Date();
		String videoId = videoInfoPost.getVideoId();
		List<VideoInfoFilePost> deleteFileList = new ArrayList<>();	// 要删除的文件列表
		List<VideoInfoFilePost> addFileList = uploadFileList;	// 要新增的文件列表

		if(StringTools.isEmpty(videoId)){	// 分支1：videoId为空，表示新增视频
			videoId = StringTools.getRandomString(Constants.LENGTH_15);
			videoInfoPost.setVideoId(videoId);	// 设置生成的VideoId
			videoInfoPost.setCreateTime(curDate);	// 设置创建时间
			videoInfoPost.setLastUpdateTime(curDate);	// 设置最后更新时间
			videoInfoPost.setStatus(VideoStatusEnum.STATUS0.getStatus());	// 新增默认状态：STATUS0（转码中）
			this.videoInfoPostMapper.insert(videoInfoPost);	// 插入视频主表

			// 新增分支：仅处理新文件，无旧文件补全逻辑
			Integer index = 1;
			for(VideoInfoFilePost videoInfoFile : uploadFileList){
				videoInfoFile.setFileIndex(index++);
				videoInfoFile.setVideoId(videoId);
				videoInfoFile.setUserId(videoInfoPost.getUserId());
				if(videoInfoFile.getFileId() == null){
					// 新文件：生成20位唯一FileId
					videoInfoFile.setFileId(StringTools.getRandomString(Constants.LENGTH_20));
					videoInfoFile.setUpdateType(VideoFileUpdateTypeEnum.UPDATE.getStatus());
					videoInfoFile.setTransferResult(VideoFileTransferResultEnum.TRANSFER.getStatus());
				}
			}
		}else{	// 分支2：videoId不为空 ，表示修改视频
			// 查询数据库中该视频已关联的文件列表
			VideoInfoFilePostQuery fileQuery = new VideoInfoFilePostQuery();
			fileQuery.setVideoId(videoId);
			fileQuery.setUserId(videoInfoPost.getUserId());	// 加用户ID，防止查别人的文件
			List<VideoInfoFilePost> dbInfoFileList = this.videoInfoFilePostMapper.selectList(fileQuery);

			//	把上传的文件列表转成Map（key=uploadId）
			Map<String, VideoInfoFilePost> uploadFileMap = uploadFileList.stream().collect(
					Collectors.toMap(item -> item.getUploadId(), // 这里先保持原写法，若仍爆红再改
							Function.identity(),
							(data1, data2) -> data2)
			);

			Boolean updateFileName = false;
			// 比对数据库文件和上传文件：筛选要删除的文件、判断文件名是否修改
			for(VideoInfoFilePost fileInfo : dbInfoFileList){
				VideoInfoFilePost updateFile = uploadFileMap.get(fileInfo.getUploadId());
				if(updateFile == null){	// 数据库有、上传列表无 → 加入删除列表
					deleteFileList.add(fileInfo);
				}else if(!updateFile.getFileName().equals(fileInfo.getFileName())){	// 文件名不一致 → 标记“文件名已修改(true)”
					updateFileName = true;
				}
			}

			// 筛选要新增的文件（fileId为空=新文件）
			addFileList = uploadFileList.stream().filter(item -> item.getFileId() == null).collect(Collectors.toList());
			// 设置最后更新时间
			videoInfoPost.setLastUpdateTime(curDate);
			// 判断视频基本信息（标题/封面/标签/简介）是否修改
			Boolean changeVideoInfo = this.changeVideoInfo(videoInfoPost);
			// 动态更新视频状态
			if(addFileList != null && !addFileList.isEmpty()){
				videoInfoPost.setStatus(VideoStatusEnum.STATUS0.getStatus());
			}else if(changeVideoInfo || updateFileName){
				videoInfoPost.setStatus(VideoStatusEnum.STATUS2.getStatus());
			}
			// 更新视频的发布表
			this.videoInfoPostMapper.updateByVideoId(videoInfoPost, videoInfoPost.getVideoId());

			// ============== 修改分支：文件处理 + 旧文件补全 ==============
			Integer index = 1;
			for(VideoInfoFilePost videoInfoFile : uploadFileList){
				videoInfoFile.setFileIndex(index++);
				videoInfoFile.setVideoId(videoId);
				videoInfoFile.setUserId(videoInfoPost.getUserId());
				if(videoInfoFile.getFileId() == null){
					// 新文件：原有逻辑
					videoInfoFile.setFileId(StringTools.getRandomString(Constants.LENGTH_20));
					videoInfoFile.setUpdateType(VideoFileUpdateTypeEnum.UPDATE.getStatus());
					videoInfoFile.setTransferResult(VideoFileTransferResultEnum.TRANSFER.getStatus());
				}else {
					// 旧文件：从数据库补全字段
					Optional<VideoInfoFilePost> optionalFile = dbInfoFileList.stream()
							.filter(f -> f.getUploadId().equals(videoInfoFile.getUploadId()))
							.findFirst();

					if (!optionalFile.isPresent()) {
						throw new BusinessException(ResponseCodeEnum.CODE_600);
					}
					VideoInfoFilePost dbFile = optionalFile.get();

					// 补全字段，防止null覆盖
					videoInfoFile.setFileSize(dbFile.getFileSize());
					videoInfoFile.setFilePath(dbFile.getFilePath());
					videoInfoFile.setDuration(dbFile.getDuration());
					videoInfoFile.setTransferResult(dbFile.getTransferResult());
					videoInfoFile.setUpdateType(dbFile.getUpdateType());
				}
			}
		}

		if(!deleteFileList.isEmpty()){
			// 提取要删除的文件ID，批量删除数据库记录
			List<String> delFileList = deleteFileList.stream().map(item -> item.getFileId()).collect(Collectors.toList());
			this.videoInfoFilePostMapper.deleteBatchByFileId(delFileList, videoInfoPost.getUserId());

			// 提取要删除的文件路径，推到Redis队列（异步删除文件）
			List<String> delFilePathList = deleteFileList.stream().map(item -> item.getFilePath()).collect(Collectors.toList());
			redisComponent.addFile2DelQueue(videoId, delFilePathList);
		}

		// 批量插入/更新文件关联表
		this.videoInfoFilePostMapper.insertOrUpdateBatch(uploadFileList);

		if(addFileList != null &&!addFileList.isEmpty()){
			for(VideoInfoFilePost file : addFileList){
				file.setUserId(videoInfoPost.getUserId());
				file.setVideoId(videoId);
			}
			// 把新增文件推到Redis转码队列
			redisComponent.addFile2TransferQueue(addFileList);
		}
	}

	private Boolean changeVideoInfo(VideoInfoPost videoInfoPost){
		// 查询数据库中的旧信息
		VideoInfoPost dbInfo = this.videoInfoPostMapper.selectByVideoId(videoInfoPost.getVideoId());
		//	判断标题、封面、标签、简介、分区、类型、视频来源说明是否有更改。
		if(!videoInfoPost.getVideoName().equals(dbInfo.getVideoName()) ||
				!videoInfoPost.getVideoCover().equals(dbInfo.getVideoCover()) ||
				!videoInfoPost.getTags().equals(dbInfo.getTags()) ||
                !(videoInfoPost.getPostType() == dbInfo.getPostType()) ||
				!(videoInfoPost.getCategoryId() == dbInfo.getCategoryId()) ||
				!(videoInfoPost.getpCategoryId() == dbInfo.getpCategoryId()) ||
				!videoInfoPost.getOriginInfo().equals(dbInfo.getOriginInfo()) ||
				!videoInfoPost.getIntroduction().equals(dbInfo.getIntroduction() == null ? "" : dbInfo.getIntroduction())
		){
			return true;	// 有修改
		}else{
			return false;	// 无修改
		}
	}

	@Override
	@GlobalTransactional(rollbackFor = Exception.class)
	public void auditVideo(String videoId, Integer status, String reason){
		VideoStatusEnum videoStatusEnum = VideoStatusEnum.getByStatus(status);
		if(videoStatusEnum == null){
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		VideoInfoPost videoInfoPost = new VideoInfoPost();
		videoInfoPost.setStatus(status);

		VideoInfoPostQuery videoInfoPostQuery = new VideoInfoPostQuery();
		videoInfoPostQuery.setStatus(VideoStatusEnum.STATUS2.getStatus());
		videoInfoPostQuery.setVideoId(videoId);
		//	判断审核的文件其状态为“待审核”
		Integer auditCount = this.videoInfoPostMapper.updateByParam(videoInfoPost, videoInfoPostQuery);
		//	返回的结果为0 -> 不是待审核 -> 给出报错
		if(auditCount == 0){
			throw new BusinessException("审核失败，请稍后重试");
		}

		VideoInfoFilePost videoInfoFilePost = new VideoInfoFilePost();
		videoInfoFilePost.setUpdateType(VideoFileUpdateTypeEnum.NO_UPDATE.getStatus());

		VideoInfoFilePostQuery filePostQuery = new VideoInfoFilePostQuery();
		filePostQuery.setVideoId(videoId);
		this.videoInfoFilePostMapper.updateByParam(videoInfoFilePost, filePostQuery);

		if(videoStatusEnum == VideoStatusEnum.STATUS4){
			return;
		}

		VideoInfoPost infoPost = this.videoInfoPostMapper.selectByVideoId(videoId);

		VideoInfo dbVideoInfo = this.videoInfoPostMapper.selectByVideoId(videoId);
		if(dbVideoInfo == null){
			SysSettingDto sysSettingDto = redisComponent.getSysSettingDto();
			//	给用户加硬币
			userInfoMapper.updateCoinCountInfo(infoPost.getUserId(), sysSettingDto.getPostVideoCoinCount());
		}
		//	更新发布信息到正式表
		VideoInfo videoInfo = CopyTools.copy(infoPost, VideoInfo.class);
		this.videoInfoMapper.insertOrUpdate(videoInfo);

		//	更新视频信息到正式表。采用先删除，在添加的方式
		VideoInfoFileQuery videoInfoFileQuery = new VideoInfoFileQuery();
		videoInfoFileQuery.setVideoId(videoId);
		this.videoInfoFileMapper.deleteByParam(videoInfoFileQuery);
		VideoInfoFilePostQuery videoInfoFilePostQuery = new VideoInfoFilePostQuery();
		videoInfoFilePostQuery.setVideoId(videoId);
		List<VideoInfoFilePost> videoInfoFilePostList = this.videoInfoFilePostMapper.selectList(videoInfoFilePostQuery);

		List<VideoInfoFile> videoInfoFileList = CopyTools.copyList(videoInfoFilePostList, VideoInfoFile.class);
		this.videoInfoFileMapper.insertBatch(videoInfoFileList);

		/**
		 * 删除文件
		 */
		List<String> filePathList = redisComponent.getDelFileList(videoId);
		if(filePathList != null){
			for(String path : filePathList){
				File file = new File(appConfig.getProjectFolder() + Constants.FILE_FOLDER + path);
				if(file.exists()){
                    try {
                        FileUtils.deleteDirectory(file);
                    } catch (IOException e) {
                        log.error("删除文件失败", e);
                    }
                }
			}
		}
		redisComponent.cleanDelFileList(videoId);


		/**	因为该方法加了@Transactional注解，因此先写数据库部分，再写es部分可以保证二者数据的一致性
		 * 	先写es再写数据库：es写入成功 -> 数据库写入失败 -> 数据库触发回滚		此时es中的数据无法回滚，数据不一致
		 * 	先写数据库再写es：数据库写入失败 -> 触发回滚	此时es的写入语句不执行，数据一致
		 */
		esSearchComponent.saveDoc(videoInfo);
	}

	@Override
	@GlobalTransactional(rollbackFor = Exception.class)
	public void transferVideoFile4Db(String videoId, String fileId, String userId, VideoInfoFilePost videoInfoFilePost) {
		videoInfoFilePostMapper.updateByUploadIdAndUserId(videoInfoFilePost, videoInfoFilePost.getUploadId(), videoInfoFilePost.getUserId());

		VideoInfoFilePostQuery filePostQuery = new VideoInfoFilePostQuery();
		filePostQuery.setVideoId(videoInfoFilePost.getVideoId());
		filePostQuery.setTransferResult(VideoFileTransferResultEnum.FAIL.getStatus());
		Integer failCount = videoInfoFilePostMapper.selectCount(filePostQuery);
		if(failCount > 0){
			VideoInfoPost videoUpdate = new VideoInfoPost();
			videoUpdate.setStatus(VideoStatusEnum.STATUS1.getStatus());
			videoInfoPostMapper.updateByVideoId(videoUpdate, videoInfoFilePost.getVideoId());
			return;
		}
		filePostQuery.setTransferResult(VideoFileTransferResultEnum.TRANSFER.getStatus());
		Integer transferCount = videoInfoFilePostMapper.selectCount(filePostQuery);
		if(transferCount == 0){
			Integer duration = videoInfoFilePostMapper.sumDuration(videoInfoFilePost.getVideoId());
			VideoInfoPost videoUpdate = new VideoInfoPost();
			videoUpdate.setStatus(VideoStatusEnum.STATUS2.getStatus());
			videoUpdate.setDuration(duration);
			videoInfoPostMapper.updateByVideoId(videoUpdate, videoInfoFilePost.getVideoId());
		}
	}
}