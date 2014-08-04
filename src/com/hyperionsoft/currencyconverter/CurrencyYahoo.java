package com.hyperionsoft.currencyconverter;

/** 
 * This is used for the currenciesYahoo list in the classes CurrencyActiviy & ConverterActivity.
 * We fill this list with the currencies for yahoo conversion. 
 * This is so we only need one HTTP call to yahoo for multiple conversions.
 * The from-amount is always 1.
 */

public class CurrencyYahoo {
	private String fromCurr;
	private String toCurr;
	private  String toAmount;
	
	public CurrencyYahoo(String fromCurr, String toCurr, String toAmount) {
		this.fromCurr = fromCurr;
		this.toCurr = toCurr;
		this.toAmount = toAmount;
	}
	public String getFromCurr() {
		return fromCurr;
	}
	public void setFromCurr(String fromCurr) {
		this.fromCurr = fromCurr;
	}
	public String getToCurr() {
		return toCurr;
	}
	public void setToCurr(String toCurr) {
		this.toCurr = toCurr;
	}
	public String getToAmount() {
		return toAmount;
	}
	public void setToAmount(String toAmount) {
		this.toAmount = toAmount;
	} 

	
	
}

