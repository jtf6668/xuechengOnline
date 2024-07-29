package com.xuecheng.content.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.token.TokenStore;

/**
 * @description 资源服务配置
 * @author Mr.M
 * @date 2022/10/18 16:33
 * @version 1.0
 */
 @Configuration
 @EnableResourceServer//启用了资源服务器功能
 //表示启用全局方法级安全性，支持 Spring Security 的 @Secured 和 @PreAuthorize 注解。
 @EnableGlobalMethodSecurity(securedEnabled = true,prePostEnabled = true)
 public class ResouceServerConfig extends ResourceServerConfigurerAdapter {


  //资源服务标识,跟认证服务AuthorizationServer类的资源对应
  public static final String RESOURCE_ID = "xuecheng-plus";

  @Autowired
  TokenStore tokenStore;//资源服务器将使用这个 TokenStore 进行令牌验证。

  @Override
  public void configure(ResourceServerSecurityConfigurer resources) {
   //用于配置资源服务器的安全性和令牌存储方式。在这段代码中，配置了资源服务的标识（RESOURCE_ID），
   // 指定了使用的令牌存储方式（tokenStore），并设置了资源服务器为无状态（stateless）
   resources.resourceId(RESOURCE_ID)//资源 id
           .tokenStore(tokenStore)
           .stateless(true);
  }

 @Override
 public void configure(HttpSecurity http) throws Exception {
   //配置了资源服务器的 HTTP 安全性。在这里，禁用了 CSRF 防护，设定了请求授权规则。
  // antMatchers("/r/**","/course/**").authenticated()表示所有以 /r/ 或 /course/ 开头的请求需要经过认证才能访问，其他所有请求允许匿名访问。
  http.csrf().disable()
          .authorizeRequests()
                .antMatchers("/r/**","/course/**").authenticated()//所有/r/**的请求必须认证通过
          .anyRequest().permitAll()
  ;
 }

 }
