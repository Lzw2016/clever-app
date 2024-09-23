//package org.clever.web.mvc;
//
//import lombok.extern.slf4j.Slf4j;
//import org.clever.core.DateUtils;
//import org.springframework.web.support.mvc.format.DateTimeFormatters;
//import org.springframework.web.support.mvc.format.WebConversionService;
//import org.junit.jupiter.api.Test;
//
//import java.util.Date;
//
///**
// * 作者：lizw <br/>
// * 创建时间：2023/01/17 14:06 <br/>
// */
//@Slf4j
//public class ConversionTest {
//    @Test
//    public void t01() {
//        WebConversionService conversion = new WebConversionService(
//                new DateTimeFormatters().dateFormat("yyyy-MM-dd HH:mm:ss")
//                        .timeFormat("HH:mm:ss")
//                        .dateTimeFormat("yyyy-MM-dd HH:mm:ss")
//        );
//        Date date = conversion.convert("2023-01-17 13:59:23", Date.class);
//        log.info("--> {}", DateUtils.formatToString(date));
//        date = conversion.convert("2023-01-17", Date.class);
//        log.info("--> {}", DateUtils.formatToString(date));
//    }
//}
