package com.iflytek.skillhub.service;

import com.iflytek.skillhub.domain.namespace.NamespaceRole;
import com.iflytek.skillhub.domain.namespace.OrgNamespaceSyncService;
import com.iflytek.skillhub.dto.OrgNamespaceReconcileRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Maps the internal reconcile request to the domain sync service. Identity resolution is not done
 * here: HunterDockServer passes the SkillHub {@code userId} obtained from provisioning.
 */
@Service
public class OrgNamespaceReconcileAppService {

    private final OrgNamespaceSyncService syncService;

    public OrgNamespaceReconcileAppService(OrgNamespaceSyncService syncService) {
        this.syncService = syncService;
    }

    public void reconcile(OrgNamespaceReconcileRequest request) {
        String userId = requireText(request.userId(), "userId");
        List<OrgNamespaceSyncService.DesiredMembership> desired = new ArrayList<>();
        if (request.memberships() != null) {
            for (OrgNamespaceReconcileRequest.Membership membership : request.memberships()) {
                String slug = requireText(membership.slug(), "slug");
                desired.add(new OrgNamespaceSyncService.DesiredMembership(
                        slug,
                        StringUtils.hasText(membership.displayName()) ? membership.displayName().trim() : slug,
                        parseRole(membership.role())));
            }
        }
        List<String> previousSlugs = request.previousSlugs() == null ? List.of() : request.previousSlugs();
        syncService.reconcile(userId, desired, previousSlugs);
    }

    private NamespaceRole parseRole(String role) {
        if (!StringUtils.hasText(role)) {
            return NamespaceRole.MEMBER;
        }
        try {
            return NamespaceRole.valueOf(role.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("unsupported namespace role: " + role);
        }
    }

    private String requireText(String value, String field) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
