package com.iflytek.skillhub.controller;

import com.iflytek.skillhub.dto.ApiResponse;
import com.iflytek.skillhub.dto.ApiResponseFactory;
import com.iflytek.skillhub.dto.HunterDockProvisionRequest;
import com.iflytek.skillhub.dto.HunterDockProvisionResponse;
import com.iflytek.skillhub.service.HunterDockProvisionAppService;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Hidden
@RestController
@RequestMapping("/api/internal/hunterdock")
public class InternalHunterDockProvisionController {

    private static final String TOKEN_HEADER = "X-HunterDock-Token";

    private final HunterDockProvisionAppService provisionAppService;
    private final ApiResponseFactory responseFactory;
    private final String provisionSecret;

    public InternalHunterDockProvisionController(HunterDockProvisionAppService provisionAppService,
                                                ApiResponseFactory responseFactory,
                                                @Value("${skillhub.integration.hunterdock.provision-secret:}") String provisionSecret) {
        this.provisionAppService = provisionAppService;
        this.responseFactory = responseFactory;
        this.provisionSecret = provisionSecret == null ? "" : provisionSecret.trim();
    }

    @PostMapping("/provision")
    public ResponseEntity<ApiResponse<?>> provision(
            @RequestHeader(value = TOKEN_HEADER, required = false) String token,
            @RequestBody HunterDockProvisionRequest request) {
        if (!isAuthorized(token)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(responseFactory.error(403, "error.forbidden"));
        }
        return ResponseEntity.ok(responseFactory.ok("response.success.created", provisionAppService.provision(request)));
    }

    private boolean isAuthorized(String token) {
        if (!StringUtils.hasText(provisionSecret) || !StringUtils.hasText(token)) {
            return false;
        }
        byte[] expected = provisionSecret.getBytes(StandardCharsets.UTF_8);
        byte[] actual = token.trim().getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, actual);
    }
}
