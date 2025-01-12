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
    public static final String LINE_FIRST /* */ = "╔══════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════";
    public static final String LINE_LAST /*  */ = "╚══════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════";
    public static final String LINE_START /* */ = "║ ";

    protected static void printConfig(Logger log, String title, Object logs) {
        String sb = "\n" + LINE_FIRST + "\n" +
            "╠═════ " + StringUtils.trim(title) + " ═════\n" +
            JacksonMapper.getInstance().toJsonPretty(logs) + "\n" +
            LINE_LAST;
        log.info(sb);
    }

    public static void printConfig(Logger log, String title, String[] logs) {
        List<String> lines = Arrays.stream(logs).map(line -> LINE_START + line).collect(Collectors.toList());
        if (lines.isEmpty()) {
            lines.add(LINE_START);
        }
        String sb = "\n" + LINE_FIRST + "\n" +
            "╠═════ " + StringUtils.trim(title) + " ═════\n" +
            StringUtils.join(lines, "\n") + "\n" +
            LINE_LAST;
        log.info(sb);
    }
}
