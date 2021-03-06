package org.redhatchallenge.rhc2013.client;

import com.google.gwt.cell.client.CheckboxCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.cell.client.SelectionCell;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.Header;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.DefaultSelectionEventManager;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.MultiSelectionModel;
import com.google.gwt.view.client.ProvidesKey;
import org.redhatchallenge.rhc2013.shared.Student;
import org.redhatchallenge.rhc2013.shared.TimeSlotList;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;


public class TimeslotScreen extends Composite {
    interface TimeslotUiBinder extends com.google.gwt.uibinder.client.UiBinder<Widget, TimeslotScreen> {
    }

    private static TimeslotUiBinder UiBinder = GWT.create(TimeslotUiBinder.class);

    @UiField ListBox countryField;
    @UiField ListBox regionField;
    //    @UiField ListBox timeslotField;
    @UiField Label noTimeslotLabel;
    @UiField CellTable<Student> timeslotCellTable;
    @UiField MySimplePager pager;
    @UiField Button noTimeSearchButton;
    @UiField Label timeslotLabel;
    @UiField ListBox timeSlotList;
    @UiField Button timeslotPageRefresh;
    @UiField Label errorLabel;
    @UiField Button timeSlotButton;


    private UserServiceAsync userService = UserService.Util.getInstance();
    private List<Student> origStudentList;
    private List<Student> studentList;
    private ListDataProvider<Student> provider;
    private List<Student> selectedStudentList = new ArrayList<Student>();
    private List<TimeSlotList> ListofTimeSlot;
    private List<String> dateList = new ArrayList<String>();
    List<Student> list = new ArrayList<Student>();

    private static final ProvidesKey<Student> KEY_PROVIDER = new ProvidesKey<Student>() {
        @Override
        public Object getKey(Student item) {
            return item.getEmail();
        }
    };

    private String check;

    public TimeslotScreen() {
        initWidget(UiBinder.createAndBindUi(this));

        userService.getListOfStudents(new AsyncCallback<List<Student>>() {
            @Override
            public void onFailure(Throwable throwable) {
                throwable.printStackTrace();
            }

            @Override
            public void onSuccess(List<Student> students) {
                origStudentList = new ArrayList<Student>(students);
                studentList = students;
                for (Student s : studentList){
                    list.add(s);
                }

                provider = new ListDataProvider<Student>(studentList);
                provider.addDataDisplay(timeslotCellTable);

                initTimeslotCellTable();
            }
        });

        userService.getListOfTimeSlot(new AsyncCallback<List<TimeSlotList>>() {
            @Override
            public void onFailure(Throwable throwable) {
                throwable.printStackTrace();
            }

            @Override
            public void onSuccess(List<TimeSlotList> timeSlotLists) {
                ListofTimeSlot = new ArrayList<TimeSlotList>(timeSlotLists);
                for(TimeSlotList d : ListofTimeSlot){
                    Date date = convertTimeSlot(d.getTimeslot());
                    String formatedDate = returnLongDateTime(DateTimeFormat.getFormat(DateTimeFormat.PredefinedFormat.DATE_TIME_SHORT).format(date));
                    dateList.add(formatedDate);

                }
            }
        });

        pager.setDisplay(timeslotCellTable);
        pager.setPageSize(8);
        timeSlotList.addItem("Please Select a Time Slot");
        errorLabel.setVisible(false);
        errorLabel.setText("No Time Slot Selected! Please try again!");
    }

    private void initTimeslotCellTable(){
        List list = provider.getList();

        final MultiSelectionModel<Student> selectionModel = new MultiSelectionModel<Student>(KEY_PROVIDER);

        ColumnSortEvent.ListHandler<Student> sortHandler = new ColumnSortEvent.ListHandler<Student>(list);
        timeslotCellTable.addColumnSortHandler(sortHandler);

        Column<Student,Boolean> selectColumn = new Column<Student, Boolean>(new CheckboxCell(true, false)){
            @Override
            public Boolean getValue(Student student) {
                return selectionModel.isSelected(student);
            }
        };


        selectColumn.setFieldUpdater(new FieldUpdater<Student, Boolean>() {
            @Override
            public void update(int i, Student student, Boolean aBoolean) {
                if (aBoolean) {
                    selectedStudentList.add(student);
                }
                else {
                    selectedStudentList.remove(student);
                }
            }
        });

        Header<Boolean> selectAllHeader = new Header<Boolean>(new CheckboxCell(true, false)) {
            @Override
            public Boolean getValue() {
                for (Student student : timeslotCellTable.getVisibleItems()){
                    if (!selectionModel.isSelected(student)){
                        return false;
                    }
                }
                return timeslotCellTable.getVisibleItems().size() >0;
            }
        };

        selectAllHeader.setUpdater(new ValueUpdater<Boolean>() {
            @Override
            public void update(Boolean aBoolean) {
                for(Student student : timeslotCellTable.getVisibleItems()){
                    selectionModel.setSelected(student, aBoolean);
                }
                if (aBoolean == true){
                    for (int i=0;i<timeslotCellTable.getVisibleItemCount(); i++){
                        if (!selectedStudentList.contains(timeslotCellTable.getVisibleItem(i)))
                            selectedStudentList.add(timeslotCellTable.getVisibleItem(i));
                    }
                }
                else if (aBoolean == false){
                    for (int i=0;i<timeslotCellTable.getVisibleItemCount(); i++){
                        selectedStudentList.remove(timeslotCellTable.getVisibleItem(i));
                    }
                }
            }
        });//End of checkbox

        Column<Student, String> emailColumn = new Column<Student, String>(new TextCell()) {
            @Override
            public String getValue(Student student) {
                return student.getEmail();
            }
        };

        emailColumn.setSortable(true);
        sortHandler.setComparator(emailColumn, new Comparator<Student>() {
            @Override
            public int compare(Student o1, Student o2) {
                if (o1 == o2) {
                    return 0;
                }
                if (o1 != null) {
                    return (o2 != null) ? o1.getEmail().compareTo(o2.getEmail()) : 1;
                }
                return -1;
            }
        });

        ArrayList<String> countryList = new ArrayList<String>();
        countryList.add("Singapore");
        countryList.add("Malaysia");
        countryList.add("Thailand");
        countryList.add("China/Beijing");
        countryList.add("China/Shanghai");
        countryList.add("China/Wuhan");
        countryList.add("China/Dalian");
        countryList.add("China/Jinan");
        countryList.add("China/Others");
        countryList.add("Hong Kong");
        countryList.add("Taiwan");

        Column<Student, String> countryColumn = new Column<Student, String>(new SelectionCell(countryList)) {
            @Override
            public String getValue(Student student) {
                return student.getCountry();
            }
        };

        countryColumn.setSortable(true);
        sortHandler.setComparator(countryColumn, new Comparator<Student>() {
            @Override
            public int compare(Student o1, Student o2) {
                if (o1 == o2) {
                    return 0;
                }
                if (o1 != null) {
                    return (o2 != null) ? o1.getCountry().compareTo(o2.getCountry()) : 1;
                }
                return -1;
            }
        });

        countryColumn.setFieldUpdater(new FieldUpdater<Student, String>() {
            @Override
            public void update(int index, Student object, String value) {
                object.setCountry(value);
                userService.updateStudentData(object, new AsyncCallback<Boolean>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        caught.printStackTrace();
                    }

                    @Override
                    public void onSuccess(Boolean result) {
                        if(!result) {
                            displayErrorBox("Failed", "Update has failed");
                        }

                        else {
                            timeslotCellTable.redraw();
                        }
                    }
                });
            }
        });

        Column<Student, String> timeSlotColumn = new Column<Student, String>(new TextCell()) {
            @Override
            public String getValue(Student student) {
                if(student.getTimeslot() == 0){
                    return "Time Slot is not Assigned";
                }
                else{
                    Date date = new Date(student.getTimeslot());
                    return DateTimeFormat.getFormat(DateTimeFormat.PredefinedFormat.DATE_TIME_SHORT).format(date);
                }
            }
        };

        timeSlotColumn.setSortable(true);
        sortHandler.setComparator(timeSlotColumn, new Comparator<Student>() {
            @Override
            public int compare(Student o1, Student o2) {
                if (o1 == o2) {
                    return 0;
                }
                if (o1 != null) {
                    return (o2 != null) ? String.valueOf(o1.getTimeslot()).compareTo(String.valueOf(o2.getTimeslot())) : 1;
                }
                return -1;
            }
        });

        timeslotCellTable.addColumn(selectColumn, selectAllHeader);
        timeslotCellTable.addColumn(emailColumn, "Email");
        timeslotCellTable.addColumn(countryColumn, "Region");
        timeslotCellTable.addColumn(timeSlotColumn, "Time Slot");
        timeslotCellTable.setSelectionModel(selectionModel, DefaultSelectionEventManager.<Student> createCheckboxManager(timeslotCellTable.getColumnIndex(selectColumn)));

        timeslotCellTable.setWidth("45%", true);
        timeslotCellTable.setColumnWidth(selectColumn, 6.0, Style.Unit.PX);
        timeslotCellTable.setColumnWidth(emailColumn, 15.0, Style.Unit.PX);
        timeslotCellTable.setColumnWidth(countryColumn, 12.0, Style.Unit.PX);
        timeslotCellTable.setColumnWidth(timeSlotColumn, 12.0, Style.Unit.PX);
    }


    private Date convertTimeSlot(long unixTime){
        Date date = new Date(unixTime);
        return date;
    }

    @UiHandler("countryField")
    public void handleTimeSlotChange(ChangeEvent event) {
        if(countryField.getItemText(countryField.getSelectedIndex()).equalsIgnoreCase("China")){

            timeSlotList.clear();
            timeSlotList.insertItem("Select All", 0);
            for(int i = 0; i < 10; i++){
                timeSlotList.insertItem(dateList.get(i).toString(),i+1);
            }

        }
        else{
            timeSlotList.clear();
            timeSlotList.insertItem("Select All", 0);
            for(int i = 10; i < 12; i++){
                timeSlotList.insertItem(dateList.get(i).toString(),i-9);
            }

        }

    }

    @UiHandler("timeSlotList")
    public void handleTimeslotChange(ChangeEvent event){
        String timeslot;
        switch (timeSlotList.getSelectedIndex()) {
            case 0:
                errorLabel.setText("No Time Slot Selected! Please try again!");
                break;
            case 1:
                if (countryField.getItemText(countryField.getSelectedIndex()).contains("China")){
                    timeslot = "2013-10-23 09:00";
                    countTimeslot(timeslot);
                    errorLabel.setVisible(false);
                }
                else {
                    timeslot ="2013-10-24 14:00";
                    countTimeslot(timeslot);
                    errorLabel.setVisible(false);
                }
                break;

            case 2:
                if (countryField.getItemText(countryField.getSelectedIndex()).contains("China")){
                    timeslot = "2013-10-23 10:15";
                    countTimeslot(timeslot);
                    errorLabel.setVisible(false);
                }
                else {
                    timeslot ="2013-10-24 16:00";
                    countTimeslot(timeslot);
                    errorLabel.setVisible(false);
                }
                break;
            case 3:
                timeslot = "2013-10-23 11:30";
                countTimeslot(timeslot);
                errorLabel.setVisible(false);
                break;
            case 4:
                timeslot = "2013-10-23 12:45";
                countTimeslot(timeslot);
                errorLabel.setVisible(false);
                break;
            case 5:
                timeslot = "2013-10-23 14:00";
                countTimeslot(timeslot);
                errorLabel.setVisible(false);
                break;
            case 6:
                timeslot = "2013-10-23 15:15";
                countTimeslot(timeslot);
                errorLabel.setVisible(false);
                break;
            case 7:
                timeslot = "2013-10-23 16:30";
                countTimeslot(timeslot);
                errorLabel.setVisible(false);
                break;
            case 8:
                timeslot = "2013-10-23 17:45";
                countTimeslot(timeslot);
                errorLabel.setVisible(false);
                break;
            case 9:
                timeslot = "2013-10-23 19:00";
                countTimeslot(timeslot);
                errorLabel.setVisible(false);
                break;
            case 10:
                timeslot = "2013-10-23 20:15";
                countTimeslot(timeslot);
                errorLabel.setVisible(false);
                break;
        }
    }

    @UiHandler("countryField")
    public void handleCountryChange(ChangeEvent event) {
        String contains = countryField.getItemText(countryField.getSelectedIndex());
        String country = null;
        list.clear();
        switch (countryField.getSelectedIndex()) {
            //Do nothing
            case 0:
                noTimeslotLabel.setText("");
                for (Student s : origStudentList){
                    list.add(s);
                }

                timeSlotList.clear();
                timeSlotList.insertItem("Please Select a Time Slot",0);
                regionField.setVisible(false);
                break;

            // Singapore
            case 1:
                country = "Singapore";
                regionField.setVisible(false);
                for (Student s : origStudentList){
                    if (s.getCountry().equals(contains)){
                        list.add(s);
                    }
                }
                regionField.setSelectedIndex(0);
                countContestant(country,null);
                break;
            // Malaysia
            case 2:
                country = "Malaysia";
                regionField.setVisible(false);
                for (Student s : origStudentList){
                    if (s.getCountry().equals(contains)){
                        list.add(s);
                    }
                }
                regionField.setSelectedIndex(0);
                countContestant(country,null);
                break;
            // Thailand
            case 3:
                country = "Thailand";
                regionField.setVisible(false);
                for (Student s : origStudentList){
                    if (s.getCountry().equals(contains)){
                        list.add(s);
                    }
                }
                regionField.setSelectedIndex(0);
                countContestant(country,null);
                break;
            // China
            case 4:
                regionField.setVisible(true);
                for (Student s : origStudentList){
                    if (s.getCountry().contains(contains)){
                        list.add(s);
                    }

                }
                regionField.setSelectedIndex(0);
                noTimeslotLabel.setText("");
                countContestant(country,null);
                break;
            // Hong Kong
            case 5:
                country = "Hong Kong";
                regionField.setVisible(false);
                for (Student s : origStudentList){
                    if (s.getCountry().equals(contains)){
                        list.add(s);
                    }
                }
                regionField.setSelectedIndex(0);
                countContestant(country,null);
                break;
            // Taiwan
            case 6:
                country = "Taiwan";
                regionField.setVisible(false);
                for (Student s : origStudentList){
                    if (s.getCountry().equals(contains)){
                        list.add(s);
                    }
                }
                regionField.setSelectedIndex(0);
                countContestant(country,null);
                break;
        }
        if(list.size() == 0){
            timeslotCellTable.setRowCount(0);
        }
        else{
        provider.getList().clear();
        provider.getList().addAll(list);
        }
    }

    @UiHandler("timeSlotButton")
    public void handleTimeSlotButtonClick(ClickEvent event) {
        final String timeSlot;
        if(!timeSlotList.getItemText(timeSlotList.getSelectedIndex()).equals("Please Select a Time Slot")){
            timeSlot = timeSlotList.getItemText(timeSlotList.getSelectedIndex());
            if(!check.equals("0")){
                userService.assignTimeSlot(selectedStudentList, timeSlot, new AsyncCallback<Boolean>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        caught.printStackTrace();
                    }

                    @Override
                    public void onSuccess(Boolean result) {
                        if(!result) {
                            displayErrorBox("Error", "Unable to Assign Time Slot");
                        }

                        else {
                            ContentContainer.INSTANCE.setContent(new TimeslotScreen());
                        }
                    }
                });

                timeslotCellTable.redraw();

            }
            else{
                timeslotLabel.setText("The time slot is full, please try another time slot");
            }
        }
        else{
            errorLabel.setVisible(true);
        }
    }


    @UiHandler("regionField")
    public void handleRegionChange(ChangeEvent event) {
        String contains = regionField.getItemText(regionField.getSelectedIndex());
        String region = null;
        switch (regionField.getSelectedIndex()) {
            // Select All
            case 0:
                noTimeslotLabel.setText("");
                list.clear();
                for (Student s : origStudentList){
                    if (s.getCountry().contains("China")){
                        list.add(s);
                    }
                }
                region = "Select All";
                countContestant(null, region);
                break;
            // Beijing
            case 1:
                region = "Beijing";

                list.clear();
                for (Student s : origStudentList){
                    if (s.getCountry().contains(contains)){
                        list.add(s);
                    }
                }
                countContestant(null, region);
                break;
            //Shanghai
            case 2:
                region = "Shanghai";
                list.clear();
                for (Student s : origStudentList){
                    if (s.getCountry().contains(contains)){
                        list.add(s);
                    }
                }
                countContestant(null, region);
                break;
            // Wuhan
            case 3:
                region = "Wuhan";
                list.clear();
                for (Student s : origStudentList){
                    if (s.getCountry().contains(contains)){
                        list.add(s);
                    }
                }
                countContestant(null, region);
                break;
            // Dalian
            case 4:
                region = "Dalian";
                list.clear();
                for (Student s : origStudentList){
                    if (s.getCountry().contains(contains)){
                        list.add(s);
                    }
                }
                countContestant(null, region);
                break;
            // Jinan
            case 5:
                region = "Jinan";
                list.clear();
                for (Student s : origStudentList){
                    if (s.getCountry().contains(contains)){
                        list.add(s);
                    }
                }
                countContestant(null, region);
                break;
            // Others
            case 6:
                region = "Others";
                list.clear();
                for (Student s : origStudentList){
                    if (s.getCountry().contains(contains)){
                        list.add(s);
                    }
                }
                countContestant(null, region);
                break;
        }
        provider.getList().clear();
        provider.getList().addAll(list);
    }


    private void countContestant(String country, String region){
        final String region1 = region;
        final String country1 = country;
        userService = UserService.Util.getInstance();

        userService.getListOfStudents(new AsyncCallback<List<Student>>(){

            @Override
            public void onFailure(Throwable caught) {
                caught.printStackTrace();
                noTimeslotLabel.setText("An unexpected error has occurred, please try again later!");
            }

            @Override
            public void onSuccess(List<Student> studentList) {

                if(!countryField.getItemText(countryField.getSelectedIndex()).equals("China")){

                    int counter = 0;
                    for(Student s : studentList) {
                        if(s.getCountry().equals(country1)){
                            if(s.getTimeslot() == 0){
                                counter++;
                            }
                        }
                    }
                    noTimeslotLabel.setText("Number of contestant from " + country1 + " without timeslot: " + counter);

                }

                if(countryField.getItemText(countryField.getSelectedIndex()).equals("China")){
                    if(regionField.getItemText(regionField.getSelectedIndex()).equals("Select All")){
                        int counter = 0;
                        for (Student s : studentList){
                            if (s.getCountry().contains("China")){
                                if (s.getTimeslot() == 0){
                                    counter++;
                                }
                            }
                        }
                        noTimeslotLabel.setText("Number of contestant from China without timeslot: " + counter);
                    }

                    else{
                        int counter = 0;
                        for(Student s : studentList) {
                            if(s.getCountry().substring(6).equals(region1)){
                                if(s.getTimeslot() == 0){
                                    counter++;
                                }
                            }
                        }
                        noTimeslotLabel.setText("Number of contestant from " + region1 + " without timeslot: " + counter);
                    }
                }


            }
        });
    }

    private void displayErrorBox(String errorHeader, String message) {
        final DialogBox errorBox = new DialogBox();
        errorBox.setText(errorHeader);
        final HTML errorLabel = new HTML();
        errorLabel.setHTML(message);
        VerticalPanel verticalPanel = new VerticalPanel();
        verticalPanel.setHorizontalAlignment(VerticalPanel.ALIGN_CENTER);
        final Button closeButton = new Button("Close");
        closeButton.setEnabled(true);
        closeButton.getElement().setId("close");
        closeButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                errorBox.hide();
            }
        });
        verticalPanel.add(errorLabel);
        verticalPanel.add(closeButton);
        errorBox.setWidget(verticalPanel);
        errorBox.center();
    }

    @UiHandler("noTimeSearchButton")
    public void onTimeSearchClick(ClickEvent event){
        List<Student> noTimeSlotList = new ArrayList<Student>();
        List<Student> withoutTimeSlot = new ArrayList<Student>();
        for (Student student : list)
        {
            noTimeSlotList.add(student);
        }
        list.clear();
        for (Student s : noTimeSlotList){
            if (s.getTimeslot() == 0){
                withoutTimeSlot.add(s);
            }
        }
        provider.setList(withoutTimeSlot);
    }

    private void countTimeslot(String timeslot){

        final String timeslot1 = timeslot;

        userService = UserService.Util.getInstance();

        userService.getListOfStudents(new AsyncCallback<List<Student>>() {
            @Override
            public void onFailure(Throwable caught) {
                caught.printStackTrace();
                timeslotLabel.setText("An unexpected error has occurred, please try again later!");
            }

            @Override
            public void onSuccess(List<Student> studentList) {

                int counter=0;
                for(Student s : studentList){

                    if(s.getTimeslot() != 0){

                        Date date = new Date(s.getTimeslot());
                        final String formatedDate = DateTimeFormat.getFormat(DateTimeFormat.PredefinedFormat.DATE_TIME_SHORT).format(date);

                        if(formatedDate.equals(timeslot1)){
                            counter++;
                        }

                    }
                }
                int availableSlot = 300 - counter;
                check =  String.valueOf(availableSlot);
                timeslotLabel.setText("Number of slots available for "+ timeslot1 + ": " + availableSlot);
            }
        });
    }

    @UiHandler("timeslotPageRefresh")
    public void refreshButton(ClickEvent event){
        ContentContainer.INSTANCE.setContent(new TimeslotScreen());
    }

    private String returnLongDateTime(String date){
        String LongDate;
        if(date.equals("2013-10-23 09:00")){
            LongDate = "23 October 2013, 9:00AM to 10:00AM";
            return LongDate;
        }
        else if(date.equals("2013-10-23 10:15")){
            LongDate = "23 October 2013, 10:15AM to 11:15AM";
            return LongDate;
        }

        else if(date.equals("2013-10-23 11:30")){
            LongDate = "23 October 2013, 11:30AM to 12:30PM";
            return LongDate;
        }
        else if(date.equals("2013-10-23 12:45")){
            LongDate = "23 October 2013, 12:45PM to 13:45PM";
            return LongDate;
        }
        else if(date.equals("2013-10-23 14:00")){
            LongDate = "23 October 2013, 14:00PM to 15:00PM";
            return LongDate;
        }
        else if(date.equals("2013-10-23 15:15")){
            LongDate = "23 October 2013, 15:15PM to 16:15PM";
            return LongDate;
        }
        else if(date.equals("2013-10-23 16:30")){
            LongDate = "23 October 2013, 16:30PM to 17:30PM";
            return LongDate;
        }
        else if(date.equals("2013-10-23 17:45")){
            LongDate = "23 October 2013, 17:45PM to 18:45PM";
            return LongDate;
        }
        else if(date.equals("2013-10-23 19:00")){
            LongDate = "23 October 2013, 19:00PM to 20:00PM";
            return LongDate;
        }
        else if(date.equals("2013-10-23 20:15")){
            LongDate = "23 October 2013, 20:15PM to 21.15PM";
            return LongDate;
        }

        else if(date.equals("2013-10-24 14:00")){
            LongDate = "24 October 2013, 14:00PM to 15.00PM";
            return LongDate;
        }

        else if(date.equals("2013-10-24 16:00")){
            LongDate = "24 October 2013, 16:00PM to 17.00PM";
            return LongDate;
        }
        else{
            LongDate = "Error";
            return LongDate;
        }
    }
}
