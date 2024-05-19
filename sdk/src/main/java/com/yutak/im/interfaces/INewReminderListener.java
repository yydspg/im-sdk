package com.yutak.im.interfaces;

import com.yutak.im.domain.YutakReminder;

import java.util.List;

public interface INewReminderListener {
    void newReminder(List<YutakReminder> list);
}
