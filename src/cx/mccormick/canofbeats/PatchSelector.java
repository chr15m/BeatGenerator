package cx.mccormick.canofbeats;

/* Based on SceneSelection code by Peter Brinkmann (thanks!) */

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.puredata.core.PdBase;
import org.puredata.core.utils.IoUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.larvalabs.svgandroid.SVG;
import com.larvalabs.svgandroid.SVGParser;

public class PatchSelector extends Activity implements OnItemClickListener {
	
	private ListView patchList;
	private final Map<String, String> patches = new HashMap<String, String>();
	Resources res = null;
	private String pdzZipPath;
	private String folderName;
	private float version;
	private float latestVersion;
	ProgressDialog progress, httpProgress;
	Handler handler;
	private String dpMainfileName;
	private static long SPLASHTIME = 2000;
	private boolean foundmainPd = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		res = getResources();
		handler=new Handler();
		// if we have a baked patch, jump straight into Pd initialisation
		if (testForBakedPatch()) {
			doSplash();
		} else {
			initGui();
		}
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		Log.d("PatchSelector", "+ onStart");
		final Intent intent = getIntent();
		if (intent != null && intent.getAction().equals(Intent.ACTION_VIEW)) {
			 Log.d("PatchSelector", "> Got intent : " + intent);
			 final Uri data = intent.getData();
			 if (data != null) {
				 Log.d("PatchSelector", "> Got data   : " + data);
				 if(data.toString().contains("http")){
					 new DownloadFileFromURL().execute(data.toString());
				 }
				 String scheme = data.getScheme();
				 if (ContentResolver.SCHEME_CONTENT.equals(scheme)) { // if the URI is of type content://
					// handle content uri to get sdcardPath
					 processContentUri(data);
						 
				 } else {
					// handle as file uri
					 pdzZipPath = data.getPath();
					 Log.d("PatchSelector", "> Open file  : " + pdzZipPath);
					 getLatestVersion(); // check the version of pdz we just clicked on
					 process();
				 }
			 }
		} else {
			Log.d("PatchSelector", "onStart = No intent");
		}
		return;
	}
	@Override
	protected Dialog onCreateDialog(int id) {
		httpProgress = new ProgressDialog(this);
	        httpProgress.setMessage("Downloading file. Please wait...");
	        httpProgress.setIndeterminate(false);
	        httpProgress.setMax(100);
	        httpProgress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
	        httpProgress.setCancelable(true);
	        httpProgress.show();
	        return httpProgress;
	}
	private void processContentUri(Uri data) {
		try 
	    {
	        InputStream attachment = getContentResolver().openInputStream(data);
	        if (attachment == null)
	            Log.e("onCreate", "cannot access mail attachment");
	        else
	        {
	            FileOutputStream tmp = new FileOutputStream(Environment.getExternalStorageDirectory().toString() + "/temp.dpz");
	            byte []buffer = new byte[1024];
	            while (attachment.read(buffer) > 0)
	                tmp.write(buffer);

	            tmp.close();
	            attachment.close();
	        }
	        pdzZipPath = Environment.getExternalStorageDirectory().toString() + "/temp.dpz";
	        getLatestVersion(); // check the version of pdz we just clicked on
	        process();
	    } 
	    catch (FileNotFoundException e) {
	        e.printStackTrace();
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
		
	}

	private void process() {
		if (!getDiskVersion()) {
			extract();
		}  else {
			String Latestversion = Float.toString(latestVersion);
			String Thisversion = Float.toString(version);
			if(Latestversion.contains(".0"));
			{
				Latestversion = Latestversion.substring(0, Latestversion.lastIndexOf("."));
			}
			if(Thisversion.contains(".0"));
			{
				Thisversion = Thisversion.substring(0, Thisversion.lastIndexOf("."));
			}
			new AlertDialog.Builder(PatchSelector.this)
			.setTitle("New .dpz File")
			.setMessage("Would you like to replace " + Thisversion
					+ " with latest version " + Latestversion
					+ " ?").setCancelable(false)
					.setPositiveButton("Okay", new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							extract();
						}
					}).setNegativeButton("Cancel", new OnClickListener() {
						@Override
						 public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
							finish();
						}
					}).show();
		} 
		
	}

	private void extract() {
		// TODO Auto-generated method stub
		try {
			List<File> listMain = IoUtils.extractZipResource(new FileInputStream(pdzZipPath), new File(Environment.getExternalStorageDirectory().toString() + "/PdDroidParty"), true);
			 if (listMain.size() != 0) {
				 for (File file : listMain) {
					 Log.d("Extracting", file.getAbsolutePath());
					
				 }
				 launchDroidParty(Environment.getExternalStorageDirectory().toString() + "/PdDroidParty/" + folderName + "/" + dpMainfileName);
			 }
		}  catch (FileNotFoundException e) {
			e.printStackTrace();
		}catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	private boolean getDiskVersion() {
		// TODO Auto-generated method stub
		List<File> listpdMain = IoUtils.find(new File(Environment.getExternalStorageDirectory().toString() + "/PdDroidParty"+ "/" + folderName), ".*droidparty_main\\.pd$");
		if (listpdMain.size() != 0) {
			 for (File f : listpdMain) {
				 Log.d("DiskFile", f.getAbsolutePath());
				 try {
					 InputStream is = new FileInputStream(f);
					 BufferedReader reader = new BufferedReader(new InputStreamReader(is));
					 String line;
					 while ((line = reader.readLine()) != null) {
						 String version;
						 if (line.contains(" version: ")) {
							 Log.d("VersionLine", line);
							 version = line.substring(line.lastIndexOf(":") + 1,line.length() - 1);
							 this.version = Float.parseFloat(version);
							 break;
						 } else {
							 version = "0";
							 this.version = Float.parseFloat(version);
						 }
						 
					 }
					 reader.close();
					 Log.d("DiskVersion", version+"");
					 Toast.makeText(PatchSelector.this, "DiskVersion"+version+"", Toast.LENGTH_SHORT).show();
				 } catch (Exception e) {
					 e.printStackTrace();
					 return false;
				 }
			 }
		}
		 return listpdMain.size() != 0;
	}

	private void getLatestVersion() {
		// TODO Auto-generated method stub
		File temp = new File(Environment.getExternalStorageDirectory().toString() + "/pdTemp");
		try{
			List<File> listMain = IoUtils.extractZipResource(new FileInputStream(pdzZipPath), temp, true);
			if (listMain.size() != 0) {
				for (File f : listMain) {
					if(f.isDirectory())
						folderName = f.getName();
					  if (f.getAbsolutePath().toLowerCase().contains("droidparty_main.pd")) {
						  foundmainPd = true;
						  dpMainfileName = f.getName();
							 InputStream is = new FileInputStream(f);
							 BufferedReader reader = new BufferedReader(new InputStreamReader(is));
							 String line;
							 while ((line = reader.readLine()) != null) {
								 String version;
								 if (line.contains(" version: ")) {
									 Log.d("LatestVersionLine", line);
									 version = line.substring(line.lastIndexOf(":") + 1, line.length() - 1);
									 this.latestVersion = Float.parseFloat(version);
									 break;
								 } else {
									 version = "0";
									 this.latestVersion = Float.parseFloat(version);
								 }
								 
							 }
							 reader.close();
							 Log.d("LatestVersion", latestVersion+"");
							 break;
						 } 
				} if(!foundmainPd){
					closePd();
					}
			}
			else{
				closePd();
			}
		} catch (Exception e){
			e.printStackTrace();
		}
	}

	private void closePd() {
		// TODO Auto-generated method stub
		Toast.makeText(PatchSelector.this, "PdDroidParty: File Format not Supported, or bad file", Toast.LENGTH_LONG).show();
		if(progress!=null){
			progress.dismiss();
			progress = null;
		}
		finish();
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View v, int position, long id) {
		TextView item = (TextView) v;
		String name = item.getText().toString();
		launchDroidParty(patches.get(name));
	}
	
	private void launchDroidParty(String path) {
		Intent intent = new Intent(this, PdDroidParty.class);
		intent.putExtra(PdDroidParty.PATCH, path);
		if(progress!=null){
			progress.dismiss();
			progress = null;
		}
		startActivity(intent);
		finish();
	}
	
	private boolean testForBakedPatch() {
		Log.d("PatchSelector", "testForBakedPatch");
		Log.d("PatchSelector", "testForBakedPatch (raw) = " + R.raw.patch);
		// return res.getIdentifier("patch", "raw", getPackageName()) != 0;
		return R.raw.patch != 0;
	}
	
	private String unpackBakedPatch() {
		Log.e("PatchSelector", "unpackBakedPatch");
		// if we have a patch zip
		if (testForBakedPatch()) {
			Log.d("PatchSelector", "Baked patch found");
			//IoUtils.extractZipResource(res.openRawResource(R.raw.abstractions), libDir, false);
			//IoUtils.extractZipResource(res.openRawResource(Properties.hasArmeabiV7a ? R.raw.externals_v7a : R.raw.externals), libDir, false);
			// where we will be storing the patch on the sdcard
			//String basedir = Environment.getExternalStorageDirectory().toString() + "/" + res.getString(res.getIdentifier("dirname", "string", getPackageName()));

			String basedir = getFilesDir().getAbsolutePath();
			// File basedir = new File(dir, "chords.pd");
			//IoUtils.extractZipResource(getResources().openRawResource(R.raw.patch), dir, true);
			//PdBase.openPatch(patchFile.getAbsolutePath());

			Log.d("PatchSelector", "Basedir: " + basedir);
			//File dest_dir = new File(Environment.getExternalStorageDirectory() + "/" + folder_main, "product1");
			//if (!f1.exists()) {
			//    f1.mkdirs();
			//}
			/*File d = new File(basedir);
			if (!d.isDirectory()) {
				Log.d("PatchSelector", "mkdirs");
				if (!d.mkdirs()) {
					Log.d("PatchSelector", "mkdirs failed!");
				}
			}*/
			// version file for the existing patch
			File version = new File(basedir + "/patch/VERSION-" + res.getInteger(res.getIdentifier("revno", "integer", getPackageName())));
			// if the version file does not exist
			if (!version.exists()) {
				try {
					Log.d("PatchSelector", "Attempting unzip");
					IoUtils.extractZipResource(getResources().openRawResource(R.raw.patch), new File(basedir), false);
					//IoUtils.extractZipResource(getResources().openRawResource(R.raw.patch), new File(Environment.getExternalStorageDirectory().toString()), false);
					// IoUtils.extractZipResource(getResources().openRawResource(res.getIdentifier("patch", "raw", getPackageName())), new File(basedir), false);
				} catch (IOException e) {
					Log.e("PatchSelector", e.toString());
					// do nothing
					return null;
				}
			}
			//PdBase.addToSearchPath(libDir.getAbsolutePath());
			PdBase.addToSearchPath(basedir + "/patch");
			// also add the recordings directory
			(new File(basedir, "recordings")).mkdirs();
			// also add the safefiles directory
			(new File(basedir, "savefiles")).mkdirs();
			// return the patch path to launch Pd
			return basedir + "/patch/droidparty_main.pd";
		} else {
			Log.d("PatchSelector", "Baked patch NOT found");
		}
		
		Log.d("PatchSelector", "AFTER");
		
		// add abstractions and externals zips
		Resources res = getResources();
		File libDir = getFilesDir();
		try {
			IoUtils.extractZipResource(res.openRawResource(R.raw.abstractions), libDir, true);
			// IoUtils.extractZipResource(res.openRawResource(Properties.hasArmeabiV7a ? R.raw.externals_v7a : R.raw.externals), libDir, true);
		} catch (IOException e) {
			Log.e("PatchSelector", e.toString());
		}
		PdBase.addToSearchPath(libDir.getAbsolutePath());
		
		return null;
	}
	
	private void initPd(final ProgressDialog progress) {
		String bakedpatch = unpackBakedPatch();
		launchDroidParty(bakedpatch);
		finish();

		/*new Thread() {
			@Override
			public void run() {
				Log.d("PatchSelector", "initPd run()");
				long start = System.currentTimeMillis();
				// see if this is an app with a zip to unpack instead
				String bakedpatch = unpackBakedPatch();
				long elapsed = System.currentTimeMillis() - start;
				if (elapsed < SPLASHTIME) {
					try {
						this.sleep(SPLASHTIME - elapsed);
					} catch (Exception e) {
					}
				}
				Log.d("PatchSelector", "bakedpatch: " + bakedpatch);
				if (bakedpatch != null) {
					launchDroidParty(bakedpatch);
					finish();
				} else {
					List<File> list = IoUtils.find(new File(Environment.getExternalStorageDirectory().toString() + "/PdDroidParty"), ".*droidparty_main\\.pd$");
					for (File f: list) {
						String[] parts = f.getParent().split("/");
						// exclude generic patch directories found in apps based on PdDroidParty
						if (!parts[parts.length - 1].equals("patch")) {
							patches.put(parts[parts.length - 1], f.getAbsolutePath());
							Log.d("AbsPath", f.getAbsolutePath());
						}
					}
					ArrayList<String> keyList = new ArrayList<String>(patches.keySet());
					Collections.sort(keyList, new Comparator<String>() {
						public int compare(String a, String b) {
							return a.toLowerCase().compareTo(b.toLowerCase());
						}
					});
					final ArrayAdapter<String> adapter = new ArrayAdapter<String>(PatchSelector.this, android.R.layout.simple_list_item_1, keyList);
					handler.post(new Runnable() {
						@Override
						public void run() {
							patchList.setAdapter(adapter);
							if (progress != null) {
								try {
									progress.dismiss();
								} catch (Exception e) {
									// nothing
								}
							}
						}
					});
				}
			};
		}.start();*/	
	}
	
	private void doSplash() {
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
	        // Create a new ImageView
		ImageView imageView = new ImageView(this);
		// Set the background color to white
		imageView.setBackgroundColor(Color.WHITE);
		int splashres = res.getIdentifier("splash", "raw", getPackageName());
		if (splashres != 0) {
			// Parse the SVG file from the resource
			SVG svg = SVGParser.getSVGFromResource(getResources(), splashres);
			// Get a drawable from the parsed SVG and set it as the drawable for the ImageView
			imageView.setImageDrawable(svg.createPictureDrawable());
			// Set the ImageView as the content view for the Activity
			setContentView(imageView);
		}
		// initialise Pd without a progress thing
		initPd(null);
	}
	
	private void initGui() {
		setContentView(R.layout.patch_selector);
		patchList = (ListView) findViewById(R.id.patch_selector);
		progress = new ProgressDialog(this);
		progress.setMessage("Finding patches...");
		progress.setCancelable(false);
		progress.setIndeterminate(true);
		progress.show();
		initPd(progress);
		patchList.setOnItemClickListener(this);
	}
	class DownloadFileFromURL extends AsyncTask<String, String, String> {
		 
	    /**
	     * Before starting background thread
	     * Show Progress Bar Dialog
	     * */
	    @Override
	    protected void onPreExecute() {
	        super.onPreExecute();
	        showDialog(0);
	    }
	 
	    /**
	     * Downloading file in background thread
	     * */
	    @Override
	    protected String doInBackground(String... f_url) {
	        int count;
	        try {
	            URL url = new URL(f_url[0]);
	            URLConnection conection = url.openConnection();
	            conection.connect();
	            // getting file length
	            int lenghtOfFile = conection.getContentLength();
	 
	            // input stream to read file - with 8k buffer
	            InputStream input = new BufferedInputStream(url.openStream(), 8192);
	 
	            // Output stream to write file
	            OutputStream output = new FileOutputStream(Environment.getExternalStorageDirectory().toString() + "/temp.dpz");
	 
	            byte data[] = new byte[1024];
	 
	            long total = 0;
	 
	            while ((count = input.read(data)) != -1) {
	                total += count;
	                // publishing the progress....
	                // After this onProgressUpdate will be called
	                publishProgress(""+(int)((total*100)/lenghtOfFile));
	 
	                // writing data to file
	                output.write(data, 0, count);
	            }
	 
	            // flushing output
	            output.flush();
	 
	            // closing streams
	            output.close();
	            input.close();
	 
	        } catch (Exception e) {
	            Log.e("Error: ", e.getMessage());
	        }
	 
	        return null;
	    }
	 
	   
	    protected void onProgressUpdate(String... progress) {
	        // setting progress percentage
	        httpProgress.setProgress(Integer.parseInt(progress[0]));
	   }
	 
	    
	    @Override
	    protected void onPostExecute(String file_url) {
	        // dismiss the dialog after the file was downloaded
	        dismissDialog(0);
	 
	     
	        pdzZipPath = Environment.getExternalStorageDirectory().toString() + "/temp.dpz";
	        getLatestVersion();
	        process();
	    }
	 
	}
}
