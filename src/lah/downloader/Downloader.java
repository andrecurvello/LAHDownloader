package lah.downloader;

import java.io.File;

import android.annotation.TargetApi;
import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;

/**
 * Blocking file download object using the system downloader
 * 
 * @author L.A.H.
 * 
 */
public class Downloader {

	private class DownloadBroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context ctx, Intent intent) {
			DownloadManager.Query query = new DownloadManager.Query();
			query.setFilterById(download_id);
			Cursor cursor = download_manager.query(query);
			if (cursor.moveToFirst()) {
				int columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
				int status = cursor.getInt(columnIndex);

				// set the flag download_finish appropriately
				switch (status) {
				// download is not done
				case DownloadManager.STATUS_RUNNING:
				case DownloadManager.STATUS_PAUSED:
				case DownloadManager.STATUS_PENDING:
					break;
				// download is done
				case DownloadManager.STATUS_SUCCESSFUL:
				case DownloadManager.STATUS_FAILED:
				default:
					context.unregisterReceiver(this);
					download_finish = true;
				}
			}
		}

	}

	private final IntentFilter intent_filter;

	private final Context context;

	private boolean download_finish;

	private long download_id;

	private final DownloadManager download_manager;

	private final BroadcastReceiver download_receiver;

	public Downloader(Context context) {
		assert context != null;
		this.context = context;
		download_manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
		download_receiver = new DownloadBroadcastReceiver();
		intent_filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
	}

	/**
	 * Download a file
	 * 
	 * @param uri
	 *            URI of the file
	 * @param outputFile
	 *            The expected file after the download completes
	 * @param showInNotification
	 *            Flag to indicate whether this should hide the running download in system notification area
	 * @return a {@link File} representing the download result or {@literal null} if the download fails
	 * @throws Exception
	 */
	@SuppressWarnings("deprecation")
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public File downloadFile(String uri, File outputFile, boolean showInNotification) throws Exception {
		// Return the file if it is already there
		if (outputFile == null || outputFile.exists())
			return outputFile;

		// File is not there yet, request it
		Request request = new Request(Uri.parse(uri));
		request.setTitle(outputFile.getName());
		Uri outputFileUri = Uri.fromFile(outputFile);
		request.setDestinationUri(outputFileUri);
		if (!showInNotification) {
			// disable show in the Downloads app
			request.setVisibleInDownloadsUi(false);
			// disable show download progress in the system notification
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
				request.setNotificationVisibility(Request.VISIBILITY_HIDDEN);
			else
				request.setShowRunningNotification(false);
		}
		download_id = download_manager.enqueue(request);
		context.registerReceiver(download_receiver, intent_filter);
		
		// Waiting until the download is done (successful|fail)
		download_finish = false;
		while (!download_finish) {
			Thread.yield();
		}
		
		// Download result does not exist means failure
		return outputFile.exists() ? outputFile : null;
	}

}
