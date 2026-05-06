package com.nfu.jasmine.common.utils;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Random;

@Component
public class SerialNumberUtil {
    private static Random random = new Random(System.currentTimeMillis());
    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

    // 根据年月日拼接8位随机数字，生成16位序列号
    public static String generateSerialNumber() {
        // 获取当前日期的年月日
        LocalDate currentDate = LocalDate.now();
        String yearMonthDay = currentDate.format(formatter);

        // 生成8位随机数字
        int randomNumber = random.nextInt(90000000) + 10000000;
        String randomDigits = String.valueOf(randomNumber);

        return yearMonthDay + randomDigits;
    }
}
