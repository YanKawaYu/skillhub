package com.iflytek.skillhub.domain.namespace;

public enum NamespaceType {
    GLOBAL,
    TEAM,
    // 组织派生库：成员由 HunterDockServer 按钉钉组织树 reconcile 同步维护，
    // 用户只能在其上做 manual 叠加（加协作者、改显示名），不可手工移除 synced 成员、不可改生命周期。
    ORG
}
