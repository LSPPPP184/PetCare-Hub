package com.petcare.dto;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Embeddable @Getter @Setter
public class QuizAnswers {
    @NotNull(message = "Vui lòng chọn diện tích nhà.")
    @Enumerated(EnumType.STRING) @Column(length = 20) private HomeSize homeSize;
    @NotNull(message = "Vui lòng cho biết nhà có trẻ em hay không.") private Boolean hasChildren;
    @NotNull(message = "Vui lòng nhập mức vận động.") @Min(value = 1, message = "Mức lựa chọn phải từ 1 đến 5.") @Max(value = 5, message = "Mức lựa chọn phải từ 1 đến 5.") private Integer activityLevel;
    @NotNull(message = "Vui lòng nhập thời gian rảnh.") @DecimalMin(value = "0", message = "Thời gian phải từ 0 đến 24 giờ.") @DecimalMax(value = "24", message = "Thời gian không quá 24 giờ.")
    private Double freeHoursPerDay;
    @NotNull(message = "Vui lòng nhập ngân sách.") @Min(value = 1, message = "Mức lựa chọn phải từ 1 đến 5.") @Max(value = 5, message = "Mức lựa chọn phải từ 1 đến 5.") private Integer budgetLevel;
    @NotNull(message = "Vui lòng cho biết tình trạng dị ứng lông.") private Boolean furAllergy;
    @NotNull(message = "Vui lòng nhập kinh nghiệm nuôi.") @Min(value = 1, message = "Mức lựa chọn phải từ 1 đến 5.") @Max(value = 5, message = "Mức lựa chọn phải từ 1 đến 5.") private Integer experienceLevel;

    public QuizAnswers copy() {
        var a = new QuizAnswers();
        a.homeSize = homeSize; a.hasChildren = hasChildren; a.activityLevel = activityLevel;
        a.freeHoursPerDay = freeHoursPerDay; a.budgetLevel = budgetLevel;
        a.furAllergy = furAllergy; a.experienceLevel = experienceLevel;
        return a;
    }
}
