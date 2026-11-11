package com.petcare.domain;
public enum Species  {
    DOG("Chó"), CAT("Mèo"), BIRD("Chim"), FISH("Cá"), OTHER("Khác");
    private final String label;
    Species(String label)  {
        this.label = label;
    }
    public String getLabel()  {
        return label;
    }
}
