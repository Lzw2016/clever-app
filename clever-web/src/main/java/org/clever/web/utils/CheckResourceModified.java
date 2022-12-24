package org.clever.web.utils;

import org.clever.util.Assert;
import org.clever.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 作者：lizw <br/>
 * 创建时间：2022/12/24 23:31 <br/>
 */
public class CheckResourceModified {
    private static final List<String> SAFE_METHODS = Arrays.asList("GET", "HEAD");
    /**
     * 模式匹配ETag标题中的多个字段值，如 "If-Match", "If-None-Match".
     *
     * @see <a href="https://tools.ietf.org/html/rfc7232#section-2.3">Section 2.3 of RFC 7232</a>
     */
    private static final Pattern ETAG_HEADER_VALUE_PATTERN = Pattern.compile("\\*|\\s*((W\\/)?(\"[^\"]*\"))\\s*,?");
    /**
     * HTTP RFC中指定的日期格式
     *
     * @see <a href="https://tools.ietf.org/html/rfc7231#section-7.1.1.1">Section 7.1.1.1 of RFC 7231</a>
     */
    private static final String[] DATE_FORMATS = new String[]{
            "EEE, dd MMM yyyy HH:mm:ss zzz",
            "EEE, dd-MMM-yy HH:mm:ss zzz",
            "EEE MMM dd HH:mm:ss yyyy"
    };
    private static final TimeZone GMT = TimeZone.getTimeZone("GMT");

    private boolean notModified = false;
    private final HttpServletRequest req;
    private final HttpServletResponse res;

    public CheckResourceModified(HttpServletRequest req, HttpServletResponse res) {
        Assert.notNull(req, "参数 req 不能为 null");
        Assert.notNull(res, "参数 res 不能为 null");
        this.req = req;
        this.res = res;
    }

    /**
     * 判断资源是否发生变化
     */
    public boolean checkNotModified(long lastModifiedTimestamp) {
        return checkNotModified(null, lastModifiedTimestamp);
    }

    /**
     * 判断资源是否发生变化
     */
    public boolean checkNotModified(String etag, long lastModifiedTimestamp) {
        if (this.notModified || HttpStatus.OK.value() != res.getStatus()) {
            return this.notModified;
        }
        // Evaluate conditions in order of precedence. See https://tools.ietf.org/html/rfc7232#section-6
        if (validateIfUnmodifiedSince(lastModifiedTimestamp)) {
            if (this.notModified) {
                res.setStatus(HttpStatus.PRECONDITION_FAILED.value());
            }
            return this.notModified;
        }
        boolean validated = validateIfNoneMatch(etag);
        if (!validated) {
            validateIfModifiedSince(lastModifiedTimestamp);
        }
        // Update response
        boolean isHttpGetOrHead = SAFE_METHODS.contains(req.getMethod());
        if (this.notModified) {
            res.setStatus(isHttpGetOrHead ? HttpStatus.NOT_MODIFIED.value() : HttpStatus.PRECONDITION_FAILED.value());
        }
        if (isHttpGetOrHead) {
            if (lastModifiedTimestamp > 0 && parseDateValue(res.getHeader(HttpHeaders.LAST_MODIFIED)) == -1) {
                res.setDateHeader(HttpHeaders.LAST_MODIFIED, lastModifiedTimestamp);
            }
            if (StringUtils.hasLength(etag) && res.getHeader(HttpHeaders.ETAG) == null) {
                res.setHeader(HttpHeaders.ETAG, padEtagIfNecessary(etag));
            }
        }
        return this.notModified;
    }

    private boolean validateIfUnmodifiedSince(long lastModifiedTimestamp) {
        if (lastModifiedTimestamp < 0) {
            return false;
        }
        long ifUnmodifiedSince = parseDateHeader(HttpHeaders.IF_UNMODIFIED_SINCE);
        if (ifUnmodifiedSince == -1) {
            return false;
        }
        // We will perform this validation...
        this.notModified = (ifUnmodifiedSince < (lastModifiedTimestamp / 1000 * 1000));
        return true;
    }

    private boolean validateIfNoneMatch(String etag) {
        if (!StringUtils.hasLength(etag)) {
            return false;
        }
        Enumeration<String> ifNoneMatch;
        try {
            ifNoneMatch = req.getHeaders(HttpHeaders.IF_NONE_MATCH);
        } catch (IllegalArgumentException ex) {
            return false;
        }
        if (!ifNoneMatch.hasMoreElements()) {
            return false;
        }
        // We will perform this validation...
        etag = padEtagIfNecessary(etag);
        if (etag.startsWith("W/")) {
            etag = etag.substring(2);
        }
        while (ifNoneMatch.hasMoreElements()) {
            String clientETags = ifNoneMatch.nextElement();
            Matcher etagMatcher = ETAG_HEADER_VALUE_PATTERN.matcher(clientETags);
            // Compare weak/strong ETags as per https://tools.ietf.org/html/rfc7232#section-2.3
            while (etagMatcher.find()) {
                if (StringUtils.hasLength(etagMatcher.group()) && etag.equals(etagMatcher.group(3))) {
                    this.notModified = true;
                    break;
                }
            }
        }
        return true;
    }

    private String padEtagIfNecessary(String etag) {
        if (!StringUtils.hasLength(etag)) {
            return etag;
        }
        if ((etag.startsWith("\"") || etag.startsWith("W/\"")) && etag.endsWith("\"")) {
            return etag;
        }
        return "\"" + etag + "\"";
    }

    private boolean validateIfModifiedSince(long lastModifiedTimestamp) {
        if (lastModifiedTimestamp < 0) {
            return false;
        }
        long ifModifiedSince = parseDateHeader(HttpHeaders.IF_MODIFIED_SINCE);
        if (ifModifiedSince == -1) {
            return false;
        }
        // We will perform this validation...
        this.notModified = ifModifiedSince >= (lastModifiedTimestamp / 1000 * 1000);
        return true;
    }

    private long parseDateHeader(String headerName) {
        long dateValue = -1;
        try {
            dateValue = req.getDateHeader(headerName);
        } catch (IllegalArgumentException ex) {
            String headerValue = req.getHeader(headerName);
            // Possibly an IE 10 style value: "Wed, 09 Apr 2014 09:57:42 GMT; length=13774"
            if (headerValue != null) {
                int separatorIndex = headerValue.indexOf(';');
                if (separatorIndex != -1) {
                    String datePart = headerValue.substring(0, separatorIndex);
                    dateValue = parseDateValue(datePart);
                }
            }
        }
        return dateValue;
    }

    private long parseDateValue(String headerValue) {
        if (headerValue == null) {
            // No header value sent at all
            return -1;
        }
        if (headerValue.length() >= 3) {
            // Short "0" or "-1" like values are never valid HTTP date headers...
            // Let's only bother with SimpleDateFormat parsing for long enough values.
            for (String dateFormat : DATE_FORMATS) {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat, Locale.US);
                simpleDateFormat.setTimeZone(GMT);
                try {
                    return simpleDateFormat.parse(headerValue).getTime();
                } catch (ParseException ex) {
                    // ignore
                }
            }
        }
        return -1;
    }
}
