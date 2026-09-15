package com.saasweb.core.page;

import com.saasweb.core.page.PageBlockDtos.PageBlockResponse;
import com.saasweb.core.page.PageBlockDtos.VisibleRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class PageBlockController {

    private final PageBlockService service;

    public PageBlockController(PageBlockService service) {
        this.service = service;
    }

    /** Bloques visibles de una página, en orden — consumido por el sitio público. */
    @GetMapping("/api/page-blocks")
    public List<PageBlockResponse> publicList(@RequestParam(defaultValue = PageBlockService.HOME) String pageType) {
        return service.findVisible(pageType).stream().map(PageBlockResponse::from).toList();
    }

    @GetMapping("/api/admin/page-blocks")
    @PreAuthorize("hasAuthority('CAROUSEL_MANAGE')")
    public List<PageBlockResponse> list(@RequestParam(defaultValue = PageBlockService.HOME) String pageType) {
        return service.findAll(pageType).stream().map(PageBlockResponse::from).toList();
    }

    @PutMapping("/api/admin/page-blocks/{id}/visible")
    @PreAuthorize("hasAuthority('CAROUSEL_MANAGE')")
    public PageBlockResponse setVisible(@PathVariable String id, @Valid @RequestBody VisibleRequest req) {
        return PageBlockResponse.from(service.setVisible(id, req.visible()));
    }
}
