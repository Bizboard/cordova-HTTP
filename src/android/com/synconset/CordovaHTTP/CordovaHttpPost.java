/**
 * A HTTP plugin for Cordova / Phonegap
 */
package com.synconset;

import java.net.UnknownHostException;
import java.io.OutputStream;
import java.util.Map;

import org.apache.cordova.CallbackContext;
import org.json.JSONException;
import org.json.JSONObject;
import javax.net.ssl.SSLHandshakeException;

import android.util.Log;

import com.github.kevinsawicki.http.HttpRequest;
import com.github.kevinsawicki.http.HttpRequest.HttpRequestException;
 
public class CordovaHttpPost extends CordovaHttp implements Runnable {
    public CordovaHttpPost(String urlString, Map<?, ?> params, Map<String, String> headers, Map<String, Boolean> options, CallbackContext callbackContext) {
        super(urlString, params, headers, options, callbackContext);
    }
    
    @Override
    public void run() {
        try {
            HttpRequest request = HttpRequest.post(this.getUrlString());
            this.setupSecurity(request, this.options);
            request.acceptCharset(CHARSET);
            request.headers(this.getHeaders());
            
            try {
                JSONObject objParams = new JSONObject(this.getParams());
                byte[] data = objParams.toString().getBytes("UTF-8");
                request.header("Content-Type", "application/json");
                request.header("Content-Length", Integer.toString(data.length));
                Log.v("CHTTP", "Sending POST(" + Integer.toString(data.length) + "): " + objParams.toString());
                
                OutputStream os = request.getConnection().getOutputStream();
                os.write(data);
            } catch (Exception e) {
                Log.v("CHTTP", "Exception: " + e.getMessage());
            }
            
            int code = request.code();
            String body = request.body(CHARSET);
            Log.v("CHTTP", "Got response: " + body);
            JSONObject response = new JSONObject();
            this.addResponseHeaders(request, response);
            response.put("status", code);
            if (code >= 200 && code < 300) {
                response.put("data", body);
                this.getCallbackContext().success(response);
            } else {
                response.put("error", body);
                this.getCallbackContext().error(response);
            }
        } catch (JSONException e) {
            this.respondWithError("There was an error generating the response");
        }  catch (HttpRequestException e) {
            if (e.getCause() instanceof UnknownHostException) {
                this.respondWithError(0, "The host could not be resolved");
            } else if (e.getCause() instanceof SSLHandshakeException) {
                this.respondWithError("SSL handshake failed");
            } else {
                this.respondWithError("There was an error with the request: " + e.getMessage());
            }
        }
    }
}
