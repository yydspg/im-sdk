package com.yutak.im.kit;

import android.text.TextUtils;

import com.google.android.material.color.utilities.SchemeFidelity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;

public class FileKit {
    private FileKit() {}

    private static final FileKit fileKit = new FileKit();

    public static FileKit getFileKit() {
        return fileKit;
    }

    public boolean exists(String filePath) {
        File file = new File(filePath);
        return file.exists();
    }
    public boolean createDir(String path) {
        File file = new File(path);
        return file.mkdirs();
    }
    public void createFile(String path) {
        File file = new File(path);
        try {
             file.createNewFile();
        } catch (IOException e) {
//            throw new RuntimeException(e);
//TODO : 处理日志
        }
    }
    public String save(String oldPath,String channelId,byte channelType,String fileName) {
        if (TextUtils.isEmpty(channelId) || TextUtils.isEmpty(oldPath))  return "";

        File f = new File(oldPath);
        String name = f.getName();
        String suffix = name.substring(name.lastIndexOf(".") +1);
        //TODO : 这里功能没有实现完
        String filePath = String.format("%s%s%s","",channelType,channelId);
        if(!createDir(filePath)) return "";
        createFile(String.format("%s%s%s",filePath,name,suffix));
        copy(oldPath,filePath);
        return null;
    }
    public void copy(String oldPath,String newPath) {
        if(!exists(oldPath)) return;

        try {
            FileInputStream fi = new FileInputStream(oldPath);
            FileOutputStream fo = new FileOutputStream(newPath);
            byte[] buffer = new byte[1024];
            int byteRead;
            while (-1 != (byteRead = fi.read(buffer))) fo.write(byteRead);
            fi.close();
            fo.flush();
            fo.close();
        }catch (Exception e) {
        //TODO : log need to add，日志！！
        }
    }
}
