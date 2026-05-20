package com.nfu.jasmine.iam.web.internal;

import com.nfu.jasmine.common.dto.internal.UserBasicDTO;
import com.nfu.jasmine.common.enums.ResultCode;
import com.nfu.jasmine.common.exception.BusinessException;
import com.nfu.jasmine.iam.application.IUserService;
import com.nfu.jasmine.iam.model.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户内部接口 —— 仅供服务间调用，网关层拦截外部访问。
 */
@Tag(name = "内部接口")
@RestController
@RequestMapping("/internal/user")
public class UserInternalController {

    private final IUserService userService;

    public UserInternalController(IUserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "根据ID查询用户基本信息（内部）")
    @GetMapping("/{id}")
    public UserBasicDTO getUserById(@PathVariable Integer id) {
        User user = userService.getActiveUserById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在！");
        }
        return toUserBasicDTO(user);
    }

    private UserBasicDTO toUserBasicDTO(User user) {
        UserBasicDTO dto = new UserBasicDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        // User 实体没有 realName 字段，暂用 username 填充
        dto.setRealName(user.getUsername());
        return dto;
    }
}
