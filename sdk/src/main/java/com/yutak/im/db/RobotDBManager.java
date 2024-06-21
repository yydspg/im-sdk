package com.yutak.im.db;


import static com.yutak.im.cs.DB.TABLE.robot;
import static com.yutak.im.cs.DB.TABLE.robotMenu;

import android.content.ContentValues;
import android.database.Cursor;

import com.yutak.im.YutakApplication;
import com.yutak.im.domain.YutakRobot;
import com.yutak.im.domain.YutakRobotMenu;

import java.util.ArrayList;
import java.util.List;

public class RobotDBManager {

    private RobotDBManager() {
    }

    private static class RobotDBManagerBinder {
        private final static RobotDBManager db = new RobotDBManager();
    }

    public static RobotDBManager getInstance() {
        return RobotDBManagerBinder.db;
    }

    public void insertOrUpdateMenus(List<YutakRobotMenu> list) {
        for (YutakRobotMenu menu : list) {
            if (isExitMenu(menu.robotID, menu.cmd)) {
                update(menu);
            } else {
                YutakApplication.get().getDbHelper().insert(robotMenu, getCV(menu));
            }
        }
    }

    public boolean isExitMenu(String robotID, String cmd) {
        boolean isExist = false;
        try (Cursor cursor = YutakApplication.get()
                .getDbHelper().select(robotMenu, "robot_id =? and cmd=?", new String[]{robotID, cmd}, null)) {
            if (cursor != null && cursor.moveToLast()) {
                isExist = true;
            }
        }
        return isExist;
    }

    private void update(YutakRobotMenu menu) {
        String[] updateKey = new String[3];
        String[] updateValue = new String[3];
        updateKey[0] = "type";
        updateValue[0] = menu.type;
        updateKey[1] = "remark";
        updateValue[1] = menu.remark;
        updateKey[2] = "updated_at";
        updateValue[2] = menu.updatedAT;
        String where = "robot_id=? and cmd=?";
        String[] whereValue = new String[2];
        whereValue[0] = menu.robotID;
        whereValue[1] = menu.cmd;
        YutakApplication.get().getDbHelper()
                .update(robotMenu, updateKey, updateValue, where, whereValue);
    }

    public void insertOrUpdateRobots(List<YutakRobot> list) {
        for (YutakRobot robot : list) {
            if (isExist(robot.robotID)) {
                update(robot);
            } else {
                insert(robot);
            }
        }
    }

    public boolean isExist(String robotID) {
        boolean isExist = false;
        try (Cursor cursor = YutakApplication.get()
                .getDbHelper().select(robot, "robot_id=?", new String[]{robotID}, null)) {
            if (cursor != null && cursor.moveToLast()) {
                isExist = true;
            }
        }
        return isExist;
    }

    private void update(YutakRobot yutakRobot) {
        String[] updateKey = new String[6];
        String[] updateValue = new String[6];
        updateKey[0] = "status";
        updateValue[0] = String.valueOf(yutakRobot.status);
        updateKey[1] = "version";
        updateValue[1] = String.valueOf(yutakRobot.version);
        updateKey[2] = "updated_at";
        updateValue[2] = String.valueOf(yutakRobot.updatedAT);
        updateKey[3] = "username";
        updateValue[3] = yutakRobot.username;
        updateKey[4] = "placeholder";
        updateValue[4] = yutakRobot.placeholder;
        updateKey[5] = "inline_on";
        updateValue[5] = String.valueOf(yutakRobot.inlineOn);

        String where = "robot_id=?";
        String[] whereValue = new String[1];
        whereValue[0] = yutakRobot.robotID;
        YutakApplication.get().getDbHelper()
                .update(robot, updateKey, updateValue, where, whereValue);

    }

    private void insert(YutakRobot robot1) {
        ContentValues cv = getCV(robot1);
        YutakApplication.get().getDbHelper().insert(robot, cv);
    }

    public void insertRobots(List<YutakRobot> list) {
        if (list == null || list.size() == 0) return;
        List<ContentValues> cvList = new ArrayList<>();
        for (YutakRobot robot : list) {
            cvList.add(getCV(robot));
        }
        try {
            YutakApplication.get().getDbHelper().getDb().beginTransaction();
            for (ContentValues cv : cvList) {
                YutakApplication.get().getDbHelper().insert(robot, cv);
            }
            YutakApplication.get().getDbHelper().getDb().setTransactionSuccessful();

        } finally {
            YutakApplication.get().getDbHelper().getDb().endTransaction();
        }
    }

    public YutakRobot query(String robotID) {
        YutakRobot yutakRobot = null;
        try (Cursor cursor = YutakApplication.get()
                .getDbHelper().select(robot, "robot_id =?", new String[]{robotID}, null)) {
            if (cursor == null) {
                return null;
            }
            if (cursor.moveToLast()) {
                yutakRobot = serializeRobot(cursor);
            }
        }
        return yutakRobot;
    }

    public YutakRobot queryWithUsername(String username) {
        YutakRobot yutakRobot = null;
        try (Cursor cursor = YutakApplication.get()
                .getDbHelper().select(robot, "username=?", new String[]{username}, null)) {
            if (cursor == null) {
                return null;
            }
            if (cursor.moveToLast()) {
                yutakRobot = serializeRobot(cursor);
            }
        }
        return yutakRobot;
    }

    public List<YutakRobot> queryRobots(List<String> robotIds) {
        List<YutakRobot> list = new ArrayList<>();
        try (Cursor cursor = YutakApplication.get().getDbHelper().select(robot, "robot_id in (" + YutakCursor.getPlaceholders(robotIds.size()) + ")", robotIds.toArray(new String[0]), null)) {
            if (cursor == null) {
                return list;
            }
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                YutakRobot robot = serializeRobot(cursor);
                list.add(robot);
            }
        }
        return list;
    }

    public List<YutakRobotMenu> queryRobotMenus(List<String> robotIds) {
        List<YutakRobotMenu> list = new ArrayList<>();
        try (Cursor cursor = YutakApplication.get().getDbHelper().select(robotMenu, "robot_id in (" + YutakCursor.getPlaceholders(robotIds.size()) + ")", robotIds.toArray(new String[0]), null)) {
            if (cursor == null) {
                return list;
            }
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                YutakRobotMenu robotMenu = serializeRobotMenu(cursor);
                list.add(robotMenu);
            }
        }
        return list;
    }

    public List<YutakRobotMenu> queryRobotMenus(String robotID) {
        List<YutakRobotMenu> list = new ArrayList<>();
        try (Cursor cursor = YutakApplication.get().getDbHelper().select(robotMenu, "robot_id=?", new String[]{robotID}, null)) {
            if (cursor == null) {
                return list;
            }
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                YutakRobotMenu robotMenu = serializeRobotMenu(cursor);
                list.add(robotMenu);
            }
        }
        return list;
    }

    public void insertMenus(List<YutakRobotMenu> list) {
        if (list == null || list.size() == 0) return;
        List<ContentValues> cvList = new ArrayList<>();
        for (YutakRobotMenu robot : list) {
            cvList.add(getCV(robot));
        }
        try {
            YutakApplication.get().getDbHelper().getDb().beginTransaction();
            for (ContentValues cv : cvList) {
                YutakApplication.get().getDbHelper().insert(robotMenu, cv);
            }
            YutakApplication.get().getDbHelper().getDb().setTransactionSuccessful();

        } finally {
            YutakApplication.get().getDbHelper().getDb().endTransaction();
        }
    }

    private YutakRobot serializeRobot(Cursor cursor) {
        YutakRobot robot = new YutakRobot();
        robot.robotID = YutakCursor.readString(cursor, "robot_id");
        robot.status = YutakCursor.readInt(cursor, "status");
        robot.version = YutakCursor.readLong(cursor, "version");
        robot.username = YutakCursor.readString(cursor, "username");
        robot.inlineOn = YutakCursor.readInt(cursor, "inline_on");
        robot.placeholder = YutakCursor.readString(cursor, "placeholder");
        robot.createdAT = YutakCursor.readString(cursor, "created_at");
        robot.updatedAT = YutakCursor.readString(cursor, "updated_at");
        return robot;
    }

    private YutakRobotMenu serializeRobotMenu(Cursor cursor) {
        YutakRobotMenu robot = new YutakRobotMenu();
        robot.robotID = YutakCursor.readString(cursor, "robot_id");
        robot.type = YutakCursor.readString(cursor, "type");
        robot.cmd = YutakCursor.readString(cursor, "cmd");
        robot.remark = YutakCursor.readString(cursor, "remark");
        robot.createdAT = YutakCursor.readString(cursor, "created_at");
        robot.updatedAT = YutakCursor.readString(cursor, "updated_at");
        return robot;
    }

    private ContentValues getCV(YutakRobot yutakRobot) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("robot_id", yutakRobot.robotID);
        contentValues.put("inline_on", yutakRobot.inlineOn);
        contentValues.put("username", yutakRobot.username);
        contentValues.put("placeholder", yutakRobot.placeholder);
        contentValues.put("status", yutakRobot.status);
        contentValues.put("version", yutakRobot.version);
        contentValues.put("created_at", yutakRobot.createdAT);
        contentValues.put("updated_at", yutakRobot.updatedAT);
        return contentValues;
    }

    private ContentValues getCV(YutakRobotMenu robotMenu) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("robot_id", robotMenu.robotID);
        contentValues.put("cmd", robotMenu.cmd);
        contentValues.put("remark", robotMenu.remark);
        contentValues.put("type", robotMenu.type);
        contentValues.put("created_at", robotMenu.createdAT);
        contentValues.put("updated_at", robotMenu.updatedAT);
        return contentValues;
    }
}
