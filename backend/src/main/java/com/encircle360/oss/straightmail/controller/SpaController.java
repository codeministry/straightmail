package com.encircle360.oss.straightmail.controller;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Spring MVC controller that enables client-side routing for the Angular single-page application.
 *
 * <p>Intercepts all browser navigation requests that do not target the API ({@code /v1}) or static
 * assets (paths containing a dot) and forwards them to {@code /index.html}, allowing Angular's
 * router to handle the URL on the client side.
 */
@Controller
public class SpaController {
    /**
     * Forwards all non-file requests (up to 4 path segments) to the SPA entry point.
     * Path variables are bound to satisfy Spring's template resolution but are not used.
     * File requests (containing a dot) are excluded so that static assets are served directly.
     */
    @RequestMapping(value = {
            "/{path:(?!v1$)[^.]*}",
            "/{path:(?!v1$)[^.]*}/{sub:[^.]*}",
            "/{path:(?!v1$)[^.]*}/{sub:[^.]*}/{sub2:[^.]*}",
            "/{path:(?!v1$)[^.]*}/{sub:[^.]*}/{sub2:[^.]*}/{sub3:[^.]*}"
    })
    @Operation(hidden = true)
    @SuppressWarnings("unused")
    public String forwardToIndex(
            @PathVariable(required = false) String path,
            @PathVariable(required = false) String sub,
            @PathVariable(required = false) String sub2,
            @PathVariable(required = false) String sub3
    ) {
        return "forward:/index.html";
    }
}
