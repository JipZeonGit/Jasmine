package com.nfu.jasmine.cus.controller;

import com.nfu.jasmine.common.enums.ResultCode;
import com.nfu.jasmine.common.vo.Result;
import com.nfu.jasmine.common.vo.TableData;
import com.nfu.jasmine.cus.dto.SalesQueryDTO;
import com.nfu.jasmine.cus.dto.SalesSaveDTO;
import com.nfu.jasmine.cus.service.ISalesService;
import com.nfu.jasmine.cus.vo.SalesVO;
import com.nfu.jasmine.cus.vo.TodayBusinessSummaryVO;
import com.nfu.jasmine.sys.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "销售接口列表")
@Validated
@RestController
@RequestMapping("/sales")
public class SalesController {
    @Autowired
    private ISalesService salesService;

    @Operation(summary = "获取全部销售单")
    @GetMapping("/all")
    public Result<List<SalesVO>> getAllSales() {
        return Result.success(salesService.listSales(), "查询成功");
    }

    @Operation(summary = "获取今日经营统计")
    @GetMapping("/today-summary")
    public Result<TodayBusinessSummaryVO> getTodayBusinessSummary() {
        return Result.success(salesService.getTodayBusinessSummary());
    }

    @Operation(summary = "新增销售单")
    @PostMapping("")
    public Result<?> addSales(@Valid @RequestBody SalesSaveDTO salesDTO, HttpServletRequest request) {
        salesService.saveSales(salesDTO, getCurrentUserId(request));
        return Result.success("新增销售单成功！");
    }

    @Operation(summary = "修改销售单")
    @PutMapping("")
    public Result<?> updateSales(@Valid @RequestBody SalesSaveDTO salesDTO, HttpServletRequest request) {
        if (salesDTO.getId() == null) {
            return Result.fail(ResultCode.VALIDATE_FAILED, "销售单ID不能为空！");
        }
        salesService.updateSales(salesDTO, getCurrentUserId(request));
        return Result.success("修改销售单成功！");
    }

    @Operation(summary = "根据ID查询销售单")
    @GetMapping("/{id}")
    public Result<SalesVO> getSalesById(@PathVariable("id") Integer id) {
        return Result.success(salesService.getSalesDetail(id));
    }

    @Operation(summary = "根据ID逻辑删除销售单")
    @DeleteMapping("/{id}")
    public Result<?> deleteSalesById(@PathVariable("id") Integer id) {
        salesService.deleteSales(id);
        return Result.success("删除销售单成功！");
    }

    @Operation(summary = "分页查询销售单")
    @GetMapping("/list")
    public Result<TableData<SalesVO>> getSalesList(@Valid SalesQueryDTO queryDTO) {
        return Result.success(salesService.pageSales(queryDTO));
    }

    private Integer getCurrentUserId(HttpServletRequest request) {
        Object loginUser = request.getAttribute("loginUser");
        if (loginUser instanceof User user) {
            return user.getId();
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User user) {
            return user.getId();
        }
        return null;
    }
}
