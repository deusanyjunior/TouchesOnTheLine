package br.usp.ime.compmus.dj.m2m_wsorchestra.scorereader;

import java.net.URL;

import org.json.JSONException;
import org.json.JSONObject;

import br.usp.ime.compmus.utils.JSONfunctions;

import android.util.Log;

public class ScoreReader {

	private URL webserver;
	
	private int lastNote;
	
	public ScoreReader(URL ws) {
		setWebserver(ws);
		lastNote = 0;
	}
	
	public int getLastNoteFromWS() {
		int lastNoteFromWS = -1;
		JSONObject results = JSONfunctions.getJSONfromURL(getWebserver()+".json");
		JSONObject resultsObject = null;
		
		try {
			if (results != null && !results.isNull("cscore")) {
				resultsObject = results.getJSONObject("cscore");
				if (resultsObject != null && !resultsObject.isNull("id")) {
					lastNoteFromWS = resultsObject.getInt("id");					
				}
			} else {
				lastNoteFromWS = -1;
			}
		} catch (JSONException e) {
			e.printStackTrace();
			lastNoteFromWS = -1;
		}
		Log.i("ScoreReader", "getLastNoteFromWS "+lastNoteFromWS);
		return lastNoteFromWS;
	}
	
	public boolean setLastNoteFromWS() {
		boolean lastNoteSetted = false;
		int lastNoteFromWS = -1;
		
		if ( (lastNoteFromWS = getLastNoteFromWS()) != -1 ) {
			Log.i("ScoreReader", "setLastNoteFromWS "+lastNoteFromWS);
			this.lastNote = lastNoteFromWS;
			lastNoteSetted = true;
		}

		return lastNoteSetted;
	}
	
	public int getLastNote() {
		Log.i("ScoreReader", "getLastNote "+lastNote);
		return lastNote;
	}
	
	public void setLastNote(int note) {
		Log.i("ScoreReader", "setLastNote "+lastNote);
		lastNote = note;
	}
	
	public String[] getNextScore() {
		String[] nextNote = null;
		int note = -1;
		
		if (lastNote != -1) {
			note = lastNote+1;
			
			JSONObject resultsObject = null;
			JSONObject nextNoteObject = null;
			resultsObject = JSONfunctions.getJSONfromURL(getWebserver()+"/"+note+".json");
			try {
				if (resultsObject != null && !resultsObject.isNull("cscore")) {
					nextNoteObject = resultsObject.getJSONObject("cscore");
					if (nextNoteObject != null && !nextNoteObject.isNull("score")) {
						// set nextNote
						nextNote = new String[4];
						nextNote[0] = nextNoteObject.get("created_at").toString();
						nextNote[1] = nextNoteObject.getString("userId");
						nextNote[2] = nextNoteObject.getString("score");
						// update the last note
						setLastNote(note);
					} else {
						return null;
					}
				} else {
					return null;
				}
			} catch (JSONException e) {
				Log.i("ScoreReader", "getNextNote JSONException");
				e.printStackTrace();
				return null;
			}	
		}
		
		return nextNote;
	}

	public URL getWebserver() {
		return webserver;
	}

	private void setWebserver(URL webserver) {
		this.webserver = webserver;
	}

}
