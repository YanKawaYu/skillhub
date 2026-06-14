package com.iflytek.skillhub.domain.namespace;

/**
 * 成员来源，用于区分组织同步维护的基线成员与人工叠加成员。
 *
 * <ul>
 *   <li>{@link #SYNCED}：由 HunterDockServer 按钉钉组织树 reconcile 写入，进/退随组织变动，用户不可手工移除。</li>
 *   <li>{@link #MANUAL}：人工添加的叠加成员（跨团队协作者、历史手工成员），reconcile 永不触碰。</li>
 * </ul>
 *
 * 现有 TEAM 库的人工成员一律视为 {@link #MANUAL}（见迁移默认值），因此默认值取 MANUAL。
 */
public enum MemberSource {
    SYNCED,
    MANUAL
}
