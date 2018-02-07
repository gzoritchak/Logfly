/*
 * Copyright Gil THOMAS
 * This file forms an integral part of Logfly project
 * See the LICENSE file distributed with source code
 * for details of Logfly licence project
 */
package controller;

import Logfly.Main;
import dialogues.alertbox;
import geoutils.position;
import geoutils.reversegeocode;
import igc.pointIGC;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParsePosition;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Paint;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.util.converter.DoubleStringConverter;
import javafx.util.converter.IntegerStringConverter;
import leaflet.map_markers_coord;
import netscape.javascript.JSObject;
import org.xnap.commons.i18n.I18n;
import org.xnap.commons.i18n.I18nFactory;
import settings.configProg;
import systemio.mylogging;

/**
 *
 * @author gil
 */
public class SiteFormController {
    
    @FXML
    private Label lbNom;
    @FXML
    private TextField txNom;    
    @FXML
    private Label lbOrien;
    @FXML
    private TextField txOrien;
    @FXML
    private Label lbLocalite;
    @FXML
    private TextField txLocalite;
    @FXML
    private Label lbCP;
    @FXML
    private TextField txCP;       
    @FXML
    private Label lbPays;
    @FXML
    private RadioButton rdDeco;    
    @FXML
    private RadioButton rdAttero;    
    @FXML
    private TextField txPays;  
    @FXML
    private Label lbLat1;
    @FXML
    private Label lbLat2;    
    @FXML
    private Label lbLat3;    
    @FXML
    private TextField txLat;
    @FXML
    private TextField txDMLatDeg;  
    @FXML
    private TextField txDMLatMin; 
    @FXML
    private TextField txDMLatHem;    
    @FXML
    private TextField txDMSLatDeg;
    @FXML
    private TextField txDMSLatMin;
    @FXML
    private TextField txDMSLatSec;
    @FXML
    private TextField txDMSLatHem;
    @FXML
    private Label lbLong1;
    @FXML
    private Label lbLong2;
    @FXML
    private Label lbLong3;    
    @FXML
    private TextField txLong;
    @FXML
    private TextField txDMLongDeg;
    @FXML
    private TextField txDMLongMin;
    @FXML
    private TextField txDMLongMer;
    @FXML
    private TextField txDMSLongDeg;
    @FXML
    private TextField txDMSLongMin;
    @FXML
    private TextField txDMSLongSec;
    @FXML
    private TextField txDMSLongMer;
    @FXML
    private Button btRefreshMap;     
    @FXML
    private Label lbAlt;
    @FXML
    private TextField txAlt; 
    @FXML
    private Label lbAltUnit;
    @FXML
    private Button btUpdateMap;
    @FXML
    private Label lbComment;
    @FXML
    private TextArea txComment; 
    @FXML
    private Label lbPointeur;
    @FXML
    private Label lbGeoloc;
    @FXML
    private WebView mapViewer;    
    @FXML
    private Button btCancel;
    @FXML
    private Button btOk;    
    
    ToggleGroup rdGroup;
    
    // Reference to SiteViewController
    private SitesViewController siteController;
    
    private Stage dialogStage;    
    
    // Localization
    private I18n i18n; 
    
    // bridge between java code and javascript map
    private final Bridge pont = new Bridge();
    
    // Settings
    private configProg myConfig;
    private StringBuilder sbError;
    private int editMode;
    private String idSite;
    private String oldName;
    private double latDep;
    private double longDep;
    private String debLbGeoloc;
    private Paint colorBadValue = Paint.valueOf("FA6C04");
    private Paint colorGoodValue = Paint.valueOf("FFFFFF");
    private Pattern validDoubleText = Pattern.compile("-?((\\d*)|(\\d+\\.\\d*))");
    private Pattern validIntText = Pattern.compile("-?(\\d*)");
    private DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols();    
    private DecimalFormat df2;
    private DecimalFormat df3;
    private position displayPos;
    private boolean updateProgress;
    
    //START | SQLITE
    private static Statement stat;
    private PreparedStatement prep;
    //END | SQLITE  
    
    
    /**
     * A la place de conversion, on initialise une position globale  à la classe
     * Valeur change on change la propriété considérée dans position
     * on lance un actuchamps qui n'est qu'une lecture de la position
     * position est à modifier avec les set à revoir comme setLatDegres
     */
    @FXML
    private void initialize() {
        // We need to intialize i18n before TableView building
        // For this reason we put building code in iniTable() 
        // This procedure will be called after setMainApp()                 
        
        rdGroup = new ToggleGroup();    
        rdDeco.setToggleGroup(rdGroup);        
        rdAttero.setToggleGroup(rdGroup);  
        
        // ********  DD Fields ************
        // Listener are waiting for numeric values
        // BE CAREFUL to set numeric strings without spaces     
        TextFormatter<Double> fmTxLat = new TextFormatter<Double>(new DoubleStringConverter(), 0.0, 
            change -> {
                String newText = change.getControlNewText() ;
                if (validDoubleText.matcher(newText).matches()) {
                    return change ;
                } else return null ;
            });                
        fmTxLat.valueProperty().addListener((obs, oldValue, newValue) -> {            
                if (newValue < -90 || newValue > 90) {                      
                    txLat.setStyle("-fx-control-inner-background: #"+colorBadValue.toString().toString().substring(2));
                    txLat.requestFocus();                
                } else {
                    txLat.setStyle("-fx-control-inner-background: #"+colorGoodValue.toString().substring(2));  
                    displayPos.setLatitudeDd(newValue);
                    if (!updateProgress) updateFieldsPos();
                }            
        });    
        txLat.setTextFormatter(fmTxLat);
        
        TextFormatter<Double> fmTxLong = new TextFormatter<Double>(new DoubleStringConverter(), 0.0, 
            change -> {
                String newText = change.getControlNewText() ;
                if (validDoubleText.matcher(newText).matches()) {
                    return change ;
                } else return null ;
            });          
        fmTxLong.valueProperty().addListener((obs, oldValue, newValue) -> {            
                if (newValue < -180 || newValue > 180) {                      
                    txLong.setStyle("-fx-control-inner-background: #"+colorBadValue.toString().toString().substring(2));
                    txLong.requestFocus();                
                } else {
                    txLong.setStyle("-fx-control-inner-background: #"+colorGoodValue.toString().substring(2));  
                    displayPos.setLongitudeDd(newValue);
                    if (!updateProgress) updateFieldsPos();
                }            
        });                            
        txLong.setTextFormatter(fmTxLong);
        
        // ********  DMm fields
        // Listener are waiting for numeric values
        // BE CAREFUL to set numeric strings without spaces  
        TextFormatter<Integer> fmTxDMLatDeg = new TextFormatter<Integer>(new IntegerStringConverter(), 0, 
            change -> {
                String newText = change.getControlNewText() ;
                if (validIntText.matcher(newText).matches()) {
                    return change ;
                } else return null ;
            });             
        
        fmTxDMLatDeg.valueProperty().addListener((obs, oldValue, newValue) -> {     
                if (newValue < -90 || newValue > 90) {                      
                    txDMLatDeg.setStyle("-fx-control-inner-background: #"+colorBadValue.toString().toString().substring(2));
                    txDMLatDeg.requestFocus();                
                } else {
                    txDMLatDeg.setStyle("-fx-control-inner-background: #"+colorGoodValue.toString().substring(2));  
                    displayPos.setLatDegres(newValue);
                    if (!updateProgress) updateFieldsPos();
                }            
        });                
        txDMLatDeg.setTextFormatter(fmTxDMLatDeg);  
        
        TextFormatter<Double> fmDMLatMin = new TextFormatter<Double>(new DoubleStringConverter(), 0.0, 
            change -> {
                String newText = change.getControlNewText() ;
                if (validDoubleText.matcher(newText).matches()) {
                    return change ;
                } else return null ;
            });                
        fmDMLatMin.valueProperty().addListener((obs, oldValue, newValue) -> {            
                if (newValue < 0 || newValue > 60) {                      
                    txDMLatMin.setStyle("-fx-control-inner-background: #"+colorBadValue.toString().toString().substring(2));
                    txDMLatMin.requestFocus();                
                } else {
                    txDMLatMin.setStyle("-fx-control-inner-background: #"+colorGoodValue.toString().substring(2));  
                    displayPos.setLatMin_mm(newValue);
                    if (!updateProgress) {
                        updateFieldsPos();
                    }
                }            
        });        
        txDMLatMin.setTextFormatter(fmDMLatMin);

        txDMLatHem.textProperty().addListener((observable, oldValue, newValue) -> {                        
            if (!newValue.equals("N") && !newValue.equals("S")) {
                txDMLatHem.setStyle("-fx-control-inner-background: #"+colorBadValue.toString().toString().substring(2));
                txDMLatHem.requestFocus();    
            } else {
               txDMLatHem.setStyle("-fx-control-inner-background: #"+colorGoodValue.toString().substring(2));   
            }
        });        
        
        TextFormatter<Integer> fmTxDMLongDeg = new TextFormatter<Integer>(new IntegerStringConverter(), 0,
                change -> {
                    String newText = change.getControlNewText();
                    if (validIntText.matcher(newText).matches()) {
                        return change;
                    } else {
                        return null;
                    }
                });

        fmTxDMLongDeg.valueProperty().addListener((obs, oldValue, newValue) -> {            
            if (newValue < -180 || newValue > 180) {
                txDMLongDeg.setStyle("-fx-control-inner-background: #" + colorBadValue.toString().toString().substring(2));
                txDMLongDeg.requestFocus();
            } else {
                txDMLongDeg.setStyle("-fx-control-inner-background: #" + colorGoodValue.toString().substring(2));
                displayPos.setLongDegres(newValue);
                if (!updateProgress) updateFieldsPos();
            }
        });         
        txDMLongDeg.setTextFormatter(fmTxDMLongDeg);
        
        TextFormatter<Double> fmDMLongMin = new TextFormatter<Double>(new DoubleStringConverter(), 0.0, 
            change -> {                                                                                                                                                                                                                                                                                                                                                                                                                       
                String newText = change.getControlNewText() ;
                if (validDoubleText.matcher(newText).matches()) {
                    return change ;
                } else return null ;
            });                
        fmDMLongMin.valueProperty().addListener((obs, oldValue, newValue) -> {            
                if (newValue < 0 || newValue > 60) {                      
                    txDMLongMin.setStyle("-fx-control-inner-background: #"+colorBadValue.toString().toString().substring(2));
                    txDMLongMin.requestFocus();                
                } else {
                    txDMLongMin.setStyle("-fx-control-inner-background: #"+colorGoodValue.toString().substring(2));  
                    displayPos.setLongMin_mm(newValue);
                    if (!updateProgress) updateFieldsPos();
                }            
        });        
        txDMLongMin.setTextFormatter(fmDMLongMin);     
        
        txDMLongMer.textProperty().addListener((observable, oldValue, newValue) -> {                        
            if (!newValue.equals("W") && !newValue.equals("E")) {
                txDMLongMer.setStyle("-fx-control-inner-background: #"+colorBadValue.toString().toString().substring(2));
                txDMLongMer.requestFocus();    
            } else {
               txDMLongMer.setStyle("-fx-control-inner-background: #"+colorGoodValue.toString().substring(2));   
            }
        });          
                
        //********** DMS Fields ****************
        TextFormatter<Integer> fmTxDMSLatDeg = new TextFormatter<Integer>(new IntegerStringConverter(), 0, 
            change -> {
                String newText = change.getControlNewText() ;
                if (validIntText.matcher(newText).matches()) {
                    return change ;
                } else return null ;
            });             
        
        fmTxDMSLatDeg.valueProperty().addListener((obs, oldValue, newValue) -> {     
                System.out.println("listener fmTxDMSLatDeg ");
                if (newValue < -90 || newValue > 90) {                      
                    txDMSLatDeg.setStyle("-fx-control-inner-background: #"+colorBadValue.toString().toString().substring(2));
                    txDMSLatDeg.requestFocus();                
                } else {
                    txDMSLatDeg.setStyle("-fx-control-inner-background: #"+colorGoodValue.toString().substring(2));  
                    displayPos.setLatDegres(newValue);
                    if (!updateProgress) updateFieldsPos();
                }            
        });                
        txDMSLatDeg.setTextFormatter(fmTxDMSLatDeg);         

        TextFormatter<Integer> fmTxDMSLatMin = new TextFormatter<Integer>(new IntegerStringConverter(), 0, 
            change -> {
                String newText = change.getControlNewText() ;
                if (validIntText.matcher(newText).matches()) {
                    return change ;
                } else return null ;
            });                     
        fmTxDMSLatMin.valueProperty().addListener((obs, oldValue, newValue) -> {                 
                if (newValue < 0 || newValue > 60) {                      
                    txDMSLatMin.setStyle("-fx-control-inner-background: #"+colorBadValue.toString().toString().substring(2));
                    txDMSLatMin.requestFocus();                
                } else {
                    txDMSLatMin.setStyle("-fx-control-inner-background: #"+colorGoodValue.toString().substring(2));  
                    displayPos.setLatMin_ms(newValue);
                    if (!updateProgress) updateFieldsPos();
                }            
        });                
        txDMSLatMin.setTextFormatter(fmTxDMSLatMin);    
        
        TextFormatter<Double> fmDMSLatSec = new TextFormatter<Double>(new DoubleStringConverter(), 0.0, 
            change -> {
                String newText = change.getControlNewText() ;
                if (validDoubleText.matcher(newText).matches()) {
                    return change ;
                } else return null ;
            });                
        fmDMSLatSec.valueProperty().addListener((obs, oldValue, newValue) -> {            
                if (newValue < 0 || newValue > 60) {                      
                    txDMSLatSec.setStyle("-fx-control-inner-background: #"+colorBadValue.toString().toString().substring(2));
                    txDMSLatSec.requestFocus();                
                } else {
                    txDMSLatSec.setStyle("-fx-control-inner-background: #"+colorGoodValue.toString().substring(2));  
                    displayPos.setLatSec_ms(newValue);                    
                    if (!updateProgress) {
                        updateFieldsPos();
                    }
                }            
        });        
        txDMSLatSec.setTextFormatter(fmDMSLatSec);

        txDMSLatHem.textProperty().addListener((observable, oldValue, newValue) -> {                        
            if (!newValue.equals("N") && !newValue.equals("S")) {
                txDMSLatHem.setStyle("-fx-control-inner-background: #"+colorBadValue.toString().toString().substring(2));
                txDMSLatHem.requestFocus();    
            } else {
               txDMSLatHem.setStyle("-fx-control-inner-background: #"+colorGoodValue.toString().substring(2));   
            }
        });           
        
        TextFormatter<Integer> fmTxDMSLongDeg = new TextFormatter<Integer>(new IntegerStringConverter(), 0, 
            change -> {
                String newText = change.getControlNewText() ;
                if (validIntText.matcher(newText).matches()) {
                    return change ;
                } else return null ;
            });                     
        fmTxDMSLongDeg.valueProperty().addListener((obs, oldValue, newValue) -> {    
                if (newValue < -180 || newValue > 180) {                      
                    txDMSLongDeg.setStyle("-fx-control-inner-background: #"+colorBadValue.toString().toString().substring(2));
                    txDMSLongDeg.requestFocus();                
                } else {
                    txDMSLongDeg.setStyle("-fx-control-inner-background: #"+colorGoodValue.toString().substring(2));  
                    displayPos.setLongDegres(newValue);
                    if (!updateProgress) updateFieldsPos();
                }            
        });                
        txDMSLongDeg.setTextFormatter(fmTxDMSLongDeg); 

        TextFormatter<Integer> fmTxDMSLongMin = new TextFormatter<Integer>(new IntegerStringConverter(), 0, 
            change -> {
                String newText = change.getControlNewText() ;
                if (validIntText.matcher(newText).matches()) {
                    return change ;
                } else return null ;
            });             
        
        fmTxDMSLongMin.valueProperty().addListener((obs, oldValue, newValue) -> {            
                if (newValue < 0 || newValue > 60) {                      
                    txDMSLongMin.setStyle("-fx-control-inner-background: #"+colorBadValue.toString().toString().substring(2));
                    txDMSLongMin.requestFocus();                
                } else {
                    txDMSLongMin.setStyle("-fx-control-inner-background: #"+colorGoodValue.toString().substring(2));  
                    displayPos.setLongMin_ms(newValue);
                    if (!updateProgress) updateFieldsPos();
                }            
        });                
        txDMSLongMin.setTextFormatter(fmTxDMSLongMin); 
    
        TextFormatter<Double> fmTxDMSLongSec = new TextFormatter<Double>(new DoubleStringConverter(), 0.0, 
            change -> {
                String newText = change.getControlNewText() ;
                if (validDoubleText.matcher(newText).matches()) {
                    return change ;
                } else return null ;
            });                
        fmTxDMSLongSec.valueProperty().addListener((obs, oldValue, newValue) -> {            
                if (newValue < 0 || newValue > 60) {                      
                    txDMSLongSec.setStyle("-fx-control-inner-background: #"+colorBadValue.toString().toString().substring(2));
                    txDMSLongSec.requestFocus();                
                } else {
                    txDMSLongSec.setStyle("-fx-control-inner-background: #"+colorGoodValue.toString().substring(2));  
                    displayPos.setLongSec_ms(newValue);                    
                    if (!updateProgress) {
                        updateFieldsPos();
                    }
                }            
        });        
        txDMSLongSec.setTextFormatter(fmTxDMSLongSec);        

        txDMSLongMer.textProperty().addListener((observable, oldValue, newValue) -> {                        
            if (!newValue.equals("W") && !newValue.equals("E")) {
                txDMSLongMer.setStyle("-fx-control-inner-background: #"+colorBadValue.toString().toString().substring(2));
                txDMSLongMer.requestFocus();    
            } else {
               txDMSLongMer.setStyle("-fx-control-inner-background: #"+colorGoodValue.toString().substring(2));   
            }
        });               
    }  
     
    /**
     * Define Edit conditions
     * ModeEdit = 0 Modification d'une fiche
     * ModeEdit = 1 Création d'une fiche depuis la liste des sites
     * ModeEdit = 2 Modification d'une fiche dynamiquement depuis le carnet.  ' Genre Site Noxx à renommer
     * ModeEdit = 3 Création d'une fiche dynamiquement depuis le carnet avec les coord déco  '  Option Site différent
     * @param mainConfig
     * @param pIdSite
     * @param modeEdit 
     */
    public void setEditForm(configProg mainConfig, String pIdSite, int modeEdit) {
        this.myConfig = mainConfig;
        this.idSite = pIdSite;
        this.editMode = modeEdit;
        i18n = I18nFactory.getI18n("","lang/Messages",SiteFormController.class.getClass().getClassLoader(),myConfig.getLocale(),0);
        winTraduction();
        iniForm();
    }
    
    private void iniForm() {
            
        ResultSet rs = null;
        Statement stmt = null;
        
        String sReq = "SELECT * FROM Site WHERE S_ID = "+idSite;
        try {        
            stmt = myConfig.getDbConn().createStatement();
            rs =  stmt.executeQuery(sReq);
            if (rs != null)  { 
                txNom.setText(rs.getString(2));
                oldName = rs.getString(2);
                txLocalite.setText(rs.getString(3));
                txCP.setText(rs.getString(4));
                txPays.setText(rs.getString(5));
                txOrien.setText(rs.getString(7));
                txAlt.setText(rs.getString(8));
                txComment.setText(rs.getString(11));
                switch (rs.getString(6)) {
                    case "A":
                        rdAttero.setSelected(true);
                        break;
                    case "D":
                        rdDeco.setSelected(true);
                        break;
                }
                // First release stored a date like YYYY-MM-dd HH:MM:SS
                // We avoid a parsing error
                String sDate = rs.getString(12);
                if (sDate.length() > 10) sDate = sDate.substring(0,10);
                setUpdateDate(sDate);
                
                displayPos = new position();
                latDep = rs.getDouble(9);
                longDep = rs.getDouble(10);
                displayPos.setLatitudeDd(latDep);
                displayPos.setLongitudeDd(longDep);
                System.out.println("Display lat "+displayPos.getLatitude()+"  long "+displayPos.getLongitude());
                updateFieldsPos();
                
                
                iniMap(latDep,longDep);
               // debugMap();
            }
        } catch ( Exception e ) {
            alertbox aError = new alertbox(myConfig.getLocale());
            aError.alertError(e.getClass().getName() + ": " + e.getMessage());  
            sbError = new StringBuilder(this.getClass().getName()+"."+Thread.currentThread().getStackTrace()[1].getMethodName());
            sbError.append("\r\n").append(e.getMessage());
            mylogging.log(Level.SEVERE, sbError.toString());            
        } finally {
            try{
                rs.close(); 
                stmt.close();
            } catch(Exception e) { } 
        }       
    }
    
    /**
     * Numeric fields have listener wich are waiting for numeric values
     * BE CAREFUL -> we must set string values not formatted strings
     *     String.Value OK
     *     String.format("%3d", xx )  -> BAD
     * Formatted strings include spaces. 
     * This spaces trigger a silencious exception
     * three days for fix the mystake...
     */
    private void updateFieldsPos() {
        decimalFormatSymbols.setDecimalSeparator('.'); 
        // fields update       
        df2 = new DecimalFormat("#0.0000", decimalFormatSymbols);
        df3 = new DecimalFormat("##0.0000", decimalFormatSymbols);
        updateProgress = true;
        
        txLat.setText(df2.format(displayPos.getLatitude()));      
        txLong.setText(df3.format(displayPos.getLongitude()));

        txDMLatDeg.setText(String.valueOf(displayPos.getLatDegres()));            
        txDMLatMin.setText(df2.format(displayPos.getLatMin_mm())); 
        txDMLatHem.setText(displayPos.getHemisphere());         
        txDMLongDeg.setText(String.valueOf(displayPos.getLongDegres()));        
        txDMLongMin.setText(df2.format(displayPos.getLongMin_mm()));
        txDMLongMer.setText(displayPos.getMeridien());
                
        txDMSLatDeg.setText(String.valueOf(displayPos.getLatDegres()));
        txDMSLatMin.setText(String.valueOf(displayPos.getLatMin_ms()));
        txDMSLatSec.setText(df2.format(displayPos.getLatSec_ms())); 
        txDMSLatHem.setText(displayPos.getHemisphere());
        txDMSLongDeg.setText(String.valueOf(displayPos.getLongDegres()));
        txDMSLongMin.setText(String.valueOf(displayPos.getLongMin_ms()));
        txDMSLongSec.setText(df2.format(displayPos.getLongSec_ms()));
        txDMSLongMer.setText(displayPos.getMeridien());
   
        updateProgress = false;
    }
    /**
     * date format must be checked, in very old versions format is dd/mm/yy
     * @param dateStr 
     */
    private void setUpdateDate(String dateStr) {      
        Pattern dayDate = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
        Matcher matchDay = dayDate.matcher(dateStr);        
        try {        
            if(matchDay.find()) {          
                // Direct parsing is possible because we have default ISO_LOCAL_DATE format
                LocalDate localDate = LocalDate.parse(dateStr);
                DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT);
                dialogStage.setTitle(i18n.tr("Date de mise à jour : "+localDate.format(formatter)));                
            } 
        } catch (Exception e) {
            sbError = new StringBuilder(this.getClass().getName()+"."+Thread.currentThread().getStackTrace()[1].getMethodName());
            sbError.append("\r\n").append(e.getMessage());
            mylogging.log(Level.SEVERE, sbError.toString()); 
        }        
    }    
    
    private void debugMap() {
        try {
            File f = new File("site3.html");
            String sHTML = new String(Files.readAllBytes(Paths.get(f.getAbsolutePath())));
            mapViewer.getEngine().loadContent(sHTML,"text/html");   
            // On passe maintenant l'objet java au formulaire. 
            final JSObject jsobj = (JSObject) mapViewer.getEngine().executeScript("window"); 
            jsobj.setMember("java", pont); 
        } catch (Exception e) {
            e.printStackTrace();
        }
  
        
    }
        
    private void findTown() {
        String finalSiteDeco;
        
        reversegeocode rechTown = new reversegeocode();
        // Unlike map_visu, we don't need to a decimalFormat
        // lat and long are always with a point as decimal separator
        
        // Best results with a low precision in coordinates
        String sCoord = txLat.getText().substring(0, 6)+","+txLong.getText().substring(0,6);
        String siteDeco = rechTown.googleGeocode(sCoord, true);
        // Very difficult to parse Google result
        // it's never the same order 
        // result can be 06460 Caussols, France
        // or 06460 Caussols, France
        if (siteDeco.length() > 10) 
            lbGeoloc.setText(debLbGeoloc+" "+siteDeco);
        else
            lbGeoloc.setText("");
    }
    
    private void iniMap(double dLatitude, double dLongitude) {
        
        pointIGC pPoint1 = new pointIGC();             
        if (dLatitude > 90 || dLatitude < -90) dLatitude = 0;
        pPoint1.setLatitude(dLatitude);        
        if (dLongitude > 180 || dLongitude < -180) dLongitude = 0;
        pPoint1.setLongitude(dLongitude);
        pPoint1.setAltiGPS(Integer.parseInt(txAlt.getText()));
        map_markers_coord myMap = new map_markers_coord(i18n, myConfig.getIdxMap(), pPoint1); 
        if (myMap.isMap_OK()) {   
            // with first maps, we made this            
            // mapViewer.getEngine().load("about:blank");                     
            // to delete cache for navigate back
            // problem : with this instruction, the bridge (jsobject) does not work
            mapViewer.getEngine().loadContent(myMap.getMap_HTML());   
            // java object is sent to webwiew
            final JSObject jsobj = (JSObject) mapViewer.getEngine().executeScript("window"); 
            jsobj.setMember("java", pont); 
        }
        
    }

    private void updateDb() {
        
        String siteType;
        StringBuilder sbReq = new StringBuilder();   
        String Quote = "'";
        boolean badCoord;
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("YYYY-MM-dd");
        
        if (rdDeco.isSelected()) 
            siteType = "A";
        else
            if (rdDeco.isSelected())
                siteType = "D";
            else
                siteType = "''";
        
        if (txLat.getText().trim().equals("")) 
            badCoord = true;
        else if (txLong.getText().trim().equals(""))
            badCoord = true;
        else
            badCoord = false;
  
        // editMode  = 0 modification of a site form
        // editMode = 1 form creation from site list
        // editMode = 2 modifying a form dynamically from the logbook like Site Noxx à renommer
        // editMode = 3 Creating a form dynamically from the logbook with take off coordinates (option different site)
        
        if (!badCoord) {
            String sNom = txNom.getText();
            String sLocalite = txLocalite.getText();
            String sPays = txPays.getText();
            String sComment = txComment.getText();
            PreparedStatement pstmt = null;
            String sReq = "";
            try {
                if (editMode == 0 || editMode == 2) {
                    sReq = "UPDATE Site SET S_Nom=?, S_Localite=?, S_CP=?, S_Pays=?, S_Type=?, S_Orientation=?, S_Alti=?, S_Latitude=?, S_Longitude=?, S_Commentaire=?, S_Maj=? WHERE S_ID =?";  
                } else {
                    sReq = "INSERT INTO Site (S_Nom,S_Localite,S_CP,S_Pays,S_Type,S_Orientation,S_Alti,S_Latitude,S_Longitude,S_Commentaire,S_Maj) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
                }
                pstmt = myConfig.getDbConn().prepareStatement(sReq);
                pstmt.setString(1,sNom); 
                pstmt.setString(2,sLocalite);
                pstmt.setString(3,txCP.getText());
                pstmt.setString(4,sPays);
                pstmt.setString(5,siteType);
                pstmt.setString(6,txOrien.getText());
                pstmt.setString(7,txAlt.getText());
                pstmt.setString(8,txLat.getText());
                pstmt.setString(9,txLong.getText());
                pstmt.setString(10,sComment);
                pstmt.setString(11,today.format(formatter));
                pstmt.setInt(12, Integer.valueOf(idSite));
                pstmt.executeUpdate(); 
            } catch (Exception e) {
                alertbox aError = new alertbox(myConfig.getLocale());
                aError.alertError(e.getClass().getName() + ": " + e.getMessage());  
                sbError = new StringBuilder(this.getClass().getName()+"."+Thread.currentThread().getStackTrace()[1].getMethodName());
                sbError.append("\r\n").append(e.getMessage());
                mylogging.log(Level.SEVERE, sbError.toString()); 
            }                       
        }
    }
    
    @FXML    
    private void handleUpdate() {
        iniMap(Double.parseDouble(txLat.getText()), Double.parseDouble(txLong.getText()));
    }
    
    /**
     * valid changes
     */
    @FXML
    private void handleOk() {
        updateDb();
        dialogStage.close();
    }      
    
    /**
     * Discard changes
     */
    @FXML
    private void handleCancel() {
        dialogStage.close();
    }    
    
    /**
     * Sets the stage of this dialog.
     *
     * @param dialogStage
     */
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }    
    
    /**
     * Initialize communication brdige with SitesViewController 
     * @param pSiteController 
     */
    public void setSiteBridge(SitesViewController pSiteController) {
        this.siteController = pSiteController;        
    }    

    /**
     * Translate labels of the window
     */
    private void winTraduction() {
        
        Tooltip caracToolTip = new Tooltip();
        caracToolTip.setStyle(myConfig.getDecoToolTip());
        caracToolTip.setText(i18n.tr("Caractères acceptés : - . 0-9"));
        Tooltip latToolTip = new Tooltip();
        latToolTip.setStyle(myConfig.getDecoToolTip());
        latToolTip.setText(i18n.tr("Caractères acceptés : N S"));
        Tooltip longToolTip = new Tooltip();
        longToolTip.setStyle(myConfig.getDecoToolTip());
        longToolTip.setText(i18n.tr("Caractères acceptés : W E"));        
        txLat.setTooltip(caracToolTip);
        txDMLatDeg.setTooltip(caracToolTip);
        txDMLatMin.setTooltip(caracToolTip); 
        txDMLatHem.setTooltip(latToolTip);  
        txDMSLatDeg.setTooltip(caracToolTip);
        txDMSLatMin.setTooltip(caracToolTip);
        txDMSLatSec.setTooltip(caracToolTip);
        txDMSLatHem.setTooltip(latToolTip);
       // txLong.setTooltip(caracToolTip);
        txDMLongDeg.setTooltip(caracToolTip);
        txDMLongMin.setTooltip(caracToolTip);
        txDMLongMer.setTooltip(longToolTip);
        txDMSLongDeg.setTooltip(caracToolTip);
        txDMSLongMin.setTooltip(caracToolTip);
        txDMSLongSec.setTooltip(caracToolTip);
        txDMSLongMer.setTooltip(longToolTip);
        lbNom.setText(i18n.tr("Nom"));
        lbOrien.setText(i18n.tr("Orientation"));   
        lbLocalite.setText(i18n.tr("Localité"));   
        lbCP.setText(i18n.tr("Code Postal"));   
        lbPays.setText(i18n.tr("Pays"));   
        rdDeco.setText(i18n.tr("Décollage"));     
        rdAttero.setText(i18n.tr("Atterissage"));       
        lbLat1.setText(i18n.tr("Latitude"));   
        lbLat2.setText(i18n.tr("Latitude"));   
        lbLat3.setText(i18n.tr("Latitude"));   
        lbLong1.setText(i18n.tr("Longitude"));   
        lbLong2.setText(i18n.tr("Longitude"));   
        lbLong3.setText(i18n.tr("Longitude"));   
        lbAlt.setText(i18n.tr("Alti déco"));   
        lbComment.setText(i18n.tr("Commentaire"));   
        btCancel.setText(i18n.tr("Annuler"));  
        btOk.setText(i18n.tr("Valider"));       
        debLbGeoloc = i18n.tr("Géolocalisation");
        lbPointeur.setText(i18n.tr("Déplacer le marqueur pour modifier les coordonnées"));
    }    
    
    public class Bridge { 
  
        public void setLatitude(String value) { 
            txLat.setText(value);            
        } 
  
        public void setLongitude(String value) { 
            txLong.setText(value);
        } 
        
        public void setAltitude(String value) { 
            txAlt.setText(value);
        }        
    }    
    
}
 