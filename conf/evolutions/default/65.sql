# --- !Ups
CREATE TABLE language_inspection (
  id             BIGINT      NOT NULL,
  exam_id        BIGINT      NOT NULL,
  assignee_id    BIGINT,
  created        TIMESTAMPTZ NOT NULL,
  creator_id     BIGINT      NOT NULL,
  started_at     TIMESTAMPTZ,
  finished_at    TIMESTAMPTZ,
  approved       BOOLEAN,
  statement_id   BIGINT,
  object_version BIGINT,
  CONSTRAINT PK_LANG_INSPECTION PRIMARY KEY (id)
);
ALTER TABLE language_inspection ADD CONSTRAINT fk_lang_inspection_exam FOREIGN KEY (exam_id) REFERENCES exam (id);
ALTER TABLE language_inspection ADD CONSTRAINT fk_lang_inspection_assignee FOREIGN KEY (assignee_id) REFERENCES app_user (id);
ALTER TABLE language_inspection ADD CONSTRAINT fk_lang_inspection_creator FOREIGN KEY (creator_id) REFERENCES app_user (id);
ALTER TABLE language_inspection ADD CONSTRAINT fk_lang_inspection_statement FOREIGN KEY (statement_id) REFERENCES comment (id);

CREATE SEQUENCE language_inspection_seq;

-- ADD MISSING INDICES
CREATE INDEX IX_MULTIPLE_CHOICE_OPTION_ANSWER ON multiple_choice_option (answer_id);
CREATE INDEX IX_EXAM_SECTION_QUESTION_SECTION ON exam_section_question (exam_section_id);
CREATE INDEX IX_EXAM_SECTION_QUESTION_QUESTION ON exam_section_question (question_id);
CREATE INDEX IX_EXAM_EXECUTION_TYPE ON exam (execution_type_id);
CREATE INDEX IX_EXAM_GRADE_SCALE ON exam (grade_scale_id);
CREATE INDEX IX_EXAM_GRADE ON exam (grade_id);
CREATE INDEX IX_EXAM_OWNER_USER ON exam_owner (user_id);
CREATE INDEX IX_EXAM_OWNER_EXAM ON exam_owner (exam_id);
CREATE INDEX IX_EXAM_LANGUAGE_LANGUAGE ON exam_language (language_code);
CREATE INDEX IX_EXAM_LANGUAGE_EXAM ON exam_language (exam_id);
CREATE INDEX IX_COMMENT_ATTACHMENT ON comment (attachment_id);
CREATE INDEX IX_QUESTION_TAG_QUESTION ON question_tag (question_id);
CREATE INDEX IX_QUESTION_TAG_TAG ON question_tag (tag_id);
CREATE INDEX IX_EXAM_PARTICIPATION_RESERVATION ON exam_participation (reservation_id);
CREATE INDEX IX_TAG_CREATOR ON tag (creator_id);
CREATE INDEX IX_TAG_MODIFIER ON tag (modifier_id);
CREATE INDEX IX_LANGUAGE_INSPECTION_EXAM ON language_inspection (exam_id);
CREATE INDEX IX_LANGUAGE_INSPECTION_ASSIGNEE ON language_inspection (assignee_id);
CREATE INDEX IX_LANGUAGE_INSPECTION_CREATOR ON language_inspection (creator_id);
CREATE INDEX IX_LANGUAGE_INSPECTION_STATEMENT ON language_inspection (statement_id);

CREATE INDEX IX_EXAM_STARTING_HOUR_ROOM ON exam_starting_hour (room_id);
CREATE INDEX IX_COURSE_GRADE_SCALE ON course (grade_scale_id);
CREATE INDEX IX_EXAM_SOFTWARE_EXAM ON exam_software (exam_id);
CREATE INDEX IX_EXAM_SOFTWARE_SOFTWARE ON exam_software (software_id);
CREATE INDEX IX_APP_USER_ROLE_ROLE ON app_user_role (role_id);
CREATE INDEX IX_APP_USER_ROLE_USER ON app_user_role (app_user_id);
CREATE INDEX IX_EXAM_MACHINE_SOFTWARE_MACHINE ON exam_machine_software (exam_machine_id);
CREATE INDEX IX_EXAM_MACHINE_SOFTWARE_SOFTWARE ON exam_machine_software (software_id);
CREATE INDEX IX_APP_USER_LANGUAGE ON app_user (language_id);
CREATE INDEX IX_ACCESSIBILITY_EXAM_ROOM_ACCESSIBILITY ON accessibility_exam_room (accessibility_id);
CREATE INDEX IX_ACCESSIBILITY_EXAM_ROOM_ROOM ON accessibility_exam_room (exam_room_id);
CREATE INDEX IX_ORGANISATION_PARENT ON organisation (parent_id);
CREATE INDEX IX_APP_USER_PERMISSION_PERMISSION ON app_user_permission (permission_id);
CREATE INDEX IX_APP_USER_PERMISSION_USER ON app_user_permission (app_user_id);


# --- !Downs
DROP INDEX IX_MULTIPLE_CHOICE_OPTION_ANSWER;
DROP INDEX IX_EXAM_SECTION_QUESTION_SECTION;
DROP INDEX IX_EXAM_SECTION_QUESTION_QUESTION;
DROP INDEX IX_EXAM_EXECUTION_TYPE;
DROP INDEX IX_EXAM_GRADE_SCALE;
DROP INDEX IX_EXAM_GRADE;
DROP INDEX IX_EXAM_OWNER_USER;
DROP INDEX IX_EXAM_OWNER_EXAM;
DROP INDEX IX_EXAM_LANGUAGE_LANGUAGE;
DROP INDEX IX_EXAM_LANGUAGE_EXAM;
DROP INDEX IX_COMMENT_ATTACHMENT;
DROP INDEX IX_QUESTION_TAG_QUESTION;
DROP INDEX IX_QUESTION_TAG_TAG;
DROP INDEX IX_EXAM_PARTICIPATION_RESERVATION;
DROP INDEX IX_TAG_CREATOR;
DROP INDEX IX_TAG_MODIFIER;
DROP INDEX IX_LANGUAGE_INSPECTION_EXAM;
DROP INDEX IX_LANGUAGE_INSPECTION_ASSIGNEE;
DROP INDEX IX_LANGUAGE_INSPECTION_CREATOR;
DROP INDEX IX_LANGUAGE_INSPECTION_STATEMENT;
DROP INDEX IX_EXAM_STARTING_HOUR_ROOM;
DROP INDEX IX_COURSE_GRADE_SCALE;
DROP INDEX IX_EXAM_SOFTWARE_EXAM;
DROP INDEX IX_EXAM_SOFTWARE_SOFTWARE;
DROP INDEX IX_APP_USER_ROLE_ROLE;
DROP INDEX IX_APP_USER_ROLE_USER;
DROP INDEX IX_EXAM_MACHINE_SOFTWARE_MACHINE;
DROP INDEX IX_EXAM_MACHINE_SOFTWARE_SOFTWARE;
DROP INDEX IX_APP_USER_LANGUAGE;
DROP INDEX IX_ACCESSIBILITY_EXAM_ROOM_ACCESSIBILITY;
DROP INDEX IX_ACCESSIBILITY_EXAM_ROOM_ROOM;
DROP INDEX IX_ORGANISATION_PARENT;
DROP INDEX IX_APP_USER_PERMISSION_PERMISSION;
DROP INDEX IX_APP_USER_PERMISSION_USER;

DROP TABLE IF EXISTS language_inspection CASCADE;
DROP SEQUENCE language_inspection_seq;