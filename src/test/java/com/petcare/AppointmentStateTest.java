package com.petcare;

import com.petcare.domain.*;
import com.petcare.domain.appointment.*;
import com.petcare.domain.factory.ProductFactory;
import com.petcare.domain.order.InvalidStateTransitionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;
import static org.assertj.core.api.Assertions.*;

class AppointmentStateTest {
    private static final Map<String, Consumer<Appointment>> ACTIONS = Map.of("book", Appointment::book,
        "start", Appointment::start, "finish", Appointment::finish, "cancel", Appointment::cancel);
    private static final Map<AppointmentStatus, Consumer<Appointment>> SETUP = Map.of(
        AppointmentStatus.BOOKED, a -> {}, AppointmentStatus.IN_PROGRESS, Appointment::start,
        AppointmentStatus.DONE, a -> { a.start(); a.finish(); }, AppointmentStatus.CANCELLED, Appointment::cancel);
    private static final Map<String, AppointmentStatus> VALID = Map.of("BOOKED:start", AppointmentStatus.IN_PROGRESS,
        "BOOKED:cancel", AppointmentStatus.CANCELLED, "IN_PROGRESS:finish", AppointmentStatus.DONE);
    static Appointment appointment() {
        var form = ProductFactoryTest.form(ProductType.VET, "Khám", "100.00");
        form.setDurationMinutes(45);
        return Appointment.create(new User("Khách", "customer@test.vn", "hash", "", Role.CUSTOMER), "Milo", Species.CAT,
            (ServiceItem) new ProductFactory().create(form), new User("Nhân viên", "staff@test.vn", "hash", "", Role.STAFF),
            LocalDateTime.parse("2026-01-05T10:00"), "Ghi chú");
    }
    static Stream<Arguments> transitions() {
        return Arrays.stream(AppointmentStatus.values()).flatMap(s -> ACTIONS.keySet().stream().map(a -> Arguments.of(s, a)));
    }
    @Test void initialBookingSnapshotsPriceAndDuration() {
        var a = appointment();
        assertThat(a.getStateName()).isEqualTo(AppointmentStatus.BOOKED);
        assertThat(a.getEndTime()).isEqualTo(LocalDateTime.parse("2026-01-05T10:45"));
        assertThat(a.getPrice()).isEqualByComparingTo("100.00");
        assertThat(a.getState()).isInstanceOf(BookedState.class);
    }
    @ParameterizedTest @MethodSource("transitions")
    void testsWholeTransitionTable(AppointmentStatus source, String action) {
        var a = appointment(); SETUP.get(source).accept(a);
        var expected = VALID.get(source.name() + ":" + action);
        assertThat(a.getState().getName()).isEqualTo(source.name());
        if (!action.equals("book")) assertThat(a.getState().getAllowedActions()
            .contains(AppointmentAction.valueOf(action.toUpperCase(Locale.ROOT)))).isEqualTo(expected != null);
        if (expected == null) {
            assertThatThrownBy(() -> ACTIONS.get(action).accept(a)).isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("Không thể").hasMessageContaining(a.getState().getLabel());
            assertThat(a.getStateName()).isEqualTo(source);
        } else { ACTIONS.get(action).accept(a); assertThat(a.getStateName()).isEqualTo(expected); }
    }
}
