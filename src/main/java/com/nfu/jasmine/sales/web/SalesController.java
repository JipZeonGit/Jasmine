package com.nfu.jasmine.sales.web;

import com.nfu.jasmine.common.enums.ResultCode;
import com.nfu.jasmine.common.vo.Result;
import com.nfu.jasmine.common.vo.TableData;
import com.nfu.jasmine.sales.web.dto.SalesQueryDTO;
import com.nfu.jasmine.sales.web.dto.SalesSaveDTO;
import com.nfu.jasmine.sales.application.ISalesService;
import com.nfu.jasmine.sales.web.vo.SalesVO;
import com.nfu.jasmine.sales.web.vo.TodayBusinessSummaryVO;
import com.nfu.jasmine.infra.idempotency.RequestIdempotencyService;
import com.nfu.jasmine.infra.security.CurrentUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
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

    @Autowired
    private RequestIdempotencyService requestIdempotencyService;
    @Autowired
    private CurrentUserProvider currentUserProvider;

// 一次性把所有的销售订单拉出来
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

    // 结账新增一笔销售单记录
    @Operation(summary = "新增销售单")
    @PostMapping("")
    public Result<?> addSales(@Valid @RequestBody SalesSaveDTO salesDTO,
                              @RequestHeader(value = RequestIdempotencyService.IDEMPOTENCY_HEADER, required = false) String idempotencyKey,
                              HttpServletRequest request) {
        Integer currentUserId = currentUserProvider.requireCurrentUserId(request);
        requestIdempotencyService.executeCreate(
                "sales:create",
                currentUserId,
                idempotencyKey,
                salesDTO,
                () -> salesService.saveSales(salesDTO, currentUserId)
        );
        return Result.success("新增销售单成功！");
    }

// 修改销售单的数据内容
    @Operation(summary = "修改销售单")
    @PutMapping("")
    public Result<?> updateSales(@Valid @RequestBody SalesSaveDTO salesDTO, HttpServletRequest request) {
        if (salesDTO.getId() == null) {
            return Result.fail(ResultCode.VALIDATE_FAILED, "销售单ID不能为空！");
        }
        salesService.updateSales(salesDTO, currentUserProvider.requireCurrentUserId(request));
        return Result.success("修改销售单成功！");
    }

// 给一个销售单ID，返回这张单的具体信息
    @Operation(summary = "根据ID查询销售单")
    @GetMapping("/{id}")
    public Result<SalesVO> getSalesById(@PathVariable("id") Integer id) {
        return Result.success(salesService.getSalesDetail(id));
    }

// 逻辑删除这一条销售订单记录
    @Operation(summary = "根据ID逻辑删除销售单")
    @DeleteMapping("/{id}")
    public Result<?> deleteSalesById(@PathVariable("id") Integer id) {
        salesService.deleteSales(id);
        return Result.success("删除销售单成功！");
    }

// 分页查询销售订单列表，支持按售出日期来过滤
    @Operation(summary = "分页查询销售单")
    @GetMapping("/list")
    public Result<TableData<SalesVO>> getSalesList(@Valid SalesQueryDTO queryDTO) {
        return Result.success(salesService.pageSales(queryDTO));
    }
}
