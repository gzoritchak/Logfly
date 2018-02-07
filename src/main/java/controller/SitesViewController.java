/*
 * Copyright Gil THOMAS
 * This file forms an integral part of Logfly project
 * See the LICENSE file distributed with source code
 * for details of Logfly licence project
 */
package controller;

import Logfly.Main;
import dialogues.alertbox;
import dialogues.dialogbox;
import igc.pointIGC;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.logging.Level;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioButton;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import leaflet.map_markers;
import model.Sitemodel;
import org.xnap.commons.i18n.I18n;
import org.xnap.commons.i18n.I18nFactory;
import settings.configProg;
import systemio.mylogging;

/**
 *
 * @author gil
 */
public class SitesViewController {
    
    @FXML
    private TableView<Sitemodel> tableSites;   
    @FXML
    private TableColumn<Sitemodel, String> nomCol;
    @FXML
    private TableColumn<Sitemodel, String> villeCol;
    @FXML
    private TableColumn<Sitemodel, String> cpCol;
    @FXML
    private TableColumn<Sitemodel, String> altCol;        
    @FXML
    private TableColumn<Sitemodel, String> orientCol;   
    @FXML
    private TextField txtSearch;    
    @FXML
    private RadioButton rdAll;    
    @FXML
    private RadioButton rdDeco; 
    @FXML
    private RadioButton rdAttero;
    @FXML
    private RadioButton rdNondef;    
    @FXML
    private ImageView top_Menu;
    @FXML
    private WebView mapViewer;    

    ToggleGroup rdGroup;
    
    // Reference to the main application.
    private Main mainApp;
    
    // Localization
    private I18n i18n; 
    
    // Settings
    configProg myConfig;
    StringBuilder sbError;
    
    //START | SQLITE
    private static Statement stat;
    private PreparedStatement prep;
    //END | SQLITE    
    
    private ObservableList <Sitemodel> dataSites; 
    private FilteredList<Sitemodel> filteredData;
    private SortedList<Sitemodel> sortedData;
    
    @FXML
    private void initialize() {
        // We need to intialize i18n before TableView building
        // For this reason we put building code in iniTable() 
        // This procedure will be called after setMainApp()   
        rdGroup = new ToggleGroup();    
        rdAll.setToggleGroup(rdGroup);
        rdAll.setSelected(true);    
        rdDeco.setToggleGroup(rdGroup);
        rdAttero.setToggleGroup(rdGroup);
        rdNondef.setToggleGroup(rdGroup);        
    }    
    
    /**
     * Initialization of the TableView
     * Fill the table with data from db
     */
    private void iniTable() {
        
        dataSites = FXCollections.observableArrayList();    
        
        tableSites.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        
        // filter process read on http://code.makery.ch/blog/javafx-8-tableview-sorting-filtering/
        // wrap the ObservableList in a FilteredList (initially display all data).
        filteredData = new FilteredList<>(dataSites, p -> true);
        
        // set the filter Predicate whenever the filter changes.
        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(site -> {
                // If filter text is empty, display all persons.
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }

                // Compare first name and last name of every person with filter text.
                String lowerCaseFilter = newValue.toLowerCase();

                if (site.getNom().toLowerCase().contains(lowerCaseFilter)) {
                    return true; // Filter matches site name
                } 
                else if (site.getVille() != null && !site.getVille().equals("")) {
                    if (site.getVille().toLowerCase().contains(lowerCaseFilter)) 
                        return true; // Filter matches site locality
                }
                return false; // Does not match.
            });
        });        
       
        // wrap the FilteredList in a SortedList. 
        sortedData = new SortedList<>(filteredData);
        
        // bind the SortedList comparator to the TableView comparator.
        sortedData.comparatorProperty().bind(tableSites.comparatorProperty());
                       
        nomCol.setCellValueFactory(new PropertyValueFactory<Sitemodel, String>("nom"));
        villeCol.setCellValueFactory(new PropertyValueFactory<Sitemodel, String>("ville"));
        cpCol.setCellValueFactory(new PropertyValueFactory<Sitemodel, String>("cp"));        
        altCol.setCellValueFactory(new PropertyValueFactory<Sitemodel, String>("alt"));     
        orientCol.setCellValueFactory(new PropertyValueFactory<Sitemodel, String>("orient"));
        
        // Listener for line changes and  display relevant details
        tableSites.getSelectionModel().selectedItemProperty().addListener(
        (observable, oldValue, newValue) -> showSiteMap((Sitemodel) newValue));          
        
        fillTable("SELECT S_ID, S_Nom, S_Localite, S_CP,S_Alti, S_Orientation,S_Type FROM Site ORDER BY S_Nom");
    }    
    
    /**
     * Fill table with filter or not in SQL request
     * @param sReq 
     */
    private void fillTable(String sReq) {
        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = myConfig.getDbConn().createStatement();
            rs = stmt.executeQuery(sReq);
            if (rs != null)  { 
                tableSites.getItems().clear();  
                while (rs.next()) {
                    Sitemodel si = new Sitemodel();  
                    si.setIdSite(rs.getString("S_ID"));
                    si.setNom(rs.getString("S_Nom"));
                    si.setVille(rs.getString("S_Localite"));
                    si.setCp(rs.getString("S_CP"));
                    si.setAlt(rs.getString("S_Alti"));
                    si.setOrient(rs.getString("S_Orientation"));  
                    si.setType(rs.getString("S_Type"));                     
                    dataSites.add(si);                
                }    
                tableSites.setItems(sortedData);
                if (tableSites.getItems().size() > 0) {
                    tableSites.getSelectionModel().select(0);                    
                }                
            }
            
        } catch ( Exception e ) {
            sbError = new StringBuilder(this.getClass().getName()+"."+Thread.currentThread().getStackTrace()[1].getMethodName());
            sbError.append("\r\n").append(e.toString());
            mylogging.log(Level.SEVERE, sbError.toString());   
        } finally {
            try{
                rs.close(); 
                stmt.close();
            } catch(Exception e) { } 
        }                 
    }
    
    /**
     * Display details of a flight
     * 
     * curSite value is tested to solve a curious problem
     * First filling of table without problem showSiteMap called once
     * Second filling, showSiteMap is called with null currSite and is called a second time with correct currSite
     * @param currSite 
     */
    private void showSiteMap(Sitemodel currSite) {
        if (currSite != null) {
            Statement stmt = null;
            ResultSet rs = null; 

            String sReq = "SELECT S_Nom, S_Alti, S_Latitude,S_Longitude, S_Commentaire FROM Site WHERE S_ID ="+currSite.getIdSite();
            try {
                stmt = myConfig.getDbConn().createStatement();
                rs =  stmt.executeQuery(sReq);
                if (rs != null)  { 
                    pointIGC pPoint1 = new pointIGC();
                    double dLatitude = Double.parseDouble(rs.getString("S_Latitude"));
                    if (dLatitude > 90 || dLatitude < -90) dLatitude = 0;
                    pPoint1.setLatitude(dLatitude);
                    double dLongitude = Double.parseDouble(rs.getString("S_Longitude"));
                    if (dLongitude > 180 || dLongitude < -180) dLongitude = 0;
                    pPoint1.setLongitude(dLongitude);
                    pPoint1.setAltiGPS(Integer.parseInt(rs.getString("S_Alti")));
                    StringBuilder sbComment = new StringBuilder();
                    sbComment.append(rs.getString("S_Nom")).append("<BR>");
                    sbComment.append(i18n.tr("Altitude")).append(" : ").append(String.valueOf(pPoint1.AltiGPS)).append(" m" );
                    pPoint1.Comment = sbComment.toString();   

                    map_markers mapSite = new map_markers(i18n, myConfig.getIdxMap());
                    mapSite.getPointsList().add(pPoint1);
                    mapSite.setStrComment(rs.getString("S_Commentaire"));
                    if (mapSite.genMap() == 0) {
                        /** ----- Debut Debug --------*/ 
                        String sDebug = mapSite.getMap_HTML();
                        final Clipboard clipboard = Clipboard.getSystemClipboard();
                        final ClipboardContent content = new ClipboardContent();
                        content.putString(mapSite.getMap_HTML());            
                        clipboard.setContent(content);
                        /** ----- Fin Debug --------*/                     
                        // Delete cache for navigate back
                        mapViewer.getEngine().load("about:blank");                     
                        mapViewer.getEngine().loadContent(mapSite.getMap_HTML());  
                    }                  
                }            
            } catch ( Exception e ) {
                alertbox aError = new alertbox(myConfig.getLocale());
                aError.alertError(e.getClass().getName() + ": " + e.getMessage());   
                sbError = new StringBuilder(this.getClass().getName()+"."+Thread.currentThread().getStackTrace()[1].getMethodName());
                sbError.append("\r\n").append(e.toString());
                mylogging.log(Level.SEVERE, sbError.toString());             
            } finally {
                try{
                    rs.close(); 
                    stmt.close();
                } catch(Exception e) { } 
            } 
        }
    }    

    @FXML
    private void pushAll() {
        if (rdAll.isSelected()) {
            String sReq = "SELECT S_ID, S_Nom, S_Localite, S_CP,S_Alti, S_Orientation,S_Type FROM Site ORDER BY S_Nom";
            fillTable(sReq);
        }
    }       

    @FXML
    private void pushDeco() {
        if (rdDeco.isSelected()) {
            String sReq = "SELECT S_ID, S_Nom, S_Localite, S_CP,S_Alti, S_Orientation,S_Type FROM Site WHERE S_Type = 'D' ORDER BY S_Nom";
            fillTable(sReq);
        }
    }       
    
    @FXML
    private void pushAttero() {
        if (rdAttero.isSelected()) {
            String sReq = "SELECT S_ID, S_Nom, S_Localite, S_CP,S_Alti, S_Orientation,S_Type FROM Site WHERE S_Type = 'A' ORDER BY S_Nom";
            fillTable(sReq);
        }
    }       
    
    @FXML
    private void pushNondef() {
        if (rdNondef.isSelected()) {
            String sReq = "SELECT S_ID, S_Nom, S_Localite, S_CP,S_Alti, S_Orientation,S_Type FROM Site WHERE S_Type <> 'D' AND S_Type <> 'A' ORDER BY S_Nom";
            fillTable(sReq);
        }
    }     
    
    private boolean editSite() {
        try {                     
            // Load the fxml file and create a new stage for the popup dialog.
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(Main.class.getResource("/SiteForm.fxml")); 
            AnchorPane page = (AnchorPane) loader.load();

            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.WINDOW_MODAL);       
            dialogStage.initOwner(mainApp.getPrimaryStage());
            Scene scene = new Scene(page);
            dialogStage.setScene(scene);

            String st =   tableSites.getSelectionModel().getSelectedItem().getIdSite();
            // Communication bridge between SiteForm and SiteView controllers
            SiteFormController controller = loader.getController();
            controller.setSiteBridge(this);
            controller.setDialogStage(dialogStage); 
            controller.setEditForm(myConfig,tableSites.getSelectionModel().getSelectedItem().getIdSite(),0);   // 0 -> edit an existing file
            // This window will be modal
            dialogStage.showAndWait();
            
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public void editReturn(boolean formUpdated, Sitemodel newsite) {
        
        if (formUpdated) {            
            tableSites.getSelectionModel().getSelectedItem().setNom(newsite.getNom());
            tableSites.getSelectionModel().getSelectedItem().setVille(newsite.getVille());
            tableSites.getSelectionModel().getSelectedItem().setCp(newsite.getCp());
            tableSites.getSelectionModel().getSelectedItem().setAlt(newsite.getAlt());
            tableSites.getSelectionModel().getSelectedItem().setOrient(newsite.getOrient());  
            tableSites.getSelectionModel().getSelectedItem().setType(newsite.getType());   
            tableSites .refresh();            
        }        
    }
    
    /**
     * Delete a site in database
     */
    private void deleteSite() {
        PreparedStatement pstmt = null;
        
        int selectedIndex = tableSites.getSelectionModel().getSelectedIndex();
        if (selectedIndex >= 0) {
            Sitemodel selSite = tableSites.getSelectionModel().getSelectedItem();
            dialogbox dConfirm = new dialogbox();
            StringBuilder sbMsg = new StringBuilder();             
            sbMsg.append(selSite.getNom());
            sbMsg.append(" ");
            sbMsg.append(selSite.getVille());                        
            if (dConfirm.YesNo(i18n.tr("Suppression du site"), sbMsg.toString()))   {                
                String sReq = "DELETE FROM Site WHERE S_ID = ?";
                try {
                    pstmt = myConfig.getDbConn().prepareStatement(sReq);
                    pstmt.setInt(1, Integer.valueOf(selSite.getIdSite()));
                    pstmt.executeUpdate();    
                    tableSites.getItems().remove(selectedIndex);
                    pstmt.close();
                } catch (Exception e) {
                    alertbox aError = new alertbox(myConfig.getLocale());
                    aError.alertError(e.getMessage());                                                           
                }                                                
            }                                 
        } else {
            // no site selected
            alertbox aError = new alertbox(myConfig.getLocale());
            aError.alertError(i18n.tr("Aucun site sélectionné..."));                       
        }        
    }    
    
    private void addSite() {
        try {                     
            // Load the fxml file and create a new stage for the popup dialog.
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(Main.class.getResource("/SiteForm.fxml")); 
            AnchorPane page = (AnchorPane) loader.load();

            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.WINDOW_MODAL);       
            dialogStage.initOwner(mainApp.getPrimaryStage());
            Scene scene = new Scene(page);
            dialogStage.setScene(scene);

            // Communication bridge between SiteForm and SiteView controllers
            SiteFormController controller = loader.getController();
            controller.setSiteBridge(this);
            controller.setDialogStage(dialogStage); 
            controller.setEditForm(myConfig,null,1);   // 1 -> add a new site 
            // This window will be modal
            dialogStage.showAndWait();
            

        } catch (IOException e) {
            e.printStackTrace();
        }        
    }
     

    /**
     * Is called by the main application to give a reference back to itself.
     * 
     * @param mainApp
     */
    public void setMainApp(Main mainApp) {
        this.mainApp = mainApp; 
        myConfig = mainApp.myConfig;
        i18n = I18nFactory.getI18n("","lang/Messages",SitesViewController.class.getClass().getClassLoader(),myConfig.getLocale(),0);
        winTraduction();
        this.mainApp.rootLayoutController.updateMsgBar("", false, 50); 
        iniTable();
        iniEventBar();     
    }    
    
    private void iniEventBar() {
        top_Menu.addEventHandler(MouseEvent.MOUSE_CLICKED,
            new EventHandler<MouseEvent>() {
                @Override public void handle(MouseEvent e) {                        
                    clicTop_Menu().show(top_Menu, e.getScreenX(), e.getScreenY());
                }
        });          
    }
    
    /**
     * Adding Context Menus, last paragraph
    *     http://docs.oracle.com/javafx/2/ui_controls/menu_controls.htm    
    */
    private ContextMenu clicTop_Menu()   {
        final ContextMenu cm = new ContextMenu();
        
        MenuItem cmItem0 = new MenuItem(i18n.tr("Modifier"));        
        cmItem0.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent e) {
                editSite();
            }            
        });
        cm.getItems().add(cmItem0);
        
        MenuItem cmItem1 = new MenuItem(i18n.tr("Ajouter"));        
        cmItem1.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent e) {
                addSite();
            }            
        });
        cm.getItems().add(cmItem1);
        
        MenuItem cmItem2 = new MenuItem(i18n.tr("Supprimer"));        
        cmItem2.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent e) {
                deleteSite();
            }            
        });
        cm.getItems().add(cmItem2);
        
        return cm;        
    }
    
    /**
    * Translate labels of the window
    */
    private void winTraduction() {        
        
        nomCol.setText(i18n.tr("Nom"));
        villeCol.setText(i18n.tr("Localité"));
        cpCol.setText(i18n.tr("CP"));
        altCol.setText(i18n.tr("Alt"));
        orientCol.setText(i18n.tr("Orientation"));
        rdAll.setText(i18n.tr("Tous"));
        rdDeco.setText(i18n.tr("Décollage"));
        rdAttero.setText(i18n.tr("Atterissage"));
        rdNondef.setText(i18n.tr("Non défini"));                        
    }    
    
}

// Comptage site
// SELECT S_Type,Count(S_ID) FROM Site GROUP BY upper(S_Type)