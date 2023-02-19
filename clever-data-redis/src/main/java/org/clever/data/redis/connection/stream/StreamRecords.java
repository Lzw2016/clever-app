package org.clever.data.redis.connection.stream;

import org.clever.data.redis.util.ByteUtils;
import org.clever.util.Assert;
import org.clever.util.ClassUtils;
import org.clever.util.ObjectUtils;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * {@link StreamRecords} 提供实用程序来创建特定的 {@link Record} 实例
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/24 21:51 <br/>
 */
public class StreamRecords {
    /**
     * 为给定的原始字段值对创建一个新的 {@link ByteRecord}
     *
     * @param raw 不能是 {@literal null}
     * @return {@link ByteRecord} 的新实例
     */
    public static ByteRecord rawBytes(Map<byte[], byte[]> raw) {
        return new ByteMapBackedRecord(null, RecordId.autoGenerate(), raw);
    }

    /**
     * 为给定的原始字段值对创建一个新的 {@link ByteBuffer Record}
     *
     * @param raw 不能是 {@literal null}
     * @return {@link ByteBufferRecord} 的新实例
     */
    public static ByteBufferRecord rawBuffer(Map<ByteBuffer, ByteBuffer> raw) {
        return new ByteBufferMapBackedRecord(null, RecordId.autoGenerate(), raw);
    }

    /**
     * 为给定的原始字段值对创建一个新的 {@link ByteBuffer Record}
     *
     * @param raw 不能是 {@literal null}
     * @return {@link ByteBufferRecord} 的新实例
     */
    public static StringRecord string(Map<String, String> raw) {
        return new StringMapBackedRecord(null, RecordId.autoGenerate(), raw);
    }

    /**
     * 创建一个由给定 {@link Map} 的字段值对支持的新 {@link MapRecord}
     *
     * @param map 不能是 {@literal null}
     * @param <S> stream key的类型
     * @param <K> map key的类型。
     * @param <V> map value的类型
     * @return {@link MapRecord} 的新实例
     */
    public static <S, K, V> MapRecord<S, K, V> mapBacked(Map<K, V> map) {
        return new MapBackedRecord<>(null, RecordId.autoGenerate(), map);
    }

    /**
     * 创建由给定值支持的新 {@link ObjectRecord}
     *
     * @param value 不能是 {@literal null}
     * @param <S>   stream key的类型
     * @param <V>   value的类型
     * @return {@link ObjectRecord} 的新实例
     */
    public static <S, V> ObjectRecord<S, V> objectBacked(V value) {
        return new ObjectBackedRecord<>(null, RecordId.autoGenerate(), value);
    }

    /**
     * 获取 {@link RecordBuilder} 的新实例以流畅地创建 {@link Record 记录}
     *
     * @return {@link RecordBuilder} 的新实例
     */
    public static RecordBuilder<?> newRecord() {
        return new RecordBuilder<>(null, RecordId.autoGenerate());
    }

    // Utility constructor
    private StreamRecords() {
    }

    /**
     * {@link Record} 的生成器
     *
     * @param <S> stream key的类型
     */
    public static class RecordBuilder<S> {
        private RecordId id;
        private final S stream;

        RecordBuilder(S stream, RecordId recordId) {
            this.stream = stream;
            this.id = recordId;
        }

        /**
         * 配置一个 stream key
         *
         * @param stream stream key, 不得为 null
         * @return {@literal this} {@link RecordBuilder}
         */
        public <STREAM_KEY> RecordBuilder<STREAM_KEY> in(STREAM_KEY stream) {
            Assert.notNull(stream, "Stream key must not be null");
            return new RecordBuilder<>(stream, id);
        }

        /**
         * 配置给定 {@link String} 的记录 ID。关联用户提供的记录 ID，而不是使用服务器生成的记录 ID
         *
         * @param id record id
         * @return {@literal this} {@link RecordBuilder}
         * @see RecordId
         */
        public RecordBuilder<S> withId(String id) {
            return withId(RecordId.of(id));
        }

        /**
         * 配置 {@link RecordId}。关联用户提供的记录 ID，而不是使用服务器生成的记录 ID
         *
         * @param id record id
         * @return {@literal this} {@link RecordBuilder}
         */
        public RecordBuilder<S> withId(RecordId id) {
            Assert.notNull(id, "RecordId must not be null");
            this.id = id;
            return this;
        }

        /**
         * 创建一个 {@link MapRecord}
         *
         * @return {@link MapRecord} 的新实例
         */
        public <K, V> MapRecord<S, K, V> ofMap(Map<K, V> map) {
            return new MapBackedRecord<>(stream, id, map);
        }

        /**
         * 创建一个 {@link StringRecord}
         *
         * @return {@link StringRecord} 的新实例
         * @see MapRecord
         */
        public StringRecord ofStrings(Map<String, String> map) {
            return new StringMapBackedRecord(ObjectUtils.nullSafeToString(stream), id, map);
        }

        /**
         * 创建一个 {@link ObjectRecord}
         *
         * @return {@link ObjectRecord} 的新实例
         */
        public <V> ObjectRecord<S, V> ofObject(V value) {
            return new ObjectBackedRecord<>(stream, id, value);
        }

        /**
         * @return {@link ByteRecord} 的新实例
         */
        public ByteRecord ofBytes(Map<byte[], byte[]> value) {
            // auto conversion of known values (已知值的自动转换)
            return new ByteMapBackedRecord((byte[]) stream, id, value);
        }

        /**
         * @return {@link ByteBufferRecord} 的新实例
         */
        public ByteBufferRecord ofBuffer(Map<ByteBuffer, ByteBuffer> value) {
            ByteBuffer streamKey;
            if (stream instanceof ByteBuffer) {
                streamKey = (ByteBuffer) stream;
            } else if (stream instanceof String) {
                streamKey = ByteUtils.getByteBuffer((String) stream);
            } else if (stream instanceof byte[]) {
                streamKey = ByteBuffer.wrap((byte[]) stream);
            } else {
                throw new IllegalArgumentException(String.format("Stream key %s cannot be converted to byte buffer.", stream));
            }
            return new ByteBufferMapBackedRecord(streamKey, id, value);
        }
    }

    /**
     * {@link MapRecord} 的默认实现
     */
    static class MapBackedRecord<S, K, V> implements MapRecord<S, K, V> {
        private final S stream;
        private final RecordId recordId;
        private final Map<K, V> kvMap;

        MapBackedRecord(S stream, RecordId recordId, Map<K, V> kvMap) {
            this.stream = stream;
            this.recordId = recordId;
            this.kvMap = kvMap;
        }

        @Override
        public S getStream() {
            return stream;
        }

        @Override
        public RecordId getId() {
            return recordId;
        }

        @Override
        public Iterator<Entry<K, V>> iterator() {
            return kvMap.entrySet().iterator();
        }

        @Override
        public Map<K, V> getValue() {
            return kvMap;
        }

        @Override
        public MapRecord<S, K, V> withId(RecordId id) {
            return new MapBackedRecord<>(stream, id, this.kvMap);
        }

        @Override
        public <S1> MapRecord<S1, K, V> withStreamKey(S1 key) {
            return new MapBackedRecord<>(key, recordId, this.kvMap);
        }

        @Override
        public String toString() {
            return "MapBackedRecord{" + "recordId=" + recordId + ", kvMap=" + kvMap + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) {
                return false;
            }
            if (this == o) {
                return true;
            }
            if (!ClassUtils.isAssignable(MapBackedRecord.class, o.getClass())) {
                return false;
            }
            MapBackedRecord<?, ?, ?> that = (MapBackedRecord<?, ?, ?>) o;
            if (!ObjectUtils.nullSafeEquals(this.stream, that.stream)) {
                return false;
            }
            if (!ObjectUtils.nullSafeEquals(this.recordId, that.recordId)) {
                return false;
            }
            return ObjectUtils.nullSafeEquals(this.kvMap, that.kvMap);
        }

        @Override
        public int hashCode() {
            int result = stream != null ? stream.hashCode() : 0;
            result = 31 * result + recordId.hashCode();
            result = 31 * result + kvMap.hashCode();
            return result;
        }
    }

    /**
     * {@link ByteRecord} 的默认实现
     */
    static class ByteMapBackedRecord extends MapBackedRecord<byte[], byte[], byte[]> implements ByteRecord {
        ByteMapBackedRecord(byte[] stream, RecordId recordId, Map<byte[], byte[]> map) {
            super(stream, recordId, map);
        }

        @Override
        public ByteMapBackedRecord withStreamKey(byte[] key) {
            return new ByteMapBackedRecord(key, getId(), getValue());
        }

        @Override
        public ByteMapBackedRecord withId(RecordId id) {
            return new ByteMapBackedRecord(getStream(), id, getValue());
        }
    }

    /**
     * {@link ByteBufferRecord} 的默认实现
     */
    static class ByteBufferMapBackedRecord extends MapBackedRecord<ByteBuffer, ByteBuffer, ByteBuffer> implements ByteBufferRecord {
        ByteBufferMapBackedRecord(ByteBuffer stream, RecordId recordId, Map<ByteBuffer, ByteBuffer> map) {
            super(stream, recordId, map);
        }

        @Override
        public ByteBufferMapBackedRecord withStreamKey(ByteBuffer key) {
            return new ByteBufferMapBackedRecord(key, getId(), getValue());
        }

        @Override
        public ByteBufferMapBackedRecord withId(RecordId id) {
            return new ByteBufferMapBackedRecord(getStream(), id, getValue());
        }
    }

    /**
     * StringRecord 的默认实现
     */
    static class StringMapBackedRecord extends MapBackedRecord<String, String, String> implements StringRecord {
        StringMapBackedRecord(String stream, RecordId recordId, Map<String, String> stringStringMap) {
            super(stream, recordId, stringStringMap);
        }

        @Override
        public StringRecord withStreamKey(String key) {
            return new StringMapBackedRecord(key, getId(), getValue());
        }

        @Override
        public StringMapBackedRecord withId(RecordId id) {
            return new StringMapBackedRecord(getStream(), id, getValue());
        }
    }

    /**
     * {@link ObjectRecord} 的默认实现
     */
    static class ObjectBackedRecord<S, V> implements ObjectRecord<S, V> {
        private final S stream;
        private final RecordId recordId;
        private final V value;

        ObjectBackedRecord(S stream, RecordId recordId, V value) {
            this.stream = stream;
            this.recordId = recordId;
            this.value = value;
        }

        @Override
        public S getStream() {
            return stream;
        }

        @Override
        public RecordId getId() {
            return recordId;
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public ObjectRecord<S, V> withId(RecordId id) {
            return new ObjectBackedRecord<>(stream, id, value);
        }

        @Override
        public <SK> ObjectRecord<SK, V> withStreamKey(SK key) {
            return new ObjectBackedRecord<>(key, recordId, value);
        }

        @Override
        public String toString() {
            return "ObjectBackedRecord{" + "recordId=" + recordId + ", value=" + value + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            ObjectBackedRecord<?, ?> that = (ObjectBackedRecord<?, ?>) o;
            if (!ObjectUtils.nullSafeEquals(stream, that.stream)) {
                return false;
            }
            if (!ObjectUtils.nullSafeEquals(recordId, that.recordId)) {
                return false;
            }
            return ObjectUtils.nullSafeEquals(value, that.value);
        }

        @Override
        public int hashCode() {
            int result = ObjectUtils.nullSafeHashCode(stream);
            result = 31 * result + ObjectUtils.nullSafeHashCode(recordId);
            result = 31 * result + ObjectUtils.nullSafeHashCode(value);
            return result;
        }
    }
}
