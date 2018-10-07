CREATE TABLE IF NOT EXISTS storage(
    keyHash     INT     NOT NULL,
    keyBytes    BINARY  PRIMARY KEY,
    valueBytes  BLOB    NOT NULL,
);

CREATE INDEX IF NOT EXISTS hashIndex ON storage(keyHash);