package com.lks.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.lks.bean.User;
import com.lks.service.CloudflareAnalyticsService;

import jakarta.servlet.http.HttpSession;

@Controller
public class CloudflareAnalyticsController {
    private final CloudflareAnalyticsService cloudflareAnalyticsService;

    public CloudflareAnalyticsController(CloudflareAnalyticsService cloudflareAnalyticsService) {
        this.cloudflareAnalyticsService = cloudflareAnalyticsService;
    }

    @GetMapping("/admin/analytics")
    public String analytics(HttpSession session, Model model) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !"ADMIN".equalsIgnoreCase(loggedInUser.getRole())) {
            return "redirect:/";
        }

        model.addAttribute("title", "Traffic Analytics");
        model.addAttribute("summary", cloudflareAnalyticsService.getSummary());
        return "cloudflare-analytics";
    }
}
