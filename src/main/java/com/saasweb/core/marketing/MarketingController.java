package com.saasweb.core.marketing;

import com.saasweb.core.marketing.MarketingDtos.MarketingConfigRequest;
import com.saasweb.core.marketing.MarketingDtos.MarketingConfigResponse;
import com.saasweb.core.marketing.MarketingDtos.MarketingSendResponse;
import com.saasweb.core.marketing.MarketingDtos.PreviewResult;
import com.saasweb.core.marketing.MarketingDtos.RunResult;
import com.saasweb.common.TenantContext;
import com.saasweb.core.PageResponse;
import com.saasweb.core.marketing.MarketingSend;
import com.saasweb.core.marketing.MarketingSendRepository;
import com.saasweb.core.marketing.MarketingCampaignService;
import com.saasweb.core.marketing.MarketingConfigService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;

@RestController
@RequestMapping("/api/admin/marketing")
public class MarketingController {

    private final MarketingConfigService configService;
    private final MarketingCampaignService campaignService;
    private final MarketingSendRepository sendRepo;

    public MarketingController(MarketingConfigService configService, MarketingCampaignService campaignService,
                                MarketingSendRepository sendRepo) {
        this.configService = configService;
        this.campaignService = campaignService;
        this.sendRepo = sendRepo;
    }

    @GetMapping("/config")
    @PreAuthorize("hasAuthority('MARKETING_MANAGE')")
    public MarketingConfigResponse getConfig() {
        return MarketingConfigResponse.from(configService.get());
    }

    @PutMapping("/config")
    @PreAuthorize("hasAuthority('MARKETING_MANAGE')")
    public MarketingConfigResponse updateConfig(@Valid @RequestBody MarketingConfigRequest req) {
        return MarketingConfigResponse.from(configService.update(req));
    }

    @GetMapping("/preview")
    @PreAuthorize("hasAuthority('MARKETING_MANAGE')")
    public PreviewResult preview() {
        return campaignService.previewToday();
    }

    @PostMapping("/run-now")
    @PreAuthorize("hasAuthority('MARKETING_MANAGE')")
    public RunResult runNow() {
        return campaignService.runNow();
    }

    @GetMapping("/history")
    @PreAuthorize("hasAuthority('MARKETING_MANAGE')")
    public PageResponse<MarketingSendResponse> history(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) MarketingSend.Reason reason,
            @RequestParam(required = false) MarketingSend.Status status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        int capped = Math.min(Math.max(size, 1), 100);
        ZoneId zone = ZoneId.systemDefault();
        var fromI = from != null ? from.atStartOfDay(zone).toInstant() : null;
        var toI = to != null ? to.plusDays(1).atStartOfDay(zone).toInstant() : null;
        Page<MarketingSend> result = sendRepo.search(TenantContext.getTenantId(), reason, status, fromI, toI,
                PageRequest.of(Math.max(page, 0), capped));
        return PageResponse.of(result, result.map(MarketingSendResponse::from).getContent());
    }
}
