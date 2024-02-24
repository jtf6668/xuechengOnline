package com.xuecheng.ucenter.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.model.po.XcUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UserServiceImpl implements UserDetailsService {

    @Autowired
    XcUserMapper xcUserMapper;
    @Override
    public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {
        //数据库中查用户是否存在
        XcUser user = xcUserMapper.selectOne(new LambdaQueryWrapper<XcUser>().eq(XcUser::getUsername, s));
        if(user==null){
            //返回空表示用户不存在，框架收到空会报用户不存在异常
            return null;
        }
        //取出数据库存储的正确密码，传给框架，框架比对输入的密码对不对
        String password  =user.getPassword();
        //用户权限,如果不加报Cannot pass a null GrantedAuthority collection
        String[] authorities= {"test"};
        //创建UserDetails对象,权限信息待实现授权功能时再向UserDetail中加入

        //密码为安全信息，所以不防止jwt中
        user.setPassword(null);
        //转成json，然后传递给jwt
        String userJson = JSON.toJSONString(user);

        //比对用户是否存在已经在前面做了，所以下面这条代码只需要比对密码信息和传递用户信息
        UserDetails userDetails = User.withUsername(userJson).password(password).authorities(authorities).build();

        return userDetails;
    }
}