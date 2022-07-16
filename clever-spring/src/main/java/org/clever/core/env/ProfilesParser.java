package org.clever.core.env;

import org.clever.util.Assert;
import org.clever.util.StringUtils;

import java.util.*;
import java.util.function.Predicate;

/**
 * {@link Profiles#of}使用的内部解析器
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/04 16:16 <br/>
 */
final class ProfilesParser {
    private ProfilesParser() {
    }

    static Profiles parse(String... expressions) {
        Assert.notEmpty(expressions, "Must specify at least one profile");
        Profiles[] parsed = new Profiles[expressions.length];
        for (int i = 0; i < expressions.length; i++) {
            parsed[i] = parseExpression(expressions[i]);
        }
        return new ParsedProfiles(expressions, parsed);
    }

    private static Profiles parseExpression(String expression) {
        Assert.hasText(expression, () -> "Invalid profile expression [" + expression + "]: must contain text");
        StringTokenizer tokens = new StringTokenizer(expression, "()&|!", true);
        return parseTokens(expression, tokens);
    }

    private static Profiles parseTokens(String expression, StringTokenizer tokens) {
        return parseTokens(expression, tokens, Context.NONE);
    }

    private static Profiles parseTokens(String expression, StringTokenizer tokens, Context context) {
        List<Profiles> elements = new ArrayList<>();
        Operator operator = null;
        while (tokens.hasMoreTokens()) {
            String token = tokens.nextToken().trim();
            if (token.isEmpty()) {
                continue;
            }
            switch (token) {
                case "(":
                    Profiles contents = parseTokens(expression, tokens, Context.BRACKET);
                    if (context == Context.INVERT) {
                        return contents;
                    }
                    elements.add(contents);
                    break;
                case "&":
                    assertWellFormed(expression, operator == null || operator == Operator.AND);
                    operator = Operator.AND;
                    break;
                case "|":
                    assertWellFormed(expression, operator == null || operator == Operator.OR);
                    operator = Operator.OR;
                    break;
                case "!":
                    elements.add(not(parseTokens(expression, tokens, Context.INVERT)));
                    break;
                case ")":
                    Profiles merged = merge(expression, elements, operator);
                    if (context == Context.BRACKET) {
                        return merged;
                    }
                    elements.clear();
                    elements.add(merged);
                    operator = null;
                    break;
                default:
                    Profiles value = equals(token);
                    if (context == Context.INVERT) {
                        return value;
                    }
                    elements.add(value);
            }
        }
        return merge(expression, elements, operator);
    }

    private static Profiles merge(String expression, List<Profiles> elements, Operator operator) {
        assertWellFormed(expression, !elements.isEmpty());
        if (elements.size() == 1) {
            return elements.get(0);
        }
        Profiles[] profiles = elements.toArray(new Profiles[0]);
        return (operator == Operator.AND ? and(profiles) : or(profiles));
    }

    private static void assertWellFormed(String expression, boolean wellFormed) {
        Assert.isTrue(wellFormed, () -> "Malformed profile expression [" + expression + "]");
    }

    private static Profiles or(Profiles... profiles) {
        return activeProfile -> Arrays.stream(profiles).anyMatch(isMatch(activeProfile));
    }

    private static Profiles and(Profiles... profiles) {
        return activeProfile -> Arrays.stream(profiles).allMatch(isMatch(activeProfile));
    }

    private static Profiles not(Profiles profiles) {
        return activeProfile -> !profiles.matches(activeProfile);
    }

    private static Profiles equals(String profile) {
        return activeProfile -> activeProfile.test(profile);
    }

    private static Predicate<Profiles> isMatch(Predicate<String> activeProfile) {
        return profiles -> profiles.matches(activeProfile);
    }

    private enum Operator {AND, OR}

    private enum Context {NONE, INVERT, BRACKET}

    private static class ParsedProfiles implements Profiles {
        private final Set<String> expressions = new LinkedHashSet<>();
        private final Profiles[] parsed;

        ParsedProfiles(String[] expressions, Profiles[] parsed) {
            Collections.addAll(this.expressions, expressions);
            this.parsed = parsed;
        }

        @Override
        public boolean matches(Predicate<String> activeProfiles) {
            for (Profiles candidate : this.parsed) {
                if (candidate.matches(activeProfiles)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public int hashCode() {
            return this.expressions.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            ParsedProfiles that = (ParsedProfiles) obj;
            return this.expressions.equals(that.expressions);
        }

        @Override
        public String toString() {
            return StringUtils.collectionToDelimitedString(this.expressions, " or ");
        }
    }
}
