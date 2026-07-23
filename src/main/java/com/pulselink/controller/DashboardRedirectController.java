package com.pulselink.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.Set;

@Controller
public class DashboardRedirectController {

    @GetMapping("/dashboard")
    public String dashboardRedirect(Authentication authentication) {
        if (authentication == null) {
            return "redirect:/login";
        }
        Set<String> roles = AuthorityUtils.authorityListToSet(authentication.getAuthorities());
        if (roles.contains("ROLE_ADMIN")) {
            return "redirect:/admin/dashboard";
        } else if (roles.contains("ROLE_DONOR")) {
            return "redirect:/donor/dashboard";
        } else if (roles.contains("ROLE_PATIENT")) {
            return "redirect:/patient/dashboard";
        }
        return "redirect:/login";
    }
}
