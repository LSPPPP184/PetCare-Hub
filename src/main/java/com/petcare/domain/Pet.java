package com.petcare.domain;
import jakarta.persistence.*;
import lombok.Getter;
import java.math.BigDecimal;
import com.petcare.dto.ProductForm;
/** Giá niêm yết; riêng chăm sóc ngoại hình cộng phụ phí kích thước đã nhập. Không cộng phụ phí thú cưng ở GĐ2. */
@Entity @Table(name="pet_products") @Getter public class Pet extends Product  {
    @OneToOne(mappedBy = "pet", cascade = CascadeType.ALL, orphanRemoval = true)
    private PetProfile profile;

    public void updateProfile(com.petcare.dto.PetProfileForm form) {
        if (profile == null) profile = new PetProfile(this, form);
        else profile.update(form);
    }
    @Enumerated(EnumType.STRING) private Species species;
    private String breed;
    private Integer ageMonths;
    private String gender;
    private String size;
    private String healthNote;
    @Enumerated(EnumType.STRING) private PetStatus status;
    @Override public BigDecimal calculatePrice() {
        return basePrice;
    }
    @Override public java.util.Map<String,String> getDetails() {
        var details=super.getDetails();
        details.put("Loài",species.getLabel());
        details.put("Giống",String.valueOf(breed));
        details.put("Tuổi (tháng)",String.valueOf(ageMonths));
        details.put("Giới tính",String.valueOf(gender));
        details.put("Kích thước",String.valueOf(size));
        details.put("Ghi chú sức khỏe",String.valueOf(healthNote));
        details.put("Trạng thái",status.getLabel());
        return details;
    }
    @Override public String getCategory() {
        return "PET";
    }
    @Override public ProductType getType() {
        return ProductType.PET;
    }
    @Override public void update(ProductForm f) {
        updateCatalog(f);
        status=f.getStatus();
    }
    /** Sửa thông tin thú cưng, giữ nguyên trạng thái do đơn hàng quản lý. */
    @Override public void updateCatalog(ProductForm f) {
        super.update(f);
        species=f.getSpecies();
        breed=f.getBreed();
        ageMonths=f.getAgeMonths();
        gender=f.getGender();
        size=f.getSize();
        healthNote=f.getHealthNote();
        if (f.getProfile() != null) updateProfile(f.getProfile());
        else if (profile == null) updateProfile(new com.petcare.dto.PetProfileForm());
    }
    @Override public ProductForm toForm() {
        var f=super.toForm();
        f.setSpecies(species);
        f.setBreed(breed);
        f.setAgeMonths(ageMonths);
        f.setGender(gender);
        f.setSize(size);
        f.setHealthNote(healthNote);
        f.setStatus(status);
        f.setProfile(profile == null ? new com.petcare.dto.PetProfileForm() : profile.toForm());
        return f;
    }
}
