package com.nfu.jasmine.cus.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nfu.jasmine.common.enums.ResultCode;
import com.nfu.jasmine.common.utils.MembershipIdUtil;
import com.nfu.jasmine.common.vo.Result;
import com.nfu.jasmine.common.vo.TableData;
import com.nfu.jasmine.cus.dto.VipQueryDTO;
import com.nfu.jasmine.cus.dto.VipSaveDTO;
import com.nfu.jasmine.cus.entity.Vip;
import com.nfu.jasmine.cus.mapper.VipMapper;
import com.nfu.jasmine.cus.service.IVipService;
import com.nfu.jasmine.cus.vo.VipVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author jipzeongit
 * @since 2023-07-06
 */
@Tag(name = "会员接口列表")
@Validated
@RestController
@RequestMapping("/vip")
public class VipController {
    @Autowired
    private IVipService vipService;
    @Autowired
    private VipMapper vipMapper;

    @Operation(summary = "获取全部会员")
    @GetMapping("/all")
    public Result<List<VipVO>> getAllVip() {
        List<VipVO> list = vipService.list().stream().map(this::toVipVO).toList();
        return Result.success(list, "查询成功");
    }

    @Operation(summary = "新增会员")
    @PostMapping("")
    public Result<?> addVip(@Valid @RequestBody VipSaveDTO vipDTO) {
        if (phoneExists(vipDTO.getPhone(), null)) {
            return Result.fail(ResultCode.CONFLICT, "该手机号已注册，请重新输入没有注册的手机号！");
        }

        Vip vip = new Vip();
        BeanUtils.copyProperties(vipDTO, vip);
        vip.setVid(MembershipIdUtil.generateMembershipCardNumber());
        vipService.save(vip);
        return Result.success("新增会员成功!");
    }

    @Operation(summary = "修改会员")
    @PutMapping("")
    public Result<?> updateVip(@Valid @RequestBody VipSaveDTO vipDTO) {
        if (vipDTO.getId() == null) {
            return Result.fail(ResultCode.VALIDATE_FAILED, "会员ID不能为空！");
        }
        if (phoneExists(vipDTO.getPhone(), vipDTO.getId())) {
            return Result.fail(ResultCode.CONFLICT, "该手机号已注册，请重新输入没有注册的手机号！");
        }

        Vip vip = new Vip();
        BeanUtils.copyProperties(vipDTO, vip);
        vipService.updateById(vip);
        return Result.success("修改会员成功！");
    }

    @Operation(summary = "根据ID查询会员")
    @GetMapping("/{id}")
    public Result<VipVO> getVipById(@PathVariable("id") Integer id) {
        Vip vip = vipService.getById(id);
        return Result.success(vip == null ? null : toVipVO(vip));
    }

    @Operation(summary = "根据ID逻辑删除会员数据")
    @DeleteMapping("/{id}")
    public Result<?> deleteVipById(@PathVariable("id") Integer id) {
        vipService.removeById(id);
        return Result.success("删除会员数据成功！");
    }

    @Operation(summary = "查询会员")
    @GetMapping("/list")
    public Result<TableData<VipVO>> getVipList(@Valid VipQueryDTO queryDTO) {
        LambdaQueryWrapper<Vip> wrapper = new LambdaQueryWrapper<>();

        wrapper.like(StringUtils.hasLength(queryDTO.getName()), Vip::getName, queryDTO.getName());
        wrapper.like(StringUtils.hasLength(queryDTO.getVid()), Vip::getVid, queryDTO.getVid());
        wrapper.like(StringUtils.hasLength(queryDTO.getPhone()), Vip::getPhone, queryDTO.getPhone());
        wrapper.orderByAsc(Vip::getId);

        Page<Vip> page = new Page<>(queryDTO.getPageNo(), queryDTO.getPageSize());
        vipService.page(page, wrapper);

        TableData<VipVO> data = new TableData<>();
        data.setTotal(page.getTotal());
        data.setRows(page.getRecords().stream().map(this::toVipVO).toList());

        return Result.success(data);
    }

    private boolean phoneExists(String phone, Integer currentId) {
        if (!StringUtils.hasLength(phone)) {
            return false;
        }

        QueryWrapper<Vip> queryWrapper = new QueryWrapper<Vip>().eq("phone", phone);
        Vip vipByPhone = vipMapper.selectOne(queryWrapper);
        return vipByPhone != null && !vipByPhone.getId().equals(currentId);
    }

    private VipVO toVipVO(Vip vip) {
        VipVO vipVO = new VipVO();
        BeanUtils.copyProperties(vip, vipVO);
        return vipVO;
    }
}