package com.nfu.jasmine.sales.application;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nfu.jasmine.common.vo.TableData;
import com.nfu.jasmine.sales.web.dto.SalesQueryDTO;
import com.nfu.jasmine.sales.web.dto.SalesSaveDTO;
import com.nfu.jasmine.sales.model.entity.Sales;
import com.nfu.jasmine.sales.web.vo.SalesVO;
import com.nfu.jasmine.sales.web.vo.TodayBusinessSummaryVO;

import java.util.List;

public interface ISalesService extends IService<Sales> {
    List<SalesVO> listSales();

    TableData<SalesVO> pageSales(SalesQueryDTO queryDTO);

    SalesVO getSalesDetail(Integer id);

    TodayBusinessSummaryVO getTodayBusinessSummary();

    void saveSales(SalesSaveDTO salesDTO, Integer operatorId);

    void updateSales(SalesSaveDTO salesDTO, Integer operatorId);

    void deleteSales(Integer id);
}
