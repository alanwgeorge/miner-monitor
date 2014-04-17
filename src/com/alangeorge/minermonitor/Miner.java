package com.alangeorge.minermonitor;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.Socket;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import android.annotation.SuppressLint;
import android.util.JsonReader;

public class Miner {
	private final int MAXRECEIVESIZE = 65535;
	
    public String name;
    public InetAddress ip;
    public int port;
    public String summaryText;
    public Date lastUpdateTime = null;
    public String version = null;
    public double mhsAvg = 0.0;
    public double mhsAvg5s = 0.0;
	public double totalMH = 0.0;
	public double bestShare = 0.0;
	
	private DecimalFormat ghsFormatter = new DecimalFormat("###,###.000");
	private DecimalFormat bestShareFormatter = new DecimalFormat("###,###,###,###,###");
	
	private ArrayList<Miner.MinerListener> listeners = new ArrayList<Miner.MinerListener>();
	
	private Exception exception = null;

	public interface MinerListener {
		public void update(Miner miner);
	}
	
    public Miner(String name, InetAddress ip, int port) {
        this.name = name;
        this.ip = ip;
        this.port = port;
    }

    public void registerListener(Miner.MinerListener listener) {
    	if (listener != null) {
    		listeners.add(listener);
    	}
    }
    
    public boolean unregisterListener(Miner.MinerListener listener) {
    	if (listener != null) {
    		return listeners.remove(listener);
    	}
    	
    	return false;
    }
    
    public void updateListeners() {
    	for (Miner.MinerListener l : listeners) {
    		l.update(this);
    	}
    }
    
    public void setException(Exception e) {
    	this.exception = e;
    }
    
    public Exception getException() {
    	return this.exception;
    }
    
	@Override
	public String toString() {
		return name + ": " + ghsFormatter.format(mhsAvg / 1000) + " GH/s: BS " + bestShareFormatter.format(bestShare);
	}
	
	public String detailContent() {
		return "Miner [name=" + name + ", ip=" + ip + ", port=" + port
				+ ", version=" + version + ", lastUpdateTime=" + lastUpdateTime+ ", bestShare=" + bestShare + ", mhsAvg=" + mhsAvg
						+ ", mhsAvg5s=" + mhsAvg5s + ", totalMH=" + totalMH +"]";
	}

	public void setSummaryText(String summaryText) {
		this.summaryText = summaryText;
	}

	@SuppressLint("DefaultLocale")
	public void update() throws Exception, IOException {
		StringBuffer stringBuffer = new StringBuffer();
		char readBuffer[] = new char[MAXRECEIVESIZE];
		int readLength = 0;
		Socket socket = null;
		
		try	{
			socket = new Socket(ip, port);
			PrintStream out = new PrintStream(socket.getOutputStream());
			out.print("{\"command\":\"summary\",\"parameter\":\"\"}".toLowerCase().toCharArray());
			out.flush();

			InputStreamReader in = new InputStreamReader(socket.getInputStream());
			while (true)	{
				readLength = in.read(readBuffer, 0, MAXRECEIVESIZE);
				if (readLength < 1) {
					break;
				}

				stringBuffer.append(readBuffer, 0, readLength);
				if (readBuffer[readLength-1] == '\0') {
					break;
				}
			}
		} finally {
			if (socket != null) {
				socket.close();
			}
		}

		String jsonString = stringBuffer.toString();
//		Log.d(Miner.class.getSimpleName(), jsonString);
		updateSummaryJson(jsonString);
	}
	
	/*
	 * Updates miner data from json String.
	 * 
	 * Example json:
	 * 		{"STATUS":[{"STATUS":"S","When":1391725518,"Code":11,"Msg":"Summary","Description":"bfgminer 3.8.1"}],
	 *		"SUMMARY":[{"Elapsed":5486,"Algorithm":"fastauto","MHS av":0.000,"MHS 5s":0.000,"Found Blocks":0,"Getworks":211,
	 *		"Accepted":0,"Rejected":0,"Hardware Errors":0,"Utility":0.000,"Discarded":2909,"Stale":0,"Get Failures":1,
	 *		"Local Work":3146,"Remote Failures":0,"Network Blocks":18,"Total MH":0.0000,"Diff1 Work":0,"Work Utility":0.000,
	 *		"Difficulty Accepted":0.00000000,"Difficulty Rejected":0.00000000,"Difficulty Stale":0.00000000,"Best Share":0,
	 *		"Device Hardware%":0.0000,"Device Rejected%":0.0000,"Pool Rejected%":0.0000,"Pool Stale%":0.0000}],"id":1}
	 */
	public void updateSummaryJson(String jsonString) throws Exception, IOException {
		JsonReader jsonReader = null;

		try {
			jsonReader = new JsonReader(new StringReader(jsonString));

			jsonReader.beginObject();

			while (jsonReader.hasNext()) {
				String name = jsonReader.nextName();
				if ("STATUS".equalsIgnoreCase(name)) {
					jsonReader.beginArray();
					jsonReader.beginObject();
					while (jsonReader.hasNext()) {
						name = jsonReader.nextName();
						if ("STATUS".equalsIgnoreCase(name)) {
							if (! "S".equalsIgnoreCase(jsonReader.nextString())) {
								throw new Exception("JSON status code was not success");
							}
						} else if ("When".equalsIgnoreCase(name)) {
							long timeLong = jsonReader.nextLong() * 1000;
							Calendar updateCal = Calendar.getInstance();
							updateCal.setTimeZone(TimeZone.getDefault());
							updateCal.setTime(new Date(timeLong));
							lastUpdateTime = updateCal.getTime();
						} else if ("Description".equalsIgnoreCase(name)) {
							version = jsonReader.nextString();
						} else {
							jsonReader.skipValue();
						}
					}
					jsonReader.endObject();
					jsonReader.endArray();
				} else if ("SUMMARY".equalsIgnoreCase(name)){
					jsonReader.beginArray();
					jsonReader.beginObject();
					while (jsonReader.hasNext()) {
						name = jsonReader.nextName();
						if ("MHS av".equalsIgnoreCase(name)) {
							mhsAvg = jsonReader.nextDouble();
						} else if ("MHS 5s".equalsIgnoreCase(name)) {
							mhsAvg5s = jsonReader.nextDouble();
						} else if ("Total MH".equalsIgnoreCase(name)) {
							totalMH = jsonReader.nextDouble();
						} else if ("Best Share".equalsIgnoreCase(name)) {
							bestShare = jsonReader.nextDouble();
						} else {
							jsonReader.skipValue();
						}
					}
					jsonReader.endObject();
					jsonReader.endArray();
				} else {
					jsonReader.skipValue();
				}
			}

			jsonReader.endObject();
		} finally {
			if (jsonReader != null) {
				jsonReader.close();
			}
		}
	}
}
