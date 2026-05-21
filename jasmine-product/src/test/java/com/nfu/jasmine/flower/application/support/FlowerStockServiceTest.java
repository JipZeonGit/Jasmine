package com.nfu.jasmine.flower.application.support;

import com.nfu.jasmine.common.dto.internal.ProductStockFacade;
import com.nfu.jasmine.common.exception.BusinessException;
import com.nfu.jasmine.flower.model.entity.Flower;
import com.nfu.jasmine.flower.persistence.mapper.FlowerMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FlowerStockServiceTest {

    @Mock
    private FlowerMapper flowerMapper;

    @InjectMocks
    private FlowerStockService flowerStockService;

    @Test
    void adjustStockShouldReturnCorrectBeforeAndAfterStock() {
        Flower flower = createFlower(1, "红玫瑰", 50);
        when(flowerMapper.selectById(1)).thenReturn(flower);
        when(flowerMapper.compareAndSetStock(eq(1), eq(50), eq(45), any(), eq(false))).thenReturn(1);

        ProductStockFacade.StockChangeResult result = flowerStockService.adjustStock(1, -5, null, false, null);

        assertThat(result.beforeStock()).isEqualTo(50);
        assertThat(result.afterStock()).isEqualTo(45);
        assertThat(result.flower().getId()).isEqualTo(1);
        assertThat(result.flower().getName()).isEqualTo("红玫瑰");
    }

    @Test
    void adjustStockShouldThrowWhenInsufficientStock() {
        Flower flower = createFlower(1, "红玫瑰", 3);
        when(flowerMapper.selectById(1)).thenReturn(flower);

        assertThatThrownBy(() -> flowerStockService.adjustStock(1, -5, null, false, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("库存不足");
    }

    @Test
    void adjustStockShouldThrowWhenFlowerNotFound() {
        when(flowerMapper.selectById(999)).thenReturn(null);

        assertThatThrownBy(() -> flowerStockService.adjustStock(999, -1, null, false, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("花卉不存在");
    }

    @Test
    void adjustStockShouldThrowWhenFlowerDeleted() {
        Flower flower = createFlower(1, "红玫瑰", 50);
        flower.setDeleted(1);
        when(flowerMapper.selectById(1)).thenReturn(flower);

        assertThatThrownBy(() -> flowerStockService.adjustStock(1, -1, null, false, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("花卉不存在");
    }

    @Test
    void adjustStockShouldThrowWhenMaxRetryExceeded() {
        Flower flower = createFlower(1, "红玫瑰", 50);
        when(flowerMapper.selectById(1)).thenReturn(flower);
        // CAS 永远失败（返回 0），模拟并发冲突
        when(flowerMapper.compareAndSetStock(eq(1), eq(50), eq(45), any(), eq(false))).thenReturn(0);

        assertThatThrownBy(() -> flowerStockService.adjustStock(1, -5, null, false, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("正在被其他请求更新");
    }

    @Test
    void adjustStockShouldRetryOnCasConflictAndSucceed() {
        Flower flower = createFlower(1, "红玫瑰", 50);
        when(flowerMapper.selectById(1)).thenReturn(flower);
        // 第一次 CAS 失败，第二次成功
        when(flowerMapper.compareAndSetStock(eq(1), eq(50), eq(45), any(), eq(false)))
                .thenReturn(0)
                .thenReturn(1);

        ProductStockFacade.StockChangeResult result = flowerStockService.adjustStock(1, -5, null, false, null);

        assertThat(result.afterStock()).isEqualTo(45);
    }

    @Test
    void adjustStockShouldAllowZeroStock() {
        Flower flower = createFlower(1, "红玫瑰", 5);
        when(flowerMapper.selectById(1)).thenReturn(flower);
        when(flowerMapper.compareAndSetStock(eq(1), eq(5), eq(0), any(), eq(false))).thenReturn(1);

        ProductStockFacade.StockChangeResult result = flowerStockService.adjustStock(1, -5, null, false, null);

        assertThat(result.afterStock()).isEqualTo(0);
    }

    @Test
    void adjustStockShouldUpdateCostPriceWhenRequested() {
        Flower flower = createFlower(1, "红玫瑰", 50);
        when(flowerMapper.selectById(1)).thenReturn(flower);
        when(flowerMapper.compareAndSetStock(eq(1), eq(50), eq(70), eq(new BigDecimal("8.50")), eq(true))).thenReturn(1);

        ProductStockFacade.StockChangeResult result = flowerStockService.adjustStock(1, 20, new BigDecimal("8.50"), true, null);

        assertThat(result.afterStock()).isEqualTo(70);
        assertThat(result.flower().getCost()).isEqualByComparingTo("8.50");
    }

    private Flower createFlower(Integer id, String name, int stock) {
        Flower flower = new Flower();
        flower.setId(id);
        flower.setName(name);
        flower.setUnit("枝");
        flower.setSalePrice(new BigDecimal("13.00"));
        flower.setCostPrice(new BigDecimal("10.00"));
        flower.setSafeStock(10);
        flower.setCurrentStock(stock);
        flower.setStatus(1);
        flower.setDeleted(0);
        return flower;
    }
}
