package com.yutak.im.message;


import android.text.TextUtils;
import android.util.Log;

import com.yutak.im.YutakApplication;
import com.yutak.im.kit.LogKit;

import org.xsocket.connection.IConnectExceptionHandler;
import org.xsocket.connection.IConnectHandler;
import org.xsocket.connection.IConnectionTimeoutHandler;
import org.xsocket.connection.IDataHandler;
import org.xsocket.connection.IDisconnectHandler;
import org.xsocket.connection.IIdleTimeoutHandler;
import org.xsocket.connection.INonBlockingConnection;

import java.io.IOException;
import java.nio.BufferUnderflowException;

/**
 * 2020-12-18 10:28
 * 连接客户端
 */
class ConnectionClient implements IDataHandler, IConnectHandler,
        IDisconnectHandler, IConnectExceptionHandler,
        IConnectionTimeoutHandler, IIdleTimeoutHandler {

    private boolean isConnectSuccess;

    ConnectionClient() {
        isConnectSuccess = false;
    }

    @Override
    public boolean onConnectException(INonBlockingConnection iNonBlockingConnection, IOException e) {
        LogKit.get().e("连接异常");
        YutakConnection.getInstance().forcedReconnection();
        close(iNonBlockingConnection);
        return true;
    }

    @Override
    public boolean onConnect(INonBlockingConnection iNonBlockingConnection) throws BufferUnderflowException {
        if (YutakConnection.getInstance().connection == null) {
            Log.e("连接信息为空", "--->");
        }
        try {
            if (YutakConnection.getInstance().connection != null && iNonBlockingConnection != null) {
                if (!YutakConnection.getInstance().connection.getId().equals(iNonBlockingConnection.getId())) {
                    close(iNonBlockingConnection);
                    YutakConnection.getInstance().forcedReconnection();
                } else {
                    //连接成功
                    isConnectSuccess = true;
                    LogKit.get().e("连接成功");
                    YutakConnection.getInstance().sendConnectMsg();
                }
            } else {
                close(iNonBlockingConnection);
                LogKit.get().e("连接成功连接对象为空");
                YutakConnection.getInstance().forcedReconnection();
            }
        } catch (Exception ignored) {
        }
        return false;

    }

    @Override
    public boolean onConnectionTimeout(INonBlockingConnection iNonBlockingConnection) {
        if (!isConnectSuccess) {
            LogKit.get().e("连接超时");
            YutakConnection.getInstance().forcedReconnection();
        }
        return true;
    }

    @Override
    public boolean onData(INonBlockingConnection iNonBlockingConnection) throws BufferUnderflowException {
        Object id = iNonBlockingConnection.getAttachment();
        if (id instanceof String) {
            if (id.toString().startsWith("close")) {
                return true;
            }
            if (!TextUtils.isEmpty(YutakConnection.getInstance().socketSingleID) && !YutakConnection.getInstance().socketSingleID.equals(id)) {
                LogKit.get().e("收到的消息ID和连接的ID对应不上---");
                try {
                    iNonBlockingConnection.close();
                    if (YutakConnection.getInstance().connection != null) {
                        YutakConnection.getInstance().connection.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (YutakApplication.get().isCanConnect) {
                    YutakConnection.getInstance().forcedReconnection();
                }
                return true;
            }
        }
        int available_len;
        int bufLen = 102400;
        try {
            available_len = iNonBlockingConnection.available();
            if (available_len == -1) {
                return true;
            }
            int readCount = available_len / bufLen;
            if (available_len % bufLen != 0) {
                readCount++;
            }

            for (int i = 0; i < readCount; i++) {
                int readLen = bufLen;
                if (i == readCount - 1) {
                    if (available_len % bufLen != 0) {
                        readLen = available_len % bufLen;
                    }
                }
                byte[] buffBytes = iNonBlockingConnection.readBytesByLength(readLen);
                if (buffBytes.length > 0) {
                    YutakConnection.getInstance().receivedData(buffBytes);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
            LogKit.get().e("处理接受到到数据异常:" + e.getMessage());
        }
        return true;
    }

    @Override
    public boolean onDisconnect(INonBlockingConnection iNonBlockingConnection) {
        LogKit.get().e("连接断开");
        try {
            if (iNonBlockingConnection != null && !TextUtils.isEmpty(iNonBlockingConnection.getId()) && iNonBlockingConnection.getAttachment() != null) {
                String id = iNonBlockingConnection.getId();
                Object attachmentObject = iNonBlockingConnection.getAttachment();
                if (attachmentObject instanceof String) {
                    String att = (String) attachmentObject;
                    String attStr = "close" + id;
                    if (att.equals(attStr)) {
                        return true;
                    }
                }
            }
            if (YutakApplication.get().isCanConnect) {
                YutakConnection.getInstance().forcedReconnection();
            } else {
                LogKit.get().e("不能重连-->");
            }
            close(iNonBlockingConnection);
        } catch (Exception ignored) {

        }

        return true;
    }

    @Override
    public boolean onIdleTimeout(INonBlockingConnection iNonBlockingConnection) {
        if (!isConnectSuccess) {
            LogKit.get().e("Idle连接超时");
            YutakConnection.getInstance().forcedReconnection();
            close(iNonBlockingConnection);
        }
        return true;
    }

    private void close(INonBlockingConnection iNonBlockingConnection) {
        try {
            if (iNonBlockingConnection != null)
                iNonBlockingConnection.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
