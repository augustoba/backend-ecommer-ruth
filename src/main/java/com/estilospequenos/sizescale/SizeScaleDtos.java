package com.estilospequenos.sizescale;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public final class SizeScaleDtos {

    private SizeScaleDtos() {}

    public record ScaleRequest(@NotBlank String name, List<String> values) {}

    public record ValuesRequest(@NotNull List<String> values) {}

    public record ScaleResponse(String id, String name, boolean system, List<String> values) {
        public static ScaleResponse from(SizeScale s) {
            return new ScaleResponse(s.getId(), s.getName(), s.isSystem(), List.copyOf(s.getValues()));
        }
    }
}
