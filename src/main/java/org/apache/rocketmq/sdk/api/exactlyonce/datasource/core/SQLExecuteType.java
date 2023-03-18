package org.apache.rocketmq.sdk.api.exactlyonce.datasource.core;

public enum SQLExecuteType {
    EXECUTE_STRING(1, "execute_string"),
    EXECUTE_STRING_INT(2, "execute_string_int"),
    EXECUTE_STRING_INTARRAY(3, "execute_string_intarray"),
    EXECUTE_STRING_STRINGARRAY(4, "execute_string_stringarray"),
    EXECUTEUPDATE_STRING(5, "executeupdate_string"),
    EXECUTEUPDATE_STRING_INT(6, "executeupdate_string_int"),
    EXECUTEUPDATE_STRING_INTARRAY(7, "executeupdate_string_intarray"),
    EXECUTEUPDATE_STRING_STRINGARRAY(8, "executeupdate_string_stringarray"),
    EXECUTEBATCH_VOID(9, "executebatch"),
    PREPARED_EXECUTE_VOID(20, "prepared_execute_void"),
    PREPARED_EXECUTEUPDATE_VOID(21, "prepared_executeupdate_void"),
    PREPARED_EXECUTEBATCH_VOID(22, "prepared_executebatch_void");
    
    private int executeCode;
    private String fullName;

    SQLExecuteType(int executeCode, String fullName) {
        this.executeCode = executeCode;
        this.fullName = fullName;
    }

    SQLExecuteType parseFromName(String fullName) {
        SQLExecuteType[] values = values();
        for (SQLExecuteType type : values) {
            if (type.fullName.equals(fullName)) {
                return type;
            }
        }
        return null;
    }

    public int getExecuteCode() {
        return this.executeCode;
    }

    public void setExecuteCode(int executeCode) {
        this.executeCode = executeCode;
    }

    public String getFullName() {
        return this.fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
}
