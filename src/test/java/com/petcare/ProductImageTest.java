package com.petcare;

import com.petcare.dto.ProductForm;
import jakarta.validation.Validation;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.assertThat;

class ProductImageTest {
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"https://example.com/pet.jpg", "http://example.com/image?id=1", "HTTPS://example.com/pet.png"})
    void acceptsOptionalHttpImageUrl(String url) {
        var form = new ProductForm();
        form.setImageUrl(url);
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertThat(factory.getValidator().validateProperty(form, "imageUrlValid")).isEmpty();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"javascript:alert(1)", "data:image/png;base64,abc", "file:///tmp/pet.png",
        "//example.com/pet.jpg", "https://", "https://example.com/a b.jpg", "https://user:pass@example.com/a.jpg"})
    void rejectsMalformedOrUnsupportedImageUrl(String url) {
        var form = new ProductForm();
        form.setImageUrl(url);
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertThat(factory.getValidator().validateProperty(form, "imageUrlValid")).hasSize(1);
        }
    }
}
