CREATE TABLE INT_CHANNEL_MESSAGE (
	MESSAGE_ID CHAR(36) NOT NULL,
	GROUP_KEY CHAR(36) NOT NULL,
	CREATED_DATE BIGINT NOT NULL,
	MESSAGE_PRIORITY INT,
	MESSAGE_SEQUENCE BIGINT AUTO_INCREMENT UNIQUE,
	MESSAGE_BYTES BLOB,
	REGION VARCHAR(100) NOT NULL,
	constraint INT_CHANNEL_MESSAGE_PK primary key (GROUP_KEY, MESSAGE_ID, REGION)
) ENGINE=InnoDB;

ALTER TABLE INT_CHANNEL_MESSAGE
ADD INDEX MSG_INDEX_DATE_IDX USING BTREE (CREATED_DATE, MESSAGE_SEQUENCE);

ALTER TABLE INT_CHANNEL_MESSAGE
ADD INDEX MSG_INDEX_PRIORITY_IDX USING BTREE (MESSAGE_PRIORITY DESC, CREATED_DATE, MESSAGE_SEQUENCE);
