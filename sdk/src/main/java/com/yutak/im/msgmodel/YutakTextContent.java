package com.yutak.im.msgmodel;

import android.os.Parcel;
import android.text.TextUtils;

import com.yutak.im.cs.Yutak;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 2020-04-04 10:35
 * 文本消息
 */
public class YutakTextContent extends YutakMessageContent {

    public YutakTextContent(String content) {
        this.content = content;
        this.type = Yutak.MsgContentType.TEXT;
    }

    // 无参构造必须提供
    public YutakTextContent() {
        this.type = Yutak.MsgContentType.TEXT;
    }

    @Override
    public JSONObject encodeMsg() {
        JSONObject jsonObject = new JSONObject();
        try {
            if (!TextUtils.isEmpty(content))
                jsonObject.put("content", content);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    @Override
    public YutakMessageContent decodeMsg(JSONObject jsonObject) {
        if (jsonObject != null) {
            if (jsonObject.has("content"))
                this.content = jsonObject.optString("content");
        }
        return this;
    }

    @Override
    public String getSearchableWord() {
        return content;
    }

    @Override
    public String getDisplayContent() {
        return content;
    }

    protected YutakTextContent(Parcel in) {
        super(in);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
    }


    public static final Creator<YutakTextContent> CREATOR = new Creator<YutakTextContent>() {
        @Override
        public YutakTextContent createFromParcel(Parcel in) {
            return new YutakTextContent(in);
        }

        @Override
        public YutakTextContent[] newArray(int size) {
            return new YutakTextContent[size];
        }
    };
}
