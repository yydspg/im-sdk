package com.yutak.im.manager;

import android.text.TextUtils;

import com.yutak.im.cs.DB;
import com.yutak.im.db.ChannelDBManager;
import com.yutak.im.domain.YutakChannel;
import com.yutak.im.domain.YutakChannelSearchResult;
import com.yutak.im.interfaces.IChannelInfoListener;
import com.yutak.im.interfaces.IGetChannelInfo;
import com.yutak.im.interfaces.IRefreshChannel;
import com.yutak.im.interfaces.IRefreshChannelAvatar;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChannelManager extends BaseManager{

    private ChannelManager() {
    }

    private static class ChannelManagerBinder {
        static final ChannelManager channelManager = new ChannelManager();
    }

    public static ChannelManager getInstance() {
        return ChannelManagerBinder.channelManager;
    }
    // get channel avatar
    private IRefreshChannelAvatar iRefreshChannelAvatar;
    // interface which get channel info
    private IGetChannelInfo iGetChannelInfo;
    // sync channel list
    private final List<YutakChannel> YutakChannelList = Collections.synchronizedList(new ArrayList<>());
    private ConcurrentHashMap<String, IRefreshChannel> refreshChannelMap;
    
    // get channel
    public synchronized YutakChannel getChannel(String channelID,byte channelType) {
        if(TextUtils.isEmpty(channelID)) return null;
        YutakChannel YutakChannel = null;
        for (YutakChannel channel : YutakChannelList) {
            if (channel != null && channel.channelID.equals(channelID) && channel.channelType == channelType) {
                YutakChannel = channel;
                break;
            }
        }
        // not in memory
        if (YutakChannel == null) {
            // load from db
            YutakChannel = ChannelDBManager.getInstance().query(channelID, channelType);
            if (YutakChannel != null) {
                // add in memory
                YutakChannelList.add(YutakChannel);
            }
        }
        return YutakChannel;
    }
    public YutakChannel getChannel(String channelId, byte channelType, IChannelInfoListener iChannelInfoListener) {
        if (this.iGetChannelInfo != null && !TextUtils.isEmpty(channelId) && iChannelInfoListener != null) {
            return iGetChannelInfo.onGetChannelInfo(channelId, channelType, iChannelInfoListener);
        } else return null;
    }
    // refresh channel info from remote
    public void fetchChannelInfo(String channelID, byte channelType) {
        if (TextUtils.isEmpty(channelID)) return;
        YutakChannel channel = getChannel(channelID, channelType, c -> {
            if (c != null)
                saveOrUpdateChannel(c);
        });
        if (channel != null) {
            saveOrUpdateChannel(channel);
        }
    }
    public void saveOrUpdateChannel(YutakChannel channel) {
        if (channel == null) return;
        //memory
        updateChannel(channel);
        setRefreshChannel(channel, true);
        ChannelDBManager.getInstance().insertOrUpdate(channel);
    }
    // update channel info in list
    private void updateChannel(YutakChannel channel) {
        if (channel == null) return;
        boolean isAdd = true;
        for (int i = 0, size = YutakChannelList.size(); i < size; i++) {
            if (YutakChannelList.get(i).channelID.equals(channel.channelID) && YutakChannelList.get(i).channelType == channel.channelType) {
                isAdd = false;
                YutakChannelList.get(i).forbidden = channel.forbidden;
                YutakChannelList.get(i).channelName = channel.channelName;
                YutakChannelList.get(i).avatar = channel.avatar;
                YutakChannelList.get(i).category = channel.category;
                YutakChannelList.get(i).lastOffline = channel.lastOffline;
                YutakChannelList.get(i).online = channel.online;
                YutakChannelList.get(i).follow = channel.follow;
                YutakChannelList.get(i).top = channel.top;
                YutakChannelList.get(i).channelRemark = channel.channelRemark;
                YutakChannelList.get(i).status = channel.status;
                YutakChannelList.get(i).version = channel.version;
                YutakChannelList.get(i).invite = channel.invite;
                YutakChannelList.get(i).localExtra = channel.localExtra;
                YutakChannelList.get(i).mute = channel.mute;
                YutakChannelList.get(i).save = channel.save;
                YutakChannelList.get(i).showNick = channel.showNick;
                YutakChannelList.get(i).isDeleted = channel.isDeleted;
                YutakChannelList.get(i).receipt = channel.receipt;
                YutakChannelList.get(i).robot = channel.robot;
                YutakChannelList.get(i).flameSecond = channel.flameSecond;
                YutakChannelList.get(i).flame = channel.flame;
                YutakChannelList.get(i).deviceFlag = channel.deviceFlag;
                YutakChannelList.get(i).parentChannelID = channel.parentChannelID;
                YutakChannelList.get(i).parentChannelType = channel.parentChannelType;
                YutakChannelList.get(i).avatarCacheKey = channel.avatarCacheKey;
                YutakChannelList.get(i).remoteExtraMap = channel.remoteExtraMap;
                break;
            }
        }
        // not in list before,so add current this
        if (isAdd) {
            YutakChannelList.add(channel);
        }
    }
    private void updateChannel(String channelID, byte channelType, String key, Object value) {
        if (TextUtils.isEmpty(channelID) || TextUtils.isEmpty(key)) return;
        for (int i = 0, size = YutakChannelList.size(); i < size; i++) {
            if (YutakChannelList.get(i).channelID.equals(channelID) && YutakChannelList.get(i).channelType == channelType) {
                switch (key) {
                    case DB.ChannelColumns.avatar_cache_key:
                        YutakChannelList.get(i).avatarCacheKey = (String) value;
                        break;
                    case DB.ChannelColumns.remote_extra:
                        YutakChannelList.get(i).remoteExtraMap = (HashMap<String, Object>) value;
                        break;
                    case DB.ChannelColumns.avatar:
                        YutakChannelList.get(i).avatar = (String) value;
                        break;
                    case DB.ChannelColumns.channel_remark:
                        YutakChannelList.get(i).channelRemark = (String) value;
                        break;
                    case DB.ChannelColumns.channel_name:
                        YutakChannelList.get(i).channelName = (String) value;
                        break;
                    case DB.ChannelColumns.follow:
                        YutakChannelList.get(i).follow = (int) value;
                        break;
                    case DB.ChannelColumns.forbidden:
                        YutakChannelList.get(i).forbidden = (int) value;
                        break;
                    case DB.ChannelColumns.invite:
                        YutakChannelList.get(i).invite = (int) value;
                        break;
                    case DB.ChannelColumns.is_deleted:
                        YutakChannelList.get(i).isDeleted = (int) value;
                        break;
                    case DB.ChannelColumns.last_offline:
                        YutakChannelList.get(i).lastOffline = (long) value;
                        break;
                    case DB.ChannelColumns.mute:
                        YutakChannelList.get(i).mute = (int) value;
                        break;
                    case DB.ChannelColumns.top:
                        YutakChannelList.get(i).top = (int) value;
                        break;
                    case DB.ChannelColumns.online:
                        YutakChannelList.get(i).online = (int) value;
                        break;
                    case DB.ChannelColumns.receipt:
                        YutakChannelList.get(i).receipt = (int) value;
                        break;
                    case DB.ChannelColumns.save:
                        YutakChannelList.get(i).save = (int) value;
                        break;
                    case DB.ChannelColumns.show_nick:
                        YutakChannelList.get(i).showNick = (int) value;
                        break;
                    case DB.ChannelColumns.status:
                        YutakChannelList.get(i).status = (int) value;
                        break;
                    case DB.ChannelColumns.username:
                        YutakChannelList.get(i).username = (String) value;
                        break;
                    case DB.ChannelColumns.flame:
                        YutakChannelList.get(i).flame = (int) value;
                        break;
                    case DB.ChannelColumns.flame_second:
                        YutakChannelList.get(i).flameSecond = (int) value;
                        break;
                    case DB.ChannelColumns.localExtra:
                        YutakChannelList.get(i).localExtra = (HashMap<String, Object>) value;
                        break;
                }
                setRefreshChannel(YutakChannelList.get(i), true);
                break;
            }
        }
    }
    /**
     * 添加或修改频道信息
     *
     * @param list 频道数据
     */
    public void saveOrUpdateChannels(List<YutakChannel> list) {
        if (list == null || list.size() == 0) return;
        // 先修改内存数据
        for (int i = 0, size = list.size(); i < size; i++) {
            updateChannel(list.get(i));
            setRefreshChannel(list.get(i), i == list.size() - 1);
        }
        ChannelDBManager.getInstance().insertChannels(list);
    }

    /**
     * 修改频道状态
     *
     * @param channelID   频道ID
     * @param channelType 频道类型
     * @param status      状态
     */
    public void updateStatus(String channelID, byte channelType, int status) {
        updateChannel(channelID, channelType, DB.ChannelColumns.status, status);
        ChannelDBManager.getInstance().updateWithField(channelID, channelType, DB.ChannelColumns.status, String.valueOf(status));
    }


    /**
     * 修改频道名称
     *
     * @param channelID   频道ID
     * @param channelType 频道类型
     * @param name        名称
     */
    public void updateName(String channelID, byte channelType, String name) {
        updateChannel(channelID, channelType, DB.ChannelColumns.channel_name, name);
        ChannelDBManager.getInstance().updateWithField(channelID, channelType, DB.ChannelColumns.channel_name, name);
    }

    /**
     * 获取频道信息
     *
     * @param channelType 频道类型
     * @param status      状态
     * @return List<YutakChannel>
     */
    public List<YutakChannel> getWithStatus(byte channelType, int status) {
        return ChannelDBManager.getInstance().queryWithStatus(channelType, status);
    }

    public List<YutakChannel> getWithChannelIdsAndChannelType(List<String> channelIds, byte channelType) {
        return ChannelDBManager.getInstance().queryWithChannelIdsAndChannelType(channelIds, channelType);
    }

    /**
     * 搜索频道
     *
     * @param keyword 关键字
     * @return List<YutakChannelSearchResult>
     */
    public List<YutakChannelSearchResult> search(String keyword) {
        return ChannelDBManager.getInstance().search(keyword);
    }

    /**
     * 搜索频道
     *
     * @param keyword     关键字
     * @param channelType 频道类型
     * @return List<YutakChannel>
     */
    public List<YutakChannel> searchWithChannelType(String keyword, byte channelType) {
        return ChannelDBManager.getInstance().searchWithChannelType(keyword, channelType);
    }

    public List<YutakChannel> searchWithChannelTypeAndFollow(String keyword, byte channelType, int follow) {
        return ChannelDBManager.getInstance().searchWithChannelTypeAndFollow(keyword, channelType, follow);
    }

    /**
     * 获取频道信息
     *
     * @param channelType 频道类型
     * @param follow      关注状态
     * @return List<YutakChannel>
     */
    public List<YutakChannel> getWithChannelTypeAndFollow(byte channelType, int follow) {
        return ChannelDBManager.getInstance().queryWithChannelTypeAndFollow(channelType, follow);
    }

    /**
     * 修改某个频道免打扰
     *
     * @param channelID   频道ID
     * @param channelType 频道类型
     * @param isMute      1：免打扰
     */
    public void updateMute(String channelID, byte channelType, int isMute) {
        updateChannel(channelID, channelType, DB.ChannelColumns.mute, isMute);
        ChannelDBManager.getInstance().updateWithField(channelID, channelType, DB.ChannelColumns.mute, String.valueOf(isMute));
    }

    /**
     * 修改备注信息
     *
     * @param channelID   频道ID
     * @param channelType 频道类型
     * @param hashExtra   扩展字段
     */
    public void updateLocalExtra(String channelID, byte channelType, HashMap<String, Object> hashExtra) {
        updateChannel(channelID, channelType, DB.ChannelColumns.localExtra, hashExtra);
        if (hashExtra != null) {
            JSONObject jsonObject = new JSONObject();
            for (String key : hashExtra.keySet()) {
                try {
                    jsonObject.put(key, hashExtra.get(key));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            ChannelDBManager.getInstance().updateWithField(channelID, channelType, DB.ChannelColumns.localExtra, jsonObject.toString());
        }
    }

    /**
     * 修改频道是否保存在通讯录
     *
     * @param channelID   频道ID
     * @param channelType 频道类型
     * @param isSave      1:保存
     */
    public void updateSave(String channelID, byte channelType, int isSave) {
        updateChannel(channelID, channelType, DB.ChannelColumns.save, isSave);
        ChannelDBManager.getInstance().updateWithField(channelID, channelType, DB.ChannelColumns.save, String.valueOf(isSave));
    }

    /**
     * 是否显示频道昵称
     *
     * @param channelID   频道ID
     * @param channelType 频道类型
     * @param showNick    1：显示频道昵称
     */
    public void updateShowNick(String channelID, byte channelType, int showNick) {
        updateChannel(channelID, channelType, DB.ChannelColumns.show_nick, showNick);
        ChannelDBManager.getInstance().updateWithField(channelID, channelType, DB.ChannelColumns.show_nick, String.valueOf(showNick));
    }

    /**
     * 修改某个频道是否置顶
     *
     * @param channelID   频道ID
     * @param channelType 频道类型
     * @param top         1：置顶
     */
    public void updateTop(String channelID, byte channelType, int top) {
        updateChannel(channelID, channelType, DB.ChannelColumns.top, top);
        ChannelDBManager.getInstance().updateWithField(channelID, channelType, DB.ChannelColumns.top, String.valueOf(top));
    }

    /**
     * 修改某个频道的备注
     *
     * @param channelID   频道ID
     * @param channelType 频道类型
     * @param remark      备注
     */
    public void updateRemark(String channelID, byte channelType, String remark) {
        updateChannel(channelID, channelType, DB.ChannelColumns.channel_remark, remark);
        ChannelDBManager.getInstance().updateWithField(channelID, channelType, DB.ChannelColumns.channel_remark, remark);
    }

    /**
     * 修改关注状态
     *
     * @param channelID   频道ID
     * @param channelType 频道类型
     * @param follow      w
     */
    public void updateFollow(String channelID, byte channelType, int follow) {
        updateChannel(channelID, channelType, DB.ChannelColumns.follow, follow);
        ChannelDBManager.getInstance().updateWithField(channelID, channelType, DB.ChannelColumns.follow, String.valueOf(follow));
    }

    /**
     * 通过follow和status查询频道
     *
     * @param channelType 频道类型
     * @param follow      是否关注 好友或陌生人
     * @param status      状态 正常或黑名单
     * @return list
     */
    public List<YutakChannel> getWithFollowAndStatus(byte channelType, int follow, int status) {
        return ChannelDBManager.getInstance().queryWithFollowAndStatus(channelType, follow, status);
    }

    public void updateAvatarCacheKey(String channelID, byte channelType, String avatar) {
        updateChannel(channelID, channelType, DB.ChannelColumns.avatar_cache_key, avatar);
        ChannelDBManager.getInstance().updateWithField(channelID, channelType, DB.ChannelColumns.avatar_cache_key, avatar);
    }

    public void addOnRefreshChannelAvatar(IRefreshChannelAvatar iRefreshChannelAvatar) {
        this.iRefreshChannelAvatar = iRefreshChannelAvatar;
    }

    public void setOnRefreshChannelAvatar(String channelID, byte channelType) {
        if (iRefreshChannelAvatar != null) {
            runOnMainThread(() -> iRefreshChannelAvatar.onRefreshChannelAvatar(channelID, channelType));
        }
    }

    public synchronized void clearARMCache() {
        YutakChannelList.clear();
    }

    // 刷新频道
    public void setRefreshChannel(YutakChannel channel, boolean isEnd) {
        if (refreshChannelMap != null) {
            runOnMainThread(() -> {
                updateChannel(channel);
                for (Map.Entry<String, IRefreshChannel> entry : refreshChannelMap.entrySet()) {
                    entry.getValue().onRefreshChannel(channel, isEnd);
                }
            });
        }
    }

    // 监听刷新普通
    public void addOnRefreshChannelInfo(String key, IRefreshChannel iRefreshChannelListener) {
        if (TextUtils.isEmpty(key)) return;
        if (refreshChannelMap == null) refreshChannelMap = new ConcurrentHashMap<>();
        if (iRefreshChannelListener != null)
            refreshChannelMap.put(key, iRefreshChannelListener);
    }

    // 移除频道刷新监听
    public void removeRefreshChannelInfo(String key) {
        if (TextUtils.isEmpty(key) || refreshChannelMap == null) return;
        refreshChannelMap.remove(key);
    }
}
