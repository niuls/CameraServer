package cn.woodox.test.cameraserver;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;


public class MainActivity extends Activity {
	private TextView tvIP,tvPort,tvLog;
	private Button btnConnect;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		findViews();

		new Thread(){
			@Override
			public void run() {
				startServer();
			}
		}.start();
		System.out.println("started!");
	}

	void findViews(){
		tvIP = (TextView)findViewById(R.id.tvIP);
		tvPort = (TextView)findViewById(R.id.tvPort);
		tvLog = (TextView)findViewById(R.id.tvLog);
		btnConnect = (Button)findViewById(R.id.btnConnect);
		btnConnect.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				new Thread(){
					@Override
					public void run() {
						super.run();
						connectServer();
					}
				}.start();
			}
		});
	}

	private void startServer(){
		ServerSocket ss;
		try {
			ss = new ServerSocket(30000);

			Bundle bundle = new Bundle();
//			bundle.putByteArray("ip",ss.getInetAddress().getAddress());
			bundle.putInt("port", ss.getLocalPort());
			Message msg = new Message();
			msg.what = 0x123;
			msg.setData(bundle);
			uiHandler.sendMessage(msg);

			while (true){
				Socket s = ss.accept();
				OutputStream os = s.getOutputStream();
				os.write("服务器连接成功！\n".getBytes("utf-8"));
				os.close();
				s.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	Handler uiHandler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			if(msg.what == 0x123){
//				byte[] ip = msg.getData().getByteArray("ip");
				int port = msg.getData().getInt("port");
				try {
//					tvIP.setText(ip[0]+"."+ip[1]+"."+ip[2]+"."+ip[3]);
					tvIP.setText(getLocalIpAddress());
					tvPort.setText(""+port);
				}catch (Exception e){
					e.printStackTrace();
					tvIP.setText("Null");
					tvPort.setText("Null");
				}
			}
			if(msg.what == 0x456){
				tvLog.append(msg.getData().getString("log"));
			}
		}
	};

	public String getLocalIpAddress() {
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					//过滤掉回环地址和IPv6
					if (!inetAddress.isLoopbackAddress()&& !(inetAddress instanceof Inet6Address)) {
						return inetAddress.getHostAddress().toString();
					}
				}
			}
		} catch (SocketException e) {
			e.printStackTrace();
		}
		return null;
	}

	void connectServer(){
		try{
			Socket socket = new Socket(tvIP.getText().toString(),30000);
			BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));

			Bundle bundle = new Bundle();
			bundle.putString("log",br.readLine()+"\n");
			Message msg = new Message();
			msg.what = 0x456;
			msg.setData(bundle);
			uiHandler.sendMessage(msg);

			br.close();
			socket.close();
		}catch (IOException e){
			e.printStackTrace();
		}
	}
}
