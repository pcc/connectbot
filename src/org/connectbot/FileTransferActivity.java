package org.connectbot;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.connectbot.bean.HostBean;
import org.connectbot.service.TerminalBridge;
import org.connectbot.service.TerminalManager;
import org.connectbot.transport.FileTransferSession;
import org.connectbot.transport.FileInfo;
import org.connectbot.transport.LocalFileTransferSession;
import org.connectbot.util.HostDatabase;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TextView;

public class FileTransferActivity extends Activity {

	private ListView listLocal, listRemote;
	private TabHost tabs = null;

	protected HostDatabase hostdb;
	private HostBean host;

	private ServiceConnection connection = null;
	protected TerminalBridge hostBridge = null;

	protected LayoutInflater inflater = null;

	private short updateCount = 0;

	private interface FXSessionSource {
		FileTransferSession getSession() throws IOException;
	}

	private class FileTransferController {

		private ListView view;
		private FXSessionSource ss;
		private String currentDirectory;
		private Handler onDirectoryChange;
		private boolean updating;

		public FileTransferController(ListView view, FXSessionSource ss, Handler onDirectoryChange) {
			this.view = view;
			this.ss = ss;
			this.onDirectoryChange = onDirectoryChange;

			view.setOnItemClickListener(new OnItemClickListener() {
				public void onItemClick(AdapterView parent, View view, int position, long id) {
					String path = parent.getItemAtPosition(position).toString();
					navigate(path);
				}
			});
		}

		public void navigate(final String path) {
			synchronized (this) {
				if (updating)
					return;
				updating = true;
			}
			synchronized (FileTransferActivity.this) {
				updateCount++;
				if (updateCount == 1)
					setProgressBarIndeterminateVisibility(true);
			}

			new Thread() {
				public void run() {
					doNavigate(path);
				}
			}.start();
		}

		private void doNavigate(String path) {
			try {
				FileTransferSession fxSession = ss.getSession();
				if (fxSession == null)
					return;
				if (path != null)
					fxSession.cd(path);

				String oldDirectory = currentDirectory;
				currentDirectory = fxSession.pwd();
				onDirectoryChange.sendEmptyMessage(-1);

				try {
					FileInfo files[] = fxSession.ls();
					Arrays.sort(files);
					final ArrayList<String> fileNames = new ArrayList<String>();
					for (FileInfo file : files) {
						fileNames.add(file.name);
					}

					new Handler(Looper.getMainLooper()) {
						public void handleMessage(Message msg) {
							ArrayAdapter<String> fileNamesAdapter = new ArrayAdapter<String>(FileTransferActivity.this, android.R.layout.simple_list_item_1, fileNames);
							view.setAdapter(fileNamesAdapter);

							synchronized (FileTransferActivity.this) {
								updateCount--;
								if (updateCount == 0)
									setProgressBarIndeterminateVisibility(false);
							}
							updating = false;
						}
					}.sendEmptyMessage(-1);
				} catch (IOException e) {
					try {
						fxSession.cd(oldDirectory);
						currentDirectory = oldDirectory;
						onDirectoryChange.sendEmptyMessage(-1);
					} catch (Exception e2) {
						Log.e("connectbot", "Error encountered changing back to old directory "+oldDirectory+": "+e2.toString());
					}
					throw e;
				}
			} catch (final IOException e) {
				new Handler(Looper.getMainLooper()) {
					public void handleMessage(Message msg) {
						new AlertDialog.Builder(FileTransferActivity.this)
							.setTitle("Error")
							.setMessage(e.getMessage())
							.setPositiveButton("OK", null)
							.show();
						synchronized (FileTransferActivity.this) {
							updateCount--;
							if (updateCount == 0)
								setProgressBarIndeterminateVisibility(false);
						}
						updating = false;

					}
				}.sendEmptyMessage(-1);
			}
		}

		public String getCurrentDirectory() {
			return currentDirectory;
		}
	}

	FileTransferController localController, remoteController;

	@Override
	public void onCreate(Bundle b) {
		super.onCreate(b);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.act_filetransfer);

		long hostId = this.getIntent().getLongExtra(Intent.EXTRA_TITLE, -1);
		hostdb = new HostDatabase(this);
		host = hostdb.findHostById(hostId);

		connection = new ServiceConnection() {
			public void onServiceConnected(ComponentName className, IBinder service) {
				TerminalManager bound = ((TerminalManager.TerminalBinder) service).getService();

				hostBridge = bound.getConnectedBridge(host);
				updateHandler.sendEmptyMessage(-1);
			}

			public void onServiceDisconnected(ComponentName name) {
				hostBridge = null;
			}
		};

		tabs = (TabHost) findViewById(android.R.id.tabhost);
		tabs.setup();

		TabSpec tsLocal = tabs.newTabSpec("Local");
		tsLocal.setIndicator("Local");
		tsLocal.setContent(R.id.localview);
		tabs.addTab(tsLocal);

		TabSpec tsRemote = tabs.newTabSpec("Remote");
		tsRemote.setIndicator("Remote");
		tsRemote.setContent(R.id.remoteview);
		tabs.addTab(tsRemote);

		tabs.setOnTabChangedListener(new OnTabChangeListener() {
			public void onTabChanged(String id) {
				updateCurrentDirectory();
			}
		});

		listLocal = (ListView) findViewById(R.id.localview);
		listRemote = (ListView) findViewById(R.id.remoteview);

		localController = new FileTransferController(listLocal, new FXSessionSource() {
			FileTransferSession localSession = null;
			public FileTransferSession getSession() throws IOException {
				if (localSession == null)
					localSession = new LocalFileTransferSession(new File("/"));
				return localSession;
			}
		}, dirChangeHandler);
		localController.navigate(null);

		remoteController = new FileTransferController(listRemote, new FXSessionSource() {
			public FileTransferSession getSession() throws IOException {
				if (hostBridge == null)
					return null;
				return hostBridge.getFileTransferSession();
			}
		}, dirChangeHandler);

		inflater = LayoutInflater.from(this);
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

	protected Handler updateHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			remoteController.navigate(null);
		}
	};

	protected Handler dirChangeHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			updateCurrentDirectory();
		}
	};

	private void updateCurrentDirectory() {
		String curTabTag = tabs.getCurrentTabTag();
		String curDir;
		if (curTabTag.equals("Local")) {
			curDir = localController.getCurrentDirectory();
		} else { // Remote
			curDir = remoteController.getCurrentDirectory();
		}
		if (curDir == null)
			curDir = "???";
		setTitle(curDir);
	}

	class FileAdapter extends ArrayAdapter<FileInfo> {

		class ViewHolder {
			public ImageView icon;
			public TextView filename;
			public TextView size;
			public TextView perms;
		}

		public FileAdapter(Context context, List<FileInfo> files) {
			super(context, R.layout.item_file, files);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;

			if (convertView == null) {
				convertView = inflater.inflate(R.layout.item_file, null, false);

				holder = new ViewHolder();

				holder.icon = (ImageView)convertView.findViewById(android.R.id.icon);
				holder.filename = (TextView)convertView.findViewById(android.R.id.text1);
				holder.size = (TextView)convertView.findViewById(R.id.size);
				holder.perms = (TextView)convertView.findViewById(R.id.perms);

				convertView.setTag(holder);
			} else
				holder = (ViewHolder) convertView.getTag();

			FileInfo fi = getItem(position);

			holder.filename.setText(fi.name);
			holder.size.setText("size");
			holder.perms.setText("perms");

			return convertView;
		}
	}

}
