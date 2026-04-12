package com.nfu.jasmine.iam.web.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class MenuVO {
    private Integer menuId;
    private String component;
    private String path;
    private String redirect;
    private String name;
    private String title;
    private String icon;
    private Integer parentId;
    private String isLeaf;
    private Boolean hidden;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<MenuVO> children;

    private Map<String, Object> meta = new HashMap<>();

    public Map<String, Object> getMeta() {
        meta.put("title", this.title);
        meta.put("icon", this.icon);
        return meta;
    }
}