package com.xuecheng.ucenter.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.xuecheng.ucenter.model.dto.XcUserExt;
import com.xuecheng.ucenter.model.po.XcUser;
import com.xuecheng.ucenter.service.AuthService;
import javafx.application.Application;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
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

    @Autowired
    ApplicationContext applicationContext;//通过它来获取在配置文件中定义的各种Bean（组件）
    @Override
    public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {
        //将传入的json转成AuthParamsDto对象
        AuthParamsDto authParamsDto = null;
        try {
            authParamsDto = JSON.parseObject(s,AuthParamsDto.class);
        }catch (Exception e){
            throw new RuntimeException("请求认证参数不符合要求");
        }

        //根据认证类型获取指定的认证方式（从容器中拿到）,通过名字拿到，因为在认证方式的实现类中的@Service上写了名字
        String authType = authParamsDto.getAuthType();
        String beanName = authType+"_authservice";
        //从Spring的ApplicationContext中获取一个名为"beanName"的Bean，并将其转换为AuthService类型的对象
        AuthService authService = applicationContext.getBean(beanName, AuthService.class);

        //调用统一的execute方法完成认证
        XcUserExt execute = authService.execute(authParamsDto);


        //账号
        String username = authParamsDto.getUsername();

        //数据库中查用户是否存在
        XcUser user = xcUserMapper.selectOne(new LambdaQueryWrapper<XcUser>().eq(XcUser::getUsername, username));
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
