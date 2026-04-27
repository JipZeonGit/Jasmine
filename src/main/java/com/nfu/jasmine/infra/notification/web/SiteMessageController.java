package com.nfu.jasmine.infra.notification.web;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nfu.jasmine.common.vo.Result;
import com.nfu.jasmine.common.vo.TableData;
import com.nfu.jasmine.infra.security.CurrentUserProvider;
import com.nfu.jasmine.infra.notification.model.entity.SiteMessage;
import com.nfu.jasmine.infra.notification.service.SiteMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 站内信接口，供前端消息中心调用。
 */
@Tag(name = "站内信接口")
@RestController
@RequestMapping("/site-message")
@RequiredArgsConstructor
public class SiteMessageController {

    private final SiteMessageService siteMessageService;
    private final CurrentUserProvider currentUserProvider;

    @Operation(summary = "获取未读站内信数量")
    @GetMapping("/unread-count")
    public Result<Long> getUnreadCount(HttpServletRequest request) {
        Integer currentUserId = currentUserProvider.requireCurrentUserId(request);
        return Result.success(siteMessageService.getUnreadCount(currentUserId));
    }

    @Operation(summary = "分页查询站内信列表")
    @GetMapping("/list")
    public Result<TableData<SiteMessage>> listMessages(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize) {
        Integer currentUserId = currentUserProvider.requireCurrentUserId(request);
        Page<SiteMessage> page = siteMessageService.pageMessages(currentUserId, pageNo, pageSize);
        TableData<SiteMessage> data = new TableData<>();
        data.setTotal(page.getTotal());
        data.setRows(page.getRecords());
        return Result.success(data);
    }

    @Operation(summary = "标记单条站内信为已读")
    @PutMapping("/read/{id}")
    public Result<Void> markAsRead(@PathVariable Long id, HttpServletRequest request) {
        Integer currentUserId = currentUserProvider.requireCurrentUserId(request);
        siteMessageService.markAsRead(id, currentUserId);
        return Result.success("已标记为已读");
    }

    @Operation(summary = "全部标记为已读")
    @PutMapping("/read-all")
    public Result<Void> markAllAsRead(HttpServletRequest request) {
        Integer currentUserId = currentUserProvider.requireCurrentUserId(request);
        siteMessageService.markAllAsRead(currentUserId);
        return Result.success("全部已读");
    }
}
