package com.nfu.jasmine.iam.persistence.mapper;

import com.nfu.jasmine.iam.model.entity.Menu;
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
public interface MenuMapper extends BaseMapper<Menu> {
    List<Menu> getMenuListByUserId(@Param("userId") Integer userId,@Param("parentId") Integer parentId);

    List<Menu> getAllMenusByUserId(@Param("userId") Integer userId);
}
