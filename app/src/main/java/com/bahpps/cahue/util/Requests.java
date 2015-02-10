package com.bahpps.cahue.util;

import android.content.Context;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.JsonRequest;
import com.bahpps.cahue.login.AuthUtils;
import com.bahpps.cahue.login.GCMUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Francesco on 09/02/2015.
 */
public class Requests {

    /**
     * Post a JSONObject
     */
    public static class JsonPostRequest extends JsonObjectRequest {

        private final Context context;

        public JsonPostRequest(Context context, String url, JSONObject jsonRequest, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
            super(url, jsonRequest, listener, errorListener);
            this.context = context;
        }

        @Override
        public Map<String, String> getHeaders() throws AuthFailureError {
            return CommUtil.generateHeaders(context);
        }
    }

    /**
     * Post a JSONArray
     */
    public static class JsonArrayPostRequest extends JsonRequest<JSONObject> {

        private final Context context;

        public JsonArrayPostRequest(Context context, String url, JSONArray jsonArray, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
            super(Method.POST, url, jsonArray.toString(), listener, errorListener);
            this.context = context;
        }

        @Override
        public Map<String, String> getHeaders() throws AuthFailureError {
            return CommUtil.generateHeaders(context);
        }

        // Need this cause we cant extend a standard class, because the cant get json arrays as a parameter
        @Override
        protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
            try {
                String jsonString =
                        new String(response.data, HttpHeaderParser.parseCharset(response.headers));
                return Response.success(new JSONObject(jsonString),
                        HttpHeaderParser.parseCacheHeaders(response));
            } catch (UnsupportedEncodingException e) {
                return Response.error(new ParseError(e));
            } catch (JSONException je) {
                return Response.error(new ParseError(je));
            }
        }
    }

}
