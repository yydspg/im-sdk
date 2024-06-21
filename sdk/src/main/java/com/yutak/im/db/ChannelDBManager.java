package com.yutak.im.db;


import static com.yutak.im.cs.DB.TABLE.channel;
import static com.yutak.im.cs.DB.TABLE.channelMembers;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;

import com.yutak.im.YutakApplication;
import com.yutak.im.cs.DB;
import com.yutak.im.domain.YutakChannel;
import com.yutak.im.domain.YutakChannelSearchResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

// channel DB Manager
public class ChannelDBManager {

    private ChannelDBManager() {
    }

    private static class ChannelDBManagerBinder {
        static final ChannelDBManager channelDBManager = new ChannelDBManager();
    }

    public static ChannelDBManager getInstance() {
        return ChannelDBManagerBinder.channelDBManager;
    }

    // query channel info as list
    public List<YutakChannel> queryWithChannelIdsAndChannelType(List<String> channelIDs, byte channelType) {
        List<YutakChannel> list = new ArrayList<>();
        if (YutakApplication
                .get()
                .getDbHelper() == null) {
            return list;
        }
        List<String> args = new ArrayList<>(channelIDs);
        args.add(String.valueOf(channelType));
        try (Cursor cursor = YutakApplication
                .get()
                .getDbHelper()
                .select(channel, "channel_id in (" + YutakCursor.getPlaceholders(channelIDs.size()) + ") and channel_type=?", args.toArray(new String[0]), null)) {
            if (cursor == null) {
                return list;
            }
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                YutakChannel channel = serializableChannel(cursor);
                list.add(channel);
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    // query a channel by id and type
    public synchronized YutakChannel query(String channelId, int channelType) {
        String selection = DB.ChannelColumns.channel_id + "=? and " + DB.ChannelColumns.channel_type + "=?";
        String[] selectionArgs = new String[2];
        selectionArgs[0] = channelId;
        selectionArgs[1] = String.valueOf(channelType);
        Cursor cursor = null;
        YutakChannel YutakChannel = null;
        // query db helper
        if (YutakApplication
                .get()
                .getDbHelper() == null) {
            return null;
        }
        try {
            cursor = YutakApplication
                    .get()
                    .getDbHelper()
                    .select(channel, selection, selectionArgs,
                            null);
            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    cursor.moveToNext();
                    YutakChannel = serializableChannel(cursor);
                }
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return YutakChannel;
    }
    // query current channel whether exists
    private boolean isExist(String channelId, int channelType) {
        String selection = DB.ChannelColumns.channel_id + "=? and " + DB.ChannelColumns.channel_type + "=?";
        String[] selectionArgs = new String[2];
        selectionArgs[0] = channelId;
        selectionArgs[1] = String.valueOf(channelType);
        Cursor cursor = null;
        boolean isExist = false;
        try {
            if (YutakApplication
                    .get()
                    .getDbHelper() == null) {
                return false;
            }
            cursor = YutakApplication
                    .get()
                    .getDbHelper()
                    // store in channel table
                    .select(channel, selection, selectionArgs,
                            null);
            if (cursor != null && cursor.moveToNext()) {
                isExist = true;
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return isExist;
    }
    // add channel as list
    public synchronized void insertChannels(List<YutakChannel> list) {
        List<ContentValues> updateCVList = new ArrayList<>();
        List<ContentValues> newCVList = new ArrayList<>();
        for (YutakChannel channel : list) {
            boolean isExist = isExist(channel.channelID, channel.channelType);
            ContentValues cv = YutakSqlContentValue.getWithChannel(channel);
            if (isExist) updateCVList.add(cv);
            else newCVList.add(cv);
        }
        try {
            if (YutakApplication.get().getDbHelper() == null) {
                return;
            }
            YutakApplication.get().getDbHelper().getDb()
                    .beginTransaction();
            if (updateCVList.size() > 0) {
                for (ContentValues cv : updateCVList) {
                    String[] update = new String[2];
                    update[0] = cv.getAsString(DB.ChannelColumns.channel_id);
                    update[1] = String.valueOf(cv.getAsByte(DB.ChannelColumns.channel_type));
                    YutakApplication.get().getDbHelper()
                            .update(channel, cv, DB.ChannelColumns.channel_id + "=? and " + DB.ChannelColumns.channel_type + "=?", update);
                }
            } else {
                for (ContentValues cv : newCVList) {
                    YutakApplication.get().getDbHelper()
                            .insert(channel, cv);
                }
            }
            YutakApplication.get().getDbHelper().getDb()
                    .setTransactionSuccessful();
        } catch (Exception ignored) {
        } finally {
            if (YutakApplication.get().getDbHelper().getDb().inTransaction()) {
                YutakApplication.get().getDbHelper().getDb()
                        .endTransaction();
            }
        }
    }

    public synchronized void insertOrUpdate(YutakChannel channel) {
        if (isExist(channel.channelID, channel.channelType)) {
            update(channel);
        } else {
            insert(channel);
        }
    }

    private synchronized void insert(YutakChannel YutakChannel) {
        ContentValues cv = new ContentValues();
        try {
            cv = YutakSqlContentValue.getWithChannel(YutakChannel);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (YutakApplication.get().getDbHelper() == null) {
            return;
        }
        YutakApplication.get().getDbHelper()
                .insert(channel, cv);
    }

    public synchronized void update(YutakChannel YutakChannel) {
        String[] update = new String[2];
        update[0] = YutakChannel.channelID;
        update[1] = String.valueOf(YutakChannel.channelType);
        ContentValues cv = new ContentValues();
        try {
            cv = YutakSqlContentValue.getWithChannel(YutakChannel);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (YutakApplication.get().getDbHelper() == null) {
            return;
        }
        YutakApplication.get().getDbHelper()
                .update(channel, cv, DB.ChannelColumns.channel_id + "=? and " + DB.ChannelColumns.channel_type + "=?", update);

    }

    /**
     * 查询频道
     *
     * @param channelType 频道类型
     * @param follow      是否关注 好友或陌生人
     * @param status      状态 正常或黑名单
     * @return List<YutakChannel>
     */
    public synchronized List<YutakChannel> queryWithFollowAndStatus(byte channelType, int follow, int status) {
        String[] args = new String[3];
        args[0] = String.valueOf(channelType);
        args[1] = String.valueOf(follow);
        args[2] = String.valueOf(status);
        String selection = DB.ChannelColumns.channel_type + "=? and " + DB.ChannelColumns.follow + "=? and " + DB.ChannelColumns.status + "=? and is_deleted=0";
        List<YutakChannel> channels = new ArrayList<>();
        if (YutakApplication
                .get()
                .getDbHelper() != null) {
            try (Cursor cursor = YutakApplication
                    .get()
                    .getDbHelper().select(channel, selection, args, null)) {
                if (cursor == null) {
                    return channels;
                }
                for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                    channels.add(serializableChannel(cursor));
                }
            }
        }

        return channels;
    }

    /**
     * 查下指定频道类型和频道状态的频道
     *
     * @param channelType 频道类型
     * @param status      状态[sdk不维护状态]
     * @return List<YutakChannel>
     */
    public synchronized List<YutakChannel> queryWithStatus(byte channelType, int status) {
        String[] args = new String[2];
        args[0] = String.valueOf(channelType);
        args[1] = String.valueOf(status);
        String selection = DB.ChannelColumns.channel_type + "=? and " + DB.ChannelColumns.status + "=?";
        List<YutakChannel> channels = new ArrayList<>();
        if (YutakApplication.get().getDbHelper() == null) {
            return channels;
        }
        try (Cursor cursor = YutakApplication
                .get()
                .getDbHelper().select(channel, selection, args, null)) {
            if (cursor == null) {
                return channels;
            }
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                channels.add(serializableChannel(cursor));
            }
        }
        return channels;
    }

    public synchronized List<YutakChannelSearchResult> search(String searchKey) {
        List<YutakChannelSearchResult> list = new ArrayList<>();
        Object[] args = new Object[4];
        args[0] = "%" + searchKey + "%";
        args[1] = "%" + searchKey + "%";
        args[2] = "%" + searchKey + "%";
        args[3] = "%" + searchKey + "%";
        String sql = " select t.*,cm.member_name,cm.member_remark from (\n" +
                " select " + channel + ".*,max(" + channelMembers + ".id) mid from " + channel + "," + channelMembers + " " +
                "where " + channel + ".channel_id=" + channelMembers + ".channel_id and " + channel + ".channel_type=" + channelMembers + ".channel_type" +
                " and (" + channel + ".channel_name like ? or " + channel + ".channel_remark" +
                " like ? or " + channelMembers + ".member_name like ? or " + channelMembers + ".member_remark like ?)\n" +
                " group by " + channel + ".channel_id," + channel + ".channel_type\n" +
                " ) t," + channelMembers + " cm where t.channel_id=cm.channel_id and t.channel_type=cm.channel_type and t.mid=cm.id";
        Cursor cursor = YutakApplication.get().getDbHelper().rawQuery(sql, args);
        if (cursor == null) {
            return list;
        }
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            String member_name = YutakCursor.readString(cursor, "member_name");
            String member_remark = YutakCursor.readString(cursor, "member_remark");
            YutakChannel channel = serializableChannel(cursor);
            YutakChannelSearchResult result = new YutakChannelSearchResult();
            result.YutakChannel = channel;
            if (!TextUtils.isEmpty(member_remark)) {
                //优先显示备注名称
                if (member_remark.toUpperCase().contains(searchKey.toUpperCase())) {
                    result.containMemberName = member_remark;
                }
            }
            if (TextUtils.isEmpty(result.containMemberName)) {
                if (!TextUtils.isEmpty(member_name)) {
                    if (member_name.toUpperCase().contains(searchKey.toUpperCase())) {
                        result.containMemberName = member_name;
                    }
                }
            }
            list.add(result);
        }
        cursor.close();
        return list;
    }

    public synchronized List<YutakChannel> searchWithChannelType(String searchKey, byte channelType) {
        List<YutakChannel> list = new ArrayList<>();
        Object[] args = new Object[3];
        args[0] = "%" + searchKey + "%";
        args[1] = "%" + searchKey + "%";
        args[2] = channelType;
        String sql = "select * from " + channel + " where (" + DB.ChannelColumns.channel_name + " LIKE ? or " + DB.ChannelColumns.channel_remark + " LIKE ?) and " + DB.ChannelColumns.channel_type + "=?";
        try (Cursor cursor = YutakApplication.get().getDbHelper().rawQuery(sql, args)) {
            if (cursor == null) {
                return list;
            }
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                list.add(serializableChannel(cursor));
            }
        }
        return list;
    }

    public synchronized List<YutakChannel> searchWithChannelTypeAndFollow(String searchKey, byte channelType, int follow) {
        List<YutakChannel> list = new ArrayList<>();
        Object[] args = new Object[4];
        args[0] = "%" + searchKey + "%";
        args[1] = "%" + searchKey + "%";
        args[2] = channelType;
        args[3] = follow;
        String sql = "select * from " + channel + " where (" + DB.ChannelColumns.channel_name + " LIKE ? or " + DB.ChannelColumns.channel_remark + " LIKE ?) and " + DB.ChannelColumns.channel_type + "=? and " + DB.ChannelColumns.follow + "=?";
        try (Cursor cursor = YutakApplication.get().getDbHelper().rawQuery(sql, args)) {
            if (cursor == null) {
                return list;
            }
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                list.add(serializableChannel(cursor));
            }
        }
        return list;
    }

    public synchronized List<YutakChannel> queryWithChannelTypeAndFollow(byte channelType, int follow) {
        String[] args = new String[2];
        args[0] = String.valueOf(channelType);
        args[1] = String.valueOf(follow);
        String selection = DB.ChannelColumns.channel_type + "=? and " + DB.ChannelColumns.follow + "=?";
        List<YutakChannel> channels = new ArrayList<>();
        try (Cursor cursor = YutakApplication
                .get()
                .getDbHelper().select(channel, selection, args, null)) {
            if (cursor == null) {
                return channels;
            }
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                channels.add(serializableChannel(cursor));
            }
        }
        return channels;
    }
    // update with special field
    public synchronized void updateWithField(String channelID, byte channelType, String field, String value) {
        String[] updateKey = new String[]{field};
        String[] updateValue = new String[]{value};
        String where = DB.ChannelColumns.channel_id + "=? and " + DB.ChannelColumns.channel_type + "=?";
        String[] whereValue = new String[2];
        whereValue[0] = channelID;
        whereValue[1] = String.valueOf(channelType);
        YutakApplication.get().getDbHelper()
                .update(channel, updateKey, updateValue, where, whereValue);
    }
    // convert function
    public YutakChannel serializableChannel(Cursor cursor) {
        YutakChannel channel = new YutakChannel();
        channel.id = YutakCursor.readLong(cursor, DB.ChannelColumns.id);
        channel.channelID = YutakCursor.readString(cursor, DB.ChannelColumns.channel_id);
        channel.channelType = YutakCursor.readByte(cursor, DB.ChannelColumns.channel_type);
        channel.channelName = YutakCursor.readString(cursor, DB.ChannelColumns.channel_name);
        channel.channelRemark = YutakCursor.readString(cursor, DB.ChannelColumns.channel_remark);
        channel.showNick = YutakCursor.readInt(cursor, DB.ChannelColumns.show_nick);
        channel.top = YutakCursor.readInt(cursor, DB.ChannelColumns.top);
        channel.mute = YutakCursor.readInt(cursor, DB.ChannelColumns.mute);
        channel.isDeleted = YutakCursor.readInt(cursor, DB.ChannelColumns.is_deleted);
        channel.forbidden = YutakCursor.readInt(cursor, DB.ChannelColumns.forbidden);
        channel.status = YutakCursor.readInt(cursor, DB.ChannelColumns.status);
        channel.follow = YutakCursor.readInt(cursor, DB.ChannelColumns.follow);
        channel.invite = YutakCursor.readInt(cursor, DB.ChannelColumns.invite);
        channel.version = YutakCursor.readLong(cursor, DB.ChannelColumns.version);
        channel.createdAt = YutakCursor.readString(cursor, DB.ChannelColumns.created_at);
        channel.updatedAt = YutakCursor.readString(cursor, DB.ChannelColumns.updated_at);
        channel.avatar = YutakCursor.readString(cursor, DB.ChannelColumns.avatar);
        channel.online = YutakCursor.readInt(cursor, DB.ChannelColumns.online);
        channel.lastOffline = YutakCursor.readLong(cursor, DB.ChannelColumns.last_offline);
        channel.category = YutakCursor.readString(cursor, DB.ChannelColumns.category);
        channel.receipt = YutakCursor.readInt(cursor, DB.ChannelColumns.receipt);
        channel.robot = YutakCursor.readInt(cursor, DB.ChannelColumns.robot);
        channel.username = YutakCursor.readString(cursor, DB.ChannelColumns.username);
        channel.avatarCacheKey = YutakCursor.readString(cursor, DB.ChannelColumns.avatar_cache_key);
        channel.flame = YutakCursor.readInt(cursor, DB.ChannelColumns.flame);
        channel.flameSecond = YutakCursor.readInt(cursor, DB.ChannelColumns.flame_second);
        channel.deviceFlag = YutakCursor.readInt(cursor, DB.ChannelColumns.device_flag);
        channel.parentChannelID = YutakCursor.readString(cursor, DB.ChannelColumns.parent_channel_id);
        channel.parentChannelType = YutakCursor.readByte(cursor, DB.ChannelColumns.parent_channel_type);
        String extra = YutakCursor.readString(cursor, DB.ChannelColumns.localExtra);
        String remoteExtra = YutakCursor.readString(cursor, DB.ChannelColumns.remote_extra);
        channel.localExtra = getChannelExtra(extra);
        channel.remoteExtraMap = getChannelExtra(remoteExtra);
        return channel;
    }
    // get channel extra info
    public HashMap<String, Object> getChannelExtra(String extra) {
        HashMap<String, Object> hashMap = new HashMap<>();
        if (!TextUtils.isEmpty(extra)) {
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
        }
        return hashMap;
    }

}

