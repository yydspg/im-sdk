package com.yutak.im.db;

import android.content.ContentValues;
import android.text.TextUtils;

import com.yutak.im.cs.DB;
import com.yutak.im.domain.YutakChannel;
import com.yutak.im.domain.YutakChannelMember;
import com.yutak.im.domain.YutakConversationMsg;
import com.yutak.im.domain.YutakConversationMsgExtra;
import com.yutak.im.domain.YutakMsg;
import com.yutak.im.domain.YutakMsgExtra;
import com.yutak.im.domain.YutakMsgReaction;
import com.yutak.im.domain.YutakMsgSetting;
import com.yutak.im.domain.YutakReminder;
import com.yutak.im.kit.TypeKit;

import org.json.JSONException;
import org.json.JSONObject;

public class YutakSqlContentValue {
        static ContentValues getWithMsg(YutakMsg msg) {
            ContentValues contentValues = new ContentValues();
            if (msg == null) {
                return contentValues;
            }
            if (msg.setting == null) {
                msg.setting = new YutakMsgSetting();
            }
            contentValues.put(DB.MessageColumns.message_id, msg.messageID);
            contentValues.put(DB.MessageColumns.message_seq, msg.messageSeq);

            contentValues.put(DB.MessageColumns.order_seq, msg.orderSeq);
            contentValues.put(DB.MessageColumns.timestamp, msg.timestamp);
            contentValues.put(DB.MessageColumns.from_uid, msg.fromUID);
            contentValues.put(DB.MessageColumns.channel_id, msg.channelID);
            contentValues.put(DB.MessageColumns.channel_type, msg.channelType);
            contentValues.put(DB.MessageColumns.is_deleted, msg.isDeleted);
            contentValues.put(DB.MessageColumns.type, msg.type);
            contentValues.put(DB.MessageColumns.content, msg.content);
            contentValues.put(DB.MessageColumns.status, msg.status);
            contentValues.put(DB.MessageColumns.created_at, msg.createdAt);
            contentValues.put(DB.MessageColumns.updated_at, msg.updatedAt);
            contentValues.put(DB.MessageColumns.voice_status, msg.voiceStatus);
            contentValues.put(DB.MessageColumns.client_msg_no, msg.clientMsgNO);
            contentValues.put(DB.MessageColumns.flame, msg.flame);
            contentValues.put(DB.MessageColumns.flame_second, msg.flameSecond);
            contentValues.put(DB.MessageColumns.viewed, msg.viewed);
            contentValues.put(DB.MessageColumns.viewed_at, msg.viewedAt);
            contentValues.put(DB.MessageColumns.topic_id, msg.topicID);
            contentValues.put(DB.MessageColumns.expire_time, msg.expireTime);
            contentValues.put(DB.MessageColumns.expire_timestamp, msg.expireTimestamp);
            byte setting = TypeKit.getInstance().getMsgSetting(msg.setting);
            contentValues.put(DB.MessageColumns.setting, setting);
            if (msg.baseContentMsgModel != null) {
                contentValues.put(DB.MessageColumns.searchable_word, msg.baseContentMsgModel.getSearchableWord());
            }
            contentValues.put(DB.MessageColumns.extra, msg.getLocalMapExtraString());
            return contentValues;
        }

        static ContentValues getWithCoverMsg(YutakConversationMsg ConversationMsg, boolean isSync) {
            ContentValues contentValues = new ContentValues();
            if (ConversationMsg == null) {
                return contentValues;
            }
            contentValues.put(DB.CoverMessageColumns.channel_id, ConversationMsg.channelID);
            contentValues.put(DB.CoverMessageColumns.channel_type, ConversationMsg.channelType);
            contentValues.put(DB.CoverMessageColumns.last_client_msg_no, ConversationMsg.lastClientMsgNO);
            contentValues.put(DB.CoverMessageColumns.last_msg_timestamp, ConversationMsg.lastMsgTimestamp);
            contentValues.put(DB.CoverMessageColumns.last_msg_seq, ConversationMsg.lastMsgSeq);
            contentValues.put(DB.CoverMessageColumns.unread_count, ConversationMsg.unreadCount);
            contentValues.put(DB.CoverMessageColumns.parent_channel_id, ConversationMsg.parentChannelID);
            contentValues.put(DB.CoverMessageColumns.parent_channel_type, ConversationMsg.parentChannelType);
            if (isSync) {
                contentValues.put(DB.CoverMessageColumns.version, ConversationMsg.version);
            }
            contentValues.put(DB.CoverMessageColumns.is_deleted, ConversationMsg.isDeleted);
            contentValues.put(DB.CoverMessageColumns.extra, ConversationMsg.getLocalExtraString());
            return contentValues;
        }

        static ContentValues getWithChannel(YutakChannel channel) {
            ContentValues contentValues = new ContentValues();
            if (channel == null) {
                return contentValues;
            }
            contentValues.put(DB.ChannelColumns.channel_id, channel.channelID);
            contentValues.put(DB.ChannelColumns.channel_type, channel.channelType);
            contentValues.put(DB.ChannelColumns.channel_name, channel.channelName);
            contentValues.put(DB.ChannelColumns.channel_remark, channel.channelRemark);
            contentValues.put(DB.ChannelColumns.avatar, channel.avatar);
            contentValues.put(DB.ChannelColumns.top, channel.top);
            contentValues.put(DB.ChannelColumns.save, channel.save);
            contentValues.put(DB.ChannelColumns.mute, channel.mute);
            contentValues.put(DB.ChannelColumns.forbidden, channel.forbidden);
            contentValues.put(DB.ChannelColumns.invite, channel.invite);
            contentValues.put(DB.ChannelColumns.status, channel.status);
            contentValues.put(DB.ChannelColumns.is_deleted, channel.isDeleted);
            contentValues.put(DB.ChannelColumns.follow, channel.follow);
            contentValues.put(DB.ChannelColumns.version, channel.version);
            contentValues.put(DB.ChannelColumns.show_nick, channel.showNick);
            contentValues.put(DB.ChannelColumns.created_at, channel.createdAt);
            contentValues.put(DB.ChannelColumns.updated_at, channel.updatedAt);
            contentValues.put(DB.ChannelColumns.online, channel.online);
            contentValues.put(DB.ChannelColumns.last_offline, channel.lastOffline);
            contentValues.put(DB.ChannelColumns.receipt, channel.receipt);
            contentValues.put(DB.ChannelColumns.robot, channel.robot);
            contentValues.put(DB.ChannelColumns.category, channel.category);
            contentValues.put(DB.ChannelColumns.username, channel.username);
            contentValues.put(DB.ChannelColumns.avatar_cache_key, TextUtils.isEmpty(channel.avatarCacheKey) ? "" : channel.avatarCacheKey);
            contentValues.put(DB.ChannelColumns.flame, channel.flame);
            contentValues.put(DB.ChannelColumns.flame_second, channel.flameSecond);
            contentValues.put(DB.ChannelColumns.device_flag, channel.deviceFlag);
            contentValues.put(DB.ChannelColumns.parent_channel_id, channel.parentChannelID);
            contentValues.put(DB.ChannelColumns.parent_channel_type, channel.parentChannelType);

            if (channel.localExtra != null) {
                JSONObject jsonObject = new JSONObject(channel.localExtra);
                contentValues.put(DB.ChannelColumns.localExtra, jsonObject.toString());
            }
            if (channel.remoteExtraMap != null) {
                JSONObject jsonObject = new JSONObject(channel.remoteExtraMap);
                contentValues.put(DB.ChannelColumns.remote_extra, jsonObject.toString());
            }
            return contentValues;
        }

        static ContentValues getWithChannelMember(YutakChannelMember channelMember) {
            ContentValues contentValues = new ContentValues();
            if (channelMember == null) {
                return contentValues;
            }
            contentValues.put(DB.ChannelMembersColumns.channel_id, channelMember.channelID);
            contentValues.put(DB.ChannelMembersColumns.channel_type, channelMember.channelType);
            contentValues.put(DB.ChannelMembersColumns.member_invite_uid, channelMember.memberInviteUID);
            contentValues.put(DB.ChannelMembersColumns.member_uid, channelMember.memberUID);
            contentValues.put(DB.ChannelMembersColumns.member_name, channelMember.memberName);
            contentValues.put(DB.ChannelMembersColumns.member_remark, channelMember.memberRemark);
            contentValues.put(DB.ChannelMembersColumns.member_avatar, channelMember.memberAvatar);
            contentValues.put(DB.ChannelMembersColumns.memberAvatarCacheKey, TextUtils.isEmpty(channelMember.memberAvatarCacheKey) ? "" : channelMember.memberAvatarCacheKey);
            contentValues.put(DB.ChannelMembersColumns.role, channelMember.role);
            contentValues.put(DB.ChannelMembersColumns.is_deleted, channelMember.isDeleted);
            contentValues.put(DB.ChannelMembersColumns.version, channelMember.version);
            contentValues.put(DB.ChannelMembersColumns.status, channelMember.status);
            contentValues.put(DB.ChannelMembersColumns.robot, channelMember.robot);
            contentValues.put(DB.ChannelMembersColumns.forbidden_expiration_time, channelMember.forbiddenExpirationTime);
            contentValues.put(DB.ChannelMembersColumns.created_at, channelMember.createdAt);
            contentValues.put(DB.ChannelMembersColumns.updated_at, channelMember.updatedAt);

            if (channelMember.extraMap != null) {
                JSONObject jsonObject = new JSONObject(channelMember.extraMap);
                contentValues.put(DB.ChannelMembersColumns.extra, jsonObject.toString());
            }

            return contentValues;
        }

        static ContentValues getWithMsgReaction(YutakMsgReaction reaction) {
            ContentValues contentValues = new ContentValues();
            if (reaction == null) {
                return contentValues;
            }
            contentValues.put("channel_id", reaction.channelID);
            contentValues.put("channel_type", reaction.channelType);
            contentValues.put("message_id", reaction.messageID);
            contentValues.put("uid", reaction.uid);
            contentValues.put("name", reaction.name);
            contentValues.put("is_deleted", reaction.isDeleted);
            contentValues.put("seq", reaction.seq);
            contentValues.put("emoji", reaction.emoji);
            contentValues.put("created_at", reaction.createdAt);
            return contentValues;
        }

        static ContentValues getWithCVWithMsgExtra(YutakMsgExtra extra) {
            ContentValues cv = new ContentValues();
            cv.put("channel_id", extra.channelID);
            cv.put("channel_type", extra.channelType);
            cv.put("message_id", extra.messageID);
            cv.put("readed", extra.readed);
            cv.put("readed_count", extra.readedCount);
            cv.put("unread_count", extra.unreadCount);
            cv.put("revoke", extra.revoke);
            cv.put("revoker", extra.revoker);
            cv.put("extra_version", extra.extraVersion);
            cv.put("is_mutual_deleted", extra.isMutualDeleted);
            cv.put("content_edit", extra.contentEdit);
            cv.put("edited_at", extra.editedAt);
            cv.put("needUpload", extra.needUpload);
            return cv;
        }

        static ContentValues getWithCVWithReminder(YutakReminder reminder) {
            ContentValues cv = new ContentValues();
            cv.put("channel_id", reminder.channelID);
            cv.put("channel_type", reminder.channelType);
            cv.put("reminder_id", reminder.reminderID);
            cv.put("message_id", reminder.messageID);
            cv.put("message_seq", reminder.messageSeq);
            cv.put("uid", reminder.uid);
            cv.put("type", reminder.type);
            cv.put("is_locate", reminder.isLocate);
            cv.put("text", reminder.text);
            cv.put("version", reminder.version);
            cv.put("done", reminder.done);
            cv.put("needUpload", reminder.needUpload);
            cv.put("publisher", reminder.publisher);

            if (reminder.data != null) {
                JSONObject jsonObject = new JSONObject();
                for (Object key : reminder.data.keySet()) {
                    try {
                        jsonObject.put(String.valueOf(key), reminder.data.get(key));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                cv.put("data", jsonObject.toString());
            }
            return cv;
        }

        static ContentValues getWithCVWithExtra(YutakConversationMsgExtra extra) {
            ContentValues cv = new ContentValues();
            cv.put("channel_id", extra.channelID);
            cv.put("channel_type", extra.channelType);
            cv.put("browse_to", extra.browseTo);
            cv.put("keep_message_seq", extra.keepMessageSeq);
            cv.put("keep_offset_y", extra.keepOffsetY);
            cv.put("draft", extra.draft);
            cv.put("draft_updated_at", extra.version);
            cv.put("version", extra.version);
            return cv;
        }
}