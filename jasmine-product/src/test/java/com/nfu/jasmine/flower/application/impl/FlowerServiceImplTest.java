package com.nfu.jasmine.flower.application.impl;

import com.nfu.jasmine.flower.model.entity.Flower;
import com.nfu.jasmine.flower.persistence.mapper.FlowerMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * FlowerServiceImpl 单元测试。
 * <p>
 * FlowerServiceImpl 主要是 CRUD 委托，本身无业务逻辑分支。
 * 但 @Cacheable/@Caching 注解行为依赖方法正确调用，仍需验证委托路径无误。
 * 注解行为本身由 Spring 容器在集成测试中验证，单元测试只验委托。
 */
@ExtendWith(MockitoExtension.class)
class FlowerServiceImplTest {

    @Mock
    private FlowerMapper flowerMapper;

    @InjectMocks
    private FlowerServiceImpl flowerService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(flowerService, "baseMapper", flowerMapper);
    }

    @Test
    void listAllFlowersShouldDelegateToMapperOrderedById() {
        Flower f1 = buildFlower(1, "红玫瑰");
        Flower f2 = buildFlower(2, "百合");
        when(flowerMapper.selectList(any())).thenReturn(List.of(f1, f2));

        List<Flower> result = flowerService.listAllFlowers();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("红玫瑰");
        verify(flowerMapper).selectList(any());
    }

    @Test
    void getFlowerDetailByIdShouldDelegateToMapper() {
        Flower flower = buildFlower(1, "红玫瑰");
        when(flowerMapper.selectById(1)).thenReturn(flower);

        Flower result = flowerService.getFlowerDetailById(1);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("红玫瑰");
        verify(flowerMapper).selectById(1);
    }

    @Test
    void addFlowerShouldDelegateToMapperInsert() {
        Flower flower = buildFlower(null, "康乃馨");

        flowerService.addFlower(flower);

        verify(flowerMapper).insert(flower);
    }

    @Test
    void updateFlowerShouldDelegateToMapperUpdateById() {
        Flower flower = buildFlower(1, "红玫瑰");

        flowerService.updateFlower(flower);

        verify(flowerMapper).updateById(flower);
    }

    @Test
    void deleteFlowerByIdShouldDelegateToMapperDeleteById() {
        flowerService.deleteFlowerById(1);

        verify(flowerMapper).deleteById(1);
    }

    private Flower buildFlower(Integer id, String name) {
        Flower flower = new Flower();
        flower.setId(id);
        flower.setName(name);
        flower.setSalePrice(new BigDecimal("13.00"));
        flower.setCostPrice(new BigDecimal("10.00"));
        return flower;
    }
}
