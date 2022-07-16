package org.clever.util;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ant风格路径模式的PathMatcher实现。
 * 此映射代码的一部分是从<a href="https://ant.apache.org">Apache Ant</a>借用的。
 * 映射使用以下规则匹配URL：
 * <ul>
 * <li>{@code ?} 匹配一个字符</li>
 * <li>{@code *} 匹配零个或多个字符</li>
 * <li>{@code **} 匹配零级或多级<em>目录</em></li>
 * <li>{@code {clever:[a-z]+}} 将regexp{@code [a-z]+}作为名为"clever"的路径变量进行匹配</li>
 * </ul>
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/04 22:21 <br/>
 */
public class AntPathMatcher implements PathMatcher {
    /**
     * 默认路径分隔符: "/"
     */
    public static final String DEFAULT_PATH_SEPARATOR = "/";
    private static final int CACHE_TURNOFF_THRESHOLD = 65536;
    @SuppressWarnings("RegExpRedundantEscape")
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{[^/]+?\\}");
    private static final char[] WILDCARD_CHARS = {'*', '?', '{'};

    private String pathSeparator;
    private PathSeparatorPatternCache pathSeparatorPatternCache;
    private boolean caseSensitive = true;
    private boolean trimTokens = false;
    private volatile Boolean cachePatterns;
    private final Map<String, String[]> tokenizedPatternCache = new ConcurrentHashMap<>(256);
    final Map<String, AntPathStringMatcher> stringMatcherCache = new ConcurrentHashMap<>(256);

    /**
     * 使用创建新实例{@link #DEFAULT_PATH_SEPARATOR}.
     */
    public AntPathMatcher() {
        this.pathSeparator = DEFAULT_PATH_SEPARATOR;
        this.pathSeparatorPatternCache = new PathSeparatorPatternCache(DEFAULT_PATH_SEPARATOR);
    }

    /**
     * 与自定义路径分隔符一起使用的方便的替代构造函数
     *
     * @param pathSeparator 要使用的路径分隔符不能为{@code null}.
     */
    public AntPathMatcher(String pathSeparator) {
        Assert.notNull(pathSeparator, "'pathSeparator' is required");
        this.pathSeparator = pathSeparator;
        this.pathSeparatorPatternCache = new PathSeparatorPatternCache(pathSeparator);
    }

    /**
     * 设置用于模式分析的路径分隔符。默认值为 "/"
     */
    public void setPathSeparator(String pathSeparator) {
        this.pathSeparator = (pathSeparator != null ? pathSeparator : DEFAULT_PATH_SEPARATOR);
        this.pathSeparatorPatternCache = new PathSeparatorPatternCache(this.pathSeparator);
    }

    /**
     * 指定是否以区分大小写的方式执行模式匹配
     * <p>默认值为 {@code true}
     */
    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    /**
     * 指定是否修剪标记化路径和模式。
     * <p>默认值为 {@code false}.
     */
    public void setTrimTokens(boolean trimTokens) {
        this.trimTokens = trimTokens;
    }

    /**
     * 指定是否为传递到此matcher的{@link #match}方法的模式缓存已解析的模式元数据。
     * true值激活无限制模式缓存；值false将完全关闭模式缓存。
     * 默认情况下，缓存是打开的，但如果在运行时遇到太多的模式需要缓存，则变体会自动将其关闭(阈值为65536)，
     * 前提是出现了任意排列的模式，很少有机会遇到重复出现的模式
     *
     * @see #getStringMatcher(String)
     */
    public void setCachePatterns(boolean cachePatterns) {
        this.cachePatterns = cachePatterns;
    }

    private void deactivatePatternCache() {
        this.cachePatterns = false;
        this.tokenizedPatternCache.clear();
        this.stringMatcherCache.clear();
    }

    @Override
    public boolean isPattern(String path) {
        if (path == null) {
            return false;
        }
        boolean uriVar = false;
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            if (c == '*' || c == '?') {
                return true;
            }
            if (c == '{') {
                uriVar = true;
                continue;
            }
            if (c == '}' && uriVar) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean match(String pattern, String path) {
        return doMatch(pattern, path, true, null);
    }

    @Override
    public boolean matchStart(String pattern, String path) {
        return doMatch(pattern, path, false, null);
    }

    /**
     * 实际将给定{@code path}与给定{@code pattern}进行匹配
     *
     * @param pattern   要匹配的模式
     * @param path      测试路径
     * @param fullMatch 是否需要完全模式匹配（否则只要给定的基本路径匹配就足够了）
     * @return 如果提供的路径匹配，则为true；如果不匹配，则为false
     */
    protected boolean doMatch(String pattern, String path, boolean fullMatch, Map<String, String> uriTemplateVariables) {
        if (path == null || path.startsWith(this.pathSeparator) != pattern.startsWith(this.pathSeparator)) {
            return false;
        }
        String[] pattDirs = tokenizePattern(pattern);
        if (fullMatch && this.caseSensitive && !isPotentialMatch(path, pattDirs)) {
            return false;
        }
        String[] pathDirs = tokenizePath(path);
        int pattIdxStart = 0;
        int pattIdxEnd = pattDirs.length - 1;
        int pathIdxStart = 0;
        int pathIdxEnd = pathDirs.length - 1;
        // Match all elements up to the first **
        while (pattIdxStart <= pattIdxEnd && pathIdxStart <= pathIdxEnd) {
            String pattDir = pattDirs[pattIdxStart];
            if ("**".equals(pattDir)) {
                break;
            }
            if (!matchStrings(pattDir, pathDirs[pathIdxStart], uriTemplateVariables)) {
                return false;
            }
            pattIdxStart++;
            pathIdxStart++;
        }
        if (pathIdxStart > pathIdxEnd) {
            // Path is exhausted, only match if rest of pattern is * or **'s
            if (pattIdxStart > pattIdxEnd) {
                return (pattern.endsWith(this.pathSeparator) == path.endsWith(this.pathSeparator));
            }
            if (!fullMatch) {
                return true;
            }
            if (pattIdxStart == pattIdxEnd && pattDirs[pattIdxStart].equals("*") && path.endsWith(this.pathSeparator)) {
                return true;
            }
            for (int i = pattIdxStart; i <= pattIdxEnd; i++) {
                if (!pattDirs[i].equals("**")) {
                    return false;
                }
            }
            return true;
        } else if (pattIdxStart > pattIdxEnd) {
            // String not exhausted, but pattern is. Failure.
            return false;
        } else if (!fullMatch && "**".equals(pattDirs[pattIdxStart])) {
            // Path start definitely matches due to "**" part in pattern.
            return true;
        }
        // up to last '**'
        while (pattIdxStart <= pattIdxEnd && pathIdxStart <= pathIdxEnd) {
            String pattDir = pattDirs[pattIdxEnd];
            if (pattDir.equals("**")) {
                break;
            }
            if (!matchStrings(pattDir, pathDirs[pathIdxEnd], uriTemplateVariables)) {
                return false;
            }
            pattIdxEnd--;
            pathIdxEnd--;
        }
        if (pathIdxStart > pathIdxEnd) {
            // String is exhausted
            for (int i = pattIdxStart; i <= pattIdxEnd; i++) {
                if (!pattDirs[i].equals("**")) {
                    return false;
                }
            }
            return true;
        }
        while (pattIdxStart != pattIdxEnd && pathIdxStart <= pathIdxEnd) {
            int patIdxTmp = -1;
            for (int i = pattIdxStart + 1; i <= pattIdxEnd; i++) {
                if (pattDirs[i].equals("**")) {
                    patIdxTmp = i;
                    break;
                }
            }
            if (patIdxTmp == pattIdxStart + 1) {
                // '**/**' situation, so skip one
                pattIdxStart++;
                continue;
            }
            // Find the pattern between padIdxStart & padIdxTmp in str between
            // strIdxStart & strIdxEnd
            int patLength = (patIdxTmp - pattIdxStart - 1);
            int strLength = (pathIdxEnd - pathIdxStart + 1);
            int foundIdx = -1;
            strLoop:
            for (int i = 0; i <= strLength - patLength; i++) {
                for (int j = 0; j < patLength; j++) {
                    String subPat = pattDirs[pattIdxStart + j + 1];
                    String subStr = pathDirs[pathIdxStart + i + j];
                    if (!matchStrings(subPat, subStr, uriTemplateVariables)) {
                        continue strLoop;
                    }
                }
                foundIdx = pathIdxStart + i;
                break;
            }
            if (foundIdx == -1) {
                return false;
            }
            pattIdxStart = patIdxTmp;
            pathIdxStart = foundIdx + patLength;
        }
        for (int i = pattIdxStart; i <= pattIdxEnd; i++) {
            if (!pattDirs[i].equals("**")) {
                return false;
            }
        }
        return true;
    }

    private boolean isPotentialMatch(String path, String[] pattDirs) {
        if (!this.trimTokens) {
            int pos = 0;
            for (String pattDir : pattDirs) {
                int skipped = skipSeparator(path, pos, this.pathSeparator);
                pos += skipped;
                skipped = skipSegment(path, pos, pattDir);
                if (skipped < pattDir.length()) {
                    return (skipped > 0 || (pattDir.length() > 0 && isWildcardChar(pattDir.charAt(0))));
                }
                pos += skipped;
            }
        }
        return true;
    }

    private int skipSegment(String path, int pos, String prefix) {
        int skipped = 0;
        for (int i = 0; i < prefix.length(); i++) {
            char c = prefix.charAt(i);
            if (isWildcardChar(c)) {
                return skipped;
            }
            int currPos = pos + skipped;
            if (currPos >= path.length()) {
                return 0;
            }
            if (c == path.charAt(currPos)) {
                skipped++;
            }
        }
        return skipped;
    }

    private int skipSeparator(String path, int pos, String separator) {
        int skipped = 0;
        while (path.startsWith(separator, pos + skipped)) {
            skipped += separator.length();
        }
        return skipped;
    }

    private boolean isWildcardChar(char c) {
        for (char candidate : WILDCARD_CHARS) {
            if (c == candidate) {
                return true;
            }
        }
        return false;
    }

    /**
     * 基于此匹配器的设置，将给定的路径模式标记为多个部分。
     * 基于{@link #setCachePatterns}执行缓存，为实际的标记化算法委托{@link #tokenizePath(String)}
     *
     * @param pattern 要标记化的模式
     * @return 标记化模式部件
     */
    protected String[] tokenizePattern(String pattern) {
        String[] tokenized = null;
        Boolean cachePatterns = this.cachePatterns;
        if (cachePatterns == null || cachePatterns) {
            tokenized = this.tokenizedPatternCache.get(pattern);
        }
        if (tokenized == null) {
            tokenized = tokenizePath(pattern);
            if (cachePatterns == null && this.tokenizedPatternCache.size() >= CACHE_TURNOFF_THRESHOLD) {
                // Try to adapt to the runtime situation that we're encountering:
                // There are obviously too many different patterns coming in here...
                // So let's turn off the cache since the patterns are unlikely to be reoccurring.
                deactivatePatternCache();
                return tokenized;
            }
            if (cachePatterns == null || cachePatterns) {
                this.tokenizedPatternCache.put(pattern, tokenized);
            }
        }
        return tokenized;
    }

    /**
     * 根据此匹配器的设置，将给定路径标记为多个部分。
     *
     * @param path 标记化的路径
     * @return 标记化路径部分
     */
    protected String[] tokenizePath(String path) {
        return StringUtils.tokenizeToStringArray(path, this.pathSeparator, this.trimTokens, true);
    }

    /**
     * 测试字符串是否与模式匹配
     *
     * @param pattern 要匹配的模式(从不为{@code null})
     * @param str     必须与模式匹配的字符串(从不为{@code null})
     * @return 如果字符串与模式匹配，则为true，否则为false
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean matchStrings(String pattern, String str, Map<String, String> uriTemplateVariables) {
        return getStringMatcher(pattern).matchStrings(str, uriTemplateVariables);
    }

    /**
     * 构建或检索{@link AntPathStringMatcher}。给定模式的AntPathStringMatcher。
     * 默认实现检查此AntPathMatcher的内部缓存，请参见{@link #setCachePatterns}，如果未找到缓存副本，则创建一个新的AntPathStringMatcher实例。
     * 如果在运行时遇到太多要缓存的模式（阈值为65536），它会关闭默认缓存，假设出现了任意排列的模式，很少有机会遇到重复出现的模式。
     * 可以重写此方法以实现自定义缓存策略
     *
     * @param pattern 要匹配的模式(从不为null)
     * @return 对应的AntPathStringMatcher(从不为null)
     * @see #setCachePatterns
     */
    protected AntPathStringMatcher getStringMatcher(String pattern) {
        AntPathStringMatcher matcher = null;
        Boolean cachePatterns = this.cachePatterns;
        if (cachePatterns == null || cachePatterns) {
            matcher = this.stringMatcherCache.get(pattern);
        }
        if (matcher == null) {
            matcher = new AntPathStringMatcher(pattern, this.caseSensitive);
            if (cachePatterns == null && this.stringMatcherCache.size() >= CACHE_TURNOFF_THRESHOLD) {
                // Try to adapt to the runtime situation that we're encountering:
                // There are obviously too many different patterns coming in here...
                // So let's turn off the cache since the patterns are unlikely to be reoccurring.
                deactivatePatternCache();
                return matcher;
            }
            if (cachePatterns == null || cachePatterns) {
                this.stringMatcherCache.put(pattern, matcher);
            }
        }
        return matcher;
    }

    /**
     * 提取{@code path}中匹配{@code pattern}的部分，如：
     * <li>'{@code /docs/cvs/commit.html}' and '{@code /docs/cvs/commit.html} &rarr; ''</li>
     * <li>'{@code /docs/*}' and '{@code /docs/cvs/commit} &rarr; '{@code cvs/commit}'</li>
     * <li>'{@code /docs/cvs/*.html}' and '{@code /docs/cvs/commit.html} &rarr; '{@code commit.html}'</li>
     * <li>'{@code /docs/**}' and '{@code /docs/cvs/commit} &rarr; '{@code cvs/commit}'</li>
     * <li>'{@code /docs/**\/*.html}' and '{@code /docs/cvs/commit.html} &rarr; '{@code cvs/commit.html}'</li>
     * <li>'{@code /*.html}' and '{@code /docs/cvs/commit.html} &rarr; '{@code docs/cvs/commit.html}'</li>
     * <li>'{@code *.html}' and '{@code /docs/cvs/commit.html} &rarr; '{@code /docs/cvs/commit.html}'</li>
     * <li>'{@code *}' and '{@code /docs/cvs/commit.html} &rarr; '{@code /docs/cvs/commit.html}'</li> </ul>
     * 假设{@link #match}为'{@code pattern}'和'{@code path}'返回true，但不强制执行此操作。
     */
    @Override
    public String extractPathWithinPattern(String pattern, String path) {
        String[] patternParts = StringUtils.tokenizeToStringArray(pattern, this.pathSeparator, this.trimTokens, true);
        String[] pathParts = StringUtils.tokenizeToStringArray(path, this.pathSeparator, this.trimTokens, true);
        StringBuilder builder = new StringBuilder();
        boolean pathStarted = false;
        for (int segment = 0; segment < patternParts.length; segment++) {
            String patternPart = patternParts[segment];
            if (patternPart.indexOf('*') > -1 || patternPart.indexOf('?') > -1) {
                for (; segment < pathParts.length; segment++) {
                    if (pathStarted || (segment == 0 && !pattern.startsWith(this.pathSeparator))) {
                        builder.append(this.pathSeparator);
                    }
                    builder.append(pathParts[segment]);
                    pathStarted = true;
                }
            }
        }
        return builder.toString();
    }

    @Override
    public Map<String, String> extractUriTemplateVariables(String pattern, String path) {
        Map<String, String> variables = new LinkedHashMap<>();
        boolean result = doMatch(pattern, path, true, variables);
        if (!result) {
            throw new IllegalStateException("Pattern \"" + pattern + "\" is not a match for \"" + path + "\"");
        }
        return variables;
    }

    /**
     * 将两个模式组合为返回的新模式。
     * <h3>例如</h3>
     * <table border="1">
     * <tr><th>Pattern 1</th><th>Pattern 2</th><th>Result</th></tr>
     * <tr><td>{@code null}</td><td>{@code null}</td><td>&nbsp;</td></tr>
     * <tr><td>/hotels</td><td>{@code null}</td><td>/hotels</td></tr>
     * <tr><td>{@code null}</td><td>/hotels</td><td>/hotels</td></tr>
     * <tr><td>/hotels</td><td>/bookings</td><td>/hotels/bookings</td></tr>
     * <tr><td>/hotels</td><td>bookings</td><td>/hotels/bookings</td></tr>
     * <tr><td>/hotels/*</td><td>/bookings</td><td>/hotels/bookings</td></tr>
     * <tr><td>/hotels/&#42;&#42;</td><td>/bookings</td><td>/hotels/&#42;&#42;/bookings</td></tr>
     * <tr><td>/hotels</td><td>{hotel}</td><td>/hotels/{hotel}</td></tr>
     * <tr><td>/hotels/*</td><td>{hotel}</td><td>/hotels/{hotel}</td></tr>
     * <tr><td>/hotels/&#42;&#42;</td><td>{hotel}</td><td>/hotels/&#42;&#42;/{hotel}</td></tr>
     * <tr><td>/*.html</td><td>/hotels.html</td><td>/hotels.html</td></tr>
     * <tr><td>/*.html</td><td>/hotels</td><td>/hotels.html</td></tr>
     * <tr><td>/*.html</td><td>/*.txt</td><td>{@code IllegalArgumentException}</td></tr>
     * </table>
     *
     * @param pattern1 第一种模式
     * @param pattern2 第二种模式
     * @return 两种模式的结合
     * @throws IllegalArgumentException 当两种模式无法组合时
     */
    @Override
    public String combine(String pattern1, String pattern2) {
        if (!StringUtils.hasText(pattern1) && !StringUtils.hasText(pattern2)) {
            return "";
        }
        if (!StringUtils.hasText(pattern1)) {
            return pattern2;
        }
        if (!StringUtils.hasText(pattern2)) {
            return pattern1;
        }
        boolean pattern1ContainsUriVar = (pattern1.indexOf('{') != -1);
        if (!pattern1.equals(pattern2) && !pattern1ContainsUriVar && match(pattern1, pattern2)) {
            // /* + /hotel -> /hotel ; "/*.*" + "/*.html" -> /*.html
            // However /user + /user -> /usr/user ; /{foo} + /bar -> /{foo}/bar
            return pattern2;
        }
        // /hotels/* + /booking -> /hotels/booking
        // /hotels/* + booking -> /hotels/booking
        if (pattern1.endsWith(this.pathSeparatorPatternCache.getEndsOnWildCard())) {
            return concat(pattern1.substring(0, pattern1.length() - 2), pattern2);
        }
        // /hotels/** + /booking -> /hotels/**/booking
        // /hotels/** + booking -> /hotels/**/booking
        if (pattern1.endsWith(this.pathSeparatorPatternCache.getEndsOnDoubleWildCard())) {
            return concat(pattern1, pattern2);
        }
        int starDotPos1 = pattern1.indexOf("*.");
        if (pattern1ContainsUriVar || starDotPos1 == -1 || this.pathSeparator.equals(".")) {
            // simply concatenate the two patterns
            return concat(pattern1, pattern2);
        }
        String ext1 = pattern1.substring(starDotPos1 + 1);
        int dotPos2 = pattern2.indexOf('.');
        String file2 = (dotPos2 == -1 ? pattern2 : pattern2.substring(0, dotPos2));
        String ext2 = (dotPos2 == -1 ? "" : pattern2.substring(dotPos2));
        boolean ext1All = (ext1.equals(".*") || ext1.isEmpty());
        boolean ext2All = (ext2.equals(".*") || ext2.isEmpty());
        if (!ext1All && !ext2All) {
            throw new IllegalArgumentException("Cannot combine patterns: " + pattern1 + " vs " + pattern2);
        }
        String ext = (ext1All ? ext2 : ext1);
        return file2 + ext;
    }

    private String concat(String path1, String path2) {
        boolean path1EndsWithSeparator = path1.endsWith(this.pathSeparator);
        boolean path2StartsWithSeparator = path2.startsWith(this.pathSeparator);
        if (path1EndsWithSeparator && path2StartsWithSeparator) {
            return path1 + path2.substring(1);
        } else if (path1EndsWithSeparator || path2StartsWithSeparator) {
            return path1 + path2;
        } else {
            return path1 + this.pathSeparator + path2;
        }
    }

    /**
     * 给定完整路径，返回一个比较器{@link Comparator}，该比较器适合按照路径的明确性顺序对模式进行排序。例如：
     * <ol>
     * <li>{@code /hotels/new}</li>
     * <li>{@code /hotels/{hotel}}</li>
     * <li>{@code /hotels/*}</li>
     * </ol>
     * 作为参数给出的完整路径用于测试精确匹配
     *
     * @param path 用于比较的完整路径
     * @return 一种比较器，能够按明确性的顺序对模式进行排序
     */
    @Override
    public Comparator<String> getPatternComparator(String path) {
        return new AntPatternComparator(path);
    }

    /**
     * 通过模式测试字符串是否与模式匹配。
     * 模式可能包含特殊字符：'*'表示零个或多个字符；'?'表示一个且仅一个字符；'{'和'}'表示URI模板模式。例如，/users/{user}
     */
    protected static class AntPathStringMatcher {
        @SuppressWarnings("RegExpRedundantEscape")
        private static final Pattern GLOB_PATTERN = Pattern.compile("\\?|\\*|\\{((?:\\{[^/]+?\\}|[^/{}]|\\\\[{}])+?)\\}");
        private static final String DEFAULT_VARIABLE_PATTERN = "((?s).*)";

        private final String rawPattern;
        private final boolean caseSensitive;
        private final boolean exactMatch;
        private final Pattern pattern;
        private final List<String> variableNames = new ArrayList<>();

        public AntPathStringMatcher(String pattern) {
            this(pattern, true);
        }

        public AntPathStringMatcher(String pattern, boolean caseSensitive) {
            this.rawPattern = pattern;
            this.caseSensitive = caseSensitive;
            StringBuilder patternBuilder = new StringBuilder();
            Matcher matcher = GLOB_PATTERN.matcher(pattern);
            int end = 0;
            while (matcher.find()) {
                patternBuilder.append(quote(pattern, end, matcher.start()));
                String match = matcher.group();
                if ("?".equals(match)) {
                    patternBuilder.append('.');
                } else if ("*".equals(match)) {
                    patternBuilder.append(".*");
                } else if (match.startsWith("{") && match.endsWith("}")) {
                    int colonIdx = match.indexOf(':');
                    if (colonIdx == -1) {
                        patternBuilder.append(DEFAULT_VARIABLE_PATTERN);
                        this.variableNames.add(matcher.group(1));
                    } else {
                        String variablePattern = match.substring(colonIdx + 1, match.length() - 1);
                        patternBuilder.append('(');
                        patternBuilder.append(variablePattern);
                        patternBuilder.append(')');
                        String variableName = match.substring(1, colonIdx);
                        this.variableNames.add(variableName);
                    }
                }
                end = matcher.end();
            }
            // No glob pattern was found, this is an exact String match
            if (end == 0) {
                this.exactMatch = true;
                this.pattern = null;
            } else {
                this.exactMatch = false;
                patternBuilder.append(quote(pattern, end, pattern.length()));
                this.pattern = this.caseSensitive ?
                        Pattern.compile(patternBuilder.toString()) :
                        Pattern.compile(patternBuilder.toString(), Pattern.CASE_INSENSITIVE);
            }
        }

        private String quote(String s, int start, int end) {
            if (start == end) {
                return "";
            }
            return Pattern.quote(s.substring(start, end));
        }

        /**
         * 主要入口点
         *
         * @return 如果字符串与模式匹配，则为true，否则为false。
         */
        public boolean matchStrings(String str, Map<String, String> uriTemplateVariables) {
            if (this.exactMatch) {
                return this.caseSensitive ? this.rawPattern.equals(str) : this.rawPattern.equalsIgnoreCase(str);
            } else if (this.pattern != null) {
                Matcher matcher = this.pattern.matcher(str);
                if (matcher.matches()) {
                    if (uriTemplateVariables != null) {
                        if (this.variableNames.size() != matcher.groupCount()) {
                            throw new IllegalArgumentException(
                                    "The number of capturing groups in the pattern segment " +
                                            this.pattern + " does not match the number of URI template variables it defines, " +
                                            "which can occur if capturing groups are used in a URI template regex. " +
                                            "Use non-capturing groups instead."
                            );
                        }
                        for (int i = 1; i <= matcher.groupCount(); i++) {
                            String name = this.variableNames.get(i - 1);
                            if (name.startsWith("*")) {
                                throw new IllegalArgumentException(
                                        "Capturing patterns (" + name + ") are not " +
                                                "supported by the AntPathMatcher. Use the PathPatternParser instead."
                                );
                            }
                            String value = matcher.group(i);
                            uriTemplateVariables.put(name, value);
                        }
                    }
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * {@link #getPatternComparator(String)}返回的默认比较器实现。
     * 按顺序，最“通用”的模式由以下因素决定：
     * <ul>
     * <li>如果为null或捕获所有模式(等于 "/**")</li>
     * <li>如果另一个图案是实际匹配的</li>
     * <li>如果这是一个包罗万象的模式(以"**"结尾</li>
     * <li>如果比其他模式有更多"*"</li>
     * <li>如果比其他模式有更多"{foo}"</li>
     * <li>如果它比其他模式短</li>
     * </ul>
     */
    protected static class AntPatternComparator implements Comparator<String> {
        private final String path;

        public AntPatternComparator(String path) {
            this.path = path;
        }

        /**
         * 比较两种模式，以确定哪种模式应首先匹配，即哪种模式最适合当前路径
         */
        @Override
        public int compare(String pattern1, String pattern2) {
            PatternInfo info1 = new PatternInfo(pattern1);
            PatternInfo info2 = new PatternInfo(pattern2);
            if (info1.isLeastSpecific() && info2.isLeastSpecific()) {
                return 0;
            } else if (info1.isLeastSpecific()) {
                return 1;
            } else if (info2.isLeastSpecific()) {
                return -1;
            }
            boolean pattern1EqualsPath = pattern1.equals(this.path);
            boolean pattern2EqualsPath = pattern2.equals(this.path);
            if (pattern1EqualsPath && pattern2EqualsPath) {
                return 0;
            } else if (pattern1EqualsPath) {
                return -1;
            } else if (pattern2EqualsPath) {
                return 1;
            }
            if (info1.isPrefixPattern() && info2.isPrefixPattern()) {
                return info2.getLength() - info1.getLength();
            } else if (info1.isPrefixPattern() && info2.getDoubleWildcards() == 0) {
                return 1;
            } else if (info2.isPrefixPattern() && info1.getDoubleWildcards() == 0) {
                return -1;
            }
            if (info1.getTotalCount() != info2.getTotalCount()) {
                return info1.getTotalCount() - info2.getTotalCount();
            }
            if (info1.getLength() != info2.getLength()) {
                return info2.getLength() - info1.getLength();
            }
            if (info1.getSingleWildcards() < info2.getSingleWildcards()) {
                return -1;
            } else if (info2.getSingleWildcards() < info1.getSingleWildcards()) {
                return 1;
            }
            if (info1.getUriVars() < info2.getUriVars()) {
                return -1;
            } else if (info2.getUriVars() < info1.getUriVars()) {
                return 1;
            }
            return 0;
        }

        /**
         * 该类保存有关模式的信息
         */
        private static class PatternInfo {
            private final String pattern;
            private int uriVars;
            private int singleWildcards;
            private int doubleWildcards;
            private boolean catchAllPattern;
            private boolean prefixPattern;
            private Integer length;

            public PatternInfo(String pattern) {
                this.pattern = pattern;
                if (this.pattern != null) {
                    initCounters();
                    this.catchAllPattern = this.pattern.equals("/**");
                    this.prefixPattern = !this.catchAllPattern && this.pattern.endsWith("/**");
                }
                if (this.uriVars == 0) {
                    this.length = (this.pattern != null ? this.pattern.length() : 0);
                }
            }

            protected void initCounters() {
                int pos = 0;
                if (this.pattern != null) {
                    while (pos < this.pattern.length()) {
                        if (this.pattern.charAt(pos) == '{') {
                            this.uriVars++;
                            pos++;
                        } else if (this.pattern.charAt(pos) == '*') {
                            if (pos + 1 < this.pattern.length() && this.pattern.charAt(pos + 1) == '*') {
                                this.doubleWildcards++;
                                pos += 2;
                            } else if (pos > 0 && !this.pattern.substring(pos - 1).equals(".*")) {
                                this.singleWildcards++;
                                pos++;
                            } else {
                                pos++;
                            }
                        } else {
                            pos++;
                        }
                    }
                }
            }

            public int getUriVars() {
                return this.uriVars;
            }

            public int getSingleWildcards() {
                return this.singleWildcards;
            }

            public int getDoubleWildcards() {
                return this.doubleWildcards;
            }

            public boolean isLeastSpecific() {
                return (this.pattern == null || this.catchAllPattern);
            }

            public boolean isPrefixPattern() {
                return this.prefixPattern;
            }

            public int getTotalCount() {
                return this.uriVars + this.singleWildcards + (2 * this.doubleWildcards);
            }

            /**
             * 返回给定模式的长度，其中模板变量的长度为1
             */
            public int getLength() {
                if (this.length == null) {
                    this.length = (this.pattern != null ? VARIABLE_PATTERN.matcher(this.pattern).replaceAll("#").length() : 0);
                }
                return this.length;
            }
        }
    }

    /**
     * 依赖于配置的路径分隔符的模式的简单缓存
     */
    private static class PathSeparatorPatternCache {
        private final String endsOnWildCard;
        private final String endsOnDoubleWildCard;

        public PathSeparatorPatternCache(String pathSeparator) {
            this.endsOnWildCard = pathSeparator + "*";
            this.endsOnDoubleWildCard = pathSeparator + "**";
        }

        public String getEndsOnWildCard() {
            return this.endsOnWildCard;
        }

        public String getEndsOnDoubleWildCard() {
            return this.endsOnDoubleWildCard;
        }
    }
}
