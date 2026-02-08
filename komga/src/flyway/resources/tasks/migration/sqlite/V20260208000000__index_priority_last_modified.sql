CREATE INDEX idx__tasks__priority_last_modified
    ON TASK (PRIORITY DESC, LAST_MODIFIED_DATE);

CREATE INDEX idx__tasks__owner
    ON TASK (OWNER);

CREATE INDEX idx__tasks__group_id
    ON TASK (GROUP_ID);
