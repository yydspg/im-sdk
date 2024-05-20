package com.yutak.im.manager;

public class CMDManager {
    private CMDManager() {
    }

    private static class CMDManagerBinder {
        static final CMDManager cmdManager = new CMDManager();
    }

    public static CMDManager getInstance() {
        return CMDManagerBinder.cmdManager;
    }
}
