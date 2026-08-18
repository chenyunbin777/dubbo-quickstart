package org.apache.dubbo.samples.quickstart.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.dubbo.samples.quickstart.user.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
