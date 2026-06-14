package com.iflytek.skillhub.domain.namespace;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NamespaceAccessPolicyTest {

    private final NamespaceAccessPolicy policy = new NamespaceAccessPolicy();

    @Test
    void globalNamespaceIsImmutable() {
        Namespace namespace = new Namespace("global", "Global", "owner");
        namespace.setType(NamespaceType.GLOBAL);

        assertThat(policy.isImmutable(namespace)).isTrue();
        assertThat(policy.canMutateSettings(namespace)).isFalse();
        assertThat(policy.canManageMembers(namespace)).isFalse();
        assertThat(policy.canTransferOwnership(namespace)).isFalse();
    }

    @Test
    void activeTeamNamespaceAllowsAdminAndOwnerToFreezeButNotMember() {
        Namespace namespace = new Namespace("team-a", "Team A", "owner");
        namespace.setType(NamespaceType.TEAM);
        namespace.setStatus(NamespaceStatus.ACTIVE);

        assertThat(policy.canFreeze(namespace, NamespaceRole.OWNER)).isTrue();
        assertThat(policy.canFreeze(namespace, NamespaceRole.ADMIN)).isTrue();
        assertThat(policy.canFreeze(namespace, NamespaceRole.MEMBER)).isFalse();
    }

    @Test
    void frozenTeamNamespaceAllowsAdminAndOwnerToUnfreezeButNotMember() {
        Namespace namespace = new Namespace("team-a", "Team A", "owner");
        namespace.setType(NamespaceType.TEAM);
        namespace.setStatus(NamespaceStatus.FROZEN);

        assertThat(policy.canUnfreeze(namespace, NamespaceRole.OWNER)).isTrue();
        assertThat(policy.canUnfreeze(namespace, NamespaceRole.ADMIN)).isTrue();
        assertThat(policy.canUnfreeze(namespace, NamespaceRole.MEMBER)).isFalse();
    }

    @Test
    void archiveAndRestoreAreOwnerOnly() {
        Namespace namespace = new Namespace("team-a", "Team A", "owner");
        namespace.setType(NamespaceType.TEAM);
        namespace.setStatus(NamespaceStatus.ACTIVE);

        assertThat(policy.canArchive(namespace, NamespaceRole.OWNER)).isTrue();
        assertThat(policy.canArchive(namespace, NamespaceRole.ADMIN)).isFalse();

        namespace.setStatus(NamespaceStatus.ARCHIVED);
        assertThat(policy.canRestore(namespace, NamespaceRole.OWNER)).isTrue();
        assertThat(policy.canRestore(namespace, NamespaceRole.ADMIN)).isFalse();
    }

    @Test
    void deleteIsOwnerOnlyForTeamNamespaces() {
        Namespace namespace = new Namespace("team-a", "Team A", "owner");
        namespace.setType(NamespaceType.TEAM);

        assertThat(policy.canDelete(namespace, NamespaceRole.OWNER)).isTrue();
        assertThat(policy.canDelete(namespace, NamespaceRole.ADMIN)).isFalse();

        namespace.setType(NamespaceType.GLOBAL);
        assertThat(policy.canDelete(namespace, NamespaceRole.OWNER)).isFalse();
    }

    @Test
    void orgNamespaceAllowsSettingsAndAdditiveMembersButProtectsSyncedMembersAndLifecycle() {
        Namespace namespace = new Namespace("org-d123", "星云工作室", "owner");
        namespace.setType(NamespaceType.ORG);
        namespace.setStatus(NamespaceStatus.ACTIVE);

        // ORG 不像 GLOBAL 那样整体只读：可改显示名（人工覆盖派生名）、可加 manual 叠加成员
        assertThat(policy.isImmutable(namespace)).isFalse();
        assertThat(policy.canMutateSettings(namespace)).isTrue();
        assertThat(policy.canManageMembers(namespace)).isTrue();

        // synced 成员由组织同步维护，不可手工移除；manual 叠加成员可移除
        NamespaceMember synced = new NamespaceMember(1L, "u1", NamespaceRole.MEMBER, MemberSource.SYNCED);
        NamespaceMember manual = new NamespaceMember(1L, "u2", NamespaceRole.MEMBER, MemberSource.MANUAL);
        assertThat(policy.canRemoveMember(namespace, synced)).isFalse();
        assertThat(policy.canRemoveMember(namespace, manual)).isTrue();

        // 所有权转移与生命周期由服务端托管，用户不可操作
        assertThat(policy.canTransferOwnership(namespace)).isFalse();
        assertThat(policy.canFreeze(namespace, NamespaceRole.OWNER)).isFalse();
        assertThat(policy.canArchive(namespace, NamespaceRole.OWNER)).isFalse();
        assertThat(policy.canDelete(namespace, NamespaceRole.OWNER)).isFalse();
    }

    @Test
    void teamNamespaceAllowsRemovingAnyMemberAndTransfer() {
        Namespace namespace = new Namespace("team-a", "Team A", "owner");
        namespace.setType(NamespaceType.TEAM);
        namespace.setStatus(NamespaceStatus.ACTIVE);

        // 默认来源为 MANUAL，TEAM 库任何成员都可手工移除
        NamespaceMember member = new NamespaceMember(1L, "u1", NamespaceRole.MEMBER);
        assertThat(member.getSource()).isEqualTo(MemberSource.MANUAL);
        assertThat(policy.canRemoveMember(namespace, member)).isTrue();
        assertThat(policy.canTransferOwnership(namespace)).isTrue();
    }
}
