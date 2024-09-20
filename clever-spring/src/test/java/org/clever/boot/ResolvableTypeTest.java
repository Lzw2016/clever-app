package org.clever.boot;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.core.ResolvableType;

/**
 * 作者：lizw <br/>
 * 创建时间：2022/07/16 11:44 <br/>
 */
@Slf4j
public class ResolvableTypeTest {
    @Data
    public static class ClassA<T> {
        private final T f;
    }

    public static class ClassB extends ClassA<String> {
        public ClassB(String f) {
            super(f);
        }
    }

    public static class ClassC extends ClassA<Integer> {
        public ClassC(Integer f) {
            super(f);
        }
    }

    public static class ClassD extends ClassA<CharSequence> {
        public ClassD(CharSequence f) {
            super(f);
        }
    }

    public static class ClassE extends ClassD {
        public ClassE(CharSequence f) {
            super(f);
        }
    }

    @Test
    public void t01() {
        {
            ClassA<String> a = new ClassA<>("a");
            ClassA<Integer> b = new ClassA<>(1);
            ResolvableType aType = ResolvableType.forType(a.getClass());
            ResolvableType bType = ResolvableType.forType(b.getClass());
            log.info("aType --> {}", aType);
            log.info("bType --> {}", bType);
            log.info("isAssignableFrom --> {}", aType.isAssignableFrom(bType));
        }
        log.info("------------------------");
        {
            ClassB a = new ClassB("a");
            ClassC b = new ClassC(1);
            ResolvableType aType = ResolvableType.forType(a.getClass());
            ResolvableType bType = ResolvableType.forType(b.getClass());
            log.info("aType --> {}", aType);
            log.info("bType --> {}", bType);
            log.info("isAssignableFrom --> {}", aType.isAssignableFrom(bType));
        }
        log.info("------------------------");
        {
            ClassB a = new ClassB("a");
            ClassD b = new ClassD("d");
            ResolvableType aType = ResolvableType.forType(a.getClass());
            ResolvableType bType = ResolvableType.forType(b.getClass());
            log.info("aType --> {}", aType);
            log.info("bType --> {}", bType);
            log.info("isAssignableFrom --> {}", aType.isAssignableFrom(bType));
            log.info("isAssignableFrom --> {}", bType.isAssignableFrom(aType));
        }
        log.info("------------------------");
        {
            ClassD a = new ClassD("d");
            ClassE b = new ClassE("e");
            ResolvableType aType = ResolvableType.forType(a.getClass());
            ResolvableType bType = ResolvableType.forType(b.getClass());
            log.info("aType --> {}", aType);
            log.info("bType --> {}", bType);
            a = b;
            log.info("isAssignableFrom --> {}", aType.isAssignableFrom(bType));
            // b = a; // 编译错误
            log.info("isAssignableFrom --> {}", bType.isAssignableFrom(aType));
        }
    }
}
