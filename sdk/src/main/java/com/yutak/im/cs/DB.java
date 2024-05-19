package com.yutak.im.cs;

public interface DB {
    
        interface TABLE{
             String message = "message";
             String messageReaction = "message_reaction";
             String messageExtra = "message_extra";
             String conversation = "conversation";
             String conversationExtra = "conversation_extra";
             String channel = "channel";
             String channelMembers = "channel_members";
             String reminders = "reminders";
             String robot = "robot";
             String robotMenu = "robot_menu";
        }
        //频道db字段
        interface ChannelColumns {
            //自增ID
             String id = "id";
            //频道ID
             String channel_id = "channel_id";
            //频道类型
             String channel_type = "channel_type";
            //频道名称
             String channel_name = "channel_name";
            //频道备注(频道的备注名称，个人的话就是个人备注，群的话就是群别名)
             String channel_remark = "channel_remark";
            //是否置顶
             String top = "top";
            //免打扰
             String mute = "mute";
            //是否保存在通讯录
             String save = "save";
            //频道状态
             String status = "status";
            //是否禁言
             String forbidden = "forbidden";
            //是否开启邀请确认
             String invite = "invite";
            //是否已关注 0.未关注（陌生人） 1.已关注（好友）
             String follow = "follow";
            //是否删除
             String is_deleted = "is_deleted";
            //是否显示频道名称
             String show_nick = "show_nick";
            //创建时间
             String created_at = "created_at";
            //修改时间
             String updated_at = "updated_at";
            //版本
             String version = "version";
            //扩展字段
             String localExtra = "extra";
            //头像
             String avatar = "avatar";
            //是否在线
             String online = "online";
            //最后一次离线时间
             String last_offline = "last_offline";
            //分类
             String category = "category";
            //是否回执消息
             String receipt = "receipt";
            // 机器人0.否1.是
             String robot = "robot";
             String username = "username";
             String avatar_cache_key = "avatar_cache_key";
             String remote_extra = "remote_extra";
             String flame = "flame";
             String flame_second = "flame_second";
             String device_flag = "device_flag";
             String parent_channel_id = "parent_channel_id";
             String parent_channel_type = "parent_channel_type";
        }

        //频道成员db字段
        interface ChannelMembersColumns {
            //自增ID
             String id = "id";
            //成员状态
             String status = "status";
            //频道id
             String channel_id = "channel_id";
            //频道类型
             String channel_type = "channel_type";
            // 邀请者uid
             String member_invite_uid = "member_invite_uid";
            //成员id
             String member_uid = "member_uid";
            //成员名称
             String member_name = "member_name";
            //成员头像
             String member_avatar = "member_avatar";
            //成员备注
             String member_remark = "member_remark";
            //成员角色
             String role = "role";
            //是否删除
             String is_deleted = "is_deleted";
            //创建时间
             String created_at = "created_at";
            //修改时间
             String updated_at = "updated_at";
            //版本
             String version = "version";
            // 机器人0.否1.是
             String robot = "robot";
            // 禁言到期时间
             String forbidden_expiration_time = "forbidden_expiration_time";
            //扩展字段
             String extra = "extra";
             String memberAvatarCacheKey = "member_avatar_cache_key";
        }

        //消息db字段
        interface MessageColumns {
            //服务器消息ID(全局唯一，无序)
             String message_id = "message_id";
            //服务器消息序号(有序递增)
             String message_seq = "message_seq";
            //客户端序号
             String client_seq = "client_seq";
            //消息时间10位时间戳
             String timestamp = "timestamp";
            //消息来源发送者
             String from_uid = "from_uid";
            //频道id
             String channel_id = "channel_id";
            //频道类型
             String channel_type = "channel_type";
            //消息正文类型
             String type = "type";
            //消息内容Json
             String content = "content";
            //发送状态
             String status = "status";
            //语音是否已读
             String voice_status = "voice_status";
            //创建时间
             String created_at = "created_at";
            //修改时间
             String updated_at = "updated_at";
            //扩展字段
             String extra = "extra";
            //搜索的关键字 如：[红包]
             String searchable_word = "searchable_word";
            //客户端唯一ID
             String client_msg_no = "client_msg_no";
            //消息是否删除
             String is_deleted = "is_deleted";
            //排序编号
             String order_seq = "order_seq";
            //消息设置
             String setting = "setting";
            // 是否开启阅后即焚
             String flame = "flame";
            // 阅后即焚秒数
             String flame_second = "flame_second";
            // 是否已查看 0.未查看 1.已查看 （这个字段跟已读的区别在于是真正的查看了消息内容，比如图片消息 已读是列表滑动到图片消息位置就算已读，viewed是表示点开图片才算已查看，语音消息类似）
             String viewed = "viewed";
            // 查看时间戳
             String viewed_at = "viewed_at";
            // 话题ID
             String topic_id = "topic_id";
             String expire_time = "expire_time";
             String expire_timestamp = "expire_timestamp";
        }

        //最近会话db字段
        interface CoverMessageColumns {
            //频道id
             String channel_id = "channel_id";
            //频道类型
             String channel_type = "channel_type";
            //最后一条消息id
             String last_msg_seq = "last_msg_seq";
            //最后一条消息时间
             String last_msg_timestamp = "last_msg_timestamp";
            //未读消息数量
             String unread_count = "unread_count";
            //扩展字段
             String extra = "extra";
            //客户端唯一ID
             String last_client_msg_no = "last_client_msg_no";
            //是否删除
             String is_deleted = "is_deleted";
            //版本
             String version = "version";
            // 父channelID
             String parent_channel_id = "parent_channel_id";
            // 父channelType
             String parent_channel_type = "parent_channel_type";
        }
}
    
