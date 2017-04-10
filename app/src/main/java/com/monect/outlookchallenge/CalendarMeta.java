package com.monect.outlookchallenge;

/**
 * Created by Monect on 12/10/2016.
 */

class CalendarMeta {
    private long mCalID = 0;

    public long getCalID() {
        return mCalID;
    }

    private String mDisplayName = null;

    public String getDisplayName() {
        return mDisplayName;
    }

    private String mAccountName = null;

    public String getAccountName() {
        return mAccountName;
    }

    private String mOwnerName = null;

    public String getOwnerName() {
        return mOwnerName;
    }


    CalendarMeta(long id, String displayName, String accountName, String ownerName) {

        mCalID = id;
        mDisplayName = displayName;
        mAccountName = accountName;
        mOwnerName = ownerName;
    }


}
