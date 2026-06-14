package com.iflytek.skillhub.domain.namespace;

import jakarta.persistence.*;
import java.time.Clock;
import java.time.Instant;

@Entity
@Table(name = "namespace_member",
       uniqueConstraints = @UniqueConstraint(columnNames = {"namespace_id", "user_id"}))
public class NamespaceMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "namespace_id", nullable = false)
    private Long namespaceId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private NamespaceRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 32)
    private MemberSource source = MemberSource.MANUAL;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected NamespaceMember() {}

    public NamespaceMember(Long namespaceId, String userId, NamespaceRole role) {
        this(namespaceId, userId, role, MemberSource.MANUAL);
    }

    public NamespaceMember(Long namespaceId, String userId, NamespaceRole role, MemberSource source) {
        this.namespaceId = namespaceId;
        this.userId = userId;
        this.role = role;
        this.source = source;
    }

    @PrePersist
    void prePersist() {
        this.createdAt = Instant.now(Clock.systemUTC());
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now(Clock.systemUTC());
    }

    public Long getId() { return id; }
    public Long getNamespaceId() { return namespaceId; }
    public void setNamespaceId(Long namespaceId) { this.namespaceId = namespaceId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public NamespaceRole getRole() { return role; }
    public void setRole(NamespaceRole role) { this.role = role; }
    public MemberSource getSource() { return source; }
    public void setSource(MemberSource source) { this.source = source; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
