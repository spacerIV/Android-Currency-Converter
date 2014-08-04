package com.hyperionsoft.currencyconverter;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import java.util.List;
import java.util.concurrent.RejectedExecutionException;

import com.google.analytics.tracking.android.EasyTracker;

import com.google.android.gms.ads.AdView;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.Time;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;



public class ConverterActivity extends Activity {
	
	
	Spinner FromSpinner, ToSpinner;
	String fromCurrency, toCurrency, toAmount;
	TextView chartHeader, converterText1, converterText2, converterSourceText, converterDateText;
	String[] currencyCodes, codeCountryCurr, currencySymbols;
	
	private Boolean networkAvailable = false;
	ImageView chartImage1, chartImage2;
	Drawable drawableChartImage;
	ProgressBar chartProgress1, chartProgress2, converterProgress1;
	String numberOfDecimals;
	private Integer chartRange=2; //Lets default to 3 months on start up.
	private DecimalFormat decFormat1, decFormat2, decFormat3, decFormat4, decFormat5, decFormat6;
	private ViewFlipper mTextFlipper, mChartFlipper;
	private int currentTextFlip=1;  //Either set to 1 for converterText1 or 2 for converterText2, which ever is currently on top.
	private int currentChartFlip=1; //Same as above...
	private Handler mySwapHandler;
	private Runnable mySwapRunnable;
	private boolean swapImageSwapped=false;
	private boolean firstRun = true; //prevents both spinners from firing the onclick listeners at first start-up.
	private String dataSource;
	private String updateFrequency;
	private CurrencyData currencyData; //db class
	private SimpleDateFormat dateFormat;
	private String[] sqlFromCols1 = {"amount", "source", "dateTime"};
	private ImageView swapImage;
	private List<CurrencyYahoo> currenciesYahoo = new ArrayList<CurrencyYahoo>();
	
	
    @Override
	public void onCreate (Bundle savedInstanceState) {
    	


		codeCountryCurr = getResources().getStringArray(R.array.code_country_currency);
		currencyCodes = getResources().getStringArray(R.array.currency_codes);
		currencySymbols = getResources().getStringArray(R.array.currency_symbols);
	
		super.onCreate(savedInstanceState);
		setContentView(R.layout.convertermain);
		
		Typeface tf = Typeface.createFromAsset(getAssets(), "DejaVuSans.ttf");
		
		mySwapHandler = new Handler();
		mySwapRunnable = new Runnable() {
			public void run() {
				// Turns off.
				swapImageSwapped=false;
			}
		};
		
		FromSpinner =  (Spinner) findViewById(R.id.FromSpinner);
		ToSpinner =  (Spinner) findViewById(R.id.ToSpinner);
		converterText1 = (TextView) findViewById(R.id.converterText1);
		converterText2 = (TextView) findViewById(R.id.converterText2);
		converterText1.setTypeface(tf);
		converterText2.setTypeface(tf);
		converterSourceText = (TextView) findViewById(R.id.converterSourceText);
		converterDateText = (TextView) findViewById(R.id.converterDateText);
		
		chartHeader = (TextView) findViewById(R.id.chartHeader);
		chartImage1 = (ImageView) findViewById(R.id.chartImage1);
		chartImage2 = (ImageView) findViewById(R.id.chartImage2);
		swapImage = (ImageView) findViewById(R.id.swapImage);
		chartImage1.setOnClickListener(chartImageListener);
		chartImage2.setOnClickListener(chartImageListener);
		swapImage.setOnClickListener(swapImageListener);
		
		FromSpinner.setPrompt("Choose a Currency");
		ToSpinner.setPrompt("Choose a Currency");
		
		chartProgress2 = (ProgressBar) findViewById(R.id.chartProgress2);
		chartProgress2.setVisibility(View.INVISIBLE);
		converterProgress1 = (ProgressBar) findViewById(R.id.converterProgress1);
		converterProgress1.setVisibility(View.INVISIBLE);
		
		mTextFlipper = ((ViewFlipper) this.findViewById(R.id.flipperText));
		mTextFlipper.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.push_up_in));
        mTextFlipper.setOutAnimation(AnimationUtils.loadAnimation(this, R.anim.push_up_out));
        
        mChartFlipper = ((ViewFlipper) this.findViewById(R.id.flipperChart));
       // mChartFlipper.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.push_left_in));
       // mChartFlipper.setOutAnimation(AnimationUtils.loadAnimation(this, R.anim.push_left_out));
        //mChartFlipper.setInAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in));
        //mChartFlipper.setOutAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_out));
        mChartFlipper.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
        mChartFlipper.setOutAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_out));
        
        currencyData = new CurrencyData(this);
        dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		decFormat1 = new DecimalFormat("######0.0");
		decFormat2 = new DecimalFormat("######0.00");
		decFormat3 = new DecimalFormat("######0.000");
		decFormat4 = new DecimalFormat("######0.0000");
		decFormat5 = new DecimalFormat("######0.00000");
		decFormat6 = new DecimalFormat("######0.000000");
		
		
		//Inner classes provide callbacks that notifiy the app when an item has been selected on a spinner.
		class FromSpinnerItemSelected implements OnItemSelectedListener {

			public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {

				fromCurrency = parent.getItemAtPosition(pos).toString().trim();
				if (swapImageSwapped == false) { 
				   startGetConverterConversion();
				   startGetYahooChart();
				   
				}
			}

			public void onNothingSelected(AdapterView<?> parent) {
				// Do nothing.
			}
		}
		
		//Inner classes provide callbacks that notifiy the app when an item has been selected on a spinner.
		class ToSpinnerItemSelected implements OnItemSelectedListener {

			public void onItemSelected(AdapterView<?> parent,View view, int pos, long id) {
			
				toCurrency = parent.getItemAtPosition(pos).toString().trim();
				if ((swapImageSwapped == false) && (firstRun == false)) {
				   startGetConverterConversion();
				   startGetYahooChart();
				}
				if (firstRun == true) firstRun = false;
			}

			public void onNothingSelected(AdapterView<?> parent) {
				// Do nothing.
			}
		} 
		
		FromSpinner.isVerticalScrollBarEnabled();
		FromSpinner.setVerticalFadingEdgeEnabled(true);
		
		FromSpinner.setOnItemSelectedListener(new FromSpinnerItemSelected());
		ToSpinner.setOnItemSelectedListener(new ToSpinnerItemSelected());
		
		ToSpinner.setVerticalFadingEdgeEnabled(false);
		
		FromSpinner.setAdapter(new MyCustomAdapter(this, R.layout.row, currencyCodes));
		ToSpinner.setAdapter(new MyCustomAdapter(this, R.layout.row, currencyCodes));
		
		//Get NUMBER_OF_DECIMALS preference...
		numberOfDecimals = SettingsPreferences.getNumberOfDecimalsDefault(this);
		dataSource = SettingsPreferences.getDataSourceDefault(this);
		
		//Is the 'remember last used currencies' checked in the Settings?
		if (SettingsPreferences.getRememberDefault(getApplicationContext())) {

			Integer fromCurr = getPreferences(MODE_PRIVATE).getInt("CONVERTER_FROM_CURRENCY_REMEMBERED", 24);  //24=GBP
			Integer toCurr = getPreferences(MODE_PRIVATE).getInt("CONVERTER_TO_CURRENCY_REMEMBERED", 83);      //83=USD
			if ((fromCurr == null) || (toCurr == null)) {
				//Nothing set yet, most likely the very first run of the program
				fromCurrency = "GBP";
				toCurrency = "USD";
				FromSpinner.setSelection(24);
				ToSpinner.setSelection(83);
			} else {

				fromCurrency = currencyCodes[fromCurr];
				toCurrency = currencyCodes[toCurr];
				FromSpinner.setSelection(fromCurr);
				ToSpinner.setSelection(toCurr);
			}
		} else  {     //now check for saved default currencies for the converter tab
			//if not, then check if the converter tab default currencies are set..
			String fromCurrPref = SettingsPreferences.getConverterFromDefault(getApplicationContext());
			String toCurrPref = SettingsPreferences.getConverterToDefault(getApplicationContext());
			if ((fromCurrPref == null) || (toCurrPref ==null)) {
				//Nothing set yet, most likely the very first run of the program. Not sure if this is needed
				fromCurrency = "GBP";
				toCurrency = "USD";
				FromSpinner.setSelection(24);
				ToSpinner.setSelection(83);
			}
			FromSpinner.setSelection(getCurrencyInt(fromCurrPref));
			ToSpinner.setSelection(getCurrencyInt(toCurrPref));
			fromCurrency = fromCurrPref;
			toCurrency = toCurrPref;

		}

	}  //end of onCreate
    
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

	@Override
	protected void onResume() {
		super.onResume();
		
	}
	
	 @Override
	    protected void onDestroy() {
	       
	        super.onDestroy();
	    }
    
    @Override 
    protected void onPause() {
    	
    	
		  super.onPause();
		  
		  //Save the from and to currencies 
		  getPreferences(MODE_PRIVATE).edit().putInt("CONVERTER_FROM_CURRENCY_REMEMBERED", FromSpinner.getSelectedItemPosition()).commit();
		  getPreferences(MODE_PRIVATE).edit().putInt("CONVERTER_TO_CURRENCY_REMEMBERED",   ToSpinner.getSelectedItemPosition()).commit();

	  }
    
    private String sortDecimals(String myTxt) {
    	
    	numberOfDecimals = SettingsPreferences.getNumberOfDecimalsDefault(this);
		String toAmtDouble = null;  
		try { 
		switch (Integer.parseInt(numberOfDecimals)) {
		  case 1:
			  toAmtDouble = decFormat1.format(Double.parseDouble(myTxt.replaceAll("�", ""))); 
			  break;
		  case 2:
			  toAmtDouble = decFormat2.format(Double.parseDouble(myTxt.replaceAll("�", ""))); 
			  break;
		  case 3: 
			  toAmtDouble = decFormat3.format(Double.parseDouble(myTxt.replaceAll("�", ""))); 
			  break;
		  case 4:
			  toAmtDouble = decFormat4.format(Double.parseDouble(myTxt.replaceAll("�", ""))); 
			  break;
		  case 5:
			  toAmtDouble = decFormat5.format(Double.parseDouble(myTxt.replaceAll("�", ""))); 
			  break;
		  case 6: 
			  toAmtDouble = decFormat6.format(Double.parseDouble(myTxt.replaceAll("�", ""))); 
			  break;
		} }
		catch (NumberFormatException n) {
			n.printStackTrace();
			toAmtDouble="Not Available";
		}
		
		return toAmtDouble;
    }
    
    private void flipText(String txt) {
    
    	switch (currentTextFlip) {
    	case 1: 
    		currentTextFlip = 2;
    		converterText2.setText(txt);
    		break;
    	case 2:
    		currentTextFlip = 1;
    		converterText1.setText(txt);
    		break;	
    	}
    	mTextFlipper.showNext();
    }
    
    private void flipChart() {

    	switch(currentChartFlip) {
    	case 1: 
    		currentChartFlip = 2;
    		chartImage2.setImageDrawable(drawableChartImage);
    		break;
    	case 2:
    		currentChartFlip = 1;
    		chartImage1.setImageDrawable(drawableChartImage);
    	}
    	
    	mChartFlipper.showNext();
    }

	private int getCurrencyInt(String currPref) {
		   int cnt=0;
		   for (String thecode : currencyCodes) {
			   if (thecode.contentEquals(currPref.trim())) break;
			   cnt++;
		   }
		   return cnt;
		}
	
	private boolean checkDB() {
		
		 dataSource = SettingsPreferences.getDataSourceDefault(this);
		 updateFrequency = SettingsPreferences.getUpdateFrequencyDefault(this);
		
		 Calendar nowMinusFrequency = Calendar.getInstance(); //Todays' date which we shall minus the update frequency
		 
		 if (updateFrequency.equals("Update Manually Only")) {}		 
		 if (updateFrequency.equals("10 Minutes" )) {nowMinusFrequency.add(Calendar.MINUTE, -10); }
		 if (updateFrequency.equals("30 Minutes" )) {nowMinusFrequency.add(Calendar.MINUTE, -30); }
		 if (updateFrequency.equals("1 Hour" )) { {  nowMinusFrequency.add(Calendar.MINUTE, -60);} }
		 if (updateFrequency.equals("2 Hours" )) { { nowMinusFrequency.add(Calendar.HOUR, -2);} }
		 if (updateFrequency.equals("3 Hours" )) { { nowMinusFrequency.add(Calendar.HOUR, -3);} }
		 if (updateFrequency.equals("4 Hours")) { {  nowMinusFrequency.add(Calendar.HOUR, -4);} }
		 if (updateFrequency.equals("6 Hours" )) { { nowMinusFrequency.add(Calendar.HOUR, -6);} }
		 if (updateFrequency.equals("12 Hours" )) { {nowMinusFrequency.add(Calendar.HOUR, -12);} }
		 if (updateFrequency.equals("24 Hours" )) { {nowMinusFrequency.add(Calendar.HOUR, -24);} }
		 	 
		 //OK. So here we check if a suitable value isnt already stored in the db. 
		 //if it is then set screen values and return true.
		 SQLiteDatabase db = currencyData.getReadableDatabase();
		 String whereClause = " fromCurrency = '" + fromCurrency + "' AND toCurrency = '" + toCurrency + "'" + 
		                      " AND source = '" + dataSource + "'" +
		                      " AND dateTime >= '" +  dateFormat.format(nowMinusFrequency.getTime()) + "'" ;

		 Cursor curs = db.query("currencydata", sqlFromCols1, whereClause,null,null,null,null);
		// Log.d("DATABASE","whereClause" + whereClause);
		// Log.d("DATABASE","Current Time :" + dateFormat.format(nowMinusFrequency.getTime()));
		 
		 if (curs.getCount() > 0) {
			 String sqltoAmount="";
			 String savedDate=null;
			 String sqlSource=null;
			 while (curs.moveToNext())  {
				 sqltoAmount = curs.getString(0);
				 sqlSource = curs.getString(1);
				 savedDate = curs.getString(2);
			 }
			 
			 if ((!sqltoAmount.equals("") || !sqltoAmount.equals("Not Available"))) {
				 //right found something so display it and exit.
				 converterDateText.setText("As of: " + savedDate);
				 converterSourceText.setText("Source: " + sqlSource);
				 int fromSymbol = getCurrencyInt(fromCurrency);
			     int toSymbol = getCurrencyInt(toCurrency);
			     if (SettingsPreferences.getShowSymbols(getApplicationContext())) 
				     flipText(currencySymbols[fromSymbol] + "1 " + fromCurrency + " = " + currencySymbols[toSymbol] + sortDecimals(sqltoAmount) + " " + toCurrency );
			     else
			    	 flipText("1 " + fromCurrency + " = " + sortDecimals(sqltoAmount) + " " + toCurrency );
			     
				 toAmount = sqltoAmount;
			 }
			 curs.close();
			 db.close();
			 return true;
		 } else 
			 curs.close();
		     db.close();
			 return false;
		 
		
	}
  
	private void startGetConverterConversion() throws IllegalStateException {
		
		//First check the db, if nothing the run the asynctask to go online.
		if (!checkDB()) {
			
			DoConverterConversionTask doConverterConversionTask = new DoConverterConversionTask();
			try { 
				flipText("Working...");
				converterSourceText.setText("");
				converterDateText.setText("");
				doConverterConversionTask.execute();	
			} catch (RejectedExecutionException r) { 
				//Log.i("CurrencyConverter","Caught RejectedExecutionException Exception on startGetConverterConversion");
			}

		}

	}
	
	private void startGetYahooChart() throws IllegalStateException {
		chartProgress2.setVisibility(View.VISIBLE);
		doYahooChartTask doYahooChartTask = new doYahooChartTask();
		try {
		  doYahooChartTask.execute();
		} catch (RejectedExecutionException r) {
			//Log.i("CurrencyConverter","Caught RejectedExecutionException Exception on startGetYahooChart()");
		}
	}

	public class MyCustomAdapter extends ArrayAdapter<String> {

		public MyCustomAdapter(Context context, int textViewResourceId,String[] objects) {
			super(context, textViewResourceId, objects);
			// TODO Auto-generated constructor stub
		}
		@Override
		public View getDropDownView(int position, View convertView, ViewGroup parent) {
			// TODO Auto-generated method stub
			return getCustomDropDownView(position, convertView, parent);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// TODO Auto-generated method stub
			return getCustomView(position, convertView, parent);
		}

		public View getCustomDropDownView(int position, View convertView, ViewGroup parent) {
	
			LayoutInflater inflater=getLayoutInflater();
			if (convertView == null) {
				convertView = inflater.inflate(R.layout.rowwithflag, parent, false);
			}
			
			//View row=inflater.inflate(R.layout.row, parent, false);
			TextView label=(TextView)convertView.findViewById(R.id.currencyRow1);
			//label.setText(currencyCodes[position]);
			ImageView icon=(ImageView)convertView.findViewById(R.id.flagRow1);

			if (currencyCodes[position].equals("AED")){ icon.setImageResource(R.drawable.aebig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("ANG")){ icon.setImageResource(R.drawable.anbig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("ARS")){ icon.setImageResource(R.drawable.arbig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("AUD")){ icon.setImageResource(R.drawable.aubig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("BDT")){ icon.setImageResource(R.drawable.bdbig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("BGN")){ icon.setImageResource(R.drawable.bgbig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("BHD")){ icon.setImageResource(R.drawable.bhbig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("BND")){ icon.setImageResource(R.drawable.bnbig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("BOB")){ icon.setImageResource(R.drawable.bobig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("BRL")){ icon.setImageResource(R.drawable.brbig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("BWP")){ icon.setImageResource(R.drawable.bwbig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("CAD")){ icon.setImageResource(R.drawable.cabig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("CHF")){ icon.setImageResource(R.drawable.chbig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("CLP")){ icon.setImageResource(R.drawable.clbig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("CNY")){ icon.setImageResource(R.drawable.cnbig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("COP")){ icon.setImageResource(R.drawable.cobig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("CRC")){ icon.setImageResource(R.drawable.crbig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("CZK")){ icon.setImageResource(R.drawable.czbig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("DKK")){ icon.setImageResource(R.drawable.dkbig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("DOP")){ icon.setImageResource(R.drawable.dmbig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("DZD")){ icon.setImageResource(R.drawable.dzbig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("EGP")){ icon.setImageResource(R.drawable.egbig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("EUR")){ icon.setImageResource(R.drawable.eubig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("FJD")){ icon.setImageResource(R.drawable.fjbig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("GBP")){ icon.setImageResource(R.drawable.gbbig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("HKD")){ icon.setImageResource(R.drawable.hkbig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("HNL")){ icon.setImageResource(R.drawable.hnbig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("HRK")){ icon.setImageResource(R.drawable.hrbig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("HUF")){ icon.setImageResource(R.drawable.hubig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("ILS")){ icon.setImageResource(R.drawable.ilbig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("INR")){ icon.setImageResource(R.drawable.irbig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("ISK")){ icon.setImageResource(R.drawable.isbig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("JMD")){ icon.setImageResource(R.drawable.jmbig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("JOD")){ icon.setImageResource(R.drawable.jobig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("JPY")){ icon.setImageResource(R.drawable.jpbig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("KES")){ icon.setImageResource(R.drawable.kebig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("KRW")){ icon.setImageResource(R.drawable.krbig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("KWD")){ icon.setImageResource(R.drawable.kwbig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("KYD")){ icon.setImageResource(R.drawable.kybig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("KZT")){ icon.setImageResource(R.drawable.kzbig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("LBP")){ icon.setImageResource(R.drawable.lbbig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("LKR")){ icon.setImageResource(R.drawable.lkbig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("LTL")){ icon.setImageResource(R.drawable.ltbig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("LVL")){ icon.setImageResource(R.drawable.lvbig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("MAD")){ icon.setImageResource(R.drawable.mabig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("MDL")){ icon.setImageResource(R.drawable.mdbig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("MKD")){ icon.setImageResource(R.drawable.mkbig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("MUR")){ icon.setImageResource(R.drawable.mubig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("MVR")){ icon.setImageResource(R.drawable.mvbig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("MXN")){ icon.setImageResource(R.drawable.mxbig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("MYR")){ icon.setImageResource(R.drawable.mybig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("NAD")){ icon.setImageResource(R.drawable.nabig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("NGN")){ icon.setImageResource(R.drawable.ngbig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("NIO")){ icon.setImageResource(R.drawable.nibig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("NOK")){ icon.setImageResource(R.drawable.nobig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("NPR")){ icon.setImageResource(R.drawable.npbig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("NZD")){ icon.setImageResource(R.drawable.nzbig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("OMR")){ icon.setImageResource(R.drawable.ombig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("PEN")){ icon.setImageResource(R.drawable.pebig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("PGK")){ icon.setImageResource(R.drawable.pgbig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("PHP")){ icon.setImageResource(R.drawable.phbig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("PKR")){ icon.setImageResource(R.drawable.pkbig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("PLN")){ icon.setImageResource(R.drawable.plbig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("PYG")){ icon.setImageResource(R.drawable.pybig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("QAR")){ icon.setImageResource(R.drawable.qabig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("RON")){ icon.setImageResource(R.drawable.robig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("RSD")){ icon.setImageResource(R.drawable.rsbig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("RUB")){ icon.setImageResource(R.drawable.rubig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("SAR")){ icon.setImageResource(R.drawable.sabig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("SCR")){ icon.setImageResource(R.drawable.scbig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("SEK")){ icon.setImageResource(R.drawable.sebig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("SGD")){ icon.setImageResource(R.drawable.sgbig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("SKK")){ icon.setImageResource(R.drawable.skbig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("SLL")){ icon.setImageResource(R.drawable.slbig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("SVC")){ icon.setImageResource(R.drawable.svbig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("THB")){ icon.setImageResource(R.drawable.thbig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("TND")){ icon.setImageResource(R.drawable.tnbig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("TRY")){ icon.setImageResource(R.drawable.trbig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("TTD")){ icon.setImageResource(R.drawable.ttbig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("TWD")){ icon.setImageResource(R.drawable.twbig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("TZS")){ icon.setImageResource(R.drawable.tzbig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("UAH")){ icon.setImageResource(R.drawable.uabig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("UGX")){ icon.setImageResource(R.drawable.ugbig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("USD")){ icon.setImageResource(R.drawable.usbig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("UYU")){ icon.setImageResource(R.drawable.uybig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("UZS")){ icon.setImageResource(R.drawable.uzbig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("VEF")){ icon.setImageResource(R.drawable.vebig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("VND")){ icon.setImageResource(R.drawable.vnbig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("YER")){ icon.setImageResource(R.drawable.yebig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("ZAR")){ icon.setImageResource(R.drawable.zabig);label.setText(codeCountryCurr[position]);}
			if (currencyCodes[position].equals("ZMK")){ icon.setImageResource(R.drawable.zmbig);label.setText(codeCountryCurr[position]);}
			
			return convertView;

		}
		
		public View getCustomView(int position, View convertView, ViewGroup parent) {
			// TODO Auto-generated method stub
			//return super.getView(position, convertView, parent);

			LayoutInflater inflater=getLayoutInflater();
			if (convertView == null) {
				convertView = inflater.inflate(R.layout.rowwithflag, parent, false);
			}
			
			//View row=inflater.inflate(R.layout.row, parent, false);
			//TextView label=(TextView)convertView.findViewById(R.id.currencyRow1);
			//label.setText(currencyCodes[position]);
			ImageView icon=(ImageView)convertView.findViewById(R.id.flagRow1);
			
			if (currencyCodes[position].equals("AED")){ icon.setImageResource(R.drawable.aebig);}
			if (currencyCodes[position].equals("ANG")){ icon.setImageResource(R.drawable.anbig);}
			if (currencyCodes[position].equals("ARS")){ icon.setImageResource(R.drawable.arbig);}
			if (currencyCodes[position].equals("AUD")){ icon.setImageResource(R.drawable.aubig);}
			if (currencyCodes[position].equals("BDT")){ icon.setImageResource(R.drawable.bdbig);}
			if (currencyCodes[position].equals("BGN")){ icon.setImageResource(R.drawable.bgbig);}
			if (currencyCodes[position].equals("BHD")){ icon.setImageResource(R.drawable.bhbig);}
			if (currencyCodes[position].equals("BND")){ icon.setImageResource(R.drawable.bnbig);}
			if (currencyCodes[position].equals("BOB")){ icon.setImageResource(R.drawable.bobig);}
			if (currencyCodes[position].equals("BRL")){ icon.setImageResource(R.drawable.brbig);}
			if (currencyCodes[position].equals("BWP")){ icon.setImageResource(R.drawable.bwbig);}
			if (currencyCodes[position].equals("CAD")){ icon.setImageResource(R.drawable.cabig);}
			if (currencyCodes[position].equals("CHF")){ icon.setImageResource(R.drawable.chbig);}
			if (currencyCodes[position].equals("CLP")){ icon.setImageResource(R.drawable.clbig);}
			if (currencyCodes[position].equals("CNY")){ icon.setImageResource(R.drawable.cnbig);}
			if (currencyCodes[position].equals("COP")){ icon.setImageResource(R.drawable.cobig);}
			if (currencyCodes[position].equals("CRC")){ icon.setImageResource(R.drawable.crbig);}
			if (currencyCodes[position].equals("CZK")){ icon.setImageResource(R.drawable.czbig);}
			if (currencyCodes[position].equals("DKK")){ icon.setImageResource(R.drawable.dkbig);}
			if (currencyCodes[position].equals("DOP")){ icon.setImageResource(R.drawable.dmbig);}
			if (currencyCodes[position].equals("DZD")){ icon.setImageResource(R.drawable.dzbig);}
			if (currencyCodes[position].equals("EGP")){ icon.setImageResource(R.drawable.egbig);}
			if (currencyCodes[position].equals("EUR")){ icon.setImageResource(R.drawable.eubig);}
			if (currencyCodes[position].equals("FJD")){ icon.setImageResource(R.drawable.fjbig);}
			if (currencyCodes[position].equals("GBP")){ icon.setImageResource(R.drawable.gbbig);}
			if (currencyCodes[position].equals("HKD")){ icon.setImageResource(R.drawable.hkbig);}
			if (currencyCodes[position].equals("HNL")){ icon.setImageResource(R.drawable.hnbig);}
			if (currencyCodes[position].equals("HRK")){ icon.setImageResource(R.drawable.hrbig);}
			if (currencyCodes[position].equals("HUF")){ icon.setImageResource(R.drawable.hubig);}
			if (currencyCodes[position].equals("ILS")){ icon.setImageResource(R.drawable.ilbig);}
			if (currencyCodes[position].equals("INR")){ icon.setImageResource(R.drawable.irbig);}
			if (currencyCodes[position].equals("ISK")){ icon.setImageResource(R.drawable.isbig);}
			if (currencyCodes[position].equals("JMD")){ icon.setImageResource(R.drawable.jmbig);}
			if (currencyCodes[position].equals("JOD")){ icon.setImageResource(R.drawable.jobig);}
			if (currencyCodes[position].equals("JPY")){ icon.setImageResource(R.drawable.jpbig);}
			if (currencyCodes[position].equals("KES")){ icon.setImageResource(R.drawable.kebig);}
			if (currencyCodes[position].equals("KRW")){ icon.setImageResource(R.drawable.krbig);}
			if (currencyCodes[position].equals("KWD")){ icon.setImageResource(R.drawable.kwbig);}
			if (currencyCodes[position].equals("KYD")){ icon.setImageResource(R.drawable.kybig);}
			if (currencyCodes[position].equals("KZT")){ icon.setImageResource(R.drawable.kzbig);}
			if (currencyCodes[position].equals("LBP")){ icon.setImageResource(R.drawable.lbbig);}
			if (currencyCodes[position].equals("LKR")){ icon.setImageResource(R.drawable.lkbig);}
			if (currencyCodes[position].equals("LTL")){ icon.setImageResource(R.drawable.ltbig);}
			if (currencyCodes[position].equals("LVL")){ icon.setImageResource(R.drawable.lvbig);}
			if (currencyCodes[position].equals("MAD")){ icon.setImageResource(R.drawable.mabig);}
			if (currencyCodes[position].equals("MDL")){ icon.setImageResource(R.drawable.mdbig);}
			if (currencyCodes[position].equals("MKD")){ icon.setImageResource(R.drawable.mkbig);}
			if (currencyCodes[position].equals("MUR")){ icon.setImageResource(R.drawable.mubig);}
			if (currencyCodes[position].equals("MVR")){ icon.setImageResource(R.drawable.mvbig);}
			if (currencyCodes[position].equals("MXN")){ icon.setImageResource(R.drawable.mxbig);}
			if (currencyCodes[position].equals("MYR")){ icon.setImageResource(R.drawable.mybig);}
			if (currencyCodes[position].equals("NAD")){ icon.setImageResource(R.drawable.nabig);}
			if (currencyCodes[position].equals("NGN")){ icon.setImageResource(R.drawable.ngbig);}
			if (currencyCodes[position].equals("NIO")){ icon.setImageResource(R.drawable.nibig);}
			if (currencyCodes[position].equals("NOK")){ icon.setImageResource(R.drawable.nobig);}
			if (currencyCodes[position].equals("NPR")){ icon.setImageResource(R.drawable.npbig);}
			if (currencyCodes[position].equals("NZD")){ icon.setImageResource(R.drawable.nzbig);}
			if (currencyCodes[position].equals("OMR")){ icon.setImageResource(R.drawable.ombig);}
			if (currencyCodes[position].equals("PEN")){ icon.setImageResource(R.drawable.pebig);}
			if (currencyCodes[position].equals("PGK")){ icon.setImageResource(R.drawable.pgbig);}
			if (currencyCodes[position].equals("PHP")){ icon.setImageResource(R.drawable.phbig);}
			if (currencyCodes[position].equals("PKR")){ icon.setImageResource(R.drawable.pkbig);}
			if (currencyCodes[position].equals("PLN")){ icon.setImageResource(R.drawable.plbig);}
			if (currencyCodes[position].equals("PYG")){ icon.setImageResource(R.drawable.pybig);}
			if (currencyCodes[position].equals("QAR")){ icon.setImageResource(R.drawable.qabig);}
			if (currencyCodes[position].equals("RON")){ icon.setImageResource(R.drawable.robig);}
			if (currencyCodes[position].equals("RSD")){ icon.setImageResource(R.drawable.rsbig);}
			if (currencyCodes[position].equals("RUB")){ icon.setImageResource(R.drawable.rubig);}
			if (currencyCodes[position].equals("SAR")){ icon.setImageResource(R.drawable.sabig);}
			if (currencyCodes[position].equals("SCR")){ icon.setImageResource(R.drawable.scbig);}
			if (currencyCodes[position].equals("SEK")){ icon.setImageResource(R.drawable.sebig);}
			if (currencyCodes[position].equals("SGD")){ icon.setImageResource(R.drawable.sgbig);}
			if (currencyCodes[position].equals("SKK")){ icon.setImageResource(R.drawable.skbig);}
			if (currencyCodes[position].equals("SLL")){ icon.setImageResource(R.drawable.slbig);}
			if (currencyCodes[position].equals("SVC")){ icon.setImageResource(R.drawable.svbig);}
			if (currencyCodes[position].equals("THB")){ icon.setImageResource(R.drawable.thbig);}
			if (currencyCodes[position].equals("TND")){ icon.setImageResource(R.drawable.tnbig);}
			if (currencyCodes[position].equals("TRY")){ icon.setImageResource(R.drawable.trbig);}
			if (currencyCodes[position].equals("TTD")){ icon.setImageResource(R.drawable.ttbig);}
			if (currencyCodes[position].equals("TWD")){ icon.setImageResource(R.drawable.twbig);}
			if (currencyCodes[position].equals("TZS")){ icon.setImageResource(R.drawable.tzbig);}
			if (currencyCodes[position].equals("UAH")){ icon.setImageResource(R.drawable.uabig);}
			if (currencyCodes[position].equals("UGX")){ icon.setImageResource(R.drawable.ugbig);}
			if (currencyCodes[position].equals("USD")){ icon.setImageResource(R.drawable.usbig);}
			if (currencyCodes[position].equals("UYU")){ icon.setImageResource(R.drawable.uybig);}
			if (currencyCodes[position].equals("UZS")){ icon.setImageResource(R.drawable.uzbig);}
			if (currencyCodes[position].equals("VEF")){ icon.setImageResource(R.drawable.vebig);}
			if (currencyCodes[position].equals("VND")){ icon.setImageResource(R.drawable.vnbig);}
			if (currencyCodes[position].equals("YER")){ icon.setImageResource(R.drawable.yebig);}
			if (currencyCodes[position].equals("ZAR")){ icon.setImageResource(R.drawable.zabig);}
			if (currencyCodes[position].equals("ZMK")){ icon.setImageResource(R.drawable.zmbig);}
			  
			return convertView;

		}
	}

	
	
	//Create an anonymous implementation of OnClickListener for the swap image
	private OnClickListener swapImageListener = new OnClickListener() {
	    public void onClick(View v) {
	        //Swops the From and To Spinner values
	    	fromCurrency = (String) ToSpinner.getSelectedItem().toString().trim();
	    	toCurrency = (String) FromSpinner.getSelectedItem().toString().trim();
	    	int toSelection = ToSpinner.getSelectedItemPosition();
	    	
	    	//Set swapImageSwapped=true for 200ms and try prevent the onclick events of the Spinners from firing...
	    	swapImageSwapped = true;  //Turn On, to be turned off in 100ms...
	    	mySwapHandler.postDelayed(mySwapRunnable, 1000); // swapImageSwapped will be false again in 100ms.
	        ToSpinner.setSelection(FromSpinner.getSelectedItemPosition()); 
	        FromSpinner.setSelection(toSelection);
	        startGetConverterConversion();
			startGetYahooChart();
	    }
	};
	
	//Create an anonymous implementation of OnClickListener for the chart image
	private OnClickListener chartImageListener = new OnClickListener() {
	    public void onClick(View v) {
	    	
	    	if (networkAvailable) {
	    		final CharSequence[] items = {"1 Day", "1 Month", "3 Months", "1 Year", "3 Years", "5 Years"};
		    	AlertDialog.Builder builder = new AlertDialog.Builder(ConverterActivity.this);
		    	builder.setTitle("Choose a Chart Range...");
		    	//Progress.setVisibility(View.VISIBLE); // Network here so put on progress dialog...
		    	
		    	builder.setItems(items, new DialogInterface.OnClickListener() {
		    	    public void onClick(DialogInterface dialog, int item) {
		    	    	chartRange = item;    //NB Autobox
		    	    	startGetYahooChart(); 
		    	    }
		    	});
		    	
		    	AlertDialog alert = builder.create();
		    	alert.show();	
		    	//Progress.setVisibility(View.INVISIBLE);
	    	} else {
	    		 Toast.makeText(getApplicationContext(), "Network not available", Toast.LENGTH_SHORT).show();
	    	}
	    }
	    	
	};
	
	 private class DoConverterConversionTask extends AsyncTask<Void, Void, Void> {

    	 @Override 
    	 protected void onCancelled() {
    		 // ToEdit.setEnabled(false);
    		 // ToEdit.setText("No Network");
    		 //converterText1.setText("No Network...");
    		 flipText("No Network...");
    		 converterSourceText.setText("");
    		 converterDateText.setText("");
    		 converterProgress1.setVisibility(View.INVISIBLE);
    		 FromSpinner.setEnabled(true);
    		 ToSpinner.setEnabled(true);
    		 swapImage.setEnabled(true);
    	 }
    	 
    	 @Override
		 protected void onPreExecute() {
		     if  (!preReadConv()) cancel(true); 
		     else  {
		    	 converterProgress1.setVisibility(View.VISIBLE);
		    	 FromSpinner.setEnabled(false);
		    	 ToSpinner.setEnabled(false);
		    	 swapImage.setEnabled(false);
		     
		     }
		 }
    
		 @Override
		 protected void onProgressUpdate(Void... values) {
		  // TODO Auto-generated method stub
		  //super.onProgressUpdate(values);
		 }

		 @Override
		 protected Void doInBackground(Void... arg0) { 
			//Get the conversion amount from yahoo...
		    
				try {
					getCurrencyConversion();
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		
		    return null;
		 }
		 
		 @Override
		 protected void onPostExecute(Void result) {
		  // Here, display the converted amount
		  displayCurrencyConversion();
		 }

		}
	 
	 private boolean preReadConv()
	 {
		  ConnectivityManager cm = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
		  NetworkInfo ni = cm.getActiveNetworkInfo();
		  if (ni != null && ni.isConnected()) networkAvailable=true;
		  else networkAvailable=false; 
		  
		  return networkAvailable;
	  }
	 
	 private void getCurrencyConversion() throws UnsupportedEncodingException, IOException {
		  
		 dataSource = SettingsPreferences.getDataSourceDefault(this);
		 
		 Calendar now = Calendar.getInstance(); //Todays' date
		 
		 if (dataSource.equals("Google")) {
			 ConverterGoogleHandler converterGoogleHandler = new ConverterGoogleHandler();
			  toAmount = converterGoogleHandler.returnRate("1",fromCurrency, toCurrency);	

		 } else if (dataSource.equals("Yahoo!")) {

			 currenciesYahoo.clear();
			 currenciesYahoo.add(new CurrencyYahoo(fromCurrency,toCurrency,"TODO"));
			 ConverterYahooHandler converterYahooHandler = new ConverterYahooHandler();
			 //toAmount = converterYahooHandler.returnRate("1",fromCurrency, toCurrency);	 
			 currenciesYahoo = converterYahooHandler.returnRate(currenciesYahoo);
			 
			 //We will only ever have one value in currenciesYahoo ArrayList in this Actvity, so just the first one...
			 toAmount = currenciesYahoo.get(0).getToAmount();
		 }
		 
		 //Save the values into the database, but first delete previous one if is there.
		 SQLiteDatabase db = currencyData.getWritableDatabase();
		 String deleteWhere = "fromCurrency = '" + fromCurrency + "' and toCurrency = '" + toCurrency + "'" + " AND " + 
		                      " source = '" + dataSource + "'";
		 int numdels = db.delete("currencydata",deleteWhere,null);
		 
		 //Log.d("DATABASE","Deleted : " + numdels + " rows");
		 
		 ContentValues values = new ContentValues();
		 values.put("fromCurrency", fromCurrency);
		 values.put("toCurrency", toCurrency);
		 values.put("amount", toAmount);
		 values.put("source", dataSource);
		 values.put("dateTime", dateFormat.format(now.getTime()));
		 
		 db.insertOrThrow("currencydata", null, values);
		 db.close();
	  }
	 
	  private void displayCurrencyConversion()
	  {  
		if ((toAmount == null) || (toAmount == "")) {
			//converterText1.setText("Not Available");
			flipText("Not Available.");
			converterSourceText.setText("");
			converterDateText.setText("");
			converterProgress1.setVisibility(View.INVISIBLE);
			
		} else {
			int fromSymbol = getCurrencyInt(fromCurrency);
			int toSymbol = getCurrencyInt(toCurrency);
			 if (SettingsPreferences.getShowSymbols(getApplicationContext())) 
			     flipText(currencySymbols[fromSymbol] + "1 " + fromCurrency + " = " + currencySymbols[toSymbol] + sortDecimals(toAmount) + " " + toCurrency );
		     else
		    	 flipText("1 " + fromCurrency + " = " + sortDecimals(toAmount) + " " + toCurrency );
			converterSourceText.setText("Source:" + dataSource.toString());
			Time now = new Time();
			now.setToNow();
			converterDateText.setText("As of: " + now.format("%Y-%m-%d %H:%M:%S"));
			converterProgress1.setVisibility(View.INVISIBLE);
		}  
		FromSpinner.setEnabled(true);
    	ToSpinner.setEnabled(true);
    	swapImage.setEnabled(true);
	  }
	 
	 private class doYahooChartTask extends AsyncTask<Void, Void, Void> {

    	 @Override 
    	 protected void onCancelled() {
		      chartImage1.setImageResource(R.drawable.z);	
		      chartHeader.setText(" Chart - ");
		      chartProgress2.setVisibility(View.INVISIBLE);    
    	 }
    	 
    	 @Override
		 protected void onPreExecute() {
		     if  (!preReadConv()) cancel(true);
 		    
		 }
    
		 @Override
		 protected void onProgressUpdate(Void... values) {
		  // TODO Auto-generated method stub
		  //super.onProgressUpdate(values);
		 }

		 @Override
		 protected Void doInBackground(Void... arg0) {
		  // Here, run ConverterHandler.returnRate
		  getYahooChart();
		  return null;
		 }
		 
		 @Override
		 protected void onPostExecute(Void result) {
		  // Here, display the converted amount
		  displayYahooChart();
		 }

		}
	 
	  private void displayYahooChart() {
		  String chartText = null;
			switch (chartRange) {
			case 0: chartText = "1 Day"; break;
			case 1: chartText = "1 Month"; break;
			case 2: chartText = "3 Months"; break;
			case 3: chartText = "1 Year"; break;
			case 4: chartText = "3 Years"; break;
			case 5: chartText = "5 Years"; break;
			}
			chartProgress2.setVisibility(View.INVISIBLE);
			//chartImage1.setImageDrawable(drawableChartImage);
			flipChart();
			chartHeader.setText(" Chart - " + chartText);
	  }
	  
	  private void getYahooChart() {
		  if (chartRange == null)   // Should be null only on app startup.
				downloadYahooChart(2);	
		      else
				downloadYahooChart(chartRange);
	  }
	  
	  private Drawable ImageOperations(Context context, String url, String saveFileName ) {
		  try {
				InputStream is = (InputStream) this.fetch(url);
				Drawable d = Drawable.createFromStream(is, "src");
				return d;
			} catch (MalformedURLException e) {
				e.printStackTrace();
				return null;
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		  
	  }
	  
	public Object fetch(String address) throws MalformedURLException,IOException {
			URL url = new URL(address);
			Object content = null;
			try {
			 content = url.getContent(); }
			catch (Exception e) {
				return null;
			}
			return content;
		}
		
	public void downloadYahooChart(int range) {
		
		String theRange = null;
		
		switch (range) {
		   case 0: theRange = "1d"; break;
		   case 1: theRange = "1m"; break;
		   case 2: theRange = "3m"; break;
		   case 3: theRange = "1y"; break;
		   case 4: theRange = "3y"; break;
		   case 5: theRange = "5y"; break;
		}

		StringBuilder chartUrl = new StringBuilder();
		chartUrl.append("http://chart.finance.yahoo.com/z?s=");
		chartUrl.append(fromCurrency.toString().trim());
		chartUrl.append(toCurrency.toString().trim());
		chartUrl.append("%3dX&t=");
		chartUrl.append(theRange);
		chartUrl.append("&q=l&l=off&z=m&a=v&p=s&lang=en-US&region=US");
	    drawableChartImage = ImageOperations(getApplicationContext(),chartUrl.toString(), "chart.jpg");
	}

}









