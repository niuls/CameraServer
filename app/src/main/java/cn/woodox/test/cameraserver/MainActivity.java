package cn.woodox.test.cameraserver;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;


public class MainActivity extends Activity implements SurfaceHolder.Callback,
		Camera.PreviewCallback {
	private SurfaceView mSurfaceview = null; // SurfaceView对象：(视图组件)视频显示
	private SurfaceHolder mSurfaceHolder = null; // SurfaceHolder对象：(抽象接口)SurfaceView支持类
	private Camera mCamera = null; // Camera对象，相机预览
	private byte frameRaw[] = null;
	private boolean haveData = false;
	private Bitmap bmp = null;
	private ImageView ivBox;
	private byte jpgBytes[];

	private TextView tvIP, tvPort, tvLog;
	private Button btnConnect;

	private LocationManager locationManager;
	private String targetAddr = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		//禁止屏幕休眠
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
				WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		// 获取系统LocationManager服务
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		DebugLog.w("location:"+location.toString());
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, new LocationListener() {
			@Override
			public void onLocationChanged(final Location location) {
				DebugLog.w("...");
				new Thread() {
					@Override
					public void run() {
						super.run();
						if (targetAddr == null) {
							return;
						}
						try {
							String addr[] = targetAddr.split(":");
							Socket socket = new Socket(addr[0], Integer.parseInt(addr[1]));
							DataOutputStream os = new DataOutputStream(socket.getOutputStream());
							os.writeUTF("Position:\nLongitude:" + location.getLongitude() + "\nLatitude:" +
									location.getLatitude() + "\nAltitude:" + location.getAltitude() +
									"\nAccuracy:" + location.getAccuracy());
							os.flush();
							os.close();
							socket.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}.start();
			}

			@Override
			public void onStatusChanged(String provider, int status, Bundle extras) {

			}

			@Override
			public void onProviderEnabled(final String provider) {
				DebugLog.w("...");
				new Thread() {
					@Override
					public void run() {
						super.run();
						if (targetAddr == null) {
							return;
						}
						try {
							Location location = locationManager
									.getLastKnownLocation(provider);
							String addr[] = targetAddr.split(":");
							Socket socket = new Socket(addr[0], Integer.parseInt(addr[1]));
							DataOutputStream os = new DataOutputStream(socket.getOutputStream());
							os.writeUTF("Position:\nLongitude:" + location.getLongitude() + "\nLatitude:" +
									location.getLatitude() + "\nAltitude:" + location.getAltitude() +
									"\nAccuracy:" + location.getAccuracy());
							os.flush();
							os.close();
							socket.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}.start();
			}

			@Override
			public void onProviderDisabled(String provider) {

			}
		});


		findViews();

		new Thread() {
			@Override
			public void run() {
				startServer();
			}
		}.start();
		System.out.println("started!");

	}

	@Override
	protected void onStart() {
		mSurfaceHolder = mSurfaceview.getHolder(); // 绑定SurfaceView，取得SurfaceHolder对象
		mSurfaceHolder.addCallback(this); // SurfaceHolder加入回调接口
		mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);// 设置显示器类型，setType必须设置

		super.onStart();
	}

	void findViews() {
		tvIP = (TextView) findViewById(R.id.tvIP);
		tvPort = (TextView) findViewById(R.id.tvPort);
		tvLog = (TextView) findViewById(R.id.tvLog);
		btnConnect = (Button) findViewById(R.id.btnConnect);
		btnConnect.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				new Thread() {
					@Override
					public void run() {
						super.run();
						connectServer();
					}
				}.start();
			}
		});

		mSurfaceview = (SurfaceView) findViewById(R.id.camera_preview);
		ivBox = (ImageView) findViewById(R.id.ivBox);
	}

	private void startServer() {
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

			while (true) {
				Socket s = ss.accept();
				DataInputStream is = new DataInputStream(s.getInputStream());
				targetAddr = is.readUTF();
				DataOutputStream os = new DataOutputStream(s.getOutputStream());
				while (true) {
					if (!haveData) {
						try {
							Thread.sleep(20);
						} catch (Exception e) {
							e.printStackTrace();
						}
						continue;
					}
					os.writeInt(jpgBytes.length);
					os.write(jpgBytes);
//				os.write("服务器连接成功！\n".getBytes("utf-8"));
					os.flush();
					haveData = false;
				}
//				os.close();
//				s.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	Handler uiHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (msg.what == 0x123) {
//				byte[] ip = msg.getData().getByteArray("ip");
				int port = msg.getData().getInt("port");
				try {
//					tvIP.setText(ip[0]+"."+ip[1]+"."+ip[2]+"."+ip[3]);
					tvIP.setText(getLocalIpAddress());
					tvPort.setText("" + port);
				} catch (Exception e) {
					e.printStackTrace();
					tvIP.setText("Null");
					tvPort.setText("Null");
				}
			}
			if (msg.what == 0x456) {
//				tvLog.append(msg.getData().getString("log"));
				ivBox.setImageBitmap(bmp);
			}
		}
	};

	public String getLocalIpAddress() {
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					//过滤掉回环地址和IPv6
					if (!inetAddress.isLoopbackAddress() && !(inetAddress instanceof Inet6Address)) {
						return inetAddress.getHostAddress().toString();
					}
				}
			}
		} catch (SocketException e) {
			e.printStackTrace();
		}
		return null;
	}

	void connectServer() {
		try {
			Socket socket = new Socket(tvIP.getText().toString(), 30000);

			DataInputStream dataInput = new DataInputStream(socket.getInputStream());
			while (true) {
				int size = dataInput.readInt();
				byte[] data = new byte[size];
				int len = 0;
				while (len < size) {
					len += dataInput.read(data, len, size - len);
				}
				bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
				DebugLog.e("lenth:" + data.length);

				Message msg = new Message();
				msg.what = 0x456;
				uiHandler.sendMessage(msg);
			}

//			dataInput.close();
//			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		InitCamera();
	}

	/**
	 * 初始化摄像头
	 */
	private void InitCamera() {
		try {
			mCamera = Camera.open();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		try {
			if (mCamera != null) {
				mCamera.setPreviewCallback(null); // ！！这个必须在前，不然退出出错
				mCamera.stopPreview();
				mCamera.release();
				mCamera = null;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		try {
			if (mCamera != null) {
				mCamera.setPreviewDisplay(mSurfaceHolder);
				mCamera.startPreview();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		// TODO Auto-generated method stub
		if (mCamera == null) {
			return;
		}
		mCamera.stopPreview();
		mCamera.setPreviewCallback(this);
		mCamera.setDisplayOrientation(0); //设置横行录制
		//获取摄像头参数
		Camera.Parameters parameters = mCamera.getParameters();
		parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
		parameters.setRotation(180);
		mCamera.setParameters(parameters);

		mCamera.startPreview();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		if (null != mCamera) {
			mCamera.setPreviewCallback(null); // ！！这个必须在前，不然退出出错
			mCamera.stopPreview();
			mCamera.release();
			mCamera = null;
		}
	}

	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {
		ByteArrayOutputStream jpgStream = new ByteArrayOutputStream();
		Camera.Size size = mCamera.getParameters().getPreviewSize();
		if (haveData)
			return;
		try {
			YuvImage image = new YuvImage(data, ImageFormat.NV21, size.width, size.height, null);
			if (image != null) {
				image.compressToJpeg(new Rect(0, 0, size.width, size.height), 80, jpgStream);
				jpgStream.flush();

				jpgBytes = jpgStream.toByteArray();

				frameRaw = data;
				haveData = true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
