package com.nfu.jasmine.flower.application;

import com.nfu.jasmine.flower.model.entity.Flower;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author jipzeongit
 * @since 2023-07-06
 */
public interface IFlowerService extends IService<Flower> {
    java.util.List<Flower> listAllFlowers();

    Flower getFlowerDetailById(Integer id);

    void addFlower(Flower flower);

    void updateFlower(Flower flower);

    void deleteFlowerById(Integer id);

}
