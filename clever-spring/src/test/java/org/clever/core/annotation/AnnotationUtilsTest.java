package org.clever.core.annotation;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.lang.annotation.*;

/**
 * 作者：lizw <br/>
 * 创建时间：2022/05/22 13:55 <br/>
 */
@Slf4j
public class AnnotationUtilsTest {
    @Test
    public void t01() {
        log.info("getAnnotation         --> {}", AnnotationUtils.getAnnotation(ParentController.class, RequestMapping.class));
        log.info("getAnnotation         --> {}", AnnotationUtils.getAnnotation(ChildController.class, RequestMapping.class));
        log.info("------------------------------");
        log.info("findAnnotation        --> {}", AnnotationUtils.findAnnotation(ParentController.class, RequestMapping.class));
        log.info("findAnnotation        --> {}", AnnotationUtils.findAnnotation(ChildController.class, RequestMapping.class));
        log.info("------------------------------");
        log.info("isAnnotated           --> {}", AnnotatedElementUtils.isAnnotated(ParentController.class, RequestMapping.class));
        log.info("isAnnotated           --> {}", AnnotatedElementUtils.isAnnotated(ChildController.class, RequestMapping.class));
        log.info("------------------------------");
        log.info("getMergedAnnotation   --> {}", AnnotatedElementUtils.getMergedAnnotation(ParentController.class, RequestMapping.class));
        log.info("getMergedAnnotation   --> {}", AnnotatedElementUtils.getMergedAnnotation(ChildController.class, RequestMapping.class));
        log.info("------------------------------");
        log.info("hasAnnotation         --> {}", AnnotatedElementUtils.hasAnnotation(ParentController.class, RequestMapping.class));
        log.info("hasAnnotation         --> {}", AnnotatedElementUtils.hasAnnotation(ChildController.class, RequestMapping.class));
        log.info("------------------------------");
        log.info("findMergedAnnotation  --> {}", AnnotatedElementUtils.findMergedAnnotation(ParentController.class, RequestMapping.class));
        log.info("findMergedAnnotation  --> {}", AnnotatedElementUtils.findMergedAnnotation(ChildController.class, RequestMapping.class));
    }
}

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Documented
@interface RequestMapping {
    String name() default "";

    @AliasFor("path")
    String[] value() default {};


    @AliasFor("value")
    String[] path() default {};
}

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@RequestMapping
@interface PostMapping {

    @AliasFor(annotation = RequestMapping.class)
    String name() default "";

    @AliasFor(annotation = RequestMapping.class)
    String[] value() default {};

    @AliasFor(annotation = RequestMapping.class)
    String[] path() default {};
}


@RequestMapping(value = "parent/controller")
class ParentController {
}

class ChildController extends ParentController {
}