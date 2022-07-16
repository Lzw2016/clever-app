package org.clever.format.number.money;

import org.clever.format.Formatter;

import javax.money.MonetaryAmount;
import javax.money.format.MonetaryAmountFormat;
import javax.money.format.MonetaryFormats;
import java.util.Locale;

/**
 * JSR-354 {@link javax.money.MonetaryAmount}值的格式化程序，
 * 委托给{@link javax.money.format.MonetaryAmountFormat#format}和{@link javax.money.format.MonetaryAmountFormat#parse}
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/30 19:54 <br/>
 */
public class MonetaryAmountFormatter implements Formatter<MonetaryAmount> {
    private String formatName;

    /**
     * Create a locale-driven MonetaryAmountFormatter.
     */
    public MonetaryAmountFormatter() {
    }

    /**
     * Create a new MonetaryAmountFormatter for the given format name.
     *
     * @param formatName the format name, to be resolved by the JSR-354
     *                   provider at runtime
     */
    public MonetaryAmountFormatter(String formatName) {
        this.formatName = formatName;
    }

    /**
     * Specify the format name, to be resolved by the JSR-354 provider
     * at runtime.
     * <p>Default is none, obtaining a {@link MonetaryAmountFormat}
     * based on the current locale.
     */
    public void setFormatName(String formatName) {
        this.formatName = formatName;
    }

    @Override
    public String print(MonetaryAmount object, Locale locale) {
        return getMonetaryAmountFormat(locale).format(object);
    }

    @Override
    public MonetaryAmount parse(String text, Locale locale) {
        return getMonetaryAmountFormat(locale).parse(text);
    }

    /**
     * Obtain a MonetaryAmountFormat for the given locale.
     * <p>The default implementation simply calls
     * {@link javax.money.format.MonetaryFormats#getAmountFormat}
     * with either the configured format name or the given locale.
     *
     * @param locale the current locale
     * @return the MonetaryAmountFormat (never {@code null})
     * @see #setFormatName
     */
    protected MonetaryAmountFormat getMonetaryAmountFormat(Locale locale) {
        if (this.formatName != null) {
            return MonetaryFormats.getAmountFormat(this.formatName);
        } else {
            return MonetaryFormats.getAmountFormat(locale);
        }
    }
}
