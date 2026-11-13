package com.petcare.domain;

/** Ràng buộc dùng chung để hồ sơ sức khỏe không vượt giới hạn lưu trữ. */
final class PetHealthValidation {
    private PetHealthValidation() {}

    static String required(String value, int max, String label) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " không được trống.");
        return optional(value, max, label);
    }

    static String optional(String value, int max, String label) {
        if (value == null) return "";
        String result = value.strip();
        if (result.length() > max) throw new IllegalArgumentException(label + " tối đa " + max + " ký tự.");
        return result;
    }
}
