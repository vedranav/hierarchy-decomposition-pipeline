/*
 * Copyright (c) 2020 Vedrana Vidulin <vedrana.vidulin@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.vedranavidulin.data;

import com.google.common.collect.Table;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Enumeration;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 *
 * @author Vedrana Vidulin <vedrana.vidulin@gmail.com>
 */
public class DataReadWrite {
    public static BufferedReader findReaderType(File inFile) throws IOException {
        BufferedReader br = null;
        
        if (inFile.getAbsolutePath().endsWith("gz")) {
            GZIPInputStream gzip = new GZIPInputStream(new FileInputStream(inFile));       
            br = new BufferedReader(new InputStreamReader(gzip));
        } else if (inFile.getAbsolutePath().endsWith(".zip")) {
            ZipFile zipFile = new ZipFile(inFile);

            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            while(entries.hasMoreElements()) { //considers only the first file in the zip archive
                ZipEntry entry = entries.nextElement();
                InputStream stream = zipFile.getInputStream(entry);
                br = new BufferedReader(new InputStreamReader(stream));
                break;
            }
        } else
            br = new BufferedReader(new FileReader(inFile));
        
        return br;
    }
    
    public static File[] listFilesWithEnding (String folder, String ending) {
        return new File(folder).listFiles((File dir, String name) -> (name.toLowerCase().endsWith(ending)));
    }
    
    public static void writeTableConf(Table<String, String, Double> table, String header, File outGZFileWithTable) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(outGZFileWithTable.getAbsoluteFile())), "UTF-8"))) {
            bw.write(header);
            for (String column : table.columnKeySet())
                bw.write("," + column);
            bw.write("\n");
            
            for (String row : table.rowKeySet()) {
                bw.write(row);
                for (String column : table.columnKeySet())
                    bw.write("," + (table.get(row, column)));
                bw.write("\n");
            }
        }
    }
    
    public static void writeTableEval(Table<Double, String, Double> table, String header, File outGZFileWithTable) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(outGZFileWithTable.getAbsoluteFile())), "UTF-8"))) {
            bw.write(header);
            for (String column : table.columnKeySet())
                bw.write("," + column);
            bw.write("\n");
            
            for (double row : table.rowKeySet()) {
                bw.write(String.valueOf(row));
                for (String column : table.columnKeySet())
                    bw.write("," + table.get(row, column));
                bw.write("\n");
            }
        }
    }
    
    public static void writeTime(String outFolderPath, String decomposition, long time) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(new File(outFolderPath + "/" + decomposition + "/Execution_time.txt")))) {
            bw.write(time + " sec\n");
        }
    }
    
    public static void zipFile (String from, String to) throws IOException {
        byte[] buffer = new byte[1024];
 
        FileOutputStream fos = new FileOutputStream(to);
        ZipOutputStream zos = new ZipOutputStream(fos);
        ZipEntry ze = new ZipEntry(new File(from).getName());
        zos.putNextEntry(ze);
        FileInputStream in = new FileInputStream(from);

        int len;
        while ((len = in.read(buffer)) > 0)
            zos.write(buffer, 0, len);

        in.close();
        zos.closeEntry();

        zos.close();
    }
    
    public static void createFolders(String outFolderPath, String decomposition) {
        String[] subfolders = {"Dataset", "Results"};
        for (String sf : subfolders) {
            File f = new File(outFolderPath + "/" + decomposition + "/" + sf);
            if (!f.exists())
                f.mkdirs();
        }
    }
    
    public static void removeDatasets(String outFolderPath, String decomposition) {
        File datasetFolder = new File(outFolderPath + decomposition + "/Dataset");
        
        if (datasetFolder.exists()) {
            for (File f : datasetFolder.listFiles())
                f.delete();
            datasetFolder.delete();
        }
    }
    
    public static void removeResults(String outFolderPath, String decomposition) {
        File resultsFolder = new File(outFolderPath + "/" + decomposition + "/Results");
        if (resultsFolder.exists()) {
            for (File f : resultsFolder.listFiles())
                f.delete();
            resultsFolder.delete();
        }
        
        File[] hierarchyFiles = listFilesWithEnding(System.getProperty("user.dir"), ".hierarchy");
        if (hierarchyFiles.length > 0)
            for (File hf : hierarchyFiles)
                hf.delete();
    }
}
