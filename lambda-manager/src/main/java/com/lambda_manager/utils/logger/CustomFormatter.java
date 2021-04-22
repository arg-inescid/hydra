package com.lambda_manager.utils.logger;

import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

public class CustomFormatter extends SimpleFormatter {

    @Override
    public String format(LogRecord record) {
        String header;
        String message = record.getMessage();
        if (message.contains("#")) {
            String[] split = message.split("#");
            message = split[1];
            header = "Timestamp (" + split[0] + ")";
        } else {
            header = "Timestamp (" + ElapseTimer.elapsedTime() + ")";
        }
        return header +
                " " +
                record.getLevel().getLocalizedName() +
                ": " +
                message +
                "\n";
    }
}
