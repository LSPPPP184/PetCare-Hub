package com.petcare.dto;

import jakarta.validation.Validation;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class RegistrationRequestTest {
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"        ", "12345678", "0901234567", "+84 901 234 567",
        "(090) 123-4567", "12345678901234567890"})
    void acceptsOptionalOrValidPhone(String phone) {
        var request = new RegistrationRequest();
        request.setPhone(phone);
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertThat(factory.getValidator().validateProperty(request, "phone")).isEmpty();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"--------", "() () ()", "1234567", "123456789012345678901",
        "090+1234567", "++84901234567", "0901234567+", "090abc4567",
        "+12345678901234567890", "1-------"})
    void rejectsInvalidPhone(String phone) {
        var request = new RegistrationRequest();
        request.setPhone(phone);
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertThat(factory.getValidator().validateProperty(request, "phone")).isNotEmpty();
        }
    }

    @ParameterizedTest
    @CsvSource(value = {"'  +84  901 234 567  ', '+84 901 234 567'", "'        ', ''"})
    void normalizesWhitespace(String input, String expected) {
        var request = new RegistrationRequest();
        request.setPhone(input);
        assertThat(request.getPhone()).isEqualTo(expected);
    }
}
