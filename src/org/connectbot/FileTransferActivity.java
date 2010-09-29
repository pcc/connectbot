package org.connectbot;

import org.connectbot.bean.HostBean;
import org.connectbot.service.TerminalBridge;
import org.connectbot.service.TerminalManager;
import org.connectbot.util.HostDatabase;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;

public class FileTransferActivity extends Activity {

	private ListView listLocal, listRemote;

	private static String alphabet[] = {
		"A",
		"B",
		"C",
		"D",
		"E",
		"F",
		"G",
		"H",
		"I",
		"J",
		"K",
		"L",
		"M",
		"N",
		"O",
		"P",
		"Q",
		"R",
		"S",
		"T",
		"U",
		"V",
		"W",
		"X",
		"Y",
		"Z"
	};

	protected HostDatabase hostdb;
	private HostBean host;

	private ServiceConnection connection = null;
	protected TerminalBridge hostBridge = null;

	@Override
	public void onCreate(Bundle b) {
		super.onCreate(b);
		setContentView(R.layout.act_filetransfer);

		long hostId = this.getIntent().getLongExtra(Intent.EXTRA_TITLE, -1);
		hostdb = new HostDatabase(this);
		host = hostdb.findHostById(hostId);

		connection = new ServiceConnection() {
			public void onServiceConnected(ComponentName className, IBinder service) {
				TerminalManager bound = ((TerminalManager.TerminalBinder) service).getService();

				hostBridge = bound.getConnectedBridge(host);
	//			updateHandler.sendEmptyMessage(-1);
			}

			public void onServiceDisconnected(ComponentName name) {
				hostBridge = null;
			}
		};

		TabHost tabs = (TabHost) findViewById(android.R.id.tabhost);
		tabs.setup();

		TabSpec tsLocal = tabs.newTabSpec("Local");
		tsLocal.setIndicator("Local");
		tsLocal.setContent(R.id.localview);
		tabs.addTab(tsLocal);

		TabSpec tsRemote = tabs.newTabSpec("Remote");
		tsRemote.setIndicator("Remote");
		tsRemote.setContent(R.id.remoteview);
		tabs.addTab(tsRemote);

		listLocal = (ListView) findViewById(R.id.localview);
		listRemote = (ListView) findViewById(R.id.remoteview);

		ArrayAdapter<String> alphabetAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, alphabet);
		listLocal.setAdapter(alphabetAdapter);
		listRemote.setAdapter(alphabetAdapter);
	}

	@Override
	public void onStart() {
		super.onStart();

		bindService(new Intent(this, TerminalManager.class), connection, Context.BIND_AUTO_CREATE);

		if(hostdb == null)
			hostdb = new HostDatabase(this);
	}

	@Override
	public void onStop() {
		super.onStop();

		unbindService(connection);

		if(hostdb != null) {
			hostdb.close();
			hostdb = null;
		}
	}

}
