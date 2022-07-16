package org.clever.boot.convert;

import org.clever.format.Formatter;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.Locale;

/**
 * 格式化 {@link InetAddress}，使用 {@link Formatter}
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 23:02 <br/>
 */
final class InetAddressFormatter implements Formatter<InetAddress> {
    @Override
    public String print(InetAddress object, Locale locale) {
        return object.getHostAddress();
    }

    @Override
    public InetAddress parse(String text, Locale locale) throws ParseException {
        try {
            return InetAddress.getByName(text);
        } catch (UnknownHostException ex) {
            throw new IllegalStateException("Unknown host " + text, ex);
        }
    }
}
