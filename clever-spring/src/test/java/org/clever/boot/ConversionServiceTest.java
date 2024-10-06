package org.clever.boot;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.format.support.DefaultFormattingConversionService;

import java.math.BigDecimal;

/**
 * 作者：lizw <br/>
 * 创建时间：2022/04/22 16:18 <br/>
 */
@Slf4j
public class ConversionServiceTest {
    @Test
    public void t01() {
        ConfigurableConversionService conversionService = new DefaultFormattingConversionService();
        log.info("--> {}", conversionService.convert("123", Integer.class));
        log.info("--> {}", conversionService.convert("123", long.class));
        log.info("--> {}", conversionService.convert("123", BigDecimal.class));

        DataA dataA = new DataA("123", new BigDecimal("456.789"));
        log.info("--> {}", conversionService.convert(dataA, DataA.class));
        // log.info("--> {}", conversionService.convert(dataA, DataB.class));
    }

    @Data
    static class DataA {
        private String fa;
        private BigDecimal fb;

        public DataA() {
        }

        public DataA(String fa, BigDecimal fb) {
            this.fa = fa;
            this.fb = fb;
        }
    }

    @Data
    static class DataB {
        private Integer fa;
        private String fb;

        public DataB() {
        }

        public DataB(Integer fa, String fb) {
            this.fa = fa;
            this.fb = fb;
        }
    }
}
