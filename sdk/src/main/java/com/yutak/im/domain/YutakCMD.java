package com.yutak.im.domain;

import org.json.JSONObject;

/**
 * 2/3/21 2:21 PM
 * CMD
 */
public class YutakCMD {
    // 命令类型
    public String cmdKey;
    // 命令参数
    public JSONObject paramJsonObject;

    public YutakCMD(String cmdKey, JSONObject paramJsonObject) {
        this.cmdKey = cmdKey;
        this.paramJsonObject = paramJsonObject;
    }
}
