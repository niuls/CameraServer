package cn.woodox.test.cameraserver;

import android.app.Activity;
import android.os.Bundle;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;


public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		new Thread(){
			@Override
			public void run() {
				startServer();
			}
		}.start();
		System.out.println("started!");
	}

	private void startServer(){
		ServerSocket ss;
		try {
			ss = new ServerSocket(34567);
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
}
