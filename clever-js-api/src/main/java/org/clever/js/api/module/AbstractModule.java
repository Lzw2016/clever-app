package org.clever.js.api.module;

import org.clever.js.api.GlobalConstant;
import org.clever.js.api.ScriptEngineContext;
import org.clever.js.api.folder.Folder;
import org.clever.js.api.require.Require;
import org.clever.util.Assert;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/07/18 22:16 <br/>
 *
 * @param <E> script引擎类型
 * @param <T> script引擎对象类型
 */
public abstract class AbstractModule<E, T> implements Module<T> {
    /**
     * 引擎上下文
     */
    protected final ScriptEngineContext<E, T> engineContext;
    /**
     * 当前模块对象对应的 module 对象
     */
    protected final T module;
    /**
     * 模块的识别符，通常是带有绝对路径的模块文件名
     */
    protected final String id;
    /**
     * 模块的完全解析后的文件名，带有绝对路径
     */
    protected final String filename;
    /**
     * 返回一个对象，最先引用该模块的模块
     */
    protected final Module<T> parent;
    /**
     * 模块的搜索路径
     */
    protected final List<String> paths;
    /**
     * module.require() 方法提供了一种加载模块的方法，就像从原始模块调用 require() 一样
     */
    protected final Require<T> require;
    /**
     * 子模块ID集合
     */
    protected final Set<String> childrenIds = new HashSet<>();
    /**
     * 模块是否已经加载完成，或正在加载中
     */
    protected boolean loaded = false;
    /**
     * 模块是否已经移除
     */
    protected boolean removed = false;

    public AbstractModule(ScriptEngineContext<E, T> engineContext,
                          String id,
                          String filename,
                          T exports,
                          Module<T> parent,
                          Require<T> require) {
        Assert.notNull(engineContext, "参数 engineContext 不能为 null");
        Assert.isNotBlank(id, "参数 id 不能为空");
        Assert.isNotBlank(filename, "参数 filename 不能为空");
        Assert.notNull(exports, "参数 exports 不能为 null");
        Assert.notNull(parent, "参数 parent 不能为 null");
        Assert.notNull(require, "参数 require 不能为 null");
        this.engineContext = engineContext;
        this.id = id;
        this.filename = filename;
        this.parent = parent;
        this.parent.addChildModule(this);
        this.paths = Collections.singletonList(filename);
        this.require = require;
        this.module = newScriptObject();
        initModule(exports);
    }

    protected AbstractModule(ScriptEngineContext<E, T> engineContext) {
        Assert.notNull(engineContext, "参数 engineContext 不能为 null");
        this.engineContext = engineContext;
        this.id = GlobalConstant.Module_Main;
        this.filename = Folder.Root_Path + GlobalConstant.Module_Main;
        this.parent = null;
        this.paths = Collections.singletonList(this.filename);
        this.require = engineContext.getRequire();
        this.module = newScriptObject();
        initModule(newScriptObject());
    }

    /**
     * 初始化ScriptModule
     *
     * @param exports 当前模块对应的script对象
     */
    protected abstract void initModule(T exports);

    /**
     * 创建ScriptObject
     */
    protected abstract T newScriptObject();

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getFilename() {
        return filename;
    }

    @Override
    public boolean isLoaded() {
        return loaded;
    }

    @Override
    public Module<T> getParent() {
        return parent;
    }

    @Override
    public List<String> paths() {
        return paths;
    }

    @Override
    public Set<String> getChildren() {
        return Collections.unmodifiableSet(childrenIds);
    }

    @Override
    public T require(String id) throws Exception {
        return require.require(id);
    }

    @Override
    public Require<T> getRequire() {
        return require;
    }

    @Override
    public T getModule() {
        return module;
    }

    @Override
    public void triggerOnLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;
        removed = false;
        doTriggerOnLoaded();
    }

    /**
     * 触发模块加载完成事件
     */
    protected abstract void doTriggerOnLoaded();

    @Override
    public void triggerOnRemove() {
        if (removed) {
            return;
        }
        removed = true;
        doTriggerOnRemove();
    }

    /**
     * 触发模块移除事件
     */
    protected abstract void doTriggerOnRemove();

    @Override
    public void addChildModule(Module<T> childModule) {
        if (childModule == null || childrenIds.contains(childModule.getId())) {
            return;
        }
        childrenIds.add(childModule.getId());
    }

    /**
     * 获取模块缓存
     */
    public ModuleCache<T> getCache() {
        return engineContext.getModuleCache();
    }
}
