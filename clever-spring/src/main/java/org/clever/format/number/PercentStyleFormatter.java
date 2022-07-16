package org.clever.format.number;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * 百分比数值格式化
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/30 17:46 <br/>
 */
public class PercentStyleFormatter extends AbstractNumberFormatter {
    @Override
    protected NumberFormat getNumberFormat(Locale locale) {
        NumberFormat format = NumberFormat.getPercentInstance(locale);
        if (format instanceof DecimalFormat) {
            ((DecimalFormat) format).setParseBigDecimal(true);
        }
        return format;
    }
}
