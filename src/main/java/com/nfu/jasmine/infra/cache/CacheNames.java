package com.nfu.jasmine.infra.cache;

/**
 * 统一收口缓存名称，避免同一个缓存被各处硬编码成不同字符串。
 */
public final class CacheNames {
    public static final String USER = "user";
    public static final String MENU_LIST = "menuList";
    public static final String ROLE_LIST = "roleList";
    public static final String FLOWER_LIST = "flowerList";
    public static final String FLOWER_DETAIL = "flowerDetail";

    private CacheNames() {
    }
}
