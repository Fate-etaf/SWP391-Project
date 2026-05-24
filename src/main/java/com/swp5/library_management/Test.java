package com.swp5.library_management;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class Test {

    @GetMapping("/test")
    public String test(Model model) {

        model.addAttribute("name", "Phú");
        model.addAttribute("age", 18);

        return "test";
    }
}