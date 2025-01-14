package org.clever.data.jdbc.type;

import org.clever.data.dynamic.sql.exception.PersistenceException;

import java.io.Serial;

public class TypeException extends PersistenceException {
    @Serial
    private static final long serialVersionUID = 8614420898975117130L;

    public TypeException() {
        super();
    }

    public TypeException(String message) {
        super(message);
    }

    public TypeException(String message, Throwable cause) {
        super(message, cause);
    }

    public TypeException(Throwable cause) {
        super(cause);
    }
}
