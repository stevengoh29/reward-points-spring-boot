package com.example.rewardspoints.shared.persistence.specification;

import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Builds a {@link Specification} from optional filters. A filter whose value is
 * null (or blank for strings, empty for collections) is skipped, so callers can pass
 * query params straight through without null checks.
 *
 * <pre>{@code
 * Specification<User> spec = SpecificationBuilder.<User>create()
 *         .contains("email", query.email())
 *         .equal("status", query.status())
 *         .build();
 * }</pre>
 */
public final class SpecificationBuilder<T> {

    private final List<Specification<T>> specs = new ArrayList<>();

    private SpecificationBuilder() {
    }

    public static <T> SpecificationBuilder<T> create() {
        return new SpecificationBuilder<>();
    }

    public SpecificationBuilder<T> equal(String field, Object value) {
        if (value != null) {
            specs.add((root, query, cb) -> cb.equal(root.get(field), value));
        }
        return this;
    }

    /** Case-insensitive "contains" match. */
    public SpecificationBuilder<T> contains(String field, String value) {
        if (value != null && !value.isBlank()) {
            String pattern = "%" + escapeLike(value.trim().toLowerCase()) + "%";
            specs.add((root, query, cb) -> cb.like(cb.lower(root.get(field)), pattern, '\\'));
        }
        return this;
    }

    public SpecificationBuilder<T> in(String field, Collection<?> values) {
        if (values != null && !values.isEmpty()) {
            specs.add((root, query, cb) -> root.get(field).in(values));
        }
        return this;
    }

    public <V extends Comparable<? super V>> SpecificationBuilder<T> greaterThanOrEqual(String field, V value) {
        if (value != null) {
            specs.add((root, query, cb) -> cb.greaterThanOrEqualTo(root.<V>get(field), value));
        }
        return this;
    }

    public <V extends Comparable<? super V>> SpecificationBuilder<T> lessThanOrEqual(String field, V value) {
        if (value != null) {
            specs.add((root, query, cb) -> cb.lessThanOrEqualTo(root.<V>get(field), value));
        }
        return this;
    }

    /** Escape hatch for joins or conditions the helpers above don't cover. */
    public SpecificationBuilder<T> and(Specification<T> spec) {
        if (spec != null) {
            specs.add(spec);
        }
        return this;
    }

    public Specification<T> build() {
        return Specification.allOf(specs);
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
