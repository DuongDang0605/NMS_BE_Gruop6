package com.mvc.nms_be.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({FIELD, PARAMETER})
@Retention(RUNTIME)
@Constraint(validatedBy = Utf8LengthValidator.class)
public @interface Utf8Length {
    String message() default "Password must contain at most {max} UTF-8 bytes";
    int max() default 72;
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
