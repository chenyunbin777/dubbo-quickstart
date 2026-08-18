package org.apache.dubbo.samples.quickstart.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.dubbo.samples.quickstart.entity.User;
import org.apache.dubbo.samples.quickstart.mapper.UserMapper;
import org.apache.dubbo.samples.quickstart.service.UserService;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
}
