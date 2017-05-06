/* 
 
  MultiTouchXYActivity.java:
 
 Copyright (C) 2011 Victor Lazzarini, Steven Yi
 
 This file is part of Csound Android Examples.
 
 The Csound Android Examples is free software; you can redistribute it
 and/or modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 2.1 of the License, or (at your option) any later version.   
 
 Csound is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Lesser General Public License for more details.
 
 You should have received a copy of the GNU Lesser General Public
 License along with Csound; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 02111-1307 USA
 
 */

package br.usp.ime.compmus.dj.touchesontheline;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Toast;
import br.usp.ime.compmus.dj.m2m_wsorchestra.scorereader.ScoreReader;
import br.usp.ime.compmus.utils.JSONfunctions;

import com.csounds.CsoundObj;
import com.csounds.CsoundObjCompletionListener;
import com.csounds.examples.BaseCsoundActivity;
import com.csounds.examples.R;
import com.csounds.valueCacheable.CsoundValueCacheable;

import csnd.CsoundMYFLTArray;

public class TouchesOnTheLineActivity extends BaseCsoundActivity implements
		CsoundObjCompletionListener, CsoundValueCacheable {

	private static final int DEVICE_TOUCHES_ON = 5;
	private static final int TOUCHES_ON = 10;
	private static final float TOUCH_MOVE_THRESHOULD = 0.0f;
	
	public View multiTouchView;
	
	int touchIds[] = new int[TOUCHES_ON];
	float touchX[] = new float[TOUCHES_ON];
	float touchY[] = new float[TOUCHES_ON];
	float last_touchX[] = new float[TOUCHES_ON];
	long touch_time[] = new long[TOUCHES_ON];
	float last_touchY[] = new float[TOUCHES_ON];
	CsoundMYFLTArray touchXPtr[] = new CsoundMYFLTArray[TOUCHES_ON];
	CsoundMYFLTArray touchYPtr[] = new CsoundMYFLTArray[TOUCHES_ON];

	
	
	// WebServer
	private static final String TAG = "CScore";
	private static int localUserId = 0;

	public LinkedList<String> webservers = new LinkedList<String>();
	public static String webserver = "";
	private StringBuffer score = new StringBuffer();
	
	private static ReaderThread readerThread;
	private static ScoreReader scoreReader;
	private static Handler wsScoreReaderThreadHandler;
	private static boolean scoreReaderRunning = true;
	
	

	protected int getTouchIdAssignment() {
		for(int i = 0; i < touchIds.length; i++) {
			if(touchIds[i] == -1) {
				return i;
			}
		}
		return -1;
	}
	
	protected int getTouchId(int touchId) {
		for(int i = 0; i < touchIds.length; i++) {
			if(touchIds[i] == touchId) {
				return i;
			}
		}
		return -1;
	}
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		

		webservers.add("http://wscompmus.deusanyjunior.dj/cscores");
//		webservers.add("http://wscompmus.deusanyjunior.dj/cscores");
//		webservers.add("http://10.0.2.1:3000/cscores");
//		webservers.add("http://10.0.0.1:3000/cscores");
		
		for(int i = 0; i < touchIds.length; i++) {
			touchIds[i] = -1;
			touchX[i] = -1;
			touchY[i] = -1;
			last_touchX[i] = -1;
			last_touchY[i] = -1;
			touch_time[i] = 0;
		}
		
		multiTouchView = new View(this);
		multiTouchView.setKeepScreenOn(true);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		
		multiTouchView.setOnTouchListener(new OnTouchListener() {

			public boolean onTouch(View v, MotionEvent event) {
				final int action = event.getAction() & MotionEvent.ACTION_MASK;
//				score = "";
				WSScorePlayer wsScorePlayer = new WSScorePlayer();
				
				switch(action) {
				case MotionEvent.ACTION_DOWN:
					Log.i("ACTION_DOWN", " ");
				case MotionEvent.ACTION_POINTER_DOWN:
					
					for(int i = 0; i < event.getPointerCount(); i++) {
						int pointerId = event.getPointerId(i);
						int id = getTouchId(pointerId);
						
						if(id == -1) {
							
							id = getTouchIdAssignment();
							
							if(id != -1) {
								touchIds[id] = pointerId;
								touchX[id] = event.getX(i) / multiTouchView.getWidth();
								touchY[id] = 1 - (event.getY(i) / multiTouchView.getHeight());
								//Webserver
								touch_time[id] = SystemClock.elapsedRealtime();
//								score = score.concat( String.format("d%d %f %f;", touch_delay[id], touchX[id], touchY[id]) );
								score = new StringBuffer();
								score.append(String.format("s %f %f;", touchX[id], touchY[id]));
								
								if(touchXPtr[id] != null) {
									touchXPtr[id].SetValue(0, touchX[id]);
									touchYPtr[id].SetValue(0, touchY[id]);
									csoundObj.sendScore(String.format("i1.%d 0 -2 %d", id, id));

									// WebServer
									long timestamp = SystemClock.elapsedRealtime();
									score.append(String.format("d%d;", timestamp-touch_time[id]));								
									touch_time[id] = timestamp;
								}
//									wsScorePlayer.execute(new String[] { Integer.toString(localUserId), score });
							}
						}
						
					}
					
					break;
					
				case MotionEvent.ACTION_MOVE:

					for(int i = 0; i < event.getPointerCount(); i++) {
						int pointerId = event.getPointerId(i);
						int id = getTouchId(pointerId);
						float tempX, tempY;

						if(id != -1) {
//							touchX[id] = event.getX(i) / multiTouchView.getWidth();
//							touchY[id] = 1 - (event.getY(i) / multiTouchView.getHeight());

							// WebServer
							tempX = event.getX(i) / multiTouchView.getWidth();
							tempY = 1 - (event.getY(i) / multiTouchView.getHeight());
							if( touchX[id] == -1 ||
									(Math.abs(Math.abs(tempX)-Math.abs(touchX[id]))>=TOUCH_MOVE_THRESHOULD) ||
									(Math.abs(Math.abs(tempY)-Math.abs(touchY[id]))>=TOUCH_MOVE_THRESHOULD) ) {
							
								touchX[id] = tempX;
								touchY[id] = tempY;
									
								long timestamp = SystemClock.elapsedRealtime();
								score.append(String.format("m%d %f %f;", timestamp-touch_time[id], tempX, tempY));								
//								score = String.format("m%d %f %f;", timestamp-touch_time[id], tempX, tempY);								
								touch_time[id] = timestamp;
//								wsScorePlayer.execute(new String[] { Integer.toString(localUserId), score });
								
							}
//							if( touchX[id] == -1 ||
//									(Math.abs(Math.abs(tempX)-Math.abs(last_touchX[id]))>=TOUCH_MOVE_THRESHOULD) ||
//									(Math.abs(Math.abs(tempY)-Math.abs(last_touchY[id]))>=TOUCH_MOVE_THRESHOULD) ) {
//								last_touchX[id] = tempX;
//								last_touchY[id] = tempY;
//								touch_delay[id] = System.currentTimeMillis() - touch_delay[id];								
//								score = score.concat(String.format("m%d %f %f;", touch_delay[id], tempX, tempY));								
//								wsScorePlayer.execute(new String[] { Integer.toString(localUserId), score });
//							}
						}
					}
					break;
					
				case MotionEvent.ACTION_POINTER_UP:
					Log.i("ACTION_POINTER_UP", " ");
				case MotionEvent.ACTION_UP:
					
					int activePointerIndex = event.getActionIndex();
					int pointerId = event.getPointerId(activePointerIndex);
						
					int id = getTouchId(pointerId);
					if(id != -1) {
						touchIds[id] = -1;
						csoundObj.sendScore(String.format("i-1.%d 0 0 %d", id, id));
						
						// WebServer
						long timestamp = SystemClock.elapsedRealtime();
						score.append(String.format("u%d;", timestamp-touch_time[id]));
//						score = String.format("u%d;", timestamp-touch_time[id]);
						touch_time[id] = timestamp;
						wsScorePlayer.execute(new String[] { Integer.toString(localUserId), score.toString() });
					}
					break;

				}
				
//				Log.i(TAG, score);
				return true;
			}
			
		});
		
		
		
		
		
		
		
		
		setContentView(multiTouchView);

		String csd = getResourceFileAsString(R.raw.multitouch_xy);
		File f = createTempFile(csd);

		csoundObj.addValueCacheable(this);

		csoundObj.startCsound(f);
		
		// WebServer methods
		
		localUserId = (int) ((double)(Math.random()*1000.0%100.0));
		Log.i("UserId", Integer.toString(localUserId));
		
		wsScoreReaderThreadHandler = new Handler() {
			public void handleMessage(Message msg) {
				int userId = (int) msg.arg1;
				String score = (String) msg.obj;
				playCSoundScore(userId, score);       			
			}
		};
		
		boolean started = startScoreReader();
		if (started) {
			Toast.makeText(this, "Connected to webserver: "+scoreReader.getWebserver(), 1000).show();
		} else {
			Toast.makeText(this, "Could no connect to webserver", 2000).show();
		}
		// End WebServer methods
		
	}
	
	@Override
    protected void onResume() {
    	if (readerThread != null && readerThread.isInterrupted()) {
    		readerThread.start();
    	}
    	super.onResume();
    }
        
    @Override
    protected void onPause() {
    	super.onPause();
    }
    
    @Override
    protected void onStop() {
    	setScoreReaderRunning(false);
        super.onStop();
    }
    
    @Override
    public void onDestroy() {
    	setScoreReaderRunning(false);
    	stopScoreReader();
    	super.onDestroy();
    }
	
	
	
	
	
	
	
	
	
	

	public void csoundObjComplete(CsoundObj csoundObj) {

	}
	
	// VALUE CACHEABLE

	public void setup(CsoundObj csoundObj) {
		for(int i = 0; i < touchIds.length; i++) {
			touchXPtr[i] = csoundObj.getInputChannelPtr(String.format("touch.%d.x", i));
			touchYPtr[i] = csoundObj.getInputChannelPtr(String.format("touch.%d.y", i));
		}
	}

	public void updateValuesToCsound() {
		for(int i = 0; i < touchX.length; i++) {
			touchXPtr[i].SetValue(0, touchX[i]);
			touchYPtr[i].SetValue(0, touchY[i]);
		}
		
	}

	public void updateValuesFromCsound() {
	}

	public void cleanup() {
		for(int i = 0; i < touchIds.length; i++) {
			touchXPtr[i].Clear();
			touchXPtr[i] = null;
			touchYPtr[i].Clear();
			touchYPtr[i] = null;
		}
	}
	
	
	
	
	
	
	
	
	
	
	
	// Webserver methods
	
	
	public synchronized static boolean isScoreReaderRunning() {
		return scoreReaderRunning;
	}

	public synchronized static void setScoreReaderRunning(boolean isScoreReaderRunning) {
		TouchesOnTheLineActivity.scoreReaderRunning = isScoreReaderRunning;
	}
	
	private void playCSoundScore(int userId, String csoundScore) {
		
		int id = userId%(TOUCHES_ON-DEVICE_TOUCHES_ON) + DEVICE_TOUCHES_ON;
		String splittedScore[] = csoundScore.split(";");

		for(String lineScore: splittedScore) {
			Log.w("CSoundScore lineScore", lineScore);
			
			// Action Down
			if(lineScore.startsWith("s")) {
				String temp[] = lineScore.split(" ");
				touchIds[id] = id;
				touchX[id] = Float.parseFloat(temp[1].replace(",", "."));
				touchY[id] = Float.parseFloat(temp[2].replace(",", ".").replace(";", ""));
			} else 		
			// Action Down (touchXPtr != null)
			if(lineScore.startsWith("d")) {
				long delay = Long.parseLong(lineScore.substring(1));
				SystemClock.sleep(delay);
				
				touchXPtr[id].SetValue(0, touchX[id]);
				touchYPtr[id].SetValue(0, touchY[id]);				
				csoundObj.sendScore(String.format("i1.%d 0 -2 %d", id, id));		
			} else
				
			// Action Move	
			if(lineScore.startsWith("m")) {
				String temp[] = lineScore.split(" ");
				long delay = Long.parseLong(temp[0].substring(1));
				SystemClock.sleep(delay);
				
				touchX[id] = Float.parseFloat(temp[1].replace(",", "."));
				touchY[id] = Float.parseFloat(temp[2].replace(",", "."));

			} else
				
			// Action Up
			if(lineScore.startsWith("u")) {
				long delay = Long.parseLong(lineScore.substring(1));
				SystemClock.sleep(delay);
				
				touchIds[id] = -1;
				csoundObj.sendScore(String.format("i-1.%d 0 0 %d", id, id));
			}
		}
	}
	
	private class WSScorePlayer extends AsyncTask<String, Void, String> {

		@Override
		protected String doInBackground(String... params) {
					
			try {
				JSONObject scoreObject = new JSONObject();
				scoreObject.put("userId", Integer.parseInt(params[0]));
				scoreObject.put("score", params[1]);
				
				JSONObject messageObject = new JSONObject();
				messageObject.put("cscore", scoreObject);
				
				JSONfunctions.sendJSONToURL(webserver, messageObject.toString());
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}
	}	
	
	static public class ReaderThread extends Thread {
		
		public void run() {
			
			while(isScoreReaderRunning()) {				
				
				// Get the last note
				String[] nextScoreId = scoreReader.getNextScore();
				if( nextScoreId != null ) {
					Message message = Message.obtain();
					// Do not need to play scores from local user
					Log.w(TAG, "ReaderThread: "+nextScoreId[1]+" "+nextScoreId[2]);
					if( Integer.parseInt(nextScoreId[1]) != localUserId) {
						message.arg1 = Integer.parseInt(nextScoreId[1]);
						message.obj = nextScoreId[2];
						message.setTarget(wsScoreReaderThreadHandler);
						message.sendToTarget();						
					}
				} else {
					try {
						Log.i(TAG, "Waiting before trying to get more scores from webserver..");
						sleep(200);
//						new Thread().sleep(2000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	private boolean isNetworkAvailable() {
	    ConnectivityManager connectivityManager 
	          = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
	    return activeNetworkInfo != null && activeNetworkInfo.isConnected();
	}
	
	private boolean startScoreReader() {
		boolean started = false;
		try {
				for(String ws : webservers) {
					scoreReader = new ScoreReader(new URL(ws));
					
					if (isNetworkAvailable()) {
						// try to read 0.json
//						if(scoreReader.getNextScore() == null) {
							// if the server exists and is online
							if (scoreReader.setLastNoteFromWS()) {
								setScoreReaderRunning(true);
								readerThread = new ReaderThread();
								readerThread.start();
								started =  true;
								webserver = ws;
							} 
//							else {
//								// if there isn't note on the server
//								scoreReader.setLastNote(0);
//								
//								setScoreReaderRunning(true);
//								readerThread = new ReaderThread();
//								readerThread.start();
//								started =  true;
//								webserver = ws;
//							}
							// webserver found!
//							break;
//						}
					
						
					}
				}
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			started = false;
			return started;
		}
		return started;
	}
	
	private void stopScoreReader() {
		setScoreReaderRunning(false);
		if (readerThread != null) {
//			//TODO check if the thread stopped
			readerThread.interrupt();
		}
	}
	

}
