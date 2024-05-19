package com.yutak.im.db;

import android.database.Cursor;

//在Android开发中，android.database.Cursor类是一个非常重要的类，它主要用于遍历查询结果。
// 当你对SQLite数据库或者其他数据存储执行查询操作时，查询的结果会被封装到一个Cursor对象中。Cursor可以被看作是一个数据集合的迭代器，通过它可以逐行访问查询得到的数据记录。
public class YutakCursor {
    public static String readString(Cursor cursor, String key) {
        try {
            int index = cursor.getColumnIndexOrThrow(key);
            return cursor.getString(index);
        } catch (IllegalArgumentException e) {
            return "";
        }
    }

    public static int readInt(Cursor cursor, String key) {
        try {
            return cursor.getInt(cursor.getColumnIndexOrThrow(key));
        } catch (IllegalArgumentException e) {
            return 0;
        }
    }

    public static long readLong(Cursor cursor, String key) {
        try {
            return cursor.getLong(cursor.getColumnIndexOrThrow(key));
        } catch (IllegalArgumentException e) {
            return 0L;
        }

    }

    public static byte readByte(Cursor cursor, String key) {
        try {
            int v = cursor.getInt(cursor.getColumnIndexOrThrow(key));
            return (byte) v;
        } catch (IllegalArgumentException e) {
            return 0;
        }
    }

    public static byte[] readBlob(Cursor cursor, String key) {
        try {
            return cursor.getBlob(cursor.getColumnIndexOrThrow(key));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    // build safe sql
    public static String getPlaceholders(int count) {
        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i != 0) {
                placeholders.append(", ");
            }
            placeholders.append("?");
        }
        return placeholders.toString();
    }
}
