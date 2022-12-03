package org.clever.core;

import org.apache.commons.lang3.StringUtils;
import org.clever.core.mapper.JacksonMapper;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/12/07 13:45 <br/>
 */
public class BannerUtils {
    public static final String DOUBLE_LINE = "# =============================================================================================================================================================";
    public static final String LINE_START = "# ";

    protected static void printBanner(Logger log, String title, Object props) {
        String sb = "\n" + DOUBLE_LINE + "\n" +
                "# ====== " + StringUtils.trim(title) + " ======\n" +
                JacksonMapper.getInstance().toJsonPretty(props) + "\n" +
                DOUBLE_LINE;
        log.info(sb);
    }

    public static void printBanner(Logger log, String title, String[] props) {
        List<String> lines = Arrays.stream(props).map(line -> LINE_START + line).collect(Collectors.toList());
        String sb = "\n" + DOUBLE_LINE + "\n" +
                "# ====== " + StringUtils.trim(title) + " ======\n" +
                StringUtils.join(lines, "\n") + "\n" +
                DOUBLE_LINE;
        log.info(sb);
    }
}