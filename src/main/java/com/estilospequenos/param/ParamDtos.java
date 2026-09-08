package com.estilospequenos.param;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

/** DTOs de entrada/salida para parametrías. */
public final class ParamDtos {

    private ParamDtos() {}

    public record GroupRequest(
            @NotBlank String name,
            boolean multiple,
            Boolean showInCatalog
    ) {}

    public record OptionRequest(@NotBlank String label) {}

    public record OptionResponse(String id, String label) {}

    public record GroupResponse(
            String id,
            String name,
            boolean multiple,
            boolean showInCatalog,
            boolean system,
            List<OptionResponse> options
    ) {
        public static GroupResponse from(ParamGroup g) {
            return new GroupResponse(
                    g.getId(), g.getName(), g.isMultiple(), g.isShowInCatalog(), g.isSystem(),
                    g.getOptions().stream()
                            .map(o -> new OptionResponse(o.getId(), o.getLabel()))
                            .toList()
            );
        }
    }
}
