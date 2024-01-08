package org.clever.core.flow;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.clever.util.Assert;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 任务变量
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2024/01/07 20:38 <br/>
 */
@EqualsAndHashCode
public class WorkerVariable {
    private static final String DEF_PARAM = WorkerVariable.class.getName() + "_DEF_PARAM";

    /**
     * 任务变量是否只读
     */
    @Getter
    private final boolean readOnly;
    /**
     * 任务变量数据
     */
    @Getter
    private final Map<String, Object> variables;

    /**
     * @param readOnly  任务变量是否只读
     * @param variables 任务变量数据
     */
    public WorkerVariable(boolean readOnly, Map<String, Object> variables) {
        Assert.notNull(variables, "参数 variables 不能为 null");
        this.readOnly = readOnly;
        if (readOnly) {
            this.variables = Collections.unmodifiableMap(variables);
        } else {
            this.variables = new ConcurrentHashMap<>(variables);
        }
    }

    public WorkerVariable() {
        this(false, Collections.emptyMap());
    }

    /**
     * 检查当前 WorkerVariable 是否可修改，如果不能修改就抛出 IllegalArgumentException 异常
     */
    public void checkModifiable() {
        if (readOnly) {
            throw new IllegalArgumentException("WorkerVariable是只读,不可修改");
        }
    }

    /**
     * 设置变量
     *
     * @param name  变量名
     * @param value 变量值
     */
    public WorkerVariable set(String name, Object value) {
        checkModifiable();
        Assert.isNotBlank(name, "变量 name 不能为空");
        if (value == null) {
            return this;
        }
        variables.put(name, value);
        return this;
    }

    /**
     * 获取变量值
     *
     * @param name     变量名
     * @param defValue 变量不存在时的默认值
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String name, T defValue) {
        Assert.isNotBlank(name, "变量 name 不能为空");
        return (T) variables.getOrDefault(name, defValue);
    }

    /**
     * 获取变量值
     *
     * @param name 变量名
     */
    public <T> T get(String name) {
        return get(name, null);
    }

    /**
     * 设置默认变量
     *
     * @param value 变量值
     */
    public WorkerVariable def(Object value) {
        return set(DEF_PARAM, value);
    }

    /**
     * 获取默认变量值
     */
    public <T> T def() {
        return get(DEF_PARAM);
    }

    /**
     * 删除指定的变量
     *
     * @param names 变量名
     */
    public WorkerVariable remove(String... names) {
        checkModifiable();
        if (names != null) {
            for (String name : names) {
                variables.remove(name);
            }
        }
        return this;
    }

    /**
     * 判断变量是否存在
     *
     * @param name 变量名
     */
    public boolean exists(String name) {
        return variables.containsKey(name);
    }

    /**
     * 获取变量数量
     */
    public int size() {
        return variables.size();
    }

    /**
     * 变量数量是否为0
     */
    public boolean isEmpty() {
        return variables.isEmpty();
    }

    /**
     * 清空所有变量
     */
    public WorkerVariable clear() {
        checkModifiable();
        variables.clear();
        return this;
    }

    /**
     * 获取所有的变量数据(返回只读集合)
     */
    public Map<String, Object> all() {
        return Collections.unmodifiableMap(variables);
    }

    /**
     * 转为不可变对象
     */
    public WorkerVariable toUnmodifiable() {
        if (readOnly) {
            return this;
        }
        return new WorkerVariable(true, new HashMap<>(variables));
    }
}
