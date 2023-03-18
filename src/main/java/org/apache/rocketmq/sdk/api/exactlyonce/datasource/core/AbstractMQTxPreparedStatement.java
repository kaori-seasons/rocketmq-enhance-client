package org.apache.rocketmq.sdk.api.exactlyonce.datasource.core;

import org.apache.rocketmq.sdk.api.exactlyonce.aop.model.LocalTxContext;
import org.apache.rocketmq.sdk.api.exactlyonce.aop.model.MQTxContext;
import org.apache.rocketmq.sdk.api.exactlyonce.datasource.SQLUtil;
import org.apache.rocketmq.sdk.api.exactlyonce.manager.MetricService;
import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;

public abstract class AbstractMQTxPreparedStatement extends AbstractMQTxStatement implements PreparedStatement {
    public AbstractMQTxPreparedStatement(AbstractMQTxDataSource dataSource, MQTxConnection connection, PreparedStatement preparedStatement, String sql) {
        super(dataSource, connection, preparedStatement);
        this.targetSql = sql;
    }

    public ResultSet executeQuery() throws SQLException {
        return ((PreparedStatement)this.targetStatement).executeQuery();
    }

    public void setNull(int parameterIndex, int sqlType) throws SQLException {
        ((PreparedStatement)this.targetStatement).setNull(parameterIndex, sqlType);
    }

    public void setBoolean(int parameterIndex, boolean x) throws SQLException {
        ((PreparedStatement)this.targetStatement).setBoolean(parameterIndex, x);
    }

    public void setByte(int parameterIndex, byte x) throws SQLException {
        ((PreparedStatement)this.targetStatement).setByte(parameterIndex, x);
    }

    public void setShort(int parameterIndex, short x) throws SQLException {
        ((PreparedStatement)this.targetStatement).setShort(parameterIndex, x);
    }

    public void setInt(int parameterIndex, int x) throws SQLException {
        ((PreparedStatement)this.targetStatement).setInt(parameterIndex, x);
    }

    public void setLong(int parameterIndex, long x) throws SQLException {
        ((PreparedStatement)this.targetStatement).setLong(parameterIndex, x);
    }

    public void setFloat(int parameterIndex, float x) throws SQLException {
        ((PreparedStatement)this.targetStatement).setFloat(parameterIndex, x);
    }

    public void setDouble(int parameterIndex, double x) throws SQLException {
        ((PreparedStatement)this.targetStatement).setDouble(parameterIndex, x);
    }

    public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException {
        ((PreparedStatement)this.targetStatement).setBigDecimal(parameterIndex, x);
    }

    public void setString(int parameterIndex, String x) throws SQLException {
        ((PreparedStatement)this.targetStatement).setString(parameterIndex, x);
    }

    public void setBytes(int parameterIndex, byte[] x) throws SQLException {
        ((PreparedStatement)this.targetStatement).setBytes(parameterIndex, x);
    }

    public void setDate(int parameterIndex, Date x) throws SQLException {
        ((PreparedStatement)this.targetStatement).setDate(parameterIndex, x);
    }

    public void setTime(int parameterIndex, Time x) throws SQLException {
        ((PreparedStatement)this.targetStatement).setTime(parameterIndex, x);
    }

    public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException {
        ((PreparedStatement)this.targetStatement).setTimestamp(parameterIndex, x);
    }

    public void setAsciiStream(int parameterIndex, InputStream x, int length) throws SQLException {
        ((PreparedStatement)this.targetStatement).setAsciiStream(parameterIndex, x, length);
    }

    /** @deprecated */
    public void setUnicodeStream(int parameterIndex, InputStream x, int length) throws SQLException {
        ((PreparedStatement)this.targetStatement).setUnicodeStream(parameterIndex, x, length);
    }

    public void setBinaryStream(int parameterIndex, InputStream x, int length) throws SQLException {
        ((PreparedStatement)this.targetStatement).setBinaryStream(parameterIndex, x, length);
    }

    public void clearParameters() throws SQLException {
        ((PreparedStatement)this.targetStatement).clearParameters();
    }

    public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException {
        ((PreparedStatement)this.targetStatement).setObject(parameterIndex, x, targetSqlType);
    }

    public void setObject(int parameterIndex, Object x) throws SQLException {
        ((PreparedStatement)this.targetStatement).setObject(parameterIndex, x);
    }

    public boolean execute() throws SQLException {
        MQTxContext context = LocalTxContext.get();
        if (context != null && context.isInTxEnv()) {
            if (SQLUtil.isReadSql(this.targetSql)) {
                this.executeQuery();
                return true;
            }

            if (SQLUtil.isWriteSql(this.targetSql)) {
                MetricService.getInstance().incWrite();
                ExecuterFactory.execute(this, (String)null, SQLExecuteType.PREPARED_EXECUTE_VOID, (Object)null);
                return false;
            }
        }

        return ((PreparedStatement)this.targetStatement).execute();
    }

    public int executeUpdate() throws SQLException {
        MQTxContext context = LocalTxContext.get();
        if (context != null && context.isInTxEnv() && SQLUtil.isWriteSql(this.targetSql)) {
            MetricService.getInstance().incWrite();
            ExecResult<Integer> result = ExecuterFactory.execute(this, (String)null, SQLExecuteType.PREPARED_EXECUTE_VOID, (Object)null);
            return (Integer)result.getResult();
        } else {
            return ((PreparedStatement)this.targetStatement).executeUpdate();
        }
    }

    public void addBatch() throws SQLException {
        ((PreparedStatement)this.targetStatement).addBatch();
    }

    public int[] executeBatch() throws SQLException {
        MQTxContext context = LocalTxContext.get();
        if (context != null && context.isInTxEnv()) {
            MetricService.getInstance().incWrite();
            ExecResult<int[]> result = ExecuterFactory.execute(this, (String)null, SQLExecuteType.PREPARED_EXECUTEBATCH_VOID, (Object)null);
            return (int[])result.getResult();
        } else {
            return ((PreparedStatement)this.targetStatement).executeBatch();
        }
    }

    public void setCharacterStream(int parameterIndex, Reader reader, int length) throws SQLException {
        ((PreparedStatement)this.targetStatement).setCharacterStream(parameterIndex, reader, length);
    }

    public void setRef(int parameterIndex, Ref x) throws SQLException {
        ((PreparedStatement)this.targetStatement).setRef(parameterIndex, x);
    }

    public void setBlob(int parameterIndex, Blob x) throws SQLException {
        ((PreparedStatement)this.targetStatement).setBlob(parameterIndex, x);
    }

    public void setClob(int parameterIndex, Clob x) throws SQLException {
        ((PreparedStatement)this.targetStatement).setClob(parameterIndex, x);
    }

    public void setArray(int parameterIndex, Array x) throws SQLException {
        ((PreparedStatement)this.targetStatement).setArray(parameterIndex, x);
    }

    public ResultSetMetaData getMetaData() throws SQLException {
        return ((PreparedStatement)this.targetStatement).getMetaData();
    }

    public void setDate(int parameterIndex, Date x, Calendar cal) throws SQLException {
        ((PreparedStatement)this.targetStatement).setDate(parameterIndex, x, cal);
    }

    public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException {
        ((PreparedStatement)this.targetStatement).setTime(parameterIndex, x, cal);
    }

    public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException {
        ((PreparedStatement)this.targetStatement).setTimestamp(parameterIndex, x, cal);
    }

    public void setNull(int parameterIndex, int sqlType, String typeName) throws SQLException {
        ((PreparedStatement)this.targetStatement).setNull(parameterIndex, sqlType, typeName);
    }

    public void setURL(int parameterIndex, URL x) throws SQLException {
        ((PreparedStatement)this.targetStatement).setURL(parameterIndex, x);
    }

    public ParameterMetaData getParameterMetaData() throws SQLException {
        return ((PreparedStatement)this.targetStatement).getParameterMetaData();
    }

    public void setRowId(int parameterIndex, RowId x) throws SQLException {
        ((PreparedStatement)this.targetStatement).setRowId(parameterIndex, x);
    }

    public void setNString(int parameterIndex, String value) throws SQLException {
        ((PreparedStatement)this.targetStatement).setNString(parameterIndex, value);
    }

    public void setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException {
        ((PreparedStatement)this.targetStatement).setNCharacterStream(parameterIndex, value, length);
    }

    public void setNClob(int parameterIndex, NClob value) throws SQLException {
        ((PreparedStatement)this.targetStatement).setNClob(parameterIndex, value);
    }

    public void setClob(int parameterIndex, Reader reader, long length) throws SQLException {
        ((PreparedStatement)this.targetStatement).setClob(parameterIndex, reader, length);
    }

    public void setBlob(int parameterIndex, InputStream inputStream, long length) throws SQLException {
        ((PreparedStatement)this.targetStatement).setBlob(parameterIndex, inputStream, length);
    }

    public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException {
        ((PreparedStatement)this.targetStatement).setNClob(parameterIndex, reader, length);
    }

    public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {
        ((PreparedStatement)this.targetStatement).setSQLXML(parameterIndex, xmlObject);
    }

    public void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength) throws SQLException {
        ((PreparedStatement)this.targetStatement).setObject(parameterIndex, x, targetSqlType, scaleOrLength);
    }

    public void setAsciiStream(int parameterIndex, InputStream x, long length) throws SQLException {
        ((PreparedStatement)this.targetStatement).setAsciiStream(parameterIndex, x, length);
    }

    public void setBinaryStream(int parameterIndex, InputStream x, long length) throws SQLException {
        ((PreparedStatement)this.targetStatement).setBinaryStream(parameterIndex, x, length);
    }

    public void setCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException {
        ((PreparedStatement)this.targetStatement).setCharacterStream(parameterIndex, reader, length);
    }

    public void setAsciiStream(int parameterIndex, InputStream x) throws SQLException {
        ((PreparedStatement)this.targetStatement).setAsciiStream(parameterIndex, x);
    }

    public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException {
        ((PreparedStatement)this.targetStatement).setBinaryStream(parameterIndex, x);
    }

    public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException {
        ((PreparedStatement)this.targetStatement).setCharacterStream(parameterIndex, reader);
    }

    public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException {
        ((PreparedStatement)this.targetStatement).setNCharacterStream(parameterIndex, value);
    }

    public void setClob(int parameterIndex, Reader reader) throws SQLException {
        ((PreparedStatement)this.targetStatement).setClob(parameterIndex, reader);
    }

    public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException {
        ((PreparedStatement)this.targetStatement).setBlob(parameterIndex, inputStream);
    }

    public void setNClob(int parameterIndex, Reader reader) throws SQLException {
        ((PreparedStatement)this.targetStatement).setNClob(parameterIndex, reader);
    }
}
