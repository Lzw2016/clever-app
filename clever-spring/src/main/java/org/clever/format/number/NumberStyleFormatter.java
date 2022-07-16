package org.clever.format.number;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * 使用NumberFormat的数字样式的通用数字格式化程序
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/30 17:40 <br/>
 */
public class NumberStyleFormatter extends AbstractNumberFormatter {
    /**
     * 自定义格式
     */
    private String pattern;

    public NumberStyleFormatter() {
    }

    public NumberStyleFormatter(String pattern) {
        this.pattern = pattern;
    }

    /**
     * 自定义格式
     *
     * @see java.text.DecimalFormat#applyPattern(String)
     */
    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    @Override
    public NumberFormat getNumberFormat(Locale locale) {
        NumberFormat format = NumberFormat.getInstance(locale);
        if (!(format instanceof DecimalFormat)) {
            if (this.pattern != null) {
                throw new IllegalStateException("Cannot support pattern for non-DecimalFormat: " + format);
            }
            return format;
        }
        DecimalFormat decimalFormat = (DecimalFormat) format;
        decimalFormat.setParseBigDecimal(true);
        if (this.pattern != null) {
            decimalFormat.applyPattern(this.pattern);
        }
        return decimalFormat;
    }
}
