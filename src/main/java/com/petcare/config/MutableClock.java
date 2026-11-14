package com.petcare.config;

import java.time.*;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/** Clock sức khỏe demo; các múi giờ chia sẻ thời điểm đã xác nhận và lần giả lập đang chạy. */
public final class MutableClock extends Clock {
    private final ZoneId zone;
    private final State state;

    private static final class State {
        private final Clock realClock;
        private final AtomicReference<Instant> committed;
        private final ThreadLocal<Instant> pending = new ThreadLocal<>();
        private State(Clock realClock) {
            this.realClock = realClock;
            this.committed = new AtomicReference<>(realClock.instant());
        }
    }

    public MutableClock(Clock realClock) { this(realClock.getZone(), new State(realClock)); }
    private MutableClock(ZoneId zone, State state) { this.zone = zone; this.state = state; }
    @Override public ZoneId getZone() { return zone; }
    @Override public Clock withZone(ZoneId zone) { return new MutableClock(Objects.requireNonNull(zone), state); }
    @Override public Instant instant() {
        var pending = state.pending.get();
        return pending == null ? state.committed.get() : pending;
    }

    public void advanceTo(LocalDate date) { advanceAndRun(date, () -> {}); }

    /**
     * Chỉ công bố ngày mới sau khi tác vụ (gồm commit DB) thành công. Nếu lỗi, bỏ ngày tạm
     * và giữ nguyên thời điểm cũ. Luồng khác không thấy ngày chưa commit, cũng không bị
     * chặn đọc Clock khi đang giữ khóa DB. Các lần giả lập dùng chung một khóa điều phối.
     */
    public void advanceAndRun(LocalDate date, Runnable task) {
        synchronized (state) {
            var next = nextInstant(date);
            state.pending.set(next);
            try {
                task.run();
                state.committed.set(next);
            } finally {
                state.pending.remove();
            }
        }
    }

    private Instant nextInstant(LocalDate date) {
        var previous = state.committed.get();
        var currentDate = previous.atZone(zone).toLocalDate();
        if (date == null || date.isBefore(currentDate))
            throw new IllegalArgumentException("Ngày giả lập phải từ ngày hiện tại trở đi.");
        var realNow = state.realClock.instant();
        if (date.equals(realNow.atZone(zone).toLocalDate()))
            return realNow.isBefore(previous) ? previous : realNow;
        if (date.equals(currentDate)) return previous;
        return date.atStartOfDay(zone).toInstant();
    }
}
