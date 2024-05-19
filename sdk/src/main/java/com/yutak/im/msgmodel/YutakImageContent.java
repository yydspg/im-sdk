package com.yutak.im.msgmodel;

import android.os.Parcel;

import com.yutak.im.cs.Yutak;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 2020-04-04 10:42
 * 图片消息
 */
public class YutakImageContent extends YutakMediaMessageContent {
    public int width;
    public int height;

    public YutakImageContent(String localPath) {
        this.localPath = localPath;
        this.type = Yutak.MsgContentType.IMAGE;
    }

    // 无参构造必须提供
    public YutakImageContent() {
        this.type = Yutak.MsgContentType.IMAGE;
    }

    @Override
    public JSONObject encodeMsg() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("url", url);
            jsonObject.put("width", width);
            jsonObject.put("height", height);
            jsonObject.put("localPath", localPath);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    @Override
    public YutakMessageContent decodeMsg(JSONObject jsonObject) {
        if (jsonObject.has("url"))
            this.url = jsonObject.optString("url");
        if (jsonObject.has("localPath"))
            this.localPath = jsonObject.optString("localPath");
        if (jsonObject.has("height"))
            this.height = jsonObject.optInt("height");
        if (jsonObject.has("width"))
            this.width = jsonObject.optInt("width");
        return this;
    }


    protected YutakImageContent(Parcel in) {
        super(in);
        width = in.readInt();
        height = in.readInt();
        url = in.readString();
        localPath = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(width);
        dest.writeInt(height);
        dest.writeString(url);
        dest.writeString(localPath);
    }


    public static final Creator<YutakImageContent> CREATOR = new Creator<YutakImageContent>() {
        @Override
        public YutakImageContent createFromParcel(Parcel in) {
            return new YutakImageContent(in);
        }

        @Override
        public YutakImageContent[] newArray(int size) {
            return new YutakImageContent[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public String getDisplayContent() {
        return "[图片]";
    }

    @Override
    public String getSearchableWord() {
        return "[图片]";
    }
}
