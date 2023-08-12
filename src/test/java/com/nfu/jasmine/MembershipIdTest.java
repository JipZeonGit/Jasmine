package com.nfu.jasmine;

import com.nfu.jasmine.common.utils.MembershipIdUtil;
import org.junit.jupiter.api.Test;
import org.junit.Assert;

public class MembershipIdTest {
    // 会员卡号生成单元测试
    @Test
    public void testGenerateMembershipCardNumber() {
        String cardNumber = MembershipIdUtil.generateMembershipCardNumber();
        System.out.println(cardNumber);

        // 正确的卡号应该以1开头并且长度为11
        Assert.assertEquals("1", cardNumber.substring(0, 1));
        Assert.assertEquals(11, cardNumber.length());

        // 生成多个卡号时，不能有重复的卡号
        for (int i = 0; i < 100; i++) {
            String newCardNumber = MembershipIdUtil.generateMembershipCardNumber();
            Assert.assertNotEquals(cardNumber, newCardNumber);
        }
    }
}
