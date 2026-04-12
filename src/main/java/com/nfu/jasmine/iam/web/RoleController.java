package com.nfu.jasmine.iam.web;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nfu.jasmine.common.enums.ResultCode;
import com.nfu.jasmine.common.vo.Result;
import com.nfu.jasmine.common.vo.TableData;
import com.nfu.jasmine.iam.web.dto.RoleQueryDTO;
import com.nfu.jasmine.iam.web.dto.RoleSaveDTO;
import com.nfu.jasmine.iam.model.entity.Role;
import com.nfu.jasmine.iam.application.IRoleService;
import com.nfu.jasmine.iam.web.vo.RoleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author jipzeongit
 * @since 2023-05-29
 */
@Tag(name = "角色接口列表")
@Validated
@RestController
@RequestMapping("/role")
public class RoleController {

    @Autowired
    private IRoleService roleService;

    // 带分页的角色列表查询接口，可以按角色名称搜索
    @Operation(summary = "查询角色")
    @GetMapping("/list")
    public Result<TableData<RoleVO>> getUserList(@Valid RoleQueryDTO queryDTO) {
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasLength(queryDTO.getRoleName()), Role::getRoleName, queryDTO.getRoleName());
        wrapper.orderByAsc(Role::getRoleId);

        Page<Role> page = new Page<>(queryDTO.getPageNo(), queryDTO.getPageSize());
        roleService.page(page, wrapper);

        TableData<RoleVO> data = new TableData<>();
        data.setTotal(page.getTotal());
        data.setRows(page.getRecords().stream().map(this::toRoleVO).collect(Collectors.toList()));
        return Result.success(data);
    }

    // 创建并保存一个新的系统角色
    @Operation(summary = "新增角色")
    @PostMapping
    public Result<?> addRole(@Valid @RequestBody RoleSaveDTO roleDTO) {
        Role role = new Role();
        BeanUtils.copyProperties(roleDTO, role);
        roleService.addRole(role);
        return Result.success("新增角色成功");
    }

    // 更新某个角色的信息设定，比如它绑定的菜单权限
    @Operation(summary = "修改角色")
    @PutMapping
    public Result<?> updateRole(@Valid @RequestBody RoleSaveDTO roleDTO) {
        if (roleDTO.getRoleId() == null) {
            return Result.fail(ResultCode.VALIDATE_FAILED, "角色ID不能为空！");
        }
        Role role = new Role();
        BeanUtils.copyProperties(roleDTO, role);
        roleService.updateRole(role);
        return Result.success("修改角色成功");
    }

    // 用角色ID来查找某一个具体角色的资料
    @Operation(summary = "根据ID查询单个角色")
    @GetMapping("/{id}")
    public Result<RoleVO> getRoleById(@PathVariable("id") Integer id) {
        Role role = roleService.getRoleById(id);
        return Result.success(toRoleVO(role));
    }

    // 收除对应ID的角色，做逻辑软删处理
    @Operation(summary = "根据ID逻辑删除角色数据")
    @DeleteMapping("/{id}")
    public Result<?> deleteRoleById(@PathVariable("id") Integer id) {
        roleService.deleteRoleById(id);
        return Result.success("删除角色数据成功");
    }

    // 获取整个系统所有的角色数据（不分页）
    @Operation(summary = "查询所有角色")
    @GetMapping("/all")
    public Result<List<RoleVO>> getAllRole() {
        List<RoleVO> roleList = roleService.list().stream().map(this::toRoleVO).collect(Collectors.toList());
        return Result.success(roleList);
    }

    private RoleVO toRoleVO(Role role) {
        if (role == null) {
            return null;
        }
        RoleVO vo = new RoleVO();
        BeanUtils.copyProperties(role, vo);
        return vo;
    }
}