package com.nfu.jasmine.vip.application.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nfu.jasmine.vip.model.entity.Vip;
import com.nfu.jasmine.vip.persistence.mapper.VipMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class VipReadFacadeImpl implements VipReadFacade {
	private final VipMapper vipMapper;

	public VipReadFacadeImpl(VipMapper vipMapper) {
		this.vipMapper = vipMapper;
	}

	@Override
	public boolean existsById(Integer vipId) {
		return vipId != null && vipMapper.selectById(vipId) != null;
	}

	@Override
	public Vip findById(Integer vipId) {
		return vipId == null ? null : vipMapper.selectById(vipId);
	}

	@Override
	public Vip resolve(Integer vipId, String vid, String phone) {
		if (vipId != null) {
			return vipMapper.selectById(vipId);
		}
		if (StringUtils.hasLength(vid)) {
			return vipMapper.selectOne(new LambdaQueryWrapper<Vip>().eq(Vip::getVid, vid));
		}
		if (StringUtils.hasLength(phone)) {
			return vipMapper.selectOne(new LambdaQueryWrapper<Vip>().eq(Vip::getPhone, phone));
		}
		return null;
	}

	@Override
	public List<Integer> findIdsByNameOrPhone(String name, String phone) {
		LambdaQueryWrapper<Vip> wrapper = new LambdaQueryWrapper<>();
		wrapper.like(StringUtils.hasLength(name), Vip::getName, name);
		wrapper.like(StringUtils.hasLength(phone), Vip::getPhone, phone);
		return vipMapper.selectList(wrapper).stream().map(Vip::getId).toList();
	}

	@Override
	public Map<Integer, Vip> findByIds(Collection<Integer> vipIds) {
		if (vipIds == null || vipIds.isEmpty()) {
			return Collections.emptyMap();
		}
		return vipMapper.selectBatchIds(vipIds).stream()
				.collect(Collectors.toMap(Vip::getId, vip -> vip, (left, right) -> left, LinkedHashMap::new));
	}
}
