/*
 * Copyright (c) 2021 Vedrana Vidulin
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */
package com.vedranavidulin.decomposition;

import java.io.*;
import java.util.Arrays;
import static com.vedranavidulin.main.HierarchyDecompositionPipeline.settings;
import static com.vedranavidulin.data.DataReadWrite.listFilesWithEnding;

/**
 *
 * @author Vedrana Vidulin
 */
public class Clus {
    public void constructModels(String decomposition, boolean crossValidation) throws IOException, InterruptedException {
        File[] sFiles = listFilesWithEnding((crossValidation ? settings.getCrossValidationPath() : settings.getAnnotationsPath()) + decomposition + "/Results", ".s");
        
        for (File sFile : sFiles) {
            Process ps = Runtime.getRuntime().exec(new String[]{"java", "-Xmx" + settings.getXmx(), "-jar", "clus.jar", "-forest", sFile.getAbsolutePath()});
            ps.waitFor();

            InputStream is = ps.getErrorStream();
            byte[] b = new byte[is.available()];
            is.read(b, 0, b.length);
            String errorOutput = new String(b);
            System.out.print("\t" + sFile.getName().substring(0, sFile.getName().lastIndexOf(".s")).replace("_", " ") + " - ");
            if (errorOutput.isEmpty() || errorOutput.trim().toUpperCase().startsWith("WARNING"))
                System.out.println("ok");
            else {
                System.err.println("error");
                Arrays.asList(errorOutput.split("\n")).forEach(line -> {
                    if (!line.trim().startsWith("at si.ijs.kt.clus"))
                        System.err.println("\t\t" + line);
                });
                System.exit(-1);
            }
        }
    }
}