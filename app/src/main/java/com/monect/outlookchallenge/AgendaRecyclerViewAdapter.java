package com.monect.outlookchallenge;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.monect.outlookchallenge.CalendarAgendaFragment.OnListFragmentInteractionListener;
import com.monect.ui.FloatingHeaderRecyclerViewAdapter;
import com.monect.ui.WeatherIconView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;


public class AgendaRecyclerViewAdapter extends
        FloatingHeaderRecyclerViewAdapter<AgendaRecyclerViewAdapter.ViewHolderHeader, RecyclerView.ViewHolder> {

    private final static int VIEW_TYPE_AGENDA = 0;
    private final static int VIEW_TYPE_WEATHER = 1;

    private List<CalendarMeta> mCalendarMetaList;
    private List<InstanceMeta> mInstanceMetaList;
    private List<ListItem> mAllListItems = new ArrayList<>();

    private final Context mContext;
    private final long mBeginMillis;
    private final long mEndMillis;
    private final long mNowMillis;

    private OnListFragmentInteractionListener mListener;

    private DateFormat mDateFormat = new SimpleDateFormat("EEEE, MMMM d", Locale.getDefault());
    private DateFormat mTimeFormat = DateFormat.getTimeInstance(DateFormat.SHORT);

    private HashMap<String, WeatherMeta> mWeatherMetaList = new HashMap<>();

    AgendaRecyclerViewAdapter(Context context, long beginMillis, long endMillis, long nowMillis, OnListFragmentInteractionListener listener) {
        mContext = context;
        mBeginMillis = beginMillis;
        mEndMillis = endMillis;
        mNowMillis = nowMillis;
        mListener = listener;

        mWeatherMetaList.put(CalendarAgendaFragment.WEATHER_TIME_MORNING, new WeatherMeta(CalendarAgendaFragment.WEATHER_TIME_MORNING, "", Integer.MIN_VALUE));
        mWeatherMetaList.put(CalendarAgendaFragment.WEATHER_TIME_AFTERNOON, new WeatherMeta(CalendarAgendaFragment.WEATHER_TIME_AFTERNOON, "", Integer.MIN_VALUE));
        mWeatherMetaList.put(CalendarAgendaFragment.WEATHER_TIME_EVENING, new WeatherMeta(CalendarAgendaFragment.WEATHER_TIME_EVENING, "", Integer.MIN_VALUE));
        mWeatherMetaList.put(CalendarAgendaFragment.WEATHER_TIME_TOMORROW_MORNING, new WeatherMeta(CalendarAgendaFragment.WEATHER_TIME_TOMORROW_MORNING, "", Integer.MIN_VALUE));
        mWeatherMetaList.put(CalendarAgendaFragment.WEATHER_TIME_TOMORROW_AFTERNOON, new WeatherMeta(CalendarAgendaFragment.WEATHER_TIME_TOMORROW_AFTERNOON, "", Integer.MIN_VALUE));
        mWeatherMetaList.put(CalendarAgendaFragment.WEATHER_TIME_TOMORROW_EVENING, new WeatherMeta(CalendarAgendaFragment.WEATHER_TIME_TOMORROW_EVENING, "", Integer.MIN_VALUE));
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    void setCalendarMetaList(List<CalendarMeta> calendarMetaList) {
        mCalendarMetaList = calendarMetaList;
    }

    void setInstanceMetaList(List<InstanceMeta> instanceMetaList) {
        mInstanceMetaList = instanceMetaList;
        generateAllListItem();
        notifyDataSetChanged();
    }

    long getTimeFromPosition(int position) {
        return mAllListItems.get(position).mGroupID * DateUtils.DAY_IN_MILLIS + mBeginMillis;
    }

    int getPositionFromTime(long time) {
        int position = 0;
        for (ListItem listItem : mAllListItems) {
            if (mBeginMillis + (listItem.mGroupID * DateUtils.DAY_IN_MILLIS) >= time) {
                break;
            }
            position++;
        }

        return position;
    }

    private void generateAllListItem() {
        if (mBeginMillis != 0 && mEndMillis != 0 && mInstanceMetaList != null) {
            mAllListItems.clear();

            int groupID = 0;
            int instanceBeginIndex = 0;
            long time = mBeginMillis;
            while (time <= mEndMillis) {

                if (time == mNowMillis) {
                    // Add weather info
                    mAllListItems.add(new ListItem(groupID, mWeatherMetaList.get(CalendarAgendaFragment.WEATHER_TIME_MORNING)));
                    mAllListItems.add(new ListItem(groupID, mWeatherMetaList.get(CalendarAgendaFragment.WEATHER_TIME_AFTERNOON)));
                    mAllListItems.add(new ListItem(groupID, mWeatherMetaList.get(CalendarAgendaFragment.WEATHER_TIME_EVENING)));
                } else if (time == (mNowMillis + DateUtils.DAY_IN_MILLIS)) {
                    mAllListItems.add(new ListItem(groupID, mWeatherMetaList.get(CalendarAgendaFragment.WEATHER_TIME_TOMORROW_MORNING)));
                    mAllListItems.add(new ListItem(groupID, mWeatherMetaList.get(CalendarAgendaFragment.WEATHER_TIME_TOMORROW_AFTERNOON)));
                    mAllListItems.add(new ListItem(groupID, mWeatherMetaList.get(CalendarAgendaFragment.WEATHER_TIME_TOMORROW_EVENING)));
                }

                boolean needAddNoEvent = true;
                for (int index = instanceBeginIndex; index < mInstanceMetaList.size(); index++) {

                    if ((mInstanceMetaList.get(index).getBeginTime() - time) < DateUtils.DAY_IN_MILLIS) {
                        mAllListItems.add(new ListItem(groupID, mInstanceMetaList.get(index)));
                        instanceBeginIndex++;
                        needAddNoEvent = false;
                    } else {
                        // The oldest instance is not in this day, stop searching
                        break;
                    }
                }

                if (needAddNoEvent) {
                    mAllListItems.add(new ListItem(groupID));
                }

                time += DateUtils.DAY_IN_MILLIS;
                groupID++;
            }
        }

    }

    void updateWeather(WeatherMeta weatherMeta) {
        WeatherMeta weather = mWeatherMetaList.get(weatherMeta.mWeatherTime);
        weather.mWeatherIcon = weatherMeta.mWeatherIcon;
        weather.mTemperature = weatherMeta.mTemperature;

        notifyDataSetChanged();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        if (viewType == VIEW_TYPE_WEATHER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_agenda_weather, parent, false);
            return new ViewHolderWeather(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_agenda_item, parent, false);
            return new ViewHolderAgenda(view);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (mAllListItems.get(position).mWeatherMeta != null) {
            return VIEW_TYPE_WEATHER;
        } else {
            return VIEW_TYPE_AGENDA;
        }
    }


    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        if (mAllListItems.get(position).mInstanceMeta == null && mAllListItems.get(position).mWeatherMeta == null) {
            // No event found
            ViewHolderAgenda viewHolderAgenda = (ViewHolderAgenda) holder;
            viewHolderAgenda.mContentView.setVisibility(View.GONE);
            viewHolderAgenda.mNoEventView.setVisibility(View.VISIBLE);
        } else if (mAllListItems.get(position).mInstanceMeta != null) {
            ViewHolderAgenda viewHolderAgenda = (ViewHolderAgenda) holder;

            InstanceMeta instanceMeta = mAllListItems.get(position).mInstanceMeta;

            viewHolderAgenda.mContentView.setVisibility(View.VISIBLE);
            viewHolderAgenda.mNoEventView.setVisibility(View.GONE);

            if (instanceMeta.isAllDay()) {
                viewHolderAgenda.mWhenView.setText(mContext.getResources().getString(R.string.all_day).toUpperCase());
            } else {
                viewHolderAgenda.mWhenView.setText(mTimeFormat.format(instanceMeta.getBeginTime()));
            }
            viewHolderAgenda.mTitleView.setText(instanceMeta.getTitle());
            viewHolderAgenda.mDurationView.setText(formatTimeSpanString(instanceMeta.getBeginTime(), instanceMeta.getEndTime()));
        } else if (mAllListItems.get(position).mWeatherMeta != null) {
            ViewHolderWeather viewHolderWeather = (ViewHolderWeather) holder;
            WeatherMeta weatherMeta = mAllListItems.get(position).mWeatherMeta;

            switch (weatherMeta.mWeatherTime) {
                case CalendarAgendaFragment.WEATHER_TIME_MORNING:
                case CalendarAgendaFragment.WEATHER_TIME_TOMORROW_MORNING:
                    viewHolderWeather.mTimeView.setText(R.string.morning);
                    break;
                case CalendarAgendaFragment.WEATHER_TIME_AFTERNOON:
                case CalendarAgendaFragment.WEATHER_TIME_TOMORROW_AFTERNOON:
                    viewHolderWeather.mTimeView.setText(R.string.afternoon);
                    break;
                case CalendarAgendaFragment.WEATHER_TIME_EVENING:
                case CalendarAgendaFragment.WEATHER_TIME_TOMORROW_EVENING:
                    viewHolderWeather.mTimeView.setText(R.string.evening);
                    break;
            }

            switch (weatherMeta.mWeatherIcon) {
                case "clear-day":
                    viewHolderWeather.mWeatherIconView.setIconResource(mContext.getString(R.string.wi_forecast_io_clear_day));
                    break;
                case "clear-night":
                    viewHolderWeather.mWeatherIconView.setIconResource(mContext.getString(R.string.wi_forecast_io_clear_night));
                    break;
                case "rain":
                    viewHolderWeather.mWeatherIconView.setIconResource(mContext.getString(R.string.wi_forecast_io_rain));
                    break;
                case "snow":
                    viewHolderWeather.mWeatherIconView.setIconResource(mContext.getString(R.string.wi_forecast_io_snow));
                    break;
                case "sleet":
                    viewHolderWeather.mWeatherIconView.setIconResource(mContext.getString(R.string.wi_forecast_io_sleet));
                    break;
                case "wind":
                    viewHolderWeather.mWeatherIconView.setIconResource(mContext.getString(R.string.wi_forecast_io_wind));
                    break;
                case "fog":
                    viewHolderWeather.mWeatherIconView.setIconResource(mContext.getString(R.string.wi_forecast_io_fog));
                    break;
                case "cloudy":
                    viewHolderWeather.mWeatherIconView.setIconResource(mContext.getString(R.string.wi_forecast_io_cloudy));
                    break;
                case "partly-cloudy-day":
                    viewHolderWeather.mWeatherIconView.setIconResource(mContext.getString(R.string.wi_forecast_io_partly_cloudy_day));
                    break;
                case "partly-cloudy-night":
                    viewHolderWeather.mWeatherIconView.setIconResource(mContext.getString(R.string.wi_forecast_io_partly_cloudy_night));
                    break;
                case "hail":
                    viewHolderWeather.mWeatherIconView.setIconResource(mContext.getString(R.string.wi_forecast_io_hail));
                    break;
                case "thunderstorm":
                    viewHolderWeather.mWeatherIconView.setIconResource(mContext.getString(R.string.wi_forecast_io_thunderstorm));
                    break;
                case "tornado":
                    viewHolderWeather.mWeatherIconView.setIconResource(mContext.getString(R.string.wi_forecast_io_tornado));
                    break;
            }

            if (weatherMeta.mTemperature == Integer.MIN_VALUE) {
                viewHolderWeather.mTempratureView.setText(R.string.fetch);
            } else {
                String temp = String.valueOf((int) weatherMeta.mTemperature) + "℃";
                viewHolderWeather.mTempratureView.setText(temp);
            }
        }
    }


    private String formatTimeSpanString(long begin, long end) {

        StringBuilder timeSpanString = new StringBuilder();

        int days = (int) ((end - begin) / DateUtils.DAY_IN_MILLIS);
        int hours = (int) ((end - begin - (days * DateUtils.DAY_IN_MILLIS)) / DateUtils.HOUR_IN_MILLIS);
        int minutes = (int) ((end - begin - (days * DateUtils.DAY_IN_MILLIS) - (hours * DateUtils.HOUR_IN_MILLIS)) / DateUtils.MINUTE_IN_MILLIS);

        if (days != 0) {
            timeSpanString.append(days).append(mContext.getResources().getString(R.string.day_short));
        }
        if (hours != 0) {
            timeSpanString.append(hours).append(mContext.getResources().getString(R.string.hour_short));
        }
        if (minutes != 0) {
            timeSpanString.append(minutes).append(mContext.getResources().getString(R.string.minute_short));
        }

        return timeSpanString.toString();
    }

    @Override
    public int getItemCount() {
        return mAllListItems.size();
    }


    @Override
    public long getHeaderId(int position) {
        return mAllListItems.get(position).mGroupID;
    }

    @Override
    public ViewHolderHeader onCreateHeaderViewHolder(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_agenda_header, parent, false);
        return new ViewHolderHeader(view);
    }

    @Override
    public void onBindHeaderViewHolder(ViewHolderHeader viewHolder, int position) {

        int groupID = mAllListItems.get(position).mGroupID;
        Date date = new Date(mBeginMillis + (groupID * DateUtils.DAY_IN_MILLIS));

        if (date.getTime() == mNowMillis) {
            viewHolder.mDateView.setTextColor(ContextCompat.getColor(mContext, R.color.colorPrimary));
            viewHolder.mDateView.setText(mContext.getString(R.string.today).toUpperCase() + " · " + mDateFormat.format(date).toUpperCase());
        } else if (date.getTime() == mNowMillis + DateUtils.DAY_IN_MILLIS) {

            viewHolder.mDateView.setTextColor(ContextCompat.getColor(mContext, R.color.textColorPrimary));
            viewHolder.mDateView.setText(mContext.getString(R.string.tomorrow).toUpperCase() + " · " + mDateFormat.format(date).toUpperCase());
        } else if (date.getTime() == mNowMillis - DateUtils.DAY_IN_MILLIS) {

            viewHolder.mDateView.setTextColor(ContextCompat.getColor(mContext, R.color.textColorPrimary));
            viewHolder.mDateView.setText(mContext.getString(R.string.yesterday).toUpperCase() + " · " + mDateFormat.format(date).toUpperCase());
        } else {
            viewHolder.mDateView.setTextColor(ContextCompat.getColor(mContext, R.color.textColorPrimary));
            viewHolder.mDateView.setText(mDateFormat.format(date).toUpperCase());
        }
    }


    private class ListItem {
        final int mGroupID;
        InstanceMeta mInstanceMeta;
        WeatherMeta mWeatherMeta;

        ListItem(int groupID) {
            mGroupID = groupID;
            mInstanceMeta = null;
            mWeatherMeta = null;
        }

        ListItem(int groupID, InstanceMeta instanceMeta) {
            mGroupID = groupID;
            mInstanceMeta = instanceMeta;
            mWeatherMeta = null;
        }

        ListItem(int groupID, WeatherMeta weatherMeta) {
            mGroupID = groupID;
            mInstanceMeta = null;
            mWeatherMeta = weatherMeta;
        }
    }


    class ViewHolderHeader extends RecyclerView.ViewHolder {
        final View mView;
        final TextView mDateView;

        ViewHolderHeader(View view) {
            super(view);
            mView = view;
            mDateView = (TextView) view.findViewById(R.id.date);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mDateView.getText() + "'";
        }
    }


    private class ViewHolderAgenda extends RecyclerView.ViewHolder {
        final View mView;
        final TextView mWhenView;
        final TextView mDurationView;
        final View mTypeView;
        final TextView mTitleView;
        final View mContentView;
        final View mNoEventView;

        ViewHolderAgenda(View view) {
            super(view);
            mView = view;
            mWhenView = (TextView) view.findViewById(R.id.when);
            mDurationView = (TextView) view.findViewById(R.id.duration);
            mTypeView = view.findViewById(R.id.type);
            mTitleView = (TextView) view.findViewById(R.id.title);
            mContentView = view.findViewById(R.id.content);
            mNoEventView = view.findViewById(R.id.no_event);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mWhenView.getText() + " '" + mDurationView.getText() + " '" + mTitleView.getText() + "'";
        }
    }

    private class ViewHolderWeather extends RecyclerView.ViewHolder {
        final View mView;
        final TextView mTimeView;
        final WeatherIconView mWeatherIconView;
        final TextView mTempratureView;

        ViewHolderWeather(View view) {
            super(view);
            mView = view;
            mTimeView = (TextView) view.findViewById(R.id.time);
            mWeatherIconView = (WeatherIconView) view.findViewById(R.id.weather_icon);
            mTempratureView = (TextView) view.findViewById(R.id.temperature);
        }

    }
}
