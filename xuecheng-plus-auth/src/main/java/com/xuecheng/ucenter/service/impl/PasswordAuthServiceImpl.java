package com.xuecheng.ucenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.ucenter.feignclient.CheckCodeClient;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.xuecheng.ucenter.model.dto.XcUserExt;
import com.xuecheng.ucenter.model.po.XcUser;
import com.xuecheng.ucenter.service.AuthService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * @author Mr.M
 * @version 1.0
 * @description 账号名密码方式
 * @date 2023/2/24 11:56
 */
@Service("password_authservice")
public class PasswordAuthServiceImpl implements AuthService {

 @Autowired
 XcUserMapper xcUserMapper;

 @Autowired
 PasswordEncoder passwordEncoder;

 @Autowired
 CheckCodeClient checkCodeClient;

 @Override
 public XcUserExt execute(AuthParamsDto authParamsDto) {
  //账号
  String username = authParamsDto.getUsername();
  //验证码
  String checkcode = authParamsDto.getCheckcode();
  //验证码对应的key
  String checkcodekey = authParamsDto.getCheckcodekey();

  //账号是否存在
  //根据username账号查询数据库
  XcUser xcUser = xcUserMapper.selectOne(new LambdaQueryWrapper<XcUser>().eq(XcUser::getUsername, username));

  //查询到用户不存在，要返回null即可，spring security框架抛出异常用户不存在
  if (xcUser == null) {
   throw new RuntimeException("账号不存在");
  }

  //如果验证码为空
  if(StringUtils.isEmpty(checkcode) || StringUtils.isEmpty(checkcodekey)){
   throw new RuntimeException("请输入的验证码");
  }

  //远程调用验证码服务接口校验验证码
  Boolean verify = checkCodeClient.verify(checkcodekey, checkcode);

  //null的情况时熔断的降级处理
  if (verify == null || !verify){
   throw new RuntimeException("验证码输入错误");
  }

  //验证密码是否正确
  //如果查到了用户拿到正确的密码
  String passwordDb = xcUser.getPassword();
  //拿 到用户输入的密码
  String passwordForm = authParamsDto.getPassword();
  //校验密码，因为在DaoAuthenticationProviderCustom类中重写了校验密码的方式，框架不会自动校验密码了，所以要手动校验
  boolean matches = passwordEncoder.matches(passwordForm, passwordDb);
  if (!matches) {
   throw new RuntimeException("账号或密码错误");
  }
  XcUserExt xcUserExt = new XcUserExt();
  BeanUtils.copyProperties(xcUser, xcUserExt);

  return xcUserExt;
 }
}
