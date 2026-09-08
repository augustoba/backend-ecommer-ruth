package com.estilospequenos.supplier;

import jakarta.validation.constraints.NotBlank;

public final class SupplierDtos {

    private SupplierDtos() {}

    public record SupplierRequest(
            @NotBlank String name,
            String phone,
            String address,
            String notes
    ) {}

    public record SupplierResponse(
            String id, String name, String phone, String address, String notes
    ) {
        public static SupplierResponse from(Supplier s) {
            return new SupplierResponse(s.getId(), s.getName(), s.getPhone(), s.getAddress(), s.getNotes());
        }
    }
}
