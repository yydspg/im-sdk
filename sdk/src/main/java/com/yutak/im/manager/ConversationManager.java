package com.yutak.im.manager;


import android.content.ContentValues;
import android.text.TextUtils;

import com.yutak.im.YutakApplication;
import com.yutak.im.YutakIM;
import com.yutak.im.cs.Yutak;
import com.yutak.im.db.ConversationDBManager;
import com.yutak.im.db.MsgDBManager;
import com.yutak.im.domain.YutakChannelType;
import com.yutak.im.domain.YutakConversationMsg;
import com.yutak.im.domain.YutakConversationMsgExtra;
import com.yutak.im.domain.YutakMsg;
import com.yutak.im.domain.YutakMsgExtra;
import com.yutak.im.domain.YutakMsgReaction;
import com.yutak.im.domain.YutakSyncChat;
import com.yutak.im.domain.YutakSyncConvMsgExtra;
import com.yutak.im.domain.YutakSyncRecent;
import com.yutak.im.domain.YutakUIConversationMsg;
import com.yutak.im.interfaces.IDeleteConversationMsg;
import com.yutak.im.interfaces.IRefreshConversationMsg;
import com.yutak.im.interfaces.ISyncConversationChat;
import com.yutak.im.interfaces.ISyncConversationChatBack;
import com.yutak.im.kit.LogKit;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// recent conversation manager
public class ConversationManager extends BaseManager {
    private ConversationManager() {
    }

    private static class ConversationManagerBinder {
        static final ConversationManager manager = new ConversationManager();
    }

    public static ConversationManager getInstance() {
        return ConversationManagerBinder.manager;
    }

    //监听刷新最近会话
    private ConcurrentHashMap<String, IRefreshConversationMsg> refreshMsgList;

    //移除某个会话
    private ConcurrentHashMap<String, IDeleteConversationMsg> iDeleteMsgList;
    // 同步最近会话
    private ISyncConversationChat iSyncConversationChat;

    /**
     * 查询会话记录消息
     *
     * @return 最近会话集合
     */
    public List<YutakUIConversationMsg> getAll() {
        return ConversationDBManager.getInstance().queryAll();
    }

    public List<YutakConversationMsg> getWithChannelType(byte channelType) {
        return ConversationDBManager.getInstance().queryWithChannelType(channelType);
    }

    public List<YutakUIConversationMsg> getWithChannelIds(List<String> channelIds) {
        return ConversationDBManager.getInstance().queryWithChannelIds(channelIds);
    }

    /**
     * 查询某条消息
     *
     * @param channelID   频道ID
     * @param channelType 频道类型
     * @return YutakConversationMsg
     */
    public YutakConversationMsg getWithChannel(String channelID, byte channelType) {
        return ConversationDBManager.getInstance().queryWithChannel(channelID, channelType);
    }

    public void updateWithMsg(YutakConversationMsg mConversationMsg) {
        YutakMsg msg = MsgDBManager.getInstance().queryMaxOrderSeqMsgWithChannel(mConversationMsg.channelID, mConversationMsg.channelType);
        if (msg != null) {
            mConversationMsg.lastClientMsgNO = msg.clientMsgNO;
            mConversationMsg.lastMsgSeq = msg.messageSeq;
        }
        ConversationDBManager.getInstance().updateMsg(mConversationMsg.channelID, mConversationMsg.channelType, mConversationMsg.lastClientMsgNO, mConversationMsg.lastMsgSeq, mConversationMsg.unreadCount);
    }

    /**
     * 删除某个会话记录信息
     *
     * @param channelId   频道ID
     * @param channelType 频道类型
     */
    public boolean deleteWitchChannel(String channelId, byte channelType) {
        return ConversationDBManager.getInstance().deleteWithChannel(channelId, channelType, 1);
    }

    /**
     * 清除所有最近会话
     */
    public boolean clearAll() {
        return ConversationDBManager.getInstance().clearEmpty();
    }


    /**
     * 监听刷新最近会话
     *
     * @param listener 回调
     */
    public void addOnRefreshMsgListener(String key, IRefreshConversationMsg listener) {
        if (TextUtils.isEmpty(key) || listener == null) return;
        if (refreshMsgList == null)
            refreshMsgList = new ConcurrentHashMap<>();
        refreshMsgList.put(key, listener);
    }

    public void removeOnRefreshMsgListener(String key) {
        if (TextUtils.isEmpty(key) || refreshMsgList == null) return;
        refreshMsgList.remove(key);
    }

    /**
     * 设置刷新最近会话
     */
    public void setOnRefreshMsg(YutakUIConversationMsg conversationMsg, boolean isEnd, String from) {
        if (refreshMsgList != null && refreshMsgList.size() > 0 && conversationMsg != null) {
            runOnMainThread(() -> {
                for (Map.Entry<String, IRefreshConversationMsg> entry : refreshMsgList.entrySet()) {
                    entry.getValue().onRefreshConversationMsg(conversationMsg, isEnd);
                }
            });
        }
    }

    //监听删除最近会话监听
    public void addOnDeleteMsgListener(String key, IDeleteConversationMsg listener) {
        if (TextUtils.isEmpty(key) || listener == null) return;
        if (iDeleteMsgList == null) iDeleteMsgList = new ConcurrentHashMap<>();
        iDeleteMsgList.put(key, listener);
    }

    public void removeOnDeleteMsgListener(String key) {
        if (TextUtils.isEmpty(key) || iDeleteMsgList == null) return;
        iDeleteMsgList.remove(key);
    }

    // 删除某个最近会话
    public void setDeleteMsg(String channelID, byte channelType) {
        if (iDeleteMsgList != null && iDeleteMsgList.size() > 0) {
            runOnMainThread(() -> {
                for (Map.Entry<String, IDeleteConversationMsg> entry : iDeleteMsgList.entrySet()) {
                    entry.getValue().onDelete(channelID, channelType);
                }
            });
        }
    }

    public void updateRedDot(String channelID, byte channelType, int redDot) {
        boolean result = ConversationDBManager.getInstance().updateRedDot(channelID, channelType, redDot);
        if (result) {
            YutakUIConversationMsg msg = getUIConversationMsg(channelID, channelType);
            setOnRefreshMsg(msg, true, "updateRedDot");
        }
    }

    public YutakConversationMsgExtra getMsgExtraWithChannel(String channelID, byte channelType) {
        return ConversationDBManager.getInstance().queryMsgExtraWithChannel(channelID, channelType);
    }

    public void updateMsgExtra(YutakConversationMsgExtra extra) {
        boolean result = ConversationDBManager.getInstance().insertOrUpdateMsgExtra(extra);
        if (result) {
            YutakUIConversationMsg msg = getUIConversationMsg(extra.channelID, extra.channelType);
            setOnRefreshMsg(msg, true, "updateMsgExtra");
        }
    }

    public YutakUIConversationMsg updateWithYutakMsg(YutakMsg msg) {
        if (msg == null || TextUtils.isEmpty(msg.channelID)) return null;
        return ConversationDBManager.getInstance().insertOrUpdateWithMsg(msg, 0);
    }

    public YutakUIConversationMsg getUIConversationMsg(String channelID, byte channelType) {
        YutakConversationMsg msg = ConversationDBManager.getInstance().queryWithChannel(channelID, channelType);
        if (msg == null) {
            return null;
        }
        return ConversationDBManager.getInstance().getUIMsg(msg);
    }

    public long getMsgExtraMaxVersion() {
        return ConversationDBManager.getInstance().queryMsgExtraMaxVersion();
    }

    public void saveSyncMsgExtras(List<YutakSyncConvMsgExtra> list) {
        List<YutakConversationMsgExtra> msgExtraList = new ArrayList<>();
        for (YutakSyncConvMsgExtra msg : list) {
            msgExtraList.add(syncConvMsgExtraToConvMsgExtra(msg));
        }
        ConversationDBManager.getInstance().insertMsgExtras(msgExtraList);
    }

    private YutakConversationMsgExtra syncConvMsgExtraToConvMsgExtra(YutakSyncConvMsgExtra extra) {
        YutakConversationMsgExtra msg = new YutakConversationMsgExtra();
        msg.channelID = extra.channel_id;
        msg.channelType = extra.channel_type;
        msg.draft = extra.draft;
        msg.keepOffsetY = extra.keep_offset_y;
        msg.keepMessageSeq = extra.keep_message_seq;
        msg.version = extra.version;
        msg.browseTo = extra.browse_to;
        msg.draftUpdatedAt = extra.draft_updated_at;
        return msg;
    }


    public void addOnSyncConversationListener(ISyncConversationChat iSyncConvChatListener) {
        this.iSyncConversationChat = iSyncConvChatListener;
    }

    public void setSyncConversationListener(ISyncConversationChatBack iSyncConversationChatBack) {
        if (iSyncConversationChat != null) {
            long version = ConversationDBManager.getInstance().queryMaxVersion();
            String lastMsgSeqStr = ConversationDBManager.getInstance().queryLastMsgSeqs();
            runOnMainThread(() -> iSyncConversationChat.syncConversationChat(lastMsgSeqStr, 20, version, syncChat -> {
                new Thread(() -> saveSyncChat(syncChat, () -> iSyncConversationChatBack.onBack(syncChat))).start();
            }));
        }
    }


    interface ISaveSyncChatBack {
        void onBack();
    }


    private void saveSyncChat(YutakSyncChat syncChat, final ISaveSyncChatBack iSaveSyncChatBack) {
        if (syncChat == null) {
            iSaveSyncChatBack.onBack();
            return;
        }
        List<YutakConversationMsg> conversationMsgList = new ArrayList<>();
        List<YutakMsg> msgList = new ArrayList<>();
        List<YutakMsgReaction> msgReactionList = new ArrayList<>();
        List<YutakMsgExtra> msgExtraList = new ArrayList<>();
        if (syncChat.conversations != null && syncChat.conversations.size() > 0) {
            for (int i = 0, size = syncChat.conversations.size(); i < size; i++) {
                //最近会话消息对象
                YutakConversationMsg conversationMsg = new YutakConversationMsg();
                byte channelType = syncChat.conversations.get(i).channel_type;
                String channelID = syncChat.conversations.get(i).channel_id;
                if (channelType == YutakChannelType.COMMUNITY_TOPIC) {
                    String[] str = channelID.split("@");
                    conversationMsg.parentChannelID = str[0];
                    conversationMsg.parentChannelType = YutakChannelType.COMMUNITY;
                }
                conversationMsg.channelID = syncChat.conversations.get(i).channel_id;
                conversationMsg.channelType = syncChat.conversations.get(i).channel_type;
                conversationMsg.lastMsgSeq = syncChat.conversations.get(i).last_msg_seq;
                conversationMsg.lastClientMsgNO = syncChat.conversations.get(i).last_client_msg_no;
                conversationMsg.lastMsgTimestamp = syncChat.conversations.get(i).timestamp;
                conversationMsg.unreadCount = syncChat.conversations.get(i).unread;
                conversationMsg.version = syncChat.conversations.get(i).version;
                //聊天消息对象
                if (syncChat.conversations.get(i).recents != null && syncChat.conversations.get(i).recents.size() > 0) {
                    for (YutakSyncRecent yutakSyncRecent : syncChat.conversations.get(i).recents) {
                        YutakMsg msg = MsgManager.getInstance().YutakSyncRecent2YutakMsg(yutakSyncRecent);
                        if (msg.reactionList != null && msg.reactionList.size() > 0) {
                            msgReactionList.addAll(msg.reactionList);
                        }
                        //判断会话列表的fromUID
                        if (conversationMsg.lastClientMsgNO.equals(msg.clientMsgNO)) {
                            conversationMsg.isDeleted = msg.isDeleted;
                        }
                        if (yutakSyncRecent.message_extra != null) {
                            YutakMsgExtra extra = MsgManager.getInstance().YutakSyncExtraMsg2YutakMsgExtra(msg.channelID, msg.channelType, yutakSyncRecent.message_extra);
                            msgExtraList.add(extra);
                        }
                        msgList.add(msg);
                    }
                }

                conversationMsgList.add(conversationMsg);
            }
        }
        if (msgExtraList.size() > 0) {
            MsgDBManager.getInstance().insertOrUpdateMsgExtras(msgExtraList);
        }
        List<YutakUIConversationMsg> uiMsgList = new ArrayList<>();
        if (conversationMsgList.size() > 0 || msgList.size() > 0) {
            if (msgList.size() > 0) {
                MsgDBManager.getInstance().insertMsgs(msgList);
            }
            try {
                if (conversationMsgList.size() > 0) {
                    List<ContentValues> cvList = new ArrayList<>();
                    for (int i = 0, size = conversationMsgList.size(); i < size; i++) {
                        ContentValues cv = ConversationDBManager.getInstance().getInsertSyncCV(conversationMsgList.get(i));
                        cvList.add(cv);
                        YutakUIConversationMsg uiMsg = ConversationDBManager.getInstance().getUIMsg(conversationMsgList.get(i));
                        if (uiMsg != null) {
                            uiMsgList.add(uiMsg);
                        }
                    }
                    YutakApplication.get().getDbHelper().getDb()
                            .beginTransaction();
                    for (ContentValues cv : cvList) {
                        ConversationDBManager.getInstance().insertSyncMsg(cv);
                    }
                    YutakApplication.get().getDbHelper().getDb()
                            .setTransactionSuccessful();
                }
            } catch (Exception ignored) {
                LogKit.get().e("同步会话消息保存异常");
            } finally {
                if (YutakApplication.get().getDbHelper().getDb().inTransaction()) {
                    YutakApplication.get().getDbHelper().getDb()
                            .endTransaction();
                }
            }
            if (msgReactionList.size() > 0) {
                MsgManager.getInstance().saveMsgReactions(msgReactionList);
            }
            // fixme 离线消息应该不能push给UI
            if (msgList.size() > 0) {
                HashMap<String, List<YutakMsg>> allMsgMap = new HashMap<>();
                for (YutakMsg YutakMsg : msgList) {
                    if (TextUtils.isEmpty(YutakMsg.channelID)) continue;
                    List<YutakMsg> list;
                    if (allMsgMap.containsKey(YutakMsg.channelID)) {
                        list = allMsgMap.get(YutakMsg.channelID);
                        if (list == null) {
                            list = new ArrayList<>();
                        }
                    } else {
                        list = new ArrayList<>();
                    }
                    list.add(YutakMsg);
                    allMsgMap.put(YutakMsg.channelID, list);
                }

//                for (Map.Entry<String, List<YutakMsg>> entry : allMsgMap.entrySet()) {
//                    List<YutakMsg> channelMsgList = entry.getValue();
//                    if (channelMsgList != null && channelMsgList.size() < 20) {
//                        Collections.sort(channelMsgList, new Comparator<YutakMsg>() {
//                            @Override
//                            public int compare(YutakMsg o1, YutakMsg o2) {
//                                return Long.compare(o1.messageSeq, o2.messageSeq);
//                            }
//                        });
//                        MsgManager.getInstance().pushNewMsg(channelMsgList);
//                    }
//                }


            }
            if (uiMsgList.size() > 0) {
                for (int i = 0, size = uiMsgList.size(); i < size; i++) {
                    YutakIM.get().getConversationManager().setOnRefreshMsg(uiMsgList.get(i), i == uiMsgList.size() - 1, "saveSyncChat");
                }
            }
        }

        if (syncChat.cmds != null && syncChat.cmds.size() > 0) {
            try {
                for (int i = 0, size = syncChat.cmds.size(); i < size; i++) {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("cmd", syncChat.cmds.get(i).cmd);
                    JSONObject json = new JSONObject(syncChat.cmds.get(i).param);
                    jsonObject.put("param", json);
                    CMDManager.getInstance().handleCMD(jsonObject);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        YutakIM.get().getConnectionManager().setConnectionStatus(Yutak.ConnectStatus.syncCompleted, "");
        iSaveSyncChatBack.onBack();
    }
}
