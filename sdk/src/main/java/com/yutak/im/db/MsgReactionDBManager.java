package com.yutak.im.db;


import static com.yutak.im.cs.DB.TABLE.messageReaction;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;

import com.yutak.im.YutakApplication;
import com.yutak.im.YutakIM;
import com.yutak.im.domain.YutakChannel;
import com.yutak.im.domain.YutakChannelType;
import com.yutak.im.domain.YutakMsgReaction;

import java.util.ArrayList;
import java.util.List;

/**
 * 4/16/21 1:46 PM
 * 消息回应
 */
class MsgReactionDBManager {
    private MsgReactionDBManager() {
    }

    private static class MessageReactionDBManagerBinder {
        final static MsgReactionDBManager manager = new MsgReactionDBManager();
    }

    public static MsgReactionDBManager getInstance() {
        return MessageReactionDBManagerBinder.manager;
    }

    public void insertReactions(List<YutakMsgReaction> list) {
        if (list == null || list.isEmpty()) return;
        for (int i = 0, size = list.size(); i < size; i++) {
            insertOrUpdate(list.get(i));
        }
    }

    public void update(YutakMsgReaction reaction) {
        String[] update = new String[2];
        update[0] = reaction.messageID;
        update[1] = reaction.uid;
        ContentValues cv = new ContentValues();
        cv.put("is_deleted", reaction.isDeleted);
        cv.put("seq", reaction.seq);
        cv.put("emoji", reaction.emoji);
        YutakApplication.get().getDbHelper()
                .update(messageReaction, cv, "message_id=? and uid=?", update);

    }

    public synchronized void insertOrUpdate(YutakMsgReaction reaction) {
        boolean isExist = isExist(reaction.uid, reaction.messageID);
        if (isExist) {
            update(reaction);
        } else {
            insert(reaction);
        }
        // insert(reaction);
    }

    public void insert(YutakMsgReaction reaction) {
        YutakApplication.get().getDbHelper()
                .insert(messageReaction, YutakSqlContentValue.getWithMsgReaction(reaction));
    }

    private boolean isExist(String uid, String messageID) {
        boolean isExist = false;
        try (Cursor cursor = YutakApplication
                .get()
                .getDbHelper().select(messageReaction, "message_id=? and uid=?", new String[]{messageID, uid}, null)) {
            if (cursor != null && cursor.moveToLast()) {
                isExist = true;
            }
        }
        return isExist;
    }

    public List<YutakMsgReaction> queryWithMessageId(String messageID) {
        List<YutakMsgReaction> list = new ArrayList<>();
        String sql = "select * from " + messageReaction + " where message_id=? and is_deleted=0 ORDER BY created_at desc";
        try (Cursor cursor = YutakApplication.get().getDbHelper().rawQuery(sql, new Object[]{messageID})) {
            if (cursor == null) {
                return list;
            }
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                YutakMsgReaction reaction = serializeReaction(cursor);
                YutakChannel channel = YutakIM.get().getChannelManager().getChannel(reaction.uid, YutakChannelType.PERSONAL);
                if (channel != null) {
                    String showName = TextUtils.isEmpty(channel.channelRemark) ? channel.channelName : channel.channelRemark;
                    if (!TextUtils.isEmpty(showName))
                        reaction.name = showName;
                }
                list.add(reaction);
            }
        }
        return list;
    }

    public List<YutakMsgReaction> queryWithMessageIds(List<String> messageIds) {
        List<YutakMsgReaction> list = new ArrayList<>();
        List<String> channelIds = new ArrayList<>();
        try (Cursor cursor = YutakApplication
                .get()
                .getDbHelper().select(messageReaction, "message_id in (" + YutakCursor.getPlaceholders(messageIds.size()) + ")", messageIds.toArray(new String[0]), "created_at desc")) {
            if (cursor == null) {
                return list;
            }
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                YutakMsgReaction msgReaction = serializeReaction(cursor);
                channelIds.add(msgReaction.uid);
                list.add(msgReaction);
            }
        } catch (Exception ignored) {
        }
        //查询用户备注
        List<YutakChannel> channelList = ChannelDBManager.getInstance().queryWithChannelIdsAndChannelType(channelIds, YutakChannelType.PERSONAL);
        for (int i = 0, size = list.size(); i < size; i++) {
            for (int j = 0, len = channelList.size(); j < len; j++) {
                if (channelList.get(j).channelID.equals(list.get(i).uid)) {
                    list.get(i).name = TextUtils.isEmpty(channelList.get(j).channelRemark) ? channelList.get(j).channelName : channelList.get(j).channelRemark;
                }
            }
        }
        return list;
    }

    public YutakMsgReaction queryWithMsgIdAndUIDAndText(String messageID, String uid, String emoji) {
        YutakMsgReaction reaction = null;
        String sql = "select * from " + messageReaction
                + " where message_id=? and uid=? and emoji=?";
        try (Cursor cursor = YutakApplication
                .get()
                .getDbHelper().rawQuery(sql, new Object[]{messageID, uid, emoji})) {
            if (cursor == null) {
                return null;
            }
            if (cursor.moveToLast()) {
                reaction = serializeReaction(cursor);
            }
        }

        return reaction;
    }

    public YutakMsgReaction queryWithMsgIdAndUID(String messageID, String uid) {
        YutakMsgReaction reaction = null;
        String sql = "select * from " + messageReaction
                + " where message_id=? and uid=?";
        try (Cursor cursor = YutakApplication
                .get()
                .getDbHelper().rawQuery(sql, new Object[]{messageID, uid})) {
            if (cursor == null) {
                return null;
            }
            if (cursor.moveToLast()) {
                reaction = serializeReaction(cursor);
            }
        }

        return reaction;
    }

    public long queryMaxSeqWithChannel(String channelID, byte channelType) {
        int maxSeq = 0;
        String sql = "select max(seq) seq from " + messageReaction
                + " where channel_id=? and channel_type=? limit 0, 1";
        try {
            if (YutakApplication.get().getDbHelper() != null) {
                Cursor cursor = YutakApplication
                        .get()
                        .getDbHelper()
                        .rawQuery(sql, new Object[]{channelID, channelType});
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        maxSeq = YutakCursor.readInt(cursor, "seq");
                    }
                    cursor.close();
                }
            }
        } catch (Exception ignored) {
        }
        return maxSeq;
    }

    private YutakMsgReaction serializeReaction(Cursor cursor) {
        YutakMsgReaction reaction = new YutakMsgReaction();
        reaction.channelID = YutakCursor.readString(cursor, "channel_id");
        reaction.channelType = (byte) YutakCursor.readInt(cursor, "channel_type");
        reaction.uid = YutakCursor.readString(cursor, "uid");
        reaction.name = YutakCursor.readString(cursor, "name");
        reaction.messageID = YutakCursor.readString(cursor, "message_id");
        reaction.createdAt = YutakCursor.readString(cursor, "created_at");
        reaction.seq = YutakCursor.readLong(cursor, "seq");
        reaction.emoji = YutakCursor.readString(cursor, "emoji");
        reaction.isDeleted = YutakCursor.readInt(cursor, "is_deleted");
        return reaction;
    }
}
