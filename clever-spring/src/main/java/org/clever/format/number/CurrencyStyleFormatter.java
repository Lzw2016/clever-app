package org.clever.format.number;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Currency;
import java.util.Locale;

/**
 * BigDecimal的货币格式化
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/30 17:44 <br/>
 */
public class CurrencyStyleFormatter extends AbstractNumberFormatter {
    /**
     * 小数位数
     */
    private int fractionDigits = 2;
    /**
     * 舍入模式
     */
    private RoundingMode roundingMode;
    private Currency currency;
    /**
     * 自定义格式
     */
    private String pattern;

    public void setFractionDigits(int fractionDigits) {
        this.fractionDigits = fractionDigits;
    }

    public void setRoundingMode(RoundingMode roundingMode) {
        this.roundingMode = roundingMode;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
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
    public BigDecimal parse(String text, Locale locale) throws ParseException {
        BigDecimal decimal = (BigDecimal) super.parse(text, locale);
        if (this.roundingMode != null) {
            decimal = decimal.setScale(this.fractionDigits, this.roundingMode);
        } else {
            // noinspection BigDecimalMethodWithoutRoundingCalled
            decimal = decimal.setScale(this.fractionDigits);
        }
        return decimal;
    }

    @Override
    protected NumberFormat getNumberFormat(Locale locale) {
        DecimalFormat format = (DecimalFormat) NumberFormat.getCurrencyInstance(locale);
        format.setParseBigDecimal(true);
        format.setMaximumFractionDigits(this.fractionDigits);
        format.setMinimumFractionDigits(this.fractionDigits);
        if (this.roundingMode != null) {
            format.setRoundingMode(this.roundingMode);
        }
        if (this.currency != null) {
            format.setCurrency(this.currency);
        }
        if (this.pattern != null) {
            format.applyPattern(this.pattern);
        }
        return format;
    }
}
