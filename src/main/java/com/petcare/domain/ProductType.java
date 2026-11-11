package com.petcare.domain;
public enum ProductType  {
    PET("Thú cưng"), ACCESSORY("Phụ kiện"), GROOMING("Tắm tỉa"), VET("Thú y"), MONTHLY("Gói tháng");
    private final String label;
    ProductType(String label)  {
        this.label = label;
    }
    public String getLabel()  {
        return label;
    }
}
