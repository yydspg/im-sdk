package com.yutak.im.db;


import static com.yutak.im.cs.DB.TABLE.channel;
import static com.yutak.im.cs.DB.TABLE.channelMembers;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;

import com.yutak.im.YutakApplication;
import com.yutak.im.cs.DB;
import com.yutak.im.domain.YutakChannelMember;
import com.yutak.im.manager.ChannelMembersManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class ChannelMemberDBManager {
    final String channelCols = channel + ".channel_remark," + channel + ".channel_name," + channel + ".avatar," + channel + ".avatar_cache_key";

    private ChannelMemberDBManager() {
    }

    private static class ChannelMembersManagerBinder {
        private final static ChannelMemberDBManager channelMembersManager = new ChannelMemberDBManager();
    }

    public static ChannelMemberDBManager getInstance() {
        return ChannelMembersManagerBinder.channelMembersManager;
    }

    public synchronized List<YutakChannelMember> search(String channelId, byte channelType, String keyword, int page, int size) {
        int queryPage = (page - 1) * size;
        Object[] args = new Object[6];
        args[0] = channelId;
        args[1] = channelType;
        args[2] = "%" + keyword + "%";
        args[3] = "%" + keyword + "%";
        args[4] = "%" + keyword + "%";
        args[5] = "%" + keyword + "%";
        String sql = "select " + channelMembers + ".*," + channelCols + " from " + channelMembers + " LEFT JOIN " + channel + " on " + channelMembers + ".member_uid=" + channel + ".channel_id and " + channel + ".channel_type=1 where " + channelMembers + ".channel_id=? and " + channelMembers + ".channel_type=? and " + channelMembers + ".is_deleted=0 and " + channelMembers + ".status=1 and (member_name like ? or member_remark like ? or channel_name like ? or channel_remark like ?) order by " + channelMembers + ".role=1 desc," + channelMembers + ".role=2 desc," + channelMembers + "." + DB.ChannelMembersColumns.created_at + " asc limit " + queryPage + "," + size;
        Cursor cursor = YutakApplication
                .get()
                .getDbHelper().rawQuery(sql, args);
        List<YutakChannelMember> list = new ArrayList<>();
        if (cursor == null) {
            return list;
        }
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            list.add(serializableChannelMember(cursor));
        }
        cursor.close();
        return list;
    }

    public synchronized List<YutakChannelMember> queryWithPage(String channelId, byte channelType, int page, int size) {
        int queryPage = (page - 1) * size;
        Object[] args = new Object[2];
        args[0] = channelId;
        args[1] = channelType;
        String sql = "select " + channelMembers + ".*," + channelCols + " from " + channelMembers + " LEFT JOIN " + channel + " on " + channelMembers + ".member_uid=" + channel + ".channel_id and " + channel + ".channel_type=1 where " + channelMembers + ".channel_id=? and " + channelMembers + ".channel_type=? and " + channelMembers + ".is_deleted=0 and " + channelMembers + ".status=1 order by " + channelMembers + ".role=1 desc," + channelMembers + ".role=2 desc," + channelMembers + "." + DB.ChannelMembersColumns.created_at + " asc limit " + queryPage + "," + size;
        Cursor cursor = YutakApplication
                .get()
                .getDbHelper().rawQuery(sql, args);
        List<YutakChannelMember> list = new ArrayList<>();
        if (cursor == null) {
            return list;
        }
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            list.add(serializableChannelMember(cursor));
        }
        cursor.close();
        return list;
    }

    /**
     * 查询某个频道的所有成员
     *
     * @param channelId 频道ID
     * @return List<YutakChannelMember>
     */
    public synchronized List<YutakChannelMember> query(String channelId, byte channelType) {
        Object[] args = new Object[2];
        args[0] = channelId;
        args[1] = channelType;
        String sql = "select " + channelMembers + ".*," + channelCols + " from " + channelMembers + " LEFT JOIN " + channel + " on " + channelMembers + ".member_uid=" + channel + ".channel_id and " + channel + ".channel_type=1 where " + channelMembers + ".channel_id=? and " + channelMembers + ".channel_type=? and " + channelMembers + ".is_deleted=0 and " + channelMembers + ".status=1 order by " + channelMembers + ".role=1 desc," + channelMembers + ".role=2 desc," + channelMembers + "." + DB.ChannelMembersColumns.created_at + " asc";
        Cursor cursor = YutakApplication
                .get()
                .getDbHelper().rawQuery(sql, args);
        List<YutakChannelMember> list = new ArrayList<>();
        if (cursor == null) {
            return list;
        }
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            list.add(serializableChannelMember(cursor));
        }
        cursor.close();
        return list;
    }

    public synchronized List<YutakChannelMember> queryDeleted(String channelId, byte channelType) {
        Object[] args = new Object[2];
        args[0] = channelId;
        args[1] = channelType;
        String sql = "select " + channelMembers + ".*," + channelCols + " from " + channelMembers + " LEFT JOIN " + channel + " on " + channelMembers + ".member_uid=" + channel + ".channel_id and " + channel + ".channel_type=1 where " + channelMembers + ".channel_id=? and " + channelMembers + ".channel_type=? and " + channelMembers + ".is_deleted=1 and " + channelMembers + ".status=1 order by " + channelMembers + ".role=1 desc," + channelMembers + ".role=2 desc," + channelMembers + "." + DB.ChannelMembersColumns.created_at + " asc";
        Cursor cursor = YutakApplication
                .get()
                .getDbHelper().rawQuery(sql, args);
        List<YutakChannelMember> list = new ArrayList<>();
        if (cursor == null) {
            return list;
        }
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            list.add(serializableChannelMember(cursor));
        }
        cursor.close();
        return list;
    }

    public synchronized boolean isExist(String channelId, byte channelType, String uid) {
        boolean isExist = false;
        Object[] args = new Object[3];
        args[0] = channelId;
        args[1] = channelType;
        args[2] = uid;
        String sql = "select " + channelMembers + ".*," + channelCols + " from " + channelMembers + " left join " + channel + " on " + channelMembers + ".member_uid = " + channel + ".channel_id AND " + channel + ".channel_type=1 where (" + channelMembers + "." + DB.ChannelMembersColumns.channel_id + "=? and " + channelMembers + "." + DB.ChannelMembersColumns.channel_type + "=? and " + channelMembers + "." + DB.ChannelMembersColumns.member_uid + "=?)";
        try (Cursor cursor = YutakApplication
                .get()
                .getDbHelper().rawQuery(sql, args)) {

            if (cursor != null && cursor.moveToLast()) {
                isExist = true;
            }
        }
        return isExist;
    }

    public List<YutakChannelMember> queryWithUIDs(String channelID, byte channelType, List<String> uidList) {
        List<String> args = new ArrayList<>();
        args.add(channelID);
        args.add(String.valueOf(channelType));
        args.addAll(uidList);
        uidList.add(String.valueOf(channelType));
        Cursor cursor = YutakApplication
                .get()
                .getDbHelper().select(channelMembers, "channel_id =? and channel_type=? and member_uid in (" + YutakCursor.getPlaceholders(uidList.size()) + ")", args.toArray(new String[0]), null);
        List<YutakChannelMember> list = new ArrayList<>();
        if (cursor == null) {
            return list;
        }
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            list.add(serializableChannelMember(cursor));
        }
        cursor.close();
        return list;
    }

    /**
     * 查询单个频道成员
     *
     * @param channelId 频道ID
     * @param uid       用户ID
     */
    public synchronized YutakChannelMember query(String channelId, byte channelType, String uid) {
        YutakChannelMember YutakChannelMember = null;
        Object[] args = new Object[3];
        args[0] = channelId;
        args[1] = channelType;
        args[2] = uid;
        String sql = "select " + channelMembers + ".*," + channelCols + " from " + channelMembers + " left join " + channel + " on " + channelMembers + ".member_uid = " + channel + ".channel_id AND " + channel + ".channel_type=1 where (" + channelMembers + "." + DB.ChannelMembersColumns.channel_id + "=? and " + channelMembers + "." + DB.ChannelMembersColumns.channel_type + "=? and " + channelMembers + "." + DB.ChannelMembersColumns.member_uid + "=?)";
        try (Cursor cursor = YutakApplication
                .get()
                .getDbHelper().rawQuery(sql, args)) {
            if (cursor == null) {
                return null;
            }
            if (cursor.moveToLast()) {
                YutakChannelMember = serializableChannelMember(cursor);
            }
        }
        return YutakChannelMember;
    }

    public synchronized void insert(YutakChannelMember channelMember) {
        if (TextUtils.isEmpty(channelMember.channelID) || TextUtils.isEmpty(channelMember.memberUID))
            return;
        ContentValues cv = new ContentValues();
        try {
            cv = YutakSqlContentValue.getWithChannelMember(channelMember);
        } catch (Exception e) {
            e.printStackTrace();
        }
        YutakApplication.get().getDbHelper()
                .insert(channelMembers, cv);
    }

    /**
     * 批量插入频道成员
     *
     * @param list List<YutakChannelMember>
     */
    public void insertMembers(List<YutakChannelMember> list) {
        List<ContentValues> updateCVList = new ArrayList<>();
        List<ContentValues> newCVList = new ArrayList<>();
        for (YutakChannelMember member : list) {
            ContentValues cv = YutakSqlContentValue.getWithChannelMember(member);
            boolean isExist = isExist(member.channelID, member.channelType, member.memberUID);
            if (isExist) {
                updateCVList.add(cv);
            } else {
                newCVList.add(cv);
            }
        }
        try {
            YutakApplication.get().getDbHelper().getDb()
                    .beginTransaction();
            if (updateCVList.size() > 0) {
                for (ContentValues cv : updateCVList) {
                    String[] update = new String[3];
                    update[0] = cv.getAsString(DB.ChannelMembersColumns.channel_id);
                    update[1] = String.valueOf(cv.getAsByte(DB.ChannelMembersColumns.channel_type));
                    update[2] = cv.getAsString(DB.ChannelMembersColumns.member_uid);
                    YutakApplication.get().getDbHelper()
                            .update(channelMembers, cv, DB.ChannelMembersColumns.channel_id + "=? and " + DB.ChannelMembersColumns.channel_type + "=? and " + DB.ChannelMembersColumns.member_uid + "=?", update);
                }
            }
            if (newCVList.size() > 0) {
                for (ContentValues cv : newCVList) {
                    YutakApplication.get().getDbHelper().insert(channelMembers, cv);
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

    public void insertMembers(List<YutakChannelMember> allMemberList, List<YutakChannelMember> existList) {
        List<ContentValues> insertCVList = new ArrayList<>();
        List<ContentValues> updateCVList = new ArrayList<>();
        for (YutakChannelMember channelMember : allMemberList) {
            boolean isAdd = true;
            for (YutakChannelMember cm : existList) {
                if (channelMember.memberUID.equals(cm.memberUID)) {
                    isAdd = false;
                    updateCVList.add(YutakSqlContentValue.getWithChannelMember(channelMember));
                    break;
                }
            }
            if (isAdd) {
                insertCVList.add(YutakSqlContentValue.getWithChannelMember(channelMember));
            }
        }
        YutakApplication.get().getDbHelper().getDb()
                .beginTransaction();
        try {
            if (insertCVList.size() > 0) {
                for (ContentValues cv : insertCVList) {
                    YutakApplication.get().getDbHelper().insert(channelMembers, cv);
                }
            }
            if (updateCVList.size() > 0) {
                for (ContentValues cv : updateCVList) {
                    String[] update = new String[3];
                    update[0] = cv.getAsString(DB.ChannelMembersColumns.channel_id);
                    update[1] = String.valueOf(cv.getAsByte(DB.ChannelMembersColumns.channel_type));
                    update[2] = cv.getAsString(DB.ChannelMembersColumns.member_uid);
                    YutakApplication.get().getDbHelper()
                            .update(channelMembers, cv, DB.ChannelMembersColumns.channel_id + "=? and " + DB.ChannelMembersColumns.channel_type + "=? and " + DB.ChannelMembersColumns.member_uid + "=?", update);
                }
            }
            YutakApplication.get().getDbHelper().getDb().setTransactionSuccessful();
        } finally {
            if (YutakApplication.get().getDbHelper().getDb().inTransaction()) {
                YutakApplication.get().getDbHelper().getDb()
                        .endTransaction();
            }
        }
    }

    public void insertOrUpdate(YutakChannelMember channelMember) {
        if (channelMember == null) return;
        if (isExist(channelMember.channelID, channelMember.channelType, channelMember.memberUID)) {
            update(channelMember);
        } else {
            insert(channelMember);
        }
    }

    /**
     * 修改某个频道的某个成员信息
     *
     * @param channelMember 成员
     */
    public synchronized void update(YutakChannelMember channelMember) {
        String[] update = new String[3];
        update[0] = channelMember.channelID;
        update[1] = String.valueOf(channelMember.channelType);
        update[2] = channelMember.memberUID;
        ContentValues cv = new ContentValues();
        try {
            cv = YutakSqlContentValue.getWithChannelMember(channelMember);
        } catch (Exception e) {
            e.printStackTrace();
        }
        YutakApplication.get().getDbHelper()
                .update(channelMembers, cv, DB.ChannelMembersColumns.channel_id + "=? and " + DB.ChannelMembersColumns.channel_type + "=? and " + DB.ChannelMembersColumns.member_uid + "=?", update);
    }

    /**
     * 根据字段修改频道成员
     *
     * @param channelID   频道ID
     * @param channelType 频道类型
     * @param uid         用户ID
     * @param field       字段
     * @param value       值
     */
    public synchronized boolean updateWithField(String channelID, byte channelType, String uid, String field, String value) {
        String[] updateKey = new String[]{field};
        String[] updateValue = new String[]{value};
        String where = DB.ChannelMembersColumns.channel_id + "=? and " + DB.ChannelMembersColumns.channel_type + "=? and " + DB.ChannelMembersColumns.member_uid + "=?";
        String[] whereValue = new String[3];
        whereValue[0] = channelID;
        whereValue[1] = String.valueOf(channelType);
        whereValue[2] = uid;
        int row = YutakApplication.get().getDbHelper()
                .update(channelMembers, updateKey, updateValue, where, whereValue);
        if (row > 0) {
            YutakChannelMember channelMember = query(channelID, channelType, uid);
            if (channelMember != null)
                //刷新频道成员信息
                ChannelMembersManager.getInstance().setRefreshChannelMember(channelMember, true);
        }
        return row > 0;
    }

    public void deleteWithChannel(String channelID, byte channelType) {
        String selection = "channel_id=? and channel_type=?";
        String[] selectionArgs = new String[2];
        selectionArgs[0] = channelID;
        selectionArgs[1] = String.valueOf(channelType);
        YutakApplication.get().getDbHelper().delete(channelMembers, selection, selectionArgs);
    }

    /**
     * 批量删除频道成员
     *
     * @param list 频道成员
     */
    public synchronized void deleteMembers(List<YutakChannelMember> list) {
        try {
            YutakApplication.get().getDbHelper().getDb()
                    .beginTransaction();
            if (list != null && list.size() > 0) {
                for (int i = 0, size = list.size(); i < size; i++) {
                    insertOrUpdate(list.get(i));
                }
                YutakApplication.get().getDbHelper().getDb()
                        .setTransactionSuccessful();
            }
        } catch (Exception ignored) {
        } finally {
            if (YutakApplication.get().getDbHelper().getDb().inTransaction()) {
                YutakApplication.get().getDbHelper().getDb()
                        .endTransaction();
            }
        }
        ChannelMembersManager.getInstance().setOnRemoveChannelMember(list);
    }

    public long queryMaxVersion(String channelID, byte channelType) {
        Object[] args = new Object[2];
        args[0] = channelID;
        args[1] = channelType;
        String sql = "select max(version) version from " + channelMembers + " where channel_id =? and channel_type=? limit 0, 1";
        long version = 0;
        try {
            if (YutakApplication.get().getDbHelper() != null) {
                Cursor cursor = YutakApplication
                        .get()
                        .getDbHelper()
                        .rawQuery(sql, args);
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        version = YutakCursor.readLong(cursor, "version");
                    }
                    cursor.close();
                }
            }
        } catch (Exception ignored) {
        }
        return version;
    }

    @Deprecated
    public synchronized YutakChannelMember queryMaxVersionMember(String channelID, byte channelType) {
        YutakChannelMember channelMember = null;
        Object[] args = new Object[2];
        args[0] = channelID;
        args[1] = channelType;
        String sql = "select * from " + channelMembers + " where " + DB.ChannelMembersColumns.channel_id + "=? and " + DB.ChannelMembersColumns.channel_type + "=? order by " + DB.ChannelMembersColumns.version + " desc limit 0,1";
        try (Cursor cursor = YutakApplication
                .get()
                .getDbHelper().rawQuery(sql, args)) {
            if (cursor == null) {
                return null;
            }
            if (cursor.moveToLast()) {
                channelMember = serializableChannelMember(cursor);
            }
        }
        return channelMember;
    }

    public synchronized List<YutakChannelMember> queryRobotMembers(String channelId, byte channelType) {
        String selection = "channel_id=? and channel_type=? and robot=1 and is_deleted=0";
        Cursor cursor = YutakApplication
                .get()
                .getDbHelper().select(channelMembers, selection, new String[]{channelId, String.valueOf(channelType)}, null);
        List<YutakChannelMember> list = new ArrayList<>();
        if (cursor == null) {
            return list;
        }
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            list.add(serializableChannelMember(cursor));
        }
        cursor.close();
        return list;
    }

    public List<YutakChannelMember> queryWithRole(String channelId, byte channelType, int role) {
        String selection = "channel_id=? AND channel_type=? AND role=? AND is_deleted=0";
        Cursor cursor = YutakApplication
                .get()
                .getDbHelper().select(channelMembers, selection, new String[]{channelId, String.valueOf(channelType), String.valueOf(role)}, null);
        List<YutakChannelMember> list = new ArrayList<>();
        if (cursor == null) {
            return list;
        }
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            list.add(serializableChannelMember(cursor));
        }
        cursor.close();
        return list;
    }

    public synchronized List<YutakChannelMember> queryWithStatus(String channelId, byte channelType, int status) {
        Object[] args = new Object[3];
        args[0] = channelId;
        args[1] = channelType;
        args[2] = status;
        String sql = "select " + channelMembers + ".*," + channel + ".channel_name," + channel + ".channel_remark," + channel + ".avatar from " + channelMembers + " left Join " + channel + " where " + channelMembers + ".member_uid = " + channel + ".channel_id AND " + channel + ".channel_type=1 AND " + channelMembers + ".channel_id=? and " + channelMembers + ".channel_type=? and " + channelMembers + ".status=? order by " + channelMembers + ".role=1 desc," + channelMembers + ".role=2 desc," + channelMembers + "." + DB.ChannelMembersColumns.created_at + " asc";
        Cursor cursor = YutakApplication
                .get()
                .getDbHelper().rawQuery(sql, args);
        List<YutakChannelMember> list = new ArrayList<>();
        if (cursor == null) {
            return list;
        }
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            list.add(serializableChannelMember(cursor));
        }
        cursor.close();
        return list;
    }

    public synchronized int queryCount(String channelID, byte channelType) {
        Object[] args = new Object[2];
        args[0] = channelID;
        args[1] = channelType;
        String sql = "select count(*) from " + channelMembers
                + " where (" + DB.ChannelMembersColumns.channel_id + "=? and "
                + DB.ChannelMembersColumns.channel_type + "=? and " + DB.ChannelMembersColumns.is_deleted + "=0 and " + DB.ChannelMembersColumns.status + "=1)";
        Cursor cursor = YutakApplication.get().getDbHelper().rawQuery(sql, args);
        cursor.moveToFirst();
        int count = cursor.getInt(0);
        cursor.close();
        return count;
    }

    //convert data
    private YutakChannelMember serializableChannelMember(Cursor cursor) {
        YutakChannelMember channelMember = new YutakChannelMember();
        channelMember.id = YutakCursor.readLong(cursor, DB.ChannelMembersColumns.id);
        channelMember.status = YutakCursor.readInt(cursor, DB.ChannelMembersColumns.status);
        channelMember.channelID = YutakCursor.readString(cursor, DB.ChannelMembersColumns.channel_id);
        channelMember.channelType = (byte) YutakCursor.readInt(cursor, DB.ChannelMembersColumns.channel_type);
        channelMember.memberUID = YutakCursor.readString(cursor, DB.ChannelMembersColumns.member_uid);
        channelMember.memberName = YutakCursor.readString(cursor, DB.ChannelMembersColumns.member_name);
        channelMember.memberAvatar = YutakCursor.readString(cursor, DB.ChannelMembersColumns.member_avatar);
        channelMember.memberRemark = YutakCursor.readString(cursor, DB.ChannelMembersColumns.member_remark);
        channelMember.role = YutakCursor.readInt(cursor, DB.ChannelMembersColumns.role);
        channelMember.isDeleted = YutakCursor.readInt(cursor, DB.ChannelMembersColumns.is_deleted);
        channelMember.version = YutakCursor.readLong(cursor, DB.ChannelMembersColumns.version);
        channelMember.createdAt = YutakCursor.readString(cursor, DB.ChannelMembersColumns.created_at);
        channelMember.updatedAt = YutakCursor.readString(cursor, DB.ChannelMembersColumns.updated_at);
        channelMember.memberInviteUID = YutakCursor.readString(cursor, DB.ChannelMembersColumns.member_invite_uid);
        channelMember.robot = YutakCursor.readInt(cursor, DB.ChannelMembersColumns.robot);
        channelMember.forbiddenExpirationTime = YutakCursor.readLong(cursor, DB.ChannelMembersColumns.forbidden_expiration_time);
        String channelName = YutakCursor.readString(cursor, DB.ChannelColumns.channel_name);
        if (!TextUtils.isEmpty(channelName)) channelMember.memberName = channelName;
        channelMember.remark = YutakCursor.readString(cursor, DB.ChannelColumns.channel_remark);
        channelMember.memberAvatar = YutakCursor.readString(cursor, DB.ChannelColumns.avatar);
        String avatarCache = YutakCursor.readString(cursor, DB.ChannelColumns.avatar_cache_key);
        if (!TextUtils.isEmpty(avatarCache)) {
            channelMember.memberAvatarCacheKey = avatarCache;
        } else {
            channelMember.memberAvatarCacheKey = YutakCursor.readString(cursor, DB.ChannelMembersColumns.memberAvatarCacheKey);
        }
        String extra = YutakCursor.readString(cursor, DB.ChannelMembersColumns.extra);
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
            channelMember.extraMap = hashMap;
        }
        return channelMember;
    }
}
