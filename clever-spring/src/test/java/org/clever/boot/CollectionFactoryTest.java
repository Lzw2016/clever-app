package org.clever.boot;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.core.CollectionFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

/**
 * 作者：lizw <br/>
 * 创建时间：2022/05/09 20:20 <br/>
 */
@Slf4j
public class CollectionFactoryTest {
    enum MyEnum {
        A, B, C
    }

    @Test
    public void t01() {
        Collection<String> list = CollectionFactory.createCollection(List.class, String.class, 1);
        list.add("aaa");
        log.info("--> {} | {}", list.getClass(), list);
        Collection<String> list2 = CollectionFactory.createCollection(ArrayList.class, Integer.class, 1);
        list2.add("aaa");
        log.info("--> {} | {}", list2.getClass(), list2);
        Collection<MyEnum> list3 = CollectionFactory.createCollection(EnumSet.class, MyEnum.class, 1);
        list3.add(MyEnum.A);
        log.info("--> {} | {}", list3.getClass(), list3);
    }
}
