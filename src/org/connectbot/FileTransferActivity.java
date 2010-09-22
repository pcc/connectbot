package org.connectbot;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;

public class FileTransferActivity extends Activity {

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
	}

}
