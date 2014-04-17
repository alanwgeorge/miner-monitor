package com.alangeorge.minermonitor;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

public class AddMinerDialogFragment extends DialogFragment {
	
	private String minerName;
	private String ip;
	private String port;
	private View view;
	
	public String getMinerName() {
		return minerName;
	}

	public String getIp() {
		return ip;
	}

	public String getPort() {
		return port;
	}

	/* The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks.
     * Each method passes the DialogFragment in case the host needs to query it. */
    public interface Callbacks {
        public void onAddMinerPositiveClick(AddMinerDialogFragment dialog);
        public void onAddMinerNegativeClick(AddMinerDialogFragment dialog);
    }
	
    // Use this instance of the interface to deliver action events
    Callbacks mListener;
    
    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (Callbacks) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement NoticeDialogListener");
        }
    }
    
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        
        LayoutInflater inflater = getActivity().getLayoutInflater();
        this.view = inflater.inflate(R.layout.fragment_add_miner, null);
        
        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(view);
        builder.setMessage(R.string.add_miner_dialog_title)
               .setPositiveButton(R.string.button_label_continue, new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                	   EditText nameField = (EditText) view.findViewById(R.id.minerNameEditText);
                	   EditText ipField = (EditText) view.findViewById(R.id.hostEditText);
                	   EditText portField = (EditText) view.findViewById(R.id.portEditText);
                	   
                	   minerName = nameField.getText().toString();
                	   ip = ipField.getText().toString();
                	   port = portField.getText().toString();
                	   
                	   mListener.onAddMinerPositiveClick(AddMinerDialogFragment.this);
                   }
               })
               .setNegativeButton(R.string.button_label_cancel, new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                	   mListener.onAddMinerNegativeClick(AddMinerDialogFragment.this);
                	   dialog.cancel();
                   }
               });
        // Create the AlertDialog object and return it
        return builder.create();
    }
}
