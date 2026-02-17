package org.example.group;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SigninResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
}
