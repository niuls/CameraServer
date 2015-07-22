package cn.woodox.test.cameraserver;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.format.Formatter;
import android.widget.TextView;


import org.apache.http.protocol.HttpDateGenerator;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;


public class MainActivity extends Activity {
	private TextView tvIP,tvPort;
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
}
