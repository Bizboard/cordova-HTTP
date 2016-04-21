/**
 * A HTTP plugin for Cordova / Phonegap
 */
package com.synconset;

import org.apache.cordova.CallbackContext;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.lang.String;
import java.util.concurrent.atomic.AtomicBoolean;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.HostnameVerifier;

import java.util.Iterator;

import android.util.Log;

import com.github.kevinsawicki.http.HttpRequest;
 
public abstract class CordovaHttp {
    protected static final String TAG = "CordovaHTTP";
    protected static final String CHARSET = "UTF-8";
    
    private static AtomicBoolean sslPinning = new AtomicBoolean(false);
    private static AtomicBoolean acceptAllCerts = new AtomicBoolean(false);
    private static AtomicBoolean validateDomainName = new AtomicBoolean(true);

    private String urlString;
    private Map<?, ?> params;
    protected Map<String, Boolean> options;
    private Map<String, String> headers;
    private CallbackContext callbackContext;
    
    public CordovaHttp(String urlString, Map<?, ?> params, Map<String, String> headers, Map<String, Boolean> options, CallbackContext callbackContext) {
        this.urlString = urlString;
        this.params = params;
        this.headers = headers;
        this.options = options;
        this.callbackContext = callbackContext;
    }
    
    public static void enableSSLPinning(boolean enable) {
        sslPinning.set(enable);
        if (enable) {
            acceptAllCerts.set(false);
        }
    }
    
    public static void acceptAllCerts(boolean accept) {
        acceptAllCerts.set(accept);
        if (accept) {
            sslPinning.set(false);
        }
    }

    public static void validateDomainName(boolean accept) {
        validateDomainName.set(accept);
    }

    protected String getUrlString() {
        return this.urlString;
    }
    
    protected Map<?, ?> getParams() {
        return this.params;
    }
    
    protected Map<String, String> getHeaders() {
        return this.headers;
    }
    
    protected CallbackContext getCallbackContext() {
        return this.callbackContext;
    }
    
    private String my_implode(String spacer, List<String> in_array){
        String res = "";

         for (String value : in_array) {
             if(value.substring(value.length() - 1) != ";"){
                 value += ";";
             }
		    res += value;
	    }
        return res;
    }
    
    protected HttpRequest setupSecurity(HttpRequest request, Map<String, Boolean> options) {
        Boolean acceptAllCertsFlag = options.containsKey("acceptAllCerts") ? options.get("acceptAllCerts") : acceptAllCerts.get();
        Boolean validateDomainNameFlag = options.containsKey("validateDomainName") ? options.get("validateDomainName") : validateDomainName.get();
        Boolean sslPinningFlag = options.containsKey("sslPinning") ? options.get("sslPinning") : sslPinning.get();
        
        
        if (acceptAllCertsFlag) {
            request.trustAllCerts();
        }
        if (!validateDomainNameFlag) {
            request.trustAllHosts();
        }
        if (sslPinningFlag) {
            request.pinToCerts();
        }
        return request;
    }
    
    protected void respondWithError(int status, String msg) {
        try {
            JSONObject response = new JSONObject();
            response.put("status", status);
            response.put("error", msg);
            this.callbackContext.error(response);
        } catch (JSONException e) {
            this.callbackContext.error(msg);
        }
    }
    
    protected void respondWithError(String msg) {
        this.respondWithError(500, msg);
    }

    protected void addResponseHeaders(HttpRequest request, JSONObject response) throws JSONException {
        Map<String, List<String>> headers = request.headers();
        Map<String, String> parsed_headers = new HashMap<String, String>();
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            String key = entry.getKey();
            List<String> value = entry.getValue();
            if ((key != null) && (!value.isEmpty())) {
                String values;
                if(value.size() == 1){
                    values = value.get(0);
                } else {
                    values = my_implode(";", value);
                }
                parsed_headers.put(key, values);
            }
        }
        response.put("headers", new JSONObject(parsed_headers));
    }
}
