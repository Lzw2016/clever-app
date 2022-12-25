package org.clever.web.http;

import org.clever.util.MultiValueMap;

import java.util.*;
import java.util.stream.Collectors;

/**
 * {@code HttpHeaders} 对象只能读取，不能写入
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/12/25 14:57 <br/>
 */
class ReadOnlyHttpHeaders extends HttpHeaders {
    private static final long serialVersionUID = -8578554704772377436L;

    private MediaType cachedContentType;
    private List<MediaType> cachedAccept;

    ReadOnlyHttpHeaders(MultiValueMap<String, String> headers) {
        super(headers);
    }

    @Override
    public MediaType getContentType() {
        if (this.cachedContentType != null) {
            return this.cachedContentType;
        } else {
            MediaType contentType = super.getContentType();
            this.cachedContentType = contentType;
            return contentType;
        }
    }

    @Override
    public List<MediaType> getAccept() {
        if (this.cachedAccept != null) {
            return this.cachedAccept;
        } else {
            List<MediaType> accept = super.getAccept();
            this.cachedAccept = accept;
            return accept;
        }
    }

    @Override
    public void clearContentHeaders() {
        // No-op.
    }

    @Override
    public List<String> get(Object key) {
        List<String> values = this.headers.get(key);
        return (values != null ? Collections.unmodifiableList(values) : null);
    }

    @Override
    public void add(String headerName, String headerValue) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addAll(String key, List<? extends String> values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addAll(MultiValueMap<String, String> values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void set(String headerName, String headerValue) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setAll(Map<String, String> values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, String> toSingleValueMap() {
        return Collections.unmodifiableMap(this.headers.toSingleValueMap());
    }

    @Override
    public Set<String> keySet() {
        return Collections.unmodifiableSet(this.headers.keySet());
    }

    @Override
    public List<String> put(String key, List<String> value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> remove(Object key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(Map<? extends String, ? extends List<String>> map) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<List<String>> values() {
        return Collections.unmodifiableCollection(this.headers.values());
    }

    @Override
    public Set<Entry<String, List<String>>> entrySet() {
        return this.headers.entrySet().stream().map(AbstractMap.SimpleImmutableEntry::new)
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(LinkedHashSet::new), // Retain original ordering of entries
                        Collections::unmodifiableSet));
    }
}
