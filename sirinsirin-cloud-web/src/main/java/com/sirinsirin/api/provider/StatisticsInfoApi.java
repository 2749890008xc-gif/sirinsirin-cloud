package com.sirinsirin.api.provider;

import com.sirinsirin.entity.constants.Constants;
import com.sirinsirin.entity.enums.StatisticsTypeEnum;
import com.sirinsirin.entity.po.StatisticsInfo;
import com.sirinsirin.entity.query.StatisticsInfoQuery;
import com.sirinsirin.entity.query.UserInfoQuery;
import com.sirinsirin.service.StatisticsInfoService;
import com.sirinsirin.service.UserInfoService;
import com.sirinsirin.utils.DateUtil;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping(Constants.INNER_API_PREFIX + "/statistics/admin")
@Validated
public class StatisticsInfoApi {

    @Resource
    private StatisticsInfoService statisticsInfoService;

    @Resource
    private UserInfoService userInfoService;

    @RequestMapping("/getActualTimeStatisticsInfo")
    public Map getActualTimeStatisticsInfo() {
        String preDate = DateUtil.getBeforeDayDate(1);
        StatisticsInfoQuery param = new StatisticsInfoQuery();
        param.setStatisticsDate(preDate);
        List<StatisticsInfo> preDayData = statisticsInfoService.findListTotalInfoByParam(param);

        Integer userCount = userInfoService.findCountByParam(new UserInfoQuery());
        preDayData.forEach(item -> {
            if (StatisticsTypeEnum.FANS.getType().equals(item.getDataType())) {
                item.setStatisticsCount(userCount);
            }
        });

        Map<Integer, Integer> preDayDataMap = preDayData.stream()
                .collect(Collectors.toMap(
                        StatisticsInfo::getDataType,
                        StatisticsInfo::getStatisticsCount,
                        (item1, item2) -> item2
                ));
        Map<String, Integer> totalCountInfo = statisticsInfoService.getStatisticsInfoActualTime(null);
        Map<String, Object> result = new HashMap<>();
        result.put("preDayData", preDayDataMap);
        result.put("totalCountInfo", totalCountInfo);
        return result;
    }

    @RequestMapping("/getWeekStatisticsInfo")
    public List<StatisticsInfo> getWeekStatisticsInfo(Integer dataType) {
        List<String> dateList = DateUtil.getBeforeDates(7);

        StatisticsInfoQuery param = new StatisticsInfoQuery();
        param.setDataType(dataType);
        param.setStatisticsDateStart(dateList.get(0));
        param.setStatisticsDateEnd(dateList.get(dateList.size() - 1));
        param.setOrderBy("statistics_date asc");

        List<StatisticsInfo> statisticsInfoList = null;
        if (!StatisticsTypeEnum.FANS.getType().equals(dataType)) {
            statisticsInfoList = statisticsInfoService.findListTotalInfoByParam(param);
        } else {
            statisticsInfoList = statisticsInfoService.findUserCountTotalInfoByParam(param);
        }

        Map<String, StatisticsInfo> dataMap = statisticsInfoList.stream()
                .collect(Collectors.toMap(
                        item -> item.getStatisticsDate(), Function.identity(),
                        (data1, data2) -> data2
                ));

        List<StatisticsInfo> resultDataList = new ArrayList<>();
        for (String date : dateList) {
            StatisticsInfo dataItem = dataMap.get(date);
            if (dataItem == null) {
                dataItem = new StatisticsInfo();
                dataItem.setStatisticsCount(0);
                dataItem.setStatisticsDate(date);
            }
            resultDataList.add(dataItem);
        }
        return resultDataList;
    }
}