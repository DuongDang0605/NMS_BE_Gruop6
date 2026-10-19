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
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/overview")
    public Map<String, Object> overview() {
        return dashboardService.overview();
    }

    @GetMapping("/news")
    public Map<String, Object> news() {
        return dashboardService.news();
    }

    @GetMapping("/notifications")
    @PreAuthorize("@recipientAuthorization.canAccess(#recipientId)")
    public Map<String, Object> notifications(@RequestParam Integer recipientId) {
        return dashboardService.notifications(recipientId);
    }
}
