package com.xuecheng.base.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * @author Mr.M
 * @version 1.0
 * @description TODO
 * @date 2023/2/12 17:01
 */
@Slf4j
@ControllerAdvice
//@RestControllerAdvice    //相当于@ControllerAdvice+ResponseBody
public class GlobalExceptionHandler {

 //对项目的自定义异常类型进行处理
   @ResponseBody//异常信息返回json形式
   @ExceptionHandler(XueChengPlusException.class)//捕获指定异常
   @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)//响应给前端的状态码
 public RestErrorResponse customException(XueChengPlusException e){

    //记录异常
    log.error("系统异常{}",e.getErrMessage(),e);
    //..

    //解析出异常信息
    String errMessage = e.getErrMessage();
    RestErrorResponse restErrorResponse = new RestErrorResponse(errMessage);
    return restErrorResponse;
   }



//如果不是我们自定义的异常，例如是数据库连接错误
   @ResponseBody
   @ExceptionHandler(Exception.class)
   @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
 public RestErrorResponse exception(Exception e){

    //记录异常
    log.error("系统异常{}",e.getMessage(),e);

    //解析出异常信息
    RestErrorResponse restErrorResponse = new RestErrorResponse(CommonError.UNKOWN_ERROR.getErrMessage());
    return restErrorResponse;
   }


}
