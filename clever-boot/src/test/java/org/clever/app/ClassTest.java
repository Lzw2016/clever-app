package org.clever.app;

import io.javalin.http.Context;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.clever.app.mvc.MvcTest;
import org.clever.core.LocalVariableTableParameterNameDiscoverer;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/01/17 11:55 <br/>
 */
@Slf4j
public class ClassTest {
    @SneakyThrows
    @Test
    public void t01() {
        Method method = MvcTest.class.getMethod("t02", Context.class);
        //noinspection ConstantConditions,RedundantClassCall --> false
        log.info("###isInstance--> {}", MvcTest.class.isInstance(null));
        for (Parameter parameter : method.getParameters()) {
            log.info("###--> {}: {}", parameter.getType().getName(), parameter.getName());
        }
        String[] parameterNames = new LocalVariableTableParameterNameDiscoverer(true).getParameterNames(method);
        log.info("###--> {}", Arrays.toString(parameterNames));
    }
}
