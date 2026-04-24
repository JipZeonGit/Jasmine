package com.nfu.jasmine.infra.notification.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
     * 写入一条站内信（默认未读）。
     */
    public void create(String bizType, String bizId, String title, String content) {
        SiteMessage msg = SiteMessage.builder()
                .bizType(bizType)
                .bizId(bizId)
                .title(title)
                .content(content)
                .isRead(0)
                .createdAt(new Date())
                .build();
        siteMessageMapper.insert(msg);
    }

    /**
     * 获取未读站内信数量。
     */
    public long getUnreadCount() {
        return siteMessageMapper.selectCount(
                new LambdaQueryWrapper<SiteMessage>().eq(SiteMessage::getIsRead, 0)
        );
    }

    /**
     * 分页查询站内信（未读优先，按时间倒序）。
     */
    public Page<SiteMessage> pageMessages(int pageNo, int pageSize) {
        Page<SiteMessage> page = new Page<>(pageNo, pageSize);
        LambdaQueryWrapper<SiteMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(SiteMessage::getIsRead)
               .orderByDesc(SiteMessage::getCreatedAt);
        return siteMessageMapper.selectPage(page, wrapper);
    }

    /**
     * 将指定消息标记为已读。
     */
    public void markAsRead(Long id) {
        SiteMessage msg = siteMessageMapper.selectById(id);
        if (msg != null && msg.getIsRead() == 0) {
            msg.setIsRead(1);
            siteMessageMapper.updateById(msg);
        }
    }

    /**
     * 将全部未读消息标记为已读。
     */
    public void markAllAsRead() {
        List<SiteMessage> unreads = siteMessageMapper.selectList(
                new LambdaQueryWrapper<SiteMessage>().eq(SiteMessage::getIsRead, 0)
        );
        for (SiteMessage msg : unreads) {
            msg.setIsRead(1);
            siteMessageMapper.updateById(msg);
        }
    }
}
