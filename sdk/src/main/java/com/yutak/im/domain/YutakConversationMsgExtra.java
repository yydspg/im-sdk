package com.yutak.im.domain;

import androidx.annotation.NonNull;

public class YutakConversationMsgExtra {
    public String channelID;
    public byte channelType;
    public long browseTo;
    public long keepMessageSeq;
    public int keepOffsetY;
    public String draft;
    public long version;
    public long draftUpdatedAt;

    @NonNull
    @Override
    public String toString() {
        return "YutakConversationMsgExtra{" +
                "channelID='" + channelID + '\'' +
                ", channelType=" + channelType +
                ", browseTo=" + browseTo +
                ", keepMessageSeq=" + keepMessageSeq +
                ", keepOffsetY=" + keepOffsetY +
                ", draft='" + draft + '\'' +
                ", version=" + version +
                ", draftUpdatedAt=" + draftUpdatedAt +
                '}';
    }
}
