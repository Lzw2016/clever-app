package org.clever.data.redis.connection.stream;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/01/27 21:32 <br/>
 */
public interface StringRecord extends MapRecord<String, String, String> {
    @Override
    StringRecord withId(RecordId id);

    /**
     * 使用关联的流 {@literal key} 创建一个新的 {@link StringRecord}
     *
     * @param key stream key
     * @return 一个新的 {@link StringRecord}
     */
    StringRecord withStreamKey(String key);

    /**
     * 将 {@link String strings} 的 {@link MapRecord} 转换为 {@link StringRecord}
     *
     * @param source 不得为 {@literal null}
     * @return {@link StringRecord} 的新实例
     */
    static StringRecord of(MapRecord<String, String, String> source) {
        return StreamRecords.newRecord().in(source.getStream()).withId(source.getId()).ofStrings(source.getValue());
    }
}
