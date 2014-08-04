package com.hyperionsoft.currencyconverter;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class SettingsPreferences extends PreferenceActivity implements OnSharedPreferenceChangeListener {

	private CheckBoxPreference mRememberCheckBoxPreference;
	private ListPreference mNewsListPreference;
	private ListPreference mConverterFromListPreference;
	private ListPreference mConverterToListPreference;
	private ListPreference mNumberOfDecimalsPreference;
	private ListPreference mDataSourcePreference;
	private ListPreference mUpdateFrequencyPreference;
	private CheckBoxPreference mShowSymbols;
	
	
	
	@Override
	protected void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings);
		
		//Get the list preferences objects...
		mNewsListPreference = (ListPreference) getPreferenceScreen().findPreference("NEWS_CURRENCY_SELECTED_DEFAULT");
		mConverterFromListPreference = (ListPreference) getPreferenceScreen().findPreference("CONVERTER_SELECTED_FROM_CURRENCY_DEFAULT");
		mConverterToListPreference = (ListPreference) getPreferenceScreen().findPreference("CONVERTER_SELECTED_TO_CURRENCY_DEFAULT");
		mRememberCheckBoxPreference = (CheckBoxPreference) getPreferenceScreen().findPreference("REMEMBER_LAST_USED_CURRENCIES");
		mNumberOfDecimalsPreference = (ListPreference) getPreferenceScreen().findPreference("NUMBER_OF_DECIMALS");
		mDataSourcePreference = (ListPreference) getPreferenceScreen().findPreference("DATA_SOURCE");
		mUpdateFrequencyPreference = (ListPreference) getPreferenceScreen().findPreference("UPDATE_FREQUENCY");
		mShowSymbols = (CheckBoxPreference) getPreferenceScreen().findPreference("SHOW_SYMBOLS");
		
		//Set the summary to the current value, note will default to default value field of xml on first run.
        mNewsListPreference.setSummary(mNewsListPreference.getEntry().toString());
        mConverterFromListPreference.setSummary(mConverterFromListPreference.getEntry().toString());
        mConverterToListPreference.setSummary(mConverterToListPreference.getEntry().toString());
        mNumberOfDecimalsPreference.setSummary("Number of decimals to display: " + mNumberOfDecimalsPreference.getEntry().toString());
        mDataSourcePreference.setSummary("Source of currency data: " + mDataSourcePreference.getEntry().toString());
        mUpdateFrequencyPreference.setSummary("Update Frequency: " + mUpdateFrequencyPreference.getEntry().toString());

        
        //Disable or Enable the Converter & News Tabs settings depending on REMEMBER_LAST_USED_CURRENCIES state...
        if (mRememberCheckBoxPreference.isChecked()) {
        	mNewsListPreference.setEnabled(false);
        	mConverterFromListPreference.setEnabled(false);
        	mConverterToListPreference.setEnabled(false);
        } else {
        	mNewsListPreference.setEnabled(true);
           	mConverterFromListPreference.setEnabled(true);
        	mConverterToListPreference.setEnabled(true);       	
        }
      
	}
	
	public static boolean getRememberDefault(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("REMEMBER_LAST_USED_CURRENCIES", true);
	}

	public static String getNewsSelectedDefault(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getString("NEWS_CURRENCY_SELECTED_DEFAULT", "GBP");  //24=GBP
	}
	public static String getConverterFromDefault(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getString("CONVERTER_SELECTED_FROM_CURRENCY_DEFAULT", "GBP"); //GBP
	}
	
	public static String getConverterToDefault(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getString("CONVERTER_SELECTED_TO_CURRENCY_DEFAULT", "USD"); //USD
	}

	public static String getNumberOfDecimalsDefault(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getString("NUMBER_OF_DECIMALS", "4"); 
	}      
	
	public static String getDataSourceDefault(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getString("DATA_SOURCE", "Yahoo!"); 
	}    
	
	public static String getUpdateFrequencyDefault(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getString("UPDATE_FREQUENCY", "3 Hours"); 
	} 
	
	public static boolean getShowSymbols(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("SHOW_SYMBOLS", true);
	}
	
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		// TODO Auto-generated method stub
		
		  //Disable or Enable the Converter & News Tabs settings depending on REMEMBER_LAST_USED_CURRENCIES state...
        if (mRememberCheckBoxPreference.isChecked()) {
        	mNewsListPreference.setEnabled(false);
        	mConverterFromListPreference.setEnabled(false);
        	mConverterToListPreference.setEnabled(false);
        } else {
        	mNewsListPreference.setEnabled(true);
           	mConverterFromListPreference.setEnabled(true);
        	mConverterToListPreference.setEnabled(true);       	
        }
		
		if (key.equals("NEWS_CURRENCY_SELECTED_DEFAULT")) mNewsListPreference.setSummary(mNewsListPreference.getEntry().toString());
		if (key.equals("CONVERTER_SELECTED_FROM_CURRENCY_DEFAULT")) mConverterFromListPreference.setSummary(mConverterFromListPreference.getEntry().toString());	
		if (key.equals("CONVERTER_SELECTED_TO_CURRENCY_DEFAULT")) mConverterToListPreference.setSummary(mConverterToListPreference.getEntry().toString());
		if (key.equals("NUMBER_OF_DECIMALS")) mNumberOfDecimalsPreference.setSummary("Number of decimals to display :" + mNumberOfDecimalsPreference.getEntry().toString());
		if (key.equals("DATA_SOURCE")) mDataSourcePreference.setSummary("Source of currency data:" + mDataSourcePreference.getEntry().toString());
		if (key.equals("UPDATE_FREQUENCY")) mUpdateFrequencyPreference.setSummary("Update Frequency: " + mUpdateFrequencyPreference.getEntry().toString());
		
	}

	 @Override
	    protected void onResume() {
	        super.onResume();
	        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	    }

	
    @Override
    protected void onPause() {
        super.onPause();
        // Unregister the listener whenever a key changes
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

	
}
