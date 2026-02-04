package org.example.controller;

import org.example.group.DashboardAnalytics;
import org.example.service.AnalyticsService;
import org.example.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = {
        "http://localhost:3001",
        "https://1e27-2405-201-5803-9887-f09f-e037-ca69-f5e6.ngrok-free.app"
})

@RequestMapping("/api/analytics")
public class AnalyticsController {

    @Autowired
    private AnalyticsService analyticsService;
    
    @Autowired
    private AuthService authService;

    // Get dashboard analytics
    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                String email = authService.getEmailFromToken(token);
                
                // Get dashboard analytics
                DashboardAnalytics analytics = analyticsService.getDashboardAnalytics();
                return ResponseEntity.ok(analytics);
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Missing or invalid authorization header");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
