package com.yutak.im.db;


import static com.yutak.im.cs.DB.TABLE.channel;
import static com.yutak.im.cs.DB.TABLE.conversation;
import static com.yutak.im.cs.DB.TABLE.conversationExtra;
import static com.yutak.im.cs.DB.TABLE.message;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;

import com.yutak.im.YutakApplication;
import com.yutak.im.YutakIM;
import com.yutak.im.cs.DB;
import com.yutak.im.domain.YutakChannel;
import com.yutak.im.domain.YutakChannelType;
import com.yutak.im.domain.YutakConversationMsg;
import com.yutak.im.domain.YutakConversationMsgExtra;
import com.yutak.im.domain.YutakMsg;
import com.yutak.im.domain.YutakUIConversationMsg;
import com.yutak.im.manager.ConversationManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

// recent conversation
public class ConversationDBManager {
    private final String extraCols = "IFNULL(" + conversationExtra + ".browse_to,0) AS browse_to,IFNULL(" + conversationExtra + ".keep_message_seq,0) AS keep_message_seq,IFNULL(" + conversationExtra + ".keep_offset_y,0) AS keep_offset_y,IFNULL(" + conversationExtra + ".draft,'') AS draft,IFNULL(" + conversationExtra + ".version,0) AS extra_version";
    private final String channelCols = channel + ".channel_remark," +
            channel + ".channel_name," +
            channel + ".top," +
            channel + ".mute," +
            channel + ".save," +
            channel + ".status as channel_status," +
            channel + ".forbidden," +
            channel + ".invite," +
            channel + ".follow," +
            channel + ".is_deleted as channel_is_deleted," +
            channel + ".show_nick," +
            channel + ".avatar," +
            channel + ".avatar_cache_key," +
            channel + ".online," +
            channel + ".last_offline," +
            channel + ".category," +
            channel + ".receipt," +
            channel + ".robot," +
            channel + ".parent_channel_id AS c_parent_channel_id," +
            channel + ".parent_channel_type AS c_parent_channel_type," +
            channel + ".version AS channel_version," +
            channel + ".remote_extra AS channel_remote_extra," +
            channel + ".extra AS channel_extra";

    private ConversationDBManager() {
    }

    private static class ConversationDbManagerBinder {
        static final ConversationDBManager db = new ConversationDBManager();
    }

    public static ConversationDBManager getInstance() {
        return ConversationDbManagerBinder.db;
    }

    public synchronized List<YutakUIConversationMsg> queryAll() {
        List<YutakUIConversationMsg> list = new ArrayList<>();
        String sql = "SELECT " + conversation + ".*," + channelCols + "," + extraCols + " FROM "
                + conversation + " LEFT JOIN " + channel + " ON "
                + conversation + ".channel_id = " + channel + ".channel_id AND "
                + conversation + ".channel_type = " + channel + ".channel_type LEFT JOIN " + conversationExtra + " ON " + conversation + ".channel_id=" + conversationExtra + ".channel_id AND " + conversation + ".channel_type=" + conversationExtra + ".channel_type where " + conversation + ".is_deleted=0 order by "
                + DB.CoverMessageColumns.last_msg_timestamp + " desc";
        try (Cursor cursor = YutakApplication
                .get()
                .getDbHelper()
                .rawQuery(sql)) {
            if (cursor == null) {
                return list;
            }
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                YutakConversationMsg msg = serializeMsg(cursor);
                if (msg.isDeleted == 0) {
                    YutakUIConversationMsg uiMsg = getUIMsg(msg, cursor);
                    list.add(uiMsg);
                }
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    public List<YutakUIConversationMsg> queryWithChannelIds(List<String> channelIds) {
        String sql = "select " + conversation + ".*," + channelCols + "," + extraCols + " from " + conversation + " left join " + channel + " on " + conversation + ".channel_id=" + channel + ".channel_id and " + conversation + ".channel_type=" + channel + ".channel_type left join " + conversationExtra + " on " + conversation + ".channel_id=" + conversationExtra + ".channel_id and " + conversation + ".channel_type=" + conversationExtra + ".channel_type where " + conversation + ".is_deleted=0 and " + conversation + ".channel_id in (" + YutakCursor.getPlaceholders(channelIds.size()) + ")";
        List<YutakUIConversationMsg> list = new ArrayList<>();
        try (Cursor cursor = YutakApplication
                .get()
                .getDbHelper()
                .rawQuery(sql, channelIds.toArray(new String[0]))) {
            if (cursor == null) {
                return list;
            }

            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                YutakConversationMsg msg = serializeMsg(cursor);
                YutakUIConversationMsg uiMsg = getUIMsg(msg, cursor);
                list.add(uiMsg);
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    public List<YutakConversationMsg> queryWithChannelType(byte channelType) {
        List<YutakConversationMsg> list = new ArrayList<>();
        try (Cursor cursor = YutakApplication
                .get()
                .getDbHelper()
                .select(conversation, "channel_type=?", new String[]{String.valueOf(channelType)}, null)) {
            if (cursor == null) {
                return list;
            }

            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                YutakConversationMsg msg = serializeMsg(cursor);
                list.add(msg);
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    private YutakUIConversationMsg getUIMsg(YutakConversationMsg msg, Cursor cursor) {
        YutakUIConversationMsg uiMsg = getUIMsg(msg);
        YutakChannel channel = ChannelDBManager.getInstance().serializableChannel(cursor);
        if (channel != null) {
            String extra = YutakCursor.readString(cursor, "channel_extra");
            channel.localExtra = ChannelDBManager.getInstance().getChannelExtra(extra);
            String remoteExtra = YutakCursor.readString(cursor, "channel_remote_extra");
            channel.remoteExtraMap = ChannelDBManager.getInstance().getChannelExtra(remoteExtra);
            channel.status = YutakCursor.readInt(cursor, "channel_status");
            channel.version = YutakCursor.readLong(cursor, "channel_version");
            channel.parentChannelID = YutakCursor.readString(cursor, "c_parent_channel_id");
            channel.parentChannelType = YutakCursor.readByte(cursor, "c_parent_channel_type");
            channel.channelID = msg.channelID;
            channel.channelType = msg.channelType;
            uiMsg.setYutakChannel(channel);
        }
        return uiMsg;
    }

    public long queryMaxVersion() {
        long maxVersion = 0;
        String sql = "select max(version) version from " + conversation + " limit 0, 1";
        Cursor cursor = YutakApplication
                .get()
                .getDbHelper()
                .rawQuery(sql);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                maxVersion = YutakCursor.readLong(cursor, DB.CoverMessageColumns.version);
            }
            cursor.close();
        }
        return maxVersion;
    }

    public synchronized ContentValues getInsertSyncCV(YutakConversationMsg conversationMsg) {
        return YutakSqlContentValue.getWithCoverMsg(conversationMsg, true);
    }

    public synchronized void insertSyncMsg(ContentValues cv) {
        YutakApplication.get().getDbHelper().insertSql(conversation, cv);
    }

    public synchronized String queryLastMsgSeqs() {
        String lastMsgSeqs = "";
        String sql = "select GROUP_CONCAT(channel_id||':'||channel_type||':'|| last_seq,'|') synckey from (select *,(select max(message_seq) from " + message + " where " + message + ".channel_id=" + conversation + ".channel_id and " + message + ".channel_type=" + conversation + ".channel_type limit 1) last_seq from " + conversation + ") cn where channel_id<>'' AND is_deleted=0";
        Cursor cursor = YutakApplication.get().getDbHelper().rawQuery(sql);
        if (cursor == null) {
            return lastMsgSeqs;
        }
        if (cursor.moveToFirst()) {
            lastMsgSeqs = YutakCursor.readString(cursor, "synckey");
        }
        cursor.close();

        return lastMsgSeqs;
    }

    public synchronized boolean updateRedDot(String channelID, byte channelType, int redDot) {
        ContentValues cv = new ContentValues();
        cv.put(DB.CoverMessageColumns.unread_count, redDot);
        return YutakApplication.get().getDbHelper().update(conversation, DB.CoverMessageColumns.channel_id + "='" + channelID + "' and " + DB.CoverMessageColumns.channel_type + "=" + channelType, cv);
    }

    public synchronized void updateMsg(String channelID, byte channelType, String clientMsgNo, long lastMsgSeq, int count) {
        String[] update = new String[2];
        update[0] = channelID;
        update[1] = String.valueOf(channelType);
        ContentValues cv = new ContentValues();
        try {
            cv.put(DB.CoverMessageColumns.last_client_msg_no, clientMsgNo);
            cv.put(DB.CoverMessageColumns.last_msg_seq, lastMsgSeq);
            cv.put(DB.CoverMessageColumns.unread_count, count);
        } catch (Exception e) {
            e.printStackTrace();
        }
        YutakApplication.get().getDbHelper()
                .update(conversation, cv, DB.CoverMessageColumns.channel_id + "=? and " + DB.CoverMessageColumns.channel_type + "=?", update);
    }

    public YutakConversationMsg queryWithChannel(String channelID, byte channelType) {
        String sql = "select " + conversation + ".*," + channelCols + "," + extraCols + " from " + conversation + " left join " + channel + " on " + conversation + ".channel_id=" + channel + ".channel_id and " + conversation + ".channel_type=" + channel + ".channel_type left join " + conversationExtra + " on " + conversation + ".channel_id=" + conversationExtra + ".channel_id and " + conversation + ".channel_type=" + conversationExtra + ".channel_type where " + conversation + ".channel_id=? and " + conversation + ".channel_type=? and " + conversation + ".is_deleted=0";
        Cursor cursor = YutakApplication
                .get()
                .getDbHelper()
                .rawQuery(sql, new Object[]{channelID, channelType});
        YutakConversationMsg conversationMsg = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                conversationMsg = serializeMsg(cursor);
            }
            cursor.close();
        }
        return conversationMsg;
    }

    public synchronized boolean deleteWithChannel(String channelID, byte channelType, int isDeleted) {
        String[] update = new String[2];
        update[0] = channelID;
        update[1] = String.valueOf(channelType);
        ContentValues cv = new ContentValues();
        try {
            cv.put(DB.CoverMessageColumns.is_deleted, isDeleted);
        } catch (Exception e) {
            e.printStackTrace();
        }

        boolean result = YutakApplication.get().getDbHelper()
                .update(conversation, cv, DB.CoverMessageColumns.channel_id + "=? and " + DB.CoverMessageColumns.channel_type + "=?", update);
        if (result) {
            ConversationManager.getInstance().setDeleteMsg(channelID, channelType);
        }
        return result;

    }

    public synchronized YutakUIConversationMsg insertOrUpdateWithMsg(YutakMsg msg, int unreadCount) {
        if (msg.channelID.equals(YutakApplication.get().getUid())) return null;
        YutakConversationMsg YutakConversationMsg = new YutakConversationMsg();
        if (msg.channelType == YutakChannelType.COMMUNITY_TOPIC && !TextUtils.isEmpty(msg.channelID)) {
            if (msg.channelID.contains("@")) {
                String[] str = msg.channelID.split("@");
                YutakConversationMsg.parentChannelID = str[0];
                YutakConversationMsg.parentChannelType = YutakChannelType.COMMUNITY;
            }
        }
        YutakConversationMsg.channelID = msg.channelID;
        YutakConversationMsg.channelType = msg.channelType;
        YutakConversationMsg.localExtraMap = msg.localExtraMap;
        YutakConversationMsg.lastMsgTimestamp = msg.timestamp;
        YutakConversationMsg.lastClientMsgNO = msg.clientMsgNO;
        YutakConversationMsg.lastMsgSeq = msg.messageSeq;
        YutakConversationMsg.unreadCount = unreadCount;
        return insertOrUpdateWithConvMsg(YutakConversationMsg);// 插入消息列表数据表
    }

    public synchronized YutakUIConversationMsg insertOrUpdateWithConvMsg(YutakConversationMsg conversationMsg) {
        boolean result;
        YutakConversationMsg lastMsg = queryWithChannelId(conversationMsg.channelID, conversationMsg.channelType);
        if (lastMsg == null || TextUtils.isEmpty(lastMsg.channelID)) {
            //如果服务器自增id为0则表示是本地数据|直接保存
            result = insert(conversationMsg);
        } else {
            conversationMsg.unreadCount = lastMsg.unreadCount + conversationMsg.unreadCount;
            result = update(conversationMsg);
        }
        if (result) {
            return getUIMsg(conversationMsg);
        }
        return null;
    }

    private synchronized boolean insert(YutakConversationMsg msg) {
        ContentValues cv = new ContentValues();
        try {
            cv = YutakSqlContentValue.getWithCoverMsg(msg, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        long result = -1;
        try {
            result = YutakApplication.get().getDbHelper()
                    .insert(conversation, cv);
        } catch (Exception ignored) {
        }
        return result > 0;
    }

    /**
     * 更新会话记录消息
     *
     * @param msg 会话消息
     * @return 修改结果
     */
    private synchronized boolean update(YutakConversationMsg msg) {
        String[] update = new String[2];
        update[0] = msg.channelID;
        update[1] = String.valueOf(msg.channelType);
        ContentValues cv = new ContentValues();
        try {
            cv = YutakSqlContentValue.getWithCoverMsg(msg, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return YutakApplication.get().getDbHelper()
                .update(conversation, cv, DB.CoverMessageColumns.channel_id + "=? and " + DB.CoverMessageColumns.channel_type + "=?", update);
    }

    private synchronized YutakConversationMsg queryWithChannelId(String channelId, byte channelType) {
        YutakConversationMsg msg = null;
        String selection = DB.CoverMessageColumns.channel_id + " = ? and " + DB.CoverMessageColumns.channel_type + "=?";
        String[] selectionArgs = new String[]{channelId, String.valueOf(channelType)};
        Cursor cursor = YutakApplication
                .get()
                .getDbHelper()
                .select(conversation, selection, selectionArgs,
                        null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                msg = serializeMsg(cursor);
            }
            cursor.close();
        }
        return msg;
    }


    public synchronized boolean clearEmpty() {
        return YutakApplication.get().getDbHelper()
                .delete(conversation, null, null);
    }

    public YutakConversationMsgExtra queryMsgExtraWithChannel(String channelID, byte channelType) {
        YutakConversationMsgExtra msgExtra = null;
        String selection = "channel_id=? and channel_type=?";
        Cursor cursor = YutakApplication
                .get()
                .getDbHelper().select(conversationExtra, selection, new String[]{channelID, String.valueOf(channelType)}, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                msgExtra = serializeMsgExtra(cursor);
            }
            cursor.close();
        }
        return msgExtra;
    }

    private List<YutakConversationMsgExtra> queryWithExtraChannelIds(List<String> channelIds) {
        List<YutakConversationMsgExtra> list = new ArrayList<>();
        try (Cursor cursor = YutakApplication.get().getDbHelper().select(conversationExtra, "channel_id in (" + YutakCursor.getPlaceholders(channelIds.size()) + ")", channelIds.toArray(new String[0]), null)) {
            if (cursor == null) {
                return list;
            }
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                YutakConversationMsgExtra extra = serializeMsgExtra(cursor);
                list.add(extra);
            }
        }
        return list;
    }

    public synchronized boolean insertOrUpdateMsgExtra(YutakConversationMsgExtra extra) {
        YutakConversationMsgExtra msgExtra = queryMsgExtraWithChannel(extra.channelID, extra.channelType);
        boolean isAdd = true;
        if (msgExtra != null) {
            extra.version = msgExtra.version;
            isAdd = false;
        }
        ContentValues cv = YutakSqlContentValue.getWithCVWithExtra(extra);
        if (isAdd) {
            return YutakApplication.get().getDbHelper().insert(conversationExtra, cv) > 0;
        }
        return YutakApplication.get().getDbHelper().update(conversationExtra, "channel_id='" + extra.channelID + "' and channel_type=" + extra.channelType, cv);
    }

    public synchronized void insertMsgExtras(List<YutakConversationMsgExtra> list) {
        List<String> channelIds = new ArrayList<>();
        for (YutakConversationMsgExtra extra : list) {
            boolean isAdd = true;
            for (String channelID : channelIds) {
                if (channelID.equals(extra.channelID)) {
                    isAdd = false;
                    break;
                }
            }
            if (isAdd) channelIds.add(extra.channelID);
        }
        List<ContentValues> insertCVList = new ArrayList<>();
        List<ContentValues> updateCVList = new ArrayList<>();
        List<YutakConversationMsgExtra> existList = queryWithExtraChannelIds(channelIds);
        for (YutakConversationMsgExtra extra : list) {
            boolean isAdd = true;
            for (YutakConversationMsgExtra existExtra : existList) {
                if (existExtra.channelID.equals(extra.channelID) && existExtra.channelType == extra.channelType) {
                    isAdd = false;
                    break;
                }
            }
            if (isAdd) {
                insertCVList.add(YutakSqlContentValue.getWithCVWithExtra(extra));
            } else {
                updateCVList.add(YutakSqlContentValue.getWithCVWithExtra(extra));
            }
        }

        try {
            YutakApplication.get().getDbHelper().getDb().beginTransaction();
            if (insertCVList.size() > 0) {
                for (ContentValues cv : insertCVList) {
                    YutakApplication.get().getDbHelper()
                            .insert(conversationExtra, cv);
                }
            }
            if (updateCVList.size() > 0) {
                for (ContentValues cv : updateCVList) {
                    String[] sv = new String[2];
                    sv[0] = cv.getAsString("channel_id");
                    sv[1] = cv.getAsString("channel_type");
                    YutakApplication.get().getDbHelper()
                            .update(conversationExtra, cv, "channel_id=? and channel_type=?", sv);
                }
            }
            YutakApplication.get().getDbHelper().getDb().setTransactionSuccessful();
        } finally {
            YutakApplication.get().getDbHelper().getDb().endTransaction();
        }
        List<YutakUIConversationMsg> uiMsgList = ConversationDBManager.getInstance().queryWithChannelIds(channelIds);
        for (int i = 0, size = uiMsgList.size(); i < size; i++) {
            YutakIM.get().getConversationManager().setOnRefreshMsg(uiMsgList.get(i), i == uiMsgList.size() - 1, "saveMsgExtras");
        }
    }

    public long queryMsgExtraMaxVersion() {
        long maxVersion = 0;
        String sql = "select max(version) version from " + conversationExtra;
        Cursor cursor = YutakApplication
                .get()
                .getDbHelper()
                .rawQuery(sql);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                maxVersion = YutakCursor.readLong(cursor, "version");
            }
            cursor.close();
        }
        return maxVersion;
    }

    private synchronized YutakConversationMsgExtra serializeMsgExtra(Cursor cursor) {
        YutakConversationMsgExtra extra = new YutakConversationMsgExtra();
        extra.channelID = YutakCursor.readString(cursor, "channel_id");
        extra.channelType = (byte) YutakCursor.readInt(cursor, "channel_type");
        extra.keepMessageSeq = YutakCursor.readLong(cursor, "keep_message_seq");
        extra.keepOffsetY = YutakCursor.readInt(cursor, "keep_offset_y");
        extra.draft = YutakCursor.readString(cursor, "draft");
        extra.browseTo = YutakCursor.readLong(cursor, "browse_to");
        extra.draftUpdatedAt = YutakCursor.readLong(cursor, "draft_updated_at");
        extra.version = YutakCursor.readLong(cursor, "version");
        if (cursor.getColumnIndex("extra_version") > 0) {
            extra.version = YutakCursor.readLong(cursor, "extra_version");
        }
        return extra;
    }

    private synchronized YutakConversationMsg serializeMsg(Cursor cursor) {
        YutakConversationMsg msg = new YutakConversationMsg();
        msg.channelID = YutakCursor.readString(cursor, DB.CoverMessageColumns.channel_id);
        msg.channelType = YutakCursor.readByte(cursor, DB.CoverMessageColumns.channel_type);
        msg.lastMsgTimestamp = YutakCursor.readLong(cursor, DB.CoverMessageColumns.last_msg_timestamp);
        msg.unreadCount = YutakCursor.readInt(cursor, DB.CoverMessageColumns.unread_count);
        msg.isDeleted = YutakCursor.readInt(cursor, DB.CoverMessageColumns.is_deleted);
        msg.version = YutakCursor.readLong(cursor, DB.CoverMessageColumns.version);
        msg.lastClientMsgNO = YutakCursor.readString(cursor, DB.CoverMessageColumns.last_client_msg_no);
        msg.lastMsgSeq = YutakCursor.readLong(cursor, DB.CoverMessageColumns.last_msg_seq);
        msg.parentChannelID = YutakCursor.readString(cursor, DB.CoverMessageColumns.parent_channel_id);
        msg.parentChannelType = YutakCursor.readByte(cursor, DB.CoverMessageColumns.parent_channel_type);
        String extra = YutakCursor.readString(cursor, DB.CoverMessageColumns.extra);
        if (!TextUtils.isEmpty(extra)) {
            HashMap<String, Object> hashMap = new HashMap<>();
            try {
                JSONObject jsonObject = new JSONObject(extra);
                Iterator<String> keys = jsonObject.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    hashMap.put(key, jsonObject.opt(key));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            msg.localExtraMap = hashMap;
        }
        msg.msgExtra = serializeMsgExtra(cursor);
        return msg;
    }

    public YutakUIConversationMsg getUIMsg(YutakConversationMsg conversationMsg) {
        YutakUIConversationMsg msg = new YutakUIConversationMsg();
        msg.lastMsgSeq = conversationMsg.lastMsgSeq;
        msg.clientMsgNo = conversationMsg.lastClientMsgNO;
        msg.unreadCount = conversationMsg.unreadCount;
        msg.lastMsgTimestamp = conversationMsg.lastMsgTimestamp;
        msg.channelID = conversationMsg.channelID;
        msg.channelType = conversationMsg.channelType;
        msg.isDeleted = conversationMsg.isDeleted;
        msg.parentChannelID = conversationMsg.parentChannelID;
        msg.parentChannelType = conversationMsg.parentChannelType;
        msg.setRemoteMsgExtra(conversationMsg.msgExtra);
        return msg;
    }
}
