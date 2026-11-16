package com.petcare.dto;

public enum HomeSize {
    SMALL("Nhỏ", 2), MEDIUM("Vừa", 3), LARGE("Lớn", 5);
    private final String label;
    private final int capacity;
    HomeSize(String label, int capacity) { this.label = label; this.capacity = capacity; }
    public String getLabel() { return label; }
    public int getCapacity() { return capacity; }
}
