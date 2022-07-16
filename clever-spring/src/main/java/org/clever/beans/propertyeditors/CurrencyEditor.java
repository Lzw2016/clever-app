package org.clever.beans.propertyeditors;

import java.beans.PropertyEditorSupport;
import java.util.Currency;

/**
 * 将货币代码公开为货币对象的文本表示形式
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/04 13:31 <br/>
 *
 * @see java.util.Currency
 */
public class CurrencyEditor extends PropertyEditorSupport {
    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        setValue(Currency.getInstance(text));
    }

    @Override
    public String getAsText() {
        Currency value = (Currency) getValue();
        return (value != null ? value.getCurrencyCode() : "");
    }
}
