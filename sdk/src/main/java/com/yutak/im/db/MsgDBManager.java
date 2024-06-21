package com.yutak.im.db;

import static com.yutak.im.cs.DB.TABLE.channel;
import static com.yutak.im.cs.DB.TABLE.message;
import static com.yutak.im.cs.DB.TABLE.messageExtra;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.yutak.im.YutakApplication;
import com.yutak.im.YutakIM;
import com.yutak.im.cs.DB;
import com.yutak.im.cs.Yutak;
import com.yutak.im.domain.YutakChannel;
import com.yutak.im.domain.YutakChannelMember;
import com.yutak.im.domain.YutakChannelType;
import com.yutak.im.domain.YutakMessageGroupByDate;
import com.yutak.im.domain.YutakMessageSearchResult;
import com.yutak.im.domain.YutakMsg;
import com.yutak.im.domain.YutakMsgExtra;
import com.yutak.im.domain.YutakMsgReaction;
import com.yutak.im.domain.YutakMsgSetting;
import com.yutak.im.interfaces.IGetOrSyncHistoryMsgBack;
import com.yutak.im.kit.TypeKit;
import com.yutak.im.manager.MsgManager;
import com.yutak.im.msgmodel.YutakMessageContent;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

// message manager
public class MsgDBManager {
    private final String extraCols = "IFNULL(" + messageExtra + ".readed,0) as readed,IFNULL(" + messageExtra + ".readed_count,0) as readed_count,IFNULL(" + messageExtra + ".unread_count,0) as unread_count,IFNULL(" + messageExtra + ".revoke,0) as revoke,IFNULL(" + messageExtra + ".revoker,'') as revoker,IFNULL(" + messageExtra + ".extra_version,0) as extra_version,IFNULL(" + messageExtra + ".is_mutual_deleted,0) as is_mutual_deleted,IFNULL(" + messageExtra + ".content_edit,'') as content_edit,IFNULL(" + messageExtra + ".edited_at,0) as edited_at";
    private final String messageCols = message + ".client_seq," + message + ".message_id," + message + ".message_seq," + message + ".channel_id," + message + ".channel_type," + message + ".timestamp," + message + ".from_uid," + message + ".type," + message + ".content," + message + ".status," + message + ".voice_status," + message + ".created_at," + message + ".updated_at," + message + ".searchable_word," + message + ".client_msg_no," + message + ".setting," + message + ".order_seq," + message + ".extra," + message + ".is_deleted," + message + ".flame," + message + ".flame_second," + message + ".viewed," + message + ".viewed_at," + message + ".expire_time," + message + ".expire_timestamp";

    private MsgDBManager() {
    }

    private static class MsgDbManagerBinder {
        static final MsgDBManager db = new MsgDBManager();
    }

    public static MsgDBManager getInstance() {
        return MsgDbManagerBinder.db;
    }

    private int requestCount;
    private int more = 1;

    public void queryOrSyncHistoryMessages(String channelId, byte channelType, long oldestOrderSeq, boolean contain, int pullMode, int limit, final IGetOrSyncHistoryMsgBack iGetOrSyncHistoryMsgBack) {
        //获取原始数据
        List<YutakMsg> list = queryMessages(channelId, channelType, oldestOrderSeq, contain, pullMode, limit);
        if (more == 0) {
            new Handler(Looper.getMainLooper()).post(() -> iGetOrSyncHistoryMsgBack.onResult(list));
            more = 1;
            return;
        }
        //业务判断数据
        List<YutakMsg> tempList = new ArrayList<>();
        for (int i = 0, size = list.size(); i < size; i++) {
            tempList.add(list.get(i));
        }

        //先通过message_seq排序
        if (tempList.size() > 0)
            Collections.sort(tempList, (o1, o2) -> (o1.messageSeq - o2.messageSeq));
        //获取最大和最小messageSeq
        long minMessageSeq = 0;
        long maxMessageSeq = 0;
        for (int i = 0, size = tempList.size(); i < size; i++) {
            if (tempList.get(i).messageSeq != 0) {
                if (minMessageSeq == 0) minMessageSeq = tempList.get(i).messageSeq;
                if (tempList.get(i).messageSeq > maxMessageSeq)
                    maxMessageSeq = tempList.get(i).messageSeq;
                if (tempList.get(i).messageSeq < minMessageSeq)
                    minMessageSeq = tempList.get(i).messageSeq;
            }
        }
        //是否同步消息
        boolean isSyncMsg = false;
        long startMsgSeq = 0;
        long endMsgSeq = 0;
        //判断页与页之间是否连续
        long oldestMsgSeq;
        //如果获取到的messageSeq为0说明oldestOrderSeq这条消息是本地消息则获取他上一条或下一条消息的messageSeq做为判断
        if (oldestOrderSeq % 1000 != 0)
            oldestMsgSeq = queryMsgSeq(channelId, channelType, oldestOrderSeq, pullMode);
        else oldestMsgSeq = oldestOrderSeq / 1000;
        if (pullMode == 0) {
            //下拉获取消息
            if (maxMessageSeq != 0 && oldestMsgSeq != 0 && oldestMsgSeq - maxMessageSeq > 1) {
                isSyncMsg = true;
                startMsgSeq = oldestMsgSeq;
                endMsgSeq = maxMessageSeq;
                // 从大往小同步
//                if (oldestMsgSeq - maxMessageSeq > 1) {
//                    startMsgSeq = oldestMsgSeq;
//                    endMsgSeq = maxMessageSeq;
//                } else {
//                    startMsgSeq = maxMessageSeq;
//                    endMsgSeq = oldestMsgSeq;
//                }

            }
        } else {
            //上拉获取消息
            if (minMessageSeq != 0 && oldestMsgSeq != 0 && minMessageSeq - oldestMsgSeq > 1) {
                isSyncMsg = true;
                startMsgSeq = oldestMsgSeq;
                endMsgSeq = minMessageSeq;
                // 从小往大同步
//                if (minMessageSeq - oldestMsgSeq > 1) {
//                    startMsgSeq = oldestMsgSeq;
//                    endMsgSeq = minMessageSeq;
//                } else {
//                    startMsgSeq = minMessageSeq;
//                    endMsgSeq = oldestMsgSeq;
//                }

            }
        }
        if (!isSyncMsg) {
            //判断当前页是否连续
            for (int i = 0, size = tempList.size(); i < size; i++) {
                int nextIndex = i + 1;
                if (nextIndex < tempList.size()) {
                    if (tempList.get(nextIndex).messageSeq != 0 && tempList.get(i).messageSeq != 0 &&
                            tempList.get(nextIndex).messageSeq - tempList.get(i).messageSeq > 1) {
                        //判断该条消息是否被删除
                        int num = getDeletedCount(tempList.get(i).messageSeq, tempList.get(nextIndex).messageSeq, channelId, channelType);
                        if (num < (tempList.get(nextIndex).messageSeq - tempList.get(i).messageSeq) - 1) {
                            isSyncMsg = true;
                            long max = tempList.get(nextIndex).messageSeq;
                            long min = tempList.get(i).messageSeq;
                            if (tempList.get(nextIndex).messageSeq < tempList.get(i).messageSeq) {
                                max = tempList.get(i).messageSeq;
                                min = tempList.get(nextIndex).messageSeq;
                            }
                            if (pullMode == 0) {
                                // 下拉
                                startMsgSeq = max;
                                endMsgSeq = min;
                            } else {
                                startMsgSeq = min;
                                endMsgSeq = max;
                            }
                            break;
                        }
                    }
                }
            }
        }
        if (oldestOrderSeq == 0) {
            isSyncMsg = true;
            startMsgSeq = 0;
            endMsgSeq = 0;
        }
        if (!isSyncMsg) {
            if (minMessageSeq == 1) {
                requestCount = 0;
                more = 1;
                new Handler(Looper.getMainLooper()).post(() -> iGetOrSyncHistoryMsgBack.onResult(list));
                return;
            }
        }
        //计算最后一页后是否还存在消息
        if (!isSyncMsg && tempList.size() < limit) {
            isSyncMsg = true;
            startMsgSeq = oldestMsgSeq;
            endMsgSeq = 0;
        }
        if (startMsgSeq == 0 && endMsgSeq == 0 && tempList.size() < limit) {
            isSyncMsg = true;
            endMsgSeq = oldestMsgSeq;
            startMsgSeq = 0;
        }

        if (isSyncMsg && requestCount < 5) {
            if (requestCount == 0) {
                new Handler(Looper.getMainLooper()).post(iGetOrSyncHistoryMsgBack::onSyncing);
            }
            //同步消息
            requestCount++;
            MsgManager.getInstance().setSyncChannelMsgListener(channelId, channelType, startMsgSeq, endMsgSeq, limit, pullMode, syncChannelMsg -> {
                if (syncChannelMsg != null) {
                    if (oldestMsgSeq == 0) {
                        requestCount = 5;
                    }
                    more = syncChannelMsg.more;
                    queryOrSyncHistoryMessages(channelId, channelType, oldestOrderSeq, contain, pullMode, limit, iGetOrSyncHistoryMsgBack);
                } else {
                    requestCount = 0;
                    more = 1;
                    new Handler(Looper.getMainLooper()).post(() -> iGetOrSyncHistoryMsgBack.onResult(list));
                }
            });
        } else {
            requestCount = 0;
            more = 1;
            new Handler(Looper.getMainLooper()).post(() -> iGetOrSyncHistoryMsgBack.onResult(list));
        }

    }

    public List<YutakMsg> queryWithFlame() {
        String sql = "select * from " + message + " where " + DB.MessageColumns.flame + "=1 and " + DB.MessageColumns.is_deleted + "=0";
        List<YutakMsg> list = new ArrayList<>();
        try (Cursor cursor = YutakApplication.get().getDbHelper().rawQuery(sql)) {
            if (cursor == null) {
                return list;
            }
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                YutakMsg extra = serializeMsg(cursor);
                list.add(extra);
            }
        }
        return list;
    }

    /**
     * 获取被删除的条数
     *
     * @param minMessageSeq 最大messageSeq
     * @param maxMessageSeq 最小messageSeq
     * @param channelID     频道ID
     * @param channelType   频道类型
     * @return 删除条数
     */
    private int getDeletedCount(long minMessageSeq, long maxMessageSeq, String channelID, byte channelType) {
        String sql = "select count(*) num from " + message + " where " + DB.MessageColumns.channel_id + "=? and " + DB.MessageColumns.channel_type + "=? and " + DB.MessageColumns.message_seq + ">? and " + DB.MessageColumns.message_seq + "<? and " + DB.MessageColumns.is_deleted + "=1";
        Cursor cursor = null;
        int num = 0;
        try {
            cursor = YutakApplication
                    .get()
                    .getDbHelper().rawQuery(sql, new Object[]{channelID, channelType, minMessageSeq, maxMessageSeq});
            if (cursor == null) {
                return 0;
            }
            if (cursor.moveToLast()) {
                num = YutakCursor.readInt(cursor, "num");
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return num;
    }

    private List<YutakMsg> queryMessages(String channelId, byte channelType, long oldestOrderSeq, boolean contain, int pullMode, int limit) {
        List<YutakMsg> msgList = new ArrayList<>();
        String sql;
        Object[] args;
        if (oldestOrderSeq <= 0) {
            sql = "SELECT * FROM (SELECT " + messageCols + "," + extraCols + " FROM " + message + " LEFT JOIN " + messageExtra + " on " + message + ".message_id=" + messageExtra + ".message_id WHERE " + message + ".channel_id=? and " + message + ".channel_type=? and " + message + ".type<>0 and " + message + ".type<>99) where is_deleted=0 and is_mutual_deleted=0 order by order_seq desc limit 0," + limit;
            args = new Object[2];
            args[0] = channelId;
            args[1] = channelType;
        } else {
            if (pullMode == 0) {
                if (contain) {
                    sql = "SELECT * FROM (SELECT " + messageCols + "," + extraCols + " FROM " + message + " LEFT JOIN " + messageExtra + " on " + message + ".message_id=" + messageExtra + ".message_id WHERE " + message + ".channel_id=? and " + message + ".channel_type=? and " + message + ".type<>0 and " + message + ".type<>99 AND " + message + ".order_seq<=?) where is_deleted=0 and is_mutual_deleted=0 order by order_seq desc limit 0," + limit;
                } else {
                    sql = "SELECT * FROM (SELECT " + messageCols + "," + extraCols + " FROM " + message + " LEFT JOIN " + messageExtra + " on " + message + ".message_id=" + messageExtra + ".message_id WHERE " + message + ".channel_id=? and " + message + ".channel_type=? and " + message + ".type<>0 and " + message + ".type<>99 AND " + message + ".order_seq<?) where is_deleted=0 and is_mutual_deleted=0 order by order_seq desc limit 0," + limit;
                }
            } else {
                if (contain) {
                    sql = "SELECT * FROM (SELECT " + messageCols + "," + extraCols + " FROM " + message + " LEFT JOIN " + messageExtra + " on " + message + ".message_id=" + messageExtra + ".message_id WHERE " + message + ".channel_id=? and " + message + ".channel_type=? and " + message + ".type<>0 and " + message + ".type<>99 AND " + message + ".order_seq>=?) where is_deleted=0 and is_mutual_deleted=0 order by order_seq asc limit 0," + limit;
                } else {
                    sql = "SELECT * FROM (SELECT " + messageCols + "," + extraCols + " FROM " + message + " LEFT JOIN " + messageExtra + " on " + message + ".message_id=" + messageExtra + ".message_id WHERE " + message + ".channel_id=? and " + message + ".channel_type=? and " + message + ".type<>0 and " + message + ".type<>99 AND " + message + ".order_seq>?) where is_deleted=0 and is_mutual_deleted=0 order by order_seq asc limit 0," + limit;
                }
            }
            args = new Object[3];
            args[0] = channelId;
            args[1] = channelType;
            args[2] = oldestOrderSeq;
        }
        Cursor cursor = null;
        List<String> messageIds = new ArrayList<>();
        List<String> replyMsgIds = new ArrayList<>();
        List<String> fromUIDs = new ArrayList<>();
        try {
            cursor = YutakApplication.get().getDbHelper().rawQuery(sql, args);
            if (cursor == null) {
                return msgList;
            }
            YutakChannel YutakChannel = ChannelDBManager.getInstance().query(channelId, channelType);
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                YutakMsg YutakMsg = serializeMsg(cursor);
                YutakMsg.setChannelInfo(YutakChannel);
                if (!TextUtils.isEmpty(YutakMsg.messageID)) {
                    messageIds.add(YutakMsg.messageID);
                }
                if (YutakMsg.baseContentMsgModel != null && YutakMsg.baseContentMsgModel.reply != null && !TextUtils.isEmpty(YutakMsg.baseContentMsgModel.reply.message_id)) {
                    replyMsgIds.add(YutakMsg.baseContentMsgModel.reply.message_id);
                }
                if (!TextUtils.isEmpty(YutakMsg.fromUID))
                    fromUIDs.add(YutakMsg.fromUID);
                if (pullMode == 0)
                    msgList.add(0, YutakMsg);
                else msgList.add(YutakMsg);
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        //扩展消息
        List<YutakMsgReaction> list = MsgReactionDBManager.getInstance().queryWithMessageIds(messageIds);
        if (list != null && list.size() > 0) {
            for (int i = 0, size = msgList.size(); i < size; i++) {
                for (int j = 0, len = list.size(); j < len; j++) {
                    if (list.get(j).messageID.equals(msgList.get(i).messageID)) {
                        if (msgList.get(i).reactionList == null)
                            msgList.get(i).reactionList = new ArrayList<>();
                        msgList.get(i).reactionList.add(list.get(j));
                    }
                }
            }
        }
        // 发送者成员信息
        if (channelType == YutakChannelType.GROUP) {
            List<YutakChannelMember> memberList = ChannelMemberDBManager.getInstance().queryWithUIDs(channelId, channelType, fromUIDs);
            if (memberList != null && memberList.size() > 0) {
                for (YutakChannelMember member : memberList) {
                    for (int i = 0, size = msgList.size(); i < size; i++) {
                        if (!TextUtils.isEmpty(msgList.get(i).fromUID) && msgList.get(i).fromUID.equals(member.memberUID)) {
                            msgList.get(i).setMemberOfFrom(member);
                        }
                    }
                }
            }
        }
        //消息发送者信息
        List<YutakChannel> YutakChannels = ChannelDBManager.getInstance().queryWithChannelIdsAndChannelType(fromUIDs, YutakChannelType.PERSONAL);
        if (YutakChannels != null && YutakChannels.size() > 0) {
            for (YutakChannel YutakChannel : YutakChannels) {
                for (int i = 0, size = msgList.size(); i < size; i++) {
                    if (!TextUtils.isEmpty(msgList.get(i).fromUID) && msgList.get(i).fromUID.equals(YutakChannel.channelID)) {
                        msgList.get(i).setFrom(YutakChannel);
                    }
                }
            }
        }
        // 被回复消息的编辑
        if (replyMsgIds.size() > 0) {
            List<YutakMsgExtra> msgExtraList = queryMsgExtrasWithMsgIds(replyMsgIds);
            if (msgExtraList.size() > 0) {
                for (YutakMsgExtra extra : msgExtraList) {
                    for (int i = 0, size = msgList.size(); i < size; i++) {
                        if (msgList.get(i).baseContentMsgModel != null
                                && msgList.get(i).baseContentMsgModel.reply != null
                                && extra.messageID.equals(msgList.get(i).baseContentMsgModel.reply.message_id)) {
                            msgList.get(i).baseContentMsgModel.reply.revoke = extra.revoke;
                        }
                        if (!TextUtils.isEmpty(extra.contentEdit) && msgList.get(i).baseContentMsgModel != null
                                && msgList.get(i).baseContentMsgModel.reply != null
                                && !TextUtils.isEmpty(msgList.get(i).baseContentMsgModel.reply.message_id)
                                && extra.messageID.equals(msgList.get(i).baseContentMsgModel.reply.message_id)) {
                            msgList.get(i).baseContentMsgModel.reply.editAt = extra.editedAt;
                            msgList.get(i).baseContentMsgModel.reply.contentEdit = extra.contentEdit;
                            msgList.get(i).baseContentMsgModel.reply.contentEditMsgModel = MsgManager.getInstance().getMsgContentModel(extra.contentEdit);
                            break;
                        }
                    }
                }
            }
        }
        return msgList;
    }

    public List<YutakMsg> queryAll() {
        String sql = "select * from " + message;

        List<YutakMsg> YutakMsgs = new ArrayList<>();
        try (Cursor cursor = YutakApplication.get().getDbHelper().rawQuery(sql)) {
            if (cursor == null) {
                return YutakMsgs;
            }
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                YutakMsg msg = serializeMsg(cursor);
                YutakMsgs.add(msg);
            }
        }
        return YutakMsgs;
    }

    public List<YutakMsg> queryExpireMessages(long timestamp, int limit) {
        String sql = "SELECT * from " + message + " where is_deleted=0 and " + DB.MessageColumns.expire_time + ">0 and " + DB.MessageColumns.expire_timestamp + "<=? order by order_seq desc limit 0," + limit;
        List<YutakMsg> list = new ArrayList<>();
        try (Cursor cursor = YutakApplication.get().getDbHelper().rawQuery(sql, new Object[]{timestamp})) {
            if (cursor == null) {
                return list;
            }
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                YutakMsg YutakMsg = serializeMsg(cursor);
                list.add(YutakMsg);
            }
        }
        return list;
    }

    public List<YutakMsg> queryWithFromUID(String channelID, byte channelType, String fromUID, long oldestOrderSeq, int limit) {
        String sql;
        Object[] args;
        if (oldestOrderSeq == 0) {
            args = new Object[3];
            args[0] = channelID;
            args[1] = channelType;
            args[2] = fromUID;
            sql = "SELECT * FROM (SELECT " + messageCols + "," + extraCols + " FROM " + message + " LEFT JOIN " + messageExtra + " on " + message + ".message_id=" + messageExtra + ".message_id WHERE " + message + ".channel_id=? and " + message + ".channel_type=? and from_uid=? and " + message + ".type<>0 and " + message + ".type<>99) where is_deleted=0 and revoke=0 order by order_seq desc limit 0," + limit;
        } else {
            args = new Object[4];
            args[0] = channelID;
            args[1] = channelType;
            args[2] = fromUID;
            args[3] = oldestOrderSeq;
            sql = "SELECT * FROM (SELECT " + messageCols + "," + extraCols + " FROM " + message + " LEFT JOIN " + messageExtra + " on " + message + ".message_id=" + messageExtra + ".message_id WHERE " + message + ".channel_id=? and " + message + ".channel_type=? and from_uid=? and " + message + ".type<>0 and " + message + ".type<>99 AND " + message + ".order_seq<?) where is_deleted=0 and revoke=0 order by order_seq desc limit 0," + limit;
        }
        List<YutakMsg> YutakMsgList = new ArrayList<>();
        try (Cursor cursor = YutakApplication.get().getDbHelper().rawQuery(sql, args)) {
            if (cursor == null) {
                return YutakMsgList;
            }
            YutakChannel YutakChannel = ChannelDBManager.getInstance().query(channelID, channelType);
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                YutakMsg YutakMsg = serializeMsg(cursor);
                YutakMsg.setChannelInfo(YutakChannel);
                if (channelType == YutakChannelType.GROUP) {
                    //查询群成员信息
                    YutakChannelMember member = ChannelMemberDBManager.getInstance().query(channelID, YutakChannelType.GROUP, YutakMsg.fromUID);
                    YutakMsg.setMemberOfFrom(member);
                }
                YutakMsgList.add(YutakMsg);
            }
        }
        return YutakMsgList;
    }

    public long queryOrderSeq(String channelID, byte channelType, long maxOrderSeq, int limit) {
        long minOrderSeq = 0;
        String sql = "select order_seq from " + message + " where " + DB.MessageColumns.channel_id + "=? and " + DB.MessageColumns.channel_type + "=? and type<>99 and order_seq <=? order by " + DB.MessageColumns.order_seq + " desc limit " + limit;
        try (Cursor cursor = YutakApplication.get().getDbHelper().rawQuery(sql, new Object[]{channelID, channelType, maxOrderSeq})) {
            if (cursor == null) {
                return minOrderSeq;
            }
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                minOrderSeq = YutakCursor.readLong(cursor, "order_seq");
            }
        }
        return minOrderSeq;
    }

    public long queryMaxOrderSeqWithChannel(String channelID, byte channelType) {
        long maxOrderSeq = 0;
        String sql = "select max(order_seq) order_seq from " + message + " where " + DB.MessageColumns.channel_id + "=? and " + DB.MessageColumns.channel_type + "=? and type<>99 and type<>0 and is_deleted=0";
        try {
            if (YutakApplication.get().getDbHelper() != null) {
                Cursor cursor = YutakApplication
                        .get()
                        .getDbHelper()
                        .rawQuery(sql, new Object[]{channelID, channelType});
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        maxOrderSeq = YutakCursor.readLong(cursor, "order_seq");
                    }
                    cursor.close();
                }
            }
        } catch (Exception ignored) {
        }
        return maxOrderSeq;
    }

    public synchronized YutakMsg updateMsgSendStatus(long clientSeq, long messageSeq, String messageID, int sendStatus) {

        String[] updateKey = new String[4];
        String[] updateValue = new String[4];

        updateKey[0] = DB.MessageColumns.status;
        updateValue[0] = String.valueOf(sendStatus);

        updateKey[1] = DB.MessageColumns.message_id;
        updateValue[1] = String.valueOf(messageID);

        updateKey[2] = DB.MessageColumns.message_seq;
        updateValue[2] = String.valueOf(messageSeq);

        YutakMsg msg = queryWithClientSeq(clientSeq);

        updateKey[3] = DB.MessageColumns.order_seq;
        if (msg != null)
            updateValue[3] = String.valueOf(MsgManager.getInstance().getMessageOrderSeq(messageSeq, msg.channelID, msg.channelType));
        else updateValue[3] = "0";

        String where = DB.MessageColumns.client_seq + "=?";
        String[] whereValue = new String[1];
        whereValue[0] = String.valueOf(clientSeq);

        int row = YutakApplication.get().getDbHelper()
                .update(message, updateKey, updateValue, where, whereValue);
        if (row > 0 && msg != null) {
            msg.status = sendStatus;
            msg.messageID = messageID;
            msg.messageSeq = (int) messageSeq;
            msg.orderSeq = MsgManager.getInstance().getMessageOrderSeq(messageSeq, msg.channelID, msg.channelType);
            YutakIM.get().getMsgManager().setRefreshMsg(msg, true);
        }
        return msg;
    }

    public synchronized void insertMsgs(List<YutakMsg> list) {
        if (list == null || list.size() == 0) return;
        if (list.size() == 1) {
            insert(list.get(0));
            return;
        }
        List<YutakMsg> saveList = new ArrayList<>();
        for (int i = 0, size = list.size(); i < size; i++) {
            boolean isExist = false;
            for (int j = 0, len = saveList.size(); j < len; j++) {
                if (list.get(i).clientMsgNO.equals(saveList.get(j).clientMsgNO)) {
                    isExist = true;
                    break;
                }
            }
            if (isExist) {
                list.get(i).clientMsgNO = YutakIM.get().getMsgManager().createClientMsgNO();
                list.get(i).isDeleted = 1;
            }
            saveList.add(list.get(i));
        }
        List<String> clientMsgNos = new ArrayList<>();
        List<String> msgIds = new ArrayList<>();
        List<YutakMsg> existMsgList = new ArrayList<>();
        List<YutakMsg> msgIdExistMsgList = new ArrayList<>();
        for (int i = 0, size = saveList.size(); i < size; i++) {
            boolean isSave = YutakIM.get().getMsgManager().setMessageStoreBeforeIntercept(saveList.get(i));
            if (!isSave) {
                saveList.get(i).isDeleted = 1;
            }
            if (saveList.get(i).setting == null) {
                saveList.get(i).setting = new YutakMsgSetting();
            }
            if (msgIds.size() == 200) {
                List<YutakMsg> tempList = queryWithMsgIds(msgIds);
                if (tempList != null && tempList.size() > 0) {
                    msgIdExistMsgList.addAll(tempList);
                }
                msgIds.clear();
            }
            if (clientMsgNos.size() == 200) {
                List<YutakMsg> tempList = queryWithClientMsgNos(clientMsgNos);
                if (tempList != null && tempList.size() > 0)
                    existMsgList.addAll(tempList);
                clientMsgNos.clear();
            }
            if (!TextUtils.isEmpty(saveList.get(i).messageID)) {
                msgIds.add(saveList.get(i).messageID);
            }
            if (!TextUtils.isEmpty(saveList.get(i).clientMsgNO))
                clientMsgNos.add(saveList.get(i).clientMsgNO);
        }
        if (msgIds.size() > 0) {
            List<YutakMsg> tempList = queryWithMsgIds(msgIds);
            if (tempList != null && tempList.size() > 0) {
                msgIdExistMsgList.addAll(tempList);
            }
            msgIds.clear();
        }
        if (clientMsgNos.size() > 0) {
            List<YutakMsg> tempList = queryWithClientMsgNos(clientMsgNos);
            if (tempList != null && tempList.size() > 0) {
                existMsgList.addAll(tempList);
            }
            clientMsgNos.clear();
        }
        List<YutakMsg> insertMsgList = new ArrayList<>();
        for (YutakMsg msg : saveList) {
            if (TextUtils.isEmpty(msg.clientMsgNO) || TextUtils.isEmpty(msg.messageID)) {
                continue;
            }
            boolean isAdd = true;
            for (YutakMsg tempMsg : existMsgList) {
                if (tempMsg == null || TextUtils.isEmpty(tempMsg.clientMsgNO)) {
                    continue;
                }
                if (tempMsg.clientMsgNO.equals(msg.clientMsgNO)) {
                    if (msg.isDeleted == tempMsg.isDeleted && tempMsg.isDeleted == 1) {
                        isAdd = false;
                    }
                    msg.isDeleted = 1;
                    msg.clientMsgNO = YutakIM.get().getMsgManager().createClientMsgNO();
                    break;
                }
            }
            if (isAdd) {
                for (YutakMsg tempMsg : msgIdExistMsgList) {
                    if (tempMsg == null || TextUtils.isEmpty(tempMsg.messageID)) {
                        continue;
                    }
                    if (msg.messageID.equals(tempMsg.messageID)) {
                        isAdd = false;
                        break;
                    }
                }
            }
            if (isAdd) {
                insertMsgList.add(msg);
            }

        }
        //  insertMsgList(insertMsgList);
        List<ContentValues> cvList = new ArrayList<>();
        for (YutakMsg YutakMsg : insertMsgList) {
            ContentValues cv = YutakSqlContentValue.getWithMsg(YutakMsg);
            cvList.add(cv);
        }
        try {
            YutakApplication.get().getDbHelper().getDb().beginTransaction();
            for (ContentValues cv : cvList) {
                YutakApplication.get().getDbHelper()
                        .insert(message, cv);
            }
            YutakApplication.get().getDbHelper().getDb().setTransactionSuccessful();
        } finally {
            YutakApplication.get().getDbHelper().getDb().endTransaction();
        }
    }

    public List<YutakMsg> queryWithClientMsgNos(List<String> clientMsgNos) {
        List<YutakMsg> msgs = new ArrayList<>();
        try (Cursor cursor = YutakApplication.get().getDbHelper().select(message, "client_msg_no in (" + YutakCursor.getPlaceholders(clientMsgNos.size()) + ")", clientMsgNos.toArray(new String[0]), null)) {
            if (cursor == null) {
                return msgs;
            }
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                YutakMsg msg = serializeMsg(cursor);
                msgs.add(msg);
            }
        }
        return msgs;
    }

    public synchronized long insert(YutakMsg msg) {
        boolean isSave = YutakIM.get().getMsgManager().setMessageStoreBeforeIntercept(msg);
        if (!isSave) {
            msg.isDeleted = 1;
        }
        //客户端id存在表示该条消息已存过库
        if (msg.clientSeq != 0) {
            update(msg);
            return msg.clientSeq;
        }
        if (!TextUtils.isEmpty(msg.clientMsgNO)) {
            boolean isExist = isExist(msg.clientMsgNO);
            if (isExist) {
                msg.isDeleted = 1;
                msg.clientMsgNO = YutakIM.get().getMsgManager().createClientMsgNO();
            }
        }
        ContentValues cv = new ContentValues();
        try {
            cv = YutakSqlContentValue.getWithMsg(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
        long result = -1;
        try {
            result = YutakApplication.get().getDbHelper()
                    .insert(message, cv);
        } catch (Exception ignored) {
        }

        return result;
    }

    public synchronized void updateViewedAt(int viewed, long viewedAt, String clientMsgNo) {
        String[] updateKey = new String[2];
        String[] updateValue = new String[2];
        updateKey[0] = DB.MessageColumns.viewed;
        updateValue[0] = String.valueOf(viewed);
        updateKey[1] = DB.MessageColumns.viewed_at;
        updateValue[1] = String.valueOf(viewedAt);
        String where = DB.MessageColumns.client_msg_no + "=?";
        String[] whereValue = new String[1];
        whereValue[0] = clientMsgNo;
        YutakApplication.get().getDbHelper()
                .update(message, updateKey, updateValue, where, whereValue);
    }

    private synchronized void update(YutakMsg msg) {
        String[] updateKey = new String[4];
        String[] updateValue = new String[4];
        updateKey[0] = DB.MessageColumns.content;
        updateValue[0] = msg.content;

        updateKey[1] = DB.MessageColumns.status;
        updateValue[1] = String.valueOf(msg.status);

        updateKey[2] = DB.MessageColumns.message_id;
        updateValue[2] = msg.messageID;

        updateKey[3] = DB.MessageColumns.extra;
        updateValue[3] = msg.getLocalMapExtraString();
        String where = DB.MessageColumns.client_seq + "=?";
        String[] whereValue = new String[1];
        whereValue[0] = String.valueOf(msg.clientSeq);
        YutakApplication.get().getDbHelper()
                .update(message, updateKey, updateValue, where, whereValue);

    }

    public boolean isExist(String clientMsgNo) {
        boolean isExist = false;
        String sql = "select * from " + message + " where " + DB.MessageColumns.client_msg_no + "=?";
        try (Cursor cursor = YutakApplication
                .get()
                .getDbHelper().rawQuery(sql, new Object[]{clientMsgNo})) {
            if (cursor != null && cursor.moveToLast()) {
                isExist = true;
            }
        }
        return isExist;
    }

    public YutakMsg queryWithClientMsgNo(String clientMsgNo) {
        YutakMsg YutakMsg = null;
        String sql = "select " + messageCols + "," + extraCols + " from " + message + " LEFT JOIN " + messageExtra + " ON " + message + ".message_id=" + messageExtra + ".message_id WHERE " + message + ".client_msg_no=?";
        try (Cursor cursor = YutakApplication
                .get()
                .getDbHelper().rawQuery(sql, new Object[]{clientMsgNo})) {
            if (cursor == null) {
                return null;
            }
            if (cursor.moveToLast()) {
                YutakMsg = serializeMsg(cursor);
            }
        }
        if (YutakMsg != null)
            YutakMsg.reactionList = MsgReactionDBManager.getInstance().queryWithMessageId(YutakMsg.messageID);
        return YutakMsg;
    }


    public YutakMsg queryWithClientSeq(long clientSeq) {
        YutakMsg msg = null;
        try (Cursor cursor = YutakApplication
                .get()
                .getDbHelper().select(message, "client_seq=?", new String[]{String.valueOf(clientSeq)}, null)) {
            if (cursor == null) {
                return null;
            }
            if (cursor.moveToLast()) {
                msg = serializeMsg(cursor);
            }
        }
        if (msg != null)
            msg.reactionList = MsgReactionDBManager.getInstance().queryWithMessageId(msg.messageID);
        return msg;
    }

    public YutakMsg queryMaxOrderSeqMsgWithChannel(String channelID, byte channelType) {
        String sql = "select * from " + message + " where " + DB.MessageColumns.channel_id + "=? and " + DB.MessageColumns.channel_type + "=? and " + DB.MessageColumns.is_deleted + "=0 and type<>0 and type<>99 order by " + DB.MessageColumns.order_seq + " desc limit 1";
        Cursor cursor = null;
        YutakMsg msg = null;
        try {
            cursor = YutakApplication
                    .get()
                    .getDbHelper().rawQuery(sql, new Object[]{channelID, channelType});
            if (cursor == null) {
                return null;
            }
            if (cursor.moveToLast()) {
                msg = serializeMsg(cursor);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return msg;
    }


    /**
     * 删除消息
     *
     * @param client_seq 消息客户端编号
     */
    public synchronized boolean deleteWithClientSeq(long client_seq) {
        String[] updateKey = new String[1];
        String[] updateValue = new String[1];
        updateKey[0] = DB.MessageColumns.is_deleted;
        updateValue[0] = "1";
        String where = DB.MessageColumns.client_seq + "=?";
        String[] whereValue = new String[1];
        whereValue[0] = String.valueOf(client_seq);
        int row = YutakApplication.get().getDbHelper()
                .update(message, updateKey, updateValue, where, whereValue);
        if (row > 0) {
            YutakMsg msg = queryWithClientSeq(client_seq);
            if (msg != null)
                YutakIM.get().getMsgManager().setDeleteMsg(msg);
        }
        return row > 0;
    }

    public int queryRowNoWithOrderSeq(String channelID, byte channelType, long orderSeq) {
        String sql = "select count(*) cn from " + message + " where channel_id=? and channel_type=? and " + DB.MessageColumns.type + "<>0 and " + DB.MessageColumns.type + "<>99 and " + DB.MessageColumns.order_seq + ">? and " + DB.MessageColumns.is_deleted + "=0 order by " + DB.MessageColumns.order_seq + " desc";
        Cursor cursor = null;
        int rowNo = 0;
        try {
            cursor = YutakApplication
                    .get()
                    .getDbHelper().rawQuery(sql, new Object[]{channelID, channelType, orderSeq});
            if (cursor == null) {
                return 0;
            }
            if (cursor.moveToLast()) {
                rowNo = YutakCursor.readInt(cursor, "cn");
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return rowNo;
    }

    public synchronized boolean deleteWithMessageID(String messageID) {
        String[] updateKey = new String[1];
        String[] updateValue = new String[1];
        updateKey[0] = DB.MessageColumns.is_deleted;
        updateValue[0] = "1";
        String where = DB.MessageColumns.message_id + "=?";
        String[] whereValue = new String[1];
        whereValue[0] = messageID;
        int row = YutakApplication.get().getDbHelper()
                .update(message, updateKey, updateValue, where, whereValue);
        if (row > 0) {
            YutakMsg msg = queryWithMessageID(messageID, false);
            if (msg != null)
                YutakIM.get().getMsgManager().setDeleteMsg(msg);
        }
        return row > 0;

    }

    public synchronized boolean deleteWithMessageIDs(List<String> messageIDs) {
        String[] updateKey = new String[1];
        String[] updateValue = new String[1];
        updateKey[0] = DB.MessageColumns.is_deleted;
        updateValue[0] = "1";
        String where = DB.MessageColumns.message_id + "in (" + YutakCursor.getPlaceholders(messageIDs.size()) + ")";
        String[] whereValue = messageIDs.toArray(new String[0]);
        int row = YutakApplication.get().getDbHelper()
                .update(message, updateKey, updateValue, where, whereValue);
//        if (row > 0) {
//            List<YutakMsg> msgList = queryWithMsgIds(messageIDs);
//            if (msgList.size() > 0) {
//                for (YutakMsg msg : msgList) {
//                    Yutak.get().getMsgManager().setDeleteMsg(msg);
//                }
//            }
//        }
        return row > 0;

    }

    private List<YutakMsgExtra> queryMsgExtrasWithMsgIds(List<String> msgIds) {
        List<YutakMsgExtra> list = new ArrayList<>();
        try (Cursor cursor = YutakApplication.get().getDbHelper().select(messageExtra, "message_id in (" + YutakCursor.getPlaceholders(msgIds.size()) + ")", msgIds.toArray(new String[0]), null)) {
            if (cursor == null) {
                return list;
            }
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                YutakMsgExtra extra = serializeMsgExtra(cursor);
                list.add(extra);
            }
        }
        return list;
    }

    public List<YutakMsg> insertOrUpdateMsgExtras(List<YutakMsgExtra> list) {
        List<String> msgIds = new ArrayList<>();
        for (int i = 0, size = list.size(); i < size; i++) {
            if (!TextUtils.isEmpty(list.get(i).messageID)) {
                msgIds.add(list.get(i).messageID);
            }
        }
        List<YutakMsgExtra> existList = queryMsgExtrasWithMsgIds(msgIds);
        List<ContentValues> insertCVList = new ArrayList<>();
        List<ContentValues> updateCVList = new ArrayList<>();
        for (int i = 0, size = list.size(); i < size; i++) {
            boolean isAdd = true;
            for (YutakMsgExtra extra : existList) {
                if (list.get(i).messageID.equals(extra.messageID)) {
                    updateCVList.add(YutakSqlContentValue.getWithCVWithMsgExtra(list.get(i)));
                    isAdd = false;
                    break;
                }
            }
            if (isAdd) {
                insertCVList.add(YutakSqlContentValue.getWithCVWithMsgExtra(list.get(i)));
            }
        }
        try {
            YutakApplication.get().getDbHelper().getDb()
                    .beginTransaction();
            if (!insertCVList.isEmpty()) {
                for (ContentValues cv : insertCVList) {
                    YutakApplication.get().getDbHelper().insert(messageExtra, cv);
                }
            }
            if (!updateCVList.isEmpty()) {
                for (ContentValues cv : updateCVList) {
                    String[] update = new String[1];
                    update[0] = cv.getAsString("message_id");
                    YutakApplication.get().getDbHelper()
                            .update(messageExtra, cv, "message_id=?", update);
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
        List<YutakMsg> msgList = queryWithMsgIds(msgIds);
        return msgList;
    }

    /**
     * 查询按日期分组的消息数量
     *
     * @param channelID   频道ID
     * @param channelType 频道类型
     * @return List<YutakMessageGroupByDate>
     */
    public List<YutakMessageGroupByDate> queryMessageGroupByDateWithChannel(String channelID, byte channelType) {
        String sql = "SELECT DATE(" + DB.MessageColumns.timestamp + ", 'unixepoch','localtime') AS days,COUNT(" + DB.MessageColumns.client_msg_no + ") count,min(" + DB.MessageColumns.order_seq + ") AS order_seq FROM " + message + "  WHERE " + DB.MessageColumns.channel_type + " =? and " + DB.MessageColumns.channel_id + "=? and is_deleted=0" + " GROUP BY " + DB.MessageColumns.timestamp + "," + DB.MessageColumns.order_seq + "";
        List<YutakMessageGroupByDate> list = new ArrayList<>();
        try (Cursor cursor = YutakApplication
                .get()
                .getDbHelper()
                .rawQuery(sql, new Object[]{channelType, channelID})) {
            if (cursor == null) {
                return list;
            }
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                YutakMessageGroupByDate msg = new YutakMessageGroupByDate();
                msg.count = YutakCursor.readLong(cursor, "count");
                msg.orderSeq = YutakCursor.readLong(cursor, "order_seq");
                msg.date = YutakCursor.readString(cursor, "days");
                list.add(msg);
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    /**
     * 清空所有聊天消息
     */
    public synchronized void clearEmpty() {
        YutakApplication.get().getDbHelper()
                .delete(message, null, null);
    }

    /**
     * 获取某个类型的聊天数据
     *
     * @param type            消息类型
     * @param oldestClientSeq 最后一次消息客户端ID
     * @param limit           数量
     */
    public List<YutakMsg> queryWithContentType(int type, long oldestClientSeq, int limit) {
        String sql;
        Object[] args;
        if (oldestClientSeq <= 0) {
            args = new Object[1];
            args[0] = type;
            sql = "select * from (select " + messageCols + "," + extraCols + " from " + message + " left join " + messageExtra + " on " + message + ".message_id=" + messageExtra + ".message_id where " + message + ".type=?) where is_deleted=0 and revoke=0 order by " + DB.MessageColumns.timestamp + " desc limit 0," + limit;
        } else {
            args = new Object[2];
            args[0] = type;
            args[1] = oldestClientSeq;
            sql = "select * from (select " + messageCols + "," + extraCols + " from " + message + " left join " + messageExtra + " on " + message + ".message_id=" + messageExtra + ".message_id where " + message + ".type=? and " + DB.MessageColumns.client_seq + "<?) where is_deleted=0 and revoke=0 order by " + DB.MessageColumns.timestamp + " desc limit 0," + limit;
        }
        List<YutakMsg> msgs = new ArrayList<>();
        try (Cursor cursor = YutakApplication.get().getDbHelper().rawQuery(sql, args)) {
            if (cursor == null) {
                return msgs;
            }
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                YutakMsg msg = serializeMsg(cursor);
                if (msg.channelType == YutakChannelType.GROUP) {
                    //查询群成员信息
                    YutakChannelMember member = ChannelMemberDBManager.getInstance().query(msg.channelID, YutakChannelType.GROUP, msg.fromUID);
                    msg.setMemberOfFrom(member);
                    YutakChannel channel = ChannelDBManager.getInstance().query(msg.fromUID, YutakChannelType.PERSONAL);
                    msg.setFrom(channel);
                } else {
                    YutakChannel channel = ChannelDBManager.getInstance().query(msg.fromUID, YutakChannelType.PERSONAL);
                    msg.setFrom(channel);
                }
                msgs.add(0, msg);
            }
        }
        return msgs;
    }

    public List<YutakMsg> searchWithChannel(String searchKey, String channelID, byte channelType) {
        List<YutakMsg> msgs = new ArrayList<>();
        String sql = "select * from (select " + messageCols + "," + extraCols + " from " + message + " left join " + messageExtra + " on " + message + ".message_id=" + messageExtra + ".message_id where " + message + ".searchable_word like ? and " + message + ".channel_id=? and " + message + ".channel_type=?) where is_deleted=0 and revoke=0";
        try (Cursor cursor = YutakApplication
                .get()
                .getDbHelper()
                .rawQuery(sql, new Object[]{"%" + searchKey + "%", channelID, channelType})) {
            if (cursor == null) {
                return msgs;
            }
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                YutakMsg msg = serializeMsg(cursor);
                if (msg.channelType == YutakChannelType.GROUP) {
                    //查询群成员信息
                    YutakChannelMember member = ChannelMemberDBManager.getInstance().query(msg.channelID, YutakChannelType.GROUP, msg.fromUID);
                    msg.setMemberOfFrom(member);
                    YutakChannel channel = ChannelDBManager.getInstance().query(msg.fromUID, YutakChannelType.PERSONAL);
                    msg.setFrom(channel);
                } else {
                    YutakChannel channel = ChannelDBManager.getInstance().query(msg.fromUID, YutakChannelType.PERSONAL);
                    msg.setFrom(channel);
                }
                msgs.add(0, msg);
            }
        } catch (Exception ignored) {
        }
        return msgs;
    }

    public List<YutakMessageSearchResult> search(String searchKey) {
        List<YutakMessageSearchResult> list = new ArrayList<>();

        String sql = "select distinct c.*, count(*) message_count, case count(*) WHEN 1 then" +
                " m.client_seq else ''END client_seq, CASE count(*) WHEN 1 THEN m.searchable_word else '' end searchable_word " +
                "from " + channel + " c LEFT JOIN " + message + " m ON m.channel_id = c.channel_id and " +
                "m.channel_type = c.channel_type WHERE m.is_deleted=0 and searchable_word LIKE ? GROUP BY " +
                "c.channel_id, c.channel_type ORDER BY m.created_at DESC limit 100";
        Cursor cursor = YutakApplication.get().getDbHelper().rawQuery(sql, new Object[]{"%" + searchKey + "%"});
        if (cursor == null) {
            return list;
        }
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            YutakChannel channel = ChannelDBManager.getInstance().serializableChannel(cursor);
            YutakMessageSearchResult result = new YutakMessageSearchResult();
            result.YutakChannel = channel;
            result.messageCount = YutakCursor.readInt(cursor, "message_count");
            result.searchableWord = YutakCursor.readString(cursor, DB.MessageColumns.searchable_word);
            list.add(result);
        }
        cursor.close();
        return list;
    }

    public synchronized boolean deleteWithChannel(String channelId, byte channelType) {

        String[] updateKey = new String[1];
        String[] updateValue = new String[1];

        updateKey[0] = DB.MessageColumns.is_deleted;
        updateValue[0] = "1";

        String where = DB.MessageColumns.channel_id + "=? and " + DB.MessageColumns.channel_type + "=?";
        String[] whereValue = new String[2];
        whereValue[0] = channelId;
        whereValue[1] = String.valueOf(channelType);

        int row = YutakApplication.get().getDbHelper()
                .update(message, updateKey, updateValue, where, whereValue);
        return row > 0;
    }

    public synchronized boolean deleteWithChannelAndFromUID(String channelId, byte channelType, String fromUID) {
        String[] updateKey = new String[1];
        String[] updateValue = new String[1];

        updateKey[0] = DB.MessageColumns.is_deleted;
        updateValue[0] = "1";

        String where = DB.MessageColumns.channel_id + "=? and " + DB.MessageColumns.channel_type + "=? and " + DB.MessageColumns.from_uid + "=?";
        String[] whereValue = new String[3];
        whereValue[0] = channelId;
        whereValue[1] = String.valueOf(channelType);
        whereValue[2] = String.valueOf(fromUID);

        int row = YutakApplication.get().getDbHelper()
                .update(message, updateKey, updateValue, where, whereValue);
        return row > 0;
    }

    /**
     * 查询固定类型的消息记录
     *
     * @param channelID      频道ID
     * @param channelType    频道类型
     * @param oldestOrderSeq 排序编号
     * @param limit          查询数量
     * @param contentTypes   内容类型
     * @return List<YutakMsg>
     */
    public List<YutakMsg> searchWithChannelAndContentTypes(String channelID, byte channelType, long oldestOrderSeq, int limit, int[] contentTypes) {
        if (TextUtils.isEmpty(channelID) || contentTypes == null || contentTypes.length == 0) {
            return null;
        }
        String whereStr = "";
        for (int contentType : contentTypes) {
            if (TextUtils.isEmpty(whereStr)) {
                whereStr = String.valueOf(contentType);
            } else {
                whereStr = "," + contentType;
            }
        }
        Object[] args;
        String sql;
        if (oldestOrderSeq <= 0) {
            args = new Object[]{channelID, channelType, whereStr};
            sql = "select * from (select " + messageCols + "," + extraCols + " from " + message + " left join " + messageExtra + " on " + message + ".message_id= " + messageExtra + ".message_id where " + message + ".channel_id=? and " + message + ".channel_type=? and " + message + ".type<>0 and " + message + ".type<>99 and " + message + ".type in (?)) where is_deleted=0 and revoke=0 order by order_seq desc limit 0," + limit;
        } else {
            args = new Object[]{channelID, channelType, oldestOrderSeq, whereStr};
            sql = "select * from (select " + messageCols + "," + extraCols + " from " + message + " left join " + messageExtra + " on " + message + ".message_id= " + messageExtra + ".message_id where " + message + ".channel_id=? and " + message + ".channel_type=? and " + message + ".order_seq<? and " + message + ".type<>0 and " + message + ".type<>99 and " + message + ".type in (?)) where is_deleted=0 and revoke=0 order by order_seq desc limit 0," + limit;
        }
        List<YutakMsg> YutakMsgs = new ArrayList<>();
        try (Cursor cursor = YutakApplication
                .get()
                .getDbHelper()
                .rawQuery(sql, args)) {
            if (cursor == null) {
                return YutakMsgs;
            }
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                YutakMsg msg = serializeMsg(cursor);
                if (msg.channelType == YutakChannelType.GROUP) {
                    //查询群成员信息
                    YutakChannelMember member = ChannelMemberDBManager.getInstance().query(msg.channelID, YutakChannelType.GROUP, msg.fromUID);
                    msg.setMemberOfFrom(member);
                    YutakChannel channel = ChannelDBManager.getInstance().query(msg.fromUID, YutakChannelType.PERSONAL);
                    msg.setFrom(channel);
                } else {
                    YutakChannel channel = ChannelDBManager.getInstance().query(msg.fromUID, YutakChannelType.PERSONAL);
                    msg.setFrom(channel);
                }
                YutakMsgs.add(msg);
            }
        } catch (Exception ignored) {
        }
        return YutakMsgs;
    }

    /**
     * 获取最大扩展编号消息
     *
     * @param channelID   频道ID
     * @param channelType 频道类型
     */
    public long queryMsgExtraMaxVersionWithChannel(String channelID, byte channelType) {
        String sql = "select * from " + messageExtra + " where channel_id =? and channel_type=? order by extra_version desc limit 1";
        Cursor cursor = null;
        long version = 0;
        try {
            cursor = YutakApplication
                    .get()
                    .getDbHelper().rawQuery(sql, new Object[]{channelID, channelType});
            if (cursor == null) {
                return 0;
            }
            if (cursor.moveToLast()) {
                version = YutakCursor.readLong(cursor, "extra_version");
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return version;
    }

    public synchronized boolean updateFieldWithClientMsgNo(String clientMsgNo, String field, String value, boolean isRefreshUI) {
        String[] updateKey = new String[]{field};
        String[] updateValue = new String[]{value};
        String where = DB.MessageColumns.client_msg_no + "=?";
        String[] whereValue = new String[1];
        whereValue[0] = clientMsgNo;
        int row = YutakApplication.get().getDbHelper()
                .update(message, updateKey, updateValue, where, whereValue);
        if (row > 0 && isRefreshUI) {
            YutakMsg msg = queryWithClientMsgNo(clientMsgNo);
            if (msg != null)
                YutakIM.get().getMsgManager().setRefreshMsg(msg, true);
        }
        return row > 0;
    }

    public synchronized boolean updateFieldWithMessageID(String messageID, String field, String value) {
        String[] updateKey = new String[]{field};
        String[] updateValue = new String[]{value};
        String where = DB.MessageColumns.message_id + "=?";
        String[] whereValue = new String[1];
        whereValue[0] = messageID;
        int row = YutakApplication.get().getDbHelper()
                .update(message, updateKey, updateValue, where, whereValue);
        if (row > 0) {
            YutakMsg msg = queryWithMessageID(messageID, true);
            if (msg != null)
                YutakIM.get().getMsgManager().setRefreshMsg(msg, true);
        }
        return row > 0;

    }


    public YutakMsg queryWithMessageID(String messageID, boolean isGetMsgReaction) {
        YutakMsg msg = null;
        String sql = "select " + messageCols + "," + extraCols + " from " + message + " LEFT JOIN " + messageExtra + " ON " + message + ".message_id=" + messageExtra + ".message_id WHERE " + message + ".message_id=? and " + message + ".is_deleted=0";

        try (Cursor cursor = YutakApplication
                .get()
                .getDbHelper().rawQuery(sql, new Object[]{messageID})) {
            if (cursor == null) {
                return null;
            }
            if (cursor.moveToLast()) {
                msg = serializeMsg(cursor);
            }
        }
        if (msg != null && isGetMsgReaction)
            msg.reactionList = MsgReactionDBManager.getInstance().queryWithMessageId(msg.messageID);
        return msg;
    }

    public int queryMaxMessageOrderSeqWithChannel(String channelID, byte channelType) {
        String sql = "SELECT max(order_seq) order_seq FROM " + message + " WHERE channel_id=? AND channel_type=?";
        int orderSeq = 0;
        try (Cursor cursor = YutakApplication
                .get()
                .getDbHelper().rawQuery(sql, new Object[]{channelID, channelType})) {
            if (cursor == null) {
                return 0;
            }
            if (cursor.moveToLast()) {
                orderSeq = YutakCursor.readInt(cursor, DB.MessageColumns.order_seq);
            }
        }
        return orderSeq;
    }

    public int queryMaxMessageSeqNotDeletedWithChannel(String channelID, byte channelType) {
        String sql = "SELECT max(message_seq) message_seq FROM " + message + " WHERE channel_id=? AND channel_type=? AND is_deleted=0";
        int messageSeq = 0;
        try (Cursor cursor = YutakApplication
                .get()
                .getDbHelper().rawQuery(sql, new Object[]{channelID, channelType})) {
            if (cursor == null) {
                return 0;
            }
            if (cursor.moveToLast()) {
                messageSeq = YutakCursor.readInt(cursor, DB.MessageColumns.message_seq);
            }
        }
        return messageSeq;
    }

    public int queryMaxMessageSeqWithChannel(String channelID, byte channelType) {
        String sql = "SELECT max(message_seq) message_seq FROM " + message + " WHERE channel_id=? AND channel_type=?";
        int messageSeq = 0;
        try (Cursor cursor = YutakApplication
                .get()
                .getDbHelper().rawQuery(sql, new Object[]{channelID, channelType})) {
            if (cursor == null) {
                return 0;
            }
            if (cursor.moveToLast()) {
                messageSeq = YutakCursor.readInt(cursor, DB.MessageColumns.message_seq);
            }
        }
        return messageSeq;
    }

    public int queryMinMessageSeqWithChannel(String channelID, byte channelType) {
        String sql = "SELECT min(message_seq) message_seq FROM " + message + " WHERE channel_id=? AND channel_type=?";
        int messageSeq = 0;
        try (Cursor cursor = YutakApplication
                .get()
                .getDbHelper().rawQuery(sql, new Object[]{channelID, channelType})) {
            if (cursor == null) {
                return 0;
            }
            if (cursor.moveToLast()) {
                messageSeq = YutakCursor.readInt(cursor, DB.MessageColumns.message_seq);
            }
        }
        return messageSeq;
    }

    private int queryMsgSeq(String channelID, byte channelType, long oldestOrderSeq, int pullMode) {
        String sql;
        int messageSeq = 0;
        if (pullMode == 1) {
            sql = "select * from " + message + " where channel_id=? and channel_type=? and  order_seq>? and message_seq<>0 order by message_seq desc limit 1";
        } else
            sql = "select * from " + message + " where channel_id=? and channel_type=? and  order_seq<? and message_seq<>0 order by message_seq asc limit 1";
        try (Cursor cursor = YutakApplication
                .get()
                .getDbHelper().rawQuery(sql, new Object[]{channelID, channelType, oldestOrderSeq})) {
            if (cursor == null) {
                return 0;
            }
            if (cursor.moveToLast()) {
                YutakMsg msg = serializeMsg(cursor);
                messageSeq = msg.messageSeq;
            }
        }
        return messageSeq;
    }

    public List<YutakMsg> queryWithMsgIds(List<String> messageIds) {

        String sql = "select " + messageCols + "," + extraCols + " from " + message + " left join " + messageExtra + " on " + message + ".message_id=" + messageExtra + ".message_id where " + message + ".message_id in (" + YutakCursor.getPlaceholders(messageIds.size()) + ")";
        List<YutakMsg> list = new ArrayList<>();
        List<String> gChannelIds = new ArrayList<>();
        List<String> pChannelIds = new ArrayList<>();
        List<String> fromChannelIds = new ArrayList<>();
        try (Cursor cursor = YutakApplication
                .get()
                .getDbHelper()
                .rawQuery(sql, messageIds.toArray())) {
            if (cursor == null) {
                return list;
            }

            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                YutakMsg msg = serializeMsg(cursor);
                boolean isAdd = true;
                if (msg.channelType == YutakChannelType.GROUP) {
                    //查询群成员信息
                    for (int i = 0; i < gChannelIds.size(); i++) {
                        if (gChannelIds.get(i).equals(msg.fromUID)) {
                            isAdd = false;
                            break;
                        }
                    }
                    if (isAdd) {
                        gChannelIds.add(msg.fromUID);
                    }
                } else {
                    for (int i = 0; i < pChannelIds.size(); i++) {
                        if (pChannelIds.get(i).equals(msg.channelID)) {
                            isAdd = false;
                            break;
                        }
                    }
                    if (isAdd) {
                        pChannelIds.add(msg.channelID);
                    }

                }
                isAdd = true;
                for (int i = 0; i < fromChannelIds.size(); i++) {
                    if (fromChannelIds.get(i).equals(msg.fromUID)) {
                        isAdd = false;
                        break;
                    }
                }
                if (isAdd) {
                    fromChannelIds.add(msg.fromUID);
                }

                list.add(msg);
            }

        } catch (Exception ignored) {
        }

        if (gChannelIds.size() > 0) {
            List<YutakChannel> channels = ChannelDBManager.getInstance().queryWithChannelIdsAndChannelType(gChannelIds, YutakChannelType.GROUP);
            if (channels != null && channels.size() > 0) {
                for (YutakChannel channel : channels) {
                    if (channel == null || TextUtils.isEmpty(channel.channelID)) continue;
                    for (int i = 0, size = list.size(); i < size; i++) {
                        if (list.get(i).channelType == YutakChannelType.GROUP && channel.channelID.equals(list.get(i).channelID)) {
                            list.get(i).setChannelInfo(channel);
                        }
                    }
                }
            }
        }
        if (pChannelIds.size() > 0) {
            List<YutakChannel> channels = ChannelDBManager.getInstance().queryWithChannelIdsAndChannelType(pChannelIds, YutakChannelType.PERSONAL);
            if (channels != null && channels.size() > 0) {
                for (YutakChannel channel : channels) {
                    if (channel == null || TextUtils.isEmpty(channel.channelID)) continue;
                    for (int i = 0, size = list.size(); i < size; i++) {
                        if (list.get(i).channelType == YutakChannelType.PERSONAL && channel.channelID.equals(list.get(i).channelID)) {
                            list.get(i).setChannelInfo(channel);
                        }
                    }
                }
            }
        }

        if (fromChannelIds.size() > 0) {
            List<YutakChannel> channels = ChannelDBManager.getInstance().queryWithChannelIdsAndChannelType(fromChannelIds, YutakChannelType.PERSONAL);
            if (channels != null && channels.size() > 0) {
                for (YutakChannel channel : channels) {
                    if (channel == null || TextUtils.isEmpty(channel.channelID)) continue;
                    for (int i = 0, size = list.size(); i < size; i++) {
                        if (!TextUtils.isEmpty(list.get(i).fromUID) && list.get(i).channelType == YutakChannelType.PERSONAL && channel.channelID.equals(list.get(i).fromUID)) {
                            list.get(i).setFrom(channel);
                        }
                    }
                }
            }
        }
        return list;
    }

    /**
     * 删除消息
     *
     * @param clientMsgNO 消息ID
     */
    public synchronized YutakMsg deleteWithClientMsgNo(String clientMsgNO) {
        String[] updateKey = new String[1];
        String[] updateValue = new String[1];
        updateKey[0] = DB.MessageColumns.is_deleted;
        updateValue[0] = "1";
        String where = DB.MessageColumns.client_msg_no + "=?";
        String[] whereValue = new String[1];
        whereValue[0] = clientMsgNO;
        YutakMsg msg = null;
        int row = YutakApplication.get().getDbHelper()
                .update(message, updateKey, updateValue, where, whereValue);
        if (row > 0) {
            msg = queryWithClientMsgNo(clientMsgNO);
        }
        return msg;
    }

    public long getMaxReactionSeqWithChannel(String channelID, byte channelType) {
        return MsgReactionDBManager.getInstance().queryMaxSeqWithChannel(channelID, channelType);
    }

    public void insertMsgReactions(List<YutakMsgReaction> list) {
        MsgReactionDBManager.getInstance().insertReactions(list);
    }

    public List<YutakMsgReaction> queryMsgReactionWithMsgIds(List<String> messageIds) {
        return MsgReactionDBManager.getInstance().queryWithMessageIds(messageIds);
    }

    public synchronized void updateAllMsgSendFail() {
        String[] updateKey = new String[1];
        updateKey[0] = DB.MessageColumns.status;
        String[] updateValue = new String[1];
        updateValue[0] = Yutak.SendMsgResult.send_fail + "";
        String where = DB.MessageColumns.status + "=? ";
        String[] whereValue = new String[1];
        whereValue[0] = "0";
        try {
            if (YutakApplication.get().getDbHelper() != null) {
                YutakApplication
                        .get()
                        .getDbHelper()
                        .update(message, updateKey, updateValue, where,
                                whereValue);
            }
        } catch (Exception ignored) {
        }
    }

    public synchronized void updateMsgStatus(long client_seq, int status) {
        String[] updateKey = new String[1];
        String[] updateValue = new String[1];
        updateKey[0] = DB.MessageColumns.status;
        updateValue[0] = String.valueOf(status);

        String where = DB.MessageColumns.client_seq + "=?";
        String[] whereValue = new String[1];
        whereValue[0] = String.valueOf(client_seq);

        int row = YutakApplication.get().getDbHelper()
                .update(message, updateKey, updateValue, where, whereValue);
        if (row > 0) {
            YutakMsg msg = queryWithClientSeq(client_seq);
            if (msg != null) {
                msg.status = status;
                YutakIM.get().getMsgManager().setRefreshMsg(msg, true);
            }
        }
    }

    public int queryMaxMessageSeqWithChannel() {
        int maxMessageSeq = 0;
        String sql = "select max(message_seq) message_seq from " + message;
        try {
            if (YutakApplication.get().getDbHelper() != null) {
                Cursor cursor = YutakApplication
                        .get()
                        .getDbHelper()
                        .rawQuery(sql);
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        maxMessageSeq = YutakCursor.readInt(cursor, DB.MessageColumns.message_seq);
                    }
                    cursor.close();
                }
            }
        } catch (Exception ignored) {
        }
        return maxMessageSeq;
    }

    public List<YutakMsgExtra> queryMsgExtraWithNeedUpload(int needUpload) {
        List<YutakMsgExtra> list = new ArrayList<>();
        String sql = "select * from " + messageExtra + " where needUpload=?";
        try (Cursor cursor = YutakApplication.get().getDbHelper().rawQuery(sql, new Object[]{needUpload})) {
            if (cursor == null) {
                return list;
            }
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                YutakMsgExtra extra = serializeMsgExtra(cursor);
                list.add(extra);
            }
        }
        return list;
    }

    public YutakMsgExtra queryMsgExtraWithMsgID(String msgID) {
        YutakMsgExtra extra = null;
        try {
            if (YutakApplication.get().getDbHelper() != null) {
                Cursor cursor = YutakApplication
                        .get()
                        .getDbHelper()
                        .select(messageExtra, "message_id=?", new String[]{msgID}, null);
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        extra = serializeMsgExtra(cursor);
                    }
                    cursor.close();
                }
            }
        } catch (Exception ignored) {
        }
        return extra;
    }

    private YutakMsgExtra serializeMsgExtra(Cursor cursor) {
        YutakMsgExtra extra = new YutakMsgExtra();
        extra.messageID = YutakCursor.readString(cursor, "message_id");
        extra.channelID = YutakCursor.readString(cursor, "channel_id");
        extra.channelType = YutakCursor.readByte(cursor, "channel_type");
        extra.readed = YutakCursor.readInt(cursor, "readed");
        extra.readedCount = YutakCursor.readInt(cursor, "readed_count");
        extra.unreadCount = YutakCursor.readInt(cursor, "unread_count");
        extra.revoke = YutakCursor.readInt(cursor, "revoke");
        extra.isMutualDeleted = YutakCursor.readInt(cursor, "is_mutual_deleted");
        extra.revoker = YutakCursor.readString(cursor, "revoker");
        extra.extraVersion = YutakCursor.readLong(cursor, "extra_version");
        extra.editedAt = YutakCursor.readLong(cursor, "edited_at");
        extra.contentEdit = YutakCursor.readString(cursor, "content_edit");
        extra.needUpload = YutakCursor.readInt(cursor, "needUpload");
        return extra;
    }

    private YutakMsg serializeMsg(Cursor cursor) {
        YutakMsg msg = new YutakMsg();
        msg.messageID = YutakCursor.readString(cursor, DB.MessageColumns.message_id);
        msg.messageSeq = YutakCursor.readInt(cursor, DB.MessageColumns.message_seq);
        msg.clientSeq = YutakCursor.readInt(cursor, DB.MessageColumns.client_seq);
        msg.timestamp = YutakCursor.readLong(cursor, DB.MessageColumns.timestamp);
        msg.fromUID = YutakCursor.readString(cursor, DB.MessageColumns.from_uid);
        msg.channelID = YutakCursor.readString(cursor, DB.MessageColumns.channel_id);
        msg.channelType = YutakCursor.readByte(cursor, DB.MessageColumns.channel_type);
        msg.type = YutakCursor.readInt(cursor, DB.MessageColumns.type);
        msg.content = YutakCursor.readString(cursor, DB.MessageColumns.content);
        msg.status = YutakCursor.readInt(cursor, DB.MessageColumns.status);
        msg.voiceStatus = YutakCursor.readInt(cursor, DB.MessageColumns.voice_status);
        msg.createdAt = YutakCursor.readString(cursor, DB.MessageColumns.created_at);
        msg.updatedAt = YutakCursor.readString(cursor, DB.MessageColumns.updated_at);
        msg.searchableWord = YutakCursor.readString(cursor, DB.MessageColumns.searchable_word);
        msg.clientMsgNO = YutakCursor.readString(cursor, DB.MessageColumns.client_msg_no);
        msg.isDeleted = YutakCursor.readInt(cursor, DB.MessageColumns.is_deleted);
        msg.orderSeq = YutakCursor.readLong(cursor, DB.MessageColumns.order_seq);
        byte setting = YutakCursor.readByte(cursor, DB.MessageColumns.setting);
        msg.setting = TypeKit.getInstance().getMsgSetting(setting);
        msg.flame = YutakCursor.readInt(cursor, DB.MessageColumns.flame);
        msg.flameSecond = YutakCursor.readInt(cursor, DB.MessageColumns.flame_second);
        msg.viewed = YutakCursor.readInt(cursor, DB.MessageColumns.viewed);
        msg.viewedAt = YutakCursor.readLong(cursor, DB.MessageColumns.viewed_at);
        msg.topicID = YutakCursor.readString(cursor, DB.MessageColumns.topic_id);
        msg.expireTime = YutakCursor.readInt(cursor, DB.MessageColumns.expire_time);
        msg.expireTimestamp = YutakCursor.readInt(cursor, DB.MessageColumns.expire_timestamp);
        // 扩展表数据
        msg.remoteExtra = serializeMsgExtra(cursor);

        String extra = YutakCursor.readString(cursor, DB.MessageColumns.extra);
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
        //获取附件
        msg.baseContentMsgModel = getMsgModel(msg);
        if (!TextUtils.isEmpty(msg.remoteExtra.contentEdit)) {
            msg.remoteExtra.contentEditMsgModel = MsgManager.getInstance().getMsgContentModel(msg.remoteExtra.contentEdit);
        }
        return msg;
    }

    private YutakMessageContent getMsgModel(YutakMsg msg) {
        JSONObject jsonObject = null;
        if (!TextUtils.isEmpty(msg.content)) {
            try {
                jsonObject = new JSONObject(msg.content);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return YutakIM.get()
                .getMsgManager().getMsgContentModel(msg.type, jsonObject);
    }

}
