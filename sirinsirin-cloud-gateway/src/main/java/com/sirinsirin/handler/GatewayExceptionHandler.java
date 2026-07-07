package com.sirinsirin.handler;

import com.sirinsirin.entity.enums.ResponseCodeEnum;
import com.sirinsirin.utils.JsonUtils;
import com.sirinsirin.entity.vo.ResponseVO;
import com.sirinsirin.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@Order(-1)
public class GatewayExceptionHandler implements WebExceptionHandler {
    protected static final String STATUS_ERROR = "error";

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ResponseVO responseVO = getResponse(ex);
        ServerHttpResponse response = exchange.getResponse();
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        DataBuffer dataBuffer = response.bufferFactory().wrap(
                JsonUtils.convertObj2Json(responseVO).getBytes(StandardCharsets.UTF_8)
        );
        return response.writeWith(Mono.just(dataBuffer));
    }

    private ResponseVO getResponse(Throwable throwable) {
        ResponseVO responseVO = new ResponseVO();
        responseVO.setStatus(STATUS_ERROR);
        if (throwable instanceof ResponseStatusException) {
            ResponseStatusException responseStatusException = (ResponseStatusException) throwable;

            if (HttpStatus.NOT_FOUND == responseStatusException.getStatus()) {
                responseVO.setCode(ResponseCodeEnum.CODE_404.getCode());
                responseVO.setInfo(ResponseCodeEnum.CODE_404.getMsg());
                responseVO.setStatus(STATUS_ERROR);
                return responseVO;
            }else if(HttpStatus.SERVICE_UNAVAILABLE == responseStatusException.getStatus()){
                responseVO.setCode(ResponseCodeEnum.CODE_503.getCode());
                responseVO.setInfo(ResponseCodeEnum.CODE_503.getMsg());
                return responseVO;
            }else{
                responseVO.setCode(responseStatusException.getStatus().value());
                responseVO.setInfo(ResponseCodeEnum.CODE_500.getMsg());
            }
        }else if(throwable instanceof BusinessException){
            BusinessException exception = (BusinessException) throwable;
            responseVO.setCode(exception.getCode());
            responseVO.setInfo(exception.getMessage());
            return responseVO;
        }

        responseVO.setCode(ResponseCodeEnum.CODE_500.getCode());
        responseVO.setInfo(ResponseCodeEnum.CODE_500.getMsg());
        return responseVO;
    }
}