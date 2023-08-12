package com.nfu.jasmine.common.utils;

import org.springframework.stereotype.Component;
import java.util.Random;
import java.util.BitSet;

@Component
public class MembershipIdUtil {
    private static Random random = new Random();
    private static BitSet generatedNumbers = new BitSet(1000000000);

    // 生成11位“1”开头的会员卡号
    public static String generateMembershipCardNumber() {
        int randomNumber;
        do {
            randomNumber = random.nextInt(1000000000);
        } while (generatedNumbers.get(randomNumber));

        generatedNumbers.set(randomNumber);
        String membershipCardNumber = String.format("%010d", randomNumber);

        return "1" + membershipCardNumber;
    }
}
