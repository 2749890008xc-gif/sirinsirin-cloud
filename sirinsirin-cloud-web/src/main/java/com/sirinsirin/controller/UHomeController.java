package com.sirinsirin.controller;

import com.sirinsirin.entity.constants.Constants;
import com.sirinsirin.entity.dto.TokenUserInfoDto;
import com.sirinsirin.entity.enums.PageSize;
import com.sirinsirin.entity.enums.UserActionTypeEnum;
import com.sirinsirin.entity.enums.VideoOrderTypeEnum;
import com.sirinsirin.entity.po.UserInfo;
import com.sirinsirin.entity.query.UserActionQuery;
import com.sirinsirin.entity.query.UserFocusQuery;
import com.sirinsirin.entity.query.VideoInfoQuery;
import com.sirinsirin.entity.vo.PaginationResultVO;
import com.sirinsirin.entity.vo.ResponseVO;
import com.sirinsirin.entity.vo.UserInfoVO;
import com.sirinsirin.service.UserFocusService;
import com.sirinsirin.service.UserInfoService;
import com.sirinsirin.service.VideoInfoService;
import com.sirinsirin.utils.CopyTools;
import com.sirinsirin.annotation.GlobalInterceptor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.constraints.*;

@RestController
@RequestMapping("/uhome")
@Validated
public class UHomeController extends ABaseController {
    @Resource
    private UserInfoService userInfoService;

    @Resource
    private VideoInfoService videoInfoService;

    @Resource
    private UserFocusService userFocusService;

    @RequestMapping("/getUserInfo")
    public ResponseVO getUserInfo(@NotEmpty String userId) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
        UserInfo userInfo = userInfoService.getUserDetailInfo(tokenUserInfoDto == null ? null : tokenUserInfoDto.getUserId(), userId);
        UserInfoVO userInfoVO = CopyTools.copy(userInfo, UserInfoVO.class);

        return getSuccessResponseVO(userInfoVO);
    }

    @RequestMapping("/updateUserInfo")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO updateUserInfo(@NotEmpty @Size(max = 20) String nickName,
                                     @NotEmpty @Size(max = 100) String avatar,
                                     @NotNull Integer sex,
                                     @Size(max = 10) String birthday,
                                     @Size(max = 150) String school,
                                     @Size(max = 80) String personIntroduction,
                                     @Size(max = 300) String noticeInfo) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(tokenUserInfoDto.getUserId());
        userInfo.setNickName(nickName);
        userInfo.setAvatar(avatar);
        userInfo.setSex(sex);
        userInfo.setBirthday(birthday);
        userInfo.setSchool(school);
        userInfo.setPersonIntroduction(personIntroduction);
        userInfo.setNoticeInfo(noticeInfo);

        userInfoService.updateUserInfo(userInfo, tokenUserInfoDto);

        return getSuccessResponseVO("调用updateUserInfo接口成功！");
    }

    @RequestMapping("/saveTheme")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO saveTheme(@Min(1) @Max(10) @NotNull Integer theme) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
        UserInfo userInfo = new UserInfo();
        userInfo.setTheme(theme);
        userInfoService.updateUserInfoByUserId(userInfo, tokenUserInfoDto.getUserId());
        return getSuccessResponseVO("调用saveTheme接口成功！");
    }

    @RequestMapping("/focus")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO focus(@NotEmpty String focusUserId) {
        userFocusService.focusUser(getTokenUserInfoDto().getUserId(), focusUserId);
        return getSuccessResponseVO("调用focus接口成功！");
    }

    @RequestMapping("/cancelFocus")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO cancelFocus(@NotEmpty String focusUserId) {
        userFocusService.cancelFocus(getTokenUserInfoDto().getUserId(), focusUserId);
        return getSuccessResponseVO("调用cancelFocus接口成功！");
    }

    //  查看关注列表（我关注的用户）
    @RequestMapping("/loadFocusList")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO loadFocusList(Integer pageNo) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
        UserFocusQuery focusQuery = new UserFocusQuery();
        focusQuery.setUserId(tokenUserInfoDto.getUserId());
        focusQuery.setPageNo(pageNo);
        focusQuery.setOrderBy("focus_time desc");
        focusQuery.setQueryType(Constants.ZERO);
        PaginationResultVO resultVO = userFocusService.findListByPage(focusQuery);
        return getSuccessResponseVO(resultVO);
    }

    //  查看粉丝列表（关注我的用户）
    @RequestMapping("/loadFansList")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO loadFansList(Integer pageNo) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
        UserFocusQuery focusQuery = new UserFocusQuery();
        focusQuery.setFocusUserId(tokenUserInfoDto.getUserId());
        focusQuery.setPageNo(pageNo);
        focusQuery.setOrderBy("focus_time desc");
        focusQuery.setQueryType(Constants.ONE);
        PaginationResultVO resultVO = userFocusService.findListByPage(focusQuery);
        return getSuccessResponseVO(resultVO);
    }

    /**
     * @param type 表示 主页 或 投稿。主页只显示前10条记录
     * */
    @RequestMapping("/loadVideoList")
    public ResponseVO loadVideoList(@NotEmpty String userId,
                                    Integer type,
                                    Integer pageNo,
                                    String videoName,
                                    Integer orderType) {
        VideoInfoQuery infoQuery = new VideoInfoQuery();
        if(type != null){
            infoQuery.setPageSize(PageSize.SIZE10.getSize());
        }
        VideoOrderTypeEnum videoOrderTypeEnum = VideoOrderTypeEnum.getByType(orderType);
        if(videoOrderTypeEnum == null){
            videoOrderTypeEnum = VideoOrderTypeEnum.CREATE_TIME;
        }
        infoQuery.setOrderBy(videoOrderTypeEnum.getField() + " desc");
        infoQuery.setVideoNameFuzzy(videoName);
        infoQuery.setPageNo(pageNo);
        infoQuery.setUserId(userId);
        PaginationResultVO resultVO = videoInfoService.findListByPage(infoQuery);
        return getSuccessResponseVO(resultVO);
    }
}
