package com.alangeorge.minermonitor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.annotation.TargetApi;
import android.app.Service;
import android.content.ComponentCallbacks2;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

public class MinerUpdateService extends Service {
	public static final int MSG_ADD_CLIENT = 0;
	public static final int MSG_REMOVE_CLIENT = 1;
	public static final int MSG_MINERS_UPDATED = 2;
	public static final int MSG_SERVICE_STOPPING = 3;
	public static final long UPDATE_INTERVAL = 1000L;

	private static boolean isRunning = false;

	private ScheduledThreadPoolExecutor threadPool = new ScheduledThreadPoolExecutor(1, new ServiceThreadFactory());
	private ArrayList<Messenger> clients = new ArrayList<Messenger>();
	private Messenger serviceMessenger = new Messenger(new InboundMessageHandler());

	public static boolean isRunning() {
		return isRunning;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		Log.i(this.getClass().getSimpleName(), "Service Created.");
	}

	private void startTimer() {
		if (threadPool == null || threadPool.isShutdown() || threadPool.isTerminated()) {
			Log.d(this.getClass().getSimpleName(), "thread pool not active, starting new one");
			threadPool = new ScheduledThreadPoolExecutor(1, new ServiceThreadFactory());
		}
		
		if (! isRunning) {
			Log.d(this.getClass().getSimpleName(), "isRuning false, scheduling timer thread");
			threadPool.scheduleAtFixedRate(new Thread(){ public void run() {onTimerTick();}}, UPDATE_INTERVAL, UPDATE_INTERVAL, TimeUnit.MILLISECONDS);
			isRunning = true;
		} else {
			Log.d(this.getClass().getSimpleName(), "service thread already scheduled");
		}

	}
	
	protected void onTimerTick() {
		Log.d(this.getClass().getSimpleName(), "Updating miners");
		for (Miner m : MinerList.ITEMS) {
			try {
				m.update();
			} catch (IOException e) {
				m.setException(e);
			} catch (Exception e) {
				m.setException(e);
			}
		}
		
		sendUpdateMessage();
	}

	@Override
    public IBinder onBind(Intent intent) {
        return serviceMessenger.getBinder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(this.getClass().getSimpleName(), "Received start id " + startId + ": " + intent);
        startTimer();
        return START_STICKY; // run until explicitly stopped.
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        threadPool.shutdown();
        Log.d(this.getClass().getSimpleName(), "Service Stopped.");
        isRunning = false;
    }
    
    private void sendUpdateMessage() {
    	for (Messenger msngr : clients) {
    		Message msg = Message.obtain();
    		msg.what = MSG_MINERS_UPDATED;
    		msg.replyTo = serviceMessenger;
    		
    		try {
				msngr.send(msg);
			} catch (RemoteException e) {
				// client not responding, remove
				clients.remove(msngr);
			}
    	}
    }
    
    private void sendServiceStopingMessage() {
    	for (Messenger msngr : clients) {
    		Message msg = Message.obtain();
    		msg.what = MSG_SERVICE_STOPPING;
    		msg.replyTo = serviceMessenger;
    		
    		try {
				msngr.send(msg);
			} catch (RemoteException e) {
				// client not responding, remove
				clients.remove(msngr);
			}
    	}
    }
    
    private class InboundMessageHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MSG_ADD_CLIENT:
                clients.add(msg.replyTo);
                break;
            case MSG_REMOVE_CLIENT:
                clients.remove(msg.replyTo);
                break;
            default:
                super.handleMessage(msg);
            }
        }
    }

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		Log.d(this.getClass().getSimpleName(), "onConfigurationChanged(" + newConfig + ")");
		super.onConfigurationChanged(newConfig);
	}

	@Override
	public void onLowMemory() {
		Log.d(this.getClass().getSimpleName(), "onLowMemory()");
		super.onLowMemory();
	}

	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	@Override
	public void onTrimMemory(int level) {
		Log.d(this.getClass().getSimpleName(), "onTrimMemory(" + level + ")");
		
		switch (level) {
		case ComponentCallbacks2.TRIM_MEMORY_BACKGROUND:
			Log.i(this.getClass().getSimpleName(), "got TRIM_MEMORY_BACKGROUND");
			break;
		case ComponentCallbacks2.TRIM_MEMORY_COMPLETE:
			Log.i(this.getClass().getSimpleName(), "got TRIM_MEMORY_BACKGROUND");
			break;
		case ComponentCallbacks2.TRIM_MEMORY_MODERATE:
			Log.i(this.getClass().getSimpleName(), "got TRIM_MEMORY_BACKGROUND");
			break;
		case ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL:
			Log.i(this.getClass().getSimpleName(), "got TRIM_MEMORY_RUNNING_CRITICAL, shuting service down");
			sendServiceStopingMessage();
			threadPool.shutdownNow();
			isRunning = false;
			break;
		case ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW:
			Log.i(this.getClass().getSimpleName(), "got TRIM_MEMORY_BACKGROUND");
			break;
		case ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE:
			Log.i(this.getClass().getSimpleName(), "got TRIM_MEMORY_BACKGROUND");
			break;
		case ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN:
			Log.i(this.getClass().getSimpleName(), "got TRIM_MEMORY_BACKGROUND");
			break;
		default:
			Log.i(this.getClass().getSimpleName(), "got unknow memory trim level");
		}

		super.onTrimMemory(level);
	}

	@Override
	public boolean onUnbind(Intent intent) {
		Log.d(this.getClass().getSimpleName(), "onUnbind(" + intent + ")");
		return super.onUnbind(intent);
	}

	@Override
	public void onRebind(Intent intent) {
		Log.d(this.getClass().getSimpleName(), "onRebind(" + intent + ")");
		super.onRebind(intent);
	}
	
	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	@Override
	public void onTaskRemoved(Intent rootIntent) {
		Log.d(this.getClass().getSimpleName(), "onTaskRemived(" + rootIntent + ")");
		super.onTaskRemoved(rootIntent);
	}

	@Override
	protected void finalize() throws Throwable {
		Log.d(this.getClass().getSimpleName(), "finalize()");
		super.finalize();
	}
}
