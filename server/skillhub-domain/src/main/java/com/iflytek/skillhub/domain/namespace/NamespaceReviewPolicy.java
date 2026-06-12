package com.iflytek.skillhub.domain.namespace;

/**
 * Controls when skill publishes into a namespace require manual review.
 *
 * <p>Applies to PUBLIC/NAMESPACE_ONLY publishes only; PRIVATE versions never
 * enter the review flow regardless of policy.
 */
public enum NamespaceReviewPolicy {
    /** Every publish is approved immediately without a review task. */
    AUTO_APPROVE,
    /** First publish requires review; later updates to an approved skill are exempt. */
    FIRST_PUBLISH_ONLY,
    /** Every publish creates a review task. */
    EVERY_PUBLISH
}
