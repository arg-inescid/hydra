package com.lambda_manager.utils.logger;

import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

public class AdvancedOutputFormatter extends SimpleFormatter {

    @Override
    public String format(LogRecord record) {
        String message = record.getMessage();
        if (message.contains("#")) {
            String[] split = message.split("#");
            record.setMessage(split[1]);
            return "Timestamp (" + split[0] + ") " + super.format(record);
        } else {
            return "Timestamp (" + ElapseTimer.elapsedTime() + ") " + super.format(record);
        }
    }
}
