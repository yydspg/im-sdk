package com.yutak.im.msgmodel;

import android.os.Parcel;
import android.os.Parcelable;

import com.yutak.im.manager.MsgManager;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 2020-10-13 12:12
 * 消息回复
 */
public class YutakReply implements Parcelable {
    public String root_mid;
    public String message_id;
    public long message_seq;
    public String from_uid;
    public String from_name;
    public int revoke;
    public YutakMessageContent payload;
    // 编辑后内容
    public String contentEdit;
    // 编辑消息实体
    public YutakMessageContent contentEditMsgModel;
    // 编辑时间
    public long editAt;

    public YutakReply() {
    }

    protected YutakReply(Parcel in) {
        root_mid = in.readString();
        message_id = in.readString();
        message_seq = in.readLong();
        from_uid = in.readString();
        from_name = in.readString();
        payload = in.readParcelable(YutakMessageContent.class.getClassLoader());
        contentEditMsgModel = in.readParcelable(YutakMessageContent.class.getClassLoader());
        contentEdit = in.readString();
        editAt = in.readLong();
        revoke = in.readInt();
    }

    public static final Creator<YutakReply> CREATOR = new Creator<YutakReply>() {
        @Override
        public YutakReply createFromParcel(Parcel in) {
            return new YutakReply(in);
        }

        @Override
        public YutakReply[] newArray(int size) {
            return new YutakReply[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(root_mid);
        dest.writeString(message_id);
        dest.writeLong(message_seq);
        dest.writeString(from_uid);
        dest.writeString(from_name);
        dest.writeParcelable(payload, flags);
        dest.writeParcelable(contentEditMsgModel, flags);
        dest.writeString(contentEdit);
        dest.writeLong(editAt);
        dest.writeInt(revoke);
    }

    public JSONObject encodeMsg() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("root_mid", root_mid);
            jsonObject.put("message_id", message_id);
            jsonObject.put("message_seq", message_seq);
            jsonObject.put("from_uid", from_uid);
            jsonObject.put("from_name", from_name);
            JSONObject payloadJson = payload.encodeMsg();
            if (payloadJson != null && !payloadJson.has("type")) {
                payloadJson.put("type", payload.type);
            }
            jsonObject.put("payload", payloadJson);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    public YutakReply decodeMsg(JSONObject jsonObject) {
        this.root_mid = jsonObject.optString("root_mid");
        this.message_id = jsonObject.optString("message_id");
        this.message_seq = jsonObject.optLong("message_seq");
        this.from_uid = jsonObject.optString("from_uid");
        this.from_name = jsonObject.optString("from_name");
        if (jsonObject.has("payload")) {
            JSONObject payloadJson = jsonObject.optJSONObject("payload");
            if (payloadJson != null)
                payload = MsgManager.getInstance().getMsgContentModel(payloadJson);
        }

        return this;
    }
}
