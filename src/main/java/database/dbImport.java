/*
 * Copyright Gil THOMAS
 * This file forms an integral part of Logfly project
 * See the LICENSE file distributed with source code
 * for details of Logfly licence project
 */
package database;

import controller.SitesViewController;
import dialogues.alertbox;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Level;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import littlewins.winTrackFile;
import org.controlsfx.dialog.ProgressDialog;
import org.xnap.commons.i18n.I18n;
import settings.configProg;
import systemio.mylogging;

/**
 *
 * @author gil
 */
public class dbImport {
    
 // Localization
    private I18n i18n;
    

    // Settings
    private configProg myConfig;
    private SitesViewController sitesController; 
    private int addNb = 0;
    private int addSitesOK = 0;
    private int addSitesBad = 0;
    private StringBuilder sbDoublons;
    private StringBuilder sbRejected;    
    private StringBuilder sbError;
    String RC = "\n";
    
    public dbImport(configProg pConfig, I18n pI18n, SitesViewController pSitesC)  {
        myConfig = pConfig;      
        this.i18n = pI18n;
        this.sitesController = pSitesC;
    } 
           
    public void importCsv(File pFile) {
        csvImport(pFile);
    }
    
    private void csvCloseImport() {
        boolean writeReject = false;
        
        alertbox aInfo = new alertbox(myConfig.getLocale());
        StringBuilder sbMsg = new StringBuilder();
        sbMsg.append(String.valueOf(addSitesOK)).append(" ").append(i18n.tr("imported sites")).append(" / ").append(String.valueOf(addNb)).append(" ").append("lignes");
        if (addSitesBad > 0) sbMsg.append(RC).append(String.valueOf(addSitesBad)).append(" ").append(i18n.tr("rejected sites"));
        aInfo.alertInfo(sbMsg.toString());
        if (addSitesOK < addNb) {    
            if (myConfig.isValidConfig()) {
                try {
                    File fileReject = new File(myConfig.getPathW()+"rejectedsites.csv");
                    FileOutputStream fileStream = new FileOutputStream(fileReject);
                    Charset ENCODING = StandardCharsets.ISO_8859_1;
                    OutputStreamWriter writer = new OutputStreamWriter(fileStream, ENCODING);   
                    writer.write(sbRejected.toString());                            
                    writer.close();
                    writeReject = true;
                } catch (Exception e) {
                    sbError = new StringBuilder(this.getClass().getName()+"."+Thread.currentThread().getStackTrace()[1].getMethodName());
                    sbError.append("\r\n").append(e.toString());
                    mylogging.log(Level.SEVERE, sbError.toString());                
                }
            }
            if (writeReject) {
                String msg = i18n.tr("List saved in")+" <rejectedsites.csv>"+RC+RC;
                sbDoublons.insert(0,msg);
                winTrackFile displayDoub = new winTrackFile(sbDoublons.toString()); 
            } 
            if (sitesController != null) {
                sitesController.pushAll();
            }
        }        
    }
    
    private Double parseDouble(String s) {
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return Double.NaN;
        } 
    }    
    
    private void csvImportFile(File importFile)  {
        
        addNb = 0;
        addSitesOK = 0;
        addSitesBad = 0;
        sbDoublons = new StringBuilder();
        sbRejected = new StringBuilder();
        
        try {
            Charset ENCODING = StandardCharsets.ISO_8859_1;
            Path path = Paths.get(importFile.getAbsolutePath());
            List<String> lines = Files.readAllLines(path, ENCODING);
            for (String oneLine : lines) {
                addNb++;
                String[] partLine = oneLine.split(";");
                if (partLine.length > 3 && !partLine[0].equals("POINT_ID")) {
                   dbSearch rechSite = new dbSearch(myConfig);
                   double dLat = parseDouble(partLine[2]);
                   double dLong = parseDouble(partLine[3]);
                   if (!Double.isNaN(dLat) && !Double.isNaN(dLong)) {
                        String siteExist = rechSite.existeSite(Double.parseDouble(partLine[2]), Double.parseDouble(partLine[3]));
                        if (siteExist == null) {
                            dbAdd addSite = new dbAdd(myConfig, i18n);
                            boolean addRes = addSite.importSite(partLine);
                            if (addRes) {
                                addSitesOK++;                                                                        
                            } else {
                                addSitesBad++;
                                System.out.println("bad : "+partLine[1]+" "+partLine[2]+" "+partLine[3]);
                            }
                         } else {
                            sbDoublons.append(partLine[1]).append(" ").append(siteExist).append(RC);   
                            sbRejected.append(oneLine).append(RC);
                        }
                   }
                }
            } 
        } catch (Exception e) {
            sbError = new StringBuilder(this.getClass().getName()+"."+Thread.currentThread().getStackTrace()[1].getMethodName());
            sbError.append("\r\n").append(e.toString());
            mylogging.log(Level.SEVERE, sbError.toString());
        }                    
    }    
    
    private void csvImport(File pFile) {
        
        Task<Object> worker = new Task<Object>() {
            @Override
            protected Object call() throws Exception {
                csvImportFile(pFile);
                return null ;
            }

        };
        worker.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent t) {
                csvCloseImport();
            }
        }); 

        ProgressDialog dlg = new ProgressDialog(worker);
        dlg.setHeaderText(i18n.tr("Csv import"));
        dlg.setTitle("");
        Thread th = new Thread(worker);
        th.setDaemon(true);
        th.start();                                  
    }    
        
}
