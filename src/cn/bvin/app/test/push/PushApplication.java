package cn.bvin.app.test.push;

import com.baidu.frontia.FrontiaApplication;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class PushApplication extends FrontiaApplication{

	private static PushApplication mApplication;
	private BaiduPush mBaiduPushServer;
	private Gson mGson;
	
	private String userId;
	private String channelId;
	
	public synchronized BaiduPush getBaiduPush() {
		if (mBaiduPushServer == null)
			mBaiduPushServer = new BaiduPush(BaiduPush.HTTP_METHOD_POST,
					BackgoundService.SECRIT_KEY, BackgoundService.APP_KEY);
		return mBaiduPushServer;

	}
	
	public synchronized Gson getGson() {
		if (mGson == null)
			// 不转换没有 @Expose 注解的字段
			mGson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation()
					.create();
		return mGson;
	}
	
	public synchronized static PushApplication getInstance() {
		return mApplication;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		mApplication = this;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getChannelId() {
		return channelId;
	}

	public void setChannelId(String channelId) {
		this.channelId = channelId;
	}
	
	
}
