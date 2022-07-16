package org.clever.boot.context.properties.source;

import java.util.List;

/**
 * 当{@link ConfigurationPropertyName}包含无效字符时引发异常。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 21:37 <br/>
 */
public class InvalidConfigurationPropertyNameException extends RuntimeException {
    private final CharSequence name;
    private final List<Character> invalidCharacters;

    public InvalidConfigurationPropertyNameException(CharSequence name, List<Character> invalidCharacters) {
        super("Configuration property name '" + name + "' is not valid");
        this.name = name;
        this.invalidCharacters = invalidCharacters;
    }

    public List<Character> getInvalidCharacters() {
        return this.invalidCharacters;
    }

    public CharSequence getName() {
        return this.name;
    }

    public static void throwIfHasInvalidChars(CharSequence name, List<Character> invalidCharacters) {
        if (!invalidCharacters.isEmpty()) {
            throw new InvalidConfigurationPropertyNameException(name, invalidCharacters);
        }
    }
}
