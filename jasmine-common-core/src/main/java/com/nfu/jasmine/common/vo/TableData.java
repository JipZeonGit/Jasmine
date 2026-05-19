package com.nfu.jasmine.common.vo;

import lombok.Data;

import java.util.List;

/**
 * <p>
 * 表格分页数据
 * </p>
 *
 * @author jipzeongit
 * @since 2026-04-07
 */
@Data
public class TableData<T> {
    private Long total;
    private List<T> rows;
}
