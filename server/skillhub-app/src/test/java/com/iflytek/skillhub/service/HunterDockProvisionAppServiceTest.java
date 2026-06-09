package com.iflytek.skillhub.service;

import com.iflytek.skillhub.auth.entity.ApiToken;
import com.iflytek.skillhub.auth.identity.IdentityBindingService;
import com.iflytek.skillhub.auth.oauth.OAuthClaims;
import com.iflytek.skillhub.auth.rbac.PlatformPrincipal;
import com.iflytek.skillhub.auth.token.ApiTokenService;
import com.iflytek.skillhub.domain.user.UserStatus;
import com.iflytek.skillhub.dto.HunterDockProvisionRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HunterDockProvisionAppServiceTest {

    private final IdentityBindingService identityBindingService = mock(IdentityBindingService.class);
    private final ApiTokenService apiTokenService = mock(ApiTokenService.class);
    private final HunterDockProvisionAppService service =
            new HunterDockProvisionAppService(identityBindingService, apiTokenService);

    @Test
    void provision_bindsExistingOidcSubjectAndRotatesHunterDockToken() {
        when(identityBindingService.bindOrCreate(any(OAuthClaims.class), eq(UserStatus.ACTIVE)))
                .thenReturn(new PlatformPrincipal("usr_1", "张三", "zhang@example.com", "", "tuyoo", Set.of("USER")));
        ApiToken token = new ApiToken("usr_1", "HunterDock", "sk_abcd", "hash", "[]");
        org.springframework.test.util.ReflectionTestUtils.setField(token, "id", 9L);
        when(apiTokenService.rotateToken("usr_1", "HunterDock", "[\"skill:read\",\"skill:publish\"]"))
                .thenReturn(new ApiTokenService.TokenCreateResult("sk_raw", token));

        var response = service.provision(new HunterDockProvisionRequest(
                "tuyoo",
                "13800138000",
                " 张三 ",
                " zhang@example.com ",
                " https://img.example/avatar.png ",
                ""
        ));

        ArgumentCaptor<OAuthClaims> claims = ArgumentCaptor.forClass(OAuthClaims.class);
        verify(identityBindingService).bindOrCreate(claims.capture(), eq(UserStatus.ACTIVE));
        assertThat(claims.getValue().provider()).isEqualTo("tuyoo");
        assertThat(claims.getValue().subject()).isEqualTo("13800138000");
        assertThat(claims.getValue().providerLogin()).isEqualTo("张三");
        assertThat(claims.getValue().email()).isEqualTo("zhang@example.com");
        assertThat(claims.getValue().extra()).containsEntry("avatar_url", "https://img.example/avatar.png");
        assertThat(response.userId()).isEqualTo("usr_1");
        assertThat(response.token()).isEqualTo("sk_raw");
        assertThat(response.tokenId()).isEqualTo(9L);
        assertThat(response.tokenName()).isEqualTo("HunterDock");
    }

    @Test
    void provision_requiresSubject() {
        assertThatThrownBy(() -> service.provision(new HunterDockProvisionRequest(
                "tuyoo",
                " ",
                "张三",
                null,
                null,
                null
        ))).isInstanceOf(IllegalArgumentException.class);
    }
}
