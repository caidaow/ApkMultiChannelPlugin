package com.github.nukc.plugin.helper;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.io.ZipUtil;

import java.io.*;
import java.util.Enumeration;
import java.util.Map;
import java.util.zip.*;

/**
 * Created by Nukc.
 */
public class ZipHelper {

    public static void extractAndroidManifestXml(String zipPath, String outputDir) {
        File file = new File(zipPath);
        File tempFile = deleteTemp(outputDir);
        tempFile.mkdirs();

        int buffer = 2048;

        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        try {
            ZipFile zip = new ZipFile(file);
            Enumeration entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) entries.nextElement();
                File entryFile = new File(outputDir, entry.getName());
                File parentFile = entryFile.getParentFile();

                if (!"AndroidManifest.xml".equals(entry.getName())) {
                    continue;
                }

                if (!parentFile.exists()) {
                    parentFile.mkdirs();
                }

                if (!entryFile.isDirectory()) {
                    bis = new BufferedInputStream(zip.getInputStream(entry));
                    int currentByte;
                    // establish buffer for writing file
                    byte[] data = new byte[buffer];

                    // write the current file to disk
                    FileOutputStream fos = new FileOutputStream(entryFile);
                    bos = new BufferedOutputStream(fos, buffer);

                    // read and write until last byte is encountered
                    while ((currentByte = bis.read(data, 0, buffer)) != -1) {
                        bos.write(data, 0, currentByte);
                    }
                    bos.flush();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bos != null) {
                    bos.close();
                }
                if (bis != null) {
                    bis.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public static File deleteTemp(String tempPath) {
        File tempFile = new File(tempPath);
        if (tempFile.exists()) {
            deleteFile(tempFile);
        }
        return tempFile;
    }

    private static void deleteFile(File file) {
        if (file.exists()) {
            if (file.isFile()) {
                file.delete();
            } else if (file.isDirectory()) {
                File[] files = file.listFiles();
                if (files != null) {
                    for (File file1 : files) {
                        deleteFile(file1);
                    }
                }
                file.delete();
            }
        }
    }

    public static boolean update(InputStream in, OutputStream out, Map<String, File> relpathToFile) {
        try {
            update(in, out, relpathToFile, true);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * update an existing jar file. Adds/replace files specified in relpathToFile map
     */
    public static void update(InputStream in, OutputStream out, Map<String, File> relpathToFile, boolean except) throws IOException {
        ZipInputStream zis = new ZipInputStream(in);
        ZipOutputStream zos = new ZipOutputStream(out);

        try {
            // put the old entries first, replace if necessary
            ZipEntry e;
            while ((e = zis.getNextEntry()) != null) {
                String name = e.getName();
                //except signature file
                if (except && name.contains("META-INF") &&
                        (name.endsWith("RSA") || name.contains("SF") || name.contains("MANIFEST.MF"))) {
                    continue;
                }

                if (!relpathToFile.containsKey(name)) { // copy the old stuff
                    // do our own compression
                    ZipEntry e2 = new ZipEntry(name);
                    e2.setMethod(e.getMethod());
                    e2.setTime(e.getTime());
                    e2.setComment(e.getComment());

                    e2.setExtra(e.getExtra());
                    if (e.getMethod() == ZipEntry.STORED) {
                        e2.setSize(e.getSize());
                        e2.setCrc(e.getCrc());
                    }
                    zos.putNextEntry(e2);
                    FileUtil.copy(zis, zos);
                } else { // replace with the new files
                    final File file = relpathToFile.get(name);
                    //addFile(file, name, zos);
                    relpathToFile.remove(name);
                    ZipUtil.addFileToZip(zos, file, name, null, null);
                }
            }

            // add the remaining new files
            for (final String path : relpathToFile.keySet()) {
                File file = relpathToFile.get(path);
                ZipUtil.addFileToZip(zos, file, path, null, null);
            }
        } finally {
            zis.close();
            zos.close();
        }
    }

}
