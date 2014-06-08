package com.alangeorge.minermonitor;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * A fragment representing a single Miner detail screen.
 * This fragment is either contained in a {@link MinerListActivity}
 * in two-pane mode (on tablets) or a {@link MinerDetailActivity}
 * on handsets.
 */
public class MinerDetailFragment extends Fragment implements Miner.MinerListener {
    private static final String TAG = "MinerDetailFragment";
    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */
    public static final String ARG_ITEM_ID = "item_id";

    /**
     * The dummy content this fragment is presenting.
     */
    private Miner miner;

	private TextView minerView;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public MinerDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            // Load the miner content specified by the fragment
            // arguments. In a real-world scenario, use a Loader
            // to load content from a content provider.
            miner = MinerList.ITEM_MAP.get(getArguments().getString(ARG_ITEM_ID));
            
           if (miner!= null) {
        	   miner.registerListener(this);
           } else {
        	   Log.e(TAG, "miner is null");
           }
        }
    }

    @Override
	public void onActivityCreated(Bundle savedInstanceState) {
		Log.d(TAG, "onActivityCreated(" + savedInstanceState + ")");
		super.onActivityCreated(savedInstanceState);
	}

	@Override
	public void onAttach(Activity activity) {
		Log.d(TAG, "onAttach(" + activity + ")");
		super.onAttach(activity);
	}

	@Override
	public void onDestroy() {
		Log.d(TAG, "onDistroy()");
		miner.unregisterListener(this);
		super.onDestroy();
	}

	@Override
	public void onDestroyView() {
		Log.d(TAG, "onDistroyView()");
		super.onDestroyView();
	}

	@Override
	public void onDetach() {
		Log.d(TAG, "onDetach()");
		super.onDetach();
	}

	@Override
	public void onPause() {
		Log.d(TAG, "onPause()");
		super.onPause();
	}

	@Override
	public void onResume() {
		Log.d(TAG, "onResume()");
		super.onResume();
	}

	@Override
	public void onStart() {
		Log.d(TAG, "onStart()");
		super.onStart();
	}

	@Override
	public void onStop() {
		Log.d(TAG, "onStop()");
		super.onStop();
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		Log.d(TAG, "onViewCreated(" + view + ", " + savedInstanceState + ")");
		super.onViewCreated(view, savedInstanceState);
	}

	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
		Log.d(TAG, "onCreateView(" + inflater + ", " + container + ", " + savedInstanceState + ")");
        View rootView = inflater.inflate(R.layout.fragment_miner_detail, container, false);

        minerView = ((TextView) rootView.findViewById(R.id.miner_detail));

        if (miner != null) {
        	minerView.setText(miner.detailContent());
        }

        return rootView;
    }

	@Override
	public void update(Miner miner) {
		if (this.miner != miner) {
			Log.d(TAG, "from update this.miner != miner");
			return;
		}
		
        if (miner != null && minerView != null) {
        	minerView.setText(miner.detailContent());
        } else {
        	Log.e(TAG, "miner or minerView is null");
        }

	}
}
