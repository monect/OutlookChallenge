package com.monect.outlookchallenge;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by Monect on 14/10/2016.
 */

class CalendarGridRecyclerViewAdapter extends RecyclerView.Adapter<CalendarGridRecyclerViewAdapter.ViewHolderWeek> {

    private final long mBeginMillis;
    private final long mEndMillis;

    private List<InstanceMeta> mInstanceMetaList;
    private List<ListItem> mAllListItems = new ArrayList<>();

    private int mScreenWidth;
    private int mCurSelectedItem = -1;

    private DayView.ClickListener mClickListener;
    private View mCurSelView;

    private int mCurrentYear;
    private int mColorCalendarBackgroundEven;
    private int mColorCalendarBackgroundOdd;
    private int mTextColorPrimary;

    void setCurSelectedItem(int dayPosition, View weekView) {
        int lastSelectedItem = mCurSelectedItem;
        mCurSelectedItem = dayPosition;

        if (mCurSelView != null) {
            DayView dayView = new DayView(mCurSelView);
            refreshDayView(mAllListItems.get(lastSelectedItem), dayView, lastSelectedItem);
        }


        if (weekView != null) {
            int dayInWeek = mCurSelectedItem % 7;
            ViewGroup viewGroup = (ViewGroup) weekView;
            View view = viewGroup.getChildAt(dayInWeek);

            DayView dayView = new DayView(view);
            refreshDayView(mAllListItems.get(mCurSelectedItem), dayView, mCurSelectedItem);

            mCurSelView = view;
        }
    }


    int getPositionFromTime(long time) {

        return (int) ((time - mBeginMillis) / DateUtils.DAY_IN_MILLIS);
    }


    long getTimeFromPosition(int position) {
        return mAllListItems.get(position).mTime;
    }

    CalendarGridRecyclerViewAdapter(Context context, long beginMillis, long endMillis, long nowMillis, DayView.ClickListener clickListener) {
        mBeginMillis = beginMillis;
        mEndMillis = endMillis;
        mClickListener = clickListener;

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(nowMillis);
        mCurrentYear = calendar.get(Calendar.YEAR);

        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        mScreenWidth = size.x;

        mColorCalendarBackgroundEven = ContextCompat.getColor(context, R.color.colorCalendarBackgroundEven);
        mColorCalendarBackgroundOdd = ContextCompat.getColor(context, R.color.colorCalendarBackgroundOdd);
        mTextColorPrimary = ContextCompat.getColor(context, R.color.textColorPrimary);
    }

    void setInstanceMetaList(List<InstanceMeta> instanceMetaList) {
        mInstanceMetaList = instanceMetaList;
        generateAllListItem();
        notifyDataSetChanged();
    }


    private void generateAllListItem() {
        if (mBeginMillis != 0 && mEndMillis != 0 && mInstanceMetaList != null) {
            mAllListItems.clear();

            int groupID = 0;
            int instanceBeginIndex = 0;
            long time = mBeginMillis;
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(time);
            int month = calendar.get(Calendar.MONTH);

            while (time <= mEndMillis) {

                boolean hasEvent = false;
                for (int index = instanceBeginIndex; index < mInstanceMetaList.size(); index++) {

                    if ((mInstanceMetaList.get(index).getBeginTime() - time) < DateUtils.DAY_IN_MILLIS) {
                        instanceBeginIndex++;
                        hasEvent = true;
                    } else {
                        // The oldest instance is not in this day, stop searching
                        break;
                    }
                }

                mAllListItems.add(new ListItem(groupID, time, hasEvent));

                time += DateUtils.DAY_IN_MILLIS;

                calendar.setTimeInMillis(time);
                if (month != calendar.get(Calendar.MONTH)) {
                    groupID++;
                    month = calendar.get(Calendar.MONTH);
                }
            }
        }

    }

    @Override
    public ViewHolderWeek onCreateViewHolder(ViewGroup parent, int viewType) {
        LinearLayout view = (LinearLayout) LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_calendar_row, parent, false);

        return new ViewHolderWeek(parent, view, mScreenWidth / 7, mScreenWidth / 7, mClickListener);
    }


    @Override
    public void onBindViewHolder(ViewHolderWeek holder, int position) {
        for (int dayInWeek = 0; dayInWeek < 7; dayInWeek++) {

            int dayPosition = position * 7 + dayInWeek;
            ListItem listItem = mAllListItems.get(dayPosition);

            refreshDayView(listItem, holder.mDayView[dayInWeek], dayPosition);
        }
    }

    private void refreshDayView(ListItem listItem, DayView dayView, int dayPosition) {

        if (listItem.mGroupID % 2 == 0) {
            dayView.mView.setBackgroundColor(mColorCalendarBackgroundEven);
        } else {
            dayView.mView.setBackgroundColor(mColorCalendarBackgroundOdd);
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(listItem.mTime);
        dayView.mDayView.setText(String.valueOf(calendar.get(Calendar.DAY_OF_MONTH)));

        if (dayPosition == mCurSelectedItem) {
            dayView.mSelectedView.setAlpha(1f);
            dayView.mDayView.setTextColor(Color.WHITE);
            dayView.mMonthView.setAlpha(0f);
            dayView.mYearView.setAlpha(0f);
            dayView.mIndicatorView.setAlpha(0f);

            mCurSelView = dayView.mView;
        } else {
            dayView.mSelectedView.setAlpha(0f);
            dayView.mDayView.setTextColor(mTextColorPrimary);

            if (calendar.get(Calendar.DAY_OF_MONTH) == 1) {
                DateFormat dateFormat = new SimpleDateFormat("MMM", Locale.getDefault());
                dayView.mMonthView.setText(dateFormat.format(new Date(listItem.mTime)));
                dayView.mMonthView.setAlpha(1f);

                if (calendar.get(Calendar.YEAR) > mCurrentYear) {
                    dayView.mYearView.setText(String.valueOf(calendar.get(Calendar.YEAR)));
                    dayView.mIndicatorView.setAlpha(0f);
                    dayView.mYearView.setAlpha(1f);
                } else {
                    dayView.mYearView.setText("");
                    dayView.mYearView.setAlpha(0f);
                }

            } else {
                dayView.mMonthView.setText("");

                dayView.mYearView.setText("");
                dayView.mYearView.setAlpha(0f);

                if (listItem.mHasEvent) {
                    dayView.mIndicatorView.setAlpha(1f);

                } else {
                    dayView.mIndicatorView.setAlpha(0f);
                }
            }
        }

    }

    @Override
    public int getItemCount() {
        return mAllListItems.size() / 7;
    }


    private class ListItem {
        final int mGroupID;
        final long mTime;
        final boolean mHasEvent;

        ListItem(int groupID, long time, boolean hasEvent) {
            mGroupID = groupID;
            mTime = time;
            mHasEvent = hasEvent;
        }
    }

    static class DayView {
        final ViewHolderWeek mParent;
        final int mDayInWeek;
        final View mView;
        final TextView mDayView;
        final TextView mMonthView;
        final TextView mYearView;
        final View mSelectedView;
        final View mIndicatorView;

        ClickListener mClickListener;

        DayView(View view) {
            this(null, 0, view, null);
        }

        DayView(ViewHolderWeek parent, int dayInWeek, View view, ClickListener clickListener) {
            mParent = parent;
            mDayInWeek = dayInWeek;
            mView = view;
            mClickListener = clickListener;

            mDayView = (TextView) view.findViewById(R.id.day);
            mMonthView = (TextView) view.findViewById(R.id.month);
            mYearView = (TextView) view.findViewById(R.id.year);
            mSelectedView = view.findViewById(R.id.selected_view);
            mIndicatorView = view.findViewById(R.id.event_indicator);

            if (mClickListener != null) {
                mView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mClickListener != null && mParent != null) {
                            mClickListener.onItemClicked(mParent.getAdapterPosition() * 7 + mDayInWeek);
                        }
                    }
                });
            }
        }


        interface ClickListener {
            void onItemClicked(int position);
        }
    }

    static class ViewHolderWeek extends RecyclerView.ViewHolder {
        final ViewGroup mWeekView;
        final DayView[] mDayView = new DayView[7];

        ViewHolderWeek(ViewGroup parent, ViewGroup weekView, int dayWidth, int dayHeight, DayView.ClickListener listener) {
            super(weekView);
            mWeekView = weekView;

            for (int dayInWeek = 0; dayInWeek < 7; dayInWeek++) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_calendar_item, parent, false);
                mWeekView.addView(view, dayWidth, dayHeight);

                DayView dayView = new DayView(this, dayInWeek, view, listener);
                mDayView[dayInWeek] = dayView;
            }

        }

    }
}
