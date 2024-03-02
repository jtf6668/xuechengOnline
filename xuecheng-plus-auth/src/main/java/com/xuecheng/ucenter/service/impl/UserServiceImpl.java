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
            authParamsDto = JSON.parseObject(s, AuthParamsDto.class);
        } catch (Exception e) {
            throw new RuntimeException("请求认证参数不符合要求");
        }

        //根据认证类型获取指定的认证方式（从容器中拿到）,通过名字拿到，因为在认证方式的实现类中的@Service上写了名字
        String authType = authParamsDto.getAuthType();
        String beanName = authType + "_authservice";
        //从Spring的ApplicationContext中获取一个名为"beanName"的Bean，并将其转换为AuthService类型的对象
        AuthService authService = applicationContext.getBean(beanName, AuthService.class);//多态，向上转型

        //调用统一的execute方法完成认证
        XcUserExt execute = authService.execute(authParamsDto);

        UserDetails userPrincipal = getUserPrincipal(execute);

        return userPrincipal;

    }
        /**
         * @description 查询用户信息，封装到令牌中
         * @param user  用户id，主键
         * @return com.xuecheng.ucenter.model.po.XcUser 用户信息
         * @author Mr.M
         * @date 2022/9/29 12:19
         */
        public UserDetails getUserPrincipal(XcUserExt user){
            //用户权限,如果不加报Cannot pass a null GrantedAuthority collection
            String[] authorities = {"p1"};
            String password = user.getPassword();
            //为了安全在令牌中不放密码
            user.setPassword(null);
            //将user对象转json
            String userString = JSON.toJSONString(user);
            //创建UserDetails对象
            UserDetails userDetails = User.withUsername(userString).password(password ).authorities(authorities).build();
            return userDetails;
        }


    }

