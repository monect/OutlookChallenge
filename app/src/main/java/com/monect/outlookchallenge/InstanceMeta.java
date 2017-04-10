package com.monect.outlookchallenge;

/**
 * Created by Monect on 12/10/2016.
 */

class InstanceMeta {
    private long mEventID = 0;

    private long mBeginTime = 0;

    private long mEndTime = 0;

    private String mTitle;

    private String mLocation;

    private boolean mIsAllDay;

    InstanceMeta(long id, long beginTime, long endTime, String title, String location, boolean isAllDay) {

        mEventID = id;
        mBeginTime = beginTime;
        mEndTime = endTime;
        mTitle = title;
        mLocation = location;
        mIsAllDay = isAllDay;
    }

    public long getEventID() {
        return mEventID;
    }

    long getBeginTime() {
        return mBeginTime;
    }

    long getEndTime() {
        return mEndTime;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getLocation() {
        return mLocation;
    }

    boolean isAllDay() {
        return mIsAllDay;
    }
}
