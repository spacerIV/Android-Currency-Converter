package com.hyperionsoft.currencyconverter;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.Tracker;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class NewsActivity extends Activity {

	public class RssLoadingTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected void onPostExecute(Void result) {
			// TODO Auto-generated method stub
			displayRss();
		}

		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			preReadRss();
		}

		@Override
		protected void onProgressUpdate(Void... values) {
			// TODO Auto-generated method stub
			// super.onProgressUpdate(values);
		}

		@Override
		protected Void doInBackground(Void... arg0) {
			// TODO Auto-generated method stub
			readRss();
			return null;
		}

	}

	private RssArrayAdapter Newsadapter;
	private String[] codeCountryCurr, currencyCodes;
	private Spinner NewsSpinner;
	private ListView RssList;
	private List<JSONObject> jobs;
	private String NewsCurrency = null;
	private ProgressBar NewsProgress;

	private final static String BOLD_OPEN = "<B>";
	private final static String BOLD_CLOSE = "</B>";
	private final static String BREAK = "<BR>";
	private final static String ITALIC_OPEN = "<I>";
	private final static String ITALIC_CLOSE = "</I>";
	private final static String SMALL_OPEN = "<SMALL>";
	private final static String SMALL_CLOSE = "</SMALL>";
	public List<RssArticle> articles;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.newsmain);

		codeCountryCurr = getResources().getStringArray(
				R.array.code_country_currency);
		currencyCodes = getResources().getStringArray(R.array.currency_codes);

		NewsSpinner = (Spinner) findViewById(R.id.NewsSpinner);
		RssList = (ListView) findViewById(R.id.totallythelist);

		NewsProgress = (ProgressBar) findViewById(R.id.newsProgress);
		NewsSpinner.setPrompt("Choose a Currency...");
		ArrayAdapter newsSpinnerAdapter = new MyCustomAdapter(this,
				R.layout.rowwithflag, codeCountryCurr);
		// ArrayAdapter newsSpinnerAdapter =
		// ArrayAdapter.createFromResource(this, R.array.code_country_currency,
		// R.layout.newsspinnerclosed);

		newsSpinnerAdapter.setDropDownViewResource(R.layout.rowwithflag);
		NewsSpinner.setAdapter(newsSpinnerAdapter);
		// NewsSpinner.setAdapter(new MyCustomAdapter(this, R.layout.row,
		// codeCountryCurr));

		NewsSpinner.setSelection(22); // Default EUR

		// No need to run this now, it should kick off in the
		// NewsSpinnerItemSelected Listener.
		// startReadRss();

		List<JSONObject> jobs = new ArrayList<JSONObject>();
		Newsadapter = new RssArrayAdapter(this, jobs);
		RssList.setAdapter(Newsadapter);

		// Is the 'remember last used currencies' checked in the Settings?
		if (SettingsPreferences.getRememberDefault(getApplicationContext())) {
			Integer newsCurr = getPreferences(MODE_PRIVATE).getInt(
					"NEWS_CURRENCY_REMEMBERED", 24); // 24=GBP
			if (newsCurr == null) {
				// Nothing set yet, most likely the very first run of the
				// program. Not sure if this is needed
				NewsCurrency = "GBP";
				NewsSpinner.setSelection(24);
			} else {
				NewsCurrency = currencyCodes[newsCurr];
				NewsSpinner.setSelection(newsCurr);
			}
		} else {
			// if not, then check if the news tab default currency is set..
			String newsCurrPref = SettingsPreferences
					.getNewsSelectedDefault(getApplicationContext());
			if (newsCurrPref == null) {
				// Nothing set yet, most likely the very first run of the
				// program. Not sure if this is needed
				NewsCurrency = "GBP";
				NewsSpinner.setSelection(24);
			}
			NewsSpinner.setSelection(getCurrencyInt(newsCurrPref));
			NewsCurrency = newsCurrPref;
		}

		// Inner classes provide callbacks that notifiy the app when an item has
		// been selected on a spinner.
		class NewsSpinnerItemSelected implements OnItemSelectedListener {

			public void onItemSelected(AdapterView<?> parent, View view,
					int pos, long id) {
				ConnectivityManager cm = (ConnectivityManager) getApplicationContext()
						.getSystemService(Context.CONNECTIVITY_SERVICE);
				NetworkInfo ni = cm.getActiveNetworkInfo();
				if (ni != null && ni.isConnected()) {
					// We have a connection ...
					NewsCurrency = currencyCodes[pos];
					// Log.d("News Currency","NewsCurrency =  " +
					// NewsCurrency.toString());
					Newsadapter.clear();
					startReadRss();
					// Analytics
					EasyTracker.getInstance();
			        Tracker myTracker = EasyTracker.getTracker();
					myTracker.sendEvent("ui_action", "NewsSpinner", NewsCurrency, id);
					
				} else {
					Toast.makeText(getApplicationContext(),
							"Network is Unavailable.", Toast.LENGTH_SHORT)
							.show();
					NewsProgress.setVisibility(ProgressBar.INVISIBLE);
				}

			}

			public void onNothingSelected(AdapterView<?> parent) {
				// Do nothing.
			}
		}

		NewsSpinner.setOnItemSelectedListener(new NewsSpinnerItemSelected());

		RssList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			public void onItemClick(AdapterView<?> parent, View view,
					int position, long arg3) {

				// Toast.makeText(getApplicationContext(),"You clicked : " +
				// position , Toast.LENGTH_SHORT).show();
				Intent i = new Intent(Intent.ACTION_VIEW);
				i.setData(Uri.parse(getUrlfromJobs(position)));
				startActivity(i);

			}
		});
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

	public String getUrlfromJobs(int pos) {
		String aUrl = new String();

		try {
			aUrl = jobs.get(pos).getString("url");
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return aUrl;
	}

	private void startReadRss() {
		new RssLoadingTask().execute();
	}

	private void preReadRss() {
		NewsProgress.setVisibility(ProgressBar.VISIBLE);
		// Toast.makeText(this, "Reading RSS, please wait.",
		// Toast.LENGTH_SHORT).show();
	}

	private void readRss() {

		try {
			jobs = getLatestRssFeed(NewsCurrency.toString());
		} catch (Exception e) {
			// Log.e("RSS ERROR", "Error loading RSS Feed Stream >> " +
			// e.getMessage() + " //" + e.toString());
		}

	}

	private void displayRss() {

		Newsadapter = new RssArrayAdapter(this, jobs);
		RssList.setAdapter(Newsadapter);
		NewsProgress.setVisibility(ProgressBar.INVISIBLE);
		if (jobs.size() > 0)
			Toast.makeText(
					this,
					jobs.size()
							+ " Articles found for "
							+ currencyCodes[NewsSpinner
									.getSelectedItemPosition()],
					Toast.LENGTH_SHORT).show();
		else
			Toast.makeText(
					this,
					"Couldn't find any news for "
							+ currencyCodes[NewsSpinner
									.getSelectedItemPosition()],
					Toast.LENGTH_SHORT).show();
	}

	public class MyCustomAdapter extends ArrayAdapter<String> {

		public MyCustomAdapter(Context context, int textViewResourceId,
				String[] objects) {
			super(context, textViewResourceId, objects);
			// TODO Auto-generated constructor stub
		}

		@Override
		public View getDropDownView(int position, View convertView,
				ViewGroup parent) {
			// TODO Auto-generated method stub
			return getCustomView(position, convertView, parent);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// TODO Auto-generated method stub
			return getCustomView(position, convertView, parent);
		}

		public View getCustomView(int position, View convertView,
				ViewGroup parent) {
			// TODO Auto-generated method stub
			// return super.getView(position, convertView, parent);

			LayoutInflater inflater = getLayoutInflater();
			if (convertView == null) {
				convertView = inflater.inflate(R.layout.rowwithflag, parent,
						false);
			}
			// View row=inflater.inflate(R.layout.row, parent, false);
			TextView label = (TextView) convertView
					.findViewById(R.id.currencyRow1);
			// label.setText(codeCountryCurr[position]);
			ImageView icon = (ImageView) convertView
					.findViewById(R.id.flagRow1);

			if (currencyCodes[position].equals("AED")) {
				icon.setImageResource(R.drawable.aebig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("ANG")) {
				icon.setImageResource(R.drawable.anbig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("ARS")) {
				icon.setImageResource(R.drawable.arbig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("AUD")) {
				icon.setImageResource(R.drawable.aubig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("BDT")) {
				icon.setImageResource(R.drawable.bdbig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("BGN")) {
				icon.setImageResource(R.drawable.bgbig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("BHD")) {
				icon.setImageResource(R.drawable.bhbig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("BND")) {
				icon.setImageResource(R.drawable.bnbig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("BOB")) {
				icon.setImageResource(R.drawable.bobig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("BRL")) {
				icon.setImageResource(R.drawable.brbig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("BWP")) {
				icon.setImageResource(R.drawable.bwbig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("CAD")) {
				icon.setImageResource(R.drawable.cabig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("CHF")) {
				icon.setImageResource(R.drawable.chbig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("CLP")) {
				icon.setImageResource(R.drawable.clbig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("CNY")) {
				icon.setImageResource(R.drawable.cnbig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("COP")) {
				icon.setImageResource(R.drawable.cobig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("CRC")) {
				icon.setImageResource(R.drawable.crbig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("CZK")) {
				icon.setImageResource(R.drawable.czbig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("DKK")) {
				icon.setImageResource(R.drawable.dkbig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("DOP")) {
				icon.setImageResource(R.drawable.dmbig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("DZD")) {
				icon.setImageResource(R.drawable.dzbig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("EGP")) {
				icon.setImageResource(R.drawable.egbig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("EUR")) {
				icon.setImageResource(R.drawable.eubig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("FJD")) {
				icon.setImageResource(R.drawable.fjbig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("GBP")) {
				icon.setImageResource(R.drawable.gbbig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("HKD")) {
				icon.setImageResource(R.drawable.hkbig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("HNL")) {
				icon.setImageResource(R.drawable.hnbig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("HRK")) {
				icon.setImageResource(R.drawable.hrbig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("HUF")) {
				icon.setImageResource(R.drawable.hubig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("ILS")) {
				icon.setImageResource(R.drawable.ilbig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("INR")) {
				icon.setImageResource(R.drawable.irbig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("ISK")) {
				icon.setImageResource(R.drawable.isbig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("JMD")) {
				icon.setImageResource(R.drawable.jmbig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("JOD")) {
				icon.setImageResource(R.drawable.jobig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("JPY")) {
				icon.setImageResource(R.drawable.jpbig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("KES")) {
				icon.setImageResource(R.drawable.kebig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("KRW")) {
				icon.setImageResource(R.drawable.krbig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("KWD")) {
				icon.setImageResource(R.drawable.kwbig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("KYD")) {
				icon.setImageResource(R.drawable.kybig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("KZT")) {
				icon.setImageResource(R.drawable.kzbig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("LBP")) {
				icon.setImageResource(R.drawable.lbbig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("LKR")) {
				icon.setImageResource(R.drawable.lkbig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("LTL")) {
				icon.setImageResource(R.drawable.ltbig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("LVL")) {
				icon.setImageResource(R.drawable.lvbig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("MAD")) {
				icon.setImageResource(R.drawable.mabig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("MDL")) {
				icon.setImageResource(R.drawable.mdbig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("MKD")) {
				icon.setImageResource(R.drawable.mkbig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("MUR")) {
				icon.setImageResource(R.drawable.mubig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("MVR")) {
				icon.setImageResource(R.drawable.mvbig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("MXN")) {
				icon.setImageResource(R.drawable.mxbig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("MYR")) {
				icon.setImageResource(R.drawable.mybig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("NAD")) {
				icon.setImageResource(R.drawable.nabig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("NGN")) {
				icon.setImageResource(R.drawable.ngbig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("NIO")) {
				icon.setImageResource(R.drawable.nibig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("NOK")) {
				icon.setImageResource(R.drawable.nobig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("NPR")) {
				icon.setImageResource(R.drawable.npbig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("NZD")) {
				icon.setImageResource(R.drawable.nzbig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("OMR")) {
				icon.setImageResource(R.drawable.ombig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("PEN")) {
				icon.setImageResource(R.drawable.pebig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("PGK")) {
				icon.setImageResource(R.drawable.pgbig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("PHP")) {
				icon.setImageResource(R.drawable.phbig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("PKR")) {
				icon.setImageResource(R.drawable.pkbig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("PLN")) {
				icon.setImageResource(R.drawable.plbig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("PYG")) {
				icon.setImageResource(R.drawable.pybig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("QAR")) {
				icon.setImageResource(R.drawable.qabig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("RON")) {
				icon.setImageResource(R.drawable.robig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("RSD")) {
				icon.setImageResource(R.drawable.rsbig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("RUB")) {
				icon.setImageResource(R.drawable.rubig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("SAR")) {
				icon.setImageResource(R.drawable.sabig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("SCR")) {
				icon.setImageResource(R.drawable.scbig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("SEK")) {
				icon.setImageResource(R.drawable.sebig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("SGD")) {
				icon.setImageResource(R.drawable.sgbig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("SKK")) {
				icon.setImageResource(R.drawable.skbig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("SLL")) {
				icon.setImageResource(R.drawable.slbig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("SVC")) {
				icon.setImageResource(R.drawable.svbig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("THB")) {
				icon.setImageResource(R.drawable.thbig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("TND")) {
				icon.setImageResource(R.drawable.tnbig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("TRY")) {
				icon.setImageResource(R.drawable.trbig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("TTD")) {
				icon.setImageResource(R.drawable.ttbig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("TWD")) {
				icon.setImageResource(R.drawable.twbig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("TZS")) {
				icon.setImageResource(R.drawable.tzbig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("UAH")) {
				icon.setImageResource(R.drawable.uabig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("UGX")) {
				icon.setImageResource(R.drawable.ugbig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("USD")) {
				icon.setImageResource(R.drawable.usbig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("UYU")) {
				icon.setImageResource(R.drawable.uybig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("UZS")) {
				icon.setImageResource(R.drawable.uzbig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("VEF")) {
				icon.setImageResource(R.drawable.vebig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("VND")) {
				icon.setImageResource(R.drawable.vnbig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("YER")) {
				icon.setImageResource(R.drawable.yebig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("ZAR")) {
				icon.setImageResource(R.drawable.zabig);
				label.setText(codeCountryCurr[position]);
			}
			if (currencyCodes[position].equals("ZMK")) {
				icon.setImageResource(R.drawable.zmbig);
				label.setText(codeCountryCurr[position]);
			}

			return convertView;

		}

	}

	@Override
	protected void onPause() {
		super.onPause();
		// Save the news currency
		getPreferences(MODE_PRIVATE)
				.edit()
				.putInt("NEWS_CURRENCY_REMEMBERED",
						NewsSpinner.getSelectedItemPosition()).commit();

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		menu.add(0, 0, 0, "Reload");
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		switch (item.getItemId()) {
		case (0):
			startReadRss();
			break;
		default:
			break;
		}

		return true;
	}

	private int getCurrencyInt(String newsCurrPref) {
		int cnt = 0;
		for (String thecode : currencyCodes) {
			if (thecode.contentEquals(newsCurrPref.trim()))
				break;
			cnt++;
		}
		return cnt;
	}

	/**
	 * This method defines a feed URL and then calles our SAX Handler to read
	 * the article list from the stream
	 * 
	 * @return List<JSONObject> - suitable for the List View activity
	 */
	public static List<JSONObject> getLatestRssFeed(String currencyCode) {

		StringBuffer feed = new StringBuffer();
		feed.append("http://www.google.com/finance/company_news?q=CURRENCY:");
		// feed.append("EUR");
		feed.append(currencyCode.toString());
		feed.append("&output=rss");

		// http://www.google.com/finance/company_news?q=CURRENCY:ZAR&output=rss
		// String feed =
		// "http://www.ibm.com/developerworks/views/rss/customrssatom.jsp?zone_by=XML&zone_by=Java&zone_by=Rational&zone_by=Linux&zone_by=Open+source&zone_by=WebSphere&type_by=Tutorials&search_by=&day=1&month=06&year=2007&max_entries=20&feed_by=rss&isGUI=true&Submit.x=48&Submit.y=14%22";
		// http://www.ibm.com/developerworks/views/rss/customrssatom.jsp?zone_by=XML&zone_by=Java&zone_by=Rational&zone_by=Linux&zone_by=Open+source&zone_by=WebSphere&type_by=Tutorials&search_by=&day=1&month=06&year=2007&max_entries=20&feed_by=rss&isGUI=true&Submit.x=48&Submit.y=14%22;

		RssHandler rh = new RssHandler();
		List<RssArticle> articles = rh.getLatestArticles(feed.toString());
		// Log.e("RSS ERROR", "Number of articles " + articles.size());
		return fillData(articles);
	}

	public static List<JSONObject> fillData(List<RssArticle> articles) {

		List<JSONObject> items = new ArrayList<JSONObject>();

		for (RssArticle article : articles) {
			JSONObject current = new JSONObject();
			try {
				buildJsonObject(article, current);
			} catch (JSONException e) {
				// Log.e("RSS ERROR",
				// "Error creating JSON Object from RSS feed");
			}
			items.add(current);

		}

		return items;
	}

	/**
	 * This method takes a single Article Object and converts it in to a single
	 * JSON object including some additional HTML formating so they can be
	 * displayed nicely
	 * 
	 * @param article
	 * @param current
	 * @throws JSONException
	 */
	public static void buildJsonObject(RssArticle article, JSONObject current)
			throws JSONException {
		String title = article.getTitle();
		String description = article.getDescription();
		String date = article.getPubDate();

		StringBuffer sb = new StringBuffer();
		sb.append(BOLD_OPEN).append(title).append(BOLD_CLOSE);
		sb.append(BREAK);
		sb.append(description);
		sb.append(BREAK);
		sb.append(SMALL_OPEN).append(ITALIC_OPEN).append(date)
				.append(ITALIC_CLOSE).append(SMALL_CLOSE);

		current.put("text", Html.fromHtml(sb.toString()));
		current.put("url", article.getUrl());
	}

}