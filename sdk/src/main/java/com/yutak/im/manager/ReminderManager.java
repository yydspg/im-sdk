package com.yutak.im.manager;

public class ReminderManager {
    private ReminderManager() {
    }

    private static class RemindManagerBinder {
        final static ReminderManager manager = new ReminderManager();
    }

    public static ReminderManager getInstance() {
        return RemindManagerBinder.manager;
    }

}
