package lah.downloader;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import android.annotation.TargetApi;
import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;

/**
 * Blocking file download object using the Android system downloader
 * 
 * @author L.A.H.
 * 
 */
public class Downloader {

	/**
	 * {@link BroadcastReceiver} binds to the ACTION_DOWNLOAD_COMPLETE intent raised by {@link DownloadManager}
	 * 
	 * @author L.A.H.
	 * 
	 */
	private class DownloadCompleteBroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context ctx, Intent intent) {
			// Get the ID of the completed donwload
			long download_id = intent.getExtras().getLong(DownloadManager.EXTRA_DOWNLOAD_ID);
			// Set the flag appropriately
			if (completion_status_map.containsKey(download_id))
				completion_status_map.put(download_id, true);
		}

	}

	/**
	 * Map from Download ID to {true, false} indicating if the download is completed by the {@link DownloadManager}
	 */
	private final ConcurrentMap<Long, Boolean> completion_status_map;

	/**
	 * {@link IntentFilter} to filter out download complete actions
	 */
	private final IntentFilter intent_filter;

	private final Context context;

	private final DownloadManager download_manager;

	private final DownloadCompleteBroadcastReceiver download_receiver;

	public Downloader(Context context) {
		assert context != null;
		this.context = context;
		download_manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
		download_receiver = new DownloadCompleteBroadcastReceiver();
		intent_filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
		completion_status_map = new ConcurrentHashMap<Long, Boolean>();
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
	 * @return the input {@code outputFile} representing the download result or {@literal null} if the download fails
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
		long download_id = download_manager.enqueue(request);
		context.registerReceiver(download_receiver, intent_filter);

		// Waiting until the download manager completes the request (successful|fail)
		completion_status_map.put(download_id, false);
		while (!completion_status_map.get(download_id)) {
			if (Thread.interrupted()) {
				download_manager.remove(download_id);
				completion_status_map.remove(download_id);
				throw new InterruptedException("Download is interrupted!");
			}
			Thread.yield();
		}

		// Download is completed
		completion_status_map.remove(download_id);

		// Unregister the receiver if there is no more submitted requests
		if (completion_status_map.isEmpty())
			context.unregisterReceiver(download_receiver);

		// Download result does not exist means failure
		return outputFile.exists() ? outputFile : null;
	}

}
