package home.controllers;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXColorPicker;
import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXListView;
import home.dbDir.CalendarDB;
import home.java.ModelCalendar;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CalendarController implements Initializable {

    private CalendarDB calendarDB;

    @FXML
    private AnchorPane root;

    @FXML
    private VBox weekdayTimeTable, holderPane, selectedCalendarInfo, calendarSelection;

    @FXML
    private HBox hourDayTimeTable, weekdayHeader;

    @FXML
    private Label errorLabel;

    @FXML
    private Label calendarNameLbl,dateLabel;

    @FXML
    private Label monthLabel;

    @FXML
    private CheckBox excurtionCheckBox, spectacleCheckBox, atelierCheckbox, siesteCheckBox, jeuxCheckBox, selectAllCheckBox;

    @FXML
    private JFXColorPicker excursionCP, spectacleCP, atelierCP, siesteCP, jeuxCP;

    @FXML
    private JFXButton loadCalendar;


    @FXML
    private JFXComboBox<String> selectedYear;

    @FXML
    private JFXListView<String> monthSelect;

    @FXML
    private GridPane calendarGrid, timeTable;

    @FXML
    private ScrollPane scrollPane1, scrollPane;

    private static boolean checkBoxesHaveBeenClicked = false;
    private static boolean calendarLoaded = false;
    private String currentYearTimeLable = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        if (Calendar.getInstance().get(Calendar.MONTH) > 6)
            currentYearTimeLable = Calendar.getInstance().get(Calendar.YEAR) + "-" + Integer.valueOf(Calendar.getInstance().get(Calendar.YEAR) + 1);
        else
            currentYearTimeLable = Integer.valueOf(Calendar.getInstance().get(Calendar.YEAR) - 1) + "-" + Calendar.getInstance().get(Calendar.YEAR);

        Timeline clock = new Timeline(new KeyFrame(Duration.ZERO, e -> {
            DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss      yyyy/MM/dd");
            java.util.Date date = new java.util.Date();
            dateLabel.setText(dateFormat.format(date));
        }),
                new KeyFrame(Duration.seconds(1))
        );
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();
        calendarDB = new CalendarDB();
        initializeCalendarGrid();
        initializeCalendarWeekdayHeader();
        initializeMonthSelector();
        initializeTimetableWeekdayHeader();
        initializeTimetableTimesHeader();
        initializeTimeTableGrid();
        initalizeColorPicker();

    }

    @FXML
    void handleCalendarLoaded() {
        calendarSelection.setVisible(false);
        selectedCalendarInfo.setVisible(true);

    }

    private void addEvent(VBox day) {

        if (!day.getChildren().isEmpty()) {

            Label lbl = (Label) day.getChildren().get(0);

            ModelCalendar.getInstance().event_day = Integer.parseInt(lbl.getText());
            ModelCalendar.getInstance().event_month = ModelCalendar.getInstance().getMonthIndex(monthSelect.getSelectionModel().getSelectedItem());
            ModelCalendar.getInstance().event_year = Integer.parseInt(selectedYear.getValue());

            try {

                FXMLLoader loader = new FXMLLoader();
                loader.setLocation(getClass().getResource("/home/fxml/addEvent.fxml"));
                AnchorPane rootLayout = loader.load();
                Stage stage = new Stage(StageStyle.UNDECORATED);
                stage.initModality(Modality.APPLICATION_MODAL);

                AddEventController eventController = loader.getController();
                eventController.setMainController(this);

                Scene scene = new Scene(rootLayout);
                stage.setScene(scene);
                stage.show();
            } catch (IOException ex) {
                Logger.getLogger(CalendarController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void editEvent(VBox day, String descript, String typeevent, Time eventTime) {

        Label dayLbl = (Label) day.getChildren().get(0);
        ModelCalendar.getInstance().event_day = Integer.parseInt(dayLbl.getText());
        ModelCalendar.getInstance().event_month = ModelCalendar.getInstance().getMonthIndex(monthSelect.getSelectionModel().getSelectedItem());
        ModelCalendar.getInstance().event_year = Integer.parseInt(selectedYear.getValue());
        ModelCalendar.getInstance().eventDescreption = descript;
        ModelCalendar.getInstance().eventType = typeevent;
        ModelCalendar.getInstance().eventTime = LocalTime.of(eventTime.getHours() - 1, eventTime.getMinutes());

        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/home/fxml/editEvent.fxml"));
            AnchorPane rootLayout = loader.load();
            Stage stage = new Stage(StageStyle.UNDECORATED);
            stage.initModality(Modality.APPLICATION_MODAL);

            EditEventController eventController = loader.getController();
            eventController.setMainController(this);

            Scene scene = new Scene(rootLayout);
            stage.setScene(scene);
            stage.show();
        } catch (IOException ex) {
            Logger.getLogger(CalendarController.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private void loadCalendarLabels() {

        int year = ModelCalendar.getInstance().viewing_year;
        int month = ModelCalendar.getInstance().viewing_month;

        GregorianCalendar gc = new GregorianCalendar(year, month, 1);
        int firstDay = gc.get(Calendar.DAY_OF_WEEK);
        int daysInMonth = gc.getActualMaximum(Calendar.DAY_OF_MONTH);

        int gridCount = 1;
        int lblCount = 1;

        for (Node node : calendarGrid.getChildren()) {
            VBox day = (VBox) node;
            day.getChildren().clear();
            day.setStyle("-fx-backgroud-color: white");
            day.setStyle("-fx-font: 14px \"System\" ");

            if (gridCount < firstDay) {
                gridCount++;
                day.setStyle("-fx-background-color: #EEEEEE");
            } else {
                if (lblCount > daysInMonth) {
                    day.setStyle("-fx-background-color: #EEEEEE");
                } else {
                    Label lbl = new Label(Integer.toString(lblCount));
                    lbl.setPadding(new Insets(5));
                    lbl.setStyle("-fx-text-fill:white");
                    day.getChildren().add(lbl);
                }

                lblCount++;
            }
        }
    }


    private void initializeCalendarGrid() {
        int rows = 6;
        int cols = 7;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                VBox vPane = new VBox();
                vPane.getStyleClass().add("calendar_pane");
                vPane.setMinWidth(calendarGrid.getPrefWidth() / 7);

                vPane.addEventHandler(MouseEvent.MOUSE_CLICKED, (e) -> addEvent(vPane));

                GridPane.setVgrow(vPane, Priority.ALWAYS);

                calendarGrid.add(vPane, j, i);
            }
        }

        for (int i = 0; i < 7; i++) {
            RowConstraints row = new RowConstraints();
            row.setMinHeight(scrollPane.getHeight() / 7);
            calendarGrid.getRowConstraints().add(row);
        }
    }

    private void initializeTimeTableGrid() {
        int rows = 5;
        int cols = 8;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                VBox vPane = new VBox();
                vPane.getStyleClass().add("calendar_pane");
                vPane.setMinWidth(timeTable.getPrefWidth() / 8);

                vPane.addEventHandler(MouseEvent.MOUSE_CLICKED, (e) -> addEvent(vPane));

                GridPane.setVgrow(vPane, Priority.ALWAYS);

                timeTable.add(vPane, j, i);
            }
        }

        for (int i = 0; i < 8; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setMinWidth(scrollPane1.getHeight() / 8);
            timeTable.getColumnConstraints().add(col);
        }
    }

    void calendarGenerate() {
        selectedYear.getItems().clear();
        selectedYear.getItems().add(Integer.toString(ModelCalendar.getInstance().calendar_start));
        selectedYear.getItems().add(Integer.toString(ModelCalendar.getInstance().calendar_end));
        selectedYear.getSelectionModel().selectFirst();

        ModelCalendar.getInstance().viewing_year = Integer.parseInt(selectedYear.getSelectionModel().getSelectedItem());

        System.out.println("---------------------------------------------------------");
        System.out.println("calendarGenerate from CaldendarController");
        System.out.println("Initialized ModelCalendar for calendar instance :");
        System.out.println("ModelCalendar.calendarName : " + ModelCalendar.getInstance().calendar_name);
        System.out.println("ModelCalendar.calendarStartDay : " + ModelCalendar.getInstance().calendar_start_date);
        System.out.println("ModelCalendar.calendarViewingMonth : " + ModelCalendar.getInstance().viewing_month);
        System.out.println("ModelCalendar.calendarViewingYear : " + ModelCalendar.getInstance().viewing_year);
        System.out.println("---------------------------------------------------------");

        selectedYear.setVisible(true);
        calendarNameLbl.setText(ModelCalendar.getInstance().calendar_name);

        String[] months = DateFormatSymbols.getInstance(new Locale("ar")).getMonths();
        String[] spliceMonths = Arrays.copyOfRange(months, 0, 12);
        //TODO:set the month select to the exact one instead of the default one
        monthSelect.getItems().clear();
        monthSelect.getItems().addAll(spliceMonths);

        monthSelect.getSelectionModel().selectFirst();
        monthLabel.setText(monthSelect.getSelectionModel().getSelectedItem());

        ModelCalendar.getInstance().viewing_month =
                ModelCalendar.getInstance().getMonthIndex(monthSelect.getSelectionModel().getSelectedItem());

        calendarLoaded = true;
        loadCalendar.setDisable(false);
        handleCalendarLoaded();

        repaintView();
    }

    void repaintView() {
        loadCalendarLabels();
        if (!checkBoxesHaveBeenClicked) {
            populateMonthWithEvents();
        } else {
            handleCheckBoxAction();
        }
    }

    private void populateMonthWithEvents() {

        String calendarName = ModelCalendar.getInstance().calendar_name;
        String currentMonth = monthLabel.getText();

        int currentMonthIndex = ModelCalendar.getInstance().getMonthIndex(currentMonth) + 1;
        int currentYear = Integer.parseInt(selectedYear.getValue());

        String getMonthEventsQuery = "SELECT * From events WHERE CalendarName='" + calendarName + "'";
        ResultSet result = calendarDB.executeQuery(getMonthEventsQuery);
        try {
            while (result.next()) {
                Date eventDate = result.getDate("EventDate");
                String eventDescript = result.getString("EventDescription");
                String typeEvent = result.getString("TypeEvent");
                Time eventTime = result.getTime("EventTime");

                if (currentYear == eventDate.toLocalDate().getYear()) {
                    if (currentMonthIndex == eventDate.toLocalDate().getMonthValue()) {
                        int day = eventDate.toLocalDate().getDayOfMonth();
                        showDate(day, eventDescript, getEventType(typeEvent), eventTime);
                    }
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(AddEventController.class.getName()).log(Level.SEVERE, null, ex);
            //System.out.println("SQL ERROR MESSAGE : " + ex.getMessage());
        }
    }

    private String getEventType(String event) {
        String eveTyp = null;
        switch (event) {
            case "رحلة":
                eveTyp = "excursion";
                break;
            case "عرض":
                eveTyp = "spectacle";
                break;
            case "ورشة":
                eveTyp = "atelier";
                break;
            case "قيلولة":
                eveTyp = "sieste";
                break;
            case "ألعاب":
                eveTyp = "jeux";
        }
        return eveTyp;
    }

    private void showDate(int dayNumber, String descript, String typeEvent, Time eventTime) {

        Image img = new Image("/home/icons/icon2.png");
        ImageView imgView = new ImageView();
        imgView.setImage(img);

        for (Node node : calendarGrid.getChildren()) {

            VBox day = (VBox) node;

            if (!day.getChildren().isEmpty()) {

                Label lbl = (Label) day.getChildren().get(0);

                int currentNumber = Integer.parseInt(lbl.getText());

                if (currentNumber == dayNumber) {

                    Label eventLbl = new Label(descript);
                    eventLbl.setGraphic(imgView);
                    eventLbl.setStyle("-fx-text-fill: white; -fx-font-size: 13px;");

                    eventLbl.setAccessibleText(typeEvent);

                    eventLbl.addEventHandler(MouseEvent.MOUSE_PRESSED, (e) -> editEvent((VBox) eventLbl.getParent(), eventLbl.getText(), eventLbl.getAccessibleText(), eventTime));

                    String eventRGB = calendarDB.getEventColor(typeEvent);

                    String[] colors = eventRGB.split("-");
                    String red = colors[0];
                    String green = colors[1];
                    String blue = colors[2];

                    eventLbl.setStyle("-fx-background-color: rgb(" + red +
                            ", " + green + ", " + blue + ", " + 1 + ");");

                    eventLbl.setMaxWidth(Double.MAX_VALUE);

                    eventLbl.addEventHandler(MouseEvent.MOUSE_ENTERED, (e) -> eventLbl.getScene().setCursor(Cursor.HAND));

                    eventLbl.addEventHandler(MouseEvent.MOUSE_EXITED, (e) -> eventLbl.getScene().setCursor(Cursor.DEFAULT));

                    day.getChildren().add(eventLbl);
                }
            }
        }
    }

    private void initializeCalendarWeekdayHeader() {

        int weekdays = 7;

        String[] weekAbbr = {"الأحد", "الإثنين", "الثلثاء", "الأربعاء", "الخميس", "الجمعة", "السبت"};

        for (int i = 0; i < weekdays; i++) {

            StackPane pane = new StackPane();
            pane.getStyleClass().add("weekday-header");

            HBox.setHgrow(pane, Priority.ALWAYS);
            pane.setMaxWidth(Double.MAX_VALUE);

            pane.setMinWidth(weekdayHeader.getPrefWidth() / 7);

            weekdayHeader.getChildren().add(pane);

            pane.getChildren().add(new Label(weekAbbr[i]));
        }
    }

    private void initializeTimetableWeekdayHeader() {

        int weekdays = 5;

        String[] weekAbbr = {"الأحد", "الإثنين", "الثلثاء", "الأربعاء", "الخميس"};

        for (int i = 0; i < weekdays; i++) {

            StackPane pane = new StackPane();
            pane.getStyleClass().add("weekday-header");

            VBox.setVgrow(pane, Priority.ALWAYS);
            pane.setMaxWidth(Double.MAX_VALUE);

            pane.setMinWidth(weekdayTimeTable.getPrefWidth() / 5);

            weekdayTimeTable.getChildren().add(pane);

            pane.getChildren().add(new Label(weekAbbr[i]));
        }
    }

    private void initializeTimetableTimesHeader() {

        int dayHours = 9;

        String[] weekAbbr = {"", "09h-08h", "10h-09h", "11h-10h", "12h-11h", "14h-13h", "15h-14h", "16h-15h", "17h-16h"};

        for (int i = 0; i < dayHours; i++) {

            if (i == 0) {
                VBox pane = new VBox();
                pane.getStyleClass().add("weekday-header");

                HBox.setHgrow(pane, Priority.ALWAYS);
                pane.setMaxWidth(Double.MAX_VALUE);
                pane.setAlignment(Pos.CENTER);

                pane.setMinWidth(120);

                hourDayTimeTable.getChildren().add(pane);
                pane.getChildren().add(new Label("السنة الدراسية"));
                pane.getChildren().add(new Label(currentYearTimeLable));
            } else {

                StackPane pane = new StackPane();
                pane.getStyleClass().add("weekday-header");

                HBox.setHgrow(pane, Priority.ALWAYS);
                pane.setMaxWidth(Double.MAX_VALUE);

                pane.setMinWidth(hourDayTimeTable.getPrefWidth() / 9);

                hourDayTimeTable.getChildren().add(pane);

                pane.getChildren().add(new Label(weekAbbr[i]));
            }
        }
    }


    private void initializeMonthSelector() {

        monthSelect.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {

            if (newValue != null) {

                monthLabel.setText(newValue);

                ModelCalendar.getInstance().viewing_month = ModelCalendar.getInstance().getMonthIndex(newValue);

                repaintView();
            }

        });

        selectedYear.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {

            if (newValue != null) {
                ModelCalendar.getInstance().viewing_year = Integer.parseInt(newValue);
                repaintView();
            }
        });
    }

    @FXML
    public void btnBackward() {
        calendarSelection.setVisible(true);
        selectedCalendarInfo.setVisible(false);
    }

    @FXML
    public void newCalendarEvent() {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/home/fxml/addCalendar.fxml"));
            AnchorPane rootLayout = loader.load();
            Stage stage = new Stage(StageStyle.UNDECORATED);
            stage.initModality(Modality.APPLICATION_MODAL);

            AddCalendarController calendarController = loader.getController();
            calendarController.setMainController(this);

            Scene scene = new Scene(rootLayout);
            stage.setScene(scene);
            stage.showAndWait();

        } catch (IOException ex) {
            Logger.getLogger(CalendarController.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (calendarLoaded) {
            calendarSelection.setVisible(false);
            selectedCalendarInfo.setVisible(true);
        }

    }

    @FXML
    private void listCalendarsEvent() {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/home/fxml/listeCalendars.fxml"));
            AnchorPane rootLayout = loader.load();
            Stage stage = new Stage(StageStyle.UNDECORATED);
            stage.initModality(Modality.APPLICATION_MODAL);

            ListeCalendarsController listController = loader.getController();
            listController.setMainController(this);

            Scene scene = new Scene(rootLayout);
            stage.setScene(scene);
            stage.show();

        } catch (IOException ex) {
            Logger.getLogger(CalendarController.class.getName()).log(Level.SEVERE, null, ex);
        }

        if (calendarLoaded) {
            calendarSelection.setVisible(false);
            selectedCalendarInfo.setVisible(true);
        }

    }

    @FXML
    void handleCheckBoxAction() {
        if (!checkBoxesHaveBeenClicked) {
            checkBoxesHaveBeenClicked = true;

        }

        ArrayList<String> eventToFilter = new ArrayList<>();

        if (excurtionCheckBox.isSelected()) {
            eventToFilter.add("رحلة");
        }
        if (!excurtionCheckBox.isSelected()) {
            int auxIndex = eventToFilter.indexOf("رحلة");
            if (auxIndex != -1) {
                eventToFilter.remove(auxIndex);
            }
        }

        if (spectacleCheckBox.isSelected()) {
            eventToFilter.add("عرض");
        }
        if (!spectacleCheckBox.isSelected()) {
            int auxIndex = eventToFilter.indexOf("عرض");
            if (auxIndex != -1) {
                eventToFilter.remove(auxIndex);
            }
        }

        if (atelierCheckbox.isSelected()) {
            eventToFilter.add("ورشة");
        }
        if (!atelierCheckbox.isSelected()) {
            int auxIndex = eventToFilter.indexOf("ورشة");
            if (auxIndex != -1) {
                eventToFilter.remove(auxIndex);
            }
        }

        if (siesteCheckBox.isSelected()) {
            eventToFilter.add("قيلولة");
        }
        if (!siesteCheckBox.isSelected()) {
            int auxIndex = eventToFilter.indexOf("قيلولة");
            if (auxIndex != -1) {
                eventToFilter.remove(auxIndex);
            }
        }

        if (jeuxCheckBox.isSelected()) {
            eventToFilter.add("ألعاب");
        }
        if (!jeuxCheckBox.isSelected()) {
            int auxIndex = eventToFilter.indexOf("ألعاب");
            if (auxIndex != -1) {
                eventToFilter.remove(auxIndex);
            }
        }

        String calName = ModelCalendar.getInstance().calendar_name;

        if (eventToFilter.isEmpty()) {
            selectAllCheckBox.setSelected(false);
            loadCalendarLabels();
        } else {

            ArrayList<String> filteredEventsList = calendarDB.getFilteredEvents(eventToFilter, calName);

            showFilteredEventsInMonth(filteredEventsList);
        }
    }

    private void showFilteredEventsInMonth(ArrayList<String> filteredEventsList) {

        String currentMonth = monthLabel.getText();
        int currentMonthIndex = ModelCalendar.getInstance().getMonthIndex(currentMonth) + 1;
        int currentYear = Integer.parseInt(selectedYear.getValue());
        loadCalendarLabels();

        for (String s : filteredEventsList) {
            String[] eventInfo = s.split("~");
            String eventDescript = eventInfo[0];
            String eventDate = eventInfo[1];
            String eventCalName = eventInfo[2];
            String eventTime = eventInfo[3];
            String eventType = eventInfo[4];

            String[] dateParts = eventDate.split("-");
            int eventYear = Integer.parseInt(dateParts[0]);
            int eventMonth = Integer.parseInt(dateParts[1]);
            int eventDay = Integer.parseInt(dateParts[2]);

            if (currentYear == eventYear) {
                if (currentMonthIndex == eventMonth) {
                    showDate(eventDay, eventDescript, getEventType(eventType), Time.valueOf(eventTime));
                }
            }
        }
    }

    @FXML
    void selectAllCheckBoxes(ActionEvent event) {
        if (selectAllCheckBox.isSelected()) {
            selectCheckBoxes();
        } else {
            unSelectCheckBoxes();
        }
        handleCheckBoxAction();

    }

    private void unSelectCheckBoxes() {
        excurtionCheckBox.setSelected(false);
        spectacleCheckBox.setSelected(false);
        atelierCheckbox.setSelected(false);
        siesteCheckBox.setSelected(false);
        jeuxCheckBox.setSelected(false);
    }

    private void selectCheckBoxes() {
        excurtionCheckBox.setSelected(true);
        spectacleCheckBox.setSelected(true);
        atelierCheckbox.setSelected(true);
        siesteCheckBox.setSelected(true);
        jeuxCheckBox.setSelected(true);

    }

    @FXML
    void updateColors(MouseEvent event) {
        changeColors();
        if (ModelCalendar.getInstance().calendar_name != null)
            repaintView();

    }

    private String getRGB(Color c) {

        return (int) (c.getRed() * 255) + "-"
                + (int) (c.getGreen() * 255) + "-"
                + (int) (c.getBlue() * 255);
    }

    private void changeColors() {

        Color excursionColor = excursionCP.getValue();
        String excursionRGB = getRGB(excursionColor);
        calendarDB.setEventColor("excursion", excursionRGB);

        Color spectacleColor = spectacleCP.getValue();
        String spectacleRGB = getRGB(spectacleColor);
        calendarDB.setEventColor("spectacle", spectacleRGB);

        Color atelierColor = atelierCP.getValue();
        String atelierRGB = getRGB(atelierColor);
        calendarDB.setEventColor("atelier", atelierRGB);

        Color siesteColor = siesteCP.getValue();
        String siesteRGB = getRGB(siesteColor);
        calendarDB.setEventColor("sieste", siesteRGB);

        Color jeuxColor = jeuxCP.getValue();
        String jeuxRGB = getRGB(jeuxColor);
        calendarDB.setEventColor("jeux", jeuxRGB);

    }

    private void initalizeColorPicker() {

        String excursionRGB = calendarDB.getEventColor("excursion");
        String spectacleRGB = calendarDB.getEventColor("spectacle");
        String atelierRGB = calendarDB.getEventColor("atelier");
        String siesteRGB = calendarDB.getEventColor("sieste");
        String jeuxRGB = calendarDB.getEventColor("jeux");

        String[] colors = excursionRGB.split("-");
        String red = colors[0];
        String green = colors[1];
        String blue = colors[2];
        Color c = Color.rgb(Integer.parseInt(red), Integer.parseInt(green), Integer.parseInt(blue));
        excursionCP.setValue(c);

        colors = spectacleRGB.split("-");
        red = colors[0];
        green = colors[1];
        blue = colors[2];
        c = Color.rgb(Integer.parseInt(red), Integer.parseInt(green), Integer.parseInt(blue));
        spectacleCP.setValue(c);

        colors = atelierRGB.split("-");
        red = colors[0];
        green = colors[1];
        blue = colors[2];
        c = Color.rgb(Integer.parseInt(red), Integer.parseInt(green), Integer.parseInt(blue));
        atelierCP.setValue(c);

        colors = siesteRGB.split("-");
        red = colors[0];
        green = colors[1];
        blue = colors[2];
        c = Color.rgb(Integer.parseInt(red), Integer.parseInt(green), Integer.parseInt(blue));
        siesteCP.setValue(c);

        colors = jeuxRGB.split("-");
        red = colors[0];
        green = colors[1];
        blue = colors[2];
        c = Color.rgb(Integer.parseInt(red), Integer.parseInt(green), Integer.parseInt(blue));
        jeuxCP.setValue(c);
    }

    void reloadStage() throws IOException {
        Scene scene = new Scene(FXMLLoader.load(getClass().getResource("/home/fxml/main.fxml")));
        ((Stage) holderPane.getScene().getWindow()).setScene(scene);
    }
}