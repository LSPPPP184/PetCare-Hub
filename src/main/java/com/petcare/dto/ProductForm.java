package com.petcare.dto;
import com.petcare.domain.*;
import java.math.BigDecimal;
import jakarta.validation.constraints.*;
import lombok.*;
@Getter @Setter public class ProductForm  {
    // Form cũ không gửi hồ sơ: giữ nguyên hồ sơ hiện có, không ghi đè bằng giá trị mặc định.
    @jakarta.validation.Valid private PetProfileForm profile;
    private Long version;
    private static final java.util.Map<ProductType, java.util.function.Predicate<ProductForm>> RULES = java.util.Map.of( ProductType.PET, f -> true, ProductType.ACCESSORY, f -> true, ProductType.GROOMING, f -> positive(f.durationMinutes), ProductType.VET, f -> positive(f.durationMinutes) && f.doctorName != null && !f.doctorName.isBlank(), ProductType.MONTHLY, f -> positive(f.durationMinutes) && positive(f.durationMonths) && positive(f.includedUses) && f.includedServices != null && !f.includedServices.isBlank());
    private static boolean positive(Integer value)  {
        return value != null && value > 0;
    }
    public static boolean isAllowedImageUrl(String value) {
        if (value == null || value.isEmpty()) return true;
        try {
            var uri = new java.net.URI(value);
            return ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                && uri.getHost() != null && uri.getUserInfo() == null;
        } catch (java.net.URISyntaxException ex) {
            return false;
        }
    }

    @AssertTrue(message = "Đường dẫn ảnh phải là URL HTTP hoặc HTTPS hợp lệ, hoặc để trống.")
    public boolean isImageUrlValid() {
        return isAllowedImageUrl(imageUrl);
    }
    @AssertTrue(message="Dịch vụ cần thời lượng dương; thú y cần bác sĩ; gói tháng cần số tháng, số lượt dương và mô tả dịch vụ.") public boolean isTypeValid()  {
        return type == null || RULES.get(type).test(this);
    }
    @NotNull(message="Vui lòng chọn loại.") private ProductType type=ProductType.PET;
    @Size(max=255,message="Nội dung quá dài.") @NotBlank(message="Vui lòng nhập tên.") private String name="";
    @Size(max=2000,message="Nội dung quá dài.") private String description="";
    @NotNull(message="Vui lòng nhập giá.") @DecimalMin(value="0",message="Giá không được âm.") @Digits(integer=17,fraction=2,message="Giá tối đa 17 chữ số và 2 số lẻ.") private BigDecimal basePrice=BigDecimal.ZERO;
    @Size(max=255,message="Nội dung quá dài.") private String imageUrl="";
    @NotNull(message="Vui lòng nhập số lượng hoặc thời lượng.") @Min(value=0,message="Giá trị không được âm.") private Integer durationMinutes=0;
    @NotNull(message="Vui lòng chọn giá trị.") private Species species=Species.OTHER;
    @Size(max=255,message="Nội dung quá dài.") private String breed="";
    @NotNull(message="Vui lòng nhập số lượng hoặc thời lượng.") @Min(value=0,message="Giá trị không được âm.") private Integer ageMonths=0;
    @Size(max=255,message="Nội dung quá dài.") private String gender="";
    @Size(max=255,message="Nội dung quá dài.") private String size="";
    @Size(max=255,message="Nội dung quá dài.") private String healthNote="";
    @NotNull(message="Vui lòng chọn giá trị.") private PetStatus status=PetStatus.AVAILABLE;
    @Size(max=255,message="Nội dung quá dài.") private String brand="";
    @NotNull(message="Vui lòng nhập số lượng hoặc thời lượng.") @Min(value=0,message="Giá trị không được âm.") private Integer stockQuantity=0;
    @Size(max=255,message="Nội dung quá dài.") private String category="";
    @NotNull(message="Vui lòng nhập giá.") @DecimalMin(value="0",message="Giá không được âm.") @Digits(integer=17,fraction=2,message="Giá tối đa 17 chữ số và 2 số lẻ.") private BigDecimal petSizeSurcharge=BigDecimal.ZERO;
    @Size(max=255,message="Nội dung quá dài.") private String doctorName="";
    @NotNull(message="Vui lòng nhập số lượng hoặc thời lượng.") @Min(value=0,message="Giá trị không được âm.") private Integer durationMonths=0;
    @NotNull(message="Vui lòng nhập số lượng hoặc thời lượng.") @Min(value=0,message="Giá trị không được âm.") private Integer includedUses=0;
    @Size(max=255,message="Nội dung quá dài.") private String includedServices="";
}
