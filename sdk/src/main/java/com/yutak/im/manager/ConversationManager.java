package com.yutak.im.manager;

public class ConversationManager {
    private ConversationManager() {
    }

    private static class ConversationManagerBinder {
        static final ConversationManager manager = new ConversationManager();
    }

    public static ConversationManager getInstance() {
        return ConversationManagerBinder.manager;
    }
}
