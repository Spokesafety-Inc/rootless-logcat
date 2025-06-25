package com.luxoft.spokelogcat;

import java.util.List;

public interface Reader {
    interface UpdateHandler {
        boolean isCancelled();
        void update(int status, List<String> lines);
    }

    int     read(UpdateHandler updateHandler);
    String getErrorMessage();
}
