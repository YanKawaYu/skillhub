package com.iflytek.skillhub.controller;

import com.iflytek.skillhub.dto.ApiResponseFactory;
import com.iflytek.skillhub.dto.HunterDockProvisionRequest;
import com.iflytek.skillhub.dto.HunterDockProvisionResponse;
import com.iflytek.skillhub.dto.OrgNamespaceReconcileRequest;
import com.iflytek.skillhub.service.HunterDockProvisionAppService;
import com.iflytek.skillhub.service.OrgNamespaceReconcileAppService;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class InternalHunterDockProvisionControllerTest {

    private final HunterDockProvisionAppService provisionAppService = mock(HunterDockProvisionAppService.class);
    private final OrgNamespaceReconcileAppService reconcileAppService = mock(OrgNamespaceReconcileAppService.class);
    private final HunterDockProvisionRequest request = new HunterDockProvisionRequest(
            "tuyoo",
            "13800138000",
            "张三",
            null,
            null,
            "HunterDock"
    );

    @Test
    void provision_rejectsMissingOrWrongSharedSecret() {
        var controller = newController();

        var response = controller.provision("wrong", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().code()).isEqualTo(403);
        verifyNoInteractions(provisionAppService);
    }

    @Test
    void provision_returnsTokenWhenSharedSecretMatches() {
        var controller = newController();
        var provisioned = new HunterDockProvisionResponse("usr_1", "sk_raw", 7L, "HunterDock", "sk_raw");
        when(provisionAppService.provision(request)).thenReturn(provisioned);

        var response = controller.provision("secret", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().code()).isEqualTo(0);
        assertThat(response.getBody().data()).isEqualTo(provisioned);
    }

    @Test
    void reconcile_rejectsWrongSharedSecret() {
        var controller = newController();

        var response = controller.reconcileNamespaces("wrong",
                new OrgNamespaceReconcileRequest("usr_1", List.of(), List.of()));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().code()).isEqualTo(403);
        verifyNoInteractions(reconcileAppService);
    }

    @Test
    void reconcile_invokesServiceWhenSharedSecretMatches() {
        var controller = newController();
        var reconcileRequest = new OrgNamespaceReconcileRequest("usr_1", List.of(), List.of());

        var response = controller.reconcileNamespaces("secret", reconcileRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().code()).isEqualTo(0);
        verify(reconcileAppService).reconcile(reconcileRequest);
    }

    private InternalHunterDockProvisionController newController() {
        return new InternalHunterDockProvisionController(
                provisionAppService,
                reconcileAppService,
                responseFactory(),
                "secret"
        );
    }

    private ApiResponseFactory responseFactory() {
        StaticMessageSource messageSource = new StaticMessageSource();
        messageSource.addMessage("response.success.created", Locale.ENGLISH, "Created successfully");
        messageSource.addMessage("response.success.updated", Locale.ENGLISH, "Updated successfully");
        messageSource.addMessage("error.forbidden", Locale.ENGLISH, "Forbidden");
        return new ApiResponseFactory(
                messageSource,
                Clock.fixed(Instant.parse("2026-06-09T00:00:00Z"), ZoneOffset.UTC)
        );
    }
}
