package at.iaik.aha;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.security.KeyManagementException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import android.content.Context;
import android.util.Log;

public class SSLServer {
	private static SSLServer instance_ = null;
	private String localPortHTTPS_;
	private Context context_;
	private String TAG = "SSLServer.java";
	private SSLServerSocket sslServerSocket_;
	private Thread serverThread_;
	private boolean phishing_ = false;
	protected boolean run_ = true;
	
	private final static TrustManager[] trustAllCerts = new TrustManager[] {
		new X509TrustManager() {
			public X509Certificate[] getAcceptedIssuers() {return null;}
			public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {}
			public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {}
		}
	};
	
	
	public static SSLServer instance() {
		if(instance_ == null)
			instance_ = new SSLServer();
		
		return instance_;
	}
	
	public void stop() {
		Log.i(TAG ,  "stop()");
		try {
			run_ = false;
			if(sslServerSocket_ != null)
				sslServerSocket_.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
	}

	public void start(String localPortHTTPS, Context context) {
		Log.i(TAG ,  "start()");
		localPortHTTPS_ = localPortHTTPS;
		context_ = context;
		run_ = true;
		
		try {
			startKeyManagement();
			Log.i(TAG, "los");
			serverThread_ = new Thread(new Runnable() {
	            public void run() {
	                while (run_ ) {
	                    try {
	                    	Log.i(TAG, "Waiting for next Session");
							Thread thread = new Thread(new mySession(sslServerSocket_.accept(), phishing_));
							thread.setDaemon(true);
							thread.start();
							Log.i(TAG, "My Session started");
							thread.join();
							Log.i(TAG, "My Session finished");
	                    } catch (SocketException e) {
	                    	Log.e(TAG, "Socket got closed");
						} catch (IOException e) {
							e.printStackTrace();
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
	                }
	            }
			});
			serverThread_.setDaemon(true);
			serverThread_.start();
			
		} catch (Exception e) {}
		
	}

	private void startKeyManagement() throws NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException, CertificateException, IOException, KeyManagementException {
		Log.i(TAG ,  "startKeyManagement()");
    	KeyManagerFactory kmf = KeyManagerFactory.getInstance("X509");
    	SSLContext mSSLContext = SSLContext.getInstance("TLS");

		///////////////////////////////////////////////////////////////////////////
		// Generate KeyPairs
		
		Map<String, KeyPair> keyPairs = new HashMap<String, KeyPair>();
		Map<String, byte[]> encodedKeyPairs = new HashMap<String, byte[]>();
		KeyPair kp;
		
		for (String cipher : new String[] {"RSA", "DSA", "ECDSA"})	//add what you want, bzw what exist :P
		{
			if((kp = generateKeyPair(cipher)) != null) {
				keyPairs.put(cipher, kp);
				encodedKeyPairs.put(cipher, kp.getPublic().getEncoded());
			}
		}
		
		Log.d(TAG, "KeyPairs generated: " + keyPairs.size());
		
		//////////////////////////////////////////////////////////////////////
		// read KeyStore
		
		char[] storePassword = "ankelepoellitschsommer".toCharArray();
		char[] keyPassword = storePassword;
		String alias = "SSLServerSocket";
		
		KeyStore ks = KeyStore.getInstance("BKS");
		InputStream iS = context_.getResources().openRawResource(R.raw.ahakeystore);
		ks.load(iS, storePassword);
		kmf.init(ks, keyPassword);
		Certificate cert = ks.getCertificate(alias);
		
		if (cert != null)
			Log.i(TAG, "Cert loaded");
		
		
		//////////////////
		// set up SSLSocket
    	mSSLContext.init(kmf.getKeyManagers(), trustAllCerts, null); //new java.security.SecureRandom());
    	SSLServerSocketFactory mSSLServerSocketFactory = mSSLContext.getServerSocketFactory();
    	sslServerSocket_ = (SSLServerSocket)mSSLServerSocketFactory.createServerSocket(Integer.valueOf(localPortHTTPS_)); //TODO: only one connection
	}


	
	private KeyPair generateKeyPair (String cipher) {
		KeyPairGenerator kg;
		KeyPair kp = null;
		try {
			kg = KeyPairGenerator.getInstance(cipher);
		
	    	kg.initialize(512);
	    	kp = kg.generateKeyPair();
	    	PublicKey pub = kp.getPublic();
	    	PrivateKey pri = kp.getPrivate();
	    	
	    	byte[] publicKey =  pub.getEncoded();
	    	byte[] privateKey = pri.getEncoded();
	    	
	    	StringBuffer publicKeyString = new StringBuffer();
	    	StringBuffer privateKeyString = new StringBuffer();
	    	
		        for (int i = 0; i < publicKey.length; ++i) {
		        	publicKeyString.append(Integer.toHexString(0x0100 + (publicKey[i] & 0x00FF)).substring(1));
		        }
		        for (int i = 0; i < privateKey.length; ++i) {
		        	privateKeyString.append(Integer.toHexString(0x0100 + (privateKey[i] & 0x00FF)).substring(1));
		        }
		        
//	        Log.i(TAG, "Public: " + publicKeyString);
//	        Log.i(TAG, "Private: " + privateKeyString);
		} catch (NoSuchAlgorithmException e) {
			Log.e(TAG, "Algo " + cipher + " not found!");
		}
		return kp;
	}

	public void setPhishing(Boolean phishing) {
		// TODO Auto-generated method stub
		phishing_ = phishing;
	}

	
	
}
