package org.clever.boot.logging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * 单个记录器组
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/14 22:32 <br/>
 */
public final class LoggerGroup {
    private final String name;
    private final List<String> members;
    private LogLevel configuredLevel;

    LoggerGroup(String name, List<String> members) {
        this.name = name;
        this.members = Collections.unmodifiableList(new ArrayList<>(members));
    }

    public String getName() {
        return this.name;
    }

    public List<String> getMembers() {
        return this.members;
    }

    public boolean hasMembers() {
        return !this.members.isEmpty();
    }

    public LogLevel getConfiguredLevel() {
        return this.configuredLevel;
    }

    public void configureLogLevel(LogLevel level, BiConsumer<String, LogLevel> configurer) {
        this.configuredLevel = level;
        this.members.forEach((name) -> configurer.accept(name, level));
    }
}
