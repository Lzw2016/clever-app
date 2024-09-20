package org.clever.boot;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.format.Printer;

import java.lang.reflect.Array;
import java.util.*;

/**
 * 作者：lizw <br/>
 * 创建时间：2022/04/30 22:45 <br/>
 */
@SuppressWarnings("DataFlowIssue")
@Slf4j
public class TypeDescriptorTest {
    @Test
    public void t01() {
        // String
        TypeDescriptor typeDescriptor = TypeDescriptor.valueOf(String.class);
        log.info("--> {}", typeDescriptor);
        // List
        List<String> list = new ArrayList<>();
        list.add("123");
        typeDescriptor = TypeDescriptor.forObject(list);
        log.info("--> {}", typeDescriptor);
        log.info("--> {}", Arrays.asList(typeDescriptor.getResolvableType().getGenerics()));
        // HashMap
        typeDescriptor = TypeDescriptor.forObject(new HashMap<String, Integer>() {{
            put("aaa", 666);
        }});
        log.info("--> {}", typeDescriptor);
        log.info("--> {}", Arrays.asList(typeDescriptor.getResolvableType().getSuperType().getGenerics()));
    }

    private HashMap<Integer, List<String>> testMap;

    @Test
    public void t02() throws NoSuchFieldException {
        ResolvableType resolvableType = ResolvableType.forField(getClass().getDeclaredField("testMap"));
        log.info("--> {}", resolvableType.getSuperType());                  // AbstractMap<Integer, List<String>>
        log.info("--> {}", resolvableType.asMap());                         // Map<Integer, List<String>>
        log.info("--> {}", resolvableType.getGeneric(0).resolve());         // Integer
        log.info("--> {}", resolvableType.getGeneric(1).resolve());         // List
        log.info("--> {}", resolvableType.getGeneric(1));                   // List<String>
        // 第二个泛型,里面的泛型,即List<String>里面的String
        log.info("--> {}", resolvableType.resolveGeneric(1, 0));    // String
    }

    @SuppressWarnings("InstantiatingObjectToGetClassObject")
    @Test
    public void t03() {
        log.info("--> {}", int[].class.getName()); // [I
        log.info("--> {}", TypeDescriptorTest[].class.getName()); // [Lorg.clever.core.convert.TypeDescriptorTest;
        log.info("--> {}", Array.newInstance(Integer.class, 0).getClass()); // class [Ljava.lang.Integer;
        log.info("--> {}", (new Integer[0]).getClass()); // class [Ljava.lang.Integer;
        log.info("--> {}", GenericTypeResolver.resolveTypeArgument(PrinterDate.class, Printer.class)); // class java.util.Date
    }

    @SuppressWarnings("NullableProblems")
    static class PrinterDate implements Printer<Date> {
        @Override
        public String print(Date object, Locale locale) {
            return null;
        }
    }
}
