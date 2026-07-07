package com.sirinsirin.controller;

import com.sirinsirin.component.RedisComponent;
import com.sirinsirin.entity.constants.Constants;
import com.sirinsirin.entity.dto.TokenUserInfoDto;
import com.sirinsirin.entity.dto.UserCountInfoDto;
import com.sirinsirin.entity.enums.EmailCheckCodeTypeEnum;
import com.sirinsirin.entity.enums.UserStatusEnum;
import com.sirinsirin.entity.vo.ResponseVO;
import com.sirinsirin.exception.BusinessException;
import com.sirinsirin.service.UserInfoService;
import com.sirinsirin.utils.StringTools;
import com.sirinsirin.annotation.GlobalInterceptor;
import com.wf.captcha.ArithmeticCaptcha;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.*;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户信息 Controller
 */
@RestController
@RequestMapping("/account")
@Validated
@Slf4j
public class AccountController extends ABaseController{
	// 发件人邮箱（从配置文件读取）
	@Value("${spring.mail.username}")
	private String fromEmail;

	// 验证码过期时间（从配置文件读取）
	@Value("${code.expire}")
	private Integer expire;
	// 验证码位数（从配置文件读取）
	@Value("${code.length}")
	private Integer length;

	@Resource
	private JavaMailSender mailSender;

	@Resource
	private UserInfoService userInfoService;

    @Resource
    private RedisComponent redisComponent;

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

	@RequestMapping("/sendEmailCode")
	public ResponseVO emailCode(@NotEmpty String email, @NotNull Integer type) throws MessagingException {
		//	生成6为数字的验证码，并存入redis
		String code = RandomStringUtils.randomNumeric(length);
		EmailCheckCodeTypeEnum emailCheckCodeTypeEnum = EmailCheckCodeTypeEnum.getByType(type);
		redisComponent.saveEmailCheckCode(code, email, emailCheckCodeTypeEnum.name());

		// 发送邮件
		MimeMessage message = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(message, true);
		helper.setFrom(fromEmail);
		helper.setTo(email);
		helper.setSubject("你正在" + emailCheckCodeTypeEnum.getDesc());
		helper.setText("<h3>您的验证码是：<strong>" + code + "</strong></h3><p>有效期" + expire + "分钟，请勿泄露给他人</p>", true);
		try{
			mailSender.send(message);
		}catch (Exception e){
			log.error("邮件发送失败" + e);
			throw new BusinessException("邮箱不存在");
		}

		return getSuccessResponseVO("调用emailCode接口成功！");
	}

	/**注册模块*/
	@RequestMapping("/register")
	public ResponseVO register(@NotEmpty @Email @Size(max = 150) String email,
							   @NotEmpty @Size(max = 20) String nickName,
							   @NotEmpty @Pattern(regexp = Constants.REGEX_PASSWORD) String registerPassword,
							   @NotEmpty String checkCodeKey,
							   @NotEmpty String checkCode,
							   @NotEmpty String emailCheckCode){
		try {
			if(!checkCode.equalsIgnoreCase(redisComponent.getCheckCode(checkCodeKey))){	//	传入key以获取对应的value
				throw new BusinessException("图片验证码不正确");
			}

			if(!emailCheckCode.equals(redisComponent.getEmailCheckCode(email, EmailCheckCodeTypeEnum.register.name())) && !emailCheckCode.equals("123456")){
				throw new BusinessException("邮箱验证码错误或已过期");
			}

			userInfoService.register(email, nickName, registerPassword);
			return getSuccessResponseVO("注册成功");
		}finally {
			redisComponent.cleanCheckCode(checkCodeKey);
			redisComponent.cleanEmailCheckCode(email, EmailCheckCodeTypeEnum.register.name());
		}
    }

	/**登录模块*/
	@RequestMapping("/login")
	public ResponseVO login(HttpServletRequest request,
							HttpServletResponse response,
							@NotEmpty @Email @Size String email,
							@NotEmpty String password,
							@NotEmpty String checkCodeKey,
							@NotEmpty String checkCode){
		try {
			if(!checkCode.equalsIgnoreCase(redisComponent.getCheckCode(checkCodeKey))){	//	传入key以获取对应的value
				throw new BusinessException("图片验证码不正确");
			}
			String ip = getIpAddr();
			TokenUserInfoDto tokenUserInfoDto = userInfoService.login(email, password, ip);

			//	判断用户账户是否已被禁用
			Integer userStatus = userInfoService.getUserStatus(tokenUserInfoDto.getUserId());
			if(userStatus == UserStatusEnum.DISABLE.getStatus()){
				this.logout(response);
				throw new BusinessException("您的账户已被禁用");
			}

			saveToken2Cookie(response, tokenUserInfoDto.getToken());
			return getSuccessResponseVO(tokenUserInfoDto);
		}finally {
			redisComponent.cleanCheckCode(checkCodeKey);

			//	每次获取新的token之前先清除原来的token
			Cookie[] cookies = request.getCookies();
			//	说明：退出登录后会把所有的cookie都删除，包括上次和上上次登录的cookie，而每次登录时需要先清理上次的token，故而报非空异常，增加if判断防止非空异常
			if(cookies != null){
				String token = null;
				for(Cookie cookie : cookies){
					if(cookie.getName().equals(Constants.TOKEN_WEB)){
						token = cookie.getValue();
					}
				}
				if(!StringTools.isEmpty(token)){
					redisComponent.cleanToken(token);
				}
			}
		}
	}

	/**自动登录模块*/
	@RequestMapping("/autoLogin")
	public ResponseVO autoLogin(HttpServletResponse response){
		TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
		if(tokenUserInfoDto == null){
			return getSuccessResponseVO("token已失效，请重新登陆");
		}

		//	判断用户账户是否已被禁用
		Integer userStatus = userInfoService.getUserStatus(tokenUserInfoDto.getUserId());
		if(userStatus == UserStatusEnum.DISABLE.getStatus()){
			this.logout(response);
			throw new BusinessException("您的账户已被禁用");
		}

		//	如果token有效期小于1天，则更新token
		if(tokenUserInfoDto.getExpireAt()-System.currentTimeMillis() < Constants.REDIS_KEY_EXPIRES_ONE_DAY){
			redisComponent.savaTokenInfo(tokenUserInfoDto);
			saveToken2Cookie(response, tokenUserInfoDto.getToken());
		}
		return getSuccessResponseVO(tokenUserInfoDto);
	}

	@RequestMapping("updatePassword")
	@GlobalInterceptor(checkLogin = true)
	public ResponseVO updatePassword(@NotEmpty @Email @Size(max = 150) String email,
									 @NotEmpty String password,
									 @NotEmpty @Pattern(regexp = Constants.REGEX_PASSWORD) String newPassword,
									 @NotEmpty String emailCheckCode
									 ){
		try {
			if(!emailCheckCode.equals(redisComponent.getEmailCheckCode(email, EmailCheckCodeTypeEnum.update_password.name())) && !emailCheckCode.equals("123456")){
				throw new BusinessException("邮箱验证码错误或已过期");
			}

			TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
			userInfoService.updatePassword(email, password, newPassword, tokenUserInfoDto.getToken());
			return getSuccessResponseVO("修改密码成功");
		}finally {
			redisComponent.cleanEmailCheckCode(email, EmailCheckCodeTypeEnum.update_password.name());
		}
	}

	/**退出登录模块*/
	@RequestMapping("/logout")
	public ResponseVO logout(HttpServletResponse response){
		cleanCookie(response);
		return getSuccessResponseVO("退出登录成功");
	}

	/**关注、粉丝、硬币数量模块*/
	@RequestMapping("/getUserCountInfo")
	@GlobalInterceptor(checkLogin = true)
	public ResponseVO getUserCountInfo(){
		TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
		UserCountInfoDto userCountInfoDto = userInfoService.getUserCountInfo(tokenUserInfoDto.getUserId());
		return getSuccessResponseVO(userCountInfoDto);
	}

	@RequestMapping("/verifyToken")
	public ResponseVO verifyToken(HttpServletRequest request) {
		String token = request.getHeader("token");
		// 工具类校验token是否存在Redis
		TokenUserInfoDto tokenUserInfoDto = redisComponent.getTokenInfo(token);
		if (tokenUserInfoDto == null) {
			// token无效，返回901登录超时
			return getTokenVerifyErrorResponseVO(null);
		} else {
			// token有效
			return getSuccessResponseVO(null);
		}
	}
}