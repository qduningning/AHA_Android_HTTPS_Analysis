package at.iaik.aha;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Vector;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

@SuppressLint("HandlerLeak")
public class Main extends Activity  {
	
    private String TAG = "Main.java";
	private Button btn_Del;
	private ToggleButton btn_Phishing;
	private ToggleButton btn_Toggle;
    @SuppressWarnings("unused")
	private TextView txt_Log;
    @SuppressWarnings("unused")
    private ScrollView scroll;
    private ListView list;
    private static Vector<String> listContent = new Vector<String>();
    private static ArrayAdapter<String> list_adapter;
    private Boolean isRunning;
    private Boolean phishing;
    private String targetPortHTTPS;
    private String localPortHTTPS;
	private static Handler logHandler;
	public static Vector<String> hostList = new Vector<String>();

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        setTitle(R.string.app_title);
		Log.i(TAG , "onCreate()");
		checkForRoot();
		init();
        initUI();
    }

	private void init() {
		isRunning = false;
		phishing = false;
		targetPortHTTPS = "443";
		localPortHTTPS  = "1236";
	}

	private void checkForRoot() {
		try {
			Runtime.getRuntime().exec("su");
		} catch (IOException e) {
			Toast.makeText(getApplicationContext(), "This App needs ROOT, sorry!", Toast.LENGTH_LONG).show();
		}
			//TODO: close App
	}

	private void initUI() {
		Log.i(TAG , "initUI()");
		
        btn_Del = 		(Button) 		this.findViewById(R.id.btn_del);
        btn_Phishing = 	(ToggleButton) 	this.findViewById(R.id.btn_phishing);
        btn_Toggle = 	(ToggleButton)	this.findViewById(R.id.btn_toggle);
        txt_Log = 		(TextView) 		this.findViewById(R.id.txt_log);
        scroll = 		(ScrollView)	this.findViewById(R.id.scroll);
        list = 			(ListView)		this.findViewById(R.id.list);
        
        btn_Toggle.setText("Analyse");
        btn_Toggle.setTextOn("Analyse");
        btn_Toggle.setTextOff("Analyse");
        
        btn_Phishing.setText("Phishing");
        btn_Phishing.setTextOn("Phishing");
        btn_Phishing.setTextOff("Phishing");
        
        list_adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, listContent);
        list.setAdapter(list_adapter);
        
        btn_Toggle.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	isRunning = !isRunning;
            	if(isRunning)
            		startAHA();
            	else
            		stopAHA();
            }
        });
        
        btn_Phishing.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	phishing = !phishing;
            	setPhishing();
            }
        });
        
        btn_Del.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	list_adapter.notifyDataSetChanged();
                listContent.clear();
                hostList.clear();
            }
        });

        list.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				// TODO Auto-generated method stub
				Log.e("LIST", "onClick()");
			}
        	
		});
        
        
        logHandler = new Handler() {
//            private StringBuffer buffer = new StringBuffer("");
            public void handleMessage(Message msg) {
                String newMsg = (String)msg.obj;
//                buffer.append(newMsg).append("\n");
//                txt_Log.setText(buffer);
//                scroll.scrollTo(0, txt_Log.getHeight()+99999);         
                list_adapter.notifyDataSetChanged();
                listContent.add(0, newMsg);
            }
        };
	}
	
	public static void sendLogMsg(String s) {
//        Message msg = Message.obtain();
//        msg.obj = s;
//        logHandler.sendMessage(msg);
	}
	
	public static void addLogToList(String s) {
		Message msg = Message.obtain();
        msg.obj = s;
		logHandler.sendMessage(msg);
	}

	@Override
	protected void onDestroy() {
		stopAHA();
		list_adapter.clear();
		super.onDestroy();
	}

	protected void stopAHA() {
		Log.i(TAG , "stopAHA()");
		configForwarding(false);
		SSLServer.instance().stop();
	}

	protected void startAHA() {
		Log.i(TAG , "startAHA()");
		configForwarding(true);
		SSLServer.instance().start(localPortHTTPS, getApplicationContext());
	}
	
	protected void setPhishing() {
		Log.i(TAG , "setPhishing(" + phishing + ")");
		SSLServer.instance().setPhishing(phishing);
	}

	private void configForwarding(Boolean enable) {
		try {						
			Process process = Runtime.getRuntime().exec("su");
	        DataOutputStream os = new DataOutputStream(process.getOutputStream());
	        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
	        
	        if (enable) {
		        //set config, enables forwarding
		        os.writeBytes("iptables -P INPUT ACCEPT; iptables -P FORWARD ACCEPT; iptables -P OUTPUT ACCEPT\n");
		        os.flush();
		        
		        //set forwarding true
		        os.writeBytes("echo '1' > /proc/sys/net/ipv4/ip_forward\n");
		        os.flush();
		        
		        //set targeted port to forward to proxy
		        os.writeBytes("iptables -t nat -A PREROUTING -p tcp --destination-port " + targetPortHTTPS + " -j REDIRECT --to-port " + localPortHTTPS + "\n");
		        os.flush();
	        }
	        else {
	        	for(int i=0; i<10; i++) {	
		        	//delete forwarding rule
			        os.writeBytes("iptables -t nat -D PREROUTING -p tcp --destination-port " + targetPortHTTPS + " -j REDIRECT --to-port " + localPortHTTPS + "\n");
			        os.flush();
	        	}
	        }
		        
	        //check if our config was done
	        os.writeBytes("iptables --list PREROUTING -t nat -n\n");
	        os.flush();
	        
	        //exit
	        os.writeBytes("exit\n");
	        os.flush();
	        
	        process.waitFor();
	        
	        printResponse(bufferedReader, 2);
	
	        
		} catch (IOException e) {
 			Log.e(TAG, "###################################ERROR WITH ROOT");
 			e.printStackTrace();
 		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void printResponse(BufferedReader bufferedReader, int offset) throws IOException {
	    String line;
	    Log.d(TAG, "Redir-Rules:");
	    while (bufferedReader.ready()) 
	    {
	    	line = bufferedReader.readLine();
	  
	    	if(--offset < 0 )
	    		Log.d(TAG, line);
	    }
	    Log.d(TAG, "{}");
	}
	
	public static boolean newHost(String host) {
		if(!hostList.contains(host)) {
			hostList.add(host);
			return true;
		}
		else
			return false;
	}
}
