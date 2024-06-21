package com.yutak.im.manager;


import android.text.TextUtils;

import com.yutak.im.YutakApplication;
import com.yutak.im.YutakIM;
import com.yutak.im.cs.DB;
import com.yutak.im.cs.Yutak;
import com.yutak.im.db.ConversationDBManager;
import com.yutak.im.db.MsgDBManager;
import com.yutak.im.domain.YutakChannel;
import com.yutak.im.domain.YutakChannelType;
import com.yutak.im.domain.YutakConversationMsg;
import com.yutak.im.domain.YutakMentionInfo;
import com.yutak.im.domain.YutakMessageGroupByDate;
import com.yutak.im.domain.YutakMessageSearchResult;
import com.yutak.im.domain.YutakMsg;
import com.yutak.im.domain.YutakMsgExtra;
import com.yutak.im.domain.YutakMsgReaction;
import com.yutak.im.domain.YutakMsgSetting;
import com.yutak.im.domain.YutakSyncExtraMsg;
import com.yutak.im.domain.YutakSyncMsg;
import com.yutak.im.domain.YutakSyncMsgReaction;
import com.yutak.im.domain.YutakSyncRecent;
import com.yutak.im.domain.YutakUIConversationMsg;
import com.yutak.im.interfaces.IClearMsgListener;
import com.yutak.im.interfaces.IDeleteMsgListener;
import com.yutak.im.interfaces.IGetOrSyncHistoryMsgBack;
import com.yutak.im.interfaces.IMessageStoreBeforeIntercept;
import com.yutak.im.interfaces.INewMsgListener;
import com.yutak.im.interfaces.IRefreshMsg;
import com.yutak.im.interfaces.ISendACK;
import com.yutak.im.interfaces.ISendMsgCallBackListener;
import com.yutak.im.interfaces.ISyncChannelMsgBack;
import com.yutak.im.interfaces.ISyncChannelMsgListener;
import com.yutak.im.interfaces.ISyncMsgReaction;
import com.yutak.im.interfaces.ISyncOfflineMsgBack;
import com.yutak.im.interfaces.ISyncOfflineMsgListener;
import com.yutak.im.interfaces.IUploadAttacResultListener;
import com.yutak.im.interfaces.IUploadAttachmentListener;
import com.yutak.im.interfaces.IUploadMsgExtraListener;
import com.yutak.im.kit.DateKit;
import com.yutak.im.kit.TypeKit;
import com.yutak.im.message.MessageHandler;
import com.yutak.im.message.YutakConnection;
import com.yutak.im.msgmodel.YutakImageContent;
import com.yutak.im.msgmodel.YutakMessageContent;
import com.yutak.im.msgmodel.YutakMsgEntity;
import com.yutak.im.msgmodel.YutakReply;
import com.yutak.im.msgmodel.YutakTextContent;
import com.yutak.im.msgmodel.YutakVideoContent;
import com.yutak.im.msgmodel.YutakVoiceContent;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 5/20/21 5:38 PM
 * 消息管理
 */
public class MsgManager extends BaseManager {
    private MsgManager() {
    }

    private static class MsgManagerBinder {
        static final MsgManager msgManager = new MsgManager();
    }

    public static MsgManager getInstance() {
        return MsgManagerBinder.msgManager;
    }

    private final long YutakOrderSeqFactor = 1000L;
    // 消息修改
    private ConcurrentHashMap<String, IRefreshMsg> refreshMsgListenerMap;
    // 监听发送消息回调
    private ConcurrentHashMap<String, ISendMsgCallBackListener> sendMsgCallBackListenerHashMap;
    // 删除消息监听
    private ConcurrentHashMap<String, IDeleteMsgListener> deleteMsgListenerMap;
    // 发送消息ack监听
    private ConcurrentHashMap<String, ISendACK> sendAckListenerMap;
    // 新消息监听
    private ConcurrentHashMap<String, INewMsgListener> newMsgListenerMap;
    // 清空消息
    private ConcurrentHashMap<String, IClearMsgListener> clearMsgMap;
    // 同步消息回应
    private ISyncMsgReaction iSyncMsgReaction;
    // 上传文件附件
    private IUploadAttachmentListener iUploadAttachmentListener;
    // 同步离线消息
    private ISyncOfflineMsgListener iOfflineMsgListener;
    // 同步channel内消息
    private ISyncChannelMsgListener iSyncChannelMsgListener;

    // 消息存库拦截器
    private IMessageStoreBeforeIntercept messageStoreBeforeIntercept;
    // 自定义消息model
    private List<Class<? extends YutakMessageContent>> customContentMsgList;
    // 上传消息扩展
    private IUploadMsgExtraListener iUploadMsgExtraListener;
    private Timer checkMsgNeedUploadTimer;

    // 初始化默认消息model
    public void initNormalMsg() {
        if (customContentMsgList == null) {
            customContentMsgList = new ArrayList<>();
            customContentMsgList.add(YutakTextContent.class);
            customContentMsgList.add(YutakImageContent.class);
            customContentMsgList.add(YutakVideoContent.class);
            customContentMsgList.add(YutakVoiceContent.class);
        }
    }

    /**
     * 注册消息module
     *
     * @param contentMsg 消息
     */
    public void registerContentMsg(java.lang.Class<? extends YutakMessageContent> contentMsg) {
        if (customContentMsgList == null || customContentMsgList.size() == 0)
            initNormalMsg();
        try {
            boolean isAdd = true;
            for (int i = 0, size = customContentMsgList.size(); i < size; i++) {
                if (customContentMsgList.get(i).getDeclaredConstructor().newInstance().type == contentMsg.newInstance().type) {
                    isAdd = false;
                    break;
                }
            }
            if (isAdd)
                customContentMsgList.add(contentMsg);
        } catch (IllegalAccessException | InstantiationException | NoSuchMethodException |
                 InvocationTargetException e) {
            e.printStackTrace();
        }

    }

    // 通过json获取消息model
    public YutakMessageContent getMsgContentModel(JSONObject jsonObject) {
        int type = jsonObject.optInt("type");
        YutakMessageContent messageContent = getMsgContentModel(type, jsonObject);
        return messageContent;
    }

    public YutakMessageContent getMsgContentModel(String jsonStr) {
        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(jsonStr);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (jsonObject == null) {
            return new YutakMessageContent();
        } else
            return getMsgContentModel(jsonObject);
    }

    public YutakMessageContent getMsgContentModel(int contentType, JSONObject jsonObject) {
        if (jsonObject == null) jsonObject = new JSONObject();
        YutakMessageContent baseContentMsgModel = getContentMsgModel(contentType, jsonObject);
        if (baseContentMsgModel != null) {
            //解析@成员列表
            if (jsonObject.has("mention")) {
                JSONObject tempJson = jsonObject.optJSONObject("mention");
                if (tempJson != null) {
                    //是否@所有人
                    if (tempJson.has("all"))
                        baseContentMsgModel.mentionAll = tempJson.optInt("all");
                    JSONArray uidList = tempJson.optJSONArray("uids");

                    if (uidList != null && uidList.length() > 0) {
                        YutakMentionInfo mentionInfo = new YutakMentionInfo();
                        List<String> mentionInfoUIDs = new ArrayList<>();
                        for (int i = 0, size = uidList.length(); i < size; i++) {
                            String uid = uidList.optString(i);
                            if (uid.equals(YutakApplication.get().getUid())) {
                                mentionInfo.isMentionMe = true;
                            }
                            mentionInfoUIDs.add(uid);
                        }
                        mentionInfo.uids = mentionInfoUIDs;
                        if (baseContentMsgModel.mentionAll == 1) {
                            mentionInfo.isMentionMe = true;
                        }
                        baseContentMsgModel.mentionInfo = mentionInfo;
                    }
                }
            }

            if (jsonObject.has("from_uid"))
                baseContentMsgModel.fromUID = jsonObject.optString("from_uid");
            if (jsonObject.has("flame"))
                baseContentMsgModel.flame = jsonObject.optInt("flame");
            if (jsonObject.has("flame_second"))
                baseContentMsgModel.flameSecond = jsonObject.optInt("flame_second");
            //判断消息中是否包含回复情况
            if (jsonObject.has("reply")) {
                baseContentMsgModel.reply = new YutakReply();
                JSONObject replyJson = jsonObject.optJSONObject("reply");
                if (replyJson != null) {
                    baseContentMsgModel.reply = baseContentMsgModel.reply.decodeMsg(replyJson);
                }
            }
            if (jsonObject.has("robot_id"))
                baseContentMsgModel.robotID = jsonObject.optString("robot_id");
            if (jsonObject.has("entities")) {
                JSONArray jsonArray = jsonObject.optJSONArray("entities");
                if (jsonArray != null && jsonArray.length() > 0) {
                    List<YutakMsgEntity> list = new ArrayList<>();
                    for (int i = 0; i < jsonArray.length(); i++) {
                        YutakMsgEntity entity = new YutakMsgEntity();
                        JSONObject jo = jsonArray.optJSONObject(i);
                        entity.type = jo.optString("type");
                        entity.offset = jo.optInt("offset");
                        entity.length = jo.optInt("length");
                        entity.value = jo.optString("value");
                        list.add(entity);
                    }
                    baseContentMsgModel.entities = list;
                }

            }

        }
        return baseContentMsgModel;
    }

    /**
     * 将json消息转成对于的消息model
     *
     * @param type       content type
     * @param jsonObject content json
     * @return model
     */
    private YutakMessageContent getContentMsgModel(int type, JSONObject jsonObject) {
        java.lang.Class<? extends YutakMessageContent> baseMsg = null;
        if (customContentMsgList != null && customContentMsgList.size() > 0) {
            try {
                for (int i = 0, size = customContentMsgList.size(); i < size; i++) {
                    if (customContentMsgList.get(i).getDeclaredConstructor().newInstance().type == type) {
                        baseMsg = customContentMsgList.get(i);
                        break;
                    }
                }
            } catch (IllegalAccessException | InstantiationException | NoSuchMethodException |
                     InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        try {
            // 注册的消息model必须提供无参的构造方法
            if (baseMsg != null) {
                return baseMsg.newInstance().decodeMsg(jsonObject);
            }
        } catch (IllegalAccessException | InstantiationException e) {
            e.printStackTrace();
        }
        return null;
    }

    private long getOrNearbyMsgSeq(long orderSeq) {
        if (orderSeq % YutakOrderSeqFactor == 0) {
            return orderSeq / YutakOrderSeqFactor;
        }
        return (orderSeq - orderSeq % YutakOrderSeqFactor) / YutakOrderSeqFactor;
    }

    /**
     * 查询或同步某个频道消息
     *
     * @param channelId                频道ID
     * @param channelType              频道类型
     * @param oldestOrderSeq           最后一次消息大orderSeq 第一次进入聊天传入0
     * @param contain                  是否包含 oldestOrderSeq 这条消息
     * @param pullMode                 拉取模式 0:向下拉取 1:向上拉取
     * @param aroundMsgOrderSeq        查询此消息附近消息
     * @param limit                    每次获取数量
     * @param iGetOrSyncHistoryMsgBack 请求返还
     */
    public void getOrSyncHistoryMessages(String channelId, byte channelType, long oldestOrderSeq, boolean contain, int pullMode, int limit, long aroundMsgOrderSeq, final IGetOrSyncHistoryMsgBack iGetOrSyncHistoryMsgBack) {
        new Thread(() -> {
            int tempPullMode = pullMode;
            long tempOldestOrderSeq = oldestOrderSeq;
            boolean tempContain = contain;
            if (aroundMsgOrderSeq != 0) {
//                    long maxMsgSeq = getMaxMessageSeqWithChannel(channelId, channelType);
                long maxMsgSeq =
                        MsgDBManager.getInstance().queryMaxMessageSeqNotDeletedWithChannel(channelId, channelType);
                long aroundMsgSeq = getOrNearbyMsgSeq(aroundMsgOrderSeq);

                if (maxMsgSeq >= aroundMsgSeq && maxMsgSeq - aroundMsgSeq <= limit) {
                    // 显示最后一页数据
//                oldestOrderSeq = 0;
                    tempOldestOrderSeq = getMaxOrderSeqWithChannel(channelId, channelType);
//                    tempOldestOrderSeq = getMessageOrderSeq(maxMsgSeq, channelId, channelType);
                    if (tempOldestOrderSeq < aroundMsgOrderSeq) {
                        tempOldestOrderSeq = aroundMsgOrderSeq;
                    }
                    tempContain = true;
                    tempPullMode = 0;
                } else {
                    long minOrderSeq = MsgDBManager.getInstance().queryOrderSeq(channelId, channelType, aroundMsgOrderSeq, 3);
                    if (minOrderSeq == 0) {
                        tempOldestOrderSeq = aroundMsgOrderSeq;
                    } else {
                        if (minOrderSeq + limit < aroundMsgOrderSeq) {
                            if (aroundMsgOrderSeq % YutakOrderSeqFactor == 0) {
                                tempOldestOrderSeq = (aroundMsgOrderSeq / YutakOrderSeqFactor - 3) * YutakOrderSeqFactor;
                            } else
                                tempOldestOrderSeq = aroundMsgOrderSeq - 3;
//                        oldestOrderSeq = aroundMsgOrderSeq;
                        } else {
                            // todo 这里只会查询3条数据  oldestOrderSeq = minOrderSeq
                            long startOrderSeq = MsgDBManager.getInstance().queryOrderSeq(channelId, channelType, aroundMsgOrderSeq, limit);
                            if (startOrderSeq == 0) {
                                tempOldestOrderSeq = aroundMsgOrderSeq;
                            } else
                                tempOldestOrderSeq = startOrderSeq;
                        }
                    }
                    tempPullMode = 1;
                    tempContain = true;
                }
            }
            MsgDBManager.getInstance().queryOrSyncHistoryMessages(channelId, channelType, tempOldestOrderSeq, tempContain, tempPullMode, limit, iGetOrSyncHistoryMsgBack);
        }).start();
    }

    public List<YutakMsg> getAll() {
        return MsgDBManager.getInstance().queryAll();
    }

    public List<YutakMsg> getWithFromUID(String channelID, byte channelType, String fromUID, long oldestOrderSeq, int limit) {
        return MsgDBManager.getInstance().queryWithFromUID(channelID, channelType, fromUID, oldestOrderSeq, limit);
    }

    /**
     * 批量删除消息
     *
     * @param clientMsgNos 消息编号集合
     */
    public void deleteWithClientMsgNos(List<String> clientMsgNos) {
        if (clientMsgNos == null || clientMsgNos.size() == 0) return;
        List<YutakMsg> list = new ArrayList<>();
        try {
            YutakApplication.get().getDbHelper().getDb()
                    .beginTransaction();
            if (clientMsgNos.size() > 0) {
                for (int i = 0, size = clientMsgNos.size(); i < size; i++) {
                    YutakMsg msg = MsgDBManager.getInstance().deleteWithClientMsgNo(clientMsgNos.get(i));
                    if (msg != null) {
                        list.add(msg);
                    }
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
        List<YutakMsg> deleteMsgList = new ArrayList<>();
        for (int i = 0, size = list.size(); i < size; i++) {
            setDeleteMsg(list.get(i));
            boolean isAdd = true;
            for (int j = 0, len = deleteMsgList.size(); j < len; j++) {
                if (deleteMsgList.get(j).channelID.equals(list.get(i).channelID)
                        && deleteMsgList.get(j).channelType == list.get(i).channelType) {
                    isAdd = false;
                    break;
                }
            }
            if (isAdd) deleteMsgList.add(list.get(i));
        }
        for (int i = 0, size = deleteMsgList.size(); i < size; i++) {
            YutakMsg msg = MsgDBManager.getInstance().queryMaxOrderSeqMsgWithChannel(deleteMsgList.get(i).channelID, deleteMsgList.get(i).channelType);
            if (msg != null) {
                YutakUIConversationMsg uiMsg = YutakIM.get().getConversationManager().updateWithYutakMsg(msg);
                if (uiMsg != null) {
                    YutakIM.get().getConversationManager().setOnRefreshMsg(uiMsg, i == deleteMsgList.size()
                            - 1, "deleteWithClientMsgNOList");
                }
            }
        }
    }

    public List<YutakMsg> getExpireMessages(int limit) {
        long time = DateKit.get().nowSeconds();
        return MsgDBManager.getInstance().queryExpireMessages(time, limit);
    }

    /**
     * 删除某条消息
     *
     * @param client_seq 客户端序列号
     */
    public boolean deleteWithClientSeq(long client_seq) {
        return MsgDBManager.getInstance().deleteWithClientSeq(client_seq);
    }

    /**
     * 查询某条消息所在行
     *
     * @param channelID   频道ID
     * @param channelType 频道类型
     * @param clientMsgNo 客户端消息ID
     * @return int
     */
    public int getRowNoWithOrderSeq(String channelID, byte channelType, String clientMsgNo) {
        YutakMsg msg = MsgDBManager.getInstance().queryWithClientMsgNo(clientMsgNo);
        return MsgDBManager.getInstance().queryRowNoWithOrderSeq(channelID, channelType, msg == null ? 0 : msg.orderSeq);
    }

    public int getRowNoWithMessageID(String channelID, byte channelType, String messageID) {
        YutakMsg msg = MsgDBManager.getInstance().queryWithMessageID(messageID, false);
        return MsgDBManager.getInstance().queryRowNoWithOrderSeq(channelID, channelType, msg == null ? 0 : msg.orderSeq);
    }

    public void deleteWithClientMsgNO(String clientMsgNo) {
        YutakMsg msg = MsgDBManager.getInstance().deleteWithClientMsgNo(clientMsgNo);
        if (msg != null) {
            setDeleteMsg(msg);
            YutakConversationMsg conversationMsg = YutakIM.get().getConversationManager().getWithChannel(msg.channelID, msg.channelType);
            if (conversationMsg != null && conversationMsg.lastClientMsgNO.equals(clientMsgNo)) {
                YutakMsg tempMsg = MsgDBManager.getInstance().queryMaxOrderSeqMsgWithChannel(msg.channelID, msg.channelType);
                if (tempMsg != null) {
                    YutakUIConversationMsg uiMsg = ConversationDBManager.getInstance().insertOrUpdateWithMsg(tempMsg, 0);
                    YutakIM.get().getConversationManager().setOnRefreshMsg(uiMsg, true, "deleteWithClientMsgNO");
                }
            }
        }
    }


    public boolean deleteWithMessageID(String messageID) {
        return MsgDBManager.getInstance().deleteWithMessageID(messageID);
    }

    public YutakMsg getWithMessageID(String messageID) {
        return MsgDBManager.getInstance().queryWithMessageID(messageID, true);
    }

    public int isDeletedMsg(JSONObject jsonObject) {
        int isDelete = 0;
        //消息可见数组
        if (jsonObject != null && jsonObject.has("visibles")) {
            boolean isIncludeLoginUser = false;
            JSONArray jsonArray = jsonObject.optJSONArray("visibles");
            if (jsonArray != null && jsonArray.length() > 0) {
                for (int i = 0, size = jsonArray.length(); i < size; i++) {
                    if (jsonArray.optString(i).equals(YutakApplication.get().getUid())) {
                        isIncludeLoginUser = true;
                        break;
                    }
                }
            }
            isDelete = isIncludeLoginUser ? 0 : 1;
        }
        return isDelete;
    }

    public List<YutakMsg> getWithFlame() {
        return MsgDBManager.getInstance().queryWithFlame();
    }

    public long getMessageOrderSeq(long messageSeq, String channelID, byte channelType) {
        if (messageSeq == 0) {
            long tempOrderSeq = MsgDBManager.getInstance().queryMaxOrderSeqWithChannel(channelID, channelType);
            return tempOrderSeq + 1;
        }
        return messageSeq * YutakOrderSeqFactor;
    }

    public long getMessageSeq(long messageOrderSeq) {
        if (messageOrderSeq % YutakOrderSeqFactor == 0) {
            return messageOrderSeq / YutakOrderSeqFactor;
        }
        return 0;
    }

    public long getReliableMessageSeq(long messageOrderSeq) {
        return messageOrderSeq / YutakOrderSeqFactor;
    }

    /**
     *  use getMaxReactionSeqWithChannel
     * @param channelID channelId
     * @param channelType channelType
     * @return channel reaction max seq version
     */
    @Deprecated
    public long getMaxSeqWithChannel(String channelID, byte channelType) {
        return MsgDBManager.getInstance().getMaxReactionSeqWithChannel(channelID, channelType);
    }

    public long getMaxReactionSeqWithChannel(String channelID, byte channelType) {
        return MsgDBManager.getInstance().getMaxReactionSeqWithChannel(channelID, channelType);
    }


    // 设置消息回应
    public void setSyncMsgReaction(String channelID, byte channelType) {
        long maxSeq = MsgDBManager.getInstance().getMaxReactionSeqWithChannel(channelID, channelType);
        if (iSyncMsgReaction != null) {
            runOnMainThread(() -> iSyncMsgReaction.onSyncMsgReaction(channelID, channelType, maxSeq));
        }
    }

    public void saveMessageReactions(List<YutakSyncMsgReaction> list) {
        if (list == null || list.size() == 0) return;
        List<YutakMsgReaction> reactionList = new ArrayList<>();
        List<String> msgIds = new ArrayList<>();
        for (int i = 0, size = list.size(); i < size; i++) {
            YutakMsgReaction reaction = new YutakMsgReaction();
            reaction.messageID = list.get(i).message_id;
            reaction.channelID = list.get(i).channel_id;
            reaction.channelType = list.get(i).channel_type;
            reaction.uid = list.get(i).uid;
            reaction.name = list.get(i).name;
            reaction.seq = list.get(i).seq;
            reaction.emoji = list.get(i).emoji;
            reaction.isDeleted = list.get(i).is_deleted;
            reaction.createdAt = list.get(i).created_at;
            msgIds.add(list.get(i).message_id);
            reactionList.add(reaction);
        }
        saveMsgReactions(reactionList);
        List<YutakMsg> msgList = MsgDBManager.getInstance().queryWithMsgIds(msgIds);
        getMsgReactionsAndRefreshMsg(msgIds, msgList);
    }

    public int getMaxMessageSeq() {
        return MsgDBManager.getInstance().queryMaxMessageSeqWithChannel();
    }

    public int getMaxMessageSeqWithChannel(String channelID, byte channelType) {
        return MsgDBManager.getInstance().queryMaxMessageSeqWithChannel(channelID, channelType);
    }

    public int getMaxOrderSeqWithChannel(String channelID, byte channelType) {
        return MsgDBManager.getInstance().queryMaxMessageOrderSeqWithChannel(channelID, channelType);
    }

    public int getMinMessageSeqWithChannel(String channelID, byte channelType) {
        return MsgDBManager.getInstance().queryMinMessageSeqWithChannel(channelID, channelType);
    }


    public List<YutakMsgReaction> getMsgReactions(String messageID) {
        List<String> ids = new ArrayList<>();
        ids.add(messageID);
        return MsgDBManager.getInstance().queryMsgReactionWithMsgIds(ids);
    }

    private void getMsgReactionsAndRefreshMsg(List<String> messageIds, List<YutakMsg> updatedMsgList) {
        List<YutakMsgReaction> reactionList = MsgDBManager.getInstance().queryMsgReactionWithMsgIds(messageIds);
        for (int i = 0, size = updatedMsgList.size(); i < size; i++) {
            for (int j = 0, len = reactionList.size(); j < len; j++) {
                if (updatedMsgList.get(i).messageID.equals(reactionList.get(j).messageID)) {
                    if (updatedMsgList.get(i).reactionList == null)
                        updatedMsgList.get(i).reactionList = new ArrayList<>();
                    updatedMsgList.get(i).reactionList.add(reactionList.get(j));
                }
            }
            setRefreshMsg(updatedMsgList.get(i), i == updatedMsgList.size() - 1);
        }
    }


    public synchronized long getClientSeq() {
        return MsgDBManager.getInstance().queryMaxMessageSeqWithChannel();
    }

    /**
     * 修改消息的扩展字段
     *
     * @param clientMsgNo 客户端ID
     * @param hashExtra   扩展字段
     */
    public boolean updateLocalExtraWithClientMsgNO(String clientMsgNo, HashMap<String, Object> hashExtra) {
        if (hashExtra != null) {
            JSONObject jsonObject = new JSONObject();
            for (String key : hashExtra.keySet()) {
                try {
                    jsonObject.put(key, hashExtra.get(key));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            return MsgDBManager.getInstance().updateFieldWithClientMsgNo(clientMsgNo, DB.MessageColumns.extra, jsonObject.toString(), true);
        }

        return false;
    }

    /**
     * 查询按日期分组的消息数量
     *
     * @param channelID   频道ID
     * @param channelType 频道类型
     * @return List<YutakMessageGroupByDate>
     */
    public List<YutakMessageGroupByDate> getMessageGroupByDateWithChannel(String channelID, byte channelType) {
        return MsgDBManager.getInstance().queryMessageGroupByDateWithChannel(channelID, channelType);
    }

    public void clearAll() {
        MsgDBManager.getInstance().clearEmpty();
    }

    public void saveMsg(YutakMsg msg) {
        int refreshType = 0;
        if (!TextUtils.isEmpty(msg.clientMsgNO)) {
            YutakMsg tempMsg = MsgDBManager.getInstance().queryWithClientMsgNo(msg.clientMsgNO);
            if (tempMsg != null) {
                refreshType = 1;
            }
        }
        if (msg.orderSeq == 0) {
            long tempOrderSeq = getMessageOrderSeq(0, msg.channelID, msg.channelType);
            msg.orderSeq = tempOrderSeq + 1;
        }
        msg.clientSeq = MsgDBManager.getInstance().insert(msg);
        if (refreshType == 0)
            pushNewMsg(msg);
        else setRefreshMsg(msg, true);
    }

    /**
     * 本地插入一条消息并更新会话记录表且未读消息数量加一
     *
     * @param YutakMsg      消息对象
     * @param addRedDots 是否显示红点
     */
    public void saveAndUpdateConversationMsg(YutakMsg YutakMsg, boolean addRedDots) {
        int refreshType = 0;
        if (!TextUtils.isEmpty(YutakMsg.clientMsgNO)) {
            YutakMsg tempMsg = MsgDBManager.getInstance().queryWithClientMsgNo(YutakMsg.clientMsgNO);
            if (tempMsg != null) {
                refreshType = 1;
            }
        }
        if (YutakMsg.orderSeq == 0) {
            long tempOrderSeq = getMessageOrderSeq(0, YutakMsg.channelID, YutakMsg.channelType);
            YutakMsg.orderSeq = tempOrderSeq + 1;
        }
        YutakMsg.clientSeq = MsgDBManager.getInstance().insert(YutakMsg);
        if (refreshType == 0)
            pushNewMsg(YutakMsg);
        else setRefreshMsg(YutakMsg, true);
        YutakUIConversationMsg msg = ConversationDBManager.getInstance().insertOrUpdateWithMsg(YutakMsg, addRedDots ? 1 : 0);
        YutakIM.get().getConversationManager().setOnRefreshMsg(msg, true, "insertAndUpdateConversationMsg");
    }

    /**
     * 查询某个频道的固定类型消息
     *
     * @param channelID      频道ID
     * @param channelType    频道列席
     * @param oldestOrderSeq 最后一次消息大orderSeq
     * @param limit          每次获取数量
     * @param contentTypes   消息内容类型
     * @return List<YutakMsg>
     */
    public List<YutakMsg> searchMsgWithChannelAndContentTypes(String channelID, byte channelType, long oldestOrderSeq, int limit, int[] contentTypes) {
        return MsgDBManager.getInstance().searchWithChannelAndContentTypes(channelID, channelType, oldestOrderSeq, limit, contentTypes);
    }

    /**
     * 搜索某个频道到消息
     *
     * @param searchKey   关键字
     * @param channelID   频道ID
     * @param channelType 频道类型
     * @return List<YutakMsg>
     */
    public List<YutakMsg> searchWithChannel(String searchKey, String channelID, byte channelType) {
        return MsgDBManager.getInstance().searchWithChannel(searchKey, channelID, channelType);
    }

    public List<YutakMessageSearchResult> search(String searchKey) {
        return MsgDBManager.getInstance().search(searchKey);
    }

    /**
     * 修改语音是否已读
     *
     * @param clientMsgNo 客户端ID
     * @param isReaded    1：已读
     */
    public boolean updateVoiceReadStatus(String clientMsgNo, int isReaded, boolean isRefreshUI) {
        return MsgDBManager.getInstance().updateFieldWithClientMsgNo(clientMsgNo, DB.MessageColumns.voice_status, String.valueOf(isReaded), isRefreshUI);
    }

    /**
     * 清空某个会话信息
     *
     * @param channelId   频道ID
     * @param channelType 频道类型
     */
    public boolean clearWithChannel(String channelId, byte channelType) {
        boolean result = MsgDBManager.getInstance().deleteWithChannel(channelId, channelType);
        if (result) {
            if (clearMsgMap != null && clearMsgMap.size() > 0) {
                runOnMainThread(() -> {
                    for (Map.Entry<String, IClearMsgListener> entry : clearMsgMap.entrySet()) {
                        entry.getValue().clear(channelId, channelType, "");
                    }
                });

            }
        }
        return result;
    }

    public boolean clearWithChannelAndFromUID(String channelId, byte channelType, String fromUID) {
        boolean result = MsgDBManager.getInstance().deleteWithChannelAndFromUID(channelId, channelType, fromUID);
        if (result) {
            if (clearMsgMap != null && clearMsgMap.size() > 0) {
                runOnMainThread(() -> {
                    for (Map.Entry<String, IClearMsgListener> entry : clearMsgMap.entrySet()) {
                        entry.getValue().clear(channelId, channelType, fromUID);
                    }
                });

            }
        }
        return result;
    }


    public boolean updateContentAndRefresh(String clientMsgNo, YutakMessageContent messageContent, boolean isRefreshUI) {
        return MsgDBManager.getInstance().updateFieldWithClientMsgNo(clientMsgNo, DB.MessageColumns.content, messageContent.encodeMsg().toString(), isRefreshUI);
    }

    public void updateViewedAt(int viewed, long viewedAt, String clientMsgNo) {
        MsgDBManager.getInstance().updateViewedAt(viewed, viewedAt, clientMsgNo);
    }

    /**
     * 获取某个类型的聊天数据
     *
     * @param type            消息类型
     * @param oldestClientSeq 最后一次消息客户端ID
     * @param limit           数量
     * @return list
     */
    public List<YutakMsg> getWithContentType(int type, long oldestClientSeq, int limit) {
        return MsgDBManager.getInstance().queryWithContentType(type, oldestClientSeq, limit);
    }

    public void saveAndUpdateConversationMsg(YutakMsg msg) {
        int refreshType = 0;
        if (!TextUtils.isEmpty(msg.clientMsgNO)) {
            YutakMsg tempMsg = MsgDBManager.getInstance().queryWithClientMsgNo(msg.clientMsgNO);
            if (tempMsg != null) {
                refreshType = 1;
            }
        }
        if (msg.orderSeq == 0) {
            long tempOrderSeq = getMessageOrderSeq(0, msg.channelID, msg.channelType);
            msg.orderSeq = tempOrderSeq + 1;
        }
        MsgDBManager.getInstance().insert(msg);
        if (refreshType == 0)
            pushNewMsg(msg);
        else setRefreshMsg(msg, true);
        ConversationDBManager.getInstance().insertOrUpdateWithMsg(msg, 0);
    }


    public long getMsgExtraMaxVersionWithChannel(String channelID, byte channelType) {
        return MsgDBManager.getInstance().queryMsgExtraMaxVersionWithChannel(channelID, channelType);
    }

    public YutakMsg getWithClientMsgNO(String clientMsgNo) {
        return MsgDBManager.getInstance().queryWithClientMsgNo(clientMsgNo);
    }


    public void saveRemoteExtraMsg(YutakChannel channel, List<YutakSyncExtraMsg> list) {
        if (list == null || list.isEmpty()) return;
        List<YutakMsgExtra> extraList = new ArrayList<>();
        List<String> messageIds = new ArrayList<>();
        List<String> deleteMsgIds = new ArrayList<>();
        for (int i = 0, size = list.size(); i < size; i++) {
            if (TextUtils.isEmpty(list.get(i).message_id)) {
                continue;
            }
            YutakMsgExtra extra = YutakSyncExtraMsg2YutakMsgExtra(channel.channelID, channel.channelType, list.get(i));
            extraList.add(extra);
            messageIds.add(list.get(i).message_id);
            if (extra.isMutualDeleted == 1) {
                deleteMsgIds.add(list.get(i).message_id);
            }
        }
        List<YutakMsg> updatedMsgList = MsgDBManager.getInstance().insertOrUpdateMsgExtras(extraList);
        if (!deleteMsgIds.isEmpty()) {
            MsgDBManager.getInstance().deleteWithMessageIDs(deleteMsgIds);
        }
        getMsgReactionsAndRefreshMsg(messageIds, updatedMsgList);
    }

    public void addOnSyncOfflineMsgListener(ISyncOfflineMsgListener iOfflineMsgListener) {
        this.iOfflineMsgListener = iOfflineMsgListener;
    }

    public void addOnSyncMsgReactionListener(ISyncMsgReaction iSyncMsgReactionListener) {
        if (iSyncMsgReactionListener != null) {
            this.iSyncMsgReaction = iSyncMsgReactionListener;
        }
    }

    //添加删除消息监听
    public void addOnDeleteMsgListener(String key, IDeleteMsgListener iDeleteMsgListener) {
        if (iDeleteMsgListener == null || TextUtils.isEmpty(key)) return;
        if (deleteMsgListenerMap == null) deleteMsgListenerMap = new ConcurrentHashMap<>();
        deleteMsgListenerMap.put(key, iDeleteMsgListener);
    }

    public void removeDeleteMsgListener(String key) {
        if (TextUtils.isEmpty(key)) return;
        if (deleteMsgListenerMap != null) deleteMsgListenerMap.remove(key);
    }

    //设置删除消息
    public void setDeleteMsg(YutakMsg msg) {
        if (deleteMsgListenerMap != null && deleteMsgListenerMap.size() > 0) {
            runOnMainThread(() -> {
                for (Map.Entry<String, IDeleteMsgListener> entry : deleteMsgListenerMap.entrySet()) {
                    entry.getValue().onDeleteMsg(msg);
                }
            });
        }
    }


    void saveMsgReactions(List<YutakMsgReaction> list) {
        MsgDBManager.getInstance().insertMsgReactions(list);
    }


    public void setSyncOfflineMsg(ISyncOfflineMsgBack iSyncOfflineMsgBack) {
        syncOfflineMsg(iSyncOfflineMsgBack);
    }

    private void syncOfflineMsg(ISyncOfflineMsgBack iSyncOfflineMsgBack) {
        if (iOfflineMsgListener != null) {
            runOnMainThread(() -> {
                long max_message_seq = getMaxMessageSeq();
                iOfflineMsgListener.getOfflineMsgs(max_message_seq, (isEnd, list) -> {
                    //保存同步消息
                    saveSyncMsg(list);
                    if (isEnd) {
                        iSyncOfflineMsgBack.onBack(isEnd, null);
                    } else {
                        syncOfflineMsg(iSyncOfflineMsgBack);
                    }
                });
            });
        } else iSyncOfflineMsgBack.onBack(true, null);
    }


    public void setSendMsgCallback(YutakMsg msg) {
        if (sendMsgCallBackListenerHashMap != null && sendMsgCallBackListenerHashMap.size() > 0) {
            runOnMainThread(() -> {
                for (Map.Entry<String, ISendMsgCallBackListener> entry : sendMsgCallBackListenerHashMap.entrySet()) {
                    entry.getValue().onInsertMsg(msg);
                }
            });
        }
    }

    public void addOnSendMsgCallback(String key, ISendMsgCallBackListener iSendMsgCallBackListener) {
        if (TextUtils.isEmpty(key)) return;
        if (sendMsgCallBackListenerHashMap == null) {
            sendMsgCallBackListenerHashMap = new ConcurrentHashMap<>();
        }
        sendMsgCallBackListenerHashMap.put(key, iSendMsgCallBackListener);
    }

    public void removeSendMsgCallBack(String key) {
        if (sendMsgCallBackListenerHashMap != null) {
            sendMsgCallBackListenerHashMap.remove(key);
        }
    }


    //监听同步频道消息
    public void addOnSyncChannelMsgListener(ISyncChannelMsgListener listener) {
        this.iSyncChannelMsgListener = listener;
    }

    public void setSyncChannelMsgListener(String channelID, byte channelType, long startMessageSeq, long endMessageSeq, int limit, int pullMode, ISyncChannelMsgBack iSyncChannelMsgBack) {
        if (this.iSyncChannelMsgListener != null) {
            runOnMainThread(() -> iSyncChannelMsgListener.syncChannelMsgs(channelID, channelType, startMessageSeq, endMessageSeq, limit, pullMode, syncChannelMsg -> {
                if (syncChannelMsg != null && syncChannelMsg.messages != null && syncChannelMsg.messages.size() > 0) {
                    saveSyncChannelMSGs(syncChannelMsg.messages);
                }
                iSyncChannelMsgBack.onBack(syncChannelMsg);
            }));
        }
    }


    private void saveSyncChannelMSGs(List<YutakSyncRecent> list) {
        if (list == null || list.size() == 0) return;
        List<YutakMsg> msgList = new ArrayList<>();
        List<YutakMsgExtra> msgExtraList = new ArrayList<>();
        List<YutakMsgReaction> reactionList = new ArrayList<>();
        for (int j = 0, len = list.size(); j < len; j++) {
            YutakMsg YutakMsg = YutakSyncRecent2YutakMsg(list.get(j));
            msgList.add(YutakMsg);
            if (list.get(j).message_extra != null) {
                YutakMsgExtra extra = YutakSyncExtraMsg2YutakMsgExtra(YutakMsg.channelID, YutakMsg.channelType, list.get(j).message_extra);
                msgExtraList.add(extra);
            }
            if (YutakMsg.reactionList != null && YutakMsg.reactionList.size() > 0) {
                reactionList.addAll(YutakMsg.reactionList);
            }
        }
        if (msgExtraList.size() > 0) {
            MsgDBManager.getInstance().insertOrUpdateMsgExtras(msgExtraList);
        }
        if (msgList.size() > 0) {
            MsgDBManager.getInstance().insertMsgs(msgList);
        }
        if (reactionList.size() > 0) {
            MsgDBManager.getInstance().insertMsgReactions(reactionList);
        }
    }

    public void addOnSendMsgAckListener(String key, ISendACK iSendACKListener) {
        if (iSendACKListener == null || TextUtils.isEmpty(key)) return;
        if (sendAckListenerMap == null) sendAckListenerMap = new ConcurrentHashMap<>();
        sendAckListenerMap.put(key, iSendACKListener);
    }

    public void setSendMsgAck(YutakMsg msg) {
        if (sendAckListenerMap != null && sendAckListenerMap.size() > 0) {
            runOnMainThread(() -> {
                for (Map.Entry<String, ISendACK> entry : sendAckListenerMap.entrySet()) {
                    entry.getValue().msgACK(msg);
                }
            });

        }
    }

    public void removeSendMsgAckListener(String key) {
        if (!TextUtils.isEmpty(key) && sendAckListenerMap != null) {
            sendAckListenerMap.remove(key);
        }
    }

    public void addOnUploadAttachListener(IUploadAttachmentListener iUploadAttachmentListener) {
        this.iUploadAttachmentListener = iUploadAttachmentListener;
    }

    public void setUploadAttachment(YutakMsg msg, IUploadAttacResultListener resultListener) {
        if (iUploadAttachmentListener != null) {
            runOnMainThread(() -> {
                iUploadAttachmentListener.onUploadAttachmentListener(msg, resultListener);
            });
        }
    }

    public void addMessageStoreBeforeIntercept(IMessageStoreBeforeIntercept iMessageStoreBeforeInterceptListener) {
        messageStoreBeforeIntercept = iMessageStoreBeforeInterceptListener;
    }

    public boolean setMessageStoreBeforeIntercept(YutakMsg msg) {
        return messageStoreBeforeIntercept == null || messageStoreBeforeIntercept.isSaveMsg(msg);
    }

    //添加消息修改
    public void addOnRefreshMsgListener(String key, IRefreshMsg listener) {
        if (TextUtils.isEmpty(key) || listener == null) return;
        if (refreshMsgListenerMap == null) refreshMsgListenerMap = new ConcurrentHashMap<>();
        refreshMsgListenerMap.put(key, listener);
    }


    public void removeRefreshMsgListener(String key) {
        if (!TextUtils.isEmpty(key) && refreshMsgListenerMap != null) {
            refreshMsgListenerMap.remove(key);
        }
    }

    public void setRefreshMsg(YutakMsg msg, boolean left) {
        if (refreshMsgListenerMap != null && refreshMsgListenerMap.size() > 0) {
            runOnMainThread(() -> {
                for (Map.Entry<String, IRefreshMsg> entry : refreshMsgListenerMap.entrySet()) {
                    entry.getValue().onRefresh(msg, left);
                }
            });

        }
    }

    public void addOnNewMsgListener(String key, INewMsgListener iNewMsgListener) {
        if (TextUtils.isEmpty(key) || iNewMsgListener == null) return;
        if (newMsgListenerMap == null)
            newMsgListenerMap = new ConcurrentHashMap<>();
        newMsgListenerMap.put(key, iNewMsgListener);
    }

    public void removeNewMsgListener(String key) {
        if (TextUtils.isEmpty(key)) return;
        if (newMsgListenerMap != null) newMsgListenerMap.remove(key);
    }

    public void addOnClearMsgListener(String key, IClearMsgListener iClearMsgListener) {
        if (TextUtils.isEmpty(key) || iClearMsgListener == null) return;
        if (clearMsgMap == null) clearMsgMap = new ConcurrentHashMap<>();
        clearMsgMap.put(key, iClearMsgListener);
    }

    public void removeClearMsg(String key) {
        if (TextUtils.isEmpty(key)) return;
        if (clearMsgMap != null) clearMsgMap.remove(key);
    }


    YutakMsgExtra YutakSyncExtraMsg2YutakMsgExtra(String channelID, byte channelType, YutakSyncExtraMsg extraMsg) {
        YutakMsgExtra extra = new YutakMsgExtra();
        extra.channelID = channelID;
        extra.channelType = channelType;
        extra.unreadCount = extraMsg.unread_count;
        extra.readedCount = extraMsg.readed_count;
        extra.readed = extraMsg.readed;
        extra.messageID = extraMsg.message_id;
        extra.isMutualDeleted = extraMsg.is_mutual_deleted;
        extra.extraVersion = extraMsg.extra_version;
        extra.revoke = extraMsg.revoke;
        extra.revoker = extraMsg.revoker;
        extra.needUpload = 0;
        if (extraMsg.content_edit != null) {
            JSONObject jsonObject = new JSONObject(extraMsg.content_edit);
            extra.contentEdit = jsonObject.toString();
        }

        extra.editedAt = extraMsg.edited_at;
        return extra;
    }

    YutakMsg YutakSyncRecent2YutakMsg(YutakSyncRecent YutakSyncRecent) {
        YutakMsg msg = new YutakMsg();
        msg.channelID = YutakSyncRecent.channel_id;
        msg.channelType = YutakSyncRecent.channel_type;
        msg.messageID = YutakSyncRecent.message_id;
        msg.messageSeq = YutakSyncRecent.message_seq;
        msg.clientMsgNO = YutakSyncRecent.client_msg_no;
        msg.fromUID = YutakSyncRecent.from_uid;
        msg.timestamp = YutakSyncRecent.timestamp;
        msg.orderSeq = msg.messageSeq * YutakOrderSeqFactor;
        msg.voiceStatus = YutakSyncRecent.voice_status;
        msg.isDeleted = YutakSyncRecent.is_deleted;
        msg.status = Yutak.SendMsgResult.send_success;
        msg.remoteExtra = new YutakMsgExtra();
        msg.remoteExtra.revoke = YutakSyncRecent.revoke;
        msg.remoteExtra.revoker = YutakSyncRecent.revoker;
        msg.remoteExtra.unreadCount = YutakSyncRecent.unread_count;
        msg.remoteExtra.readedCount = YutakSyncRecent.readed_count;
        msg.remoteExtra.readed = YutakSyncRecent.readed;
        msg.expireTime = YutakSyncRecent.expire;
        msg.expireTimestamp = msg.expireTime + msg.timestamp;
        // msg.reactionList = YutakSyncRecent.reactions;
        // msg.receipt = YutakSyncRecent.receipt;
        msg.remoteExtra.extraVersion = YutakSyncRecent.extra_version;
        //处理消息设置
        byte[] setting = TypeKit.getInstance().intToByte(YutakSyncRecent.setting);
        msg.setting = TypeKit.getInstance().getMsgSetting(setting[0]);
        //如果是单聊先将channelId改成发送者ID
        if (!TextUtils.isEmpty(msg.channelID)
                && !TextUtils.isEmpty(msg.fromUID)
                && msg.channelType == YutakChannelType.PERSONAL
                && msg.channelID.equals(YutakApplication.get().getUid())) {
            msg.channelID = msg.fromUID;
        }

        if (YutakSyncRecent.payload != null) {
            JSONObject jsonObject = new JSONObject(YutakSyncRecent.payload);
            msg.content = jsonObject.toString();
        }
        JSONObject jsonObject = null;
        if (!TextUtils.isEmpty(msg.content)) {
            try {
                jsonObject = new JSONObject(msg.content);
                if (jsonObject.has("type"))
                    msg.type = jsonObject.optInt("type");
                jsonObject.put(DB.MessageColumns.from_uid, msg.fromUID);
                if (jsonObject.has("flame"))
                    msg.flame = jsonObject.optInt("flame");
                if (jsonObject.has("flame_second"))
                    msg.flameSecond = jsonObject.optInt("flame_second");
                msg.content = jsonObject.toString();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        // 处理消息回应
        if (YutakSyncRecent.reactions != null && YutakSyncRecent.reactions.size() > 0) {
            msg.reactionList = getMsgReaction(YutakSyncRecent);
        }
        if (msg.type != Yutak.MsgContentType.SIGNAL_DECRYPT_ERROR && msg.type != Yutak.MsgContentType.CONTENT_FORMAT_ERROR)
            msg.baseContentMsgModel = YutakIM.get().getMsgManager().getMsgContentModel(msg.type, jsonObject);

        return msg;
    }

    private List<YutakMsgReaction> getMsgReaction(YutakSyncRecent YutakSyncRecent) {
        List<YutakMsgReaction> list = new ArrayList<>();
        for (int i = 0, size = YutakSyncRecent.reactions.size(); i < size; i++) {
            YutakMsgReaction reaction = new YutakMsgReaction();
            reaction.channelID = YutakSyncRecent.reactions.get(i).channel_id;
            reaction.channelType = YutakSyncRecent.reactions.get(i).channel_type;
            reaction.uid = YutakSyncRecent.reactions.get(i).uid;
            reaction.name = YutakSyncRecent.reactions.get(i).name;
            reaction.emoji = YutakSyncRecent.reactions.get(i).emoji;
            reaction.seq = YutakSyncRecent.reactions.get(i).seq;
            reaction.isDeleted = YutakSyncRecent.reactions.get(i).is_deleted;
            reaction.messageID = YutakSyncRecent.reactions.get(i).message_id;
            reaction.createdAt = YutakSyncRecent.reactions.get(i).created_at;
            list.add(reaction);
        }
        return list;
    }

    public void saveSyncMsg(List<YutakSyncMsg> YutakSyncMsgs) {
        if (YutakSyncMsgs == null || YutakSyncMsgs.size() == 0) return;
        for (int i = 0, size = YutakSyncMsgs.size(); i < size; i++) {
            YutakSyncMsgs.get(i).yutakMsg = MessageHandler.getInstance().parsingMsg(YutakSyncMsgs.get(i).yutakMsg);
            if (YutakSyncMsgs.get(i).yutakMsg.timestamp != 0)
                YutakSyncMsgs.get(i).yutakMsg.orderSeq = YutakSyncMsgs.get(i).yutakMsg.timestamp;
            else
                YutakSyncMsgs.get(i).yutakMsg.orderSeq = getMessageOrderSeq(YutakSyncMsgs.get(i).yutakMsg.messageSeq, YutakSyncMsgs.get(i).yutakMsg.channelID, YutakSyncMsgs.get(i).yutakMsg.channelType);
        }
        MessageHandler.getInstance().saveSyncMsg(YutakSyncMsgs);
    }


    public void updateMsgEdit(String msgID, String channelID, byte channelType, String content) {
        YutakMsgExtra YutakMsgExtra = MsgDBManager.getInstance().queryMsgExtraWithMsgID(msgID);
        if (YutakMsgExtra == null) {
            YutakMsgExtra = new YutakMsgExtra();
        }
        YutakMsgExtra.messageID = msgID;
        YutakMsgExtra.channelID = channelID;
        YutakMsgExtra.channelType = channelType;
        YutakMsgExtra.editedAt = DateKit.get().nowSeconds();
        YutakMsgExtra.contentEdit = content;
        YutakMsgExtra.needUpload = 1;
        List<YutakMsgExtra> list = new ArrayList<>();
        list.add(YutakMsgExtra);
        List<YutakMsg> YutakMsgs = MsgDBManager.getInstance().insertOrUpdateMsgExtras(list);
        List<String> messageIds = new ArrayList<>();
        messageIds.add(msgID);
        if (YutakMsgs != null && YutakMsgs.size() > 0) {
            getMsgReactionsAndRefreshMsg(messageIds, YutakMsgs);
            setUploadMsgExtra(YutakMsgExtra);
        }
    }

    private synchronized void startCheckTimer() {
        if (checkMsgNeedUploadTimer == null) {
            checkMsgNeedUploadTimer = new Timer();
        }
        checkMsgNeedUploadTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                List<YutakMsgExtra> list = MsgDBManager.getInstance().queryMsgExtraWithNeedUpload(1);
                if (list != null && list.size() > 0) {
                    for (YutakMsgExtra extra : list) {
                        if (iUploadMsgExtraListener != null) {
                            iUploadMsgExtraListener.onUpload(extra);
                        }
                    }
                } else {
                    checkMsgNeedUploadTimer.cancel();
                    checkMsgNeedUploadTimer.purge();
                    checkMsgNeedUploadTimer = null;
                }
            }
        }, 1000 * 5, 1000 * 5);
    }

    private void setUploadMsgExtra(YutakMsgExtra extra) {
        if (iUploadMsgExtraListener != null) {
            iUploadMsgExtraListener.onUpload(extra);
        }
        startCheckTimer();
    }

    public void addOnUploadMsgExtraListener(IUploadMsgExtraListener iUploadMsgExtraListener) {
        this.iUploadMsgExtraListener = iUploadMsgExtraListener;
    }

    public void pushNewMsg(List<YutakMsg> YutakMsgList) {
        if (newMsgListenerMap != null && newMsgListenerMap.size() > 0) {
            runOnMainThread(() -> {
                for (Map.Entry<String, INewMsgListener> entry : newMsgListenerMap.entrySet()) {
                    entry.getValue().newMsg(YutakMsgList);
                }
            });
        }
    }

    /**
     * push新消息
     *
     * @param msg 消息
     */
    public void pushNewMsg(YutakMsg msg) {
        if (msg == null) return;
        List<YutakMsg> msgs = new ArrayList<>();
        msgs.add(msg);
        pushNewMsg(msgs);
    }


    public void sendMessage(YutakMessageContent messageContent, String channelID, byte channelType) {
        YutakConnection.getInstance().sendMessage(messageContent, channelID, channelType);
    }

    public void sendMessage(YutakMessageContent messageContent, YutakMsgSetting setting, String channelID, byte channelType) {
        YutakConnection.getInstance().sendMessage(messageContent, setting, channelID, channelType);
    }

    /**
     * 发送消息
     *
     * @param msg 消息对象
     */
    public void sendMessage(YutakMsg msg) {
        YutakConnection.getInstance().sendMessage(msg);
    }

    public String createClientMsgNO() {
        return UUID.randomUUID().toString().replaceAll("-", "") + "1";
    }
}
