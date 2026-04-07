package com.nfu.jasmine;

import com.nfu.jasmine.common.utils.MembershipIdUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class MembershipIdTest {
    // 会员卡号生成单元测试
    @Test
    public void testGenerateMembershipCardNumber() {
        String cardNumber = MembershipIdUtil.generateMembershipCardNumber();
        System.out.println(cardNumber);

        // 正确的卡号应当以 1 开头并且长度为 11
        assertEquals("1", cardNumber.substring(0, 1));
        assertEquals(11, cardNumber.length());

        // 生成多个卡号时，不能出现重复的卡号
        for (int i = 0; i < 100; i++) {
            String newCardNumber = MembershipIdUtil.generateMembershipCardNumber();
            assertNotEquals(cardNumber, newCardNumber);
        }
    }
}
