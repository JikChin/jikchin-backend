package com.jikchin.jikchinbackend.domain.member.dto.request;

import com.jikchin.jikchinbackend.domain.member.entity.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record SignUpRequest(
    @NotBlank @Email @Size(max = 100) String email,
    @NotBlank @Size(min = 8, max = 72) String password,
    @NotBlank @Size(max = 50) String nickname,
    @Size(max = 500) String profileImageUrl,
    Gender gender,
    @Past LocalDate birthDate,
    @Size(max = 100) String region) {}
