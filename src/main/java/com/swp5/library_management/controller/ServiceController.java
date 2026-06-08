package com.swp5.library_management.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/services")
public class ServiceController {

    @GetMapping("/borrowing")
    public String borrowingService() {
        return "services/borrowing";
    }

    @GetMapping("/renewal")
    public String renewalService() {
        return "services/renewal";
    }

    @GetMapping("/group-study")
    public String groupStudyService() {
        return "services/group-study";
    }
}
