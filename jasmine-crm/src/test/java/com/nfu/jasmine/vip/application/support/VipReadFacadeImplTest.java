package com.nfu.jasmine.vip.application.support;

import com.nfu.jasmine.vip.model.entity.Vip;
import com.nfu.jasmine.vip.persistence.mapper.VipMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * VipReadFacadeImpl 单元测试。
 * <p>
 * 重点覆盖 resolve 方法的三分支（vipId/vid/phone）和 findIdsByNameOrPhone 的动态查询。
 */
@ExtendWith(MockitoExtension.class)
class VipReadFacadeImplTest {

    @Mock
    private VipMapper vipMapper;

    @InjectMocks
    private VipReadFacadeImpl vipReadFacade;

    // ==================== existsById ====================

    @Test
    void existsByIdShouldReturnTrueWhenVipExists() {
        when(vipMapper.selectById(1)).thenReturn(buildVip(1, "V001", "张三"));

        assertThat(vipReadFacade.existsById(1)).isTrue();
    }

    @Test
    void existsByIdShouldReturnFalseWhenVipNotFound() {
        when(vipMapper.selectById(999)).thenReturn(null);

        assertThat(vipReadFacade.existsById(999)).isFalse();
    }

    @Test
    void existsByIdShouldReturnFalseWhenNull() {
        assertThat(vipReadFacade.existsById(null)).isFalse();
    }

    // ==================== findById ====================

    @Test
    void findByIdShouldReturnVip() {
        Vip vip = buildVip(1, "V001", "张三");
        when(vipMapper.selectById(1)).thenReturn(vip);

        Vip result = vipReadFacade.findById(1);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("张三");
    }

    @Test
    void findByIdShouldReturnNullWhenIdNull() {
        assertThat(vipReadFacade.findById(null)).isNull();
    }

    // ==================== resolve ====================

    @Test
    void resolveShouldPreferVipIdWhenPresent() {
        // vipId 优先级最高，其他参数应被忽略
        Vip vip = buildVip(1, "V001", "张三");
        when(vipMapper.selectById(1)).thenReturn(vip);

        Vip result = vipReadFacade.resolve(1, "V002", "13900000000");

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1);
        // 不应查询 vid 或 phone
        verify(vipMapper).selectById(1);
    }

    @Test
    void resolveShouldQueryByVidWhenVipIdAbsent() {
        Vip vip = buildVip(2, "V002", "李四");
        when(vipMapper.selectOne(any())).thenReturn(vip);

        Vip result = vipReadFacade.resolve(null, "V002", "13900000000");

        assertThat(result).isNotNull();
        assertThat(result.getVid()).isEqualTo("V002");
    }

    @Test
    void resolveShouldQueryByPhoneWhenBothVipIdAndVidAbsent() {
        Vip vip = buildVip(3, "V003", "王五");
        vip.setPhone("13900000000");
        when(vipMapper.selectOne(any())).thenReturn(vip);

        Vip result = vipReadFacade.resolve(null, null, "13900000000");

        assertThat(result).isNotNull();
        assertThat(result.getPhone()).isEqualTo("13900000000");
    }

    @Test
    void resolveShouldReturnNullWhenAllIdentifiersAbsent() {
        Vip result = vipReadFacade.resolve(null, null, null);

        assertThat(result).isNull();
    }

    // ==================== findIdsByNameOrPhone ====================

    @Test
    void findIdsByNameOrPhoneShouldReturnMatchingIds() {
        Vip v1 = buildVip(1, "V001", "张三");
        Vip v2 = buildVip(2, "V002", "张四");
        when(vipMapper.selectList(any())).thenReturn(List.of(v1, v2));

        List<Integer> ids = vipReadFacade.findIdsByNameOrPhone("张", null);

        assertThat(ids).containsExactly(1, 2);
    }

    @Test
    void findIdsByNameOrPhoneShouldReturnEmptyWhenNoMatch() {
        when(vipMapper.selectList(any())).thenReturn(List.of());

        List<Integer> ids = vipReadFacade.findIdsByNameOrPhone("不存在", null);

        assertThat(ids).isEmpty();
    }

    // ==================== findByIds ====================

    @Test
    void findByIdsShouldReturnMapKeyedById() {
        Vip v1 = buildVip(1, "V001", "张三");
        Vip v2 = buildVip(2, "V002", "李四");
        when(vipMapper.selectBatchIds(anyCollection())).thenReturn(List.of(v1, v2));

        Map<Integer, Vip> result = vipReadFacade.findByIds(List.of(1, 2));

        assertThat(result).hasSize(2);
        assertThat(result.get(1).getName()).isEqualTo("张三");
        assertThat(result.get(2).getName()).isEqualTo("李四");
    }

    @Test
    void findByIdsShouldReturnEmptyWhenInputEmpty() {
        Map<Integer, Vip> result = vipReadFacade.findByIds(List.of());

        assertThat(result).isEmpty();
    }

    @Test
    void findByIdsShouldReturnEmptyWhenInputNull() {
        Map<Integer, Vip> result = vipReadFacade.findByIds(null);

        assertThat(result).isEmpty();
    }

    private Vip buildVip(Integer id, String vid, String name) {
        Vip vip = new Vip();
        vip.setId(id);
        vip.setVid(vid);
        vip.setName(name);
        vip.setPhone("13800138000");
        vip.setSex("男");
        return vip;
    }
}
