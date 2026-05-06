package com.nfu.jasmine.infra.notification.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nfu.jasmine.infra.notification.model.entity.SiteMessage;
import com.nfu.jasmine.infra.notification.persistence.mapper.SiteMessageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 站内信服务，负责写入、查询和标记已读。
 */
@Service
@RequiredArgsConstructor
public class SiteMessageService {

    private final SiteMessageMapper siteMessageMapper;

    /**
     * 按接收人批量写入站内信（默认未读）。
     */
    public void createForUsers(String bizType, String bizId, List<Integer> receiverUserIds, String title, String content) {
        if (receiverUserIds == null || receiverUserIds.isEmpty()) {
            return;
        }
        Date now = new Date();
        for (Integer receiverUserId : receiverUserIds) {
            if (receiverUserId == null) {
                continue;
            }
            SiteMessage msg = SiteMessage.builder()
                    .bizType(bizType)
                    .bizId(bizId)
                    .receiverUserId(receiverUserId)
                    .title(title)
                    .content(content)
                    .isRead(0)
                    .createdAt(now)
                    .build();
            siteMessageMapper.insert(msg);
        }
    }

    /**
     * 获取未读站内信数量。
     */
    public long getUnreadCount(Integer receiverUserId) {
        return siteMessageMapper.selectCount(new LambdaQueryWrapper<SiteMessage>()
                .eq(SiteMessage::getReceiverUserId, receiverUserId)
                .eq(SiteMessage::getIsRead, 0));
    }

    /**
     * 分页查询站内信（未读优先，按时间倒序）。
     */
    public Page<SiteMessage> pageMessages(Integer receiverUserId, int pageNo, int pageSize) {
        Page<SiteMessage> page = new Page<>(pageNo, pageSize);
        LambdaQueryWrapper<SiteMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SiteMessage::getReceiverUserId, receiverUserId)
                .eq(SiteMessage::getIsRead, 0)
                .orderByDesc(SiteMessage::getCreatedAt);
        return siteMessageMapper.selectPage(page, wrapper);
    }

    /**
     * 将指定消息标记为已读。
     */
    public void markAsRead(Long id, Integer receiverUserId) {
        siteMessageMapper.update(
                null,
                new LambdaUpdateWrapper<SiteMessage>()
                        .eq(SiteMessage::getId, id)
                        .eq(SiteMessage::getReceiverUserId, receiverUserId)
                        .eq(SiteMessage::getIsRead, 0)
                        .set(SiteMessage::getIsRead, 1)
        );
    }

    /**
     * 将全部未读消息标记为已读。
     */
    public void markAllAsRead(Integer receiverUserId) {
        siteMessageMapper.update(
                null,
                new LambdaUpdateWrapper<SiteMessage>()
                        .eq(SiteMessage::getReceiverUserId, receiverUserId)
                        .eq(SiteMessage::getIsRead, 0)
                        .set(SiteMessage::getIsRead, 1)
        );
    }
}
