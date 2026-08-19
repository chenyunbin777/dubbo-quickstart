package org.apache.dubbo.samples.quickstart.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.apache.dubbo.samples.quickstart.entity.User;

public interface UserService extends IService<User> {

    User findUserById(Long id);

    User updateUser(User user);

    boolean deleteUser(Long id);
}
