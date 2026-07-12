-- Matches the Membership/Schedule JPA entities
-- (src/main/java/com/senthil/membership/model/) exactly. Idempotent (IF NOT EXISTS) so
-- it's safe to run on every startup, not just the first. ddl-auto is deliberately
-- "validate" in prod (see application-prod.yml) -- schema changes are never
-- auto-migrated by Hibernate; this file (run by Spring's SQL initializer, before
-- Hibernate's validation) is the one deliberate exception, and it runs on every deploy,
-- not as a one-off manual step.
CREATE TABLE IF NOT EXISTS membership (
    id BIGSERIAL PRIMARY KEY,
    member_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    class_type VARCHAR(20) NOT NULL
);

CREATE TABLE IF NOT EXISTS schedule (
    id BIGSERIAL PRIMARY KEY,
    membership_id BIGINT NOT NULL REFERENCES membership(id),
    frequency VARCHAR(20) NOT NULL,
    day_of_week VARCHAR(20),
    time TIME NOT NULL
);
