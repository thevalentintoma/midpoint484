-- resource templates/inheritance
ALTER TABLE m_resource ADD template NUMBER(1, 0);

-- WRITE CHANGES ABOVE ^^
UPDATE m_global_metadata SET value = '4.6' WHERE name = 'databaseSchemaVersion';
COMMIT;
