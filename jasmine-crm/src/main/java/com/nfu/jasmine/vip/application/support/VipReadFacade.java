package com.nfu.jasmine.vip.application.support;

import com.nfu.jasmine.vip.model.entity.Vip;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 会员读模型内部门面。
 * <p>
 * 预约与交易侧只依赖该门面，后续切换远程调用时收敛改造面。
 */
public interface VipReadFacade {

	boolean existsById(Integer vipId);

	Vip findById(Integer vipId);

	Vip resolve(Integer vipId, String vid, String phone);

	List<Integer> findIdsByNameOrPhone(String name, String phone);

	Map<Integer, Vip> findByIds(Collection<Integer> vipIds);
}
