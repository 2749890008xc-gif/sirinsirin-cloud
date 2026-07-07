package com.sirinsirin.controller;

import com.sirinsirin.component.RedisComponent;
import com.sirinsirin.entity.config.AppConfig;
import com.sirinsirin.entity.constants.Constants;
import com.sirinsirin.entity.vo.ResponseVO;
import com.sirinsirin.exception.BusinessException;
import com.sirinsirin.utils.StringTools;
import com.wf.captcha.ArithmeticCaptcha;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotEmpty;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户信息 Controller
 */
@RestController
@RequestMapping("/account")
@Validated
public class AccountController extends ABaseController{

    @Resource
    private RedisComponent redisComponent;

    @Resource
    private AppConfig appConfig;

    @RequestMapping("/checkCode")
    public ResponseVO checkCode(){
        ArithmeticCaptcha captcha = new ArithmeticCaptcha(100, 42);
        String code = captcha.text();	//code的值是验证码的结果，比如验证码图片显示“2-9”，code就等于-7
        String checkCodeKey = redisComponent.saveCheckCode(code);
        String checkCodeBase64 = captcha.toBase64();
        Map<String, String> result = new HashMap<>();
        result.put("checkCode", checkCodeBase64);
        result.put("checkCodeKey", checkCodeKey);
        return getSuccessResponseVO(result);
    }

    /**登录模块*/
    @RequestMapping("/login")
    public ResponseVO login(HttpServletRequest request,
                            HttpServletResponse response,
                            @NotEmpty String account,
                            @NotEmpty String password,
                            @NotEmpty String checkCodeKey,
                            @NotEmpty String checkCode){
        try {
            if(!checkCode.equalsIgnoreCase(redisComponent.getCheckCode(checkCodeKey))){	//	传入key以获取对应的value
                throw new BusinessException("图片验证码不正确");
            }
            if(!account.equals(appConfig.getAdminAccount()) || !password.equals(StringTools.encodeByMd5(appConfig.getAdminPassword()))){
                throw new BusinessException("账号或密码错误");
            }
            String token = redisComponent.savaTokenInfo4Admin(account);
            saveToken2Cookie(response, token);

            return getSuccessResponseVO(account);
        }finally {
            redisComponent.cleanCheckCode(checkCodeKey);

            //	每次获取新的token之前先清除原来的token
            Cookie[] cookies = request.getCookies();
            //	说明：退出登录后会把所有的cookie都删除，包括上次和上上次登录的cookie，而每次登录时需要先清理上次的token，故而报非空异常，增加if判断防止非空异常
            if(cookies != null){
                String token = null;
                for(Cookie cookie : cookies){
                    if(cookie.getName().equals(Constants.TOKEN_ADMIN)){
                        token = cookie.getValue();
                    }
                }
                if(!StringTools.isEmpty(token)){
                    redisComponent.cleanToken4Admin(token);
                }
            }
        }
    }

    /**退出登录模块*/
    @RequestMapping("/logout")
    public ResponseVO logout(HttpServletResponse response){
        cleanCookie(response);
        return getSuccessResponseVO("退出登录成功");
    }

    /**关注、粉丝、硬币数量模块*/
    @RequestMapping("/getUserCountInfo")	//	这个方法后期需要修改
    public ResponseVO getUserCountInfo(){
        return getSuccessResponseVO("这个方法目前没有教，是我自己写的！！");
    }
}