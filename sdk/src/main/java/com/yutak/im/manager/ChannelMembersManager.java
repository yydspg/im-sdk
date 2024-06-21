package com.yutak.im.manager;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.yutak.im.YutakApplication;
import com.yutak.im.YutakIM;
import com.yutak.im.cs.DB;
import com.yutak.im.db.ChannelMemberDBManager;
import com.yutak.im.domain.YutakChannel;
import com.yutak.im.domain.YutakChannelExtras;
import com.yutak.im.domain.YutakChannelMember;
import com.yutak.im.interfaces.IAddChannelMemberListener;
import com.yutak.im.interfaces.IChannelMemberInfoListener;
import com.yutak.im.interfaces.IGetChannelMemberInfo;
import com.yutak.im.interfaces.IGetChannelMemberList;
import com.yutak.im.interfaces.IGetChannelMemberListResult;
import com.yutak.im.interfaces.IRefreshChannelMember;
import com.yutak.im.interfaces.IRemoveChannelMember;
import com.yutak.im.interfaces.ISyncChannelMembers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChannelMembersManager extends BaseManager {
    private ChannelMembersManager() {
    }

    private static class ChannelMembersManagerBinder {
        static final ChannelMembersManager channelMembersManager = new ChannelMembersManager();
    }

    public static ChannelMembersManager getInstance() {
        return ChannelMembersManagerBinder.channelMembersManager;
    }
    private ConcurrentHashMap<String, IRefreshChannelMember> refreshMemberMap;
    private ConcurrentHashMap<String, IRemoveChannelMember> removeChannelMemberMap;//监听添加频道成员
    private ConcurrentHashMap<String, IAddChannelMemberListener> addChannelMemberMap;
    private ISyncChannelMembers syncChannelMembers;
    //获取频道成员监听
    private IGetChannelMemberInfo iGetChannelMemberInfoListener;
    private IGetChannelMemberList iGetChannelMemberList;

    public long getMaxVersion(String channelID, byte channelType) {
        return ChannelMemberDBManager.getInstance().queryMaxVersion(channelID, channelType);
    }

    public List<YutakChannelMember> getRobotMembers(String channelID, byte channelType) {
        return ChannelMemberDBManager.getInstance().queryRobotMembers(channelID, channelType);
    }

    public List<YutakChannelMember> getWithRole(String channelID, byte channelType, int role) {
        return ChannelMemberDBManager.getInstance().queryWithRole(channelID, channelType, role);
    }

    /**
     * 批量保存成员
     *
     * @param list 成员数据
     */
    public synchronized void save(List<YutakChannelMember> list) {
        if (list == null || list.size() == 0) return;
        new Thread(() -> {
            String channelID = list.get(0).channelID;
            byte channelType = list.get(0).channelType;

            List<YutakChannelMember> addList = new ArrayList<>();
            List<YutakChannelMember> deleteList = new ArrayList<>();
            List<YutakChannelMember> updateList = new ArrayList<>();

            List<YutakChannelMember> existList = new ArrayList<>();
            List<String> uidList = new ArrayList<>();
            for (YutakChannelMember channelMember : list) {
                if (uidList.size() == 200) {
                    List<YutakChannelMember> tempList = ChannelMemberDBManager.getInstance().queryWithUIDs(channelMember.channelID, channelMember.channelType, uidList);
                    if (tempList != null && tempList.size() > 0)
                        existList.addAll(tempList);
                    uidList.clear();
                }
                uidList.add(channelMember.memberUID);
            }

            if (uidList.size() > 0) {
                List<YutakChannelMember> tempList = ChannelMemberDBManager.getInstance().queryWithUIDs(channelID, channelType, uidList);
                if (tempList != null && tempList.size() > 0)
                    existList.addAll(tempList);
                uidList.clear();
            }

            for (YutakChannelMember channelMember : list) {
                boolean isNewMember = true;
                for (int i = 0, size = existList.size(); i < size; i++) {
                    if (channelMember.memberUID.equals(existList.get(i).memberUID)) {
                        isNewMember = false;
                        if (channelMember.isDeleted == 1) {
                            deleteList.add(channelMember);
                        } else {
                            if (existList.get(i).isDeleted == 1) {
                                isNewMember = true;
                            } else
                                updateList.add(channelMember);
                        }
                        break;
                    }
                }
                if (isNewMember) {
                    addList.add(channelMember);
                }
            }

            // 先保存或修改成员
            ChannelMemberDBManager.getInstance().insertMembers(list, existList);

            if (addList.size() > 0) {
                setOnAddChannelMember(addList);
            }
            if (deleteList.size() > 0)
                setOnRemoveChannelMember(deleteList);

            if (updateList.size() > 0) {
                for (int i = 0, size = updateList.size(); i < size; i++) {
                    setRefreshChannelMember(updateList.get(i), i == updateList.size() - 1);
                }
            }
        }).start();

    }

    /**
     * 批量移除频道成员
     *
     * @param list 频道成员
     */
    public void delete(List<YutakChannelMember> list) {
        runOnMainThread(() -> ChannelMemberDBManager.getInstance().deleteMembers(list));
    }

    /**
     * 通过状态查询频道成员
     *
     * @param channelId   频道ID
     * @param channelType 频道类型
     * @param status      状态
     * @return List<>
     */
    public List<YutakChannelMember> getWithStatus(String channelId, byte channelType, int status) {
        return ChannelMemberDBManager.getInstance().queryWithStatus(channelId, channelType, status);
    }

    /**
     * 修改频道成员备注
     *
     * @param channelID   频道ID
     * @param channelType 频道类型
     * @param uid         用户ID
     * @param remarkName  备注
     */
    public boolean updateRemarkName(String channelID, byte channelType, String uid, String remarkName) {
        return ChannelMemberDBManager.getInstance().updateWithField(channelID, channelType, uid, DB.ChannelMembersColumns.member_remark, remarkName);
    }

    /**
     * 修改频道成员名称
     *
     * @param channelID   频道ID
     * @param channelType 频道类型
     * @param uid         用户ID
     * @param name        名称
     */
    public boolean updateMemberName(String channelID, byte channelType, String uid, String name) {
        return ChannelMemberDBManager.getInstance().updateWithField(channelID, channelType, uid, DB.ChannelMembersColumns.member_name, name);
    }

    /**
     * 修改频道成员状态
     *
     * @param channelId   频道ID
     * @param channelType 频道类型
     * @param uid         用户ID
     * @param status      状态
     */
    public boolean updateMemberStatus(String channelId, byte channelType, String uid, int status) {
        return ChannelMemberDBManager.getInstance().updateWithField(channelId, channelType, uid, DB.ChannelMembersColumns.status, String.valueOf(status));
    }

    public void addOnGetChannelMembersListener(IGetChannelMemberList iGetChannelMemberList) {
        this.iGetChannelMemberList = iGetChannelMemberList;
    }

    public void getWithPageOrSearch(String channelID, byte channelType, String searchKey, int page, int limit, @NonNull IGetChannelMemberListResult iGetChannelMemberListResult) {
        List<YutakChannelMember> list;
        if (TextUtils.isEmpty(searchKey)) {
            list = getMembersWithPage(channelID, channelType, page, limit);
        } else {
            list = searchMembers(channelID, channelType, searchKey, page, limit);
        }

        iGetChannelMemberListResult.onResult(list, false);
        int groupType = 0;
        YutakChannel channel = YutakIM.get().getChannelManager().getChannel(channelID, channelType);
        if (channel != null && channel.remoteExtraMap != null && channel.remoteExtraMap.containsKey(YutakChannelExtras.groupType)) {
            Object groupTypeObject = channel.remoteExtraMap.get(YutakChannelExtras.groupType);
            if (groupTypeObject instanceof Integer) {
                groupType = (int) groupTypeObject;
            }
        }
        if (iGetChannelMemberList != null && groupType == 1) {
            iGetChannelMemberList.request(channelID, channelType, searchKey, page, limit, list1 -> {
                iGetChannelMemberListResult.onResult(list1, true);
                if (list1 != null && list1.size() > 0) {
                    ChannelMemberDBManager.getInstance().deleteWithChannel(channelID, channelType);
                    save(list1);
                }
            });
        }
    }

    public void addOnGetChannelMemberListener(IGetChannelMemberInfo iGetChannelMemberInfoListener) {
        this.iGetChannelMemberInfoListener = iGetChannelMemberInfoListener;
    }

    public void refreshChannelMemberCache(YutakChannelMember channelMember) {
        if (channelMember == null) return;
        List<YutakChannelMember> list = new ArrayList<>();
        list.add(channelMember);
        ChannelMemberDBManager.getInstance().insertMembers(list);
    }

    /**
     * 添加加入频道成员监听
     *
     * @param listener 回调
     */
    public void addOnAddChannelMemberListener(String key, IAddChannelMemberListener listener) {
        if (TextUtils.isEmpty(key) || listener == null) return;
        if (addChannelMemberMap == null)
            addChannelMemberMap = new ConcurrentHashMap<>();
        addChannelMemberMap.put(key, listener);
    }

    public void removeAddChannelMemberListener(String key) {
        if (TextUtils.isEmpty(key) || addChannelMemberMap == null) return;
        addChannelMemberMap.remove(key);
    }

    public void setOnAddChannelMember(List<YutakChannelMember> list) {
        if (addChannelMemberMap != null) {
            runOnMainThread(() -> {
                for (Map.Entry<String, IAddChannelMemberListener> entry : addChannelMemberMap.entrySet()) {
                    entry.getValue().onAddMembers(list);
                }
            });
        }
    }

    /**
     * 获取频道成员信息
     *
     * @param channelId                  频道ID
     * @param uid                        成员ID
     * @param iChannelMemberInfoListener 回调
     */
    public YutakChannelMember getMember(String channelId, byte channelType, String uid, IChannelMemberInfoListener iChannelMemberInfoListener) {
        if (iGetChannelMemberInfoListener != null && !TextUtils.isEmpty(channelId) && !TextUtils.isEmpty(uid) && iChannelMemberInfoListener != null) {
            return iGetChannelMemberInfoListener.onResult(channelId, channelType, uid, iChannelMemberInfoListener);
        } else return null;
    }

    public YutakChannelMember getMember(String channelID, byte channelType, String uid) {
        return ChannelMemberDBManager.getInstance().query(channelID, channelType, uid);
    }

    public List<YutakChannelMember> getMembers(String channelID, byte channelType) {
        return ChannelMemberDBManager.getInstance().query(channelID, channelType);
    }

    private List<YutakChannelMember> searchMembers(String channelId, byte channelType, String keyword, int page, int size) {
        return ChannelMemberDBManager.getInstance().search(channelId, channelType, keyword, page, size);
    }

    private List<YutakChannelMember> getMembersWithPage(String channelId, byte channelType, int page, int size) {
        return ChannelMemberDBManager.getInstance().queryWithPage(channelId, channelType, page, size);
    }

    public List<YutakChannelMember> getDeletedMembers(String channelID, byte channelType) {
        return ChannelMemberDBManager.getInstance().queryDeleted(channelID, channelType);
    }

    //成员数量
    public int getMemberCount(String channelID, byte channelType) {
        return ChannelMemberDBManager.getInstance().queryCount(channelID, channelType);
    }

    public void addOnRefreshChannelMemberInfo(String key, IRefreshChannelMember iRefreshChannelMemberListener) {
        if (TextUtils.isEmpty(key) || iRefreshChannelMemberListener == null) return;
        if (refreshMemberMap == null)
            refreshMemberMap = new ConcurrentHashMap<>();
        refreshMemberMap.put(key, iRefreshChannelMemberListener);
    }

    public void removeRefreshChannelMemberInfo(String key) {
        if (TextUtils.isEmpty(key) || refreshMemberMap == null) return;
        refreshMemberMap.remove(key);
    }

    public void setRefreshChannelMember(YutakChannelMember channelMember, boolean isEnd) {
        if (refreshMemberMap != null && channelMember != null) {
            runOnMainThread(() -> {
                for (Map.Entry<String, IRefreshChannelMember> entry : refreshMemberMap.entrySet()) {
                    entry.getValue().onRefresh(channelMember, isEnd);
                }
            });
        }
    }

    public void addOnRemoveChannelMemberListener(String key, IRemoveChannelMember listener) {
        if (listener == null || TextUtils.isEmpty(key)) return;
        if (removeChannelMemberMap == null) removeChannelMemberMap = new ConcurrentHashMap<>();
        removeChannelMemberMap.put(key, listener);
    }

    public void removeRemoveChannelMemberListener(String key) {
        if (TextUtils.isEmpty(key) || removeChannelMemberMap == null) return;
        removeChannelMemberMap.remove(key);
    }

    public void setOnRemoveChannelMember(List<YutakChannelMember> list) {
        if (removeChannelMemberMap != null) {
            runOnMainThread(() -> {
                for (Map.Entry<String, IRemoveChannelMember> entry : removeChannelMemberMap.entrySet()) {
                    entry.getValue().onRemoveMembers(list);
                }
            });
        }
    }

    public void addOnSyncChannelMembers(ISyncChannelMembers syncChannelMembersListener) {
        this.syncChannelMembers = syncChannelMembersListener;
    }

    public void setOnSyncChannelMembers(String channelID, byte channelType) {
        if (syncChannelMembers != null) {
            runOnMainThread(() -> {
                syncChannelMembers.onSyncChannelMembers(channelID, channelType);
            });
        }
    }
}
