package com.oracle.svm.graalvisor.jdbc;

public enum MethodIdentifier {
    CONNECTION__GET_CONNECTION(100), //
    CONNECTION__CREATE_STATEMENT(101), //
    CONNECTION__CLOSE(102), //
    CONNECTION__RELEASE(103), //

    STATEMENT__EXECUTE_QUERY(200), //
    STATEMENT__CLOSE(201), //

    RESULT_SET__NEXT(300), //
    RESULT_SET__GET_INT_INDEX(301), //
    RESULT_SET__GET_INT_LABEL(302), //
    RESULT_SET__GET_STRING_INDEX(303), //
    RESULT_SET__GET_STRING_LABEL(304), //
    RESULT_SET__CLOSE(305), //

    RESULT_SET_META_DATA__GET_COLUMN_COUNT(400), //
    RESULT_SET_META_DATA__GET_COLUMN_NAME(401);

    private final int value;

    private MethodIdentifier(final int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

    public static MethodIdentifier fromCode(int methodCode) {
        for (MethodIdentifier mi : MethodIdentifier.values()) {
            if (mi.value == methodCode) {
                return mi;
            }
        }
        return null;
    }
}
