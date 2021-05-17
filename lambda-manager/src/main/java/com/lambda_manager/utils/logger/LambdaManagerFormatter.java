package com.lambda_manager.utils.logger;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

public class LambdaManagerFormatter extends SimpleFormatter {

    @Override
    public String format(LogRecord record) {
        String throwable = "";
        if (record.getThrown() != null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            pw.println();
            record.getThrown().printStackTrace(pw);
            pw.close();
            throwable = sw.toString();
        }
        return String.format("%s %s %s %s%n",
                String.format("Timestamp (%s)", ElapseTimer.elapsedTime()),
                record.getLevel().getLocalizedName(),
                record.getMessage(),
                throwable);
    }
}
