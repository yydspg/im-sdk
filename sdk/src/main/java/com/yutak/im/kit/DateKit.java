package com.yutak.im.kit;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateKit {
    private DateKit() {
    }
    private static final DateKit dateKit = new DateKit();

    public static DateKit get() { return dateKit;}

    public long now() {return System.currentTimeMillis();}

    public long nowSeconds() {return System.currentTimeMillis() /1000;}

    public int hour() {return Calendar.getInstance().get(Calendar.HOUR);}

    public int minute() {return Calendar.getInstance().get(Calendar.MINUTE);}

    public String format(long timeStamp) {
        if(String.valueOf(timeStamp).length() < 13) {
            timeStamp = timeStamp * 1000;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date(timeStamp));
    }
}
