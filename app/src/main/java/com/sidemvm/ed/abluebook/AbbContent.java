package com.sidemvm.ed.abluebook;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.util.LongSparseArray;

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
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Created by super on 25/03/2018.*/

class AbbContent {

    /**The external file path of app*/
    private static File ABB_FILES_PATH;
    /** A list and long sparse array of  for the list */
    static final List<eFormItem> eFORM_ITEM_LIST = new ArrayList<>();
    static LongSparseArray<eFormItem> eFORM_ITEMS = new LongSparseArray<>();

    static {
        int countForAdOdds = 1;
        //Set files path
        ABB_FILES_PATH = getAbbFilesPath();
        //Array to manage the source data
        File[] listOfFiles = ABB_FILES_PATH.listFiles();
        // Filter only usable eForm files
        for (File everyFile : listOfFiles) {
            //Condition to check eForm file extension
            if (everyFile.getName().endsWith(".eqz")) {
                // get the content and verify if there is a eForm info line
                String everyName = everyFile.getName().replace(".eqz","");
                eFormItem ef = getFormItem(everyName);
                if(ef != null) {
                    addFormItem(ef, -1, false);
                    if(adOdds(countForAdOdds)){
                        addFormItem(new eFormItem("", UUID.randomUUID().hashCode(), "",
                                "ad", new String[0]), -1, false);
                        countForAdOdds = 1;
                    } else countForAdOdds++;
                }
            }
        }
    }

    static File getAbbFilesPath(){
        File filesPath = new File(AbbMainActivity.eFormFilePath ,"abb");
        if (filesPath.mkdirs()) return filesPath;
        else return filesPath;
    }

    private static boolean adOdds(int count){
        Random random = new Random();
        switch (count) {
            case 4: return true;
            case 3: return random.nextInt(3) == 0;
            case 2: return random.nextInt(10) == 0;
            case 1: return random.nextInt(25) == 0;
            default: return false;
        }
    }

    /**update eForm items*/
    static void addFormItem(eFormItem efItem, int index, boolean isEditing) {
        if (index != -1){
            if (isEditing) eFORM_ITEM_LIST.set(index, efItem);
            else eFORM_ITEM_LIST.add(index, efItem);
        }
        else eFORM_ITEM_LIST.add(efItem);
        eFORM_ITEMS.put(efItem.efId, efItem);
    }

    static void setFormItem(int index, eFormItem efItem){
        eFORM_ITEM_LIST.set(index, efItem);
        eFORM_ITEMS.put(efItem.efId, efItem);
    }

    static void removeFormItem(eFormItem efItem){
        eFORM_ITEM_LIST.remove(efItem);
        eFORM_ITEMS.remove(efItem.efId);
    }

    /**Call it to get the eForm item*/
    private static eFormItem getFormItem(String fileName){
        String getInfo;
        String [] efInfo;
        List<String> contentByLines = getContentByLines(fileName, true);
        if (contentByLines.size() != 0) getInfo = contentByLines.get(0); else getInfo = "";
        if (getInfo.contains("<<eFormInfoLine>>")) {
            efInfo = getInfo.split(">>(.*?)::");
            return new eFormItem(
                    fileName,
                    Long.parseLong(efInfo[2]),
                    efInfo[1],
                    efInfo[3],
                    new String[]{efInfo[4], efInfo[5], efInfo[6], efInfo[7], efInfo[8]});
        } return null;
    }

    /**Call it to get the eForm question items*/
    static List<QuestionItem> getQuestionItems(long efId) {
        List<String> efByLines = new ArrayList<>();
        eFormItem ef = eFORM_ITEMS.get(efId);
        if(ef != null) efByLines = getContentByLines(ef.efName,false);
        List<QuestionItem> qList = new ArrayList<>();
        String[] qInfo;
        for (String everyLine : efByLines) {
            if (everyLine.contains("<<qLine>>")) {
                qInfo = everyLine.split(">>(.*?)::");
                QuestionItem everyQItem = new QuestionItem(
                        Long.parseLong(qInfo[1]),
                        Long.parseLong(qInfo[2]),
                        Integer.parseInt(qInfo[3]),
                        qInfo[4],
                        new ArrayList<>(Arrays.asList(qInfo).subList(5, qInfo.length-1)));
                qList.add(everyQItem);
            }
        } return qList;
    }

    /**Call it to get the eForm answers item*/
    static List<AnswerItem> getAnswersItems(long efId) {
        List<String> efByLines = new ArrayList<>();
        eFormItem ef = eFORM_ITEMS.get(efId);
        if(ef != null) efByLines = getContentByLines(ef.efName,false);
        List<AnswerItem> aList = new ArrayList<>();
        String[] aInfo;
        for (String everyLine : efByLines) {
            if (everyLine.contains("<<aLine>>")) {
                aInfo = everyLine.split(">>(.*?)::");
                LongSparseArray<String> as = new LongSparseArray<>();
                for (int i=4; i<aInfo.length-1; i+=2) as.put(Long.parseLong(aInfo[i]),aInfo[i+1]);
                AnswerItem everyAItem = new AnswerItem(
                        Long.parseLong(aInfo[1]),
                        aInfo[2],
                        aInfo[3],
                        as);
                aList.add(everyAItem);
            }
        } return aList;
    }

    /**Charge all eForm in a array list*/
    static List<String> getContentByLines(String efName, boolean OnlyFirstLine){
        List<String> efByLines = new ArrayList<>();
        File eqFile = new File(ABB_FILES_PATH, efName + ".eqz");
        try {BufferedReader br = new BufferedReader(new FileReader(eqFile));
            String line;
            while ((line = br.readLine())!= null) {
                efByLines.add(line);
                if (OnlyFirstLine) break;
            }
            br.close();
            return efByLines;
        } catch (IOException ioe) {return efByLines;}
    }

    /**To save on eForm file*/
    static boolean writeEFormFile(String efName, List<String> lines, boolean append){
        File fileToWrite = new File(ABB_FILES_PATH, efName + ".eqz");
        try {BufferedWriter bw = new BufferedWriter(new FileWriter(fileToWrite, append));
            for (int i = 0; i < lines.size(); i++) {
                bw.write(lines.get(i));
                bw.newLine();
            }
            bw.flush();
            bw.close();
            return true;
        } catch (IOException ioe) {return false;}
    }

    /**Call to save eForm on a new file*/
    static String saveNewEForm(int index, eFormItem efItem, List<String> eFormInfoLines){
        File testFile = new File(ABB_FILES_PATH, efItem.efName + ".eqz");
        try {//try to create the test file
            if (testFile.createNewFile()) {
                if (writeEFormFile(efItem.efName, eFormInfoLines,false)) {
                    addFormItem(efItem, index, false);
                    return "ok";
                }
                else return "Write fail";
            } else return "Crate new file fail";
        } catch (IOException ioe) {return ioe.toString();}
    }

    /**Call to rename file*/
    static Boolean renameEFormFile(eFormItem efIdToRename, eFormItem efWhitNewName) {
        int index = AbbContent.eFORM_ITEM_LIST.indexOf(efIdToRename);
        File oldFile = new File(ABB_FILES_PATH, efIdToRename.efName + ".eqz");
        if (oldFile.renameTo(new File(ABB_FILES_PATH, efWhitNewName.efName + ".eqz"))){
            addFormItem(efWhitNewName, index, true);
            return true;
        } else return false;
    }

    /**Call to delete eForm file*/
    static String deleteEF(eFormItem efToDelete){
        File eFormFileToDelete = new File(ABB_FILES_PATH, efToDelete.efName +".eqz");
        String imaDel = "";
        try {
            //try to delete the form file
            if (eFormFileToDelete.delete()) {
                removeFormItem(efToDelete);
                File[] listOfFiles = AbbContent.getAbbFilesPath().listFiles();
                for(File everyFile : listOfFiles){
                    if(everyFile.getName().contains(String.valueOf(efToDelete.efId)))
                        if (everyFile.delete()) imaDel = "Images deleted";
                }
                return "ok";
            } else return "Delete Fail";
        } catch(SecurityException e){return e.toString()+imaDel;}
    }

    /**Call to get the uri and create file in local storage*/
    static String uriImageToFile(AbbMainActivity mActivity, Uri uri){
        try{InputStream inputStream = mActivity.getContentResolver().openInputStream(uri);
            Bitmap bmp = BitmapFactory.decodeStream(inputStream);
            File targetFile = new File(ABB_FILES_PATH, "lastPick.jpeg");
            int imaWidth = bmp.getWidth();
            int imaHeight = bmp.getHeight();
            int newWidth = imaWidth;
            int newHeight = imaHeight;
            if(imaWidth > 512){
                newWidth = 512;
                newHeight = (newWidth*imaHeight)/imaWidth;
            }
            if(imaHeight > 512){
                newHeight = 512;
                newWidth = (newHeight*imaWidth)/imaHeight;
            }
            Bitmap scaledBmp = Bitmap.createScaledBitmap(bmp, newWidth, newHeight, false);
            FileOutputStream outStream = new FileOutputStream(targetFile);
            scaledBmp.compress(Bitmap.CompressFormat.JPEG, 85, outStream);
            outStream.flush();
            outStream.close();
            return "ok";
        } catch (IOException ioe){return ioe.toString();}
    }

    /**Call to delete images*/
    static boolean delImageFile(String imaName){
        File imaToDelete = new File(ABB_FILES_PATH, imaName);
        return imaToDelete.delete();
    }

    /**To rename any file to get it in abb files path*/
    static boolean renameToGet(String fullPath, String newName){
        File fToRename = new File(fullPath);
        return fToRename.renameTo(new File(ABB_FILES_PATH, newName));
    }

    /**handles files intend*/
    static String handleIncomingFile(AbbMainActivity mActivity, Uri uri){
        try {
            InputStream inputStream = mActivity.getContentResolver().openInputStream(uri);
            if(inputStream != null) {
                String aLine;
                BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
                aLine = br.readLine();
                br.close();
                if(aLine != null && aLine.contains("<<aLine>>")) return aLine;
                else {
                    InputStream iStream = mActivity.getContentResolver().openInputStream(uri);
                    String r = mActivity.getString(R.string.fail_message);
                    if (iStream != null) {
                        r = runUnzipping(mActivity, getFileFromInputStream(iStream));
                        if (r.equals("ok")) return mActivity.getString(R.string.opening_file_msg);
                        else return r;
                    } else return r;
                }
            } else return mActivity.getString(R.string.handle_file_fail_msg);
        } catch (IOException ioe) {
            return mActivity.getString(R.string.handle_file_fail_msg)+"\n"+ioe.toString();
        }
    }

    /**to get the file from uri to unzip*/
    private static File getFileFromInputStream(InputStream iStream){
        try {
            byte[] buffer = new byte[iStream.available()];
            int data = iStream.read(buffer);
            if(data == -1) iStream.close();
            File fToReturn = new File(getAbbFilesPath(),"default.abb");
            OutputStream out = new FileOutputStream(fToReturn);
            out.write(buffer);
            return fToReturn;
        } catch (IOException e) {return null;}
    }

    /**Call it to run unzip*/
    static String runUnzipping(final AbbMainActivity mActivity, File f){
        try {
            return new unZip(f, new unZip.AsyncResponse() {
                @Override
                public void processFinish(String output) {
                    List<eFormItem> newEfList = new ArrayList<>();
                    File[] listOfFiles = ABB_FILES_PATH.listFiles();
                    for(File everyFile: listOfFiles) {
                        //Condition to check eForm file extension
                        if (everyFile.getName().endsWith(".eqz")) {
                            // get the content and verify if there is a eForm info line
                            String everyName = everyFile.getName().replace(".eqz","");
                            eFormItem ef = getFormItem(everyName);
                            if(ef != null) newEfList.add(ef);
                        }
                    }
                    EfListFragment lFrag = (EfListFragment) AbbMainActivity.gSFManager
                            .findFragmentByTag("eFormList");
                    if(lFrag != null){
                        for (eFormItem ef : newEfList){
                            eFormItem readyForm = eFORM_ITEMS.get(ef.efId);
                            if(readyForm != null){
                                List<String> efLines = getContentByLines(readyForm.efName, false);
                                List<String> newEfLines= getContentByLines(ef.efName, false);
                                newEfLines.remove(0);
                                newEfLines.removeAll(efLines);
                                efLines.addAll(newEfLines);
                                writeEFormFile(readyForm.efName, efLines, false);
                            } else {
                                addFormItem(ef, -1, false);
                                int index = AbbContent.eFORM_ITEM_LIST.indexOf(ef);
                                lFrag.efListAdapter.notifyItemInserted(index);
                                lFrag.recyclerView.smoothScrollToPosition(index);
                                mActivity.showSnackBarMsg(mActivity.getString(R.string.get_form_msg,
                                        output), 0, android.R.string.ok, null);
                            }
                        }
                    }
                }
            }).execute().get();
        } catch (ExecutionException e) {
            return e.toString();
        } catch (InterruptedException e) {
            return e.toString();
        }
    }

    /**get content unzipping file with this async task*/
    public static class unZip extends AsyncTask<Void, Void, String> {

        private File fToUnzip;
        AsyncResponse delegate;

        unZip(File fToUnzip, AsyncResponse delegate){
            this.fToUnzip = fToUnzip;
            this.delegate = delegate;
        }

        public interface AsyncResponse {
            void processFinish(String output);
        }

        @Override
        protected String doInBackground(Void... voids) {
            try {
                ZipInputStream zin = new ZipInputStream(new FileInputStream(fToUnzip));
                ZipEntry ze;
                while ((ze = zin.getNextEntry()) != null){
                    String path = AbbContent.getAbbFilesPath() + File.separator + ze.getName();
                    FileOutputStream fOut = new FileOutputStream(path, false);
                    try{
                        for (int c = zin.read(); c != -1; c = zin.read()) fOut.write(c);
                        zin.closeEntry();
                    } finally {fOut.close();}
                } zin.close();
                return "ok";
            } catch (IOException ioe) {return ioe.toString();}
            catch (NullPointerException npe){return npe.toString();}
        }

        @Override
        protected void onPostExecute(String result) {
            delegate.processFinish(result);
        }
    }

    /** A class item representing a piece of content (eForm).*/
    static class eFormItem {

        final String efName;
        final long efId;
        final String efOwner;
        final String efType;
        final String[] efDetails;

        eFormItem(String efName, long efId, String efOwner, String efType, String[] efDetails) {
            this.efName = efName;
            this.efId = efId;
            this.efOwner = efOwner;
            this.efType = efType;
            this.efDetails = efDetails;
        }

        public String getString() {
            return  "<<eFormInfoLine>>" +
                    "<<Owner::"+ efOwner +">>" +
                    "<<Id::"+String.valueOf(efId)+">>" +
                    "<<Type::"+ efType +">>" +
                    "<<Date::"+ efDetails[0]+">>" +
                    "<<lmDate::"+ efDetails[1]+">>" +
                    "<<qCount::"+ efDetails[2]+">>" +
                    "<<Comments::"+ efDetails[3]+">>" +
                    "<<eFormState::"+ efDetails[4]+">>" +
                    "<<eFormInfoLine::>>";
        }
    }

    /** A class item representing a questions of a an eForm.*/
    static class QuestionItem {

        final long efId;
        final long qId;
        final int qType;
        final String q;
        final List<String> as;

        QuestionItem(long efId, long qId, int qType, String q, List<String> answers){
            this.efId = efId;
            this.qId = qId;
            this.qType = qType;
            this.q = q;
            this.as = answers;
        }

        public String getString() {
            String answers;
            if (as.size() == 0) answers = "";
            else {
                StringBuilder msaLine = new StringBuilder();
                for (int i = 0; i < as.size(); i++){
                    msaLine.append("<<msa::");
                    msaLine.append(as.get(i));
                    msaLine.append(">>");
                }
                answers = msaLine.toString();
            }
            return  "<<qLine>>" +
                    "<<efId::"+String.valueOf(efId)+">>" +
                    "<<qId::"+String.valueOf(qId)+">>" +
                    "<<qType::"+String.valueOf(qType)+">>" +
                    "<<q::"+q+">>" +
                    answers +
                    "<<qLine::>>";
        }
    }

    /** A class item representing an owner's answers of a question from a eForm*/
    static class AnswerItem {

        final long efId;
        final String aOwner;
        final String aDate;
        final LongSparseArray<String> as;

        AnswerItem(long efId, String aOwner, String aDate, LongSparseArray<String> as){
            this.efId = efId;
            this.aOwner = aOwner;
            this.aDate = aDate;
            this.as = as;
        }

        public String getString() {
            String qIdsAndAnswers;
            StringBuilder qIdAndAnswerLine = new StringBuilder();
            for (int i = 0; i < as.size(); i++){
                long qId = as.keyAt(i);
                qIdAndAnswerLine.append("<<qId::");
                qIdAndAnswerLine.append(String.valueOf(qId));
                qIdAndAnswerLine.append(">><<a::");
                qIdAndAnswerLine.append(as.get(qId));
                qIdAndAnswerLine.append(">>");
            }
            qIdsAndAnswers = qIdAndAnswerLine.toString();

            return  "<<aLine>>" +
                    "<<efId::"+String.valueOf(efId)+">>" +
                    "<<aOwner::"+aOwner+">>" +
                    "<<aDate::"+aDate+">>" +
                    qIdsAndAnswers +
                    "<<aLine::>>";
        }
    }
}
