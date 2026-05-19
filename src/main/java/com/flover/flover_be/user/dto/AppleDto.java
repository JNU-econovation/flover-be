package com.flover.flover_be.user.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public class AppleDto {

    public record LoginRequest(@NotBlank String identityToken, String name) {}

    public record JwksResponse(List<JwksKey> keys) {}

    public record JwksKey(String kty, String kid, String use, String alg, String n, String e) {}
}
