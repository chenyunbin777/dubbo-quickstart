package org.apache.dubbo.samples.quickstart.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.dubbo.samples.quickstart.entity.User;
import org.apache.dubbo.samples.quickstart.cache.annotation.SmartCacheEvict;
import org.apache.dubbo.samples.quickstart.cache.annotation.SmartCachePut;
import org.apache.dubbo.samples.quickstart.cache.annotation.SmartCacheable;
import org.apache.dubbo.samples.quickstart.mapper.UserMapper;
import org.apache.dubbo.samples.quickstart.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    @SmartCacheable(namespace = "user", key = "#id")
    public User findUserById(Long id) {
        if (id == null || id <= 0) {
            return null;
        }
        return getById(id);
    }

    @Override
    @Transactional
    @SmartCachePut(namespace = "user", key = "#result.id")
    public User updateUser(User user) {
        if (user == null || user.getId() == null || !updateById(user)) {
            return null;
        }
        return getById(user.getId());
    }

    @Override
    @Transactional
    @SmartCacheEvict(namespace = "user", key = "#id")
    public boolean deleteUser(Long id) {
        return id != null && id > 0 && removeById(id);
    }
}
