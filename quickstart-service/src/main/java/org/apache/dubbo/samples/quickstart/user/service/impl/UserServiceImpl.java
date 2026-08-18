package org.apache.dubbo.samples.quickstart.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.dubbo.samples.quickstart.user.entity.User;
import org.apache.dubbo.samples.quickstart.user.mapper.UserMapper;
import org.apache.dubbo.samples.quickstart.user.service.UserService;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
}
