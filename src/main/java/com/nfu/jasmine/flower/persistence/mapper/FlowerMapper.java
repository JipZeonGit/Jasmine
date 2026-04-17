package com.nfu.jasmine.flower.persistence.mapper;

import com.nfu.jasmine.flower.model.entity.Flower;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author jipzeongit
 * @since 2023-07-06
 */
public interface FlowerMapper extends BaseMapper<Flower> {

    int compareAndSetStock(@Param("id") Integer id,
                           @Param("expectedStock") Integer expectedStock,
                           @Param("newStock") Integer newStock,
                           @Param("costPrice") java.math.BigDecimal costPrice,
                           @Param("updateCostPrice") boolean updateCostPrice);

}
