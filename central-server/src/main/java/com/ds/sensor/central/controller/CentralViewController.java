package com.ds.sensor.central.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CentralViewController {

    @GetMapping("/")
    public String index() {
        return "dashboard";
    }

    @GetMapping("/monitoring")
    public String monitoring() {
        return "monitoring";
    }

    @GetMapping("/sensors")
    public String sensors() {
        return "sensors";
    }

    @GetMapping("/alerts")
    public String alerts() {
        return "alerts";
    }

    @GetMapping("/reports")
    public String reports() {
        return "reports";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "access-denied";
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public String userManagement() {
        return "users";
    }
}
