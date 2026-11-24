-- ELRICS Patient Management Portal - initial schema (MySQL 8)

CREATE TABLE users (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    full_name   VARCHAR(100) NOT NULL,
    username    VARCHAR(50)  NOT NULL,
    email       VARCHAR(150) NOT NULL,
    password    VARCHAR(100) NOT NULL,
    role        VARCHAR(20)  NOT NULL,
    -- Constant guard column: its UNIQUE constraint lets the database itself guarantee that
    -- only ONE administrator can ever be registered, even with several backend instances.
    admin_guard TINYINT(1)   NOT NULL DEFAULT 1,
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT uq_users_single_admin UNIQUE (admin_guard)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE patients (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    patient_id        VARCHAR(20)  NOT NULL,
    full_name         VARCHAR(100) NOT NULL,
    age               INT          NOT NULL,
    gender            VARCHAR(10)  NOT NULL,
    phone             VARCHAR(20)  NOT NULL,
    email             VARCHAR(150) NULL,
    address           VARCHAR(255) NULL,
    blood_group       VARCHAR(3)   NOT NULL,
    status            VARCHAR(20)  NOT NULL DEFAULT 'Pending',
    emergency_contact VARCHAR(150) NULL,
    admitted_at       DATETIME(6)  NULL,
    discharged_at     DATETIME(6)  NULL,
    created_by        BIGINT       NULL,
    created_at        DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at        DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uq_patients_patient_id UNIQUE (patient_id),
    CONSTRAINT uq_patients_email UNIQUE (email),
    CONSTRAINT uq_patients_name_phone UNIQUE (full_name, phone),
    CONSTRAINT fk_patients_created_by FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT chk_patients_age CHECK (age BETWEEN 0 AND 150)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_patients_status ON patients (status);
CREATE INDEX idx_patients_created_at ON patients (created_at);
CREATE INDEX idx_patients_updated_at ON patients (updated_at);
CREATE INDEX idx_patients_admitted_at ON patients (admitted_at);
CREATE INDEX idx_patients_discharged_at ON patients (discharged_at);
