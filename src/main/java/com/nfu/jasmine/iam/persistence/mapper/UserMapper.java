package com.nfu.jasmine.iam.persistence.mapper;

import com.nfu.jasmine.iam.model.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author jipzeongit
 * @since 2023-05-29
 */
public interface UserMapper extends BaseMapper<User> {
    List<String> getRoleNameByUserId(Integer userId);

    List<Integer> getActiveUserIdsByRoleNames(@Param("roleNames") List<String> roleNames);
}
