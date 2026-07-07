package com.sirinsirin.service.impl;

import com.sirinsirin.api.consumer.WebClient;
import com.sirinsirin.component.RedisComponent;
import com.sirinsirin.entity.config.AppConfig;
import com.sirinsirin.entity.constants.Constants;
import com.sirinsirin.entity.enums.PageSize;
import com.sirinsirin.entity.po.CategoryInfo;
import com.sirinsirin.entity.query.CategoryInfoQuery;
import com.sirinsirin.entity.query.SimplePage;
import com.sirinsirin.entity.query.VideoInfoQuery;
import com.sirinsirin.entity.vo.PaginationResultVO;
import com.sirinsirin.exception.BusinessException;
import com.sirinsirin.mappers.CategoryInfoMapper;
import com.sirinsirin.service.CategoryInfoService;
import com.sirinsirin.utils.StringTools;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * 分类信息 业务接口实现
 */
@Service("categoryInfoService")
public class CategoryInfoServiceImpl implements CategoryInfoService {
	//	创建异步线程池
	private static ExecutorService executorService = Executors.newFixedThreadPool(10);

	@Resource
	private CategoryInfoMapper<CategoryInfo, CategoryInfoQuery> categoryInfoMapper;

	@Resource
	private RedisComponent redisComponent;

//	@Resource
//	private VideoInfoService videoInfoService;

	@Resource
	private AppConfig appConfig;
    @Autowired
    private WebClient webClient;

	/**
	 * 根据条件查询列表
	 */
	@Override
	public List<CategoryInfo> findListByParam(CategoryInfoQuery param) {
		List<CategoryInfo> categoryInfoList = this.categoryInfoMapper.selectList(param);
		if(param.getConvert2Tree() != null && param.getConvert2Tree()){
			categoryInfoList = convertLine2Tree(categoryInfoList, Constants.ZERO);	//	因为一级分类的父分类id为0，所以这里从0开始
		}
		return categoryInfoList;
	}

	private List<CategoryInfo> convertLine2Tree(List<CategoryInfo> dataList, Integer pid){
		List<CategoryInfo> children = new ArrayList<>();
		for(CategoryInfo m : dataList){
			if(m.getCategoryId() != null && m.getpCategoryId() != null && m.getpCategoryId().equals(pid)){
				m.setChildren(convertLine2Tree(dataList, m.getCategoryId()));
				children.add(m);
			}
		}
		return children;
	}

	/**
	 * 根据条件查询列表
	 */
	@Override
	public Integer findCountByParam(CategoryInfoQuery param) {
		return this.categoryInfoMapper.selectCount(param);
	}

	/**
	 * 分页查询方法
	 */
	@Override
	public PaginationResultVO<CategoryInfo> findListByPage(CategoryInfoQuery param) {
		int count = this.findCountByParam(param);
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
		param.setSimplePage(page);
		List<CategoryInfo> list = this.findListByParam(param);
		PaginationResultVO<CategoryInfo> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	@Override
	public Integer add(CategoryInfo bean) {
		return this.categoryInfoMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	@Override
	public Integer addBatch(List<CategoryInfo> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.categoryInfoMapper.insertBatch(listBean);
	}

	/**
	 * 批量新增或者修改
	 */
	@Override
	public Integer addOrUpdateBatch(List<CategoryInfo> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.categoryInfoMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 多条件更新
	 */
	@Override
	public Integer updateByParam(CategoryInfo bean, CategoryInfoQuery param) {
		StringTools.checkParam(param);
		return this.categoryInfoMapper.updateByParam(bean, param);
	}

	/**
	 * 多条件删除
	 */
	@Override
	public Integer deleteByParam(CategoryInfoQuery param) {
		StringTools.checkParam(param);
		return this.categoryInfoMapper.deleteByParam(param);
	}

	/**
	 * 根据CategoryId获取对象
	 */
	@Override
	public CategoryInfo getCategoryInfoByCategoryId(Integer categoryId) {
		return this.categoryInfoMapper.selectByCategoryId(categoryId);
	}

	/**
	 * 根据CategoryId修改
	 */
	@Override
	public Integer updateCategoryInfoByCategoryId(CategoryInfo bean, Integer categoryId) {
		return this.categoryInfoMapper.updateByCategoryId(bean, categoryId);
	}

	/**
	 * 根据CategoryId删除
	 */
	@Override
	public Integer deleteCategoryInfoByCategoryId(Integer categoryId) {
		return this.categoryInfoMapper.deleteByCategoryId(categoryId);
	}

	/**
	 * 根据CategoryCode获取对象
	 */
	@Override
	public CategoryInfo getCategoryInfoByCategoryCode(String categoryCode) {
		return this.categoryInfoMapper.selectByCategoryCode(categoryCode);
	}

	/**
	 * 根据CategoryCode修改
	 */
	@Override
	public Integer updateCategoryInfoByCategoryCode(CategoryInfo bean, String categoryCode) {
		return this.categoryInfoMapper.updateByCategoryCode(bean, categoryCode);
	}

	/**
	 * 根据CategoryCode删除
	 */
	@Override
	public Integer deleteCategoryInfoByCategoryCode(String categoryCode) {
		return this.categoryInfoMapper.deleteByCategoryCode(categoryCode);
	}

	@Override
	public void saveCategory(CategoryInfo bean) throws IOException {
		CategoryInfo dbBean = this.categoryInfoMapper.selectByCategoryCode(bean.getCategoryCode());
		/**
		 * 第一种情况：管理员新增一个分类，但填写的分类编号已经存在的情况
		 * 第二种情况：管理员修改某个分类的编号时，该分类编号已经存在的情况
		 **/
		if(bean.getCategoryId() == null && dbBean != null ||
				bean.getCategoryId() != null && dbBean != null && !bean.getCategoryId().equals(dbBean.getCategoryId())) {
			throw new BusinessException("分类编号已经存在");
		}

		if(bean.getCategoryId() == null){
			Integer maxSort = this.categoryInfoMapper.selectMaxSort(bean.getpCategoryId());
			bean.setSort(maxSort + 1);
			this.categoryInfoMapper.insert(bean);
		}else{
			//把删除代码放进redis异步队列中
			executorService.execute(() -> {
				if(!bean.getIcon().equals(dbBean.getIcon())){
					//修改图标时先删除原图标
					String icon = dbBean.getIcon();
					FileUtils.deleteQuietly(
							new File(appConfig.getProjectFolder() +
									Constants.FILE_FOLDER + icon));
				}

				if(!bean.getBackground().equals(dbBean.getBackground())){
					//修改背景时先删除原背景
					String background = dbBean.getBackground();
					FileUtils.deleteQuietly(
							new File(appConfig.getProjectFolder() +
									Constants.FILE_FOLDER + background));
				}
			});

			this.categoryInfoMapper.updateByCategoryId(bean, bean.getCategoryId());
		}

		save2Redis();
	}

	@Override
	public void delCategory(Integer categoryId) {
		CategoryInfo dbBean = this.categoryInfoMapper.selectByCategoryId(categoryId);
		if(dbBean == null){
			throw new BusinessException("分类不存在！");
		}

		VideoInfoQuery videoInfoQuery = new VideoInfoQuery();
		videoInfoQuery.setCategoryIdOrPCategoryId(categoryId);
		Integer count = webClient.getVideoCount(videoInfoQuery);
		if (count > 0) {
			throw new BusinessException("分类下有视频信息，无法删除");
		}

		//	把删除代码放进redis异步队列中
		executorService.execute(() -> {
			//删除分类时要先删除图标
			if(dbBean.getIcon() != null){
				FileUtils.deleteQuietly(
						new File(appConfig.getProjectFolder() +
								Constants.FILE_FOLDER + dbBean.getIcon()));
			}
			//删除分类时要先删除背景
			if(dbBean.getBackground() != null){
				FileUtils.deleteQuietly(
						new File(appConfig.getProjectFolder() +
								Constants.FILE_FOLDER + dbBean.getBackground()));
			}
		});


		CategoryInfoQuery categoryInfoQuery = new CategoryInfoQuery();
		categoryInfoQuery.setCategoryIdOrPCategoryId(categoryId);
		categoryInfoMapper.deleteByParam(categoryInfoQuery);

		save2Redis();
	}

	@Override
	public void changeSort(Integer pCategoryId, String categoryIds) {
		String[] categoryIdArray = categoryIds.split(",");
		List<CategoryInfo> categoryInfoList = new ArrayList<>();
		Integer sort = 0;
		for(String categoryId : categoryIdArray){
			CategoryInfo categoryInfo = new CategoryInfo();
			categoryInfo.setCategoryId(Integer.parseInt(categoryId));
			categoryInfo.setpCategoryId(pCategoryId);
			//	TODO ++sort会导致每次更换排序，其排序号会越来越高，可以试着解决一下
			categoryInfo.setSort(++sort);
			categoryInfoList.add(categoryInfo);
		}
		categoryInfoMapper.updateSortBatch(categoryInfoList);

		save2Redis();
	}

	@Override
	public List<CategoryInfo> getAllCategoryList() {
		List<CategoryInfo> categoryInfoList = redisComponent.getCategoryList();
		if(categoryInfoList == null|| categoryInfoList.isEmpty()){
			save2Redis();
		}
		return redisComponent.getCategoryList();
	}

	private void save2Redis(){
		CategoryInfoQuery query = new CategoryInfoQuery();
		query.setOrderBy("sort asc");
		query.setConvert2Tree(true);
		List<CategoryInfo> categoryInfoList = findListByParam(query);
		redisComponent.saveCategoryList(categoryInfoList);
	}
}