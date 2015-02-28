package cn.bvin.app.test.push;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Style;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.baidu.android.pushservice.PushConstants;
import com.baidu.android.pushservice.PushManager;
/**
 * 
 * @ClassName: BackgoundService 
 * @Description: 后台服务，一旦启动知道系统关机才会onDestroy
 * @author: Bvin
 * @date: 2015年2月27日 上午11:46:58
 */
public class BackgoundService extends Service {
	
	private static final int BACK_GROUND_NOTIFICATION_ID = 1610;
	
	public static final String APP_KEY = "6VcQVy58uM1v2GQA0YBsupl7";
	public static final String SECRIT_KEY = "luFEGyoNPPWAfK5BRlM2MNsU5z0aT5Ib";
	
	private boolean isBaiduPushStarted;
	private String userId;
	private String channelId;
	private String userNumber;
	
	BroadcastReceiver commReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.hasExtra("onBind")) {
				Bundle bindData = intent.getBundleExtra("onBind");
				int errorCode =  bindData.getInt("errorCode");
				isBaiduPushStarted = errorCode==0;
				if (isBaiduPushStarted) {
					String userId = bindData.getString("userId");
					String channelId = bindData.getString("channelId");
					issueNotificationWithBind(context, userId, channelId);
				}else {
					issueNotificationWithBindFaild(errorCode);
				}
				
			}
		}
		
	};
	
	BroadcastReceiver networkReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			boolean noNetworkAvailable = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
			Log.e("网络变化", noNetworkAvailable?"没有网络":"有网");
			if (noNetworkAvailable) {//离线
				issueNotificationWithNoConnective();
			}else if (!isBaiduPushStarted) {//尚未启动推动并且有网了
				launchBaiduPushService();//再绑定一次
			}else {
				//不知道断网后在联网，要不要再重新startWork。。。
			}
		}
		
	};
	
	/**离现时通知*/
	private void issueNotificationWithNoConnective() {
		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
		.setTicker("推送服务离线")
		.setSmallIcon(R.drawable.ic_launcher)
		.setContentTitle("后台服务")
		.setContentText(userNumber+"(离线)");
		mBuilder.setStyle(newInboxStyle(userId, channelId));
		Intent intent = new Intent(this, BackgoundService.class);
		intent.putExtra("rebound", true);
		mBuilder.addAction(0, "重绑", PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT));
		NotificationManager mNotifyMgr =  
		        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotifyMgr.notify(BACK_GROUND_NOTIFICATION_ID, mBuilder.build(
				));
	}
	
	/**调用startWork后回调onBind()方法后，绑定失败发送的通知*/
	private void issueNotificationWithBindFaild(int errorCode) {
		//icon、title、text三要素不可缺少？
		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
		.setTicker("推送服务绑定失败")
		.setSmallIcon(R.drawable.ic_launcher)
		.setContentTitle("后台服务")
		.setContentText("错误码"+errorCode);
		Intent intent = new Intent(this, BackgoundService.class);
		intent.putExtra("rebound", true);
		mBuilder.addAction(0, "重绑", PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT));
		NotificationManager mNotifyMgr =  
		        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotifyMgr.notify(BACK_GROUND_NOTIFICATION_ID, mBuilder.build(
				));
	}
	
	/**调用startWork后回调onBind()方法后，绑定成功发送的通知*/
	private void issueNotificationWithBind(Context context,String userId,String channelId) {
		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context);
		mBuilder.setDefaults(NotificationCompat.DEFAULT_ALL);
		mBuilder.setContentTitle("后台服务");
		String userNumber = "No."+userId.substring(userId.length()-4);
		this.userId = userId;
		this.userNumber = userNumber;
		this.channelId = channelId;
		mBuilder.setContentText(userNumber);
		mBuilder.setTicker("后台服务已绑定");
		mBuilder.setAutoCancel(false);
		mBuilder.setSmallIcon(R.drawable.ic_launcher);
		mBuilder.setOngoing(true);
		mBuilder.setStyle(newInboxStyle(userId, channelId));
		mBuilder.addAction(0, "打开", null);
		mBuilder.addAction(0, "关闭", null);
		mBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.huli));
		NotificationManager mNotifyMgr =  
		        (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		mNotifyMgr.notify(BACK_GROUND_NOTIFICATION_ID, mBuilder.build(
				));
	}
	
	Style newInboxStyle(String userId,String channelId) {
		
        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
        inboxStyle.setBigContentTitle("后台服务");
        inboxStyle.setSummaryText("推送服务已绑定设备");
        inboxStyle.addLine("userId: " + userId);
        inboxStyle.addLine("channelId: " + channelId);
        return inboxStyle;
    }
	
	public static void luanch(Context context) {
		Intent service = new Intent(context, BackgoundService.class);
		context.startService(service);
	}
	
	public BackgoundService() {}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		Log.e("BackgoundService", "onCreate");
		registerMessageCommReceiver();
		registerNetworkReceiver();
		launchBaiduPushService();
		Log.e("PushManager", "startWork");
		notif(getApplicationContext(), "启动服务","推送后台正在绑定设备...");
	}
	
	private void registerMessageCommReceiver() {
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(MessageReceiver.ACTION_COMMUNICATION);
		LocalBroadcastManager.getInstance(this).registerReceiver(commReceiver, intentFilter);
	}

	private void registerNetworkReceiver() {
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
		registerReceiver(networkReceiver, intentFilter);
	}
	
	private void launchBaiduPushService() {
		/**绑定只执行一次，只有后台服务被杀掉才会重新绑定*/
		PushManager.startWork(getApplicationContext(), PushConstants.LOGIN_TYPE_API_KEY, APP_KEY);
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		//notif(getApplicationContext(), "开始运行","后台服务正在运行");
		Log.e("BackgoundService", "onStartCommand"+startId);
		if(intent!=null&&intent.hasExtra("rebound")) {
			if (intent.getBooleanExtra("rebound", false)) {
				launchBaiduPushService();//重新绑定
			}
		}
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.e("BackgoundService", "onDestroy");
		LocalBroadcastManager.getInstance(this).unregisterReceiver(commReceiver);
		unregisterReceiver(networkReceiver);
		notif(getApplicationContext(), "后台服务结束","后台服务已停止");
	}
	
	private void notif(Context context,String ticker,String text) {
		notif(context, ticker, text, null, null);
	}
	
	private void notif(Context context,String ticker,String text,String userId,String channelId) {
		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context);
		mBuilder.setDefaults(NotificationCompat.DEFAULT_ALL);
		mBuilder.setAutoCancel(false);
		mBuilder.setOngoing(true);
		mBuilder.setTicker(ticker);
		mBuilder.setSmallIcon(R.drawable.ic_launcher);
		mBuilder.setContentTitle("后台服务");
		mBuilder.setContentText(text);
		NotificationManager mNotifyMgr =  
		        (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		// Builds the notification and issues it.
		mNotifyMgr.notify(BACK_GROUND_NOTIFICATION_ID, mBuilder.build(
				));                                                                     
		//Toast.makeText(arg0, msg.getMessage(), Toast.LENGTH_SHORT).show();
	}
}
