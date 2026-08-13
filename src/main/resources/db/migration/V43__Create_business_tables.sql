CREATE TABLE course (
    id         VARCHAR(50),
    reference  VARCHAR(20)  NOT NULL,
    title      VARCHAR(50)  NOT NULL,
    credits    SMALLINT     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE (reference),
    UNIQUE (title)
);

CREATE TABLE exam (
    id            VARCHAR(50),
    exam_date     TIMESTAMPTZ    NOT NULL,
    coefficient   NUMERIC(5,4)   NOT NULL,
    course_id     VARCHAR(50)    NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (course_id) REFERENCES course (id)
);

CREATE TABLE users (
    id          CHAR(8),
    first_name  VARCHAR(100)  NOT NULL,
    last_name   VARCHAR(100),
    birth_date  DATE          NOT NULL,
    email       VARCHAR(100)  NOT NULL,
    password    VARCHAR(255)  NOT NULL,
    is_active   BOOLEAN       NOT NULL,
    role        VARCHAR(20)   NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE promotion (
    id          VARCHAR(50),
    reference   VARCHAR(50)  NOT NULL,
    start_year  SMALLINT     NOT NULL,
    end_year    SMALLINT     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE (reference),
    UNIQUE (start_year),
    UNIQUE (end_year)
);

CREATE TABLE student_group (
    id            VARCHAR(50),
    reference     CHAR(2)      NOT NULL,
    promotion_id  VARCHAR(50)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE (reference),
    FOREIGN KEY (promotion_id) REFERENCES promotion (id)
);

CREATE TABLE grade (
    id          VARCHAR(50),
    score       REAL           NOT NULL,
    grade_date  TIMESTAMPTZ    NOT NULL,
    reason      VARCHAR(255)   NOT NULL,
    student_id  CHAR(8)        NOT NULL,
    exam_id     VARCHAR(50)    NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (student_id) REFERENCES users (id),
    FOREIGN KEY (exam_id) REFERENCES exam (id)
);

CREATE TABLE membership (
    id          VARCHAR(50),
    group_id    VARCHAR(50),
    student_id  CHAR(8),
    start_date  DATE  NOT NULL,
    end_date    DATE,
    PRIMARY KEY (id),
    UNIQUE (group_id, student_id),
    FOREIGN KEY (group_id) REFERENCES student_group (id),
    FOREIGN KEY (student_id) REFERENCES users (id)
);

CREATE TABLE group_course (
    id          VARCHAR(50),
    course_id   VARCHAR(50),
    group_id    VARCHAR(50),
    PRIMARY KEY (id),
    UNIQUE (course_id, group_id),
    FOREIGN KEY (course_id) REFERENCES course (id),
    FOREIGN KEY (group_id) REFERENCES student_group (id)
);

CREATE TABLE teacher_course (
    id            VARCHAR(50),
    course_id     VARCHAR(50),
    teacher_id    CHAR(8),
    school_year   SMALLINT  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE (course_id, teacher_id),
    FOREIGN KEY (course_id) REFERENCES course (id),
    FOREIGN KEY (teacher_id) REFERENCES users (id)
);
