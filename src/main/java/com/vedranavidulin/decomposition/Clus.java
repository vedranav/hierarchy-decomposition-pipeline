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
package com.vedranavidulin.decomposition;

import static com.vedranavidulin.data.DataReadWrite.listFilesWithEnding;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 *
 * @author Vedrana Vidulin <vedrana.vidulin@gmail.com>
 */
public class Clus {
    private final String outFolderPath;
    private final String Xmx;
    
    public Clus(String outFolderPath, String Xmx) {
        this.outFolderPath = outFolderPath;
        this.Xmx = Xmx;
    }
    
    public void constructModels(String decomposition) throws IOException, InterruptedException {
        File[] sFiles = listFilesWithEnding(outFolderPath + "/" + decomposition + "/Results", ".s");
        
        for (File sFile : sFiles) {
            String sFilePath = sFile.getAbsolutePath();
            
            Process ps = Runtime.getRuntime().exec(new String[]{"java", "-Xmx" + Xmx, "-jar", "clus.jar", "-forest", sFilePath});
            ps.waitFor();

            InputStream is = ps.getErrorStream();
            byte b[] = new byte[is.available()];
            is.read(b, 0, b.length);
            String errorOutput = new String(b); 
            System.out.print("\t" + sFilePath.substring(sFilePath.lastIndexOf("/") + 1, sFilePath.lastIndexOf(".s")).replace("_", " ") + " - ");
            if (errorOutput.isEmpty() || errorOutput.trim().toUpperCase().startsWith("WARNING"))
                System.out.println("ok");
            else {
                System.out.println("error");
                String[] errorMsg = errorOutput.split("\n");
                for (String line : errorMsg)
                    if (!line.trim().startsWith("at si.ijs.kt.clus"))
                        System.out.println("\t\t" + line);

            }
        }
    }
}