package org.clever.data.jdbc.type;

import org.clever.data.dynamic.sql.exception.PersistenceException;

import java.io.Serial;

public class ResultMapException extends PersistenceException {
    @Serial
    private static final long serialVersionUID = 3270932060569707623L;

    public ResultMapException() {
    }

    public ResultMapException(String message) {
        super(message);
    }

    public ResultMapException(String message, Throwable cause) {
        super(message, cause);
    }

    public ResultMapException(Throwable cause) {
        super(cause);
    }
}
