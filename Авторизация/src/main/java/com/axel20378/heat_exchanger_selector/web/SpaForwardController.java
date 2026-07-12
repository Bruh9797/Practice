package com.axel20378.heat_exchanger_selector.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Перенаправляет только известные UI-маршруты на собранный React index.html. */
@Controller
public class SpaForwardController {

    @GetMapping({
            "/",
            "/login",
            "/register",
            "/forbidden",
            "/catalog",
            "/compare",
            "/account",
            "/admin",
            "/admin/catalog",
            "/admin/catalog/new",
            "/admin/users"
    })
    public String forwardStaticRoutes() {
        return "forward:/index.html";
    }

    @GetMapping({
            "/heat-exchangers/{slug}",
            "/admin/catalog/{id}"
    })
    public String forwardParameterizedRoutes() {
        return "forward:/index.html";
    }
}
