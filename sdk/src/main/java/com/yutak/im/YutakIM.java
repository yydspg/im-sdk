package com.yutak.im;

public class YutakIM {
    private final String Version = "V1.1.4";
    private boolean isDebug = false;
    private boolean isWriteLog = false;
    public boolean isDebug() {
        return isDebug;
    }

    public boolean isWriteLog() {
        return isWriteLog;
    }

    public void setWriteLog(boolean isWriteLog) {
        this.isWriteLog = isWriteLog;
    }

    // debug模式会输出一些连接信息，发送消息情况等
    public void setDebug(boolean isDebug) {
        this.isDebug = isDebug;
    }
}
