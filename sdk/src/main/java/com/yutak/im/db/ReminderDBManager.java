package com.yutak.im.db;


import static com.yutak.im.cs.DB.TABLE.reminders;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;

import com.yutak.im.YutakApplication;
import com.yutak.im.YutakIM;
import com.yutak.im.domain.YutakReminder;
import com.yutak.im.domain.YutakUIConversationMsg;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class ReminderDBManager {
    private ReminderDBManager() {
    }

    private static class ReminderDBManagerBinder {
        final static ReminderDBManager binder = new ReminderDBManager();
    }

    public static ReminderDBManager getInstance() {
        return ReminderDBManagerBinder.binder;
    }

    public long queryMaxVersion() {
        String sql = "select * from " + reminders + " order by version desc limit 1";
        long version = 0;
        try (Cursor cursor = YutakApplication.get()
                .getDbHelper().rawQuery(sql)) {
            if (cursor == null) {
                return 0;
            }
            if (cursor.moveToLast()) {
                version = YutakCursor.readLong(cursor, "version");
            }
        }
        return version;
    }

    public List<YutakReminder> queryWithChannelAndDone(String channelID, byte channelType, int done) {
        String sql = "select * from " + reminders + " where channel_id=? and channel_type=? and done=? order by message_seq desc";
        List<YutakReminder> list = new ArrayList<>();
        try (Cursor cursor = YutakApplication.get().getDbHelper().rawQuery(sql, new Object[]{channelID, channelType, done})) {
            if (cursor == null) {
                return list;
            }
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                YutakReminder reminder = serializeReminder(cursor);
                list.add(reminder);
            }
        }
        return list;
    }

    public List<YutakReminder> queryWithChannelAndTypeAndDone(String channelID, byte channelType, int type, int done) {
        String sql = "select * from " + reminders + " where channel_id=? and channel_type=? and done=? and type =? order by message_seq desc";
        List<YutakReminder> list = new ArrayList<>();
        try (Cursor cursor = YutakApplication.get().getDbHelper().rawQuery(sql, new Object[]{channelID, channelType, done, type})) {
            if (cursor == null) {
                return list;
            }
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                YutakReminder reminder = serializeReminder(cursor);
                list.add(reminder);
            }
        }
        return list;
    }

    private List<YutakReminder> queryWithIds(List<Long> ids) {
        StringBuilder stringBuffer = new StringBuilder();
        for (int i = 0, size = ids.size(); i < size; i++) {
            if (!TextUtils.isEmpty(stringBuffer)) {
                stringBuffer.append(",");
            }
            stringBuffer.append(ids.get(i));
        }
        String sql = "select * from " + reminders + " where reminder_id in (" + stringBuffer + ")";
        List<YutakReminder> list = new ArrayList<>();
        try (Cursor cursor = YutakApplication.get()
                .getDbHelper()
                .rawQuery(sql)) {
            if (cursor == null) {
                return list;
            }
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                YutakReminder reminder = serializeReminder(cursor);
                list.add(reminder);
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    private List<YutakReminder> queryWithChannelIds(List<String> channelIds) {
        List<YutakReminder> list = new ArrayList<>();
        try (Cursor cursor = YutakApplication.get()
                .getDbHelper()
                .select(reminders, "channel_id in (" + YutakCursor.getPlaceholders(channelIds.size()) + ")", channelIds.toArray(new String[0]), null)) {
            if (cursor == null) {
                return list;
            }
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                YutakReminder reminder = serializeReminder(cursor);
                list.add(reminder);
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    public List<YutakReminder> insertOrUpdateReminders(List<YutakReminder> list) {
        List<Long> ids = new ArrayList<>();
        List<String> channelIds = new ArrayList<>();
        for (int i = 0, size = list.size(); i < size; i++) {
            boolean isAdd = true;
            for (String channelId : channelIds) {
                if (!TextUtils.isEmpty(list.get(i).channelID) && channelId.equals(list.get(i).channelID)) {
                    isAdd = false;
                    break;
                }
            }
            if (isAdd) channelIds.add(list.get(i).channelID);
            ids.add(list.get(i).reminderID);

        }
        List<ContentValues> insertCVs = new ArrayList<>();
        List<ContentValues> updateCVs = new ArrayList<>();
        List<YutakReminder> allList = queryWithIds(ids);
        for (int i = 0, size = list.size(); i < size; i++) {
            boolean isAdd = true;
            for (YutakReminder reminder : allList) {
                if (reminder.reminderID == list.get(i).reminderID) {
                    updateCVs.add(YutakSqlContentValue.getWithCVWithReminder(list.get(i)));
                    isAdd = false;
                    break;
                }
            }
            if (isAdd) {
                insertCVs.add(YutakSqlContentValue.getWithCVWithReminder(list.get(i)));
            }
        }
        try {
            YutakApplication.get().getDbHelper().getDb()
                    .beginTransaction();
            if (insertCVs.size() > 0) {
                for (ContentValues cv : insertCVs) {
                    YutakApplication.get().getDbHelper().insert(reminders, cv);
                }
            }
            if (updateCVs.size() > 0) {
                for (ContentValues cv : updateCVs) {
                    String[] update = new String[1];
                    update[0] = cv.getAsString("reminder_id");
                    YutakApplication.get().getDbHelper()
                            .update(reminders, cv, "reminder_id=?", update);
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

        List<YutakReminder> reminderList = queryWithChannelIds(channelIds);
        HashMap<String, List<YutakReminder>> maps = listToMap(reminderList);
        List<YutakUIConversationMsg> uiMsgList = ConversationDBManager.getInstance().queryWithChannelIds(channelIds);
        for (int i = 0, size = uiMsgList.size(); i < size; i++) {
            String key = uiMsgList.get(i).channelID + "_" + uiMsgList.get(i).channelType;
            if (maps.containsKey(key)) {
                uiMsgList.get(i).setReminderList(maps.get(key));
            }
            YutakIM.get().getConversationManager().setOnRefreshMsg(uiMsgList.get(i), i == list.size() - 1, "saveReminders");
        }
        return reminderList;
    }

    private HashMap<String, List<YutakReminder>> listToMap(List<YutakReminder> list) {
        HashMap<String, List<YutakReminder>> map = new HashMap<>();
        if (list == null || list.size() == 0) {
            return map;
        }
        for (YutakReminder reminder : list) {
            String key = reminder.channelID + "_" + reminder.channelType;
            List<YutakReminder> tempList = null;
            if (map.containsKey(key)) {
                tempList = map.get(key);
            }
            if (tempList == null) tempList = new ArrayList<>();
            tempList.add(reminder);
            map.put(key, tempList);
        }
        return map;
    }

    private YutakReminder serializeReminder(Cursor cursor) {
        YutakReminder reminder = new YutakReminder();
        reminder.type = YutakCursor.readInt(cursor, "type");
        reminder.reminderID = YutakCursor.readLong(cursor, "reminder_id");
        reminder.messageID = YutakCursor.readString(cursor, "message_id");
        reminder.messageSeq = YutakCursor.readLong(cursor, "message_seq");
        reminder.isLocate = YutakCursor.readInt(cursor, "is_locate");
        reminder.channelID = YutakCursor.readString(cursor, "channel_id");
        reminder.channelType = (byte) YutakCursor.readInt(cursor, "channel_type");
        reminder.text = YutakCursor.readString(cursor, "text");
        reminder.version = YutakCursor.readLong(cursor, "version");
        reminder.done = YutakCursor.readInt(cursor, "done");
        String data = YutakCursor.readString(cursor, "data");
        reminder.needUpload = YutakCursor.readInt(cursor, "needUpload");
        reminder.publisher = YutakCursor.readString(cursor, "publisher");
        if (!TextUtils.isEmpty(data)) {
            HashMap<String, Object> hashMap = new HashMap<>();
            try {
                JSONObject jsonObject = new JSONObject(data);
                Iterator<String> keys = jsonObject.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    hashMap.put(key, jsonObject.opt(key));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            reminder.data = hashMap;
        }
        return reminder;
    }
}

