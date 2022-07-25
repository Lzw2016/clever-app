package org.clever.core.json;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Json-List的相互转换工具<br/>
 * 1.通过Jackson实现<br/>
 * <p/>
 * 作者：LiZW <br/>
 * 创建时间：2016-4-28 0:55 <br/>
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class JsonArrayWrapper implements Collection<Object> {
    public final List innerList;

    public JsonArrayWrapper(List innerList) {
        this.innerList = innerList;
    }

    public JsonArrayWrapper appendObj(Object... args) {
        JsonWrapper jw = new JsonWrapper();
        jw.set(args);
        innerList.add(jw.getInnerMap());
        return this;
    }

    public JsonArrayWrapper append(Object obj) {
        innerList.add(obj);
        return this;
    }

    public Object get(int index) {
        return innerList.get(index);
    }

    @Override
    public int size() {
        return innerList.size();
    }

    @Override
    public boolean isEmpty() {
        return innerList.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return innerList.contains(o);
    }

    @Override
    public Iterator<Object> iterator() {
        return innerList.iterator();
    }

    @Override
    public Object[] toArray() {
        return innerList.toArray();
    }

    @Override
    public Object[] toArray(Object[] a) {
        return innerList.toArray(a);
    }

    @Override
    public boolean add(Object o) {
        if (o instanceof JsonWrapper) {
            return innerList.add(((JsonWrapper) o).getInnerMap());
        }
        return innerList.add(o);
    }

    @Override
    public boolean remove(Object o) {
        return innerList.remove(o);
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        return innerList.containsAll(c);
    }

    @Override
    public boolean addAll(@NotNull Collection<?> c) {
        return innerList.addAll(c);
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        return innerList.removeAll(c);
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        return innerList.retainAll(c);
    }

    @Override
    public void clear() {
        innerList.clear();
    }
}
