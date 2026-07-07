package com.sirinsirin.component;

import com.sirinsirin.entity.config.AppConfig;
import com.sirinsirin.entity.dto.VideoInfoEsDto;
import com.sirinsirin.entity.enums.PageSize;
import com.sirinsirin.entity.enums.SearchOrderTypeEnum;
import com.sirinsirin.entity.po.UserInfo;
import com.sirinsirin.entity.po.VideoInfo;
import com.sirinsirin.entity.query.SimplePage;
import com.sirinsirin.entity.query.UserInfoQuery;
import com.sirinsirin.entity.vo.PaginationResultVO;
import com.sirinsirin.exception.BusinessException;
import com.sirinsirin.mappers.UserInfoMapper;
import com.sirinsirin.utils.CopyTools;
import com.sirinsirin.utils.JsonUtils;
import com.sirinsirin.utils.StringTools;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.xcontent.XContentType;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component("esSearchComponent")
@Slf4j
public class EsSearchComponent {
    @Resource
    private AppConfig appConfig;

    @Resource
    private RestHighLevelClient restHighLevelClient;

    @Resource
    private UserInfoMapper<UserInfo, UserInfoQuery> userInfoMapper;


    private Boolean isExistIndex() throws IOException {
        GetIndexRequest getIndexRequest = new GetIndexRequest(appConfig.getEsIndexVideoName());
        return restHighLevelClient.indices().exists(getIndexRequest, RequestOptions.DEFAULT);
    }

    public void createIndex() {
        try {
            if (isExistIndex()) {
                return;
            }

            CreateIndexRequest request = new CreateIndexRequest(appConfig.getEsIndexVideoName());
            request.settings("{\"analysis\": {\n" +
                    "        \"analyzer\": {\n" +
                    "            \"comma\": {\n" +
                    "                \"type\": \"pattern\",\n" +
                    "                \"pattern\": \",\"\n" +
                    "            }\n" +
                    "        }\n" +
                    "    }}", XContentType.JSON);
            request.mapping("{\"properties\": {\n" +
                    "        \"videoId\": {\n" +
                    "            \"type\": \"text\",\n" +
                    "            \"index\": false\n" +
                    "        },\n" +
                    "        \"userId\": {\n" +
                    "            \"type\": \"text\",\n" +
                    "            \"index\": false\n" +
                    "        },\n" +
                    "        \"videoCover\": {\n" +
                    "            \"type\": \"text\",\n" +
                    "            \"index\": false\n" +
                    "        },\n" +
                    "        \"videoName\": {\n" +
                    "            \"type\": \"text\",\n" +
                    "            \"analyzer\": \"ik_max_word\"\n" +
                    "        },\n" +
                    "        \"tags\": {\n" +
                    "            \"type\": \"text\",\n" +
                    "            \"analyzer\": \"comma\"\n" +
                    "        },\n" +
                    "        \"playCount\": {\n" +
                    "            \"type\": \"integer\",\n" +
                    "            \"index\": false\n" +
                    "        },\n" +
                    "        \"danmuCount\": {\n" +
                    "            \"type\": \"integer\",\n" +
                    "            \"index\": false\n" +
                    "        },\n" +"        \"collectCount\": {\n" +
                    "            \"type\": \"integer\",\n" +
                    "            \"index\": false\n" +
                    "        },\n" +
                    "        \"createTime\": {\n" +
                    "            \"type\": \"date\",\n" +
                    "            \"format\": \"yyyy-MM-dd HH:mm:ss\",\n" +
                    "            \"index\": false\n" +
                    "        }\n" +
                    "    }}", XContentType.JSON);

            CreateIndexResponse createIndexResponse = restHighLevelClient.indices().create(request, RequestOptions.DEFAULT);
            Boolean acknowledged = createIndexResponse.isAcknowledged();
            if (!acknowledged) {
                throw new BusinessException("初始化es失败");
            }
//            System.out.println("=================初始化es成功======================");
        } catch (Exception e) {
            log.error("初始化es失败", e);
            throw new BusinessException("初始化es失败");
        }
    }

    public void saveDoc(VideoInfo videoInfo) {
        try {
            //  如果存在id则做更新操作
            if(docExist(videoInfo.getVideoId())){
                updateDoc(videoInfo);
            }else{  //  否则做插入操作
                VideoInfoEsDto videoInfoEsDto = CopyTools.copy(videoInfo, VideoInfoEsDto.class);
                videoInfoEsDto.setCollectCount(0);
                videoInfoEsDto.setPlayCount(0);
                videoInfoEsDto.setDanmuCount(0);
                IndexRequest request = new IndexRequest(appConfig.getEsIndexVideoName());
                request.id(videoInfo.getVideoId()).source(JsonUtils.convertObj2Json(videoInfoEsDto), XContentType.JSON);
                restHighLevelClient.index(request, RequestOptions.DEFAULT);
            }
        } catch (Exception e) {
            log.error("保存到es失败", e);
            throw new BusinessException("保存到es失败");
        }
    }

    private Boolean docExist(String id) throws IOException {
        GetRequest getRequest = new GetRequest(appConfig.getEsIndexVideoName(), id);
        GetResponse response = restHighLevelClient.get(getRequest, RequestOptions.DEFAULT);
        return response.isExists();
    }

    private void updateDoc(VideoInfo videoInfo) {
        try {
            videoInfo.setLastUpdateTime(null);
            videoInfo.setCreateTime(null);

            Map<String, Object> dataMap = new HashMap<>();
            Field[] fields = videoInfo.getClass().getDeclaredFields();
            for (Field field : fields) {
                String methodName = "get" + StringTools.upperCaseFirstLetter(field.getName());
                Method method = videoInfo.getClass().getMethod(methodName);
                Object object = method.invoke(videoInfo);
                if (
                        object != null && object instanceof String && !StringTools.isEmpty(object.toString()) ||
                        object != null && !(object instanceof String)
                ) {
                    dataMap.put(field.getName(), object);
                }
            }
            if (dataMap.isEmpty()) {
                return;
            }

            UpdateRequest updateRequest = new UpdateRequest(appConfig.getEsIndexVideoName(), videoInfo.getVideoId());
            updateRequest.doc(dataMap);
            restHighLevelClient.update(updateRequest, RequestOptions.DEFAULT);
        } catch (Exception e) {
            log.error("es更新视频失败", e);
            throw new BusinessException("保存视频失败");
        }
    }

    public void updateDocCount(String videoId, String fieldName, Integer count) {
        try {
            UpdateRequest updateRequest = new UpdateRequest(appConfig.getEsIndexVideoName(), videoId);
            Script script = new Script(ScriptType.INLINE, "painless",  "ctx._source." + fieldName + " += params.count", Collections.singletonMap("count", count));
            updateRequest.script(script);
            restHighLevelClient.update(updateRequest, RequestOptions.DEFAULT);
        } catch (Exception e) {
            log.error("更新数据到es失败", e);
            throw new BusinessException("更新数据到es失败");
        }
    }

    public void delDoc(String videoId) {
        DeleteRequest deleteRequest = new DeleteRequest(appConfig.getEsIndexVideoName(), videoId);
        try {
            restHighLevelClient.delete(deleteRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            log.error("从es删除视频失败", e);
            throw new BusinessException("删除视频失败");
        }
    }

    public PaginationResultVO<VideoInfo> search(Boolean highlight, String keyword, Integer orderType, Integer pageNo, Integer pageSize){
        try {
            SearchOrderTypeEnum searchOrderTypeEnum = SearchOrderTypeEnum.getByType(orderType);
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            // ====================== 替换这里 ======================
            // 原有代码注释掉
            // searchSourceBuilder.query(QueryBuilders.multiMatchQuery(keyword, "videoName", "tags"));

            // 新代码：保留comma分词，同时解决匹配问题
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
            // 1. 视频名称：原有全文搜索逻辑（ik分词）
            boolQuery.should(QueryBuilders.matchQuery("videoName", keyword));
            // 2. 标签字段：查询时使用comma解析关键词
            boolQuery.should(QueryBuilders.matchQuery("tags", keyword).analyzer("comma"));
            // 至少匹配一个字段即可返回结果
            boolQuery.minimumShouldMatch(1);
            // 赋值查询条件
            searchSourceBuilder.query(boolQuery);

            //高亮
            if (highlight) {
                HighlightBuilder highlightBuilder = new HighlightBuilder();
                highlightBuilder.field("videoName");
                highlightBuilder.preTags("<span class='highlight'>");
                highlightBuilder.postTags("</span>");
                searchSourceBuilder.highlighter(highlightBuilder);
            }

            //排序
            searchSourceBuilder.sort("_score", SortOrder.ASC);
            if (orderType != null) {
                searchSourceBuilder.sort(searchOrderTypeEnum.getField(), SortOrder.DESC);
            }

            pageNo = pageNo == null ? 1 : pageNo;
            pageSize = pageSize == null ? PageSize.SIZE20.getSize() : pageSize;
            searchSourceBuilder.size(pageSize);
            searchSourceBuilder.from((pageNo - 1) * pageSize);

            SearchRequest searchRequest = new SearchRequest(appConfig.getEsIndexVideoName());
            searchRequest.source(searchSourceBuilder);

            SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);

            SearchHits hits = searchResponse.getHits();
            Integer totalCount = (int) hits.getTotalHits().value;

            List<VideoInfo> videoInfoList = new ArrayList<>();
            List<String> userIdList = new ArrayList<>();

            for (SearchHit hit : hits.getHits()) {
                VideoInfo videoInfo = JsonUtils.convertJson2Obj(hit.getSourceAsString(), VideoInfo.class);
                if (hit.getHighlightFields().get("videoName") != null) {
                    videoInfo.setVideoName(hit.getHighlightFields().get("videoName").fragments()[0].string());
                }
                videoInfoList.add(videoInfo);
                userIdList.add(videoInfo.getUserId());
            }

            UserInfoQuery userInfoQuery = new UserInfoQuery();
            userInfoQuery.setUserIdList(userIdList);
            List<UserInfo> userInfoList = userInfoMapper.selectList(userInfoQuery);
            Map<String, UserInfo> userInfoMap = userInfoList.stream().collect(
                    Collectors.toMap(
                            item -> item.getUserId(), Function.identity(), (data1, data2) -> data2
                    )
            );

            videoInfoList.forEach(item -> {
                UserInfo userInfo = userInfoMap.get(item.getUserId());
                item.setNickName(userInfo == null ? "" : userInfo.getNickName());
            });

            SimplePage page = new SimplePage(pageNo, totalCount, pageSize);
            PaginationResultVO<VideoInfo> resultVO = new PaginationResultVO<>(
                    totalCount,
                    page.getPageSize(),
                    page.getPageNo(),
                    page.getPageTotal(),
                    videoInfoList
            );

            return resultVO;
        }catch(Exception e){
            log.error("查询视频到es失败", e);
            throw new BusinessException("查询失败");
        }
    }
}
