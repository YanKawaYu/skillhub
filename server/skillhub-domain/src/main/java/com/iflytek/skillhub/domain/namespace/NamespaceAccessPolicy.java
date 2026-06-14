package com.iflytek.skillhub.domain.namespace;

import org.springframework.stereotype.Component;

/**
 * Encapsulates namespace lifecycle rules that determine which management actions are currently
 * allowed.
 */
@Component
public class NamespaceAccessPolicy {

    public boolean isImmutable(Namespace namespace) {
        return namespace.getType() == NamespaceType.GLOBAL;
    }

    /**
     * TEAM 与 ORG 都允许改设置（ORG 用于人工覆盖派生显示名）和管理成员；
     * 但 ORG 的成员同步由服务端 reconcile 维护，所有权转移与生命周期不开放给用户。
     */
    private boolean isMemberManagedType(Namespace namespace) {
        return (namespace.getType() == NamespaceType.TEAM || namespace.getType() == NamespaceType.ORG)
                && namespace.getStatus() == NamespaceStatus.ACTIVE;
    }

    public boolean canMutateSettings(Namespace namespace) {
        return isMemberManagedType(namespace);
    }

    public boolean canManageMembers(Namespace namespace) {
        return isMemberManagedType(namespace);
    }

    /**
     * ORG 库的 synced 成员由组织同步维护，不可手工移除；manual 叠加成员可移除。
     * 其它类型沿用 {@link #canManageMembers} 的判断。
     */
    public boolean canRemoveMember(Namespace namespace, NamespaceMember member) {
        if (!canManageMembers(namespace)) {
            return false;
        }
        return !(namespace.getType() == NamespaceType.ORG && member.getSource() == MemberSource.SYNCED);
    }

    public boolean canTransferOwnership(Namespace namespace) {
        return namespace.getType() == NamespaceType.TEAM
                && namespace.getStatus() == NamespaceStatus.ACTIVE;
    }

    public boolean canFreeze(Namespace namespace, NamespaceRole role) {
        return namespace.getType() == NamespaceType.TEAM
                && namespace.getStatus() == NamespaceStatus.ACTIVE
                && (role == NamespaceRole.OWNER || role == NamespaceRole.ADMIN);
    }

    public boolean canUnfreeze(Namespace namespace, NamespaceRole role) {
        return namespace.getType() == NamespaceType.TEAM
                && namespace.getStatus() == NamespaceStatus.FROZEN
                && (role == NamespaceRole.OWNER || role == NamespaceRole.ADMIN);
    }

    public boolean canArchive(Namespace namespace, NamespaceRole role) {
        return namespace.getType() == NamespaceType.TEAM
                && namespace.getStatus() != NamespaceStatus.ARCHIVED
                && role == NamespaceRole.OWNER;
    }

    public boolean canRestore(Namespace namespace, NamespaceRole role) {
        return namespace.getType() == NamespaceType.TEAM
                && namespace.getStatus() == NamespaceStatus.ARCHIVED
                && role == NamespaceRole.OWNER;
    }

    public boolean canDelete(Namespace namespace, NamespaceRole role) {
        return namespace.getType() == NamespaceType.TEAM
                && role == NamespaceRole.OWNER;
    }
}
