package com.iflytek.skillhub.dto;

public record HunterDockProvisionResponse(
        String userId,
        String token,
        Long tokenId,
        String tokenName,
        String tokenPrefix
) {}
