package com.yutak.im.domain;

import java.util.List;

/**
 * 2020-10-09 14:49
 * 同步会话
 */
public class YutakSyncChat {
    public long cmd_version;
    public List<YutakSyncCmd> cmds;
    public String uid;
    public List<YutakSyncConvMsg> conversations;
}
