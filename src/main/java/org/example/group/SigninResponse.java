package org.example.group;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SigninResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";

}
