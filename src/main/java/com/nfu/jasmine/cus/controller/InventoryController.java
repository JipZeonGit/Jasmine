package com.nfu.jasmine.cus.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nfu.jasmine.common.enums.ResultCode;
import com.nfu.jasmine.common.vo.Result;
import com.nfu.jasmine.common.vo.TableData;
import com.nfu.jasmine.cus.dto.InventoryQueryDTO;
import com.nfu.jasmine.cus.dto.InventorySaveDTO;
import com.nfu.jasmine.cus.service.IInventoryService;
import com.nfu.jasmine.cus.vo.InventoryVO;
import com.nfu.jasmine.sys.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "库存接口列表")
@Validated
@RestController
@RequestMapping("/inventory")
public class InventoryController {
    @Autowired
    private IInventoryService inventoryService;

    @Operation(summary = "获取全部库存流水")
    @GetMapping("/all")
    public Result<List<InventoryVO>> getAllInventory() {
        return Result.success(inventoryService.listInventory(), "查询成功");
    }

    @Operation(summary = "新增库存动作")
    @PostMapping("")
    public Result<?> addInventory(@Valid @RequestBody InventorySaveDTO inventoryDTO, HttpServletRequest request) {
        inventoryService.saveInventory(inventoryDTO, getCurrentUserId(request));
        return Result.success("新增库存动作成功！");
    }

    @Operation(summary = "修改库存动作")
    @PutMapping("")
    public Result<?> updateInventory(@Valid @RequestBody InventorySaveDTO inventoryDTO, HttpServletRequest request) {
        if (inventoryDTO.getId() == null) {
            return Result.fail(ResultCode.VALIDATE_FAILED, "库存流水ID不能为空！");
        }
        inventoryService.updateInventory(inventoryDTO, getCurrentUserId(request));
        return Result.success("修改库存动作成功！");
    }

    @Operation(summary = "根据ID查询库存流水")
    @GetMapping("/{id}")
    public Result<InventoryVO> getInventoryById(@PathVariable("id") Integer id) {
        return Result.success(inventoryService.getInventoryDetail(id));
    }

    @Operation(summary = "根据ID逻辑删除库存流水")
    @DeleteMapping("/{id}")
    public Result<?> deleteInventoryById(@PathVariable("id") Integer id) {
        inventoryService.deleteInventory(id);
        return Result.success("删除库存流水成功！");
    }

    @Operation(summary = "分页查询库存流水")
    @GetMapping("/list")
    public Result<TableData<InventoryVO>> getInventoryList(@Valid InventoryQueryDTO queryDTO) {
        return Result.success(inventoryService.pageInventory(queryDTO));
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
