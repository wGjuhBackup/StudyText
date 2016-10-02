package com.studypoem.wgjuh.byheart;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by WGJUH on 20.09.2016.
 */
public class SQLWorker extends SQLiteOpenHelper {
    public static final String DB_NAME = "PoemsTable";
    public static final String DB_LOCATION = "/data/data/"+BuildConfig.APPLICATION_ID+"/databases/";
    Context context;
    SQLiteDatabase database;
    public SQLWorker(Context context) {
        super(context,DB_NAME,null,1);
        this.context=context;
        System.out.println(MainActivity.TAG + "Context: " + context);
        boolean dbexist = checkdatabase();
        if (dbexist) {
            System.out.println(MainActivity.TAG + "Database exists");
            //opendatabase();
        } else {
            System.out.println(MainActivity.TAG + "Database doesn't exist");
            try {
                createdatabase();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    public void createdatabase() throws IOException {
        boolean dbexist = checkdatabase();
        if(dbexist) {
            System.out.println(MainActivity.TAG + " Database exists.");
        } else {
            this.getReadableDatabase();
            try {
                copydatabase();
            } catch(IOException e) {
                e.printStackTrace();
                throw new Error("Error copying database");
            }
        }
    }

    private boolean checkdatabase() {

        boolean checkdb = false;
        try {
            String myPath = DB_LOCATION + DB_NAME;
            File dbfile = new File(myPath);
            checkdb = dbfile.exists();
        } catch(SQLiteException e) {
            System.out.println(MainActivity.TAG + "Database doesn't exist");
        }
        return checkdb;
    }

    /**
     * TODO error with assets file
     * @throws IOException
     */
    private void copydatabase() throws IOException{
        //Open your local db as the input stream
        System.out.println(MainActivity.TAG + " START COPYING");
        InputStream myinput = null;
        try {
            myinput = context.getAssets().open(DB_NAME);
        } catch (IOException e) {
            System.out.println(MainActivity.TAG + " get from assets failed ");
            e.printStackTrace();
        }
        System.out.println(MainActivity.TAG + " assets: " + myinput.available());
        // Path to the just created empty db
        String outfilename = DB_LOCATION + DB_NAME;

        //Open the empty db as the output stream
        OutputStream myoutput = new FileOutputStream(outfilename);

        // transfer byte to inputfile to outputfile
        byte[] buffer = new byte[myinput.available()];
        System.out.println(MainActivity.TAG + " BUFFER: " + buffer.length);
        int length;
        while ((length = myinput.read(buffer))>0) {
            myoutput.write(buffer,0,length);
        }

        //Close the streams
        myoutput.flush();
        myoutput.close();
        myinput.close();
    }

    public void opendatabase() throws SQLException {
        //Open the database
        String mypath = DB_LOCATION + DB_NAME;
        database = SQLiteDatabase.openDatabase(mypath, null, SQLiteDatabase.OPEN_READWRITE);

    }

    public synchronized void close() {
        if(database != null) {
            database.close();
        }
        super.close();
    }

    public Values getStringsFromDB(String regex) {
        opendatabase();
        String column = "author_name";
        Cursor cursor;
        Integer currentLvl = ListPoems.lvl;
        if (currentLvl != 0 && regex != null) {
            switch (currentLvl) {
                case 0:
                    column = "author_name";
                    break;
                case 1:
                    column = "title";
                    break;
                case 2:
                    column = "poem";
                    break;
                default:
                    break;
            }
            cursor = database.query(DB_NAME, new String[]{column}, "author_name LIKE ? OR title LIKE ?", new String[]{"%" + regex + "%", "%" + regex + "%"}, column, null, column);
        } else {
            cursor = database.query(DB_NAME, new String[]{column,"author_portrait_id"}, null, null, column, null, column);
        }
        System.out.println(MainActivity.TAG + " " + cursor.getCount() + " names " + Arrays.toString(cursor.getColumnNames()));
        ArrayList<String> adapterStrings = new ArrayList<>();
        ArrayList<Integer>ids = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {
                String temp = cursor.getString(0);
                if(temp != null) {
                    System.out.println(MainActivity.TAG + "STRING: " + temp);
                    adapterStrings.add(temp);
                    if (currentLvl == 0)
                        ids.add(cursor.getInt(1));
                }
            } while (cursor.moveToNext());
        } else System.out.println(MainActivity.TAG + MainActivity.TAG + " 0 elements");
//        System.out.println(MainActivity.TAG + MainActivity.TAG + adapterStrings.get(0).toString());
        close();
        return new Values(adapterStrings,ids);
    }
    public Values getStarred(){
        opendatabase();
        Cursor cursor = database.query(DB_NAME,new String[]{"title"},"favorite = ?",new String[]{""+1},null,null,null);
        ArrayList<String> adapterStrings = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {
                String temp = cursor.getString(0);
                if(temp != null) {
                    System.out.println(MainActivity.TAG + "STRING: " + temp);
                    adapterStrings.add(temp);
                }
            } while (cursor.moveToNext());
        } else System.out.println(MainActivity.TAG + MainActivity.TAG + " 0 elements");
        close();
        return new Values(adapterStrings,null);
    }
    public boolean isStarred(String author, String title){
        boolean starred = false;
        opendatabase();
        System.out.println(MainActivity.TAG + " author: " + author + " title: " + title);
        if(!author.equals("Starred")){
        if(author != null){
            Cursor cursor = database.query(DB_NAME, new String[]{"id_author","favorite"}, "author_name LIKE ? AND title LIKE ?", new String[]{"%" + author + "%", "%" + title + "%"}, null, null, null);
            if(cursor.moveToFirst()){
                System.out.println(MainActivity.TAG + " current state: " + cursor.getInt(cursor.getColumnIndex("favorite")));
                if(cursor.getInt(cursor.getColumnIndex("favorite")) == 1){
                    starred = true;
                }else starred = false;
            }else {
                System.out.println(MainActivity.TAG + " NOT found");
            }
        }
        }else{
            System.out.println(MainActivity.TAG + " STARRED ");
            Cursor cursor = database.query(DB_NAME, new String[]{"id_author","favorite"}, "title LIKE ?", new String[]{"%" + title + "%"}, null, null, null);
            if(cursor.moveToFirst()){
                System.out.println(MainActivity.TAG + " current state: " + cursor.getInt(cursor.getColumnIndex("favorite")));
                if(cursor.getInt(cursor.getColumnIndex("favorite")) == 1){
                    starred = true;
                }else starred = false;
            }else {
                System.out.println(MainActivity.TAG + " NOT found");
            }
        }
        close();
        return starred;
    }
    public int setStar(String author, String title){
        int newState = 0;
        opendatabase();
        System.out.println(MainActivity.TAG + " author: " + author + " title: " + title);
        if(!author.equals("Starred")){
            if(author != null){
            Cursor cursor = database.query(DB_NAME, new String[]{"id_author","favorite"}, "author_name LIKE ? AND title LIKE ?", new String[]{"%" + author + "%", "%" + title + "%"}, null, null, null);

            ContentValues contentValues = new ContentValues();

            if(cursor.moveToFirst()){
                System.out.println(MainActivity.TAG + " current state: " + cursor.getInt(cursor.getColumnIndex("favorite")) + " new state: " + (cursor.getInt(cursor.getColumnIndex("favorite"))^1));
                newState = cursor.getInt(cursor.getColumnIndex("favorite"))^1;
                contentValues.put("favorite",newState);
                System.out.println(MainActivity.TAG + " upadated: " + database.update(DB_NAME,contentValues,"id_author = ?", new String[]{cursor.getString(0)}));
            }else {
                System.out.println(MainActivity.TAG + " NOT found");
            }

        }
        }else{
            Cursor cursor = database.query(DB_NAME, new String[]{"id_author","favorite"}, "title LIKE ?", new String[]{"%" + title + "%"}, null, null, null);
            ContentValues contentValues = new ContentValues();
            if(cursor.moveToFirst()){
                System.out.println(MainActivity.TAG + " current state: " + cursor.getInt(cursor.getColumnIndex("favorite")) + " new state: " + (cursor.getInt(cursor.getColumnIndex("favorite"))^1));
                newState = cursor.getInt(cursor.getColumnIndex("favorite"))^1;
                contentValues.put("favorite",newState);
                System.out.println(MainActivity.TAG + " upadated: " + database.update(DB_NAME,contentValues,"id_author = ?", new String[]{cursor.getString(0)}));
            }else {
                System.out.println(MainActivity.TAG + " NOT found");
            }
        }
        close();
        return newState;
    }
    public void addStringToDB(String authorName, String poemTitle, String text, Boolean starred){
        //opendatabase();
        database = getWritableDatabase();
        Cursor cursor = database.query(DB_NAME,null,"(poem LIKE ? OR title LIKE ?)",new String[]{"%" + text + "%", "%" + poemTitle + "%"},null,null,null);
        ContentValues contentValues = new ContentValues();
        contentValues.put("author_name",authorName);
        contentValues.put("title",poemTitle);
        contentValues.put("poem",text);
        if(starred)contentValues.put("favorite",1);
        if(cursor.moveToFirst()){
            for(int i = 0; i < cursor.getColumnCount(); i++)
            System.out.println(MainActivity.TAG + " "+cursor.getColumnName(i)+" " + cursor.getString(i));
            database.update(DB_NAME,contentValues, "id_author = ?", new String[]{cursor.getString(0)});
        }else
            database.insert(DB_NAME,null,contentValues);
        close();
    }

    public int removeRow(String author, String title){
        int deleted = -1;
        Cursor cursor;
        opendatabase();
        System.out.println(MainActivity.TAG + " author: " + author + " title: " + title);
        if(author.equals("Library")){
            cursor = database.query(DB_NAME, new String[]{"id_author","favorite"}, "author_name = ?", new String[]{ title}, null, null, null);
        }else {
            cursor = database.query(DB_NAME, new String[]{"id_author","favorite"}, "author_name = ? AND title = ?", new String[]{ author,title}, null, null, null);
        }
        if(cursor.moveToFirst()) {
            do {
                deleted = database.delete(DB_NAME, "id_author = ?", new String[]{cursor.getString(0)});
            }while (cursor.moveToNext());
        }else System.out.println(MainActivity.TAG + " NOT FOUND");
        System.out.println(MainActivity.TAG + " deleted: " + deleted);
        close();
        return deleted;
    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        System.out.println(MainActivity.TAG + MainActivity.TAG + " CREATE db");
/*        db.execSQL("create table "+ DB_NAME+" ("
                +   "id_author integer primary key,"
                +   "author_name text,"
                +   "author_portrait_id integer,"
                +   "title text,"
                +   "poem text"
                +   ");");
        String[] authors = context.getResources().getStringArray(R.array.poets);
        TypedArray authorsPortraitIds= context.getResources().obtainTypedArray(R.array.poets_portraits);
        String[] titles = context.getResources().getStringArray(R.array.titles);
        String[] poems = context.getResources().getStringArray(R.array.poems);
        System.out.println(MainActivity.TAG + MainActivity.TAG + " poems: " + Arrays.toString(poems)+ " names: " + Arrays.toString(authors) + " Titles: " + Arrays.toString(titles));
        ContentValues contentValues = new ContentValues();
        for(int i = 0 ; i < authors.length; i++){
            contentValues.clear();
            contentValues.put("author_name",authors[i]);
            contentValues.put("author_portrait_id", authorsPortraitIds.getResourceId(i,-1));
            contentValues.put("title",titles[i]);
            contentValues.put("poem",poems[i]);
            System.out.println(MainActivity.TAG + MainActivity.TAG+ " put into: " +  db.insert(DB_NAME,null,contentValues));
        }*/
    }
    public int getRowNumber(String s){
        System.out.println(MainActivity.TAG + " take rowNumber");
        opendatabase();
        Cursor cursor = database.query(DB_NAME,null,null,null,"author_name",null,"author_name");
        int i = 0;
        if(cursor.moveToFirst())
            do {
                i++;
                if(cursor.getString(cursor.getColumnIndex("author_name")).equals(s)) break;
            }while (cursor.moveToNext());
            close();
        return i;
    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}