package org.clever.boot.env;

import org.clever.boot.origin.Origin;
import org.clever.boot.origin.OriginTrackedValue;
import org.clever.boot.origin.TextResourceOrigin;
import org.clever.boot.origin.TextResourceOrigin.Location;
import org.clever.core.io.Resource;
import org.clever.util.Assert;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

/**
 * 类将{@code .properties}文件加载到{@code String} -> {@link OriginTrackedValue}的map中。
 * 还支持扩展{@code name[]=a,b,c}列表样式值。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/03 17:43 <br/>
 */
class OriginTrackedPropertiesLoader {
    private final Resource resource;

    /**
     * 创建新的 {@link OriginTrackedPropertiesLoader}
     *
     * @param resource {@code .properties}数据的资源
     */
    OriginTrackedPropertiesLoader(Resource resource) {
        Assert.notNull(resource, "Resource must not be null");
        this.resource = resource;
    }

    /**
     * 加载 {@code .properties} 并返回文档列表。
     *
     * @return 加载的属性
     * @throws IOException 读取时出错
     */
    List<Document> load() throws IOException {
        return load(true);
    }

    /**
     * 加载{@code .properties}数据并返回 {@code String}->OriginTrackedValue的map。
     *
     * @param expandLists 如果列表名[]=a、b、c，则应展开快捷方式
     * @return 加载的属性
     * @throws IOException 读取时出错
     */
    @SuppressWarnings("SameParameterValue")
    List<Document> load(boolean expandLists) throws IOException {
        List<Document> documents = new ArrayList<>();
        Document document = new Document();
        StringBuilder buffer = new StringBuilder();
        try (CharacterReader reader = new CharacterReader(this.resource)) {
            while (reader.read()) {
                if (reader.isPoundCharacter()) {
                    if (isNewDocument(reader)) {
                        if (!document.isEmpty()) {
                            documents.add(document);
                        }
                        document = new Document();
                    } else {
                        if (document.isEmpty() && !documents.isEmpty()) {
                            document = documents.remove(documents.size() - 1);
                        }
                        reader.setLastLineComment(true);
                        reader.skipComment();
                    }
                } else {
                    reader.setLastLineComment(false);
                    loadKeyAndValue(expandLists, document, reader, buffer);
                }
            }
        }
        if (!document.isEmpty() && !documents.contains(document)) {
            documents.add(document);
        }
        return documents;
    }

    private void loadKeyAndValue(boolean expandLists, Document document, CharacterReader reader, StringBuilder buffer) throws IOException {
        String key = loadKey(buffer, reader).trim();
        if (expandLists && key.endsWith("[]")) {
            key = key.substring(0, key.length() - 2);
            int index = 0;
            do {
                OriginTrackedValue value = loadValue(buffer, reader, true);
                document.put(key + "[" + (index++) + "]", value);
                if (!reader.isEndOfLine()) {
                    reader.read();
                }
            }
            while (!reader.isEndOfLine());
        } else {
            OriginTrackedValue value = loadValue(buffer, reader, false);
            document.put(key, value);
        }
    }

    private String loadKey(StringBuilder buffer, CharacterReader reader) throws IOException {
        buffer.setLength(0);
        boolean previousWhitespace = false;
        while (!reader.isEndOfLine()) {
            if (reader.isPropertyDelimiter()) {
                reader.read();
                return buffer.toString();
            }
            if (!reader.isWhiteSpace() && previousWhitespace) {
                return buffer.toString();
            }
            previousWhitespace = reader.isWhiteSpace();
            buffer.append(reader.getCharacter());
            reader.read();
        }
        return buffer.toString();
    }

    private OriginTrackedValue loadValue(StringBuilder buffer, CharacterReader reader, boolean splitLists) throws IOException {
        buffer.setLength(0);
        while (reader.isWhiteSpace() && !reader.isEndOfLine()) {
            reader.read();
        }
        Location location = reader.getLocation();
        while (!reader.isEndOfLine() && !(splitLists && reader.isListDelimiter())) {
            buffer.append(reader.getCharacter());
            reader.read();
        }
        Origin origin = new TextResourceOrigin(this.resource, location);
        return OriginTrackedValue.of(buffer.toString(), origin);
    }

    private boolean isNewDocument(CharacterReader reader) throws IOException {
        if (reader.isLastLineComment()) {
            return false;
        }
        boolean result = reader.getLocation().getColumn() == 0 && reader.isPoundCharacter();
        result = result && readAndExpect(reader, reader::isHyphenCharacter);
        result = result && readAndExpect(reader, reader::isHyphenCharacter);
        result = result && readAndExpect(reader, reader::isHyphenCharacter);
        if (!reader.isEndOfLine()) {
            reader.read();
            reader.skipWhitespace();
        }
        return result && reader.isEndOfLine();
    }

    private boolean readAndExpect(CharacterReader reader, BooleanSupplier check) throws IOException {
        reader.read();
        return check.getAsBoolean();
    }

    /**
     * 从源资源中读取字符，注意跳过注释、处理多行值和跟踪“\”转义。
     */
    private static class CharacterReader implements Closeable {
        private static final String[] ESCAPES = {"trnf", "\t\r\n\f"};

        private final LineNumberReader reader;
        private int columnNumber = -1;
        private boolean escaped;
        private int character;

        private boolean lastLineComment;

        CharacterReader(Resource resource) throws IOException {
            this.reader = new LineNumberReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.ISO_8859_1)
            );
        }

        @Override
        public void close() throws IOException {
            this.reader.close();
        }

        boolean read() throws IOException {
            return read(false);
        }

        boolean read(boolean wrappedLine) throws IOException {
            this.escaped = false;
            this.character = this.reader.read();
            this.columnNumber++;
            if (this.columnNumber == 0) {
                skipWhitespace();
                if (!wrappedLine) {
                    if (this.character == '!') {
                        skipComment();
                    }
                }
            }
            if (this.character == '\\') {
                this.escaped = true;
                readEscaped();
            } else if (this.character == '\n') {
                this.columnNumber = -1;
            }
            return !isEndOfFile();
        }

        private void skipWhitespace() throws IOException {
            while (isWhiteSpace()) {
                this.character = this.reader.read();
                this.columnNumber++;
            }
        }

        private void setLastLineComment(boolean lastLineComment) {
            this.lastLineComment = lastLineComment;
        }

        private boolean isLastLineComment() {
            return this.lastLineComment;
        }

        private void skipComment() throws IOException {
            while (this.character != '\n' && this.character != -1) {
                this.character = this.reader.read();
            }
            this.columnNumber = -1;
        }

        private void readEscaped() throws IOException {
            this.character = this.reader.read();
            int escapeIndex = ESCAPES[0].indexOf(this.character);
            if (escapeIndex != -1) {
                this.character = ESCAPES[1].charAt(escapeIndex);
            } else if (this.character == '\n') {
                this.columnNumber = -1;
                read(true);
            } else if (this.character == 'u') {
                readUnicode();
            }
        }

        private void readUnicode() throws IOException {
            this.character = 0;
            for (int i = 0; i < 4; i++) {
                int digit = this.reader.read();
                if (digit >= '0' && digit <= '9') {
                    this.character = (this.character << 4) + digit - '0';
                } else if (digit >= 'a' && digit <= 'f') {
                    this.character = (this.character << 4) + digit - 'a' + 10;
                } else if (digit >= 'A' && digit <= 'F') {
                    this.character = (this.character << 4) + digit - 'A' + 10;
                } else {
                    throw new IllegalStateException("Malformed \\uxxxx encoding.");
                }
            }
        }

        boolean isWhiteSpace() {
            return !this.escaped && (this.character == ' ' || this.character == '\t' || this.character == '\f');
        }

        boolean isEndOfFile() {
            return this.character == -1;
        }

        boolean isEndOfLine() {
            return this.character == -1 || (!this.escaped && this.character == '\n');
        }

        boolean isListDelimiter() {
            return !this.escaped && this.character == ',';
        }

        boolean isPropertyDelimiter() {
            return !this.escaped && (this.character == '=' || this.character == ':');
        }

        char getCharacter() {
            return (char) this.character;
        }

        Location getLocation() {
            return new Location(this.reader.getLineNumber(), this.columnNumber);
        }

        boolean isPoundCharacter() {
            return this.character == '#';
        }

        boolean isHyphenCharacter() {
            return this.character == '-';
        }
    }

    /**
     * 属性文件中的单个文档。
     */
    static class Document {
        private final Map<String, OriginTrackedValue> values = new LinkedHashMap<>();

        void put(String key, OriginTrackedValue value) {
            if (!key.isEmpty()) {
                this.values.put(key, value);
            }
        }

        boolean isEmpty() {
            return this.values.isEmpty();
        }

        Map<String, OriginTrackedValue> asMap() {
            return this.values;
        }
    }
}
