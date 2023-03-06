package com.oracle.svm.graalvisor.utils;

import java.awt.Dimension;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Map;

import com.criteo.vips.VipsException;
import com.criteo.vips.VipsImage;
import com.criteo.vips.enums.VipsImageFormat;

import org.graalvm.polyglot.HostAccess;

public class PolyglotHostAccess {

    @HostAccess.Export
    public void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @HostAccess.Export
    public byte[] readBytes(String path) {
        try {
            return Files.readAllBytes(new File(path).toPath());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @HostAccess.Export
    public void writeBytes(byte[] bytes, String path) {
        try {
            Files.write(new File(path).toPath(), bytes, StandardOpenOption.CREATE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @HostAccess.Export
    public byte[] downloadBytes(String url) {
        try {
            URLConnection conn = new URL(url).openConnection();
            InputStream is = conn.getInputStream();
            byte[] bytes = is.readAllBytes();
            is.close();
            return bytes;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @HostAccess.Export
    public void uploadBytes(String url, byte[] bytes) {
        int postDataLength = bytes.length;
        HttpURLConnection conn;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setDoOutput(true);
            conn.setInstanceFollowRedirects(false);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "image/png");
            conn.setRequestProperty("charset", "utf-8");
            conn.setRequestProperty("Content-Length", Integer.toString(postDataLength));
            conn.setUseCaches(false);
            try( DataOutputStream wr = new DataOutputStream( conn.getOutputStream())) {
                wr.write(bytes);
             } catch (IOException e) {
                 e.printStackTrace();
             }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Note: Access to vips (replacement for nodejs sharp package).
    @HostAccess.Export
    public byte[] resize(byte[] bytes, float ratio) {
        try {
            VipsImage image = new VipsImage(bytes, bytes.length);
            int width = (int) (image.getWidth() * ratio);
            int height =(int) (image.getHeight() * ratio);
            image.thumbnailImage(new Dimension(width, height), true);
            bytes = image.writeToArray(VipsImageFormat.PNG, false);
            image.release();
            return bytes;
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }

}
