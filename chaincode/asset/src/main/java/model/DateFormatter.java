package model;

import org.apache.commons.lang3.SerializationUtils;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.shim.ChaincodeException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class DateFormatter {

    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE
            .withZone(ZoneId.from(ZoneOffset.UTC));

    public static String getFormattedTimestamp(Context ctx) {
        return DATE_TIME_FORMATTER.format(ctx.getStub().getTxTimestamp());
    }

    public static void checkDateFormat(String date) {
        try {
            LocalDate.parse(date, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new ChaincodeException("expected format YYYY-MM-DD, received " + date);
        }
    }
}
