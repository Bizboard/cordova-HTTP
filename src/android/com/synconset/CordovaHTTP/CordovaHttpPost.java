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
            HttpRequest request = HttpRequest.post(this.getUrlString());;
            if(this.options.containsKey("noRedirect")){
                 request.followRedirects(false);
            }
            this.setupSecurity(request, this.options);
            request.acceptCharset(CHARSET);
            request.headers(this.getHeaders());
            
            try {
                if(this.options.containsKey("isFormData") && this.getParams().containsKey("data")){
                    // Use form-encoded data
                    byte[] data = this.getParams().get("data").toString().getBytes("UTF-8");
                    request.contentType(HttpRequest.CONTENT_TYPE_FORM);
                    request.send(data);
                } else {
                    // Use JSON-encoded data
                    JSONObject objParams = new JSONObject(this.getParams());
                    byte[] data = objParams.toString().getBytes("UTF-8");
                    request.contentType(HttpRequest.CONTENT_TYPE_JSON);
                    request.send(data);
                }
            } catch (Exception e) {
                Log.v("CHTTP", "Exception: " + e.getMessage());
            }
            
            int code = request.code();
            String body = request.body(CHARSET);
            Log.v("CHTTP", "Got response: " + body);
            JSONObject response = new JSONObject();
            this.addResponseHeaders(request, response);
            Log.v("CHTTP", "Response headers: " + response.get("headers").toString());
            String headerSetCookie = "";
            Map<String, String> setCookieHeaders = request.getConnection(). getHeaderFields().get("Set-Cookie").toString());
            for (Map.Entry<String, String> entry : setCookieHeaders) {
                String headerValue = entry.getValue();
                headerSetCookie += headerValue;
            }
            response.get('headers').get('Set-Content') = headerSetCookie;
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
