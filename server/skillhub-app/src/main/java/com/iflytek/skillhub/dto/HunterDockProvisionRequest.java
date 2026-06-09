package com.iflytek.skillhub.dto;

/**
 * Internal request used by HunterDockServer to provision the same SkillHub account as OIDC login.
 */
public record HunterDockProvisionRequest(
        String provider,
        String subject,
        String displayName,
        String email,
        String avatarUrl,
        String tokenName
) {}
