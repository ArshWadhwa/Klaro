package org.example.service;

import org.example.entity.RefreshToken;
import org.example.entity.User;
import org.example.group.SigninRequest;
import org.example.group.SigninResponse;
import org.example.group.SignupRequest;
import org.example.group.RefreshTokenRequest;
import org.example.group.TokenRefreshResponse;
import org.example.security.JwtUtil;
import org.example.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RefreshTokenService refreshTokenService;


    public String register(SignupRequest signupRequest){
        System.out.println("REGISTER METHOD HIT");

        if(userRepository.existsByEmail(signupRequest.getEmail())){
            System.out.println("EMAIL ALREADY EXISTS");
            return "Email already registered";
        }

        if(!signupRequest.getPassword().equals(signupRequest.getConfirmPassword())){
            System.out.println("PASSWORD MISMATCH");
            return "Passwords don't match";
        }

        User user = new User();
        user.setFullName(signupRequest.getFullName());
        user.setEmail(signupRequest.getEmail());
        user.setPassword(passwordEncoder.encode(signupRequest.getPassword()));
        user.setRole(signupRequest.getRole()); // Set the role from request

        System.out.println("Saving user to DB: " + user.getEmail() + " with role: " + user.getRole());
        userRepository.save(user);

        return "User registered successfully!";
    }
    public SigninResponse login(SigninRequest request){
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(()-> new RuntimeException("User Not found"));

        if(!passwordEncoder.matches(request.getPassword(),user.getPassword())){
            throw new RuntimeException("Invalid credentials");
        }

        String accessToken = jwtUtil.generateToken(user.getEmail(), user.getFullName(), user.getRole().toString());
        String refreshToken = refreshTokenService.createRefreshToken(user.getEmail()).getToken();
        
        return new SigninResponse(accessToken, refreshToken, "Bearer");
    }

    public TokenRefreshResponse refreshToken(RefreshTokenRequest request) {
        String requestRefreshToken = request.getRefreshToken();

        return refreshTokenService.findByToken(requestRefreshToken)
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    String token = jwtUtil.generateToken(user.getEmail(), user.getFullName(), user.getRole().toString());
                    return new TokenRefreshResponse(token, requestRefreshToken, "Bearer");
                })
                .orElseThrow(() -> new RuntimeException("Refresh token is not in database!"));
    }

    public String logout(String userEmail) {
        refreshTokenService.deleteByUser(userEmail);
        return "Logout successful";
    }


    public String getEmailFromToken(String token) {
        return jwtUtil.extractEmail(token);
    }


    public String getFullName(String email){
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User Not found"+email));
        return user.getFullName();

    }

    // New method to fetch fullName from token
    public String getFullNameFromToken(String token) {
        String fullName = jwtUtil.extractFullName(token);
        System.out.println("Fetched fullName from token: " + fullName); // Add logging
        return fullName;
//        return jwtUtil.extractFullName(token);
    }

    // New method to fetch role from token
    public String getRoleFromToken(String token) {
        return jwtUtil.extractRole(token);
    }

    // New method to check if user is admin
    public boolean isAdmin(String token) {
        String role = getRoleFromToken(token);
        return "ROLE_ADMIN".equals(role);
    }

    // New method to check if user is admin by email
    public boolean isAdminByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
        return user.getRole().toString().equals("ROLE_ADMIN");
    }
}
