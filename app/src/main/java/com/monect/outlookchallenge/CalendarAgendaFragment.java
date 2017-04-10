package com.monect.outlookchallenge;

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.ContentUris;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Point;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.RelativeLayout;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.gson.Gson;
import com.monect.ui.FloatingHeaderDecoration;
import com.monect.ui.RecycleViewDivider;
import com.monect.ui.TopSnapHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static android.support.v4.content.PermissionChecker.checkSelfPermission;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnListFragmentInteractionListener}
 * interface.
 */
public class CalendarAgendaFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = CalendarAgendaFragment.class.getName();

    final static int PERMISSIONS_REQUEST_ALL = 1;
    String[] ALL_PERMISSIONS = {Manifest.permission.READ_CALENDAR, Manifest.permission.ACCESS_COARSE_LOCATION};

    private OnListFragmentInteractionListener mListener;

    private AgendaRecyclerViewAdapter mAgendaRecyclerViewAdapter;
    private CalendarGridRecyclerViewAdapter mCalendarGridRecyclerViewAdapter;

    public static final String[] CALENDAR_PROJECTION = new String[]{
            CalendarContract.Calendars._ID,                           // 0
            CalendarContract.Calendars.ACCOUNT_NAME,                  // 1
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,         // 2
            CalendarContract.Calendars.OWNER_ACCOUNT                  // 3
    };

    // The indices for the projection array above.
    private static final int CAL_PROJECTION_ID_INDEX = 0;
    private static final int CAL_PROJECTION_ACCOUNT_NAME_INDEX = 1;
    private static final int CAL_PROJECTION_DISPLAY_NAME_INDEX = 2;
    private static final int CAL_PROJECTION_OWNER_ACCOUNT_INDEX = 3;


    public static final String[] INSTANCE_PROJECTION = new String[]{
            CalendarContract.Instances.EVENT_ID,        // 0
            CalendarContract.Instances.BEGIN,           // 1
            CalendarContract.Instances.END,             // 2
            CalendarContract.Instances.TITLE,           // 3
            CalendarContract.Instances.EVENT_LOCATION,  // 4
            CalendarContract.Instances.ALL_DAY          // 5
    };

    // The indices for the projection array above.
    private static final int INSTANCE_PROJECTION_ID_INDEX = 0;
    private static final int INSTANCE_PROJECTION_BEGIN_INDEX = 1;
    private static final int INSTANCE_PROJECTION_END_INDEX = 2;
    private static final int INSTANCE_PROJECTION_TITLE_INDEX = 3;
    private static final int INSTANCE_PROJECTION_LOCATION_INDEX = 4;
    private static final int INSTANCE_PROJECTION_ALLDAY_INDEX = 5;

    // Query 66 weeks, 13 weeks before this week, 52 weeks after this week
    private static final int mWeekRangeBefore = 13;
    private static final int mWeekRangeAfter = 52;

    private List<CalendarMeta> mCalendarMetaList = new ArrayList<>();
    private List<InstanceMeta> mInstanceMetaList = new ArrayList<>();

    public static final int LOADER_ID_CALENDARS = 0;
    public static final int LOADER_ID_CALENDAR_INSTANCES = 1;

    long mBeginMillis;
    long mEndMillis;
    long mNowMillis;
    long mCurSelectTime;

    RecyclerView mCalendarGrid;
    RecyclerView mAgendaList;

    LinearLayoutManager mAgendaListLayoutManager;
    LinearLayoutManager mCalendarLayoutManager;

    private int mScreenWidth;
    private static final int EXPAND_ANIMATION_DURATION = 300; // In milli seconds
    ValueAnimator mCalendarGridAnimator;

    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;

    public final static String WEATHER_TIME_MORNING = "WEATHER_TIME_MORNING";
    public final static String WEATHER_TIME_AFTERNOON = "WEATHER_TIME_AFTERNOON";
    public final static String WEATHER_TIME_EVENING = "WEATHER_TIME_EVENING";
    public final static String WEATHER_TIME_TOMORROW_MORNING = "WEATHER_TIME_TOMORROW_MORNING";
    public final static String WEATHER_TIME_TOMORROW_AFTERNOON = "WEATHER_TIME_TOMORROW_AFTERNOON";
    public final static String WEATHER_TIME_TOMORROW_EVENING = "WEATHER_TIME_TOMORROW_EVENING";


    final static int MORNING_HOUR = 6;
    final static int AFTERNOON_HOUR = 13;
    final static int EVENING_HOUR = 19;

    private final static String KEY_WEATHER_REQUEST_TIME = "weather_request_time";
    private final static long WEATHER_REQUEST_BUFFER_TIME = DateUtils.HOUR_IN_MILLIS;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public CalendarAgendaFragment() {
    }

    public static CalendarAgendaFragment newInstance() {
        return new CalendarAgendaFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Calendar rightNow = Calendar.getInstance();
        mNowMillis = rightNow.getTimeInMillis() - (rightNow.getTimeInMillis() % DateUtils.DAY_IN_MILLIS);

        long daysBeforeToday = rightNow.get(Calendar.DAY_OF_WEEK) - 1 + (mWeekRangeBefore * 7); // 7 days a week
        mBeginMillis = mNowMillis - (daysBeforeToday * DateUtils.DAY_IN_MILLIS);

        long daysAfterToday = (7 - rightNow.get(Calendar.DAY_OF_WEEK)) + (mWeekRangeAfter * 7); // 7 days a week
        mEndMillis = mNowMillis + (daysAfterToday * DateUtils.DAY_IN_MILLIS);

        mAgendaRecyclerViewAdapter = new AgendaRecyclerViewAdapter(getActivity(), mBeginMillis, mEndMillis, mNowMillis, mListener);

        mCalendarGridRecyclerViewAdapter = new CalendarGridRecyclerViewAdapter(getActivity(), mBeginMillis, mEndMillis, mNowMillis, new CalendarGridRecyclerViewAdapter.DayView.ClickListener() {
            @Override
            public void onItemClicked(int position) {

                long time = mCalendarGridRecyclerViewAdapter.getTimeFromPosition(position);

                int agendaPosition = mAgendaRecyclerViewAdapter.getPositionFromTime(time);
                mAgendaListLayoutManager.scrollToPositionWithOffset(agendaPosition, 0);
            }
        });

        mCalendarGridAnimator = ValueAnimator.ofFloat();
        mCalendarGridAnimator.setDuration(EXPAND_ANIMATION_DURATION);
        mCalendarGridAnimator.setInterpolator(new LinearInterpolator());
        mCalendarGridAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (float) animation.getAnimatedValue();
                RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mCalendarGrid.getLayoutParams();
                layoutParams.height = (int) value;

                View view = getView();
                if (view != null) {
                    view.requestLayout();
                }
            }
        });


        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            DateFormat dateFormat = new SimpleDateFormat("MMMM", Locale.getDefault());
            actionBar.setTitle(dateFormat.format(new Date()));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (checkPermissions()) {
            enumerateCalendar();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_agenda_list, container, false);
        View agendaList = view.findViewById(R.id.agenda_list);
        View calendarGrid = view.findViewById(R.id.calendar_grid);

        if (agendaList instanceof RecyclerView) {
            mAgendaList = (RecyclerView) agendaList;
            mAgendaListLayoutManager = new LinearLayoutManager(getActivity());
            mAgendaList.setLayoutManager(mAgendaListLayoutManager);

            mAgendaList.setAdapter(mAgendaRecyclerViewAdapter);

            mAgendaList.addItemDecoration(new FloatingHeaderDecoration(mAgendaRecyclerViewAdapter));
            mAgendaList.addItemDecoration(new RecycleViewDivider(getActivity(), LinearLayoutManager.HORIZONTAL, 2, ContextCompat.getColor(getActivity(), R.color.dividerColor)));
            mAgendaList.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getActionMasked()) {
                        case MotionEvent.ACTION_DOWN:
                            break;

                        case MotionEvent.ACTION_MOVE:
                            if (mIsCalendarGridExpanded) {
                                mIsCalendarGridExpanded = false;
                                collapseCalendarGrid();
                            }
                            break;
                    }
                    return false;
                }
            });

            mAgendaList.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    super.onScrollStateChanged(recyclerView, newState);
                }

                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    int agendaPosition = mAgendaListLayoutManager.findFirstVisibleItemPosition();
                    long time = mAgendaRecyclerViewAdapter.getTimeFromPosition(agendaPosition);
                    if (mCurSelectTime != time) {
                        mCurSelectTime = time;

                        int dayPosition = mCalendarGridRecyclerViewAdapter.getPositionFromTime(mCurSelectTime);
                        int weekPosition = dayPosition / 7;

                        int firstVisibleItemPosition = mCalendarLayoutManager.findFirstCompletelyVisibleItemPosition();
                        int lastVisibleItemPosition = mCalendarLayoutManager.findLastCompletelyVisibleItemPosition();

                        if (weekPosition < firstVisibleItemPosition || weekPosition > lastVisibleItemPosition) {

                            mCalendarGrid.scrollBy(0, (weekPosition - firstVisibleItemPosition) * mCalendarGrid.getChildAt(0).getHeight());
                        }

                        View weekView = mCalendarLayoutManager.findViewByPosition(weekPosition);

                        mCalendarGridRecyclerViewAdapter.setCurSelectedItem(dayPosition, weekView);
                    }
                }
            });
        }

        if (calendarGrid instanceof RecyclerView) {
            mCalendarGrid = (RecyclerView) calendarGrid;

            TopSnapHelper topSnapHelper = new TopSnapHelper();
            topSnapHelper.attachToRecyclerView(mCalendarGrid);

            mCalendarLayoutManager = new LinearLayoutManager(getActivity());
            mCalendarGrid.setLayoutManager(mCalendarLayoutManager);

            mCalendarGrid.setAdapter(mCalendarGridRecyclerViewAdapter);
            //mCalendarGrid.addItemDecoration(new RecycleViewDivider(getActivity(), LinearLayoutManager.HORIZONTAL, 2, getResources().getColor(R.color.dividerColor)));

            // Resize calendar grid
            Display display = getActivity().getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            mScreenWidth = size.x;

            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mCalendarGrid.getLayoutParams();
            layoutParams.width = mScreenWidth;

            if (mIsCalendarGridExpanded) {
                layoutParams.height = layoutParams.width * 5 / 7;
            } else {
                layoutParams.height = layoutParams.width * 2 / 7;
            }
            mCalendarGrid.setLayoutParams(layoutParams);

            mCalendarGrid.setOnTouchListener(new View.OnTouchListener() {

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getActionMasked()) {
                        case MotionEvent.ACTION_MOVE:
                            if (!mIsCalendarGridExpanded) {
                                mIsCalendarGridExpanded = true;
                                expandCalendarGrid();

                            }
                            break;
                    }
                    return false;
                }
            });

        }


        return view;
    }

    @Override
    public void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    @Override
    public void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    void expandCalendarGrid() {
        mCalendarGridAnimator.setFloatValues((float) mScreenWidth * 2 / 7, (float) mScreenWidth * 5 / 7);
        mCalendarGridAnimator.start();
    }

    void collapseCalendarGrid() {
        mCalendarGridAnimator.setFloatValues((float) mScreenWidth * 5 / 7, (float) mScreenWidth * 2 / 7);
        mCalendarGridAnimator.start();
    }

    private boolean mIsCalendarGridExpanded = false;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

        if (checkPermissions()) {
            getWeatherInfo();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    public interface OnListFragmentInteractionListener {
    }


    void getWeatherInfo() {
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            long currentTime = new Date().getTime();
            SharedPreferences sharedPreferences = getActivity().getPreferences(Context.MODE_PRIVATE);
            long lastWeatherRequestTime = sharedPreferences.getLong(KEY_WEATHER_REQUEST_TIME, 0);

            if (currentTime - lastWeatherRequestTime > WEATHER_REQUEST_BUFFER_TIME) {
                mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                new FetchHttpContentTask().execute(true);
                new FetchHttpContentTask().execute(false);
            } else {
                // Get weather meta data in preference
                Gson gson = new Gson();
                updateLastRequestedWeather(WEATHER_TIME_MORNING, gson, sharedPreferences);
                updateLastRequestedWeather(WEATHER_TIME_AFTERNOON, gson, sharedPreferences);
                updateLastRequestedWeather(WEATHER_TIME_EVENING, gson, sharedPreferences);
                updateLastRequestedWeather(WEATHER_TIME_TOMORROW_MORNING, gson, sharedPreferences);
                updateLastRequestedWeather(WEATHER_TIME_TOMORROW_AFTERNOON, gson, sharedPreferences);
                updateLastRequestedWeather(WEATHER_TIME_TOMORROW_EVENING, gson, sharedPreferences);
            }
        }
    }

    void updateLastRequestedWeather(String weatherTime, Gson gson, SharedPreferences sharedPreferences) {
        String string = sharedPreferences.getString(weatherTime, null);
        WeatherMeta weatherMeta = gson.fromJson(string, WeatherMeta.class);
        mAgendaRecyclerViewAdapter.updateWeather(weatherMeta);
    }

    class FetchHttpContentTask extends AsyncTask<Boolean, Void, Boolean> {

        List<WeatherMeta> mWeatherMetaList = new ArrayList<>();

        @Override
        protected Boolean doInBackground(Boolean... params) {
            boolean requestSuccess = false;
            try {
                boolean isToday = params[0];
                int time = (int) (mNowMillis / 1000);
                if (!isToday) {
                    time = (int) ((mNowMillis + DateUtils.DAY_IN_MILLIS) / 1000);
                }

                String requestString = "https://api.darksky.net/forecast/" + "392cc52ae2771856ef01c93de22ab569/" +
                        String.valueOf(mLastLocation.getLatitude()) +
                        "," +
                        String.valueOf(mLastLocation.getLongitude()) +
                        "," +
                        String.valueOf(time) +
                        "?exclude=currently,minutely,daily,alerts,flags&units=si";

                ByteArrayOutputStream os = new ByteArrayOutputStream();
                byte[] data = new byte[1024];
                int len;
                URL url;
                url = new URL(requestString);

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                InputStream in = conn.getInputStream();
                while ((len = in.read(data)) != -1) {
                    os.write(data, 0, len);
                }
                in.close();

                String jasonData = new String(os.toByteArray());
                JSONObject jsonObject = new JSONObject(jasonData);
                JSONObject jsonObjectHourly = jsonObject.getJSONObject("hourly");
                JSONArray jsonArray = jsonObjectHourly.getJSONArray("data");
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject1 = (JSONObject) jsonArray.get(i);

                    long timeInSeconds = jsonObject1.getLong("time");
                    String icon = jsonObject1.getString("icon");
                    double temperature = jsonObject1.getDouble("temperature");

                    Calendar calendar = Calendar.getInstance();
                    calendar.setTimeInMillis(timeInSeconds * 1000);
                    switch (calendar.get(Calendar.HOUR_OF_DAY)) {
                        case MORNING_HOUR:
                            if (isToday) {
                                mWeatherMetaList.add(new WeatherMeta(WEATHER_TIME_MORNING, icon, temperature));
                            } else {
                                mWeatherMetaList.add(new WeatherMeta(WEATHER_TIME_TOMORROW_MORNING, icon, temperature));
                            }
                            break;
                        case AFTERNOON_HOUR:
                            if (isToday) {
                                mWeatherMetaList.add(new WeatherMeta(WEATHER_TIME_AFTERNOON, icon, temperature));
                            } else {
                                mWeatherMetaList.add(new WeatherMeta(WEATHER_TIME_TOMORROW_AFTERNOON, icon, temperature));
                            }
                            break;
                        case EVENING_HOUR:
                            if (isToday) {
                                mWeatherMetaList.add(new WeatherMeta(WEATHER_TIME_EVENING, icon, temperature));
                            } else {
                                mWeatherMetaList.add(new WeatherMeta(WEATHER_TIME_TOMORROW_EVENING, icon, temperature));
                            }
                            break;
                    }
                }

                requestSuccess = true;
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
            return requestSuccess;
        }

        @Override
        protected void onPostExecute(Boolean requestSuccess) {
            super.onPostExecute(requestSuccess);
            if (requestSuccess) {

                SharedPreferences.Editor editor = getActivity().getPreferences(Context.MODE_PRIVATE).edit();
                Gson gson = new Gson();

                for (WeatherMeta weatherMeta : mWeatherMetaList) {
                    mAgendaRecyclerViewAdapter.updateWeather(weatherMeta);

                    editor.putString(weatherMeta.mWeatherTime, gson.toJson(weatherMeta));
                }

                editor.putLong(KEY_WEATHER_REQUEST_TIME, new Date().getTime());
                editor.apply();
            }
        }
    }


    /* Calendar section begin */

    void enumerateCalendar() {
        if (checkSelfPermission(getActivity(), Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
            getLoaderManager().restartLoader(LOADER_ID_CALENDARS, null, this);
            getLoaderManager().restartLoader(LOADER_ID_CALENDAR_INSTANCES, null, this);
        }
    }

    /* Calendar section end */

    /* LoaderCallbacks begin */

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        CursorLoader cursorLoader = null;
        switch (id) {
            case LOADER_ID_CALENDARS:
                Uri uri = CalendarContract.Calendars.CONTENT_URI;
                cursorLoader = new CursorLoader(getActivity(), uri, CALENDAR_PROJECTION, null, null, null);
                break;

            case LOADER_ID_CALENDAR_INSTANCES:

                // Construct the query with the desired date range.
                Uri.Builder builder = CalendarContract.Instances.CONTENT_URI.buildUpon();
                ContentUris.appendId(builder, mBeginMillis);
                ContentUris.appendId(builder, mEndMillis);

                cursorLoader = new CursorLoader(getActivity(), builder.build(), INSTANCE_PROJECTION, null, null, CalendarContract.Instances.BEGIN + " ASC");
                break;
        }
        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        switch (loader.getId()) {
            case LOADER_ID_CALENDARS:
                mCalendarMetaList.clear();
                while (cursor.moveToNext()) {
                    long calID = cursor.getLong(CAL_PROJECTION_ID_INDEX);
                    String displayName = cursor.getString(CAL_PROJECTION_DISPLAY_NAME_INDEX);
                    String accountName = cursor.getString(CAL_PROJECTION_ACCOUNT_NAME_INDEX);
                    String ownerName = cursor.getString(CAL_PROJECTION_OWNER_ACCOUNT_INDEX);
                    mCalendarMetaList.add(new CalendarMeta(calID, displayName, accountName, ownerName));
                }
                mAgendaRecyclerViewAdapter.setCalendarMetaList(mCalendarMetaList);
                break;

            case LOADER_ID_CALENDAR_INSTANCES:
                mInstanceMetaList.clear();
                while (cursor.moveToNext()) {
                    long eventID = cursor.getLong(INSTANCE_PROJECTION_ID_INDEX);
                    long begin = cursor.getLong(INSTANCE_PROJECTION_BEGIN_INDEX);
                    long end = cursor.getLong(INSTANCE_PROJECTION_END_INDEX);
                    String title = cursor.getString(INSTANCE_PROJECTION_TITLE_INDEX);
                    String location = cursor.getString(INSTANCE_PROJECTION_LOCATION_INDEX);
                    boolean isAllDay = cursor.getInt(INSTANCE_PROJECTION_ALLDAY_INDEX) == 1;
                    mInstanceMetaList.add(new InstanceMeta(eventID, begin, end, title, location, isAllDay));
                }
                mAgendaRecyclerViewAdapter.setInstanceMetaList(mInstanceMetaList);
                mCalendarGridRecyclerViewAdapter.setInstanceMetaList(mInstanceMetaList);


                View rootView = getView();
                if (rootView != null) {
                    rootView.post(new Runnable() {
                        @Override
                        public void run() {
                            scrollToToday();
                        }
                    });
                }
                break;
        }

    }

    public void scrollToToday() {
        int agendaPosition = mAgendaRecyclerViewAdapter.getPositionFromTime(mNowMillis);
        mAgendaListLayoutManager.scrollToPositionWithOffset(agendaPosition, 0);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
    /* LoaderCallbacks end */


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ALL: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    enumerateCalendar();
                    getWeatherInfo();
                } else {
                    Log.i(TAG, "permission denied, boo!");
                }
            }
            break;

        }
    }

    boolean checkPermissions() {
        if (!hasPermissions(getActivity(), ALL_PERMISSIONS)) {
            ActivityCompat.requestPermissions(getActivity(), ALL_PERMISSIONS, PERMISSIONS_REQUEST_ALL);
            return false;
        } else {
            return true;
        }
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }
}
