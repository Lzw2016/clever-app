package org.clever.boot.context.properties.source;

import org.clever.util.Assert;
import org.clever.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * 由点分隔的元素组成的配置属性名称。
 * 用户创建的名称可能包含字符“a-z”“0-9”）和“-”，它们必须是小写，并且必须以字母数字字符开头。
 * “-”仅用于格式化，即“foo-bar”和“fooBar”被认为是等效的。
 * <p>
 * “[”和“]”字符可用于指示关联索引（即Map key或Collection index）。
 * 索引名称不受限制，并视为区分大小写。
 * <p>
 * 以下是一些典型示例：
 * <ul>
 * <li>{@code clever.main.banner-mode}</li>
 * <li>{@code server.hosts[0].name}</li>
 * <li>{@code log[org.clever].level}</li>
 * </ul>
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 21:36 <br/>
 *
 * @see #of(CharSequence)
 * @see ConfigurationPropertySource
 */
public final class ConfigurationPropertyName implements Comparable<ConfigurationPropertyName> {
    private static final String EMPTY_STRING = "";
    /**
     * 一个空的 {@link ConfigurationPropertyName}.
     */
    public static final ConfigurationPropertyName EMPTY = new ConfigurationPropertyName(Elements.EMPTY);

    private final Elements elements;
    private final CharSequence[] uniformElements;
    private String string;
    private int hashCode;

    private ConfigurationPropertyName(Elements elements) {
        this.elements = elements;
        this.uniformElements = new CharSequence[elements.getSize()];
    }

    /**
     * 如果此{@link ConfigurationPropertyName}为空，则返回true。
     *
     * @return 如果名称为空，则为true
     */
    public boolean isEmpty() {
        return this.elements.getSize() == 0;
    }

    /**
     * 如果名称中的最后一个元素已编制索引，则返回。
     *
     * @return 如果最后一个元素被索引，则为true
     */
    public boolean isLastElementIndexed() {
        int size = getNumberOfElements();
        return (size > 0 && isIndexed(size - 1));
    }

    /**
     * 如果名称中的任何元素被索引，则返回true。
     *
     * @return 如果元素有一个或多个索引元素
     */
    public boolean hasIndexedElement() {
        for (int i = 0; i < getNumberOfElements(); i++) {
            if (isIndexed(i)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 如果名称中的元素已编制索引，则返回。
     *
     * @param elementIndex 元素的索引
     * @return 如果元素已索引，则为true
     */
    boolean isIndexed(int elementIndex) {
        return this.elements.getType(elementIndex).isIndexed();
    }

    /**
     * 如果名称中的元素为索引和数字，则返回。
     *
     * @param elementIndex 元素的索引
     * @return 如果元素为索引和数字，则为true
     */
    public boolean isNumericIndex(int elementIndex) {
        return this.elements.getType(elementIndex) == ElementType.NUMERICALLY_INDEXED;
    }

    /**
     * 以给定形式返回名称中的最后一个元素。
     *
     * @param form 要返回的Form
     * @return 最后一个元素
     */
    public String getLastElement(Form form) {
        int size = getNumberOfElements();
        return (size != 0) ? getElement(size - 1, form) : EMPTY_STRING;
    }

    /**
     * 以给定形式返回名称中的元素。
     *
     * @param elementIndex 元素索引
     * @param form         要返回的Form
     * @return 最后一个元素
     */
    public String getElement(int elementIndex, Form form) {
        CharSequence element = this.elements.get(elementIndex);
        ElementType type = this.elements.getType(elementIndex);
        if (type.isIndexed()) {
            return element.toString();
        }
        if (form == Form.ORIGINAL) {
            if (type != ElementType.NON_UNIFORM) {
                return element.toString();
            }
            return convertToOriginalForm(element).toString();
        }
        if (form == Form.DASHED) {
            if (type == ElementType.UNIFORM || type == ElementType.DASHED) {
                return element.toString();
            }
            return convertToDashedElement(element).toString();
        }
        CharSequence uniformElement = this.uniformElements[elementIndex];
        if (uniformElement == null) {
            uniformElement = (type != ElementType.UNIFORM) ? convertToUniformElement(element) : element;
            this.uniformElements[elementIndex] = uniformElement.toString();
        }
        return uniformElement.toString();
    }

    private CharSequence convertToOriginalForm(CharSequence element) {
        return convertElement(
                element,
                false,
                (ch, i) -> ch == '_' || ElementsParser.isValidChar(Character.toLowerCase(ch), i)
        );
    }

    private CharSequence convertToDashedElement(CharSequence element) {
        return convertElement(element, true, ElementsParser::isValidChar);
    }

    private CharSequence convertToUniformElement(CharSequence element) {
        return convertElement(element, true, (ch, i) -> ElementsParser.isAlphaNumeric(ch));
    }

    private CharSequence convertElement(CharSequence element, boolean lowercase, ElementCharPredicate filter) {
        StringBuilder result = new StringBuilder(element.length());
        for (int i = 0; i < element.length(); i++) {
            char ch = lowercase ? Character.toLowerCase(element.charAt(i)) : element.charAt(i);
            if (filter.test(ch, i)) {
                result.append(ch);
            }
        }
        return result;
    }

    /**
     * 返回名称中元素的总数。
     *
     * @return 元素的数量
     */
    public int getNumberOfElements() {
        return this.elements.getSize();
    }

    /**
     * 通过附加给定后缀创建新的 {@link ConfigurationPropertyName}
     *
     * @param suffix 要附加的元素
     * @return 一个新的 {@link ConfigurationPropertyName}
     * @throws InvalidConfigurationPropertyNameException 如果结果无效
     */
    public ConfigurationPropertyName append(String suffix) {
        if (!StringUtils.hasLength(suffix)) {
            return this;
        }
        Elements additionalElements = probablySingleElementOf(suffix);
        return new ConfigurationPropertyName(this.elements.append(additionalElements));
    }

    /**
     * 通过附加给定后缀创建新的 {@link ConfigurationPropertyName}
     *
     * @param suffix 要附加的元素
     * @return a new {@link ConfigurationPropertyName}
     */
    public ConfigurationPropertyName append(ConfigurationPropertyName suffix) {
        if (suffix == null) {
            return this;
        }
        return new ConfigurationPropertyName(this.elements.append(suffix.elements));
    }

    /**
     * 如果没有父项，则返回此{@link ConfigurationPropertyName}或{@link ConfigurationPropertyName#EMPTY}的父项。
     *
     * @return 父名称
     */
    public ConfigurationPropertyName getParent() {
        int numberOfElements = getNumberOfElements();
        return (numberOfElements <= 1) ? EMPTY : chop(numberOfElements - 1);
    }

    /**
     * 通过将此名称剪切到给定大小来返回一个新的{@link ConfigurationPropertyName}。
     * 例如，名称{@code foo.bar}上的{@code chop(1)}将返回{@code foo}。
     *
     * @param size 要剪切的大小
     * @return 被切掉的名字
     */
    public ConfigurationPropertyName chop(int size) {
        if (size >= getNumberOfElements()) {
            return this;
        }
        return new ConfigurationPropertyName(this.elements.chop(size));
    }

    /**
     * 返回一个新的{@link ConfigurationPropertyName}，基于此名称按特定元素索引偏移。
     * 例如 {@code chop(1)} 在名字上 {@code foo.bar} 将返回 {@code bar}.
     *
     * @param offset 元素偏移
     * @return 子名称
     */
    public ConfigurationPropertyName subName(int offset) {
        if (offset == 0) {
            return this;
        }
        if (offset == getNumberOfElements()) {
            return EMPTY;
        }
        if (offset < 0 || offset > getNumberOfElements()) {
            throw new IndexOutOfBoundsException("Offset: " + offset + ", NumberOfElements: " + getNumberOfElements());
        }
        return new ConfigurationPropertyName(this.elements.subElements(offset));
    }

    /**
     * 如果此元素是指定名称的直接父级，则返回true。
     *
     * @param name 要检查的名称
     * @return 如果此名称是祖先，则为true
     */
    public boolean isParentOf(ConfigurationPropertyName name) {
        Assert.notNull(name, "Name must not be null");
        if (getNumberOfElements() != name.getNumberOfElements() - 1) {
            return false;
        }
        return isAncestorOf(name);
    }

    /**
     * 如果此元素是指定名称的祖先（直接父级或嵌套父级），则返回true。
     *
     * @param name 要检查的名称
     * @return 如果此名称是祖先，则为true
     */
    public boolean isAncestorOf(ConfigurationPropertyName name) {
        Assert.notNull(name, "Name must not be null");
        if (getNumberOfElements() >= name.getNumberOfElements()) {
            return false;
        }
        return elementsEqual(name);
    }

    @Override
    public int compareTo(ConfigurationPropertyName other) {
        return compare(this, other);
    }

    private int compare(ConfigurationPropertyName n1, ConfigurationPropertyName n2) {
        int l1 = n1.getNumberOfElements();
        int l2 = n2.getNumberOfElements();
        int i1 = 0;
        int i2 = 0;
        while (i1 < l1 || i2 < l2) {
            try {
                ElementType type1 = (i1 < l1) ? n1.elements.getType(i1) : null;
                ElementType type2 = (i2 < l2) ? n2.elements.getType(i2) : null;
                String e1 = (i1 < l1) ? n1.getElement(i1++, Form.UNIFORM) : null;
                String e2 = (i2 < l2) ? n2.getElement(i2++, Form.UNIFORM) : null;
                int result = compare(e1, type1, e2, type2);
                if (result != 0) {
                    return result;
                }
            } catch (ArrayIndexOutOfBoundsException ex) {
                throw new RuntimeException(ex);
            }
        }
        return 0;
    }

    private int compare(String e1, ElementType type1, String e2, ElementType type2) {
        if (e1 == null) {
            return -1;
        }
        if (e2 == null) {
            return 1;
        }
        int result = Boolean.compare(type2.isIndexed(), type1.isIndexed());
        if (result != 0) {
            return result;
        }
        if (type1 == ElementType.NUMERICALLY_INDEXED && type2 == ElementType.NUMERICALLY_INDEXED) {
            long v1 = Long.parseLong(e1);
            long v2 = Long.parseLong(e2);
            return Long.compare(v1, v2);
        }
        return e1.compareTo(e2);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != getClass()) {
            return false;
        }
        ConfigurationPropertyName other = (ConfigurationPropertyName) obj;
        if (getNumberOfElements() != other.getNumberOfElements()) {
            return false;
        }
        if (this.elements.canShortcutWithSource(ElementType.UNIFORM)
                && other.elements.canShortcutWithSource(ElementType.UNIFORM)) {
            return toString().equals(other.toString());
        }
        return elementsEqual(other);
    }

    private boolean elementsEqual(ConfigurationPropertyName name) {
        for (int i = this.elements.getSize() - 1; i >= 0; i--) {
            if (elementDiffers(this.elements, name.elements, i)) {
                return false;
            }
        }
        return true;
    }

    private boolean elementDiffers(Elements e1, Elements e2, int i) {
        ElementType type1 = e1.getType(i);
        ElementType type2 = e2.getType(i);
        if (type1.allowsFastEqualityCheck() && type2.allowsFastEqualityCheck()) {
            return !fastElementEquals(e1, e2, i);
        }
        if (type1.allowsDashIgnoringEqualityCheck() && type2.allowsDashIgnoringEqualityCheck()) {
            return !dashIgnoringElementEquals(e1, e2, i);
        }
        return !defaultElementEquals(e1, e2, i);
    }

    private boolean fastElementEquals(Elements e1, Elements e2, int i) {
        int length1 = e1.getLength(i);
        int length2 = e2.getLength(i);
        if (length1 == length2) {
            int i1 = 0;
            while (length1-- != 0) {
                char ch1 = e1.charAt(i, i1);
                char ch2 = e2.charAt(i, i1);
                if (ch1 != ch2) {
                    return false;
                }
                i1++;
            }
            return true;
        }
        return false;
    }

    private boolean dashIgnoringElementEquals(Elements e1, Elements e2, int i) {
        int l1 = e1.getLength(i);
        int l2 = e2.getLength(i);
        int i1 = 0;
        int i2 = 0;
        while (i1 < l1) {
            if (i2 >= l2) {
                return false;
            }
            char ch1 = e1.charAt(i, i1);
            char ch2 = e2.charAt(i, i2);
            if (ch1 == '-') {
                i1++;
            } else if (ch2 == '-') {
                i2++;
            } else if (ch1 != ch2) {
                return false;
            } else {
                i1++;
                i2++;
            }
        }
        if (i2 < l2) {
            if (e2.getType(i).isIndexed()) {
                return false;
            }
            do {
                char ch2 = e2.charAt(i, i2++);
                if (ch2 != '-') {
                    return false;
                }
            }
            while (i2 < l2);
        }
        return true;
    }

    private boolean defaultElementEquals(Elements e1, Elements e2, int i) {
        int l1 = e1.getLength(i);
        int l2 = e2.getLength(i);
        boolean indexed1 = e1.getType(i).isIndexed();
        boolean indexed2 = e2.getType(i).isIndexed();
        int i1 = 0;
        int i2 = 0;
        while (i1 < l1) {
            if (i2 >= l2) {
                return remainderIsNotAlphanumeric(e1, i, i1);
            }
            char ch1 = indexed1 ? e1.charAt(i, i1) : Character.toLowerCase(e1.charAt(i, i1));
            char ch2 = indexed2 ? e2.charAt(i, i2) : Character.toLowerCase(e2.charAt(i, i2));
            if (!indexed1 && !ElementsParser.isAlphaNumeric(ch1)) {
                i1++;
            } else if (!indexed2 && !ElementsParser.isAlphaNumeric(ch2)) {
                i2++;
            } else if (ch1 != ch2) {
                return false;
            } else {
                i1++;
                i2++;
            }
        }
        if (i2 < l2) {
            return remainderIsNotAlphanumeric(e2, i, i2);
        }
        return true;
    }

    private boolean remainderIsNotAlphanumeric(Elements elements, int element, int index) {
        if (elements.getType(element).isIndexed()) {
            return false;
        }
        int length = elements.getLength(element);
        do {
            char c = Character.toLowerCase(elements.charAt(element, index++));
            if (ElementsParser.isAlphaNumeric(c)) {
                return false;
            }
        }
        while (index < length);
        return true;
    }

    @Override
    public int hashCode() {
        int hashCode = this.hashCode;
        Elements elements = this.elements;
        if (hashCode == 0 && elements.getSize() != 0) {
            for (int elementIndex = 0; elementIndex < elements.getSize(); elementIndex++) {
                int elementHashCode = 0;
                boolean indexed = elements.getType(elementIndex).isIndexed();
                int length = elements.getLength(elementIndex);
                for (int i = 0; i < length; i++) {
                    char ch = elements.charAt(elementIndex, i);
                    if (!indexed) {
                        ch = Character.toLowerCase(ch);
                    }
                    if (ElementsParser.isAlphaNumeric(ch)) {
                        elementHashCode = 31 * elementHashCode + ch;
                    }
                }
                hashCode = 31 * hashCode + elementHashCode;
            }
            this.hashCode = hashCode;
        }
        return hashCode;
    }

    @Override
    public String toString() {
        if (this.string == null) {
            this.string = buildToString();
        }
        return this.string;
    }

    private String buildToString() {
        if (this.elements.canShortcutWithSource(ElementType.UNIFORM, ElementType.DASHED)) {
            return this.elements.getSource().toString();
        }
        int elements = getNumberOfElements();
        StringBuilder result = new StringBuilder(elements * 8);
        for (int i = 0; i < elements; i++) {
            boolean indexed = isIndexed(i);
            if (result.length() > 0 && !indexed) {
                result.append('.');
            }
            if (indexed) {
                result.append('[');
                result.append(getElement(i, Form.ORIGINAL));
                result.append(']');
            } else {
                result.append(getElement(i, Form.DASHED));
            }
        }
        return result.toString();
    }

    /**
     * 如果给定名称有效，则返回。如果此方法返回true，则该名称可以与{@link #of(CharSequence)}一起使用，而不会引发异常。
     *
     * @param name 要测试的名称
     * @return 如果名称有效，则为true
     */
    public static boolean isValid(CharSequence name) {
        return of(name, true) != null;
    }

    /**
     * 返回指定字符串的{@link ConfigurationPropertyName}。
     *
     * @param name 源名称
     * @return {@link ConfigurationPropertyName} 对象
     * @throws InvalidConfigurationPropertyNameException 如果名称无效
     */
    public static ConfigurationPropertyName of(CharSequence name) {
        return of(name, false);
    }

    /**
     * 返回指定字符串的{@link ConfigurationPropertyName}，如果名称无效，则返回null。
     *
     * @param name 源名称
     * @return {@link ConfigurationPropertyName} 对象
     */
    public static ConfigurationPropertyName ofIfValid(CharSequence name) {
        return of(name, true);
    }

    /**
     * 返回指定字符串的 {@link ConfigurationPropertyName}。
     *
     * @param name                源名称
     * @param returnNullIfInvalid 如果名称无效，则应返回if null
     * @return {@link ConfigurationPropertyName} 实例
     * @throws InvalidConfigurationPropertyNameException 如果名称无效且{@code returnNullIfInvalid}为false
     */
    static ConfigurationPropertyName of(CharSequence name, boolean returnNullIfInvalid) {
        Elements elements = elementsOf(name, returnNullIfInvalid);
        return (elements != null) ? new ConfigurationPropertyName(elements) : null;
    }

    private static Elements probablySingleElementOf(CharSequence name) {
        return elementsOf(name, false, 1);
    }

    private static Elements elementsOf(CharSequence name, boolean returnNullIfInvalid) {
        return elementsOf(name, returnNullIfInvalid, ElementsParser.DEFAULT_CAPACITY);
    }

    private static Elements elementsOf(CharSequence name, boolean returnNullIfInvalid, int parserCapacity) {
        if (name == null) {
            Assert.isTrue(returnNullIfInvalid, "Name must not be null");
            return null;
        }
        if (name.length() == 0) {
            return Elements.EMPTY;
        }
        if (name.charAt(0) == '.' || name.charAt(name.length() - 1) == '.') {
            if (returnNullIfInvalid) {
                return null;
            }
            throw new InvalidConfigurationPropertyNameException(name, Collections.singletonList('.'));
        }
        Elements elements = new ElementsParser(name, '.', parserCapacity).parse();
        for (int i = 0; i < elements.getSize(); i++) {
            if (elements.getType(i) == ElementType.NON_UNIFORM) {
                if (returnNullIfInvalid) {
                    return null;
                }
                throw new InvalidConfigurationPropertyNameException(name, getInvalidChars(elements, i));
            }
        }
        return elements;
    }

    private static List<Character> getInvalidChars(Elements elements, int index) {
        List<Character> invalidChars = new ArrayList<>();
        for (int charIndex = 0; charIndex < elements.getLength(index); charIndex++) {
            char ch = elements.charAt(index, charIndex);
            if (!ElementsParser.isValidChar(ch, charIndex)) {
                invalidChars.add(ch);
            }
        }
        return invalidChars;
    }

    /**
     * 通过调整给定源来创建{@link ConfigurationPropertyName}。详见{@link #adapt(CharSequence, char, Function)}。
     *
     * @param name      要分析的名称
     * @param separator 用于拆分名称的分隔符
     * @return {@link ConfigurationPropertyName}
     */
    public static ConfigurationPropertyName adapt(CharSequence name, char separator) {
        return adapt(name, separator, null);
    }

    /**
     * 通过调整给定源来创建{@link ConfigurationPropertyName}。
     * 名称被拆分为围绕给定分隔符的元素。
     * 此方法比{@link #of}更宽松，因为它允许大小写名称和‘_’字符混合。
     * 在解析过程中，其他无效字符被去掉。
     * <p>
     * 如果需要对提取的元素值进行额外处理，则可以使用{@code elementValueProcessor}函数。
     *
     * @param name                  要分析的名称
     * @param separator             用于拆分名称的分隔符
     * @param elementValueProcessor 处理元素值的函数
     * @return a {@link ConfigurationPropertyName}
     */
    static ConfigurationPropertyName adapt(CharSequence name, char separator, Function<CharSequence, CharSequence> elementValueProcessor) {
        Assert.notNull(name, "Name must not be null");
        if (name.length() == 0) {
            return EMPTY;
        }
        Elements elements = new ElementsParser(name, separator).parse(elementValueProcessor);
        if (elements.getSize() == 0) {
            return EMPTY;
        }
        return new ConfigurationPropertyName(elements);
    }

    /**
     * 非索引元素值可以采取的各种形式。
     */
    public enum Form {
        /**
         * 创建或修改名称时指定的原始形式。例如：
         * <ul>
         * <li>"{@code foo-bar}" = "{@code foo-bar}"</li>
         * <li>"{@code fooBar}" = "{@code fooBar}"</li>
         * <li>"{@code foo_bar}" = "{@code foo_bar}"</li>
         * <li>"{@code [Foo.bar]}" = "{@code Foo.bar}"</li>
         * </ul>
         */
        ORIGINAL,
        /**
         * 虚线配置形式（用于toString；小写，仅包含字母数字字符和虚线）。
         * <ul>
         * <li>"{@code foo-bar}" = "{@code foo-bar}"</li>
         * <li>"{@code fooBar}" = "{@code foobar}"</li>
         * <li>"{@code foo_bar}" = "{@code foobar}"</li>
         * <li>"{@code [Foo.bar]}" = "{@code Foo.bar}"</li>
         * </ul>
         */
        DASHED,
        /**
         * 统一配置形式（用于equals/hashCode；小写，仅包含字母数字字符）。
         * <ul>
         * <li>"{@code foo-bar}" = "{@code foobar}"</li>
         * <li>"{@code fooBar}" = "{@code foobar}"</li>
         * <li>"{@code foo_bar}" = "{@code foobar}"</li>
         * <li>"{@code [Foo.bar]}" = "{@code Foo.bar}"</li>
         * </ul>
         */
        UNIFORM
    }

    /**
     * 允许访问组成名称的各个元素。为了节省内存，我们将索引存储在数组中，而不是对象列表中。
     */
    private static class Elements {
        private static final int[] NO_POSITION = {};
        private static final ElementType[] NO_TYPE = {};
        public static final Elements EMPTY = new Elements("", 0, NO_POSITION, NO_POSITION, NO_TYPE, null);

        private final CharSequence source;
        private final int size;
        private final int[] start;
        private final int[] end;
        private final ElementType[] type;

        /**
         * 包含任何已解析的元素，如果没有，则可以为null。
         * 解析的元素允许我们以某种方式修改元素值（例如，当使用映射函数进行调整时，或者当调用append时）。
         * 请注意，此数组不用作缓存，事实上，当它不为null时，{@link #canShortcutWithSource}将始终返回false，这可能会影响性能。
         */
        private final CharSequence[] resolved;

        Elements(CharSequence source, int size, int[] start, int[] end, ElementType[] type, CharSequence[] resolved) {
            super();
            this.source = source;
            this.size = size;
            this.start = start;
            this.end = end;
            this.type = type;
            this.resolved = resolved;
        }

        Elements append(Elements additional) {
            int size = this.size + additional.size;
            ElementType[] type = new ElementType[size];
            System.arraycopy(this.type, 0, type, 0, this.size);
            System.arraycopy(additional.type, 0, type, this.size, additional.size);
            CharSequence[] resolved = newResolved(size);
            for (int i = 0; i < additional.size; i++) {
                resolved[this.size + i] = additional.get(i);
            }
            return new Elements(this.source, size, this.start, this.end, type, resolved);
        }

        Elements chop(int size) {
            CharSequence[] resolved = newResolved(size);
            return new Elements(this.source, size, this.start, this.end, this.type, resolved);
        }

        Elements subElements(int offset) {
            int size = this.size - offset;
            CharSequence[] resolved = newResolved(size);
            int[] start = new int[size];
            System.arraycopy(this.start, offset, start, 0, size);
            int[] end = new int[size];
            System.arraycopy(this.end, offset, end, 0, size);
            ElementType[] type = new ElementType[size];
            System.arraycopy(this.type, offset, type, 0, size);
            return new Elements(this.source, size, start, end, type, resolved);
        }

        private CharSequence[] newResolved(int size) {
            CharSequence[] resolved = new CharSequence[size];
            if (this.resolved != null) {
                System.arraycopy(this.resolved, 0, resolved, 0, Math.min(size, this.size));
            }
            return resolved;
        }

        int getSize() {
            return this.size;
        }

        CharSequence get(int index) {
            if (this.resolved != null && this.resolved[index] != null) {
                return this.resolved[index];
            }
            int start = this.start[index];
            int end = this.end[index];
            return this.source.subSequence(start, end);
        }

        int getLength(int index) {
            if (this.resolved != null && this.resolved[index] != null) {
                return this.resolved[index].length();
            }
            int start = this.start[index];
            int end = this.end[index];
            return end - start;
        }

        char charAt(int index, int charIndex) {
            if (this.resolved != null && this.resolved[index] != null) {
                return this.resolved[index].charAt(charIndex);
            }
            int start = this.start[index];
            return this.source.charAt(start + charIndex);
        }

        ElementType getType(int index) {
            return this.type[index];
        }

        CharSequence getSource() {
            return this.source;
        }

        /**
         * 返回元素源是否可以用作操作的快捷方式，例如 {@code equals} 或 {@code toString}.
         *
         * @param requiredType 所需类型
         * @return 如果所有元素至少与其中一种类型匹配，则为true
         */
        boolean canShortcutWithSource(ElementType requiredType) {
            return canShortcutWithSource(requiredType, requiredType);
        }

        /**
         * 返回元素源是否可以用作操作的快捷方式，例如 {@code equals} 或 {@code toString}.
         *
         * @param requiredType    所需类型
         * @param alternativeType 和替代所需类型
         * @return 如果所有元素至少与其中一种类型匹配，则为true
         */
        boolean canShortcutWithSource(ElementType requiredType, ElementType alternativeType) {
            if (this.resolved != null) {
                return false;
            }
            for (int i = 0; i < this.size; i++) {
                ElementType type = this.type[i];
                if (type != requiredType && type != alternativeType) {
                    return false;
                }
                if (i > 0 && this.end[i - 1] + 1 != this.start[i]) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * 用于转换 {@link CharSequence} 到 {@link Elements}.
     */
    private static class ElementsParser {
        private static final int DEFAULT_CAPACITY = 6;

        private final CharSequence source;
        private final char separator;
        private int size;
        private int[] start;
        private int[] end;
        private ElementType[] type;
        private CharSequence[] resolved;

        ElementsParser(CharSequence source, char separator) {
            this(source, separator, DEFAULT_CAPACITY);
        }

        ElementsParser(CharSequence source, char separator, int capacity) {
            this.source = source;
            this.separator = separator;
            this.start = new int[capacity];
            this.end = new int[capacity];
            this.type = new ElementType[capacity];
        }

        Elements parse() {
            return parse(null);
        }

        Elements parse(Function<CharSequence, CharSequence> valueProcessor) {
            int length = this.source.length();
            int openBracketCount = 0;
            int start = 0;
            ElementType type = ElementType.EMPTY;
            for (int i = 0; i < length; i++) {
                char ch = this.source.charAt(i);
                if (ch == '[') {
                    if (openBracketCount == 0) {
                        add(start, i, type, valueProcessor);
                        start = i + 1;
                        type = ElementType.NUMERICALLY_INDEXED;
                    }
                    openBracketCount++;
                } else if (ch == ']') {
                    openBracketCount--;
                    if (openBracketCount == 0) {
                        add(start, i, type, valueProcessor);
                        start = i + 1;
                        type = ElementType.EMPTY;
                    }
                } else if (!type.isIndexed() && ch == this.separator) {
                    add(start, i, type, valueProcessor);
                    start = i + 1;
                    type = ElementType.EMPTY;
                } else {
                    type = updateType(type, ch, i - start);
                }
            }
            if (openBracketCount != 0) {
                type = ElementType.NON_UNIFORM;
            }
            add(start, length, type, valueProcessor);
            return new Elements(this.source, this.size, this.start, this.end, this.type, this.resolved);
        }

        private ElementType updateType(ElementType existingType, char ch, int index) {
            if (existingType.isIndexed()) {
                if (existingType == ElementType.NUMERICALLY_INDEXED && !isNumeric(ch)) {
                    return ElementType.INDEXED;
                }
                return existingType;
            }
            if (existingType == ElementType.EMPTY && isValidChar(ch, index)) {
                return (index == 0) ? ElementType.UNIFORM : ElementType.NON_UNIFORM;
            }
            if (existingType == ElementType.UNIFORM && ch == '-') {
                return ElementType.DASHED;
            }
            if (!isValidChar(ch, index)) {
                if (existingType == ElementType.EMPTY && !isValidChar(Character.toLowerCase(ch), index)) {
                    return ElementType.EMPTY;
                }
                return ElementType.NON_UNIFORM;
            }
            return existingType;
        }

        private void add(int start, int end, ElementType type, Function<CharSequence, CharSequence> valueProcessor) {
            if ((end - start) < 1 || type == ElementType.EMPTY) {
                return;
            }
            if (this.start.length == this.size) {
                this.start = expand(this.start);
                this.end = expand(this.end);
                this.type = expand(this.type);
                this.resolved = expand(this.resolved);
            }
            if (valueProcessor != null) {
                if (this.resolved == null) {
                    this.resolved = new CharSequence[this.start.length];
                }
                CharSequence resolved = valueProcessor.apply(this.source.subSequence(start, end));
                Elements resolvedElements = new ElementsParser(resolved, '.').parse();
                Assert.state(resolvedElements.getSize() == 1, "Resolved element must not contain multiple elements");
                this.resolved[this.size] = resolvedElements.get(0);
                type = resolvedElements.getType(0);
            }
            this.start[this.size] = start;
            this.end[this.size] = end;
            this.type[this.size] = type;
            this.size++;
        }

        private int[] expand(int[] src) {
            int[] dest = new int[src.length + DEFAULT_CAPACITY];
            System.arraycopy(src, 0, dest, 0, src.length);
            return dest;
        }

        private ElementType[] expand(ElementType[] src) {
            ElementType[] dest = new ElementType[src.length + DEFAULT_CAPACITY];
            System.arraycopy(src, 0, dest, 0, src.length);
            return dest;
        }

        private CharSequence[] expand(CharSequence[] src) {
            if (src == null) {
                return null;
            }
            CharSequence[] dest = new CharSequence[src.length + DEFAULT_CAPACITY];
            System.arraycopy(src, 0, dest, 0, src.length);
            return dest;
        }

        static boolean isValidChar(char ch, int index) {
            return isAlpha(ch) || isNumeric(ch) || (index != 0 && ch == '-');
        }

        static boolean isAlphaNumeric(char ch) {
            return isAlpha(ch) || isNumeric(ch);
        }

        private static boolean isAlpha(char ch) {
            return ch >= 'a' && ch <= 'z';
        }

        private static boolean isNumeric(char ch) {
            return ch >= '0' && ch <= '9';
        }
    }

    /**
     * 我们可以检测到的各种类型的元素。
     */
    private enum ElementType {
        /**
         * 元素在逻辑上为空（不包含有效字符）。
         */
        EMPTY(false),
        /**
         * 元素是一个统一的名称（a-z，0-9，无破折号，小写）。
         */
        UNIFORM(false),
        /**
         * 该元素几乎是均匀的，但它至少包含一个破折号（但不是以破折号开头）。
         */
        DASHED(false),
        /**
         * 元素包含非统一字符，需要进行转换。
         */
        NON_UNIFORM(false),
        /**
         * 该元素没有数字索引。
         */
        INDEXED(true),
        /**
         * 该元素以数字索引。
         */
        NUMERICALLY_INDEXED(true);

        private final boolean indexed;

        ElementType(boolean indexed) {
            this.indexed = indexed;
        }

        public boolean isIndexed() {
            return this.indexed;
        }

        public boolean allowsFastEqualityCheck() {
            return this == UNIFORM || this == NUMERICALLY_INDEXED;
        }

        public boolean allowsDashIgnoringEqualityCheck() {
            return allowsFastEqualityCheck() || this == DASHED;
        }
    }

    /**
     * 用于过滤元素字符的谓词。
     */
    private interface ElementCharPredicate {
        boolean test(char ch, int index);
    }
}
