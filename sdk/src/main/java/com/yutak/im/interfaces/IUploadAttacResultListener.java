package com.yutak.im.interfaces;


import com.yutak.im.msgmodel.YutakMessageContent;

/**
 * 2019-11-10 16:12
 * 上传附件回掉
 */
public interface IUploadAttacResultListener {
    /**
     * 上传附件返回结果
     **/
    void onUploadResult(boolean isSuccess, YutakMessageContent messageContent);
}
