package com.example.appdropboxbq;

import android.os.Bundle;
import android.app.Activity;
import android.content.SharedPreferences;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;

import android.content.SharedPreferences.Editor;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.exception.DropboxIOException;
import com.dropbox.client2.exception.DropboxParseException;
import com.dropbox.client2.exception.DropboxPartialFileException;
import com.dropbox.client2.exception.DropboxServerException;
import com.dropbox.client2.exception.DropboxUnlinkedException;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;

import com.example.appdropboxbq.Book;


public class MainActivity extends Activity {

	private static final String TAG = "AppDropBox > MainActivity";
	
	final static private String APP_KEY = "xl3dcwl0thmvud2";
    final static private String APP_SECRET = "niakhqlaxcvzhem";

    final static private String ACCOUNT_PREFS_NAME = "prefs";
    final static private String ACCESS_KEY_NAME = "ACCESS_KEY";
    final static private String ACCESS_SECRET_NAME = "ACCESS_SECRET";

    private static final boolean USE_OAUTH1 = false;

    DropboxAPI<AndroidAuthSession> mApi;

    private boolean isLogged;
    private String error_msg = "";
    
    ArrayList<Book> bookList;
    ArrayList<String> bookNameSortedList;
    private ListView lvBooks;
    private Button downloadBooks_button;
    private Button closeApp_button;
  
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
       
        AndroidAuthSession session = buildSession();
        mApi = new DropboxAPI<AndroidAuthSession>(session);

        setContentView(R.layout.activity_main);
        
        /*
        if (isLogged) {
            logOut();
        } else {
            // Start the remote authentication
            if (USE_OAUTH1) {
                mApi.getSession().startAuthentication(MainActivity.this);
            } else {
                mApi.getSession().startOAuth2Authentication(MainActivity.this);
            }
        }
        */
        if (isLogged) {
            logOut();
        }
        // Start the remote authentication
        if (USE_OAUTH1) {
            mApi.getSession().startAuthentication(MainActivity.this);
        } else {
            mApi.getSession().startOAuth2Authentication(MainActivity.this);
        }
        
        
        downloadBooks_button = (Button)findViewById(R.id.download_books);

        downloadBooks_button.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	 bookList = new ArrayList<Book>();
            	 bookNameSortedList = new ArrayList<String>();
            	 
                 Boolean b_list_checked = searchBooksInMyDropbox("/bq/");
                 showToast(bookList.size() + " books");
                 if(b_list_checked){                 	                  	 
                 	 sortBooksByName();
                 	 showListOfBooks();
                 }else{
                	 showToast(error_msg);
                 }
            }
        });   
        
        
        
        closeApp_button = (Button)findViewById(R.id.close_app);

        closeApp_button.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	 logOut();
            	 finish();
            }
        });   
        
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.sortbooks_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.ordenar_nombre:
                showToast("Libros ordenados por Nombre");
                sortBooksByName();
                showListOfBooks();
                return true;
            case R.id.ordenar_fecha:
            	showToast("Libros ordenados por Fecha");
            	sortBooksByDate();
            	showListOfBooks();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        AndroidAuthSession session = mApi.getSession();
        

        // The next part must be inserted in the onResume() method of the
        // activity from which session.startAuthentication() was called, so
        // that Dropbox authentication completes properly.
        if (session.authenticationSuccessful()) {
            try {
                // Mandatory call to complete the auth
                session.finishAuthentication();

                // Store it locally in our app for later use
                storeAuth(session);
                isLogged = true;
            } catch (IllegalStateException e) {
            	Toast error = Toast.makeText(this, "Couldn't authenticate with Dropbox:" + e.getLocalizedMessage(), Toast.LENGTH_LONG);
                error.show();
                Log.i(TAG, "Error authenticating", e);
            }
        }
    }
    
    //////
    
    private AndroidAuthSession buildSession() {
        AppKeyPair appKeyPair = new AppKeyPair(APP_KEY, APP_SECRET);

        AndroidAuthSession session = new AndroidAuthSession(appKeyPair);
        loadAuth(session);
        return session;
    }
    
    private void loadAuth(AndroidAuthSession session) {
        SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
        String key = prefs.getString(ACCESS_KEY_NAME, null);
        String secret = prefs.getString(ACCESS_SECRET_NAME, null);
        if (key == null || secret == null || key.length() == 0 || secret.length() == 0) return;

        if (key.equals("oauth2:")) {
            // If the key is set to "oauth2:", then we can assume the token is for OAuth 2.
            session.setOAuth2AccessToken(secret);
        } else {
            // Still support using old OAuth 1 tokens.
            session.setAccessTokenPair(new AccessTokenPair(key, secret));
        }
    }
    
    private void storeAuth(AndroidAuthSession session) {
        // Store the OAuth 2 access token, if there is one.
        String oauth2AccessToken = session.getOAuth2AccessToken();
        if (oauth2AccessToken != null) {
            SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
            Editor edit = prefs.edit();
            edit.putString(ACCESS_KEY_NAME, "oauth2:");
            edit.putString(ACCESS_SECRET_NAME, oauth2AccessToken);
            edit.commit();
            return;
        }
        // Store the OAuth 1 access token, if there is one.  This is only necessary if
        // you're still using OAuth 1.
        AccessTokenPair oauth1AccessToken = session.getAccessTokenPair();
        if (oauth1AccessToken != null) {
            SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
            Editor edit = prefs.edit();
            edit.putString(ACCESS_KEY_NAME, oauth1AccessToken.key);
            edit.putString(ACCESS_SECRET_NAME, oauth1AccessToken.secret);
            edit.commit();
            return;
        }
    }
    
    private Boolean searchBooksInMyDropbox(String dbPath){
    	
    	try {
	    	// Get the metadata for a directory
	        Entry dirent = mApi.metadata(dbPath, 1000, null, true, null);
	
	        if (!dirent.isDir || dirent.contents == null) {
	            // It's not a directory, or there's nothing in it
	        	error_msg = "File or empty directory";
	            return false;
	        }
	        
            for (Entry ent: dirent.contents) {      
            	if(ent.isDir){
            		Log.d("DIRECTORY_CHANGE",ent.path);
            		searchBooksInMyDropbox(ent.path);
            	}else if(isAnEpubFile(ent)){
            		
            		Book currentBook = new Book(ent.fileName(), getDate(ent.clientMtime));
            		Log.i("BOOK ->",ent.fileName() + "; " + getDate(ent.clientMtime).getTime() + "; " + ent.clientMtime);
                	bookList.add(currentBook);
            	}
            }
	        
            if (bookList.size() == 0) {
                // No books in that directory
                error_msg = "No book files in that directory";
                return false;
            }
            
	        return true;	        
	        
    	} catch (DropboxUnlinkedException e) {
            // The AuthSession wasn't properly authenticated or user unlinked.
        } catch (DropboxPartialFileException e) {
            // We canceled the operation
        	error_msg = "Download canceled";
        } catch (DropboxServerException e) {
            // Server-side exception.  These are examples of what could happen,
            // but we don't do anything special with them here.
            if (e.error == DropboxServerException._304_NOT_MODIFIED) {
                // won't happen since we don't pass in revision with metadata
            } else if (e.error == DropboxServerException._401_UNAUTHORIZED) {
                // Unauthorized, so we should unlink them.  You may want to
                // automatically log the user out in this case.
            } else if (e.error == DropboxServerException._403_FORBIDDEN) {
                // Not allowed to access this
            } else if (e.error == DropboxServerException._404_NOT_FOUND) {
                // path not found
            } else if (e.error == DropboxServerException._406_NOT_ACCEPTABLE) {
                // too many entries to return
            } else if (e.error == DropboxServerException._415_UNSUPPORTED_MEDIA) {
            } else if (e.error == DropboxServerException._507_INSUFFICIENT_STORAGE) {
                // user is over quota
            } else {
                // Something else
            }
            // This gets the Dropbox error, translated into the user's language
            error_msg = e.body.userError;
            if (error_msg == null) {
            	error_msg = e.body.error;
            }
        } catch (DropboxIOException e) {
            // Happens all the time, probably want to retry automatically.
        	error_msg = "Network error.  Try again.";
        } catch (DropboxParseException e) {
            // Probably due to Dropbox server restarting, should retry
        	error_msg = "Dropbox error.  Try again.";
        } catch (DropboxException e) {
            // Unknown error
        	error_msg = "Unknown error.  Try again.";
        }

        return false;
    }
    
    private Boolean isAnEpubFile(Entry currentEnt){
		String[] pathFileSplit = currentEnt.fileName().split("\\.");
		return ("epub".equals(pathFileSplit[1]));
    }
    
    private void logOut() {
        // Remove credentials from the session
        mApi.getSession().unlink();

        // Clear our stored keys
        clearKeys();
        // Change UI state to display logged out version
        isLogged = false;
    }
    
    private void clearKeys() {
        SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
        Editor edit = prefs.edit();
        edit.clear();
        edit.commit();
    }
    
    private Date getDate(String stringDate){
    	SimpleDateFormat df = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.US);
    	Date date = null;
		try {
			date = df.parse(stringDate);
		} catch (ParseException e) {
			Log.e("Error in date",e.getMessage());
		}
		return date;
    }
    
    private void sortBooksByName(){
    	Collections.sort(bookList, new Comparator<Book>() {
    		  public int compare(Book o1, Book o2) {
    		      return o1.getName().compareTo(o2.getName());
    		  }
    	});
    	buildBookNameList();
    }
    
    private void sortBooksByDate(){
    	Collections.sort(bookList, new Comparator<Book>() {
    		  public int compare(Book o1, Book o2) {
    		      if (o1.getModified() == null || o2.getModified() == null)
    		        return 0;
    		      return o1.getModified().compareTo(o2.getModified());
    		  }
    	});
    	buildBookNameList();
    }
    
    private void buildBookNameList(){
    	bookNameSortedList.clear();
    	
    	Iterator<Book> it = bookList.iterator();
    	while(it.hasNext()){
    		Book currentBook = it.next();
    		bookNameSortedList.add(currentBook.getName());
    	}
    }
    
    private void showListOfBooks(){
    	lvBooks = (ListView) findViewById(R.id.listView);

    	 ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(MainActivity.this, 
    			 														android.R.layout.simple_list_item_1,
    			 														bookNameSortedList);             			 
    	 lvBooks.setAdapter(arrayAdapter);
    }
    

    private void showToast(String msg) {
        Toast error = Toast.makeText(this, msg, Toast.LENGTH_LONG);
        error.show();
    }
    
}
