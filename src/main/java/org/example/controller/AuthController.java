package org.example.controller;


import org.example.group.SigninRequest;
import org.example.group.SigninResponse;
import org.example.group.SignupRequest;
import org.example.group.RefreshTokenRequest;
import org.example.group.TokenRefreshResponse;
import org.example.group.UserInfoResponse;
import org.example.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;



@RestController
@CrossOrigin(origins = "http://localhost:8081")
@RequestMapping("/auth")

@RequiredArgsConstructor

public class AuthController {

    @Autowired
    private AuthService authService;



    @PostMapping("/signup")
    public ResponseEntity<String> signUp(@Valid @RequestBody SignupRequest signupRequest){
        try {
            String result = authService.register(signupRequest);
            System.out.println("Register result: " + result);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/signin")
    public ResponseEntity<?> signin(@Valid @RequestBody SigninRequest request){
        try {
            SigninResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            TokenRefreshResponse response = authService.refreshToken(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                String email = authService.getEmailFromToken(token);
                String result = authService.logout(email);
                return ResponseEntity.ok(result);
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing or invalid authorization header");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/user")
    public ResponseEntity<UserInfoResponse> getUserEmail(@RequestHeader(value = "Authorization", required = false) String token) {
        System.out.println("Received Authorization header in /auth/user: " + token);
        if (token != null && token.startsWith("Bearer ")) {

            token = token.substring(7); // Remove "Bearer " prefix
            String email = authService.getEmailFromToken(token);
            System.out.println("Fetched email: " + email); // Log email
            String fullName = authService.getFullNameFromToken(token); // Fetch fullName from token
            System.out.println("Fetched fullName in controller: " + fullName); // Log fullName
            String role = authService.getRoleFromToken(token); // Fetch role from token
            System.out.println("Fetched role in controller: " + role); // Log role
            UserInfoResponse response = new UserInfoResponse(email, fullName, role);
            System.out.println("Response object: " + response); // Log response
            return ResponseEntity.ok(response);

        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
    }
}
