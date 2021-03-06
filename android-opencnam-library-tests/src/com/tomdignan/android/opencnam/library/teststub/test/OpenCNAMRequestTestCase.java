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

package com.tomdignan.android.opencnam.library.teststub.test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.tomdignan.android.opencnam.library.OpenCNAMRequest;
import com.tomdignan.android.opencnam.library.SSLCertifiedOpenCNAMRequest;
import com.tomdignan.android.opencnam.library.teststub.StubApplication;

import android.test.InstrumentationTestCase;
import android.util.Log;

/**
 * Testcase for Android OpenCNAM library project.
 * 
 * @author Tom Dignan
 */
public class OpenCNAMRequestTestCase extends InstrumentationTestCase {
	private static final String TAG = "OpenCNAMRequestTestCase";
	
	private OpenCNAMRequest mOpenCNAMRequest;
	
	private static final String NUMBER = "3392033301";
	private static final String CNAM = "MASSACHUSETTS";
	private static final String USERNAME = "_REDACTED_";
	private static final String APIKEY = "_REDACTED_";
	
	private DocumentBuilder mDocumentBuilder;
	
	@Override
	protected void setUp() throws Exception {
		mOpenCNAMRequest = new SSLCertifiedOpenCNAMRequest(StubApplication.getApplication()
				.getApplicationContext());
		
		mOpenCNAMRequest.setPhoneNumber(NUMBER);
//		mOpenCNAMRequest.setUsername(USERNAME);
//		mOpenCNAMRequest.setAPIKey(APIKEY);
//		
		// Needed for the XML test
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		mDocumentBuilder = factory.newDocumentBuilder();
		
		super.setUp();
	}
	
	public void testTextRequest() {
		// TEXT is default.
		String response = null;
		
		try {
			response = (String) mOpenCNAMRequest.execute();
		} catch (ClientProtocolException e) {
			Log.e(TAG, "ClientProtocolException: " + e.getMessage());
		} catch (IOException e) {
			Log.e(TAG, "ClientProtocolException: " + e.getMessage());
		}
		
		assertNotNull(response);
		assertEquals(CNAM, response.trim());
	}
	
	public void testNumberWithLeadingCountryCode() {
		String response = null;
		mOpenCNAMRequest.setPhoneNumber("1" + NUMBER);
		try {
			response = (String) mOpenCNAMRequest.execute();
		} catch (ClientProtocolException e) {
			Log.e(TAG, "ClientProtocolException: " + e.getMessage());
		} catch (IOException e) {
			Log.e(TAG, "ClientProtocolException: " + e.getMessage());
		}
		
		assertNotNull(response);
		assertEquals(CNAM, response.trim());
	}
	
	public void testNumberWithLeadingCountryCodeAndNonNumericChars() {
		String response = null;
		mOpenCNAMRequest.setPhoneNumber("--1--*((*&(*&(&&*&basdjasjcjasca" + NUMBER);
		try {
			response = (String) mOpenCNAMRequest.execute();
		} catch (ClientProtocolException e) {
			Log.e(TAG, "ClientProtocolException: " + e.getMessage());
		} catch (IOException e) {
			Log.e(TAG, "ClientProtocolException: " + e.getMessage());
		}
		
		assertNotNull(response);
		assertEquals(CNAM, response.trim());
	}
	
	// must throw IllegalArgumentException
	public void testNumberLessThan10Digits() {
		Exception caught = null;
		try {
			mOpenCNAMRequest.setPhoneNumber("411");
		} catch (IllegalArgumentException e) {
			caught = e;
		}
		assertNotNull(caught);
	}
	
	public void testJSONRequest() {
		mOpenCNAMRequest.setSerializationFormat(OpenCNAMRequest.FORMAT_JSON);

		String response = null;
		
		try {
			response = (String) mOpenCNAMRequest.execute();
		} catch (ClientProtocolException e) {
			Log.e(TAG, "ClientProtocolException: " + e.getMessage());
		} catch (IOException e) {
			Log.e(TAG, "ClientProtocolException: " + e.getMessage());
		}
		
		assertNotNull(response);
		
		JSONObject object = null;
		
		try {
			object = new JSONObject(response);        
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		assertNotNull(object);
		
		String cnam = null;
		String number = null;
		
		try {
			cnam = object.getString("cnam");
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		assertNotNull(cnam);
		cnam = cnam.trim();
		
		try {
			number = object.getString("number");
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		assertNotNull(number);
		number = number.trim();
		
		assertEquals(cnam, CNAM);
		assertEquals(number, NUMBER);
	}
	
	// Warning, slight API changes may break this test.
	public void testXMLRequest() {
		mOpenCNAMRequest.setSerializationFormat(OpenCNAMRequest.FORMAT_XML);
		// TEXT is default.
		String response = null;
		
		try {
			response = (String) mOpenCNAMRequest.execute();
		} catch (ClientProtocolException e) {
			Log.e(TAG, "ClientProtocolException: " + e.getMessage());
		} catch (IOException e) {
			Log.e(TAG, "ClientProtocolException: " + e.getMessage());
		}
		
		assertNotNull(response);
		ByteArrayInputStream inputStream = new ByteArrayInputStream(response.getBytes());
		Document document = null;
		
		try {
			document = mDocumentBuilder.parse(inputStream);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		assertNotNull(document);
		
		Element root =  document.getDocumentElement();
		
		String rootName = root.getNodeName();
		assertNotNull(rootName);
		assertEquals(rootName, "object");
		
		NodeList children = root.getChildNodes();
		assertNotNull(children);

		int size = children.getLength();
		assertEquals(size, 2);
		
		Node cnamNode = children.item(0);
		assertNotNull(cnamNode);

		String cnamValue = cnamNode.getTextContent().trim();
		assertEquals(cnamValue, CNAM);
		
		String cnamNodeName = cnamNode.getNodeName();
		assertEquals(cnamNodeName, "cnam");
		
		Node numberNode = children.item(1);
		assertNotNull(numberNode);
		
		String numberName = numberNode.getNodeName().trim();
		
		assertNotNull(numberName);
		assertEquals(numberName, "number");
		
		String numberValue = numberNode.getTextContent().trim();
		
		assertNotNull(numberValue);
		assertEquals(numberValue, NUMBER);
	}
	
	@Override
	protected void tearDown() throws Exception {
		mOpenCNAMRequest = null;
		super.tearDown();
	}
}
