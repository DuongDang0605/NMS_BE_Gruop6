package com.mvc.nms_be.controller;

import com.mvc.nms_be.service.DashboardService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'CONTRIBUTOR', 'EDITOR', 'MODERATOR')")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/overview")
    public Map<String, Object> overview() {
        return dashboardService.overview();
    }

    @GetMapping("/news")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public Map<String, Object> news() {
        return dashboardService.news();
    }

    @GetMapping("/notifications")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTRIBUTOR', 'EDITOR', 'MODERATOR') and @recipientAuthorization.canAccess(#recipientId)")
    public Map<String, Object> notifications(@RequestParam Integer recipientId) {
        return dashboardService.notifications(recipientId);
    }

    @GetMapping("/engagement")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTRIBUTOR', 'EDITOR')")
    public Map<String, Object> engagement() {
        return dashboardService.engagement();
    }
}
