package com.iflytek.skillhub.dto;

import java.util.List;

/**
 * Internal request used by HunterDockServer to reconcile a single user's org-derived namespace
 * memberships against the DingTalk org tree. The {@code userId} is the SkillHub account id returned
 * by the provision endpoint, so SkillHub does not resolve identities here.
 */
public record OrgNamespaceReconcileRequest(
        String userId,
        List<Membership> memberships,
        List<String> previousSlugs
) {
    public record Membership(String slug, String displayName, String role) {}
}
