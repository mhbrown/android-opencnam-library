/*
   Copyright 2012 Thomas Dignan <tom@tomdignan.com>

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package com.tomdignan.android.opencnam.library;

import java.io.IOException;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import android.content.Context;
import android.util.Log;

/**
 * Reusable OpenCNAM request. Can return result in XML, JSON, or TEXT formats.
 * 
 * @author Tom Dignan
 */
abstract public class OpenCNAMRequest implements Request {
	//=========================================================================
	// Static Members
	//=========================================================================
	
	/** Tag for identifying class in Log */
	private static final String TAG = "OpenCNAMRequest";
	
	/** Base URL for request. MUST include trailing slash. */
	private static final String OPENCNAM_BASE_URL = "https://api.opencnam.com/v2/phone/";
	
	/** Identifier for api_key parameter */
	private static final String PARAM_AUTH_TOKEN = "auth_token";
	
	/** Identifier for format parameter */
	private static final String PARAM_FORMAT = "format";
	
	/** Identifier for username parameter */
	private static final String PARAM_ACCOUNT_SID = "account_sid";
	
	/** Format value for XML */
	public static final String FORMAT_XML = "xml";
	
	/** Format value for JSON */
	public static final String FORMAT_JSON = "json";
	
	/** Format values for plain text */
	public static final String FORMAT_TEXT = "text";
	
	
	//=========================================================================
	// Instance Members
	//=========================================================================
	
	/** Phone number to look up CNAM */
	private String mPhoneNumber = null;
	
	/** Serialization format for response. Default is TEXT. */
	private String mSerializationFormat = FORMAT_TEXT;
	
	/** Optional username parameter */
	private String mAccountSid = null;
	
	/** Optional API key */
	private String mAuthToken = null;
	
	/** Reusable HttpClient instance */
	private HttpClient mHttpClient;
	
	/** Reusable HttpPost instance */
	private HttpGet mHttpGet = new HttpGet();
	
	/** Reusable ResponseHandler instance */
	private BasicResponseHandler mResponseHandler = new BasicResponseHandler();
	
	/** The context, if set */
	private Context mContext;
	
	//=========================================================================
	// Constructors
	//=========================================================================
	
	/** Default constructor 
	 * @throws IOException 
	 * @throws CertificateException 
	 * @throws NoSuchAlgorithmException 
	 * @throws KeyStoreException 
	 * @throws UnrecoverableKeyException 
	 * @throws KeyManagementException */
	public OpenCNAMRequest() {
		// Otherwise we will get a ClientProtocolException, Not trusted server certificate.
		mHttpClient = getHttpClient();
	}
	
	/**
	 * This consturctor is needed for SSLCertifiedOpenCNAMRequest
	 * @param context
	 */
	public OpenCNAMRequest(Context context) {
		mContext = context;
		mHttpClient = getHttpClient();
	}
	
	/**
	 *  Implement this to construct the HttpClient instance that OpenCNAMRequest
	 *  will use. This is meant to be a clean way of being able to fall back on
	 *  the version that does not use special a SSLSocketFactory if it fails for
	 *  some reason. Some android devices do not recognize the cert that is on
	 *  api.opencnam.com, so I made a custom keystore just for the library
	 */
	abstract protected HttpClient getHttpClient();
	 
	//=========================================================================
	// Accessors
	//=========================================================================
	
	/**
	 * Returns the context if set, else null.
	 */
	protected Context getContext() {
		return mContext;
	}

	/**
	 * Sets the account sid for professional account holders.
	 */
	public void setAccountSid(String username) {
		mAccountSid = username;
	}
	
	/** 
	 * Set the auth token for professional account holders.
	 * @param apiKey
	 */
	public void setAuthToken(String apiKey) {
		mAuthToken = apiKey;
	}

	/**
	 * Set the phone number to be looked up when execute() is called.
	 * 
	 * Phone numbers will be restricted to the rightmost 10 digits, because
	 * that is what opencnam requires. If you attempt to set a phone number
	 * that is less than 10 digits, an exception will be thrown.
	 * 
	 * @thanks to @rdegges for input sanitization fix.
	 * 
	 * @param phoneNumber
	 */
	public void setPhoneNumber(String phoneNumber) throws IllegalArgumentException {
		phoneNumber = phoneNumber.replaceAll("[^\\d]", "");
		int length = phoneNumber.length();
		if (length < 10) {
			throw new IllegalArgumentException("Phone numbers must be at least 10 digits");
		} else if (length > 10) {
			phoneNumber = phoneNumber.substring(length - 10, length);
			mPhoneNumber = phoneNumber;
		} else {
			mPhoneNumber = phoneNumber;
		}
	}
	
	/**
	 * Sets the serialization format for the response. T
	 * 
	 * @param format -- must be one of the FORMAT_* constants defined in this class.
	 */
	public void setSerializationFormat(String format) {
		if (format != null && 
				(  format.equals(FORMAT_JSON) 
				|| format.equals(FORMAT_TEXT)
				|| format.equals(FORMAT_XML)) ) {
			mSerializationFormat = format;
		} else {
			throw new IllegalArgumentException("Invalid format. Must be one of the FORMAT_* constants.");
		}
	}
	
	//=========================================================================
	// Request Interface
	//=========================================================================
	
	/**
	 * Returns the serialized version of the desired output format on success, 
	 * or throws an exception on failure. You will need to deserialize this 
	 * result depending on the format you chose.
	 * 
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 * @returns response
	 */
	@Override
	public String execute() throws ClientProtocolException, IOException {
		mHttpGet.setURI(makeRequestURI());
		Log.d(TAG, "requestLine="+mHttpGet.getRequestLine());
		Log.d(TAG, "params="+mHttpGet.getParams().toString());
		HttpResponse response = mHttpClient.execute(mHttpGet);
		// It is slower to convert the response to a string first, but so much easier to
		// debug and such a negligible speed consideration when the payload is this small
		// that it is the best way.
		return mResponseHandler.handleResponse(response);
	}	
	
	//=========================================================================
	// Private Helpers
	//=========================================================================

	/** 
	 * Build out an appropriate URI for this request.
	 * 
	 * @return params
	 */
	private URI makeRequestURI() {
		StringBuilder builder = new StringBuilder();
		
		builder.append(OPENCNAM_BASE_URL);
		builder.append(mPhoneNumber);
		
		builder.append("?");
		builder.append(PARAM_FORMAT);
		builder.append("=");
		builder.append(mSerializationFormat);
		
		if (mAuthToken != null) {
			builder.append("&");
			builder.append(PARAM_AUTH_TOKEN);
			builder.append("=");
			builder.append(mAuthToken);
		}
		
		if (mAccountSid != null) {
			builder.append("&");
			builder.append(PARAM_ACCOUNT_SID);
			builder.append("=");
			builder.append(mAccountSid);
		}

		return URI.create(builder.toString());
	}
}
