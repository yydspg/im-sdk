package com.yutak.im.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;

import com.yutak.im.kit.LogKit;

import java.util.Map;

public class DBHelper {

    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;

    public SQLiteDatabase getDb() {
        return mDb;
    }

    private volatile static DBHelper openHelper = null;
    // 数据库版本
    private final static int version = 1;
    private static String myDBName;
    private static String uid;
    public static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, myDBName, null, version);
            //不可忽略的 进行so库加载
            //TODO : sdk 升级，此api 被废弃！！，查看替代方案
//            SQLiteDatabase.loadLibs(context);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {

        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int arg1, int arg2) {
        }
    }
    public void close() {
        try {
            uid = "";
            if (mDb != null) {
                mDb.close();
                mDb = null;
            }
            myDBName = "";
            if (mDbHelper != null) {
                mDbHelper.close();
                mDbHelper = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void insertSql(String tab, ContentValues cv) {
        if (mDb == null) {
            return;
        }
        mDb.insertWithOnConflict(tab, "", cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public Cursor rawQuery(String sql) {
        if (mDb == null) {
            return null;
        }
        return mDb.rawQuery(sql, null);
    }

    public Cursor rawQuery(String sql, Object[] selectionArgs) {
        if (mDb == null) {
            return null;
        }
        return mDb.rawQuery(sql, selectionArgs);
    }

    public Cursor select(String table, String selection,
                         String[] selectionArgs,
                         String orderBy) {
        if (mDb == null) return null;
        Cursor cursor;
        try {
            cursor = mDb.query(table, null, selection, selectionArgs,
                    null, null, orderBy);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return cursor;
    }

    public long insert(String table, ContentValues cv) {
        if (mDb == null) return 0;
        long count = 0;
        try {
            //TODO : api 变了
            count = mDb.insert(table, String.valueOf(SQLiteDatabase.CONFLICT_REPLACE), cv);
//            count = mDb.insert(table, null, cv);
        } catch (Exception e) {
            StringBuilder fields = new StringBuilder();
            for (Map.Entry<String, Object> item : cv.valueSet()) {
                if (!TextUtils.isEmpty(fields)) {
                    fields.append(",");
                }
                fields.append(item.getKey()).append(":").append(item.getValue());
            }
            LogKit.get().e("数据库插入异常，插入表：" + table + "，字段信息：" + fields);
            e.printStackTrace();
        }
        return count;
    }

    public boolean delete(String tableName, String where, String[] whereValue) {
        if (mDb == null) return false;
        int count = mDb.delete(tableName, where, whereValue);
        return count > 0;
    }

    public int update(String table, String[] updateFields,
                      String[] updateValues, String where, String[] whereValue) {
        if (mDb == null) return 0;
        ContentValues cv = new ContentValues();
        for (int i = 0; i < updateFields.length; i++) {
            cv.put(updateFields[i], updateValues[i]);
        }
        int count = 0;
        try {
            count = mDb.update(table, cv, where, whereValue);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return count;
    }

    public boolean update(String tableName, ContentValues cv, String where,
                          String[] whereValue) {
        if (mDb == null) return false;
        boolean flag = false;
        try {
            flag = mDb.update(tableName, cv, where, whereValue) > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return flag;
    }

    public boolean update(String tableName, String whereClause,
                          ContentValues args) {
        if (mDb == null) return false;
        boolean flag = false;
        try {
            flag = mDb.update(tableName, args, whereClause, null) > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return flag;
    }

}
