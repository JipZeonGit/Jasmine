package com.nfu.jasmine.infra.notification.web;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nfu.jasmine.common.vo.Result;
import com.nfu.jasmine.common.vo.TableData;
import com.nfu.jasmine.infra.notification.model.entity.SiteMessage;
import com.nfu.jasmine.infra.notification.service.SiteMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

    @Operation(summary = "获取未读站内信数量")
    @GetMapping("/unread-count")
    public Result<Long> getUnreadCount() {
        return Result.success(siteMessageService.getUnreadCount());
    }

    @Operation(summary = "分页查询站内信列表")
    @GetMapping("/list")
    public Result<TableData<SiteMessage>> listMessages(
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize) {
        Page<SiteMessage> page = siteMessageService.pageMessages(pageNo, pageSize);
        TableData<SiteMessage> data = new TableData<>();
        data.setTotal(page.getTotal());
        data.setRows(page.getRecords());
        return Result.success(data);
    }

    @Operation(summary = "标记单条站内信为已读")
    @PutMapping("/read/{id}")
    public Result<Void> markAsRead(@PathVariable Long id) {
        siteMessageService.markAsRead(id);
        return Result.success("已标记为已读");
    }

    @Operation(summary = "全部标记为已读")
    @PutMapping("/read-all")
    public Result<Void> markAllAsRead() {
        siteMessageService.markAllAsRead();
        return Result.success("全部已读");
    }
}
