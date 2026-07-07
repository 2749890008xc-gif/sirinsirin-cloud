package com.sirinsirin.filter;

import com.sirinsirin.entity.constants.Constants;
import com.sirinsirin.entity.enums.ResponseCodeEnum;
import com.sirinsirin.exception.BusinessException;
import com.sirinsirin.utils.StringTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AdminFilter extends AbstractGatewayFilterFactory {

    // 账号相关接口路径（放行路径）
    private final static String URL_ACCOUNT = "/account";
    // 文件相关接口路径（特殊token获取方式）
    private final static String URL_FILE = "/file";

    @Override
    public GatewayFilter apply(Object config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String rawPath = request.getURI().getRawPath();
            log.info("admin rawpath{}", rawPath);
            if (rawPath.contains(URL_ACCOUNT)) {
                return chain.filter(exchange);
            }

            String token = getToken(request);
            if (rawPath.contains(URL_FILE)) {
                token = getTokenFromCookie(request);
            }
            if (StringTools.isEmpty(token)) {
                throw new BusinessException(ResponseCodeEnum.CODE_901);
            }

            return chain.filter(exchange);
        };
    }

    private String getToken(ServerHttpRequest request) {
        return request.getHeaders().getFirst(Constants.TOKEN_ADMIN);
    }

    private String getTokenFromCookie(ServerHttpRequest request) {
        return request.getCookies().getFirst(Constants.TOKEN_ADMIN).getValue();
    }
}