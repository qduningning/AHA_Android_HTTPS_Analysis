package at.iaik.aha;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import android.util.Log;



public class mySSLHandshake implements Runnable {
	private static final String TAG = "mySSLHandshake";
	String host_; 
	String trafficC_;
	String traffic_;
	
	InputStream is_;
	OutputStream os_;
	
	InputStream isC_;
	OutputStream osC_;
	
	BufferedReader br_;
	BufferedWriter bw_;
	
	BufferedReader brC_;
	BufferedWriter bwC_;
	
	SSLSocket sslsocket;
	
	
	
	
	public mySSLHandshake(String host, String traffic, InputStream inputStream, OutputStream outputStream) {
		host_ = host;
		isC_ = inputStream;
		osC_ = outputStream;
		trafficC_ = traffic;
	}
	

	public void run() {
		try {
			Log.i(TAG, "Hello from HandShake-Thread");
			SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
			sslsocket = (SSLSocket) sslsocketfactory.createSocket();
			sslsocket.setSoTimeout(3000);
			sslsocket.connect(new InetSocketAddress(host_, 443), 3000);
			sslsocket.setUseClientMode(true);

			if(!sslsocket.isConnected()) {
				Log.e(TAG, "No Connection to " + host_);
				return;
			}
				
			sslsocket.addHandshakeCompletedListener(new HandshakeCompletedListener() {
				
				public void handshakeCompleted(HandshakeCompletedEvent e) {
					Log.i(TAG, "HANDSHAKE COMPLETE\nHost: " + host_);
//					Main.addLogToList("HANDSHAKE COMPLETE\nHost: " + host_);
					
					try {
						is_ = sslsocket.getInputStream();
						os_ = sslsocket.getOutputStream();
						
						br_ = new BufferedReader(new InputStreamReader(is_));
						bw_ = new BufferedWriter(new OutputStreamWriter(os_));
					
						brC_ = new BufferedReader(new InputStreamReader(isC_));
						bwC_ = new BufferedWriter(new OutputStreamWriter(osC_));
						
						
						
						bw_.write(trafficC_+"\n");
						bw_.flush();
						
						
						

						byte[] buffer = new byte[1024];
						int len = is_.read(buffer);
						while (len != -1) {
							osC_.write(buffer, 0, len);
							osC_.flush();
						    len = is_.read(buffer);
						}
//						while(br_.ready() && (traffic_ = br_.readLine()) != null) {
//							bwC_.write(traffic_ + "\n");
//							bwC_.flush();
//						}
						
						bw_.close();
						br_.close();
						
					} catch (SocketTimeoutException e1) {
						Log.i(TAG, "Expected Timeout");
//						Main.addLogToList("Timeout");
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					
				} //end HandshakeComplete
				
			});	//end HandShakeListener
			
			sslsocket.startHandshake();
			sslsocket.close();
			
			
		} catch (Exception e) {
			Log.e(TAG, "Error during HandShake... Internet? " + e);
			Main.addLogToList("No connection to " + host_ + " possible.\nCheck your internet connection!");
		}
	}
}
