package org.clever.format.number.money;

import org.clever.format.Formatter;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import java.util.Locale;

/**
 * JSR-354 {@link javax.money.CurrencyUnit}值的格式化程序，货币转为字符串
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/30 19:53 <br/>
 */
public class CurrencyUnitFormatter implements Formatter<CurrencyUnit> {
    @Override
    public String print(CurrencyUnit object, Locale locale) {
        return object.getCurrencyCode();
    }

    @Override
    public CurrencyUnit parse(String text, Locale locale) {
        return Monetary.getCurrency(text);
    }
}
