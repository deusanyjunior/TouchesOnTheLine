package br.usp.ime.compmus.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyStore;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONObject;

import br.usp.ime.compmus.utils.MySSLSocketFactory;

import android.util.Log;

public class JSONfunctions {

	/**
	 * Read a JSON from a WebServer URL
	 * @param url WebServer URL
	 * @return json JSONObject read or null otherwise;
	 */
	public static JSONObject getJSONfromURL(String url){
		System.setProperty("http.keepAlive", "false");
		
		JSONObject json = null;
		StringBuilder builder = new StringBuilder();
		String webserverUrl = url;
		HttpGet httpGet;
//		HttpClient httpClient = getNewHttpClient();
		HttpClient httpClient = new DefaultHttpClient();
		
		System.gc();
		try {
			httpGet = new HttpGet(new URI(webserverUrl));
			// Getting JSONObject
			Log.i("JSONFunctions", "GetJSONFrom: "+httpGet.getURI());
			HttpResponse response = httpClient.execute(httpGet);
			
			StatusLine statusLine = response.getStatusLine();
			int statusCode = statusLine.getStatusCode();
 			if(statusCode == 200) {
				HttpEntity entity = response.getEntity();
				InputStream streamContent = entity.getContent();
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(streamContent, "iso-8859-1"), 8);
				String lineReceived;
				// Reading each line received
				System.gc();
				while((lineReceived = reader.readLine()) != null) {
					builder.append(lineReceived);
				}
				streamContent.close();
			}
			else {
				Log.e("JSONFunctions", "Status == "+statusCode);
				httpClient.getConnectionManager().shutdown();
				return null;
			}
		} catch (ClientProtocolException e) {
			Log.e("JSONFunctions", "ClientProtocolException during JSON requisition");
			e.printStackTrace();
			httpClient.getConnectionManager().shutdown();
			return null;
		} catch (IOException e) {
			Log.e("JSONFuntions", "IOException during JSON requisition");
			e.printStackTrace();
			httpClient.getConnectionManager().shutdown();
			return null;
		} catch (URISyntaxException e) {
			Log.e("JSONFuntions", "URISyntaxException during JSON requisition");
			// TODO Auto-generated catch block
			e.printStackTrace();
			httpClient.getConnectionManager().shutdown();
		}
		
		try {
			// Creating JSONObject from server message
			json = new JSONObject(builder.toString());				
		} catch (JSONException e) {
			Log.e("JSONFunctions", "IOException while JSON object was been created");
			e.printStackTrace();
			httpClient.getConnectionManager().shutdown();
			return null;
		}
		
		httpClient.getConnectionManager().shutdown();
		return json; 
		
		
	}
	
	/**
	 * Send a JSON to Webserver URL
	 * @param url Webserver URL
	 * @param jsonMessage String in JSON format
	 * @return messageSent Return true if the message was sent (it doesnt mean that the message was received)
	 */
	public static boolean sendJSONToURL(String url, String jsonMessage){ 
		System.setProperty("http.keepAlive", "false");
		
		Boolean messageSent = false;
//		HttpResponse httpResponse = null;
	    HttpPost httpPost = new HttpPost(url);
//	    HttpClient httpClient = getNewHttpClient();
	    DefaultHttpClient httpClient = new DefaultHttpClient();
	    StringEntity entity;

		try {
			// setting up the post content
			entity = new StringEntity(jsonMessage, HTTP.UTF_8);
//	        entity.setContentType("application/x-www-form-urlencoded");
	        httpPost.setEntity(entity);
	        httpPost.setHeader("Content-type", "application/json");
	        // posting
//		    httpResponse = httpclient.execute(httpPost);
	        httpClient.execute(httpPost);

			Log.i("JSONFunctions", "SendJSONTo: "+httpPost.getURI());
		    
//		    // response handler
//		    BasicResponseHandler responseHandler = new BasicResponseHandler();
//            String strResponse = null;
//            if (httpResponse != null) {
//                strResponse = responseHandler.handleResponse(httpResponse);
//            }
//            Log.e("JSONFunctions", "SendJSON response: "+strResponse);
            
		    
		} catch (UnsupportedEncodingException e) {
			Log.e("JSONFunctions", "SendJSON Exception: unsupportedEncoding ");
			e.printStackTrace();
	        httpClient.getConnectionManager().shutdown();
	        messageSent = false;
			return messageSent;
		} catch (ClientProtocolException e) {
			Log.e("JSONFunctions", "SendJSON Exception: clientProtocol ");
			e.printStackTrace();
	        httpClient.getConnectionManager().shutdown();
			messageSent = false;
			return messageSent;
		} catch (IOException e) {
			Log.e("JSONFunctions", "SendJSON Exception: IOException ");
			e.printStackTrace();
	        httpClient.getConnectionManager().shutdown();
			messageSent = false;
			return messageSent;
		}
		
        httpClient.getConnectionManager().shutdown();
		messageSent = true;
		return messageSent;
	}
	
	
	public static HttpClient getNewHttpClient() {
		try {
			KeyStore trustStore = KeyStore.getInstance(KeyStore
					.getDefaultType());
			trustStore.load(null, null);

			SSLSocketFactory sf = new MySSLSocketFactory(trustStore);
			sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

			HttpParams params = new BasicHttpParams();
			HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
			HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);

			SchemeRegistry registry = new SchemeRegistry();
			registry.register(new Scheme("http", PlainSocketFactory
					.getSocketFactory(), 80));
			registry.register(new Scheme("https", sf, 443));

			ClientConnectionManager ccm = new ThreadSafeClientConnManager(
					params, registry);

			return new DefaultHttpClient(ccm, params);
		} catch (Exception e) {
			e.printStackTrace();
			return new DefaultHttpClient();
		}
	}
		
}
