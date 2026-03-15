package com.akku.backend.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank(message = "role은 필수입니다.")
        @Pattern(regexp = "^(PARENT|CHILD)$", message = "role은 PARENT 또는 CHILD여야 합니다.")
        String role,

        @NotBlank(message = "이름은 필수입니다.")
        @Size(max = 50, message = "이름은 50자 이내여야 합니다.")
        String name
) {}
