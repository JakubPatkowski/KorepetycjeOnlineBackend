package com.example.ekorki.validators.implementation;

import com.example.ekorki.validators.ValidPassword;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.stereotype.Controller;
import jakarta.validation.ConstraintValidator;

import java.util.regex.Pattern;

import java.util.ArrayList;
import java.util.List;

@Controller
public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile(".*[a-z].*");
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile(".*[A-Z].*");
    private static final Pattern DIGIT_PATTERN = Pattern.compile(".*\\d.*");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile(".*[\\W_].*");

    @Override
    public void initialize(ValidPassword constraintAnnotation) {
    }

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        List<String> errors = new ArrayList<>();

        if (password == null) {
            errors.add("Password cannot be null");
        } else {
            if (!LOWERCASE_PATTERN.matcher(password).matches()) {
                errors.add("Password must contain at least one lowercase letter");
            }

            if (!UPPERCASE_PATTERN.matcher(password).matches()) {
                errors.add("Password must contain at least one uppercase letter");
            }

            if (!DIGIT_PATTERN.matcher(password).matches()) {
                errors.add("Password must contain at least one digit");
            }

            if (!SPECIAL_CHAR_PATTERN.matcher(password).matches()) {
                errors.add("Password must contain at least one special character");
            }
        }

        if (!errors.isEmpty()) {
            context.disableDefaultConstraintViolation();
            for (String error : errors) {                context.buildConstraintViolationWithTemplate(error)
                        .addConstraintViolation();
            }
            return false;
        }

        return true;
    }
}
