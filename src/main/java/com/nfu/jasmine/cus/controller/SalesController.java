package com.nfu.jasmine.cus.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nfu.jasmine.common.enums.ResultCode;
import com.nfu.jasmine.common.vo.Result;
import com.nfu.jasmine.common.vo.TableData;
import com.nfu.jasmine.cus.dto.SalesQueryDTO;
import com.nfu.jasmine.cus.dto.SalesSaveDTO;
import com.nfu.jasmine.cus.entity.Sales;
import com.nfu.jasmine.cus.service.ISalesService;
import com.nfu.jasmine.cus.vo.SalesVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
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
@Tag(name = "销售接口列表")
@Validated
@RestController
@RequestMapping("/sales")
public class SalesController {
    @Autowired
    private ISalesService salesService;

    @Operation(summary = "获取全部销售订单")
    @GetMapping("/all")
    public Result<List<SalesVO>> getAllSales() {
        List<SalesVO> list = salesService.list().stream().map(this::toSalesVO).toList();
        return Result.success(list, "查询成功");
    }

    @Operation(summary = "新增销售订单")
    @PostMapping("")
    public Result<?> addSales(@Valid @RequestBody SalesSaveDTO salesDTO) {
        Sales sales = new Sales();
        BeanUtils.copyProperties(salesDTO, sales);
        salesService.save(sales);
        return Result.success("新增销售订单成功！");
    }

    @Operation(summary = "修改销售订单")
    @PutMapping("")
    public Result<?> updateSales(@Valid @RequestBody SalesSaveDTO salesDTO) {
        if (salesDTO.getId() == null) {
            return Result.fail(ResultCode.VALIDATE_FAILED, "销售订单ID不能为空！");
        }
        Sales sales = new Sales();
        BeanUtils.copyProperties(salesDTO, sales);
        salesService.updateById(sales);
        return Result.success("修改销售订单成功！");
    }

    @Operation(summary = "根据ID查询单份销售订单")
    @GetMapping("/{id}")
    public Result<SalesVO> getSalesById(@PathVariable("id") Integer id) {
        Sales sales = salesService.getById(id);
        return Result.success(sales == null ? null : toSalesVO(sales));
    }

    @Operation(summary = "根据ID逻辑删除销售订单数据")
    @DeleteMapping("/{id}")
    public Result<?> deleteSalesById(@PathVariable("id") Integer id) {
        salesService.removeById(id);
        return Result.success("删除销售订单数据成功！");
    }

    @Operation(summary = "查询销售订单")
    @GetMapping("/list")
    public Result<TableData<SalesVO>> getSalesList(@Valid SalesQueryDTO queryDTO) {
        LambdaQueryWrapper<Sales> wrapper = new LambdaQueryWrapper<>();

        if (queryDTO.getDate() != null) {
            wrapper.like(Sales::getDate, queryDTO.getDate());
        }

        wrapper.orderByAsc(Sales::getId);

        Page<Sales> page = new Page<>(queryDTO.getPageNo(), queryDTO.getPageSize());
        salesService.page(page, wrapper);

        TableData<SalesVO> data = new TableData<>();
        data.setTotal(page.getTotal());
        data.setRows(page.getRecords().stream().map(this::toSalesVO).toList());

        return Result.success(data);
    }

    private SalesVO toSalesVO(Sales sales) {
        SalesVO salesVO = new SalesVO();
        BeanUtils.copyProperties(sales, salesVO);
        return salesVO;
    }
}