package com.github.bharatmicrosystems.gatest;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.net.ssl.SSLContext;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;

public class GAHttpClient {

	private String url;
	private File cert = null;
	private String certPass = null;
	private StatusLine statusLine;
	private Header[] headers;
	private Header[] responseHeaders;
	private String statusCode;
	private byte[] responseContent;
	private String proxyHost, proxyUsername, proxyPassword;
	private int proxyPort;
	private String basicUsername;
	private CloseableHttpClient httpclient;
	private RequestConfig config;
	private BasicCredentialsProvider credsProvider;
	private String basicPassword;
	private byte[] requestStream;

	public String getBasicUsername() {
		return basicUsername;
	}

	public GAHttpClient setBasicUsername(String basicUsername) {
		this.basicUsername = basicUsername;
		return this;
	}

	public String getBasicPassword() {
		return basicPassword;
	}

	public GAHttpClient setBasicPassword(String basicPassword) {
		this.basicPassword = basicPassword;
		return this;
	}

	public byte[] getRequestStream() {
		return requestStream;
	}

	public GAHttpClient setRequestStream(byte[] requestStream) {
		this.requestStream = requestStream;
		return this;
	}

	public String getProxyHost() {
		return proxyHost;
	}

	public GAHttpClient setProxyHost(String proxyHost) {
		this.proxyHost = proxyHost;
		return this;
	}

	public int getProxyPort() {
		return proxyPort;
	}

	public GAHttpClient setProxyPort(int proxyPort) {
		this.proxyPort = proxyPort;
		return this;
	}

	public String getProxyUsername() {
		return proxyUsername;
	}

	public GAHttpClient setProxyUsername(String proxyUsername) {
		this.proxyUsername = proxyUsername;
		return this;
	}

	public String getProxyPassword() {
		return proxyPassword;
	}

	public GAHttpClient setProxyPassword(String proxyPassword) {
		this.proxyPassword = proxyPassword;
		return this;
	}

	public String getStatusCode() {
		return statusCode;
	}

	public GAHttpClient setStatusCode(String statusCode) {
		this.statusCode = statusCode;
		return this;
	}

	public byte[] getResponseContent() {
		return responseContent;
	}

	public GAHttpClient setResponseContent(byte[] responseContent) {
		this.responseContent = responseContent;
		return this;
	}

	public String getUrl() {
		return url;
	}

	public GAHttpClient setUrl(String url) {
		this.url = url;
		return this;
	}

	public File getCert() {
		return cert;
	}

	public GAHttpClient setCert(File cert) {
		this.cert = cert;
		return this;
	}

	public String getCertPass() {
		return certPass;
	}

	public GAHttpClient setCertPass(String certPass) {
		this.certPass = certPass;
		return this;
	}

	private void setResponseVars(HttpEntity entity, CloseableHttpResponse response) throws IOException {
		this.statusLine = response.getStatusLine();
		this.responseHeaders = response.getAllHeaders();
		this.responseContent = EntityUtils.toByteArray(entity);
	}

	public SSLConnectionSocketFactory getSSLContext() throws KeyManagementException, NoSuchAlgorithmException,
			KeyStoreException, CertificateException, IOException {
		// Trust own CA and all self-signed certs
		SSLContext sslcontext = SSLContexts.custom()
				.loadTrustMaterial(cert, certPass.toCharArray(), new TrustSelfSignedStrategy()).build();
		// Allow TLSv1 protocol only
		SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslcontext, new String[] { "TLSv1" }, null,
				SSLConnectionSocketFactory.getDefaultHostnameVerifier());
		return sslsf;
	}

	public GAHttpClient httpGet() throws IOException, KeyManagementException, NoSuchAlgorithmException,
			KeyStoreException, CertificateException {
		build();
		HttpGet httpget = new HttpGet(url);
		System.out.println("Executing request " + httpget.getRequestLine());
		CloseableHttpResponse response = httpclient.execute(httpget);
		try {
			HttpEntity entity = response.getEntity();
			System.out.println("----------------------------------------");
			System.out.println(response.getStatusLine());
			setResponseVars(entity, response);
			return this;
		} finally {
			response.close();
		}
	}

	public GAHttpClient httpPost() throws IOException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException, CertificateException {
		build();
		HttpPost httpPost = new HttpPost(url);

		ByteArrayEntity requestEntity = new ByteArrayEntity(requestStream);
		httpPost.setEntity(requestEntity);
		if (headers != null) {
			httpPost.setHeaders(headers);
		}
		CloseableHttpResponse response = httpclient.execute(httpPost);
		try {
			HttpEntity entity = response.getEntity();
			System.out.println("----------------------------------------");
			System.out.println(response.getStatusLine());
			setResponseVars(entity, response);
			return this;
		} finally {
			response.close();
		}
	}


	private void build() throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException,
			CertificateException, IOException {
		if (cert == null && proxyHost == null && proxyUsername == null && basicUsername == null) {
			httpclient = HttpClients.createDefault();
		} else {
			HttpClientBuilder builder = HttpClients.custom();
			if (cert != null) {
				builder.setSSLSocketFactory(getSSLContext());
			}
			if (proxyHost != null || basicUsername != null) {
				this.credsProvider = new BasicCredentialsProvider();
				if (proxyHost != null) {
					credsProvider.setCredentials(new AuthScope(proxyHost, proxyPort),
							new UsernamePasswordCredentials(proxyUsername, proxyPassword));
					HttpHost proxy = new HttpHost(proxyHost, proxyPort);
					config = RequestConfig.custom().setProxy(proxy).build();
				}
				if (basicUsername != null) {
					credsProvider.setCredentials(new AuthScope(new URL(url).getHost(), new URL(url).getPort()),
							new UsernamePasswordCredentials(basicUsername, basicPassword));
				}
				builder.setDefaultCredentialsProvider(credsProvider);
			}
			httpclient = builder.build();
		}

	}

	public static void main(String args[]) throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException,
			CertificateException, IOException {
		GAHttpClient client = new GAHttpClient();
		System.out.println(new String(client.setUrl("http://ptsv2.com/t/p0r5m-1554057245/post").setRequestStream("q=Hello".getBytes()).setBasicUsername("gaurav").setBasicPassword("agarwal").httpPost().getResponseContent()));
	}

	public StatusLine getStatusLine() {
		return statusLine;
	}

	public GAHttpClient setStatusLine(StatusLine statusLine) {
		this.statusLine = statusLine;
		return this;
	}

	public Header[] getHeaders() {
		return headers;
	}

	public GAHttpClient setHeaders(Header[] headers) {
		this.headers = headers;
		return this;
	}

	public Header[] getResponseHeaders() {
		return responseHeaders;
	}

	public GAHttpClient setResponseHeaders(Header[] responseHeaders) {
		this.responseHeaders = responseHeaders;
		return this;
	}

}
