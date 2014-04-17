package com.alangeorge.minermonitor;

import java.net.InetAddress;
import java.net.UnknownHostException;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;


/**
 * An activity representing a list of Miners. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link MinerDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 * <p>
 * The activity makes heavy use of fragments. The list of items is a
 * {@link MinerListFragment} and the item details
 * (if present) is a {@link MinerDetailFragment}.
 * <p>
 * This activity also implements the required
 * {@link MinerListFragment.Callbacks} interface
 * to listen for item selections.
 */
public class MinerListActivity extends FragmentActivity
        implements MinerListFragment.Callbacks, AddMinerDialogFragment.Callbacks {

    private static final String TAG = "MinerListActivity";
    private boolean mTwoPane; // Whether or not the activity is in two-pane mode, i.e. running on a tablet device.
    private boolean isBoundToMinerUpdateService = false;
    private MinerListFragment minerListFragment;
    private MinerListAdapter minerListAdapter;
    private View selectedMinerView = null;
    private Messenger minerUpdateServiceMesssenger = null;
    private final Messenger activityMessager = new Messenger(new MinerUpdateServiceIncomingMessageHandler());
	
    /**
     *  the service connection with the MinerUpdateService
     */
	private ServiceConnection serviceConnection = new ServiceConnection() {
        /**
         * @see android.content.ServiceConnection#onServiceConnected(android.content.ComponentName, android.os.IBinder)
         * 
         * Here we send service a message to add us as a client.  Service will send an "updated" message when miners 
         * have been refreshed
         */
        public void onServiceConnected(ComponentName className, IBinder service) {
            minerUpdateServiceMesssenger = new Messenger(service);
            try {
                Message msg = Message.obtain(null, MinerUpdateService.MSG_ADD_CLIENT);
                msg.replyTo = activityMessager;
                minerUpdateServiceMesssenger.send(msg);
            } catch (RemoteException e) {
                // In this case the service has crashed before we could even do anything with it
            }
        }

        /**
         * @see android.content.ServiceConnection#onServiceDisconnected(android.content.ComponentName)
         */
        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been unexpectedly disconnected.
            minerUpdateServiceMesssenger = null;
        }
    };
	private int notificationCount;
    

	/**
	 * @param savedInstanceState
	 */
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_miner_list);

        minerListFragment = (MinerListFragment) getSupportFragmentManager().findFragmentById(R.id.miner_list);
        minerListAdapter = (MinerListAdapter) minerListFragment.getListAdapter();
        selectedMinerView = findViewById(R.id.miner_detail_container);
        
        if (selectedMinerView != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-large and
            // res/values-sw600dp). If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;

            // In two-pane mode, list items should be given the
            // 'activated' state when touched.
            ((MinerListFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.miner_list))
                    .setActivateOnItemClick(true);
        }
        
        doBindMinerUpdateService();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            doUnbindMinerUpdateService();
        } catch (Throwable t) {
            Log.e(TAG, "Failed to unbind from the service", t);
        }
    }
    
    
    @Override
	protected void onNewIntent(Intent intent) {
    	// notify adapter, specifically the detail activity (small screen)
    	// can delete a miner 
    	minerListAdapter.notifyDataSetChanged();
		super.onNewIntent(intent);
	}

	/**
     * Callback method from {@link MinerListFragment.Callbacks}
     * indicating that the miner with the given ID was selected.
     */
    @Override
    public void onItemSelected(String id) {
        if (mTwoPane) {
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            Bundle arguments = new Bundle();
            arguments.putString(MinerDetailFragment.ARG_ITEM_ID, id);
            MinerDetailFragment fragment = new MinerDetailFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.miner_detail_container, fragment)
                    .commit();

        } else {
            // In single-pane mode, simply start the detail activity
            // for the selected item ID.
            Intent detailIntent = new Intent(this, MinerDetailActivity.class);
            detailIntent.putExtra(MinerDetailFragment.ARG_ITEM_ID, id);
            startActivity(detailIntent);
        }
    }

	/**
	 * @param arg0
	 * @param menu
	 * @return
	 */
	@Override
	public boolean onCreatePanelMenu(int arg0, Menu menu) {
		   MenuInflater inflater = getMenuInflater();
		   inflater.inflate(R.menu.list_actionbar_menu, menu);

		   return true;
	}

	/**
	 * Tablet size has two menu items for add and delete of miner.  Smaller size has only add. 
	 * Delete for small size is on the detail activity act bar menu.
	 * 
	 * @param featureId
	 * @param view
	 * @param menu
	 * @return
	 */
	@Override
	public boolean onPreparePanel(int featureId, View view, Menu menu) {
		
		int selectedPosition = minerListFragment.getListView().getCheckedItemPosition();
		Log.d(this.getLocalClassName(), "selectedPosition = " + selectedPosition);

		MenuItem deleteItem = menu.findItem(R.id.deleteminer);

		if (deleteItem != null) {// deleteItem will be null on < sw600dp
			if (minerListFragment.getListView().getCheckedItemPosition() < 0) {
				deleteItem.setEnabled(false);
			} else {
				deleteItem.setEnabled(true);
			}
		}

		return super.onPreparePanel(featureId, view, menu);
	}

	/**
	 * Tablet size has two menu items for add and delete of miner.  Smaller size has only add. 
	 * Delete for small size is on the detail activity act bar menu.
	 * 
	 * @param featureId
	 * @param item
	 * @return
	 */
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		   switch (item.getItemId()) {
	        case R.id.addminer:
	        	AddMinerDialogFragment addMinerFreg = new AddMinerDialogFragment();
	        	addMinerFreg.show(getSupportFragmentManager(), "AddMinerDialogFragment");

	        	return true;
	        case R.id.deleteminer:
	        	minerListAdapter.remove(minerListAdapter.getItem(minerListFragment.getListView().getCheckedItemPosition()));
	        	if (mTwoPane) {
	        		getSupportFragmentManager().beginTransaction().remove(getSupportFragmentManager().findFragmentById(R.id.miner_detail_container)).commit();
	        		minerListFragment.getListView().clearChoices();
	        		minerListFragment.getListView().requestLayout();
	        	}
	            return true;
	        case R.id.testInboxPN:
	        	sendTestInboxNotification();
	        case R.id.testCustomLayoutPN:
	        	sendTestCustomLayoutNotification();
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}

	/**
	 * This is the callback from the add miner dialog.  It starts an AsyncTask to contact the miner and
	 * fill in it's data
	 * 
	 * @param dialog
	 */
	@Override
	public void onAddMinerPositiveClick(AddMinerDialogFragment dialog) {
		MinerMakerTask minerMaker = new MinerMakerTask(this);
		minerMaker.execute(dialog.getMinerName(), dialog.getIp(), dialog.getPort());
	}

	/**
	 * This is the callback for the cancel on the add miner dialog.
	 * Currently not doing any thing with "cancel" 
	 * 
	 * @param dialog
	 */
	@Override
	public void onAddMinerNegativeClick(AddMinerDialogFragment dialog) {
		// do nothing
	}
	
	private void doBindMinerUpdateService() {
	       // Bind to the miner update service.  It's responsible for polling miner status and details
        isBoundToMinerUpdateService = bindService(new Intent(this, MinerUpdateService.class), serviceConnection, Context.BIND_AUTO_CREATE);
        if (isBoundToMinerUpdateService) {
        	if (startService(new Intent(this, MinerUpdateService.class)) == null) {
            	Log.e(TAG, "unable to start to MinerUpdateService");
        	}
        } else {
        	Log.e(TAG, "unable to bind to MinerUpdateService");
        }
	}

    /**
     * Send "remove" me message and then unbind from service
     */
    private void doUnbindMinerUpdateService() {
        if (isBoundToMinerUpdateService) {
            // If we are bound to the service, here we remove ourselves.
            if (minerUpdateServiceMesssenger != null) {
                try {
                    Message msg = Message.obtain(null, MinerUpdateService.MSG_REMOVE_CLIENT);
                    msg.replyTo = activityMessager;
                    minerUpdateServiceMesssenger.send(msg);
                } catch (RemoteException e) {
                	Log.e(TAG, "Unable to remove self from MinerUpdateService", e);
                    // Service unreachable
                }
            }
            // Detach our existing connection.
            unbindService(serviceConnection);
            isBoundToMinerUpdateService = false;
        }
    }

    private boolean isMinerUpdateServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (MinerUpdateService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
    
    private void sendTestInboxNotification() {
    	// come back to this activity on notification tap
    	Intent intent = new Intent(this, MinerListActivity.class);
    	intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    	PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

    	Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
    	
    	NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
    	builder.setSmallIcon(R.drawable.ic_launcher)
    	.setContentTitle("ContentTitle")
    	.setContentText("ContentText")
    	.setTicker("Ticker")
    	.setWhen(System.currentTimeMillis())
    	.setAutoCancel(true)
    	.setSound(soundUri)
    	.setContentIntent(pendingIntent);
    	
    	NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
    	inboxStyle.addLine("Line 1")
    	.addLine("Line 2")
    	.addLine("Line 3")
    	.setBigContentTitle("BigContentTitle")
    	.setSummaryText("+3 more");
    	
    	builder.setStyle(inboxStyle);
    	
    	NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    	
    	notificationManager.notify(notificationCount++, builder.build());
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	private void sendTestCustomLayoutNotification() {
    	// come back to this activity on notification tap
    	Intent intent = new Intent(this, MinerListActivity.class);
    	intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    	PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

    	Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
    	
    	RemoteViews customLayout = new RemoteViews(getPackageName(), R.layout.notifcation_layout2);
    	customLayout.setTextViewText(R.id.textViewMessage, "Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum. Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.");
    	
    	NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
    	builder.setSmallIcon(R.drawable.ic_launcher)
    	.setContent(customLayout)
    	.setTicker("Ticker")
    	.setWhen(System.currentTimeMillis())
    	.setAutoCancel(true)
    	.setSound(soundUri)
    	.setContentIntent(pendingIntent);

    	Notification notification = builder.build();
    	notification.bigContentView = customLayout;
    	
    	NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    	
    	notificationManager.notify(notificationCount++, notification);
    }

    /**
     * Handles messages from MinerUpdateService.
     * 
     * Messages handled:
     * MSG_MINERS_UNDATED, call from the service once the miner updates have completed. ListAdapter is notify of data change.
     * MSG_SERVICE_STOPPING, service was requested to trim memory so has stop it's miner update service.
     * 
     */
	private class MinerUpdateServiceIncomingMessageHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MinerUpdateService.MSG_MINERS_UPDATED:
            	// Miners have been updated
            	minerListAdapter.notifyDataSetChanged();
                break;
            case MinerUpdateService.MSG_SERVICE_STOPPING:
            	// Miner Update Service got a TrimMemory message and is shutting down
            	Log.d(TAG, "isMinerServiceRuning(): " + isMinerUpdateServiceRunning());
            	doUnbindMinerUpdateService();
                break;
            default:
                super.handleMessage(msg);
            }
        }
    }
	
	/**
	 * This creates new miners, called from Add Miner dialog
	 * This handles network IO on separate thread from UI thread
	 */
    private class MinerMakerTask extends AsyncTask<String, Integer, Miner> {
    	private Context context;
    	private String error = null;
    	
    	public MinerMakerTask(Context context) {
    		this.context = context;
    	}

		@Override
		protected Miner doInBackground(String... params) {
			String name = params[0];
			InetAddress ip = null;
			int port = 0;
			Miner miner = null;
			
			try {
				if (name == null || name.isEmpty()) {
					error = "Miner Name is empty";
					throw new Exception("Miner Name is empty");
				}

				if (params[1] != null) {
					try {
						ip = InetAddress.getByName(params[1]); // validate ip/hostname
					} catch (UnknownHostException e) {
						error = "Invalid host or IP address";
						throw new Exception(error, e);
					}
				}

				try {
					port = Integer.valueOf(params[2]);	
				} catch (NumberFormatException nfe) {
					error = "Port value is not a number";
					throw new Exception(error, nfe);
				}
			
				miner = new Miner(name, ip, port);

				try {
					miner.update(); // this retrieves data from the miner
				} catch (Exception e) {
					error = "Unable to talk to miner: " + e.getMessage();
					throw new Exception(error, e);
				}
			
			} catch (Exception e) {
				Log.e(MinerMakerTask.class.getSimpleName(), error, e);
				return null;
			}

			return miner;
		}
		
		/**
		 * Back on the UI thread, we add our new miner to our ListAdapter and set it as the selected item
		 * On error we display a Toast with error message
		 * (non-Javadoc)
		 * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
		 */
		protected void onPostExecute(Miner miner) {
			if (error != null) {
				Toast toast = Toast.makeText(context, error, Toast.LENGTH_LONG);
				toast.show(); 
			} else if (miner != null) {
				MinerList.addMiner(miner);
				MinerListActivity.this.minerListAdapter.notifyDataSetChanged();
				// set new miner as selected position
				int position = MinerListActivity.this.minerListAdapter.getPosition(miner);
	        	MinerListActivity.this.minerListFragment.getListView().setItemChecked(position, true);
	        	MinerListActivity.this.onItemSelected(miner.name);
			}
		}
    }

    // Below are add for debugging Activity lifecycle issues.
	@Override
	public void onAttachFragment(Fragment fragment) {
		Log.d(TAG, "onAttachFragment(" + fragment + ")");
		super.onAttachFragment(fragment);
	}

	@Override
	public void onBackPressed() {
		Log.d(TAG, "onBackPressed()");
		super.onBackPressed();
	}

	@Override
	public void onLowMemory() {
		Log.d(TAG, "onLowMemory()");
    	Log.d(TAG, "isMinerServiceRuning(): " + isMinerUpdateServiceRunning());
		super.onLowMemory();
	}

	@Override
	protected void onPause() {
		Log.d(TAG, "onLowMemory()");
    	Log.d(TAG, "isMinerServiceRuning(): " + isMinerUpdateServiceRunning());
		super.onPause();
	}

	@Override
	protected void onPostResume() {
		Log.d(TAG, "onPostResume()");
    	Log.d(TAG, "isMinerServiceRuning(): " + isMinerUpdateServiceRunning());
		super.onPostResume();
	}

	@Override
	protected void onResume() {
		Log.d(TAG, "onResume()");
    	Log.d(TAG, "isMinerServiceRuning(): " + isMinerUpdateServiceRunning());
    	doBindMinerUpdateService();
		super.onResume();
	}

	@Override
	protected void onResumeFragments() {
		Log.d(TAG, "onResumeFragments()");
		super.onResumeFragments();
	}

	@Override
	protected void onStart() {
		Log.d(TAG, "onStart()");
		super.onStart();
	}

	@Override
	protected void onStop() {
		Log.d(TAG, "onStop()");
    	Log.d(TAG, "isMinerServiceRuning(): " + isMinerUpdateServiceRunning());
		super.onStop();
	}
}
