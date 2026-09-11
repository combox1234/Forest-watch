package com.ds.sensor.node.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SensorViewController {

    @GetMapping("/")
    public String index() {
        return "sensor";
    }
}
