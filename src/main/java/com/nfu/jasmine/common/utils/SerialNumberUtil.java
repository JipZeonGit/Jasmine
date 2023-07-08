package com.nfu.jasmine.common.utils;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Random;

@Component
public class SerialNumberUtil {
    // 根据年月日拼接8位随机数字，生成16位序列号
    public static String generateSerialNumber() {
        // 获取当前日期的年月日
        LocalDate currentDate = LocalDate.now();
        String yearMonthDay = currentDate.toString().replace("-", "");

        // 生成8位随机数字
        Random random = new Random(System.currentTimeMillis());
        int randomNumber = random.nextInt(90000000) + 10000000;
        String randomDigits = String.valueOf(randomNumber);

        // 拼接单号
        String serialNumber = yearMonthDay + randomDigits;

        return serialNumber;
    }
}
