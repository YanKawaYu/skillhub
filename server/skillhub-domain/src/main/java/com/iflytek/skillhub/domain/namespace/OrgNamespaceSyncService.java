package com.iflytek.skillhub.domain.namespace;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 组织派生命名空间的幂等对账（reconcile）。
 *
 * <p>由 HunterDockServer 在用户登录/刷新时按钉钉组织树算出该用户「应在哪些库、各是什么角色」后调用。
 * 本服务只校准 <b>该用户自己</b> 的 synced 成员关系，不解析其他用户，符合「每用户、登录时」的对账模型：
 *
 * <ul>
 *   <li>库不存在 → 懒创建为 {@link NamespaceType#ORG}（建者记为该用户，但不强制其为 OWNER）。</li>
 *   <li>库已存在但非 ORG（收编现有库，如 {@code szxiuxian}）→ 就地转 ORG，保留原成员（默认 MANUAL）与原 OWNER。</li>
 *   <li>请求 OWNER 但库已有其他 OWNER → 降级为 ADMIN：实现「收编保留原 OWNER + 主管加 ADMIN」，并杜绝双 OWNER。</li>
 *   <li>{@code previousSlugs} 中已不在本次 desired 的库 → 撤销该用户的 synced 非 OWNER 成员；MANUAL 叠加与 OWNER 保留。</li>
 * </ul>
 *
 * 幂等：组织未变时重复调用不产生额外写入语义变化。
 */
@Service
public class OrgNamespaceSyncService {

    private final NamespaceRepository namespaceRepository;
    private final NamespaceMemberRepository namespaceMemberRepository;

    public OrgNamespaceSyncService(NamespaceRepository namespaceRepository,
                                   NamespaceMemberRepository namespaceMemberRepository) {
        this.namespaceRepository = namespaceRepository;
        this.namespaceMemberRepository = namespaceMemberRepository;
    }

    /** 该用户应有的一条组织派生成员关系。 */
    public record DesiredMembership(String slug, String displayName, NamespaceRole role) {}

    @Transactional
    public void reconcile(String userId, List<DesiredMembership> desired, List<String> previousSlugs) {
        Set<String> desiredSlugs = new HashSet<>();
        for (DesiredMembership membership : desired) {
            desiredSlugs.add(membership.slug());
            Namespace namespace = ensureOrgNamespace(membership.slug(), membership.displayName(), userId);
            applySyncedMembership(namespace, userId, membership.role());
        }
        for (String slug : previousSlugs) {
            if (desiredSlugs.contains(slug)) {
                continue;
            }
            namespaceRepository.findBySlug(slug)
                    .filter(namespace -> namespace.getType() == NamespaceType.ORG)
                    .ifPresent(namespace -> retractSyncedMembership(namespace, userId));
        }
    }

    private Namespace ensureOrgNamespace(String slug, String displayName, String createdByUserId) {
        Optional<Namespace> existing = namespaceRepository.findBySlug(slug);
        if (existing.isPresent()) {
            Namespace namespace = existing.get();
            if (namespace.getType() != NamespaceType.ORG) {
                // 收编现有库：转 ORG，原成员与原 OWNER 原样保留（现有成员在迁移中默认 MANUAL）
                namespace.setType(NamespaceType.ORG);
                namespace = namespaceRepository.save(namespace);
            }
            return namespace;
        }
        SlugValidator.validate(slug);
        Namespace namespace = new Namespace(slug, displayName, createdByUserId);
        namespace.setType(NamespaceType.ORG);
        return namespaceRepository.save(namespace);
    }

    private void applySyncedMembership(Namespace namespace, String userId, NamespaceRole requestedRole) {
        NamespaceRole effectiveRole = resolveEffectiveRole(namespace, userId, requestedRole);
        Optional<NamespaceMember> existing =
                namespaceMemberRepository.findByNamespaceIdAndUserId(namespace.getId(), userId);
        if (existing.isPresent()) {
            NamespaceMember member = existing.get();
            // 不降级既有 OWNER（收编时保留原 OWNER；也不把 OWNER 翻成 synced 抢管理权）
            if (member.getRole() == NamespaceRole.OWNER) {
                return;
            }
            member.setRole(effectiveRole);
            member.setSource(MemberSource.SYNCED);
            namespaceMemberRepository.save(member);
            return;
        }
        namespaceMemberRepository.save(new NamespaceMember(namespace.getId(), userId, effectiveRole, MemberSource.SYNCED));
    }

    private NamespaceRole resolveEffectiveRole(Namespace namespace, String userId, NamespaceRole requestedRole) {
        if (requestedRole != NamespaceRole.OWNER) {
            return requestedRole;
        }
        List<NamespaceMember> owners = namespaceMemberRepository
                .findByNamespaceIdAndRoleIn(namespace.getId(), List.of(NamespaceRole.OWNER));
        boolean otherOwnerExists = owners.stream().anyMatch(owner -> !owner.getUserId().equals(userId));
        return otherOwnerExists ? NamespaceRole.ADMIN : NamespaceRole.OWNER;
    }

    private void retractSyncedMembership(Namespace namespace, String userId) {
        namespaceMemberRepository.findByNamespaceIdAndUserId(namespace.getId(), userId).ifPresent(member -> {
            if (member.getSource() == MemberSource.SYNCED && member.getRole() != NamespaceRole.OWNER) {
                namespaceMemberRepository.deleteByNamespaceIdAndUserId(namespace.getId(), userId);
            }
        });
    }
}
