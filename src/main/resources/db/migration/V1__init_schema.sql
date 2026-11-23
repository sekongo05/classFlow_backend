-- =============================================================================
-- ClassFlow v1.0 — Schéma PostgreSQL initial (V1)
-- Aligné strictement sur le Cahier des charges v1.0 (Phases 1 à 6)
-- =============================================================================

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    action VARCHAR(80) NOT NULL,
    entity_type VARCHAR(80) NOT NULL,
    entity_id VARCHAR(80) NOT NULL,
    actor_id UUID,
    actor_email VARCHAR(150),
    actor_role VARCHAR(40),
    details VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE academic_classes (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    track VARCHAR(120) NOT NULL,
    level VARCHAR(60) NOT NULL,
    academic_year VARCHAR(20) NOT NULL,
    expected_size INT NOT NULL,
    delegate_id UUID,
    deputy_delegate_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_class_name_year UNIQUE (name, academic_year)
);

CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    matricule VARCHAR(60) UNIQUE,
    role VARCHAR(30) NOT NULL,
    academic_class_id UUID REFERENCES academic_classes(id),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    token VARCHAR(128) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE subjects (
    id UUID PRIMARY KEY,
    academic_class_id UUID NOT NULL REFERENCES academic_classes(id),
    code VARCHAR(40) NOT NULL,
    name VARCHAR(150) NOT NULL,
    default_coefficient DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    teacher_id UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE class_roster_entries (
    id UUID PRIMARY KEY,
    academic_class_id UUID NOT NULL REFERENCES academic_classes(id),
    matricule VARCHAR(60) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    email VARCHAR(150),
    linked_user_id UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_roster_class_matricule UNIQUE (academic_class_id, matricule)
);

CREATE TABLE roster_import_history (
    id UUID PRIMARY KEY,
    academic_class_id UUID NOT NULL REFERENCES academic_classes(id),
    imported_by UUID NOT NULL REFERENCES users(id),
    file_name VARCHAR(255) NOT NULL,
    imported_count INT NOT NULL,
    updated_count INT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE rooms (
    id UUID PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    wifi_ssid VARCHAR(120) NOT NULL,
    network_secret VARCHAR(120) NOT NULL,
    qr_code_token VARCHAR(120) NOT NULL UNIQUE,
    operational BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE timetable_slots (
    id UUID PRIMARY KEY,
    academic_class_id UUID NOT NULL REFERENCES academic_classes(id),
    subject_id UUID NOT NULL REFERENCES subjects(id),
    teacher_id UUID REFERENCES users(id),
    room_id UUID NOT NULL REFERENCES rooms(id),
    day_of_week VARCHAR(15) NOT NULL,
    specific_date DATE,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE course_sessions (
    id UUID PRIMARY KEY,
    academic_class_id UUID NOT NULL REFERENCES academic_classes(id),
    subject_id UUID NOT NULL REFERENCES subjects(id),
    teacher_id UUID REFERENCES users(id),
    room_id UUID NOT NULL REFERENCES rooms(id),
    session_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'SCHEDULED',
    emergency_mode BOOLEAN NOT NULL DEFAULT FALSE,
    notes VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE teacher_absence_declarations (
    id UUID PRIMARY KEY,
    course_session_id UUID NOT NULL REFERENCES course_sessions(id),
    declared_by UUID NOT NULL REFERENCES users(id),
    declaration_type VARCHAR(30) NOT NULL,
    comment VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE attendance_sheets (
    id UUID PRIMARY KEY,
    course_session_id UUID NOT NULL UNIQUE REFERENCES course_sessions(id),
    sheet_version INT NOT NULL DEFAULT 1,
    present_count INT NOT NULL DEFAULT 0,
    late_count INT NOT NULL DEFAULT 0,
    absent_count INT NOT NULL DEFAULT 0,
    closed_at TIMESTAMP WITH TIME ZONE,
    validated_by UUID REFERENCES users(id),
    validated_at TIMESTAMP WITH TIME ZONE,
    contains_emergency_records BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE attendance_records (
    id UUID PRIMARY KEY,
    course_session_id UUID NOT NULL REFERENCES course_sessions(id),
    student_id UUID NOT NULL REFERENCES users(id),
    matricule VARCHAR(60) NOT NULL,
    student_name VARCHAR(200) NOT NULL,
    status VARCHAR(20) NOT NULL,
    mode VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    checked_in_at TIMESTAMP WITH TIME ZONE,
    regularized BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_attendance_session_student UNIQUE (course_session_id, student_id)
);

CREATE TABLE attendance_corrections (
    id UUID PRIMARY KEY,
    attendance_record_id UUID NOT NULL REFERENCES attendance_records(id),
    course_session_id UUID NOT NULL REFERENCES course_sessions(id),
    previous_status VARCHAR(20) NOT NULL,
    new_status VARCHAR(20) NOT NULL,
    modified_by UUID NOT NULL REFERENCES users(id),
    reason VARCHAR(500) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE sheet_validations (
    id UUID PRIMARY KEY,
    course_session_id UUID NOT NULL REFERENCES course_sessions(id),
    sheet_id UUID NOT NULL REFERENCES attendance_sheets(id),
    sheet_version INT NOT NULL,
    teacher_id UUID NOT NULL REFERENCES users(id),
    decision VARCHAR(20) NOT NULL,
    comment VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE personal_evaluations (
    id UUID PRIMARY KEY,
    student_id UUID NOT NULL REFERENCES users(id),
    subject_name VARCHAR(150) NOT NULL,
    evaluation_type VARCHAR(20) NOT NULL,
    title VARCHAR(150) NOT NULL,
    score DOUBLE PRECISION NOT NULL,
    max_score DOUBLE PRECISION NOT NULL DEFAULT 20.0,
    coefficient DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    semester VARCHAR(20),
    academic_year VARCHAR(20),
    evaluation_date DATE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    recipient_id UUID NOT NULL REFERENCES users(id),
    type VARCHAR(60) NOT NULL,
    title VARCHAR(200) NOT NULL,
    message VARCHAR(500) NOT NULL,
    read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE device_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    token VARCHAR(255) NOT NULL UNIQUE,
    platform VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE sync_operation_logs (
    id UUID PRIMARY KEY,
    client_operation_id UUID NOT NULL UNIQUE,
    user_id UUID NOT NULL REFERENCES users(id),
    operation_type VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL,
    message VARCHAR(300),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_sessions_class_date ON course_sessions(academic_class_id, session_date);
CREATE INDEX idx_sessions_teacher_date ON course_sessions(teacher_id, session_date);
CREATE INDEX idx_attendance_session ON attendance_records(course_session_id);
CREATE INDEX idx_attendance_student ON attendance_records(student_id);
CREATE INDEX idx_notifications_recipient ON notifications(recipient_id, read);
CREATE INDEX idx_evaluations_student ON personal_evaluations(student_id);
