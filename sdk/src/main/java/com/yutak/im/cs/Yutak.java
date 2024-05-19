package com.yutak.im.cs;

public interface Yutak {
     interface MsgType {
        //保留
         short Reserved = 0;
        //客户端请求连接到服务器(c2s)
         short CONNECT = 1;
        //服务端收到连接请求后确认的报文(s2c)
         short CONNACK = 2;
        //发送消息(c2s)
         short SEND = 3;
        //收到消息确认的报文(s2c)
         short SENDACK = 4;
        //收取消息(s2c)
         short RECVEIVED = 5;
        //收取消息确认(c2s)
         short REVACK = 6;
        //ping请求
         short PING = 7;
        //对ping请求的相应
         short PONG = 8;
        //请求断开连接
         short DISCONNECT = 9;
    }
    interface MsgContentType {
        //文本
         int TEXT = 1;
        //图片
         int IMAGE = 2;
        //GIF
         int GIF = 3;
        //语音
         int VOICE = 4;
        //视频
         int VIDEO = 5;
        //位置
         int LOCATION = 6;
        //名片
         int CARD = 7;
        //文件
         int FILE = 8;
        //红包
        @Deprecated
         int REDPACKET = 9;
        //转账
        @Deprecated
         int TRANSFER = 10;
        //合并转发消息
         int MULTIPLE_FORWARD = 11;
        //矢量贴图
         int VECTOR_STICKER = 12;
        //emoji 贴图
         int EMOJI_STICKER = 13;
        // content 格式错误
         int CONTENT_FORMAT_ERROR = 97;
        // signal 解密失败
         int SIGNAL_DECRYPT_ERROR = 98;
        //内部消息，无需存储到数据库
         int INSIDE_MSG = 99;
    }
    public interface ConnectReason {
         String ReasonAuthFail = "ReasonAuthFail";
         String ReasonConnectKick = "ReasonConnectKick";
         String NoNetwork = "NoNetwork";
         String Connecting = "Connecting";
         String SyncMsg = "SyncMsg";
         String ConnectSuccess = "ConnectSuccess";
    }
    public interface ConnectStatus {
        //失败
         int fail = 0;
        //登录或者发送消息回执返回状态成功
         int success = 1;
        //被踢（其他设备登录）
         int kicked = 2;
        //同步消息中
         int syncMsg = 3;
        //连接中
         int connecting = 4;
        //无网络
         int noNetwork = 5;
        // 同步完成
         int syncCompleted = 6;
    }
    interface SendMsgResult {
        //不在白名单内
         int not_on_white_list = 13;
        //黑名单
         int black_list = 4;
        //不是好友或不在群内
         int no_relation = 3;
        //发送失败
         int send_fail = 2;
        //成功
         int send_success = 1;
        //发送中
         int send_loading = 0;
    }

}
