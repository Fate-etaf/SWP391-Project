package com.swp5.library_management.admin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/policies")
public class PolicyController {

    @GetMapping("/general")
    public String generalPolicy() {
        return "policies/general";
    }

    @GetMapping("/intro")
    public String introPolicy() {
        return "policies/intro";
    }

    @GetMapping("/rules")
    public String rulesPolicy() {
        return "policies/rules";
    }

    @GetMapping("/hours")
    public String hoursPolicy() {
        return "policies/hours";
    }
}
