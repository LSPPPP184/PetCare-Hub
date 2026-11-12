package com.petcare;

import com.petcare.domain.*;
import com.petcare.domain.order.*;
import com.petcare.domain.factory.ProductFactory;
import com.petcare.dto.ProductForm;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;
import static org.assertj.core.api.Assertions.*;

class OrderStateTest {
    private static final Map<String, Consumer<Order>> ACTIONS = Map.of(
        "confirm", Order::confirm, "ship", Order::ship, "complete", Order::complete, "cancel", o -> o.cancel());
    private static final Map<OrderStatus, Consumer<Order>> SETUP = Map.of(
        OrderStatus.PENDING, o -> {},
        OrderStatus.CONFIRMED, Order::confirm,
        OrderStatus.SHIPPING, o -> { o.confirm(); o.ship(); },
        OrderStatus.COMPLETED, o -> { o.confirm(); o.ship(); o.complete(); },
        OrderStatus.CANCELLED, o -> o.cancel());
    private static final Map<String, OrderStatus> VALID = Map.of(
        "PENDING:confirm", OrderStatus.CONFIRMED, "PENDING:cancel", OrderStatus.CANCELLED,
        "CONFIRMED:ship", OrderStatus.SHIPPING, "CONFIRMED:cancel", OrderStatus.CANCELLED,
        "SHIPPING:complete", OrderStatus.COMPLETED);

    static Stream<Arguments> transitions() {
        return Arrays.stream(OrderStatus.values()).flatMap(s -> ACTIONS.keySet().stream().map(a -> Arguments.of(s, a)));
    }
    static Order order() {
        var customer = new User("Khách", "test@petcare.vn", "hash", "", Role.CUSTOMER);
        var form = new ProductForm();
        form.setType(ProductType.ACCESSORY);
        form.setName("Bóng");
        form.setBasePrice(new BigDecimal("100.25"));
        form.setStockQuantity(10);
        var product = new ProductFactory().create(form);
        ReflectionTestUtils.setField(product, "id", 1L);
        var cart = new Cart(customer);
        cart.add(product, 2);
        return Order.fromCart(cart, "123 Đường A");
    }

    @ParameterizedTest @MethodSource("transitions")
    void verifiesEveryTransitionAndRejectsInvalidActions(OrderStatus source, String action) {
        var order = order();
        SETUP.get(source).accept(order);
        var expected = VALID.get(source.name() + ":" + action);
        var state = OrderStateFactory.from(source);
        assertThat(state.getAllowedActions().contains(OrderAction.valueOf(action.toUpperCase(Locale.ROOT))))
            .isEqualTo(expected != null);
        assertThat(state.getName()).isEqualTo(source.name());
        assertThat(state.getLabel()).isNotBlank();
        if (expected == null) {
            assertThatThrownBy(() -> ACTIONS.get(action).accept(order))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("Không thể").hasMessageContaining(state.getLabel());
            assertThat(order.getStateName()).isEqualTo(source);
        } else {
            ACTIONS.get(action).accept(order);
            assertThat(order.getStateName()).isEqualTo(expected);
            assertThat(order.getState().getName()).isEqualTo(expected.name());
        }
    }
}
