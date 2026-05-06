package com.nfu.jasmine.common.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

public final class BusinessNoUtil {
    // 单号按“前缀 + 时间戳 + 随机尾号”生成，开发阶段足够直观且便于排查。
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private BusinessNoUtil() {
    }

    public static String generateSalesOrderNo() {
        return "SO" + generateSuffix();
    }

    public static String generateInventoryBizNo() {
        return "INV" + generateSuffix();
    }

    private static String generateSuffix() {
        return LocalDateTime.now().format(FORMATTER) + ThreadLocalRandom.current().nextInt(1000, 9999);
    }
}
