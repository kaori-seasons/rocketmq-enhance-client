package org.apache.rocketmq.sdk.api.exactlyonce.datasource;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;

public class SQLUtil {
    public static boolean isReadSql(String sql) {
        SqlType sqlType;
        try {
            sqlType = getSqlType(sql);
        } catch (Exception e) {
            sqlType = null;
        }
        return isReadSqlType(sqlType);
    }

    private static boolean isReadSqlType(SqlType sqlType) {
        return sqlType == SqlType.SELECT || sqlType == SqlType.SHOW || sqlType == SqlType.DESC || sqlType == SqlType.DUMP || sqlType == SqlType.DEBUG || sqlType == SqlType.EXPLAIN || sqlType == SqlType.SELECT_UNION;
    }

    public static boolean isWriteSql(String sql) {
        SqlType sqlType;
        try {
            sqlType = getSqlType(sql);
        } catch (Exception e) {
            sqlType = null;
        }
        return isWriteSqlType(sqlType);
    }

    private static boolean isWriteSqlType(SqlType sqlType) {
        return sqlType == SqlType.INSERT || sqlType == SqlType.DELETE || sqlType == SqlType.UPDATE;
    }

    private static SqlType getSqlType(String sql) throws Exception {
        SqlType sqlType;
        String noCommentsSql = sql.trim();
        if (StringUtils.startsWithIgnoreCase(noCommentsSql, "select")) {
            sqlType = SqlType.SELECT;
        } else if (StringUtils.startsWithIgnoreCase(noCommentsSql, "insert")) {
            sqlType = SqlType.INSERT;
        } else if (StringUtils.startsWithIgnoreCase(noCommentsSql, "update")) {
            sqlType = SqlType.UPDATE;
        } else if (StringUtils.startsWithIgnoreCase(noCommentsSql, "delete")) {
            sqlType = SqlType.DELETE;
        } else if (StringUtils.startsWithIgnoreCase(noCommentsSql, "show")) {
            sqlType = SqlType.SHOW;
        } else if (StringUtils.startsWithIgnoreCase(noCommentsSql, "replace")) {
            sqlType = SqlType.REPLACE;
        } else if (StringUtils.startsWithIgnoreCase(noCommentsSql, "truncate")) {
            sqlType = SqlType.TRUNCATE;
        } else if (StringUtils.startsWithIgnoreCase(noCommentsSql, "create")) {
            sqlType = SqlType.CREATE;
        } else if (StringUtils.startsWithIgnoreCase(noCommentsSql, "drop")) {
            sqlType = SqlType.DROP;
        } else if (StringUtils.startsWithIgnoreCase(noCommentsSql, "load")) {
            sqlType = SqlType.LOAD;
        } else if (StringUtils.startsWithIgnoreCase(noCommentsSql, BeanDefinitionParserDelegate.MERGE_ATTRIBUTE)) {
            sqlType = SqlType.MERGE;
        } else if (StringUtils.startsWithIgnoreCase(noCommentsSql, "alter")) {
            sqlType = SqlType.ALTER;
        } else if (StringUtils.startsWithIgnoreCase(noCommentsSql, "rename")) {
            sqlType = SqlType.RENAME;
        } else if (StringUtils.startsWithIgnoreCase(noCommentsSql, "dump")) {
            sqlType = SqlType.DUMP;
        } else if (StringUtils.startsWithIgnoreCase(noCommentsSql, "debug")) {
            sqlType = SqlType.DEBUG;
        } else if (StringUtils.startsWithIgnoreCase(noCommentsSql, "savepoint")) {
            sqlType = SqlType.SAVE_POINT;
        } else if (StringUtils.startsWithIgnoreCase(noCommentsSql, "release")) {
            sqlType = SqlType.SAVE_POINT;
        } else if (StringUtils.startsWithIgnoreCase(noCommentsSql, "rollback")) {
            sqlType = SqlType.SAVE_POINT;
        } else if (StringUtils.startsWithIgnoreCase(noCommentsSql, "explain")) {
            sqlType = SqlType.EXPLAIN;
        } else if (StringUtils.startsWithIgnoreCase(noCommentsSql, "desc")) {
            sqlType = SqlType.DESC;
        } else if (StringUtils.startsWithIgnoreCase(noCommentsSql, "call")) {
            sqlType = SqlType.PROCEDURE;
        } else if (StringUtils.startsWithIgnoreCase(noCommentsSql, BeanDefinitionParserDelegate.SET_ELEMENT)) {
            sqlType = SqlType.SET;
        } else if (StringUtils.startsWithIgnoreCase(noCommentsSql, "reload")) {
            sqlType = SqlType.RELOAD;
        } else {
            throw new Exception("SqlType is Not Support ," + noCommentsSql);
        }
        return sqlType;
    }

    public enum SqlType {
        SELECT(1),
        INSERT(2),
        UPDATE(3),
        DELETE(4),
        SHOW(5),
        REPLACE(6),
        TRUNCATE(7),
        CREATE(8),
        DROP(9),
        LOAD(10),
        MERGE(11),
        ALTER(12),
        RENAME(13),
        DUMP(14),
        DEBUG(15),
        SAVE_POINT(16),
        EXPLAIN(17),
        DESC(18),
        PROCEDURE(19),
        SET(20),
        SELECT_UNION(21),
        RELOAD(22);
        
        private int code;

        SqlType(int code) {
            this.code = code;
        }
    }
}
