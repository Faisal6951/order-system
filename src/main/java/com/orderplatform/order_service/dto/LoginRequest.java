package com.orderplatform.order_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "please provide UserName")
    private String email;

    @NotBlank(message = "please provide password")
    private String password;
}
