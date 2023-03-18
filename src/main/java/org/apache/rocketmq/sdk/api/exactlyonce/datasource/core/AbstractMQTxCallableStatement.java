package org.apache.rocketmq.sdk.api.exactlyonce.datasource.core;

import org.apache.rocketmq.sdk.api.exactlyonce.MQUnsupportException;
import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Map;

public class AbstractMQTxCallableStatement extends AbstractMQTxPreparedStatement implements CallableStatement {
    public AbstractMQTxCallableStatement(AbstractMQTxDataSource dataSource, MQTxConnection connection, PreparedStatement preparedStatement, String sql) {
        super(dataSource, connection, preparedStatement, sql);
    }

    @Override
    public void registerOutParameter(int parameterIndex, int sqlType) throws SQLException {
        ((CallableStatement) this.targetStatement).registerOutParameter(parameterIndex, sqlType);
    }

    @Override
    public void registerOutParameter(int parameterIndex, int sqlType, int scale) throws SQLException {
        ((CallableStatement) this.targetStatement).registerOutParameter(parameterIndex, sqlType, scale);
    }

    @Override
    public boolean wasNull() throws SQLException {
        return ((CallableStatement) this.targetStatement).wasNull();
    }

    @Override
    public String getString(int parameterIndex) throws SQLException {
        return ((CallableStatement) this.targetStatement).getString(parameterIndex);
    }

    @Override
    public boolean getBoolean(int parameterIndex) throws SQLException {
        return ((CallableStatement) this.targetStatement).getBoolean(parameterIndex);
    }

    @Override
    public byte getByte(int parameterIndex) throws SQLException {
        return ((CallableStatement) this.targetStatement).getByte(parameterIndex);
    }

    @Override
    public short getShort(int parameterIndex) throws SQLException {
        return ((CallableStatement) this.targetStatement).getShort(parameterIndex);
    }

    @Override
    public int getInt(int parameterIndex) throws SQLException {
        return ((CallableStatement) this.targetStatement).getInt(parameterIndex);
    }

    @Override
    public long getLong(int parameterIndex) throws SQLException {
        return ((CallableStatement) this.targetStatement).getLong(parameterIndex);
    }

    @Override
    public float getFloat(int parameterIndex) throws SQLException {
        return ((CallableStatement) this.targetStatement).getFloat(parameterIndex);
    }

    @Override
    public double getDouble(int parameterIndex) throws SQLException {
        return ((CallableStatement) this.targetStatement).getDouble(parameterIndex);
    }

    @Override
    public BigDecimal getBigDecimal(int parameterIndex, int scale) throws SQLException {
        return ((CallableStatement) this.targetStatement).getBigDecimal(parameterIndex);
    }

    @Override
    public byte[] getBytes(int parameterIndex) throws SQLException {
        return ((CallableStatement) this.targetStatement).getBytes(parameterIndex);
    }

    @Override
    public Date getDate(int parameterIndex) throws SQLException {
        return ((CallableStatement) this.targetStatement).getDate(parameterIndex);
    }

    @Override
    public Time getTime(int parameterIndex) throws SQLException {
        return ((CallableStatement) this.targetStatement).getTime(parameterIndex);
    }

    @Override
    public Timestamp getTimestamp(int parameterIndex) throws SQLException {
        return ((CallableStatement) this.targetStatement).getTimestamp(parameterIndex);
    }

    @Override
    public Object getObject(int parameterIndex) throws SQLException {
        return ((CallableStatement) this.targetStatement).getObject(parameterIndex);
    }

    @Override
    public BigDecimal getBigDecimal(int parameterIndex) throws SQLException {
        return ((CallableStatement) this.targetStatement).getBigDecimal(parameterIndex);
    }

    @Override
    public Object getObject(int parameterIndex, Map<String, Class<?>> map) throws SQLException {
        return ((CallableStatement) this.targetStatement).getObject(parameterIndex);
    }

    @Override
    public Ref getRef(int parameterIndex) throws SQLException {
        return ((CallableStatement) this.targetStatement).getRef(parameterIndex);
    }

    @Override
    public Blob getBlob(int parameterIndex) throws SQLException {
        return ((CallableStatement) this.targetStatement).getBlob(parameterIndex);
    }

    @Override
    public Clob getClob(int parameterIndex) throws SQLException {
        return ((CallableStatement) this.targetStatement).getClob(parameterIndex);
    }

    @Override
    public Array getArray(int parameterIndex) throws SQLException {
        return ((CallableStatement) this.targetStatement).getArray(parameterIndex);
    }

    @Override
    public Date getDate(int parameterIndex, Calendar cal) throws SQLException {
        return ((CallableStatement) this.targetStatement).getDate(parameterIndex, cal);
    }

    @Override
    public Time getTime(int parameterIndex, Calendar cal) throws SQLException {
        return ((CallableStatement) this.targetStatement).getTime(parameterIndex, cal);
    }

    @Override
    public Timestamp getTimestamp(int parameterIndex, Calendar cal) throws SQLException {
        return ((CallableStatement) this.targetStatement).getTimestamp(parameterIndex, cal);
    }

    @Override
    public void registerOutParameter(int parameterIndex, int sqlType, String typeName) throws SQLException {
        ((CallableStatement) this.targetStatement).registerOutParameter(parameterIndex, sqlType, typeName);
    }

    @Override
    public void registerOutParameter(String parameterName, int sqlType) throws SQLException {
        ((CallableStatement) this.targetStatement).registerOutParameter(parameterName, sqlType);
    }

    @Override
    public void registerOutParameter(String parameterName, int sqlType, int scale) throws SQLException {
        ((CallableStatement) this.targetStatement).registerOutParameter(parameterName, sqlType, scale);
    }

    @Override
    public void registerOutParameter(String parameterName, int sqlType, String typeName) throws SQLException {
        ((CallableStatement) this.targetStatement).registerOutParameter(parameterName, sqlType, typeName);
    }

    @Override
    public URL getURL(int parameterIndex) throws SQLException {
        return ((CallableStatement) this.targetStatement).getURL(parameterIndex);
    }

    @Override
    public void setURL(String parameterName, URL val) throws SQLException {
        ((CallableStatement) this.targetStatement).setURL(parameterName, val);
    }

    @Override
    public void setNull(String parameterName, int sqlType) throws SQLException {
        ((CallableStatement) this.targetStatement).setNull(parameterName, sqlType);
    }

    @Override
    public void setBoolean(String parameterName, boolean x) throws SQLException {
        ((CallableStatement) this.targetStatement).setBoolean(parameterName, x);
    }

    @Override
    public void setByte(String parameterName, byte x) throws SQLException {
        ((CallableStatement) this.targetStatement).setByte(parameterName, x);
    }

    @Override
    public void setShort(String parameterName, short x) throws SQLException {
        ((CallableStatement) this.targetStatement).setShort(parameterName, x);
    }

    @Override
    public void setInt(String parameterName, int x) throws SQLException {
        ((CallableStatement) this.targetStatement).setInt(parameterName, x);
    }

    @Override
    public void setLong(String parameterName, long x) throws SQLException {
        ((CallableStatement) this.targetStatement).setLong(parameterName, x);
    }

    @Override
    public void setFloat(String parameterName, float x) throws SQLException {
        ((CallableStatement) this.targetStatement).setFloat(parameterName, x);
    }

    @Override
    public void setDouble(String parameterName, double x) throws SQLException {
        ((CallableStatement) this.targetStatement).setDouble(parameterName, x);
    }

    @Override
    public void setBigDecimal(String parameterName, BigDecimal x) throws SQLException {
        ((CallableStatement) this.targetStatement).setBigDecimal(parameterName, x);
    }

    @Override
    public void setString(String parameterName, String x) throws SQLException {
        ((CallableStatement) this.targetStatement).setString(parameterName, x);
    }

    @Override
    public void setBytes(String parameterName, byte[] x) throws SQLException {
        ((CallableStatement) this.targetStatement).setBytes(parameterName, x);
    }

    @Override
    public void setDate(String parameterName, Date x) throws SQLException {
        ((CallableStatement) this.targetStatement).setDate(parameterName, x);
    }

    @Override
    public void setTime(String parameterName, Time x) throws SQLException {
        ((CallableStatement) this.targetStatement).setTime(parameterName, x);
    }

    @Override
    public void setTimestamp(String parameterName, Timestamp x) throws SQLException {
        ((CallableStatement) this.targetStatement).setTimestamp(parameterName, x);
    }

    @Override
    public void setAsciiStream(String parameterName, InputStream x, int length) throws SQLException {
        ((CallableStatement) this.targetStatement).setAsciiStream(parameterName, x, length);
    }

    @Override
    public void setBinaryStream(String parameterName, InputStream x, int length) throws SQLException {
        ((CallableStatement) this.targetStatement).setBinaryStream(parameterName, x, length);
    }

    @Override
    public void setObject(String parameterName, Object x, int targetSqlType, int scale) throws SQLException {
        ((CallableStatement) this.targetStatement).setObject(parameterName, x, targetSqlType, scale);
    }

    @Override
    public void setObject(String parameterName, Object x, int targetSqlType) throws SQLException {
        ((CallableStatement) this.targetStatement).setObject(parameterName, x, targetSqlType);
    }

    @Override
    public void setObject(String parameterName, Object x) throws SQLException {
        ((CallableStatement) this.targetStatement).setObject(parameterName, x);
    }

    @Override
    public void setCharacterStream(String parameterName, Reader reader, int length) throws SQLException {
        ((CallableStatement) this.targetStatement).setCharacterStream(parameterName, reader, length);
    }

    @Override
    public void setDate(String parameterName, Date x, Calendar cal) throws SQLException {
        ((CallableStatement) this.targetStatement).setDate(parameterName, x, cal);
    }

    @Override
    public void setTime(String parameterName, Time x, Calendar cal) throws SQLException {
        ((CallableStatement) this.targetStatement).setTime(parameterName, x, cal);
    }

    @Override
    public void setTimestamp(String parameterName, Timestamp x, Calendar cal) throws SQLException {
        ((CallableStatement) this.targetStatement).setTimestamp(parameterName, x, cal);
    }

    @Override
    public void setNull(String parameterName, int sqlType, String typeName) throws SQLException {
        ((CallableStatement) this.targetStatement).setNull(parameterName, sqlType, typeName);
    }

    @Override
    public String getString(String parameterName) throws SQLException {
        return ((CallableStatement) this.targetStatement).getString(parameterName);
    }

    @Override
    public boolean getBoolean(String parameterName) throws SQLException {
        return ((CallableStatement) this.targetStatement).getBoolean(parameterName);
    }

    @Override
    public byte getByte(String parameterName) throws SQLException {
        return ((CallableStatement) this.targetStatement).getByte(parameterName);
    }

    @Override
    public short getShort(String parameterName) throws SQLException {
        return ((CallableStatement) this.targetStatement).getShort(parameterName);
    }

    @Override
    public int getInt(String parameterName) throws SQLException {
        return ((CallableStatement) this.targetStatement).getInt(parameterName);
    }

    @Override
    public long getLong(String parameterName) throws SQLException {
        return ((CallableStatement) this.targetStatement).getLong(parameterName);
    }

    @Override
    public float getFloat(String parameterName) throws SQLException {
        return ((CallableStatement) this.targetStatement).getFloat(parameterName);
    }

    @Override
    public double getDouble(String parameterName) throws SQLException {
        return ((CallableStatement) this.targetStatement).getDouble(parameterName);
    }

    @Override
    public byte[] getBytes(String parameterName) throws SQLException {
        return ((CallableStatement) this.targetStatement).getBytes(parameterName);
    }

    @Override
    public Date getDate(String parameterName) throws SQLException {
        return ((CallableStatement) this.targetStatement).getDate(parameterName);
    }

    @Override
    public Time getTime(String parameterName) throws SQLException {
        return ((CallableStatement) this.targetStatement).getTime(parameterName);
    }

    @Override
    public Timestamp getTimestamp(String parameterName) throws SQLException {
        return ((CallableStatement) this.targetStatement).getTimestamp(parameterName);
    }

    @Override
    public Object getObject(String parameterName) throws SQLException {
        return ((CallableStatement) this.targetStatement).getObject(parameterName);
    }

    @Override
    public BigDecimal getBigDecimal(String parameterName) throws SQLException {
        return ((CallableStatement) this.targetStatement).getBigDecimal(parameterName);
    }

    @Override
    public Object getObject(String parameterName, Map<String, Class<?>> map) throws SQLException {
        return ((CallableStatement) this.targetStatement).getObject(parameterName, map);
    }

    @Override
    public Ref getRef(String parameterName) throws SQLException {
        return ((CallableStatement) this.targetStatement).getRef(parameterName);
    }

    @Override
    public Blob getBlob(String parameterName) throws SQLException {
        return ((CallableStatement) this.targetStatement).getBlob(parameterName);
    }

    @Override
    public Clob getClob(String parameterName) throws SQLException {
        return ((CallableStatement) this.targetStatement).getClob(parameterName);
    }

    @Override
    public Array getArray(String parameterName) throws SQLException {
        return ((CallableStatement) this.targetStatement).getArray(parameterName);
    }

    @Override
    public Date getDate(String parameterName, Calendar cal) throws SQLException {
        return ((CallableStatement) this.targetStatement).getDate(parameterName);
    }

    @Override
    public Time getTime(String parameterName, Calendar cal) throws SQLException {
        return ((CallableStatement) this.targetStatement).getTime(parameterName);
    }

    @Override
    public Timestamp getTimestamp(String parameterName, Calendar cal) throws SQLException {
        return ((CallableStatement) this.targetStatement).getTimestamp(parameterName);
    }

    @Override
    public URL getURL(String parameterName) throws SQLException {
        return ((CallableStatement) this.targetStatement).getURL(parameterName);
    }

    @Override
    public RowId getRowId(int parameterIndex) throws SQLException {
        return ((CallableStatement) this.targetStatement).getRowId(parameterIndex);
    }

    @Override
    public RowId getRowId(String parameterName) throws SQLException {
        return ((CallableStatement) this.targetStatement).getRowId(parameterName);
    }

    @Override
    public void setRowId(String parameterName, RowId x) throws SQLException {
        ((CallableStatement) this.targetStatement).setRowId(parameterName, x);
    }

    @Override
    public void setNString(String parameterName, String value) throws SQLException {
        ((CallableStatement) this.targetStatement).setNString(parameterName, value);
    }

    @Override
    public void setNCharacterStream(String parameterName, Reader value, long length) throws SQLException {
        ((CallableStatement) this.targetStatement).setNCharacterStream(parameterName, value, length);
    }

    @Override
    public void setNClob(String parameterName, NClob value) throws SQLException {
        ((CallableStatement) this.targetStatement).setNClob(parameterName, value);
    }

    @Override
    public void setClob(String parameterName, Reader reader, long length) throws SQLException {
        ((CallableStatement) this.targetStatement).setClob(parameterName, reader, length);
    }

    @Override
    public void setBlob(String parameterName, InputStream inputStream, long length) throws SQLException {
        ((CallableStatement) this.targetStatement).setBlob(parameterName, inputStream, length);
    }

    @Override
    public void setNClob(String parameterName, Reader reader, long length) throws SQLException {
        ((CallableStatement) this.targetStatement).setNClob(parameterName, reader, length);
    }

    @Override
    public NClob getNClob(int parameterIndex) throws SQLException {
        return ((CallableStatement) this.targetStatement).getNClob(parameterIndex);
    }

    @Override
    public NClob getNClob(String parameterName) throws SQLException {
        return ((CallableStatement) this.targetStatement).getNClob(parameterName);
    }

    @Override
    public void setSQLXML(String parameterName, SQLXML xmlObject) throws SQLException {
        ((CallableStatement) this.targetStatement).setSQLXML(parameterName, xmlObject);
    }

    @Override
    public SQLXML getSQLXML(int parameterIndex) throws SQLException {
        return ((CallableStatement) this.targetStatement).getSQLXML(parameterIndex);
    }

    @Override
    public SQLXML getSQLXML(String parameterName) throws SQLException {
        return ((CallableStatement) this.targetStatement).getSQLXML(parameterName);
    }

    @Override
    public String getNString(int parameterIndex) throws SQLException {
        return ((CallableStatement) this.targetStatement).getNString(parameterIndex);
    }

    @Override
    public String getNString(String parameterName) throws SQLException {
        return ((CallableStatement) this.targetStatement).getNString(parameterName);
    }

    @Override
    public Reader getNCharacterStream(int parameterIndex) throws SQLException {
        return ((CallableStatement) this.targetStatement).getNCharacterStream(parameterIndex);
    }

    @Override
    public Reader getNCharacterStream(String parameterName) throws SQLException {
        return ((CallableStatement) this.targetStatement).getNCharacterStream(parameterName);
    }

    @Override
    public Reader getCharacterStream(int parameterIndex) throws SQLException {
        return ((CallableStatement) this.targetStatement).getCharacterStream(parameterIndex);
    }

    @Override
    public Reader getCharacterStream(String parameterName) throws SQLException {
        return ((CallableStatement) this.targetStatement).getCharacterStream(parameterName);
    }

    @Override
    public void setBlob(String parameterName, Blob x) throws SQLException {
        ((CallableStatement) this.targetStatement).setBlob(parameterName, x);
    }

    @Override
    public void setClob(String parameterName, Clob x) throws SQLException {
        ((CallableStatement) this.targetStatement).setClob(parameterName, x);
    }

    @Override
    public void setAsciiStream(String parameterName, InputStream x, long length) throws SQLException {
        ((CallableStatement) this.targetStatement).setAsciiStream(parameterName, x, length);
    }

    @Override
    public void setBinaryStream(String parameterName, InputStream x, long length) throws SQLException {
        ((CallableStatement) this.targetStatement).setBinaryStream(parameterName, x, length);
    }

    @Override
    public void setCharacterStream(String parameterName, Reader reader, long length) throws SQLException {
        ((CallableStatement) this.targetStatement).setCharacterStream(parameterName, reader, length);
    }

    @Override
    public void setAsciiStream(String parameterName, InputStream x) throws SQLException {
        ((CallableStatement) this.targetStatement).setAsciiStream(parameterName, x);
    }

    @Override
    public void setBinaryStream(String parameterName, InputStream x) throws SQLException {
        ((CallableStatement) this.targetStatement).setBinaryStream(parameterName, x);
    }

    @Override
    public void setCharacterStream(String parameterName, Reader reader) throws SQLException {
        ((CallableStatement) this.targetStatement).setCharacterStream(parameterName, reader);
    }

    @Override
    public void setNCharacterStream(String parameterName, Reader value) throws SQLException {
        ((CallableStatement) this.targetStatement).setNCharacterStream(parameterName, value);
    }

    @Override
    public void setClob(String parameterName, Reader reader) throws SQLException {
        ((CallableStatement) this.targetStatement).setClob(parameterName, reader);
    }

    @Override
    public void setBlob(String parameterName, InputStream inputStream) throws SQLException {
        ((CallableStatement) this.targetStatement).setBlob(parameterName, inputStream);
    }

    @Override
    public void setNClob(String parameterName, Reader reader) throws SQLException {
        ((CallableStatement) this.targetStatement).setNClob(parameterName, reader);
    }

    public <T> T getObject(int parameterIndex, Class<T> type) throws SQLException {
        throw new MQUnsupportException();
    }

    public <T> T getObject(String parameterName, Class<T> type) throws SQLException {
        throw new MQUnsupportException();
    }
}
