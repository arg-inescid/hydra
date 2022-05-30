package org.graalvm.argo.lambda_proxy.utils;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.sun.net.httpserver.HttpExchange;

public class ProxyUtils {

    public static void writeResponse(HttpExchange t, int code, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        t.sendResponseHeaders(code, bytes.length);
        OutputStream os = t.getResponseBody();
        os.write(bytes);
        os.close();
    }

    public static void errorResponse(HttpExchange t, String errorMessage) throws IOException {
        writeResponse(t, 502, "ERROR: " + errorMessage);
    }

    public static String extractRequestBody(HttpExchange t) {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        try {
            for (int length; (length = t.getRequestBody().read(buffer)) != -1;) {
                result.write(buffer, 0, length);
            }
            return result.toString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    public static void catFile(String path) {
        String line;
        FileInputStream fin = null;
        BufferedReader br = null;
        InputStreamReader isr = null;
        try {
           fin =  new FileInputStream(path);
           isr = new InputStreamReader(fin);
           br = new BufferedReader(isr);
           while ((line = br.readLine()) != null) {
              System.out.println(line);
           }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
	} catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            try {
	        if (br != null) {
	            br.close();
	        }
	        if (isr != null) {
	            isr.close();
	        }
	        if (fin != null) {
	             fin.close();
	        }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
     }

    public static void writeFile(String path, String content) {
        try (PrintWriter out = new PrintWriter(path)) {
            out.println(content);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void listf(String directoryName, List<String> files) throws IOException {
        File directory = new File(directoryName);
        File[] fList = directory.listFiles();
        if(fList != null) {
            for (File file : fList) {
                if (file.getCanonicalPath().equals(file.getAbsolutePath())) {
                    System.out.println(file.getAbsolutePath());
                    if (file.isFile()) {
                        files.add(file.getCanonicalPath());
                    } else if (file.isDirectory()) {
                        listf(file.getAbsolutePath(), files);
                    }
                }
            }
        }
    }
}
