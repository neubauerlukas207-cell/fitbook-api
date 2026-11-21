-- Schéma vychází z mé školní databáze fitness-reservation-db, zjednodušené pro API.

CREATE TABLE room (
    id        BIGSERIAL PRIMARY KEY,
    name      VARCHAR(100) NOT NULL UNIQUE,
    capacity  INT          NOT NULL CHECK (capacity > 0)
);

CREATE TABLE instructor (
    id        BIGSERIAL PRIMARY KEY,
    name      VARCHAR(100) NOT NULL,
    rating    NUMERIC(2, 1) NOT NULL CHECK (rating BETWEEN 1 AND 5)
);

CREATE TABLE member (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    email      VARCHAR(150) NOT NULL UNIQUE,
    created_at TIMESTAMP    NOT NULL
);

CREATE TABLE lesson (
    id               BIGSERIAL PRIMARY KEY,
    type             VARCHAR(30) NOT NULL,
    starts_at        TIMESTAMP   NOT NULL,
    duration_minutes INT         NOT NULL CHECK (duration_minutes BETWEEN 15 AND 180),
    room_id          BIGINT      NOT NULL REFERENCES room (id),
    instructor_id    BIGINT      NOT NULL REFERENCES instructor (id)
);
CREATE INDEX idx_lesson_starts_at ON lesson (starts_at);

CREATE TABLE reservation (
    id         BIGSERIAL PRIMARY KEY,
    member_id  BIGINT      NOT NULL REFERENCES member (id),
    lesson_id  BIGINT      NOT NULL REFERENCES lesson (id),
    status     VARCHAR(20) NOT NULL,
    created_at TIMESTAMP   NOT NULL,
    -- jeden člen má na lekci nejvýš jednu rezervaci (po zrušení se znovu aktivuje)
    CONSTRAINT uq_reservation_member_lesson UNIQUE (member_id, lesson_id)
);
CREATE INDEX idx_reservation_lesson ON reservation (lesson_id, status);
