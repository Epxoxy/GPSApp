package com.daijian.gpsapp;

//A debug only class
class Messenger {
    private Messenger(){}

    void setLogListener(LogListener listener){
        this.logListener = listener;
    }

    private void tryLog(String text){
        if(logListener != null){
            logListener.onNewLogAppend(text);
        }
    }

    private LogListener logListener;
    interface LogListener{
        void onNewLogAppend(String text);
    }

    static final Messenger defaultOne = new Messenger();
    static void logln(String text){
        System.out.println(text);
        defaultOne.tryLog(text + System.getProperty("line.separator"));
    }
}
