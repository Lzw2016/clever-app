package org.clever.core.validator;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import java.util.List;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/01/13 10:30 <br/>
 */
@Slf4j
public class BaseValidatorUtilsTest {
    @Test
    public void t01() {
        EntityA entityA = new EntityA();
        List<FieldError> list = BaseValidatorUtils.validate(entityA);
        log.info("list -> {}", list);
        log.info("getMessages -> {}", FieldError.getMessages(list));
        log.info("getErrors -> {}", FieldError.getErrors(list));
        log.info("getErrorMap -> {}", FieldError.getErrorMap(list));
    }

    @Data
    public static class EntityA {
        @NotBlank(message = "字段a不能为空")
        private String a;
        @Min(value = 11)
        @Max(value = 9)
        private int b = 10;
        @Valid
        private EntityB entityB = new EntityB();
    }

    @Data
    public static class EntityB {
        @NotBlank(message = "字段c不能为空")
        private String c;
        @Min(value = 11)
        @Max(value = 9)
        private int d = 10;
    }
}
