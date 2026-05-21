package com.nfu.jasmine.vip.web.internal;

import com.nfu.jasmine.common.dto.internal.VipBasicDTO;
import com.nfu.jasmine.common.enums.ResultCode;
import com.nfu.jasmine.common.exception.BusinessException;
import com.nfu.jasmine.vip.model.entity.Vip;
import com.nfu.jasmine.vip.persistence.mapper.VipMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;

/**
 * 会员内部接口 —— 仅供服务间调用，网关层拦截外部访问。
 */
@Tag(name = "内部接口")
@RestController
@RequestMapping("/internal/vip")
public class VipInternalController {

    private final VipMapper vipMapper;

    public VipInternalController(VipMapper vipMapper) {
        this.vipMapper = vipMapper;
    }

    @Operation(summary = "根据ID查询会员基本信息（内部）")
    @GetMapping("/{id}")
    public VipBasicDTO getVipById(@PathVariable Integer id) {
        Vip vip = vipMapper.selectById(id);
        if (vip == null || Integer.valueOf(1).equals(vip.getDeleted())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "会员不存在！");
        }
        return toVipBasicDTO(vip);
    }

    @Operation(summary = "批量查询会员基本信息（内部）")
    @PostMapping("/batch")
    public List<VipBasicDTO> getVipsByIds(@RequestBody Collection<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return vipMapper.selectBatchIds(ids).stream()
                .filter(v -> v.getDeleted() == null || v.getDeleted() == 0)
                .map(this::toVipBasicDTO)
                .toList();
    }

    @Operation(summary = "判断会员是否存在（内部）")
    @GetMapping("/exists/{id}")
    public boolean existsById(@PathVariable Integer id) {
        if (id == null) return false;
        Vip vip = vipMapper.selectById(id);
        return vip != null && (vip.getDeleted() == null || vip.getDeleted() == 0);
    }

    private VipBasicDTO toVipBasicDTO(Vip vip) {
        VipBasicDTO dto = new VipBasicDTO();
        dto.setId(vip.getId());
        dto.setVid(vip.getVid());
        dto.setName(vip.getName());
        dto.setSex(vip.getSex());
        dto.setPhone(vip.getPhone());
        return dto;
    }
}
