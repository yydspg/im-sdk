package com.yutak.im.interfaces;


import com.yutak.im.domain.YutakMsg;

/**
 * 2020-08-02 00:29
 * 上传聊天附件
 */
public interface IUploadAttachmentListener {
    void onUploadAttachmentListener(YutakMsg msg, IUploadAttacResultListener attacResultListener);
}
