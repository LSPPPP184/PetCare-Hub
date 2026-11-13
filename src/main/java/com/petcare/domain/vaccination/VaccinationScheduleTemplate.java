package com.petcare.domain.vaccination;

import com.petcare.domain.OwnedPet;
import com.petcare.domain.Species;
import com.petcare.domain.VaccinationRecord;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Lịch theo loài dùng registry dữ liệu: dữ liệu mẫu, không phải tư vấn thú y.
 * Chó/mèo có các mốc 6, 8, 12 tuần; nhắc hằng năm tính từ mốc 12 tuần.
 * Giả định khi đăng ký muộn: bỏ các mũi cơ bản đã qua, chỉ giữ mũi nhắc hằng năm
 * gần nhất đã đến hạn (nếu có, chưa biết đã tiêm) và N mũi nhắc tương lai theo cấu hình.
 * Mũi đã qua ngày hẹn là quá hạn; đúng ngày hẹn vẫn sắp đến hạn. Không suy diễn đã tiêm.
 * Thú cưng mới sinh vẫn có đủ 6, 8, 12 tuần và N mũi nhắc như trước.
 */
public final class VaccinationScheduleTemplate {
    private static final Map<Species, String> VACCINES = Map.of(
        Species.DOG, "Vaccine tổng hợp cho chó (mẫu)", Species.CAT, "Vaccine tổng hợp cho mèo (mẫu)");
    private static final List<Integer> INITIAL_WEEKS = List.of(6, 8, 12);
    private static final String NOTE = "Dữ liệu mẫu, không phải tư vấn thú y.";
    private final int annualBoosterYears;
    private final Clock clock;

    public VaccinationScheduleTemplate(int annualBoosterYears, Clock clock) {
        if (annualBoosterYears < 1 || annualBoosterYears > 20)
            throw new IllegalArgumentException("Số năm nhắc lại phải từ 1 đến 20.");
        this.annualBoosterYears = annualBoosterYears;
        this.clock = Objects.requireNonNull(clock, "Đồng hồ không được trống.");
    }

    public List<VaccinationRecord> generate(OwnedPet pet, int dueSoonDays) {
        LocalDate today = LocalDate.now(clock);
        String vaccine = VACCINES.get(pet.getSpecies());
        if (vaccine == null) return List.of();
        var records = new ArrayList<VaccinationRecord>();
        for (int week : INITIAL_WEEKS) {
            LocalDate date = pet.getBirthDate().plusWeeks(week);
            if (!date.isBefore(today)) {
                records.add(VaccinationRecord.schedule(pet, vaccine, date, NOTE, today, dueSoonDays));
            }
        }
        LocalDate lastInitialDose = pet.getBirthDate().plusWeeks(INITIAL_WEEKS.getLast());
        // So sánh với mốc gốc để ngày 29/02 không bị trôi sau năm không nhuận.
        int latestDueYear = Math.max(0, today.getYear() - lastInitialDose.getYear());
        if (latestDueYear > 0 && lastInitialDose.plusYears(latestDueYear).isAfter(today)) latestDueYear--;
        if (latestDueYear > 0) {
            records.add(VaccinationRecord.annualBooster(pet, vaccine + " — nhắc lại", latestDueYear,
                NOTE, today, dueSoonDays));
        }
        for (int year = latestDueYear + 1; year <= latestDueYear + annualBoosterYears; year++) {
            records.add(VaccinationRecord.annualBooster(pet, vaccine + " — nhắc lại", year,
                NOTE, today, dueSoonDays));
        }
        return List.copyOf(records);
    }
}
