package com.saasweb.core.page;

import jakarta.validation.constraints.NotNull;

public final class PageBlockDtos {

    private PageBlockDtos() {}

    public record VisibleRequest(@NotNull Boolean visible) {}

    public record PageBlockResponse(String id, String pageType, String blockType, int position, boolean visible) {
        public static PageBlockResponse from(PageBlock b) {
            return new PageBlockResponse(b.getId(), b.getPageType(), b.getBlockType(), b.getPosition(), b.isVisible());
        }
    }
}
