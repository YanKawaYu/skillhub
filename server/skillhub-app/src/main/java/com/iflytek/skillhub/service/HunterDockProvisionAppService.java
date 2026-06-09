package com.iflytek.skillhub.service;

import com.iflytek.skillhub.auth.identity.IdentityBindingService;
import com.iflytek.skillhub.auth.oauth.OAuthClaims;
import com.iflytek.skillhub.auth.token.ApiTokenService;
import com.iflytek.skillhub.domain.user.UserStatus;
import com.iflytek.skillhub.dto.HunterDockProvisionRequest;
import com.iflytek.skillhub.dto.HunterDockProvisionResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;

@Service
public class HunterDockProvisionAppService {

    private static final String DEFAULT_TOKEN_NAME = "HunterDock";
    private static final String DEFAULT_TOKEN_SCOPES = "[\"skill:read\",\"skill:publish\"]";

    private final IdentityBindingService identityBindingService;
    private final ApiTokenService apiTokenService;

    public HunterDockProvisionAppService(IdentityBindingService identityBindingService,
                                         ApiTokenService apiTokenService) {
        this.identityBindingService = identityBindingService;
        this.apiTokenService = apiTokenService;
    }

    public HunterDockProvisionResponse provision(HunterDockProvisionRequest request) {
        String provider = requiredText(request.provider(), "provider");
        String subject = requiredText(request.subject(), "subject");
        String displayName = optionalText(request.displayName(), subject);
        String email = optionalText(request.email(), null);
        String avatarUrl = optionalText(request.avatarUrl(), null);
        String tokenName = optionalText(request.tokenName(), DEFAULT_TOKEN_NAME);

        Map<String, Object> extra = avatarUrl == null ? Map.of() : Map.of("avatar_url", avatarUrl);
        var principal = identityBindingService.bindOrCreate(
                new OAuthClaims(provider, subject, email, email != null, displayName, extra),
                UserStatus.ACTIVE
        );
        var token = apiTokenService.rotateToken(principal.userId(), tokenName, DEFAULT_TOKEN_SCOPES);
        return new HunterDockProvisionResponse(
                principal.userId(),
                token.rawToken(),
                token.entity().getId(),
                token.entity().getName(),
                token.entity().getTokenPrefix()
        );
    }

    private String requiredText(String value, String field) {
        String text = optionalText(value, null);
        if (text == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return text;
    }

    private String optionalText(String value, String fallback) {
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        return value.trim();
    }
}
