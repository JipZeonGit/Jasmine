package com.nfu.jasmine.infra.notification.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nfu.jasmine.infra.notification.model.entity.SiteMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SiteMessageMapper extends BaseMapper<SiteMessage> {
}
