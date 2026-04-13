package com.nfu.jasmine.flower.application.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nfu.jasmine.flower.model.entity.Flower;
import com.nfu.jasmine.flower.persistence.mapper.FlowerMapper;
import com.nfu.jasmine.flower.application.IFlowerService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nfu.jasmine.infra.cache.CacheNames;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author jipzeongit
 * @since 2023-07-06
 */
@Service
public class FlowerServiceImpl extends ServiceImpl<FlowerMapper, Flower> implements IFlowerService {

    @Override
    @Cacheable(value = CacheNames.FLOWER_LIST, key = "'all'")
    public List<Flower> listAllFlowers() {
        return this.list(new LambdaQueryWrapper<Flower>().orderByAsc(Flower::getId));
    }

    @Override
    @Cacheable(value = CacheNames.FLOWER_DETAIL, key = "#id")
    public Flower getFlowerDetailById(Integer id) {
        return this.getById(id);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheNames.FLOWER_LIST, allEntries = true),
            @CacheEvict(value = CacheNames.FLOWER_DETAIL, allEntries = true)
    })
    public void addFlower(Flower flower) {
        this.save(flower);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheNames.FLOWER_LIST, allEntries = true),
            @CacheEvict(value = CacheNames.FLOWER_DETAIL, key = "#flower.id")
    })
    public void updateFlower(Flower flower) {
        this.updateById(flower);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheNames.FLOWER_LIST, allEntries = true),
            @CacheEvict(value = CacheNames.FLOWER_DETAIL, key = "#id")
    })
    public void deleteFlowerById(Integer id) {
        this.removeById(id);
    }

}
