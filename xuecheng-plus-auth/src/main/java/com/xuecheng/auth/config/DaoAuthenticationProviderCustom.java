package com.xuecheng.auth.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

/**
 * @author Mr.M
 * @version 1.0
 * @description 重写了DaoAuthenticationProvider的校验的密码的方法，因为我们统一认证入口，有一些认证方式不需要校验密码
 * @date 2023/2/24 11:40
 */
//本来是在这个类校验由userDetail（数据库结果传来的账号和密码)跟客户端的是否匹配，这里取消掉
@Component
public class DaoAuthenticationProviderCustom extends DaoAuthenticationProvider {

 @Autowired
 public void setUserDetailsService(UserDetailsService userDetailsService) {
  //注入自己重写的UserDetailsService
  super.setUserDetailsService(userDetailsService);
 }

 /**
  * 重写校验密码，有些认证方式不需要密码，所以不写，即屏蔽密码对比
  * @param userDetails
  * @param authentication
  * @throws AuthenticationException
  */
 @Override
 protected void additionalAuthenticationChecks(UserDetails userDetails, UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {

 }
}
