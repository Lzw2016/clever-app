//package org.clever.data.jdbc;
//
//import org.clever.core.RenameStrategy;
//import lombok.Data;
//import lombok.extern.slf4j.Slf4j;
//import org.junit.jupiter.api.Test;
//
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
///**
// * 作者：lizw <br/>
// * 创建时间：2021/12/06 16:39 <br/>
// */
//@Slf4j
//public class JdbcTest {
//
//    @Test
//    public void t01() {
//        Jdbc jdbc = BaseTest.newJdbcDataSource();
//        Map<String, Object> paramMap = new HashMap<>();
//        String sql = "select * from YW_XJZL where DANJ_NO=:DANJ_NO and HANGHAO=:HANGHAO";
//        paramMap.put("DANJ_NO", "CKD11500000026");
//        paramMap.put("HANGHAO", 2);
//        Map<String, Object> map = jdbc.queryOne(sql, paramMap, RenameStrategy.ToCamel);
//        log.info(" #1 ---> {}", map);
//        map = jdbc.queryOne(sql, paramMap, RenameStrategy.ToUnderline);
//        log.info(" #2 ---> {}", map);
//
//        sql = "select * from YW_XJZL where DANJ_NO=:DANJ_NO";
//        paramMap.remove("HANGHAO");
//        List<?> list = jdbc.queryMany(sql, paramMap, RenameStrategy.ToCamel);
//        log.info(" #3 ---> {}", list);
//
//        list = jdbc.queryMany(sql, paramMap, YwXjzl.class);
//        log.info(" #3 ---> {}", list);
//
//        jdbc.close();
//    }
//
//    @Test
//    public void t02() {
//        Jdbc jdbc = BaseTest.newJdbcDataSource();
//        Map<String, Object> paramMap = new HashMap<>();
//
//        String sql = "select * from YW_XJZL where DANJ_NO=:DANJ_NO and HANGHAO=:HANGHAO";
//        paramMap.put("DANJ_NO", "CKD11500000026");
//        paramMap.put("HANGHAO", 2);
//        YwXjzl ywXjzl = jdbc.queryOne(sql, paramMap, YwXjzl.class);
//        log.info(" #1 ---> {}", ywXjzl);
//
//        ywXjzl = jdbc.queryOneForTable("YW_XJZL", paramMap, YwXjzl.class);
//        log.info(" #2 ---> {}", ywXjzl);
//
//        paramMap.remove("HANGHAO");
//        List<?> list = jdbc.queryManyForTable("YW_XJZL", paramMap, YwXjzl.class);
//        log.info(" #3 ---> {}", list);
//
//        jdbc.close();
//    }
//
//    @Data
//    public static class YwXjzl {
//        private String danjNo;
//        private Integer hanghao;
//        private String yezId;
//        private String shangpId;
//        private String lot;
//        private String huowId;
//        private String shijhwId;
//        private String jihuaNum;
//        private String shijNum;
//        private String kub;
//        private String kufangNo;
//        private String quyuNo;
//        private String chailGroup;
//        private String yewType;
//        private String liushNo;
//        private String shangjlsNo;
//        private String chukuOrder;
//        private String jihFlg;
//        private String renwState;
//        private String tiqjhState;
//        private String jianhWay;
//        private String zuoyWay;
//        private String zuoyCategory;
//        private String zhengsSign;
//        private String shangpType;
//        private String chaifdNo;
//        private String fenpdNo;
//        private String jianhOrder;
//        private String lotRequest;
//        private String price;
//        private String pingxNo;
//        private String tuopBarcode;
//        private String chonghFlg;
//        private String zuijjhFlg;
//        private String zancqNo;
//        private String zhouzxNo;
//        private String jiachdNo;
//        private String fuhtNo;
//        private String zhantNo;
//        private String huowBuffer;
//        private String liushBarcode;
//        private String daytmFlg;
//        private java.sql.Timestamp shengchenTime;
//        private java.sql.Timestamp gengxTime;
//        private String gengxIp;
//        private String jianxlNo;
//        private String zhongyResidual;
//        private String fenpdGroup;
//        private String hanghaoKpd;
//        private String zhuanyNo;
//        private String zhuanyState;
//        private String zhuanyName;
//        private String bociNo;
//        private String nfuhType;
//        private String tuopNum;
//        private String jihwcFlg;
//        private String zzbs;
//        private java.sql.Timestamp rukTime;
//        private String houseId;
//        private String zhuanyId;
//        private String zuijjhType;
//        private String bzjhwNo;
//        private String cscJihwcFlg;
//        private String jxtId;
//        private String jxwId;
//        private String assignFlg;
//        private String assignState;
//        private String kucState;
//        private String bjbbFpd;
//        private String wxFlg;
//    }
//}
