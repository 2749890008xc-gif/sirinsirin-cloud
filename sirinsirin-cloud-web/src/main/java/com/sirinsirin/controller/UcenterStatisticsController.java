package com.sirinsirin.controller;

import com.sirinsirin.entity.dto.TokenUserInfoDto;
import com.sirinsirin.entity.po.StatisticsInfo;
import com.sirinsirin.entity.query.StatisticsInfoQuery;
import com.sirinsirin.entity.vo.ResponseVO;
import com.sirinsirin.service.StatisticsInfoService;
import com.sirinsirin.utils.DateUtil;
import com.sirinsirin.annotation.GlobalInterceptor;
import lombok.extern.slf4j.Slf4j;
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
@RequestMapping("/ucenter")
@Validated
@Slf4j
public class UcenterStatisticsController extends ABaseController {

    @Resource
    private StatisticsInfoService statisticsInfoService;

    @RequestMapping("/getActualTimeStatisticsInfo")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO getActualTimeStatisticsInfo() {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
        String preDate = DateUtil.getBeforeDayDate(1);
        StatisticsInfoQuery param = new StatisticsInfoQuery();
        param.setStatisticsDate(preDate);
        param.setUserId(tokenUserInfoDto.getUserId());

        List<StatisticsInfo> preDayData = statisticsInfoService.findListByParam(param);
        Map<Integer, Integer> preDayDataMap = preDayData.stream()
                .collect(Collectors.toMap(
                        StatisticsInfo::getDataType,
                        StatisticsInfo::getStatisticsCount,
                        (item1, item2) -> item2
                ));

        Map<String, Integer> totalCountInfo = statisticsInfoService.getStatisticsInfoActualTime(tokenUserInfoDto.getUserId());
        Map<String, Object> result = new HashMap<>();
        result.put("preDayData", preDayDataMap);
        result.put("totalCountInfo", totalCountInfo);
        return getSuccessResponseVO(result);
    }

    @RequestMapping("/getWeekStatisticsInfo")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO getWeekStatisticsInfo(Integer dataType) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();

        List<String> dateList = DateUtil.getBeforeDates(7);

        StatisticsInfoQuery param = new StatisticsInfoQuery();
        param.setDataType(dataType);
        param.setUserId(tokenUserInfoDto.getUserId());
        param.setStatisticsDateStart(dateList.get(0));
        param.setStatisticsDateEnd(dateList.get(dateList.size() - 1));
        param.setOrderBy("statistics_date asc");
        List<StatisticsInfo> statisticsInfoList = statisticsInfoService.findListByParam(param);

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
        return getSuccessResponseVO(resultDataList);
    }
}
