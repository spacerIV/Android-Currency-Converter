package com.hyperionsoft.currencyconverter;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import android.app.Dialog;
import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TabHost;

@SuppressWarnings("deprecation")
public class CurrencyConverter extends TabActivity {
		
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    
     	 // Look up the AdView as a resource and load a request.
    	 AdView mAdView = (AdView) this.findViewById(R.id.adView);
   	     AdRequest adRequest = new AdRequest.Builder().build();
   	     mAdView.loadAd(adRequest);
	    
	    setContentView(R.layout.currencyconvertermain);

        //Do tabs stuff here
	    Resources res = getResources(); // Resource object to get Drawables
	    TabHost tabHost = getTabHost();  // The activity TabHost
	    TabHost.TabSpec spec;  // Resusable TabSpec for each tab
	    Intent intent;  // Reusable Intent for each tab

	    //Create an Intent to launch an Activity for the tab (to be reused)
	    intent = new Intent().setClass(this, CurrencyActivity.class);
	    //Initialize a TabSpec for each tab and add it to the TabHost
	    spec = tabHost.newTabSpec("currency").setIndicator("Currencies",
	                      res.getDrawable(R.drawable.tabcoins))
	                  .setContent(intent);
	    tabHost.addTab(spec);
	    
	    intent = new Intent().setClass(this, ConverterActivity.class);
	    spec = tabHost.newTabSpec("converter").setIndicator("Converter",
	                      res.getDrawable(R.drawable.tabchart))
	                  .setContent(intent);
	  
	    tabHost.addTab(spec);
	    
	    // Do the same for the other tabs
	    intent = new Intent().setClass(this, NewsActivity.class);
	    spec = tabHost.newTabSpec("news").setIndicator("News",
	                      res.getDrawable(R.drawable.tabnews))
	                  .setContent(intent);
	    tabHost.addTab(spec);

	    
	    tabHost.setCurrentTab(0);
	    tabHost.setBackgroundColor(0);
	    
	}
	
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.optionsmenu, menu);
	    return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	    case R.id.settingsmenu:
	        startActivity(new Intent(this, SettingsPreferences.class));
	        return true;
	    case R.id.aboutmenu:
	    	showDialog(0);
	    
	    	
	        return true;   
	    }
		return true;
	}
	
	protected Dialog onCreateDialog(int id) {
	    //About box stuff here...
    	Dialog dialog = new Dialog(this);
    	dialog.setContentView(R.layout.aboutbox);
    	dialog.setTitle("Currency Converter");
    
    	//TextView text = (TextView) dialog.findViewById(R.id.abouttext);
    	//text.setText("By Victor Houston");
    	ImageView image = (ImageView) dialog.findViewById(R.id.abouticon);
    	image.setImageResource(R.drawable.moneychart);
	    
	    return dialog;
	}
	
	@Override
	public void onStart() {
		super.onStart();
		EasyTracker.getInstance().activityStart(this);
	}

	@Override
	public void onStop() {
		super.onStop();
		EasyTracker.getInstance().activityStop(this);
	}
	

}
