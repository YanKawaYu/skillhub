package com.iflytek.skillhub.domain.namespace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.iflytek.skillhub.domain.namespace.OrgNamespaceSyncService.DesiredMembership;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class OrgNamespaceSyncServiceTest {

    private final NamespaceRepository namespaceRepository = mock(NamespaceRepository.class);
    private final NamespaceMemberRepository memberRepository = mock(NamespaceMemberRepository.class);
    private final OrgNamespaceSyncService service =
            new OrgNamespaceSyncService(namespaceRepository, memberRepository);

    @Test
    void lazilyCreatesOrgNamespaceAndAddsSyncedMember() {
        when(namespaceRepository.findBySlug("org-d1")).thenReturn(Optional.empty());
        when(namespaceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(memberRepository.findByNamespaceIdAndUserId(any(), any())).thenReturn(Optional.empty());

        service.reconcile("u1",
                List.of(new DesiredMembership("org-d1", "星云工作室", NamespaceRole.MEMBER)),
                List.of());

        ArgumentCaptor<Namespace> ns = ArgumentCaptor.forClass(Namespace.class);
        verify(namespaceRepository).save(ns.capture());
        assertThat(ns.getValue().getSlug()).isEqualTo("org-d1");
        assertThat(ns.getValue().getType()).isEqualTo(NamespaceType.ORG);

        ArgumentCaptor<NamespaceMember> member = ArgumentCaptor.forClass(NamespaceMember.class);
        verify(memberRepository).save(member.capture());
        assertThat(member.getValue().getUserId()).isEqualTo("u1");
        assertThat(member.getValue().getRole()).isEqualTo(NamespaceRole.MEMBER);
        assertThat(member.getValue().getSource()).isEqualTo(MemberSource.SYNCED);
    }

    @Test
    void adoptsExistingTeamNamespaceIntoOrgPreservingIt() {
        Namespace legacy = new Namespace("szxiuxian", "星云工作室", "creator");
        legacy.setType(NamespaceType.TEAM);
        when(namespaceRepository.findBySlug("szxiuxian")).thenReturn(Optional.of(legacy));
        when(namespaceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(memberRepository.findByNamespaceIdAndUserId(any(), any())).thenReturn(Optional.empty());

        service.reconcile("u1",
                List.of(new DesiredMembership("szxiuxian", "星云工作室", NamespaceRole.MEMBER)),
                List.of());

        assertThat(legacy.getType()).isEqualTo(NamespaceType.ORG);
        verify(namespaceRepository).save(legacy);
        verify(memberRepository).save(any());
    }

    @Test
    void demotesRequestedOwnerToAdminWhenAnotherOwnerExists() {
        Namespace org = orgNamespace("org-d2", "恒星工作室");
        when(namespaceRepository.findBySlug("org-d2")).thenReturn(Optional.of(org));
        when(memberRepository.findByNamespaceIdAndUserId(any(), any())).thenReturn(Optional.empty());
        when(memberRepository.findByNamespaceIdAndRoleIn(any(), any()))
                .thenReturn(List.of(new NamespaceMember(null, "existing-owner", NamespaceRole.OWNER, MemberSource.MANUAL)));

        service.reconcile("u-leader",
                List.of(new DesiredMembership("org-d2", "恒星工作室", NamespaceRole.OWNER)),
                List.of());

        ArgumentCaptor<NamespaceMember> member = ArgumentCaptor.forClass(NamespaceMember.class);
        verify(memberRepository).save(member.capture());
        assertThat(member.getValue().getRole()).isEqualTo(NamespaceRole.ADMIN);
        assertThat(member.getValue().getSource()).isEqualTo(MemberSource.SYNCED);
    }

    @Test
    void keepsRequestedOwnerWhenNoOtherOwnerExists() {
        Namespace org = orgNamespace("org-d3", "工作室");
        when(namespaceRepository.findBySlug("org-d3")).thenReturn(Optional.of(org));
        when(memberRepository.findByNamespaceIdAndUserId(any(), any())).thenReturn(Optional.empty());
        when(memberRepository.findByNamespaceIdAndRoleIn(any(), any())).thenReturn(List.of());

        service.reconcile("u-leader",
                List.of(new DesiredMembership("org-d3", "工作室", NamespaceRole.OWNER)),
                List.of());

        ArgumentCaptor<NamespaceMember> member = ArgumentCaptor.forClass(NamespaceMember.class);
        verify(memberRepository).save(member.capture());
        assertThat(member.getValue().getRole()).isEqualTo(NamespaceRole.OWNER);
    }

    @Test
    void doesNotTouchExistingOwnerMembership() {
        Namespace org = orgNamespace("org-d4", "工作室");
        when(namespaceRepository.findBySlug("org-d4")).thenReturn(Optional.of(org));
        when(memberRepository.findByNamespaceIdAndUserId(any(), any()))
                .thenReturn(Optional.of(new NamespaceMember(null, "u1", NamespaceRole.OWNER, MemberSource.MANUAL)));

        service.reconcile("u1",
                List.of(new DesiredMembership("org-d4", "工作室", NamespaceRole.MEMBER)),
                List.of());

        verify(memberRepository, never()).save(any());
        verify(namespaceRepository, never()).save(any());
    }

    @Test
    void retractsSyncedNonOwnerMembershipOnLeave() {
        Namespace org = orgNamespace("org-old", "旧库");
        when(namespaceRepository.findBySlug("org-old")).thenReturn(Optional.of(org));
        when(memberRepository.findByNamespaceIdAndUserId(any(), any()))
                .thenReturn(Optional.of(new NamespaceMember(null, "u1", NamespaceRole.MEMBER, MemberSource.SYNCED)));

        service.reconcile("u1", List.of(), List.of("org-old"));

        verify(memberRepository).deleteByNamespaceIdAndUserId(any(), eq("u1"));
    }

    @Test
    void keepsManualMembershipOnLeave() {
        Namespace org = orgNamespace("org-old", "旧库");
        when(namespaceRepository.findBySlug("org-old")).thenReturn(Optional.of(org));
        when(memberRepository.findByNamespaceIdAndUserId(any(), any()))
                .thenReturn(Optional.of(new NamespaceMember(null, "u1", NamespaceRole.MEMBER, MemberSource.MANUAL)));

        service.reconcile("u1", List.of(), List.of("org-old"));

        verify(memberRepository, never()).deleteByNamespaceIdAndUserId(any(), any());
    }

    @Test
    void idempotentReconcileDoesNotRecreateExistingOrgNamespace() {
        Namespace org = orgNamespace("org-x", "X");
        when(namespaceRepository.findBySlug("org-x")).thenReturn(Optional.of(org));
        when(memberRepository.findByNamespaceIdAndUserId(any(), any()))
                .thenReturn(Optional.of(new NamespaceMember(null, "u1", NamespaceRole.MEMBER, MemberSource.SYNCED)));

        service.reconcile("u1",
                List.of(new DesiredMembership("org-x", "X", NamespaceRole.MEMBER)),
                List.of());

        verify(namespaceRepository, never()).save(any());
    }

    private static Namespace orgNamespace(String slug, String displayName) {
        Namespace namespace = new Namespace(slug, displayName, "creator");
        namespace.setType(NamespaceType.ORG);
        return namespace;
    }
}
