package com.sidemvm.ed.abluebook;

import android.os.Environment;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Created by super on 20/06/2018.*/

class AbbFileGenerator {

    /**The external file path of app*/
    private static File ABB_FILES_PATH;

    static {
        //Set files path
        ABB_FILES_PATH = getExternalAbbFilesPath();
    }

    static File getExternalAbbFilesPath(){
        File filesPath = new File(Environment.getExternalStorageDirectory(),"abb");
        if (filesPath.mkdirs()) return filesPath;
        else return filesPath;
    }

    /**Generate csv file*/
    static String generateCsvFile(long eFormId, List<AbbContent.AnswerItem> asToWrite){
        AbbContent.eFormItem ef = AbbContent.eFORM_ITEMS.get(eFormId);
        File fileToWrite;
        if(ef != null) fileToWrite = new File(ABB_FILES_PATH, ef.efName + ".csv");
        else fileToWrite = new File(ABB_FILES_PATH, "default.csv");
        try {BufferedWriter bw = new BufferedWriter(new FileWriter(fileToWrite));
            List<AbbContent.QuestionItem> qsForHeader = AbbContent.getQuestionItems(eFormId);
            bw.write("Originator,Date");
            List<Long> qIds = new ArrayList<>();
            for (int i = 0; i < qsForHeader.size(); i++){
                bw.write(",");
                bw.write(qsForHeader.get(i).q);
                qIds.add(qsForHeader.get(i).qId);
            }
            bw.newLine();
            for (int i = 0; i < asToWrite.size(); i++){
                AbbContent.AnswerItem aToWrite = asToWrite.get(i);
                bw.write(aToWrite.aOwner);
                bw.write(",");
                bw.write(aToWrite.aDate);
                for (long qId : qIds){
                    bw.write(",");
                    String a = aToWrite.as.get(qId);
                    if (a != null) bw.write(a);
                } bw.newLine();
            }
            bw.flush();
            bw.close();
            return "ok";
        } catch (IOException ioe) {return ioe.toString();}
    }

    /**Save eForm file*/
    static File saveEFromFile(long eFormId){
        AbbContent.eFormItem ef = AbbContent.eFORM_ITEMS.get(eFormId);
        String eFormName;
        File abbFile;
        if(ef != null) {
            eFormName = ef.efName;
            abbFile = new File(AbbContent.getAbbFilesPath(), eFormName + ".eqz");
        } else {
            eFormName = "default";
            abbFile = new File(AbbContent.getAbbFilesPath(), eFormName + ".eqz");
        }
        List<String> filesToZip = new ArrayList<>();
        filesToZip.add(abbFile.getAbsolutePath());
        File[] listOfFiles = AbbContent.getAbbFilesPath().listFiles();
        for(File everyFile : listOfFiles){
            if(everyFile.getName().contains(String.valueOf(eFormId)))
                filesToZip.add(everyFile.getAbsolutePath());
        }
        return zip(filesToZip, eFormName);
    }

    /**Call to save line on a new file*/
    static File saveAnsFile(String fileName, String lineToWrite){
        File lineFile = new File(ABB_FILES_PATH, fileName + ".abba");
        try {//try to create the test file
            BufferedWriter bw = new BufferedWriter(new FileWriter(lineFile));
            bw.write(lineToWrite);
            bw.flush();
            bw.close();
        } catch (IOException ioe) {return null;} return lineFile;
    }

    /**Generate eForm File*/
    private static File zip(List<String> filesToZip, String eFormName){
        File zipFile = new File(ABB_FILES_PATH, eFormName + ".abb");
        BufferedInputStream origin;
        try {
            ZipOutputStream out = new
                    ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)));
            int BUFFER_SIZE = 6 * 1024;
            byte data[] = new byte[BUFFER_SIZE];
            for (int i = 0; i < filesToZip.size(); i++){
                FileInputStream fi = new FileInputStream(filesToZip.get(i));
                origin = new BufferedInputStream(fi, BUFFER_SIZE);
                try {
                    ZipEntry entry = new
                            ZipEntry(filesToZip.get(i)
                            .substring(filesToZip.get(i).lastIndexOf("/") + 1));
                    out.putNextEntry(entry);
                    int count;
                    while ((count = origin.read(data, 0, BUFFER_SIZE)) != -1)
                        out.write(data, 0, count);
                } finally {
                    origin.close();
                }
            } out.close();
            return zipFile;
        } catch (IOException ioe) {return null;}
    }

    /**To rename any file to get it in abb files path*/
    static boolean renameToGet(String fullPath, String newName){
        File fToRename = new File(fullPath);
        return fToRename.renameTo(new File(ABB_FILES_PATH, newName));
    }
}
