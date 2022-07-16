package org.clever.core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 根据{@link Properties}的键按字母数字对属性进行排序。
 *
 * <p>这在将属性实例存储在{@link Properties}文件中时非常有用，因为它允许以可重复的方式生成此类文件，并且属性的顺序一致。
 *
 * <p>也可以选择省略生成的属性文件中的注释。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/03 17:53 <br/>
 *
 * @see java.util.Properties
 */
class SortedProperties extends Properties {
    static final String EOL = System.lineSeparator();
    private static final Comparator<Object> keyComparator = Comparator.comparing(String::valueOf);
    private static final Comparator<Map.Entry<Object, Object>> entryComparator = Map.Entry.comparingByKey(keyComparator);

    private final boolean omitComments;

    /**
     * 构造一个新的{@code SortedProperties}实例，该实例接受提供的{@code omitComments}标志。
     */
    SortedProperties(boolean omitComments) {
        this.omitComments = omitComments;
    }

    /**
     * 构造一个新的{@code SortedProperties}实例，
     * 其中包含从提供的{@link Properties}对象填充的属性，
     * 并遵循提供的{@code omitComments}标志。
     * <p>不会复制提供的{@code Properties}对象中的默认属性
     *
     * @param properties   要从中复制初始特性的Properties对象
     * @param omitComments 如果在文件中存储属性时应省略注释，则为true
     */
    SortedProperties(Properties properties, boolean omitComments) {
        this(omitComments);
        putAll(properties);
    }

    @Override
    public void store(OutputStream out, String comments) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        super.store(baos, (this.omitComments ? null : comments));
        String contents = baos.toString(StandardCharsets.ISO_8859_1.name());
        for (String line : contents.split(EOL)) {
            if (!(this.omitComments && line.startsWith("#"))) {
                out.write((line + EOL).getBytes(StandardCharsets.ISO_8859_1));
            }
        }
    }

    @Override
    public void store(Writer writer, String comments) throws IOException {
        StringWriter stringWriter = new StringWriter();
        super.store(stringWriter, (this.omitComments ? null : comments));
        String contents = stringWriter.toString();
        for (String line : contents.split(EOL)) {
            if (!(this.omitComments && line.startsWith("#"))) {
                writer.write(line + EOL);
            }
        }
    }

    @Override
    public void storeToXML(OutputStream out, String comments) throws IOException {
        super.storeToXML(out, (this.omitComments ? null : comments));
    }

    @Override
    public void storeToXML(OutputStream out, String comments, String encoding) throws IOException {
        super.storeToXML(out, (this.omitComments ? null : comments), encoding);
    }

    /**
     * 返回此{@link Properties}对象中键的排序枚举。
     *
     * @see #keySet()
     */
    @Override
    public synchronized Enumeration<Object> keys() {
        return Collections.enumeration(keySet());
    }

    /**
     * 返回此{@link Properties}对象中已排序的键集。
     * <p>如有必要，将使用{@link String#valueOf(Object)}将键转换为字符串，并根据字符串的自然顺序进行字母数字排序。
     */
    @Override
    public Set<Object> keySet() {
        Set<Object> sortedKeys = new TreeSet<>(keyComparator);
        sortedKeys.addAll(super.keySet());
        return Collections.synchronizedSet(sortedKeys);
    }

    /**
     * 返回此{@link Properties}对象中已排序的一组条目。
     * <p>条目将根据其键进行排序，必要时使用{@link String#valueOf(Object)}将键转换为字符串，并根据字符串的自然顺序进行字母数字比较。
     */
    @Override
    public Set<Map.Entry<Object, Object>> entrySet() {
        Set<Map.Entry<Object, Object>> sortedEntries = new TreeSet<>(entryComparator);
        sortedEntries.addAll(super.entrySet());
        return Collections.synchronizedSet(sortedEntries);
    }
}
