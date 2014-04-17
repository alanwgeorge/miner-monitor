package com.alangeorge.minermonitor;

import java.text.DecimalFormat;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class MinerListAdapter extends ArrayAdapter<Miner> {
	private List<Miner> minersRef = null;
	private static LayoutInflater inflater = null;
	private DecimalFormat ghsFormatter = new DecimalFormat("###,###.000");
	private DecimalFormat bestShareFormatter = new DecimalFormat("###,###,###,###,###");

	
	public MinerListAdapter(Context context, int resource,
			List<Miner> miners) {
		super(context, resource, miners);
		inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.minersRef = miners;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = convertView;
		
		if (view == null) {
			view = inflater.inflate(R.layout.miner_list_item, null);
		}

		TextView name = (TextView) view.findViewById(R.id.minerName);
		TextView ghs = (TextView) view.findViewById(R.id.minerGHs);
		TextView bestShare = (TextView) view.findViewById(R.id.minerBestShare);
		
		Miner targetMiner = minersRef.get(position);
		
		name.setText(targetMiner.name);
		ghs.setText(ghsFormatter.format(targetMiner.mhsAvg / 1000));
		bestShare.setText(bestShareFormatter.format(targetMiner.bestShare));
		
		return view;
	}

	@Override
	public void notifyDataSetChanged() {
		super.notifyDataSetChanged();
		
		for (Miner m : minersRef) {
			m.updateListeners();
		}
	}

}
