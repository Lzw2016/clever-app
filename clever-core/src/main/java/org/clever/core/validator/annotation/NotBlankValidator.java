package org.clever.core.validator.annotation;

import org.apache.commons.lang3.StringUtils;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * 作者： lzw<br/>
 * 创建时间：2018-11-09 21:49 <br/>
 */
public class NotBlankValidator implements ConstraintValidator<NotBlank, String> {
    @Override
    public boolean isValid(String str, ConstraintValidatorContext constraintValidatorContext) {
        if (str == null) {
            return true;
        }
        return StringUtils.isNotBlank(str);
    }
}
