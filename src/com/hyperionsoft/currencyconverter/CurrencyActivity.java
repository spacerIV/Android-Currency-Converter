package com.hyperionsoft.currencyconverter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.RejectedExecutionException;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.Tracker;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class CurrencyActivity extends ListActivity {

	private String[] codeCountryCurr, currencyCodes, currencySymbols;
	private boolean networkAvailable1 = false;
	private int listPosition;
	private String newAmountString;
	private List<CurrencyQuote> currencies = new ArrayList<CurrencyQuote>();
	private List<String> currenciesToAdd = new ArrayList<String>();
	private ArrayAdapter<String> myCurrencyActivityAdapter;
	private AlertDialog.Builder footerDialog;
	private ArrayAdapter<String> footerAdapter;
	private DecimalFormat decFormat1, decFormat2, decFormat3, decFormat4,
			decFormat5, decFormat6;
	private String numberOfDecimals;
	private String dataSource;
	private String updateFrequency;
	private CurrencyData currencyData; // db class
	private SimpleDateFormat dateFormat;
	private String[] sqlFromCols1 = { "amount", "source", "dateTime" };

	private ListView lv;
	private boolean lvIsClickable = true;
	private String baseCurrencyAmount;
	private List<CurrencyYahoo> currenciesYahoo = new ArrayList<CurrencyYahoo>();
	private Typeface tf;
	private ProgressBar currencyProgress2;
	private TextView currencyHeader1, currencyHeader2;

	public static final String SAVE_CURRENCIES = "CurrencyActivityPrefs";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.currencymain);

		codeCountryCurr = getResources().getStringArray(
				R.array.code_country_currency);
		currencyCodes = getResources().getStringArray(R.array.currency_codes);
		currencySymbols = getResources().getStringArray(
				R.array.currency_symbols);

		tf = Typeface.createFromAsset(getAssets(), "DejaVuSans.ttf");

		currencyHeader1 = (TextView) findViewById(R.id.currencyheader1);
		currencyHeader1.setText("  Source: "
				+ SettingsPreferences
						.getDataSourceDefault(getApplicationContext()));
		// currencyHeader2 = (TextView) findViewById(R.id.currencyHeader2);
		// currencyHeader2.setText("Update Frequency: " +
		// SettingsPreferences.getUpdateFrequencyDefault(getApplicationContext()));
		currencyProgress2 = (ProgressBar) findViewById(R.id.currencyProgress2);
		currencyProgress2.setVisibility(View.INVISIBLE);

		// Restore last currencies, if null (first start) then give default
		// values)
		String savedCodes = getPreferences(MODE_PRIVATE).getString(
				SAVE_CURRENCIES, null);
		if (savedCodes == null) {
			// First CurrencyQuote object should set the static variable
			// baseCurr.
			CurrencyQuote cq = new CurrencyQuote("USD", "1");
			cq.setbaseCurr("USD");
			currencies.add(cq);
			currencies.add(new CurrencyQuote("EUR", "Working..."));
			currencies.add(new CurrencyQuote("GBP", "Working..."));
		} else {
			// Have some saved currencies so add em...
			StringTokenizer st = new StringTokenizer(savedCodes, ",");
			while (st.hasMoreTokens()) {
				currencies.add(new CurrencyQuote(st.nextToken(), "Working..."));
			}
			// NOTE THIS IS DEBUG SHIT, SET THE FIRST ONE TO baseCurr.
			currencies.get(0).setbaseCurr(currencies.get(0).getCurrCode());
			currencies.get(0).setAmount("1.00");
		}

		currencyData = new CurrencyData(this);
		dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		decFormat1 = new DecimalFormat("######0.0");
		decFormat2 = new DecimalFormat("######0.00");
		decFormat3 = new DecimalFormat("######0.000");
		decFormat4 = new DecimalFormat("######0.0000");
		decFormat5 = new DecimalFormat("######0.00000");
		decFormat6 = new DecimalFormat("######0.000000");

		// Get NUMBER_OF_DECIMALS preference...
		numberOfDecimals = SettingsPreferences.getNumberOfDecimalsDefault(this);
		dataSource = SettingsPreferences.getDataSourceDefault(this);

		lv = getListView();
		View footerView = getLayoutInflater().inflate(
				R.layout.currencylistfooter, null);
		lv.addFooterView(footerView);

		myCurrencyActivityAdapter = new myCurrencyActivityAdapter(this,
				currencies);
		setListAdapter(myCurrencyActivityAdapter);

		footerAdapter = new footerAdapter(this, currenciesToAdd);
		footerDialog = new AlertDialog.Builder(this);

		footerDialog.setTitle("Select a currency");
		footerDialog.setAdapter(footerAdapter, new OnClickListener() {

			public void onClick(DialogInterface dialog, int which) {
				if (!lvIsClickable) {
					Toast.makeText(getApplicationContext(), "Working...",
							Toast.LENGTH_SHORT).show();
				} else {

					// Toast.makeText(getApplicationContext(),
					// "Dude you selected.." + which,
					// Toast.LENGTH_SHORT).show();
					String code = currenciesToAdd.get(which);
					currencies.add(new CurrencyQuote(code, "Working..."));
					myCurrencyActivityAdapter.notifyDataSetChanged();
					startGetCurrencyConversion();
					// myCurrencyActivityAdapter.notifyDataSetChanged();
					
					EasyTracker.getInstance();
			        Tracker myTracker = EasyTracker.getTracker();
					myTracker.sendEvent("ui_action", "ListAddCurrency", code, (long) which);
				}
			}

		});

		lv.setOnItemLongClickListener(new OnItemLongClickListener() {

			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int position, long id) {

				if (!lvIsClickable) {
					Toast.makeText(getApplicationContext(), "Working...",
							Toast.LENGTH_SHORT).show();
					return true;
				}

				CurrencyQuote o = (CurrencyQuote) getListAdapter().getItem(
						position);
				int header = getCurrencyInt(o.getCurrCode());
				listPosition = position;

				final CharSequence[] items = { "Set Amount", "Remove currency" };
				AlertDialog.Builder builder = new AlertDialog.Builder(
						CurrencyActivity.this);
				builder.setTitle(codeCountryCurr[header].toString());
				builder.setIcon(getFlag(o.getCurrCode()));

				builder.setItems(items, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int item) {

						switch (item) {
						case 0:
							showConvertDialog(listPosition);
							break;
						case 1:
							if (currencies.size() == 1) {
								Toast.makeText(getApplicationContext(),
										"Leave at least one currency.",
										Toast.LENGTH_SHORT).show();
							} else {
								// If removing the baseCurr, then set baseCurr
								// to nothing.
								if (currencies
										.get(listPosition)
										.getCurrCode()
										.equals(currencies.get(listPosition)
												.getBaseCurr())) {
									currencies.get(listPosition)
											.setbaseCurr("");
								}
								currencies.remove(listPosition);
								myCurrencyActivityAdapter
										.notifyDataSetChanged();
							}
							break;
						}

					}
				});

				AlertDialog alert = builder.create();
				alert.show();
				return true;
			}

		});

		startGetCurrencyConversion();

		purgeDB(); // Remove old records from db.

		// lets download this db to SD card for debug //
		SQLiteDatabase dbdb = currencyData.getReadableDatabase();
		String whereClause = "1=1";
		String[] mySQLyo = { "fromCurrency", "toCurrency", "amount", "source",
				"dateTime" };
		Cursor curs = dbdb.query("currencydata", mySQLyo, whereClause, null,
				null, null, null);
		int cnt = curs.getCount();
		if (cnt > 0) {
			try {
				File root = Environment.getExternalStorageDirectory();
				if (root.canWrite()) {
					File myOutputFile = new File(root, "currency.txt");
					FileWriter myFileWriter = new FileWriter(myOutputFile);
					BufferedWriter out = new BufferedWriter(myFileWriter);
					while (curs.moveToNext()) {
						out.write(curs.getString(0));
						out.write(",");
						out.write(curs.getString(1));
						out.write(",");
						out.write(curs.getString(2));
						out.write(",");
						out.write(curs.getString(3));
						out.write(",");
						out.write(curs.getString(4));
						out.write(",");
						out.write("\n");
					}
					out.close();
					dbdb.close();
					curs.close();
				}
			} catch (IOException e) {
			}
		}

		// App rater code!
		AppRater.app_launched(this);

	} // End onCreate()

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

	private void purgeDB() {

		Calendar aWeekAgo = Calendar.getInstance();

		aWeekAgo.add(Calendar.DAY_OF_YEAR, -2);

		SQLiteDatabase dbpurge = currencyData.getReadableDatabase();
		String whereClause = " dateTime <= '"
				+ dateFormat.format(aWeekAgo.getTime()) + "'";
		int numdels = dbpurge.delete("currencydata", whereClause, null);

		// Toast.makeText(getApplicationContext(), numdels +
		// " records purged...", Toast.LENGTH_SHORT).show();
	}

	@Override
	protected void onPause() {
		super.onPause();
		StringBuilder codes = new StringBuilder();
		for (CurrencyQuote q : currencies) {
			codes.append(q.getCurrCode());
			codes.append(",");
		}

		// Save the current currencies to SAVE_CURRENCIES, removing the last
		// comma
		getPreferences(MODE_PRIVATE).edit()
				.putString(SAVE_CURRENCIES, codes.toString()).commit();
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		if (!lvIsClickable) {
			Toast.makeText(getApplicationContext(), "Working...",
					Toast.LENGTH_SHORT).show();
		} else {

			if (position == currencies.size()) {
				// Footer so show add currencies dialog...but first remove
				// currency already in List<CurrencyQuotes>currencies

				footerAdapter.clear();
				currenciesToAdd.clear();
				// Add all.
				for (String code : currencyCodes)
					currenciesToAdd.add(code);

				// Remove the ones we've got already
				for (CurrencyQuote q : currencies)
					if (currenciesToAdd.contains(q.getCurrCode()))
						currenciesToAdd.remove(q.getCurrCode());

				footerAdapter.notifyDataSetChanged();
				footerDialog.show();

			} else {
				// Show convert dialog
				showConvertDialog(position);
			}
		}
	}

	private void showConvertDialog(int position) {

		final CurrencyQuote o = (CurrencyQuote) getListAdapter().getItem(
				position);
		int header = getCurrencyInt(o.getCurrCode());
		LayoutInflater factory = LayoutInflater.from(this);
		View textEntryView = factory.inflate(R.layout.currencysetamount, null);
		final TextView newAmountText = (TextView) textEntryView
				.findViewById(R.id.currency_amount_edit);
		// textEntryView.findViewById(id)
		new AlertDialog.Builder(this)
				.setIcon(getFlag(o.getCurrCode()))
				.setTitle(codeCountryCurr[header])
				.setView(textEntryView)
				.setPositiveButton("Convert",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								// Call asynctask to get new values...first
								// check network...
								ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
								NetworkInfo ni = cm.getActiveNetworkInfo();
								if (ni != null && ni.isConnected()) {
									networkAvailable1 = true;
									/* User clicked OK so do some stuff */
									newAmountString = newAmountText.getText()
											.toString();
									o.setAmount(newAmountString);
									o.setbaseCurr(o.getCurrCode());
									myCurrencyActivityAdapter
											.notifyDataSetChanged();
									startGetCurrencyConversion();
								} else {
									networkAvailable1 = false;
									Toast.makeText(getApplicationContext(),
											"Network Unavailable...",
											Toast.LENGTH_SHORT).show();
								}
							}
						})
				.setNegativeButton("Cancel",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {

								/* User clicked cancel so do some stuff */
							}
						}).create().show();

	}

	public class myCurrencyActivityAdapter extends ArrayAdapter<String> {

		private final Activity activity;
		private final List<CurrencyQuote> currencies;

		public myCurrencyActivityAdapter(Activity activity, List objects) {
			super(activity, R.layout.currencyrow, objects);
			this.activity = activity;
			this.currencies = objects;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			LayoutInflater inflater = getLayoutInflater();
			if (convertView == null) {
				convertView = inflater.inflate(R.layout.currencyrow, parent,
						false);
			}
			TextView topLabel = (TextView) convertView
					.findViewById(R.id.currencytoptext);
			TextView bottomLabel = (TextView) convertView
					.findViewById(R.id.currencybottomtext);
			TextView currencySymbol = (TextView) convertView
					.findViewById(R.id.currencySymbol);
			currencySymbol.setTypeface(tf);

			CurrencyQuote currentCurrency = (CurrencyQuote) currencies
					.get(position);

			int pos = getCurrencyInt(currentCurrency.getCurrCode());
			String currCode = codeCountryCurr[pos];
			topLabel.setText(codeCountryCurr[pos]);
			String baseCurr = currentCurrency.getBaseCurr();
			if (currentCurrency.getCurrCode().equals(baseCurr)) {
				bottomLabel.setTextColor(getResources().getColor(R.color.red));
			} else
				bottomLabel.setTextColor(getResources().getColor(
						R.color.darkblue));

			if ((currentCurrency.getAmount().equals("Working..."))
					|| (currentCurrency.getAmount().equals(""))
					|| (currentCurrency.getAmount().equals("Not Available"))
					|| (currentCurrency.getAmount().equals("No Network"))) {
				bottomLabel.setText(currentCurrency.getAmount());
				currencySymbol.setText("");
			} else {
				// dateLabel.setText(currentCurrency.get
				String str = currentCurrency.getAmount().replaceAll("�", "");
				numberOfDecimals = SettingsPreferences
						.getNumberOfDecimalsDefault(getApplicationContext());
				String toAmtDouble = null;
				switch (Integer.parseInt(numberOfDecimals)) {
				case 1:
					toAmtDouble = decFormat1.format(Double.parseDouble(str));
					break;
				case 2:
					toAmtDouble = decFormat2.format(Double.parseDouble(str));
					break;
				case 3:
					toAmtDouble = decFormat3.format(Double.parseDouble(str));
					break;
				case 4:
					toAmtDouble = decFormat4.format(Double.parseDouble(str));
					break;
				case 5:
					toAmtDouble = decFormat5.format(Double.parseDouble(str));
					break;
				case 6:
					toAmtDouble = decFormat6.format(Double.parseDouble(str));
					break;
				}

				if (SettingsPreferences.getShowSymbols(getApplicationContext()))
					currencySymbol.setText(currencySymbols[pos]);
				else
					currencySymbol.setText("");

				bottomLabel.setText(toAmtDouble);

			}

			ImageView icon = (ImageView) convertView
					.findViewById(R.id.currencyicon);

			if (currentCurrency.getCurrCode().equals("AED")) {
				icon.setImageResource(R.drawable.aebig);
			}
			if (currentCurrency.getCurrCode().equals("ANG")) {
				icon.setImageResource(R.drawable.anbig);
			}
			if (currentCurrency.getCurrCode().equals("ARS")) {
				icon.setImageResource(R.drawable.arbig);
			}
			if (currentCurrency.getCurrCode().equals("AUD")) {
				icon.setImageResource(R.drawable.aubig);
			}
			if (currentCurrency.getCurrCode().equals("BDT")) {
				icon.setImageResource(R.drawable.bdbig);
			}
			if (currentCurrency.getCurrCode().equals("BGN")) {
				icon.setImageResource(R.drawable.bgbig);
			}
			if (currentCurrency.getCurrCode().equals("BHD")) {
				icon.setImageResource(R.drawable.bhbig);
			}
			if (currentCurrency.getCurrCode().equals("BND")) {
				icon.setImageResource(R.drawable.bnbig);
			}
			if (currentCurrency.getCurrCode().equals("BOB")) {
				icon.setImageResource(R.drawable.bobig);
			}
			if (currentCurrency.getCurrCode().equals("BRL")) {
				icon.setImageResource(R.drawable.brbig);
			}
			if (currentCurrency.getCurrCode().equals("BWP")) {
				icon.setImageResource(R.drawable.bwbig);
			}
			if (currentCurrency.getCurrCode().equals("CAD")) {
				icon.setImageResource(R.drawable.cabig);
			}
			if (currentCurrency.getCurrCode().equals("CHF")) {
				icon.setImageResource(R.drawable.chbig);
			}
			if (currentCurrency.getCurrCode().equals("CLP")) {
				icon.setImageResource(R.drawable.clbig);
			}
			if (currentCurrency.getCurrCode().equals("CNY")) {
				icon.setImageResource(R.drawable.cnbig);
			}
			if (currentCurrency.getCurrCode().equals("COP")) {
				icon.setImageResource(R.drawable.cobig);
			}
			if (currentCurrency.getCurrCode().equals("CRC")) {
				icon.setImageResource(R.drawable.crbig);
			}
			if (currentCurrency.getCurrCode().equals("CZK")) {
				icon.setImageResource(R.drawable.czbig);
			}
			if (currentCurrency.getCurrCode().equals("DKK")) {
				icon.setImageResource(R.drawable.dkbig);
			}
			if (currentCurrency.getCurrCode().equals("DOP")) {
				icon.setImageResource(R.drawable.dmbig);
			}
			if (currentCurrency.getCurrCode().equals("DZD")) {
				icon.setImageResource(R.drawable.dzbig);
			}
			if (currentCurrency.getCurrCode().equals("EGP")) {
				icon.setImageResource(R.drawable.egbig);
			}
			if (currentCurrency.getCurrCode().equals("EUR")) {
				icon.setImageResource(R.drawable.eubig);
			}
			if (currentCurrency.getCurrCode().equals("FJD")) {
				icon.setImageResource(R.drawable.fjbig);
			}
			if (currentCurrency.getCurrCode().equals("GBP")) {
				icon.setImageResource(R.drawable.gbbig);
			}
			if (currentCurrency.getCurrCode().equals("HKD")) {
				icon.setImageResource(R.drawable.hkbig);
			}
			if (currentCurrency.getCurrCode().equals("HNL")) {
				icon.setImageResource(R.drawable.hnbig);
			}
			if (currentCurrency.getCurrCode().equals("HRK")) {
				icon.setImageResource(R.drawable.hrbig);
			}
			if (currentCurrency.getCurrCode().equals("HUF")) {
				icon.setImageResource(R.drawable.hubig);
			}
			if (currentCurrency.getCurrCode().equals("ILS")) {
				icon.setImageResource(R.drawable.ilbig);
			}
			if (currentCurrency.getCurrCode().equals("INR")) {
				icon.setImageResource(R.drawable.irbig);
			}
			if (currentCurrency.getCurrCode().equals("ISK")) {
				icon.setImageResource(R.drawable.isbig);
			}
			if (currentCurrency.getCurrCode().equals("JMD")) {
				icon.setImageResource(R.drawable.jmbig);
			}
			if (currentCurrency.getCurrCode().equals("JOD")) {
				icon.setImageResource(R.drawable.jobig);
			}
			if (currentCurrency.getCurrCode().equals("JPY")) {
				icon.setImageResource(R.drawable.jpbig);
			}
			if (currentCurrency.getCurrCode().equals("KES")) {
				icon.setImageResource(R.drawable.kebig);
			}
			if (currentCurrency.getCurrCode().equals("KRW")) {
				icon.setImageResource(R.drawable.krbig);
			}
			if (currentCurrency.getCurrCode().equals("KWD")) {
				icon.setImageResource(R.drawable.kwbig);
			}
			if (currentCurrency.getCurrCode().equals("KYD")) {
				icon.setImageResource(R.drawable.kybig);
			}
			if (currentCurrency.getCurrCode().equals("KZT")) {
				icon.setImageResource(R.drawable.kzbig);
			}
			if (currentCurrency.getCurrCode().equals("LBP")) {
				icon.setImageResource(R.drawable.lbbig);
			}
			if (currentCurrency.getCurrCode().equals("LKR")) {
				icon.setImageResource(R.drawable.lkbig);
			}
			if (currentCurrency.getCurrCode().equals("LTL")) {
				icon.setImageResource(R.drawable.ltbig);
			}
			if (currentCurrency.getCurrCode().equals("LVL")) {
				icon.setImageResource(R.drawable.lvbig);
			}
			if (currentCurrency.getCurrCode().equals("MAD")) {
				icon.setImageResource(R.drawable.mabig);
			}
			if (currentCurrency.getCurrCode().equals("MDL")) {
				icon.setImageResource(R.drawable.mdbig);
			}
			if (currentCurrency.getCurrCode().equals("MKD")) {
				icon.setImageResource(R.drawable.mkbig);
			}
			if (currentCurrency.getCurrCode().equals("MUR")) {
				icon.setImageResource(R.drawable.mubig);
			}
			if (currentCurrency.getCurrCode().equals("MVR")) {
				icon.setImageResource(R.drawable.mvbig);
			}
			if (currentCurrency.getCurrCode().equals("MXN")) {
				icon.setImageResource(R.drawable.mxbig);
			}
			if (currentCurrency.getCurrCode().equals("MYR")) {
				icon.setImageResource(R.drawable.mybig);
			}
			if (currentCurrency.getCurrCode().equals("NAD")) {
				icon.setImageResource(R.drawable.nabig);
			}
			if (currentCurrency.getCurrCode().equals("NGN")) {
				icon.setImageResource(R.drawable.ngbig);
			}
			if (currentCurrency.getCurrCode().equals("NIO")) {
				icon.setImageResource(R.drawable.nibig);
			}
			if (currentCurrency.getCurrCode().equals("NOK")) {
				icon.setImageResource(R.drawable.nobig);
			}
			if (currentCurrency.getCurrCode().equals("NPR")) {
				icon.setImageResource(R.drawable.npbig);
			}
			if (currentCurrency.getCurrCode().equals("NZD")) {
				icon.setImageResource(R.drawable.nzbig);
			}
			if (currentCurrency.getCurrCode().equals("OMR")) {
				icon.setImageResource(R.drawable.ombig);
			}
			if (currentCurrency.getCurrCode().equals("PEN")) {
				icon.setImageResource(R.drawable.pebig);
			}
			if (currentCurrency.getCurrCode().equals("PGK")) {
				icon.setImageResource(R.drawable.pgbig);
			}
			if (currentCurrency.getCurrCode().equals("PHP")) {
				icon.setImageResource(R.drawable.phbig);
			}
			if (currentCurrency.getCurrCode().equals("PKR")) {
				icon.setImageResource(R.drawable.pkbig);
			}
			if (currentCurrency.getCurrCode().equals("PLN")) {
				icon.setImageResource(R.drawable.plbig);
			}
			if (currentCurrency.getCurrCode().equals("PYG")) {
				icon.setImageResource(R.drawable.pybig);
			}
			if (currentCurrency.getCurrCode().equals("QAR")) {
				icon.setImageResource(R.drawable.qabig);
			}
			if (currentCurrency.getCurrCode().equals("RON")) {
				icon.setImageResource(R.drawable.robig);
			}
			if (currentCurrency.getCurrCode().equals("RSD")) {
				icon.setImageResource(R.drawable.rsbig);
			}
			if (currentCurrency.getCurrCode().equals("RUB")) {
				icon.setImageResource(R.drawable.rubig);
			}
			if (currentCurrency.getCurrCode().equals("SAR")) {
				icon.setImageResource(R.drawable.sabig);
			}
			if (currentCurrency.getCurrCode().equals("SCR")) {
				icon.setImageResource(R.drawable.scbig);
			}
			if (currentCurrency.getCurrCode().equals("SEK")) {
				icon.setImageResource(R.drawable.sebig);
			}
			if (currentCurrency.getCurrCode().equals("SGD")) {
				icon.setImageResource(R.drawable.sgbig);
			}
			if (currentCurrency.getCurrCode().equals("SKK")) {
				icon.setImageResource(R.drawable.skbig);
			}
			if (currentCurrency.getCurrCode().equals("SLL")) {
				icon.setImageResource(R.drawable.slbig);
			}
			if (currentCurrency.getCurrCode().equals("SVC")) {
				icon.setImageResource(R.drawable.svbig);
			}
			if (currentCurrency.getCurrCode().equals("THB")) {
				icon.setImageResource(R.drawable.thbig);
			}
			if (currentCurrency.getCurrCode().equals("TND")) {
				icon.setImageResource(R.drawable.tnbig);
			}
			if (currentCurrency.getCurrCode().equals("TRY")) {
				icon.setImageResource(R.drawable.trbig);
			}
			if (currentCurrency.getCurrCode().equals("TTD")) {
				icon.setImageResource(R.drawable.ttbig);
			}
			if (currentCurrency.getCurrCode().equals("TWD")) {
				icon.setImageResource(R.drawable.twbig);
			}
			if (currentCurrency.getCurrCode().equals("TZS")) {
				icon.setImageResource(R.drawable.tzbig);
			}
			if (currentCurrency.getCurrCode().equals("UAH")) {
				icon.setImageResource(R.drawable.uabig);
			}
			if (currentCurrency.getCurrCode().equals("UGX")) {
				icon.setImageResource(R.drawable.ugbig);
			}
			if (currentCurrency.getCurrCode().equals("USD")) {
				icon.setImageResource(R.drawable.usbig);
			}
			if (currentCurrency.getCurrCode().equals("UYU")) {
				icon.setImageResource(R.drawable.uybig);
			}
			if (currentCurrency.getCurrCode().equals("UZS")) {
				icon.setImageResource(R.drawable.uzbig);
			}
			if (currentCurrency.getCurrCode().equals("VEF")) {
				icon.setImageResource(R.drawable.vebig);
			}
			if (currentCurrency.getCurrCode().equals("VND")) {
				icon.setImageResource(R.drawable.vnbig);
			}
			if (currentCurrency.getCurrCode().equals("YER")) {
				icon.setImageResource(R.drawable.yebig);
			}
			if (currentCurrency.getCurrCode().equals("ZAR")) {
				icon.setImageResource(R.drawable.zabig);
			}
			if (currentCurrency.getCurrCode().equals("ZMK")) {
				icon.setImageResource(R.drawable.zmbig);
			}

			return convertView;

		}

	}

	private int getCurrencyInt(String currCode) {
		int pos = 0;
		for (String theCode : currencyCodes) {
			if (theCode.contentEquals(currCode.trim()))
				break;
			pos++;
		}
		return pos;
	}

	private boolean checkDB(CurrencyQuote q) {

		Calendar nowMinusFrequency = Calendar.getInstance(); // Todays' date
																// which we
																// shall minus
																// the update
																// frequency

		if (updateFrequency.equals("Update Manually Only")) {
		}
		if (updateFrequency.equals("10 Minutes")) {
			nowMinusFrequency.add(Calendar.MINUTE, -10);
		}
		if (updateFrequency.equals("30 Minutes")) {
			nowMinusFrequency.add(Calendar.MINUTE, -30);
		}
		if (updateFrequency.equals("1 Hour")) {
			{
				nowMinusFrequency.add(Calendar.MINUTE, -60);
			}
		}
		if (updateFrequency.equals("2 Hours")) {
			{
				nowMinusFrequency.add(Calendar.HOUR, -2);
			}
		}
		if (updateFrequency.equals("3 Hours")) {
			{
				nowMinusFrequency.add(Calendar.HOUR, -3);
			}
		}
		if (updateFrequency.equals("4 Hours")) {
			{
				nowMinusFrequency.add(Calendar.HOUR, -4);
			}
		}
		if (updateFrequency.equals("6 Hours")) {
			{
				nowMinusFrequency.add(Calendar.HOUR, -6);
			}
		}
		if (updateFrequency.equals("12 Hours")) {
			{
				nowMinusFrequency.add(Calendar.HOUR, -12);
			}
		}
		if (updateFrequency.equals("24 Hours")) {
			{
				nowMinusFrequency.add(Calendar.HOUR, -24);
			}
		}

		// OK. So here we check if a suitable value isnt already stored in the
		// db.
		// if it is then set screen values and return true.
		SQLiteDatabase db = currencyData.getReadableDatabase();
		String whereClause = " fromCurrency = '" + q.getBaseCurr()
				+ "' AND toCurrency = '" + q.getCurrCode() + "'"
				+ " AND source = '" + dataSource + "'" + " AND dateTime >= '"
				+ dateFormat.format(nowMinusFrequency.getTime()) + "'";

		Cursor curs = db.query("currencydata", sqlFromCols1, whereClause, null,
				null, null, null);
		// Log.d("DATABASE","whereClause" + whereClause);
		// Log.d("DATABASE","Current Time :" +
		// dateFormat.format(nowMinusFrequency.getTime()));
		// db.close();

		int cnt = curs.getCount();
		if (cnt > 0) {
			String sqltoAmount = "";
			while (curs.moveToNext()) {
				sqltoAmount = curs.getString(0);
			}

			if ((!sqltoAmount.equals(""))
					|| (!sqltoAmount.equals("Not Available"))) {
				// right found something so display it and exit.
				// Log.d("CRASH",dataSource + " baseCurr = " + q.getBaseCurr() +
				// " baseAmt = " + baseCurrencyAmount + "  CurrCode = " +
				// q.getCurrCode() + " convAmt = " + sqltoAmount );

				Float baseAmt = Float.parseFloat(baseCurrencyAmount);
				Float convAmt = Float.parseFloat(sqltoAmount.replace("�", ""));
				Float finalAmt = baseAmt * convAmt;
				q.setAmount(finalAmt.toString());

				curs.close();
				db.close();
				return true;
			}
		}
		curs.close();
		db.close();
		return false;

	}

	private void startGetCurrencyConversion() throws IllegalStateException {

		dataSource = SettingsPreferences.getDataSourceDefault(this);
		updateFrequency = SettingsPreferences.getUpdateFrequencyDefault(this);

		// First check if a baseCurrency exists, and if not then set it to the
		// first one.
		if ((currencies.get(0).getBaseCurr().equals("")))
			currencies.get(0).setbaseCurr(currencies.get(0).getCurrCode());

		// Then loop through all currencies and get the baseCurrenyAmount
		for (CurrencyQuote q : currencies) {
			// Set global baseCurrencyAmount...
			if (q.getBaseCurr().equals(q.getCurrCode()))
				baseCurrencyAmount = q.getAmount();
		}

		if (baseCurrencyAmount.equals("Working...")) {
			baseCurrencyAmount = "1";
		}

		// Now check the db for all currencies...
		for (CurrencyQuote q : currencies) {

			// Check the db if we have a valid value, if not then setAmount =
			// "Working...".
			// Later on in doInBackGround, if the amount = "Working..." then we
			// download a new value.
			if (!checkDB(q)) {
				// Yeah yeah we do this twice , blah blah blah
				if (!q.getBaseCurr().equals(q.getCurrCode()))
					q.setAmount("Working...");
			}
		}
		myCurrencyActivityAdapter.notifyDataSetChanged();

		DoCurrencyConversionTask doCurrencyConversionTask = new DoCurrencyConversionTask();
		try {
			doCurrencyConversionTask.execute();
		} catch (RejectedExecutionException r) {
			// Log.i("CurrencyConverter","Caught RejectedExecutionException Exception on startGetYahooConversion");
		}

	}

	private class DoCurrencyConversionTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected void onCancelled() {
			// lv.setEnabled(true);
			// lv.setClickable(true);
			for (CurrencyQuote q : currencies) {
				if (q.getBaseCurr().equals(q.getCurrCode())) {
					// q.setAmount("1");
					continue;
				}

				if (!checkDB(q)) {
					if (networkAvailable1)
						q.setAmount("Not Available");
					else
						q.setAmount("No Network");
				}
			}
			myCurrencyActivityAdapter.notifyDataSetChanged();
			lvIsClickable = true;

		}

		@Override
		protected void onPreExecute() {
			currencyHeader1.setText("  Source: "
					+ SettingsPreferences
							.getDataSourceDefault(getApplicationContext()));
			// currencyHeader2.setText("Update Frequency: " +
			// SettingsPreferences.getUpdateFrequencyDefault(getApplicationContext()));
			ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo ni = cm.getActiveNetworkInfo();
			if (ni != null && ni.isConnected()) {
				networkAvailable1 = true;
				// lv.setEnabled(false);
				// lv.setClickable(false);
				lvIsClickable = false;
				currencyProgress2.setVisibility(ProgressBar.VISIBLE);
			} else {
				networkAvailable1 = false;
				Toast.makeText(getApplicationContext(),
						"Network Unavailable...", Toast.LENGTH_SHORT).show();
				currencyProgress2.setVisibility(ProgressBar.INVISIBLE);
				cancel(true);
			}

		}

		@Override
		protected void onProgressUpdate(Void... values) {
			// TODO Auto-generated method stub
			// super.onProgressUpdate(values);
			myCurrencyActivityAdapter.notifyDataSetChanged();

		}

		@Override
		protected Void doInBackground(Void... arg0) {
			// Get the conversion amount from source...

			dataSource = SettingsPreferences
					.getDataSourceDefault(getApplicationContext());
			updateFrequency = SettingsPreferences
					.getUpdateFrequencyDefault(getApplicationContext());

			if (dataSource.equals("Google"))
				doBackgroundGoogle();
			else if (dataSource.equals("Yahoo!"))
				try {
					doBackgroundYahoo();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			// Here, display the converted amount
			// Toast.makeText(getApplicationContext(), "Finished all!",
			// Toast.LENGTH_LONG).show();
			myCurrencyActivityAdapter.notifyDataSetChanged();
			// lv.setEnabled(true);
			// lv.setClickable(true);
			lvIsClickable = true;
			currencyProgress2.setVisibility(ProgressBar.INVISIBLE);

		}

		protected void doBackgroundGoogle() {

			for (CurrencyQuote q : currencies) {
				if (q.getCurrCode().equals(q.getBaseCurr())) {
					// dont set this, unless for some reason its set to
					// "Working..." then set to 1
					if (q.getAmount().equals("Working...")) {
						q.setAmount("1");
					}

				} else {

					if (!checkDB(q)) {

						if (q.getAmount().equals("Working...")) {

							String amtGoogle = null;
							ConverterGoogleHandler converterGoogleHandler = new ConverterGoogleHandler();
							amtGoogle = converterGoogleHandler.returnRate("1",
									q.getBaseCurr(), q.getCurrCode());

							if ((amtGoogle.equals(null))
									|| (amtGoogle.equals(""))
									|| amtGoogle.equals("Working...")
									|| (amtGoogle.equals("No Network") || (amtGoogle
											.equals("Not Available")))) {
								amtGoogle = "Not Available";
								q.setAmount(amtGoogle);
							} else {

								// Multiply the amount we get back by the
								// baseCurrency amount...
								Float baseAmt = Float
										.parseFloat(baseCurrencyAmount);
								Float convAmt = Float.parseFloat(amtGoogle
										.replace("�", ""));
								Float finalAmt = baseAmt * convAmt;
								q.setAmount(finalAmt.toString());

								// Save the values into the database, but first
								// delete previous one if is there.
								Calendar now = Calendar.getInstance(); // Todays'
																		// date
								SQLiteDatabase db = currencyData
										.getWritableDatabase();
								String deleteWhere = "fromCurrency = '"
										+ q.getBaseCurr()
										+ "' and toCurrency = '"
										+ q.getCurrCode() + "'" + " AND "
										+ " source = '" + dataSource + "'";
								int numdels = db.delete("currencydata",
										deleteWhere, null);

								ContentValues values = new ContentValues();
								values.put("fromCurrency", q.getBaseCurr());
								values.put("toCurrency", q.getCurrCode());
								values.put("amount", amtGoogle);
								values.put("source", dataSource);
								values.put("dateTime",
										dateFormat.format(now.getTime()));

								db.insertOrThrow("currencydata", null, values);
								db.close();

							}
						}
					}
				}

				publishProgress();
			}

		}

		protected void doBackgroundYahoo() throws IOException {

			currenciesYahoo.clear();

			for (CurrencyQuote q : currencies) {
				if (q.getCurrCode().equals(q.getBaseCurr())) {
					// dont set this, unless for some reason its set to
					// "Working..." then set to 1
					if (q.getAmount().equals("Working..."))
						q.setAmount("1");

				} else {
					if (!checkDB(q)) {
						currenciesYahoo.add(new CurrencyYahoo(q.getBaseCurr(),
								q.getCurrCode(), "TODO"));
					}
				}
			}

			if (!currenciesYahoo.isEmpty()) {

				ConverterYahooHandler converterYahooHandler = new ConverterYahooHandler();
				currenciesYahoo = converterYahooHandler
						.returnRate(currenciesYahoo);

				/**
				 * Now loop through the list returned from the yahoo handler ...
				 * i) and set amount of the curencies list ii) and save to db
				 * iii) and publishProgress
				 **/
				for (CurrencyYahoo y : currenciesYahoo) {

					/** i) and set amount of the curencies list */
					for (CurrencyQuote q : currencies) {
						if (q.getCurrCode().equals(q.getBaseCurr()))
							continue;

						if (y.getToCurr().equals(q.getCurrCode())) {

							if ((y.getToAmount().equals(null))
									|| (y.getToAmount().equals(""))
									|| y.getToAmount().equals("Working...")
									|| (y.getToAmount().equals("Not Available"))) {
								y.setToAmount("Not Available");
								q.setAmount("Not Available");
							} else {

								/** ii) and save to db */
								// Multiply the amount we get back by the
								// baseCurrency amount...
								Float baseAmt = Float
										.parseFloat(baseCurrencyAmount);
								Float convAmt = Float.parseFloat(y
										.getToAmount().replace("�", ""));
								Float finalAmt = baseAmt * convAmt;
								q.setAmount(finalAmt.toString());

								// Save the values into the database, but first
								// delete previous one if is there.
								Calendar now = Calendar.getInstance(); // Todays'
																		// date
								SQLiteDatabase db = currencyData
										.getWritableDatabase();
								String deleteWhere = "fromCurrency = '"
										+ q.getBaseCurr()
										+ "' and toCurrency = '"
										+ q.getCurrCode() + "'" + " AND "
										+ " source = '" + dataSource + "'";
								int numdels = db.delete("currencydata",
										deleteWhere, null);

								ContentValues values = new ContentValues();
								values.put("fromCurrency", q.getBaseCurr());
								values.put("toCurrency", q.getCurrCode());
								values.put("amount", y.getToAmount());
								values.put("source", dataSource);
								values.put("dateTime",
										dateFormat.format(now.getTime()));

								db.insertOrThrow("currencydata", null, values);
								db.close();

							}
							// q.setAmount(y.getToAmount());
						}
					}
				}
			}
			/** and publish progress */
			publishProgress();
		}
	}

	private int getFlag(String currCode) {

		if (currCode.compareTo("AED") == 0)
			return R.drawable.aebig;
		if (currCode.compareTo("ANG") == 0)
			return R.drawable.anbig;
		if (currCode.compareTo("ARS") == 0)
			return R.drawable.arbig;
		if (currCode.compareTo("AUD") == 0)
			return R.drawable.aubig;
		if (currCode.compareTo("BDT") == 0)
			return R.drawable.bdbig;
		if (currCode.compareTo("BGN") == 0)
			return R.drawable.bgbig;
		if (currCode.compareTo("BHD") == 0)
			return R.drawable.bhbig;
		if (currCode.compareTo("BND") == 0)
			return R.drawable.bnbig;
		if (currCode.compareTo("BOB") == 0)
			return R.drawable.bobig;
		if (currCode.compareTo("BRL") == 0)
			return R.drawable.brbig;
		if (currCode.compareTo("BWP") == 0)
			return R.drawable.bwbig;
		if (currCode.compareTo("CAD") == 0)
			return R.drawable.cabig;
		if (currCode.compareTo("CHF") == 0)
			return R.drawable.chbig;
		if (currCode.compareTo("CLP") == 0)
			return R.drawable.clbig;
		if (currCode.compareTo("CNY") == 0)
			return R.drawable.cnbig;
		if (currCode.compareTo("COP") == 0)
			return R.drawable.cobig;
		if (currCode.compareTo("CRC") == 0)
			return R.drawable.crbig;
		if (currCode.compareTo("CZK") == 0)
			return R.drawable.czbig;
		if (currCode.compareTo("DKK") == 0)
			return R.drawable.dkbig;
		if (currCode.compareTo("DOP") == 0)
			return R.drawable.dmbig;
		if (currCode.compareTo("DZD") == 0)
			return R.drawable.dzbig;
		if (currCode.compareTo("EGP") == 0)
			return R.drawable.egbig;
		if (currCode.compareTo("EUR") == 0)
			return R.drawable.eubig;
		if (currCode.compareTo("FJD") == 0)
			return R.drawable.fjbig;
		if (currCode.compareTo("GBP") == 0)
			return R.drawable.gbbig;
		if (currCode.compareTo("HKD") == 0)
			return R.drawable.hkbig;
		if (currCode.compareTo("HNL") == 0)
			return R.drawable.hnbig;
		if (currCode.compareTo("HRK") == 0)
			return R.drawable.hrbig;
		if (currCode.compareTo("HUF") == 0)
			return R.drawable.hubig;
		if (currCode.compareTo("ILS") == 0)
			return R.drawable.ilbig;
		if (currCode.compareTo("INR") == 0)
			return R.drawable.irbig;
		if (currCode.compareTo("ISK") == 0)
			return R.drawable.isbig;
		if (currCode.compareTo("JMD") == 0)
			return R.drawable.jmbig;
		if (currCode.compareTo("JOD") == 0)
			return R.drawable.jobig;
		if (currCode.compareTo("JPY") == 0)
			return R.drawable.jpbig;
		if (currCode.compareTo("KES") == 0)
			return R.drawable.kebig;
		if (currCode.compareTo("KRW") == 0)
			return R.drawable.krbig;
		if (currCode.compareTo("KWD") == 0)
			return R.drawable.kwbig;
		if (currCode.compareTo("KYD") == 0)
			return R.drawable.kybig;
		if (currCode.compareTo("KZT") == 0)
			return R.drawable.kzbig;
		if (currCode.compareTo("LBP") == 0)
			return R.drawable.lbbig;
		if (currCode.compareTo("LKR") == 0)
			return R.drawable.lkbig;
		if (currCode.compareTo("LTL") == 0)
			return R.drawable.ltbig;
		if (currCode.compareTo("LVL") == 0)
			return R.drawable.lvbig;
		if (currCode.compareTo("MAD") == 0)
			return R.drawable.mabig;
		if (currCode.compareTo("MDL") == 0)
			return R.drawable.mdbig;
		if (currCode.compareTo("MKD") == 0)
			return R.drawable.mkbig;
		if (currCode.compareTo("MUR") == 0)
			return R.drawable.mubig;
		if (currCode.compareTo("MVR") == 0)
			return R.drawable.mvbig;
		if (currCode.compareTo("MXN") == 0)
			return R.drawable.mxbig;
		if (currCode.compareTo("MYR") == 0)
			return R.drawable.mybig;
		if (currCode.compareTo("NAD") == 0)
			return R.drawable.nabig;
		if (currCode.compareTo("NGN") == 0)
			return R.drawable.ngbig;
		if (currCode.compareTo("NIO") == 0)
			return R.drawable.nibig;
		if (currCode.compareTo("NOK") == 0)
			return R.drawable.nobig;
		if (currCode.compareTo("NPR") == 0)
			return R.drawable.npbig;
		if (currCode.compareTo("NZD") == 0)
			return R.drawable.nzbig;
		if (currCode.compareTo("OMR") == 0)
			return R.drawable.ombig;
		if (currCode.compareTo("PEN") == 0)
			return R.drawable.pebig;
		if (currCode.compareTo("PGK") == 0)
			return R.drawable.pgbig;
		if (currCode.compareTo("PHP") == 0)
			return R.drawable.phbig;
		if (currCode.compareTo("PKR") == 0)
			return R.drawable.pkbig;
		if (currCode.compareTo("PLN") == 0)
			return R.drawable.plbig;
		if (currCode.compareTo("PYG") == 0)
			return R.drawable.pybig;
		if (currCode.compareTo("QAR") == 0)
			return R.drawable.qabig;
		if (currCode.compareTo("RON") == 0)
			return R.drawable.robig;
		if (currCode.compareTo("RSD") == 0)
			return R.drawable.rsbig;
		if (currCode.compareTo("RUB") == 0)
			return R.drawable.rubig;
		if (currCode.compareTo("SAR") == 0)
			return R.drawable.sabig;
		if (currCode.compareTo("SCR") == 0)
			return R.drawable.scbig;
		if (currCode.compareTo("SEK") == 0)
			return R.drawable.sebig;
		if (currCode.compareTo("SGD") == 0)
			return R.drawable.sgbig;
		if (currCode.compareTo("SKK") == 0)
			return R.drawable.skbig;
		if (currCode.compareTo("SLL") == 0)
			return R.drawable.slbig;
		if (currCode.compareTo("SVC") == 0)
			return R.drawable.svbig;
		if (currCode.compareTo("THB") == 0)
			return R.drawable.thbig;
		if (currCode.compareTo("TND") == 0)
			return R.drawable.tnbig;
		if (currCode.compareTo("TRY") == 0)
			return R.drawable.trbig;
		if (currCode.compareTo("TTD") == 0)
			return R.drawable.ttbig;
		if (currCode.compareTo("TWD") == 0)
			return R.drawable.twbig;
		if (currCode.compareTo("TZS") == 0)
			return R.drawable.tzbig;
		if (currCode.compareTo("UAH") == 0)
			return R.drawable.uabig;
		if (currCode.compareTo("UGX") == 0)
			return R.drawable.ugbig;
		if (currCode.compareTo("USD") == 0)
			return R.drawable.usbig;
		if (currCode.compareTo("UYU") == 0)
			return R.drawable.uybig;
		if (currCode.compareTo("UZS") == 0)
			return R.drawable.uzbig;
		if (currCode.compareTo("VEF") == 0)
			return R.drawable.vebig;
		if (currCode.compareTo("VND") == 0)
			return R.drawable.vnbig;
		if (currCode.compareTo("YER") == 0)
			return R.drawable.yebig;
		if (currCode.compareTo("ZAR") == 0)
			return R.drawable.zabig;
		if (currCode.compareTo("ZMK") == 0)
			return R.drawable.zmbig;

		return 0;

	}

	public class footerAdapter extends ArrayAdapter<String> {

		private final Activity activity;
		private final List<String> currenciesToAdd;

		public footerAdapter(Activity activity, List objects) {
			super(activity, R.layout.rowwithflag, objects);
			this.activity = activity;
			this.currenciesToAdd = objects;
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
			int pos = getCurrencyInt(currenciesToAdd.get(position));

			if (currenciesToAdd.get(position).toString().equals("AED")) {
				icon.setImageResource(R.drawable.aebig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("ANG")) {
				icon.setImageResource(R.drawable.anbig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("ARS")) {
				icon.setImageResource(R.drawable.arbig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("AUD")) {
				icon.setImageResource(R.drawable.aubig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("BDT")) {
				icon.setImageResource(R.drawable.bdbig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("BGN")) {
				icon.setImageResource(R.drawable.bgbig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("BHD")) {
				icon.setImageResource(R.drawable.bhbig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("BND")) {
				icon.setImageResource(R.drawable.bnbig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("BOB")) {
				icon.setImageResource(R.drawable.bobig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("BRL")) {
				icon.setImageResource(R.drawable.brbig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("BWP")) {
				icon.setImageResource(R.drawable.bwbig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("CAD")) {
				icon.setImageResource(R.drawable.cabig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("CHF")) {
				icon.setImageResource(R.drawable.chbig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("CLP")) {
				icon.setImageResource(R.drawable.clbig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("CNY")) {
				icon.setImageResource(R.drawable.cnbig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("COP")) {
				icon.setImageResource(R.drawable.cobig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("CRC")) {
				icon.setImageResource(R.drawable.crbig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("CZK")) {
				icon.setImageResource(R.drawable.czbig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("DKK")) {
				icon.setImageResource(R.drawable.dkbig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("DOP")) {
				icon.setImageResource(R.drawable.dmbig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("DZD")) {
				icon.setImageResource(R.drawable.dzbig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("EGP")) {
				icon.setImageResource(R.drawable.egbig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("EUR")) {
				icon.setImageResource(R.drawable.eubig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("FJD")) {
				icon.setImageResource(R.drawable.fjbig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("GBP")) {
				icon.setImageResource(R.drawable.gbbig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("HKD")) {
				icon.setImageResource(R.drawable.hkbig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("HNL")) {
				icon.setImageResource(R.drawable.hnbig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("HRK")) {
				icon.setImageResource(R.drawable.hrbig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("HUF")) {
				icon.setImageResource(R.drawable.hubig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("ILS")) {
				icon.setImageResource(R.drawable.ilbig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("INR")) {
				icon.setImageResource(R.drawable.irbig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("ISK")) {
				icon.setImageResource(R.drawable.isbig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("JMD")) {
				icon.setImageResource(R.drawable.jmbig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("JOD")) {
				icon.setImageResource(R.drawable.jobig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("JPY")) {
				icon.setImageResource(R.drawable.jpbig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("KES")) {
				icon.setImageResource(R.drawable.kebig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("KRW")) {
				icon.setImageResource(R.drawable.krbig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("KWD")) {
				icon.setImageResource(R.drawable.kwbig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("KYD")) {
				icon.setImageResource(R.drawable.kybig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("KZT")) {
				icon.setImageResource(R.drawable.kzbig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("LBP")) {
				icon.setImageResource(R.drawable.lbbig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("LKR")) {
				icon.setImageResource(R.drawable.lkbig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("LTL")) {
				icon.setImageResource(R.drawable.ltbig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("LVL")) {
				icon.setImageResource(R.drawable.lvbig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("MAD")) {
				icon.setImageResource(R.drawable.mabig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("MDL")) {
				icon.setImageResource(R.drawable.mdbig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("MKD")) {
				icon.setImageResource(R.drawable.mkbig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("MUR")) {
				icon.setImageResource(R.drawable.mubig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("MVR")) {
				icon.setImageResource(R.drawable.mvbig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("MXN")) {
				icon.setImageResource(R.drawable.mxbig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("MYR")) {
				icon.setImageResource(R.drawable.mybig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("NAD")) {
				icon.setImageResource(R.drawable.nabig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("NGN")) {
				icon.setImageResource(R.drawable.ngbig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("NIO")) {
				icon.setImageResource(R.drawable.nibig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("NOK")) {
				icon.setImageResource(R.drawable.nobig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("NPR")) {
				icon.setImageResource(R.drawable.npbig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("NZD")) {
				icon.setImageResource(R.drawable.nzbig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("OMR")) {
				icon.setImageResource(R.drawable.ombig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("PEN")) {
				icon.setImageResource(R.drawable.pebig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("PGK")) {
				icon.setImageResource(R.drawable.pgbig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("PHP")) {
				icon.setImageResource(R.drawable.phbig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("PKR")) {
				icon.setImageResource(R.drawable.pkbig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("PLN")) {
				icon.setImageResource(R.drawable.plbig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("PYG")) {
				icon.setImageResource(R.drawable.pybig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("QAR")) {
				icon.setImageResource(R.drawable.qabig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("RON")) {
				icon.setImageResource(R.drawable.robig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("RSD")) {
				icon.setImageResource(R.drawable.rsbig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("RUB")) {
				icon.setImageResource(R.drawable.rubig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("SAR")) {
				icon.setImageResource(R.drawable.sabig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("SCR")) {
				icon.setImageResource(R.drawable.scbig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("SEK")) {
				icon.setImageResource(R.drawable.sebig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("SGD")) {
				icon.setImageResource(R.drawable.sgbig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("SKK")) {
				icon.setImageResource(R.drawable.skbig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("SLL")) {
				icon.setImageResource(R.drawable.slbig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("SVC")) {
				icon.setImageResource(R.drawable.svbig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("THB")) {
				icon.setImageResource(R.drawable.thbig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("TND")) {
				icon.setImageResource(R.drawable.tnbig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("TRY")) {
				icon.setImageResource(R.drawable.trbig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("TTD")) {
				icon.setImageResource(R.drawable.ttbig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("TWD")) {
				icon.setImageResource(R.drawable.twbig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("TZS")) {
				icon.setImageResource(R.drawable.tzbig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("UAH")) {
				icon.setImageResource(R.drawable.uabig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("UGX")) {
				icon.setImageResource(R.drawable.ugbig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("USD")) {
				icon.setImageResource(R.drawable.usbig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("UYU")) {
				icon.setImageResource(R.drawable.uybig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("UZS")) {
				icon.setImageResource(R.drawable.uzbig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("VEF")) {
				icon.setImageResource(R.drawable.vebig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("VND")) {
				icon.setImageResource(R.drawable.vnbig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("YER")) {
				icon.setImageResource(R.drawable.yebig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("ZAR")) {
				icon.setImageResource(R.drawable.zabig);
				label.setText(codeCountryCurr[pos]);
			}
			if (currenciesToAdd.get(position).toString().equals("ZMK")) {
				icon.setImageResource(R.drawable.zmbig);
				label.setText(codeCountryCurr[pos]);
			}

			return convertView;

		}

	}

}
