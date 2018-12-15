package ru.kspt.highload.dao;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.h2.jdbcx.JdbcConnectionPool;
import org.jooq.DSLContext;
import org.jooq.Record1;
import org.jooq.Result;
import org.jooq.SQLDialect;

import static org.jooq.impl.DSL.*;

@RequiredArgsConstructor
@Accessors(fluent = true)
class H2Bridge {
    private final static String DB_INIT_SCRIPT_PATH = "src/main/resources/db_init.sql";

    private final static String TABLE_NAME = "storage";

    private final static String KEY_HASH_COLUMN = "keyHash";

    private final static String KEY_BYTES_COLUMN = "keyBytes";

    private final static String VALUE_BYTES_COLUMN = "valueBytes";

    private final String dbFilesDirectory;

    @Getter(value = AccessLevel.PRIVATE, lazy = true)
    private final JdbcConnectionPool connectionPool =
            JdbcConnectionPool.create(makeH2ConnectionString(), "sa", "sa");

    private String makeH2ConnectionString() {
        return "jdbc:h2:file:" + dbFilesDirectory + "/kvstorage;"
                + "INIT=RUNSCRIPT FROM '" + DB_INIT_SCRIPT_PATH + "'";
    }

    void closeConnection() {
        connectionPool().dispose();
    }

    private DSLContext sql() {
        return using(connectionPool(), SQLDialect.H2);
    }

    void insert(final Key key, final byte[] value) {
        sql().insertInto(table(TABLE_NAME))
                .set(field(KEY_HASH_COLUMN), key.getHash())
                .set(field(KEY_BYTES_COLUMN), key.getBytes())
                .set(field(VALUE_BYTES_COLUMN), value)
                .execute();
    }

    void update(final Key key, final byte[] value) {
        sql().update(table(TABLE_NAME))
                .set(field(KEY_HASH_COLUMN), key.getHash())
                .set(field(KEY_BYTES_COLUMN), key.getBytes())
                .set(field(VALUE_BYTES_COLUMN), value)
                .where(field(KEY_HASH_COLUMN).eq(key.getHash())
                        .and(field(KEY_BYTES_COLUMN).eq(key.getBytes())))
                .execute();
    }

    byte[] get(final Key key) {
        final Result<Record1<Object>> result = sql().select(field(VALUE_BYTES_COLUMN))
                .from(table(TABLE_NAME))
                .where(field(KEY_BYTES_COLUMN).eq(key.getBytes()))
                .fetch();
        return (byte[]) result.getValue(0, 0);
    }

    void remove(final Key key) {
        sql().deleteFrom(table(TABLE_NAME))
                .where(field(KEY_BYTES_COLUMN).eq(key.getBytes()))
                .execute();
    }

    boolean contains(final Key key) {
        if (countEqualHashes(key.getHash()) == 0) {
            return false;
        } else return countEqualKeys(key) == 1;
    }

    private int countEqualHashes(final int hash) {
        final Result<Record1<Integer>> equalsHashesCount = sql().select(count())
                .from(table(TABLE_NAME))
                .where(field(KEY_HASH_COLUMN).eq(hash)).fetch();
        return (Integer) equalsHashesCount.getValue(0, 0);
    }

    private int countEqualKeys(final Key key) {
        final Result<Record1<Integer>> equalsKeysCount = sql().select(count())
                .from(table(TABLE_NAME))
                .where(field(KEY_BYTES_COLUMN).eq(key.getBytes())).fetch();
        return (Integer) equalsKeysCount.getValue(0, 0);
    }
}
