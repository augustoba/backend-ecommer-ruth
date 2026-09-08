package com.estilospequenos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** DTOs para recuperación y gestión de la cuenta del admin. */
public final class AccountDtos {

    private AccountDtos() {}

    /** POST /api/auth/recover — público. */
    public record RecoverRequest(
            @NotBlank String username,
            @NotBlank String recoveryPhrase,
            @NotBlank @Size(min = 4) String newPassword
    ) {}

    /** PUT /api/admin/account/password */
    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 4) String newPassword
    ) {}

    /** PUT /api/admin/account/recovery */
    public record ChangeRecoveryRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 4) String recoveryPhrase
    ) {}

    /** GET /api/admin/account */
    public record AccountResponse(String username, boolean hasRecoveryPhrase) {}
}
