package com.yutak.im.db;

public class ChannelDBManager {
    private ChannelDBManager() {
    }

    private static class ChannelDBManagerBinder {
        static final ChannelDBManager channelDBManager = new ChannelDBManager();
    }

    public static ChannelDBManager getInstance() {
        return ChannelDBManagerBinder.channelDBManager;
    }

}
