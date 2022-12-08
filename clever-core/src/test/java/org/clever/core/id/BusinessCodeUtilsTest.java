package org.clever.core.id;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import java.util.Date;

/**
 * 作者：lizw <br/>
 * 创建时间：2022/12/05 12:01 <br/>
 */
@Slf4j
public class BusinessCodeUtilsTest {
    @Test
    public void t01() {
        createTest("CK${yy}${MMdd}-${seq3}END", 103L);
        createTest("CK${yy}-${MMdd}-${seq3}END", 3L);
        createTest("CK${yy}-${MMdd}-${seq3}END", 1032L);
        createTest("${yy}${MMdd}${seq3}", 2L);
        createTest("${yyMMdd}${seq3}", 1032L);
        createTest("${yyMMdd}${seq3}", -1L);
        createTest("${yyyyMMdd}${seq3}", -1L);
        createTest("${yyyyMMdd}_${seq}", 9000000000000000000L);
        createTest("${yyyyMMdd}_${seq6}", 9000000000000000000L);
        createTest("${yyyyMMdd}_${id6}", 123456L);
        createTest("${yyyyMMdd}_${id6}", 1234567L);
    }

    private void createTest(String pattern, Long seq) {
        String code = BusinessCodeUtils.create(pattern, new Date(), seq);
        log.info("{} --> {}", StringUtils.rightPad(pattern, 32), code);
    }

    @Test
    public void t02() {
        long startTime = System.currentTimeMillis();
        int count = 100_0000;
        for (int i = 0; i < count; i++) {
            BusinessCodeUtils.create("CS${yyyyMMdd}${id6}", new Date(), i);
        }
        long endTime = System.currentTimeMillis();
        // 540ms/次 | 1849ms
        log.info("{}ms/次 | {}ms", count / (endTime - startTime), (endTime - startTime));
    }
}
