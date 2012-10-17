package com.camera.simplemjpeg;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;


import java.util.Properties;

/*
import java.net.URI;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
*/

import java.net.HttpURLConnection;
import java.net.URL;

import android.util.Log;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class MjpegInputStream extends DataInputStream {
    private final byte[] SOI_MARKER = { (byte) 0xFF, (byte) 0xD8 };
    private final byte[] EOF_MARKER = { (byte) 0xFF, (byte) 0xD9 };
    private final String CONTENT_LENGTH = "Content-Length";
    private final static int HEADER_MAX_LENGTH = 100;
    private final static int FRAME_MAX_LENGTH = 40000 + HEADER_MAX_LENGTH;
    private int mContentLength = -1;
    byte[] header =null;
    byte[] frameData =null;
    int headerLen = -1;
    int headerLenPrev = -1;
    
    int skip=1;
    int count=0;
    
    static {
    	System.loadLibrary("ImageProc");
    }
    public native void pixeltobmp(byte[] jp, int l, Bitmap bmp);
	
	
    public static MjpegInputStream read(String surl) {
    	try {
    		URL url = new URL(surl);
    		HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
    		return new MjpegInputStream(urlConnection.getInputStream());
    	}catch(Exception e){}
    	
        return null;
    }
	
    public MjpegInputStream(InputStream in) {
        super(new BufferedInputStream(in, FRAME_MAX_LENGTH));
    }
	
    private int getEndOfSeqeunce(DataInputStream in, byte[] sequence) 
        throws IOException 
    {

        int seqIndex = 0;
        byte c;
        for(int i=0; i < FRAME_MAX_LENGTH; i++) {
            c = (byte) in.readUnsignedByte();
            if(c == sequence[seqIndex]) {
                seqIndex++;
                if(seqIndex == sequence.length){

                	return i + 1;
                }
            } else seqIndex = 0;
        }
        

        return -1;
    }
	
    private int getStartOfSequence(DataInputStream in, byte[] sequence) 
        throws IOException 
    {
        int end = getEndOfSeqeunce(in, sequence);
        return (end < 0) ? (-1) : (end - sequence.length);
    }

    private int parseContentLength(byte[] headerBytes) 
        throws IOException, NumberFormatException
    {
        ByteArrayInputStream headerIn = new ByteArrayInputStream(headerBytes);
        Properties props = new Properties();
        props.load(headerIn);
        return Integer.parseInt(props.getProperty(CONTENT_LENGTH));
    }	

    public Bitmap readMjpegFrame() throws IOException {
        mark(FRAME_MAX_LENGTH);
        int headerLen = getStartOfSequence(this, SOI_MARKER);
        reset();

        if(header==null || headerLen != headerLenPrev){
        	header = new byte[headerLen];
        	Log.d("TEST","header renewed");
        }
        headerLenPrev = headerLen;
        readFully(header);

        try {
            mContentLength = parseContentLength(header);
        } catch (NumberFormatException nfe) { 
            mContentLength = getEndOfSeqeunce(this, EOF_MARKER); 
        }
        reset();
        
        if(frameData==null){
        	frameData = new byte[FRAME_MAX_LENGTH*2];
        	Log.d("TEST","frameData renewed cl="+mContentLength);
        }
        if( mContentLength>FRAME_MAX_LENGTH*2){
        	frameData = new byte[mContentLength];
        	Log.d("TEST","frameData renewed cl="+mContentLength);
        }
        
        skipBytes(headerLen);

        readFully(frameData, 0, mContentLength);

        if(count++%skip==0){
        	return BitmapFactory.decodeStream(new ByteArrayInputStream(frameData,0,mContentLength));
        }else{
        	return null;
        }
    }
    
    public void readMjpegFrame(Bitmap bmp) throws IOException {
        mark(FRAME_MAX_LENGTH);
        int headerLen = getStartOfSequence(this, SOI_MARKER);
        reset();

        if(header==null || headerLen != headerLenPrev){
        	header = new byte[headerLen];
        	Log.d("TEST","header renewed");
        }
        headerLenPrev = headerLen;
        readFully(header);

        try {
            mContentLength = parseContentLength(header);
        } catch (NumberFormatException nfe) { 
            mContentLength = getEndOfSeqeunce(this, EOF_MARKER); 
        }
        reset();
        
        if(frameData==null){
        	frameData = new byte[FRAME_MAX_LENGTH*2];
        	Log.d("TEST","frameData renewed cl="+mContentLength);
        }
        if( mContentLength>FRAME_MAX_LENGTH*2){
        	frameData = new byte[mContentLength];
        	Log.d("TEST","frameData renewed cl="+mContentLength);
        }
        
        skipBytes(headerLen);

        readFully(frameData, 0, mContentLength);

        if(count++%skip==0){
        	pixeltobmp(frameData, mContentLength, bmp);
        }else{
        }
    }
    public void setSkip(int s){
    	skip = s;
    }
}
