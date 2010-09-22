package org.connectbot;

import android.app.Activity;
import android.os.Bundle;
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

	@Override
	public void onCreate(Bundle b) {
		super.onCreate(b);
		setContentView(R.layout.act_filetransfer);

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

}
