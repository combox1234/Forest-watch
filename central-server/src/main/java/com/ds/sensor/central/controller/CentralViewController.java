package com.ds.sensor.central.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CentralViewController {

    @GetMapping("/")
    public String index() {
        return "dashboard";
    }
}
