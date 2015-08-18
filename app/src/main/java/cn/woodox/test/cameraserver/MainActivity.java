package cn.woodox.test.cameraserver;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.location.Poi;

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
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


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
	private long lastCompTime = 0;

	private TextView tvIP, tvPort, tvLog;
	private Button btnConnect;

	//	private LocationManager locationManager;
	private String targetAddr = null;

	public LocationClient mLocationClient = null;
	public BDLocationListener myListener = new MyLocationListener();

	private int fps =0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		//禁止屏幕休眠
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
				WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		mLocationClient = new LocationClient(getApplicationContext());     //声明LocationClient类
		mLocationClient.registerLocationListener(myListener);    //注册监听函数
		initLocation();
		mLocationClient.start();

		// 获取系统LocationManager服务
		/*locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		DebugLog.w("location:" + location.toString());
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
*/

		findViews();
		new Thread() {
			@Override
			public void run() {
				startServer();
			}
		}.start();
		System.out.println("started!");

		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				Message msg = new Message();
				msg.what = 0x789;
				uiHandler.sendMessage(msg);
			}
		},1000,1000);
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
				new SendThread(s).start();

/*				DataInputStream is = new DataInputStream(s.getInputStream());
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
				}*/
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
			if(msg.what == 0x789){
				tvLog.setText("FPS: "+fps);
				fps = 0;
			}
		}
	};

	private void initLocation() {
		LocationClientOption option = new LocationClientOption();
		option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy
		);//可选，默认高精度，设置定位模式，高精度，低功耗，仅设备
		option.setCoorType("bd09ll");//可选，默认gcj02，设置返回的定位结果坐标系
		int span = 1000;
		option.setScanSpan(span);//可选，默认0，即仅定位一次，设置发起定位请求的间隔需要大于等于1000ms才是有效的
		option.setIsNeedAddress(true);//可选，设置是否需要地址信息，默认不需要
		option.setOpenGps(true);//可选，默认false,设置是否使用gps
		option.setLocationNotify(true);//可选，默认false，设置是否当gps有效时按照1S1次频率输出GPS结果
		option.setIsNeedLocationDescribe(true);//可选，默认false，设置是否需要位置语义化结果，可以在BDLocation.getLocationDescribe里得到，结果类似于“在北京天安门附近”
		option.setIsNeedLocationPoiList(false);//可选，默认false，设置是否需要POI结果，可以在BDLocation.getPoiList里得到
		option.setIgnoreKillProcess(false);//可选，默认false，定位SDK内部是一个SERVICE，并放到了独立进程，设置是否在stop的时候杀死这个进程，默认杀死
		option.SetIgnoreCacheException(false);//可选，默认false，设置是否收集CRASH信息，默认收集
		option.setEnableSimulateGps(false);//可选，默认false，设置是否需要过滤gps仿真结果，默认需要
		mLocationClient.setLocOption(option);
	}

	public class MyLocationListener implements BDLocationListener {

		@Override
		public void onReceiveLocation(final BDLocation location) {
			//Receive Location
			final StringBuffer sb = new StringBuffer(256);
//			sb.append("time : ");
//			sb.append(location.getTime());
//			sb.append("\nerror code : ");
//			sb.append(location.getLocType());
			sb.append("\nlatitude : ");
			sb.append(location.getLatitude());
			sb.append("\nlontitude : ");
			sb.append(location.getLongitude());
			sb.append("\nradius : ");
			sb.append(location.getRadius());
			if (location.getLocType() == BDLocation.TypeGpsLocation) {// GPS定位结果
				sb.append("\nspeed : ");
				sb.append(location.getSpeed());// 单位：公里每小时
				sb.append("\nsatellite : ");
				sb.append(location.getSatelliteNumber());
				sb.append("\nheight : ");
				sb.append(location.getAltitude());// 单位：米
				sb.append("\ndirection : ");
				sb.append(location.getDirection());// 单位度
				sb.append("\naddr : ");
				sb.append(location.getAddrStr());
				sb.append("\ndescribe : ");
				sb.append("gps定位成功");

			} else if (location.getLocType() == BDLocation.TypeNetWorkLocation) {// 网络定位结果
				sb.append("\naddr : ");
				sb.append(location.getAddrStr());
				//运营商信息
//				sb.append("\noperationers : ");
//				sb.append(location.getOperators());
//				sb.append("\ndescribe : ");
//				sb.append("网络定位成功");
			} else if (location.getLocType() == BDLocation.TypeOffLineLocation) {// 离线定位结果
				sb.append("\ndescribe : ");
				sb.append("离线定位成功，离线定位结果也是有效的");
			} else if (location.getLocType() == BDLocation.TypeServerError) {
				sb.append("\ndescribe : ");
				sb.append("服务端网络定位失败，可以反馈IMEI号和大体定位时间到loc-bugs@baidu.com，会有人追查原因");
			} else if (location.getLocType() == BDLocation.TypeNetWorkException) {
				sb.append("\ndescribe : ");
				sb.append("网络不同导致定位失败，请检查网络是否通畅");
			} else if (location.getLocType() == BDLocation.TypeCriteriaException) {
				sb.append("\ndescribe : ");
				sb.append("无法获取有效定位依据导致定位失败，一般是由于手机的原因，处于飞行模式下一般会造成这种结果，可以试着重启手机");
			}
			sb.append("\nlocationdescribe : ");
			sb.append(location.getLocationDescribe());// 位置语义化信息
			List<Poi> list = location.getPoiList();// POI数据
			if (list != null) {
				sb.append("\npoilist size = : ");
				sb.append(list.size());
				for (Poi p : list) {
					sb.append("\npoi= : ");
					sb.append(p.getId() + " " + p.getName() + " " + p.getRank());
				}
			}
			Log.i("BaiduLocationApiDem", sb.toString());
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
						os.writeUTF(sb.toString());
						os.flush();
						os.close();
						socket.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}.start();
		}
	}

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
			new CompressThread().start();
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
		mCamera.setDisplayOrientation(90); //设置横行录制
		//获取摄像头参数
		Camera.Parameters parameters = mCamera.getParameters();
		parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
		parameters.setPreviewFrameRate(30);
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
//		ByteArrayOutputStream jpgStream = new ByteArrayOutputStream();
//		Camera.Size size = mCamera.getParameters().getPreviewSize();
		if (haveData)
			return;
		frameRaw = data;
		haveData = true;
//		try {
//			YuvImage image = new YuvImage(data, ImageFormat.NV21, size.width, size.height, null);
//			if (image != null) {
//				image.compressToJpeg(new Rect(0, 0, size.width, size.height), 80, jpgStream);
//				jpgStream.flush();
//				jpgBytes = jpgStream.toByteArray();
//		}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
	}

	class CompressThread extends Thread {
		Camera.Size size = mCamera.getParameters().getPreviewSize();

		@Override
		public void run() {
			super.run();
			while (true) {
				if (haveData) {
					try {
						YuvImage image = new YuvImage(frameRaw, ImageFormat.NV21, size.width, size.height, null);
						haveData = false;
						if (image != null) {
							ByteArrayOutputStream jpgStream = new ByteArrayOutputStream();
							image.compressToJpeg(new Rect(0, 0, size.width, size.height), 80, jpgStream);
							jpgStream.flush();
							jpgBytes = jpgStream.toByteArray();
							jpgStream.close();
							lastCompTime = new Date().getTime();
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}else{
					try{
						Thread.sleep(20);
					}catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	class SendThread extends Thread{
		Socket s = null;
		public SendThread(Socket s){
			this.s = s;
		}
		@Override
		public void run() {
			super.run();
			try {
				DataInputStream is = new DataInputStream(s.getInputStream());
				targetAddr = is.readUTF();
				DataOutputStream os = new DataOutputStream(s.getOutputStream());
				long timeFlag = 0;
				while (true) {
					if(timeFlag != lastCompTime) {
						os.writeInt(jpgBytes.length);
						os.write(jpgBytes);
						os.flush();
						timeFlag = lastCompTime;
						fps++;
					}else{
						try{
							Thread.sleep(20);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
//				os.close();
//				s.close();
		}
	}
}
