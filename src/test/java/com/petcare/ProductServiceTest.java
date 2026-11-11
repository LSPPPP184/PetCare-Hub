package com.petcare;

import com.petcare.domain.*;
import com.petcare.domain.factory.ProductFactory;
import com.petcare.repository.ProductRepository;
import com.petcare.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import java.util.stream.IntStream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ProductServiceTest {
    private final ProductRepository repository = mock(ProductRepository.class);
    private final ProductFactory factory = new ProductFactory();
    private final ProductService service = new ProductService(repository, factory);

    private Product product(long id, ProductType type, String name, String price) {
        var p = factory.create(ProductFactoryTest.form(type, name, price));
        ReflectionTestUtils.setField(p, "id", id);
        return p;
    }

    @Test void filtersGroupNameAndHiddenProducts() {
        var hidden = product(3, ProductType.PET, "Mèo ẩn", "10");
        hidden.hide();
        var visible = List.of(
            product(1, ProductType.PET, "Mèo con", "100"),
            product(2, ProductType.ACCESSORY, "Đồ chơi mèo", "50"));
        when(repository.findForCatalog("mèo", false)).thenReturn(visible);
        when(repository.findForCatalog("mèo", true)).thenReturn(List.of(visible.get(0), visible.get(1), hidden));
        when(repository.findForCatalog("", false)).thenReturn(visible);
        assertThat(service.search("PET", " MÈO ", 0, false, false).getContent())
            .extracting(v -> v.name()).containsExactly("Mèo con");
        assertThat(service.search("", "mèo", 0, false, true).getTotalElements()).isEqualTo(3);
        assertThat(service.search("SERVICE", "", 0, false, false)).isEmpty();
        verify(repository).findForCatalog("mèo", false);
        verify(repository).findForCatalog("mèo", true);
        verify(repository, never()).findAll();
    }

    @Test void sortsByCalculatedPriceAndPaginates() {
        when(repository.findForCatalog("", false)).thenReturn(List.of(
            product(1, ProductType.GROOMING, "Tắm", "100"),
            product(2, ProductType.ACCESSORY, "Bóng", "120")));
        assertThat(service.search("", "", 0, false, false).getContent())
            .extracting(v -> v.id()).containsExactly(2L, 1L);
        assertThat(service.search("", "", 0, true, false).getContent())
            .extracting(v -> v.id()).containsExactly(1L, 2L);
        when(repository.findForCatalog("", false)).thenReturn(IntStream.rangeClosed(1, 13)
            .mapToObj(i -> product(i, ProductType.PET, "Thú " + i, "100")).toList());
        assertThat(service.search("", "", 0, false, false).getNumberOfElements()).isEqualTo(12);
        assertThat(service.search("", "", 1, false, false).getContent()).extracting(v -> v.id()).containsExactly(13L);
        assertThat(service.search("", "", 2, false, false)).isEmpty();
    }
}
