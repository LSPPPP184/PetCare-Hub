package com.petcare.domain;
public enum PetStatus  {
    AVAILABLE("Sẵn sàng"), RESERVED("Đã giữ chỗ"), SOLD("Đã bán");
    private final String label;
    PetStatus(String label)  {
        this.label = label;
    }
    public String getLabel()  {
        return label;
    }
}
