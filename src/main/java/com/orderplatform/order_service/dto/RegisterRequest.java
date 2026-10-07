package com.orderplatform.order_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "please provide name")
    private String name;

    @NotBlank(message = "please provide email")
    private String email;

    @NotBlank(message = "please provide password")
    private String password;

}
