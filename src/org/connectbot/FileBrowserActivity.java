package org.connectbot;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.connectbot.bean.HostBean;
import org.connectbot.service.FileTransferTask;
import org.connectbot.service.TerminalBridge;
import org.connectbot.service.TerminalManager;
import org.connectbot.transport.FileTransport;
import org.connectbot.transport.FileInfo;
import org.connectbot.transport.LocalFileTransport;
import org.connectbot.util.HostDatabase;

import static com.trilead.ssh2.sftp.AttribPermissions.*;

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
import android.text.format.Formatter;
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

public class FileBrowserActivity extends Activity {

	private ListView listLocal, listRemote;
	private TabHost tabs = null;

	protected HostDatabase hostdb;
	private HostBean host;

	private ServiceConnection connection = null;
	protected TerminalManager manager = null;
	protected TerminalBridge hostBridge = null;

	protected LayoutInflater inflater = null;

	private short updateCount = 0;

	private interface FileTransportSource {
		FileTransport getTransport() throws IOException;
		void close();
	}

	private class FileTransferController {

		private ListView view;
		private FileTransportSource ts;
		private String currentDirectory;
		private Handler onDirectoryChange, onFileSelected;
		private boolean updating;

		public FileTransferController(ListView view, FileTransportSource ts, Handler onDirectoryChange, Handler onFileSelected) {
			this.view = view;
			this.ts = ts;
			this.onDirectoryChange = onDirectoryChange;
			this.onFileSelected = onFileSelected;

			view.setOnItemClickListener(new OnItemClickListener() {
				public void onItemClick(AdapterView parent, View view, int position, long id) {
					FileInfo fi = (FileInfo) parent.getItemAtPosition(position);
					if (fi.isDirectory) {
						navigate(fi.name);
					} else {
						Message msg = Message.obtain(FileTransferController.this.onFileSelected, 0, fi);
						FileTransferController.this.onFileSelected.sendMessage(msg);
					}
				}
			});
		}

		public void navigate(final String path) {
			synchronized (this) {
				if (updating)
					return;
				updating = true;
			}
			synchronized (FileBrowserActivity.this) {
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
				FileTransport fileTransport = ts.getTransport();
				if (fileTransport == null)
					return;

				String oldDirectory = currentDirectory;
				String nextDirectory = currentDirectory != null ? currentDirectory : fileTransport.startingDirectory();
				if (path != null)
					nextDirectory = fileTransport.realpath(nextDirectory, path);

				currentDirectory = nextDirectory;
				onDirectoryChange.sendEmptyMessage(-1);

				try {
					FileInfo files[] = fileTransport.ls(nextDirectory);
					Arrays.sort(files);
					final ArrayList<FileInfo> fileList = new ArrayList<FileInfo>();

					for (FileInfo file : files) {
						if (file.name.equals(".")) continue;
						if (file.name.equals(".."))
							fileList.add(0, file);
						else
							fileList.add(file);
					}

					if (!fileList.get(0).name.equals("..")) {
						FileInfo parentDir = new FileInfo();
						parentDir.name = "..";
						parentDir.isDirectory = true;
						fileList.add(0, parentDir);
					}

					new Handler(Looper.getMainLooper()) {
						public void handleMessage(Message msg) {
							FileAdapter fileAdapter = new FileAdapter(FileBrowserActivity.this, fileList);
							view.clearTextFilter();
							view.setAdapter(fileAdapter);

							synchronized (FileBrowserActivity.this) {
								updateCount--;
								if (updateCount == 0)
									setProgressBarIndeterminateVisibility(false);
							}
							updating = false;
						}
					}.sendEmptyMessage(-1);
				} catch (IOException e) {
					currentDirectory = oldDirectory;
					onDirectoryChange.sendEmptyMessage(-1);
					throw e;
				}
			} catch (final IOException e) {
				new Handler(Looper.getMainLooper()) {
					public void handleMessage(Message msg) {
						new AlertDialog.Builder(FileBrowserActivity.this)
							.setTitle("Error")
							.setMessage(e.getMessage())
							.setPositiveButton("OK", null)
							.show();
						synchronized (FileBrowserActivity.this) {
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

		public void close() {
			ts.close();
		}

	}

	FileTransferController localController, remoteController;

	private void startFileUpload(String localBase, String remoteBase) {
		FileTransferTask task = new FileTransferTask(manager, hostBridge, localBase, FileTransferTask.UPLOAD);
		task.execute(localController.getCurrentDirectory() + "/" + localBase,
		             remoteController.getCurrentDirectory() + "/" + remoteBase);
	}

	private void startFileDownload(String localBase, String remoteBase) {
		FileTransferTask task = new FileTransferTask(manager, hostBridge, remoteBase, FileTransferTask.DOWNLOAD);
		task.execute(localController.getCurrentDirectory() + "/" + localBase,
		             remoteController.getCurrentDirectory() + "/" + remoteBase);
	}

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
				manager = ((TerminalManager.TerminalBinder) service).getService();

				hostBridge = manager.getConnectedBridge(host);
				updateHandler.sendEmptyMessage(-1);
			}

			public void onServiceDisconnected(ComponentName name) {
				localController.close();
				remoteController.close();

				manager = null;
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

		localController = new FileTransferController(listLocal, new FileTransportSource() {
			FileTransport localTransport = null;
			public FileTransport getTransport() throws IOException {
				if (localTransport == null)
					localTransport = new LocalFileTransport();
				return localTransport;
			}
			public void close() {
				localTransport = null;
			}
		}, dirChangeHandler, new Handler() {
			public void handleMessage(Message msg) {
				FileInfo fi = (FileInfo) msg.obj;
				startFileUpload(fi.name, fi.name);
			}
		});
		localController.navigate(null);

		remoteController = new FileTransferController(listRemote, new FileTransportSource() {
			FileTransport fileTransport = null;
			public FileTransport getTransport() throws IOException {
				if (fileTransport != null)
					return fileTransport;
				if (hostBridge == null)
					return null;
				fileTransport = hostBridge.getFileTransport();
				return fileTransport;
			}
			public void close() {
				hostBridge.releaseFileTransport(fileTransport);
				fileTransport = null;
			}
		}, dirChangeHandler, new Handler() {
			public void handleMessage(Message msg) {
				FileInfo fi = (FileInfo) msg.obj;
				startFileDownload(fi.name, fi.name);
			}
		});

		inflater = LayoutInflater.from(this);
	}

	@Override
	public void onStart() {
		super.onStart();

		bindService(new Intent(this, TerminalManager.class), connection, Context.BIND_AUTO_CREATE);

		if(hostdb == null)
			hostdb = new HostDatabase(this);
	}

	public void onRestart() {
		super.onRestart();

		if (hostBridge == null || hostBridge.isDisconnected())
			finish();
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
			if (hostBridge != null && !hostBridge.isDisconnected())
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

	private String formatPermissions(int perms) {
		char permStr[] = { '-', '-', '-', '-', '-', '-', '-', '-', '-', '-' };
		if ((perms & S_IFMT) == S_IFDIR) permStr[0] = 'd';
		if ((perms & S_IRUSR) != 0)      permStr[1] = 'r';
		if ((perms & S_IWUSR) != 0)      permStr[2] = 'w';
		if ((perms & S_IXUSR) != 0)      permStr[3] = 'x';
		if ((perms & S_IRGRP) != 0)      permStr[4] = 'r';
		if ((perms & S_IWGRP) != 0)      permStr[5] = 'w';
		if ((perms & S_IXGRP) != 0)      permStr[6] = 'x';
		if ((perms & S_IROTH) != 0)      permStr[7] = 'r';
		if ((perms & S_IWOTH) != 0)      permStr[8] = 'w';
		if ((perms & S_IXOTH) != 0)      permStr[9] = 'x';
		return new String(permStr);
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

			holder.icon.setImageResource(fi.isDirectory ? R.drawable.ic_folder : R.drawable.ic_file);
			holder.filename.setText(fi.name);
			holder.size.setText(!fi.isDirectory && fi.size != null
					? Formatter.formatFileSize(FileBrowserActivity.this, fi.size.longValue())
					: "");
			holder.perms.setText(fi.permissions != null
					? formatPermissions(fi.permissions.intValue())
					: "");

			return convertView;
		}
	}

}
