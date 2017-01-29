package com.caleb.spotifysleep;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import java.util.List;

/**
 * Created by Caleb on 10/5/15.
 */
public class PreferencesActivity extends android.preference.PreferenceActivity {

    public static final String PLAYLIST_KEY = "com.caleb.spotifysleep.playlist_key";
    public static final String DEFAULT_TIME_KEY = "com.caleb.spotifysleep.default_time_key";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new MyPreferenceFragment()).commit();
    }

    public static class MyPreferenceFragment extends PreferenceFragment
    {
        @Override
        public void onCreate(final Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
        }
    }
}
