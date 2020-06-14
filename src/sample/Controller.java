package sample;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

public class Controller {

    public static Worker.State state = null;

    private static Logger logger = Logger.getLogger("Controller");

    @FXML
    private Button visitButton;

    @FXML
    private Button storeButton;

    @FXML
    private Button searchButton;

    @FXML
    private Tab webpageTab;

    @FXML
    private TextField searchField;

    @FXML
    private WebView webView;

    @FXML
    private Button refreshButton;

    @FXML
    private Tab emailTab;

    @FXML
    private TextField addressField;

    @FXML
    private ProgressBar progressBar;

    @FXML
    private Label stateLabel;

    private WebEngine webEngine;

    public Controller() {
    }

    /*URI uri = URI.create("http://" + addressField.getText());
        Map<String, List<String>> headers = new LinkedHashMap<>();
        headers.put("Set-Cookie", Arrays.asList("name=value"));
        Map temp = java.net.CookieHandler.getDefault().get(uri, headers);
        temp.entrySet().forEach(System.out::println);*/

    @FXML
    void visitButtonClick(ActionEvent event) {
        webEngine = webView.getEngine();
        String url = addressField.getText();
        if (!url.startsWith("http://")) url = "http://" + url;
        webEngine.load(url);
        webEngine.locationProperty().addListener((e)-> addressField.setText(webEngine.getLocation()));
        progressBar.progressProperty().bind(webEngine.getLoadWorker().progressProperty());
        webEngine.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
            state = newValue;
            stateLabel.setText(newValue.toString());
            if (newValue == Worker.State.RUNNING) {
                Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        if (Controller.state == Worker.State.RUNNING){
                            Platform.runLater(() -> stateLabel.setText("time out"));
                        }
                    }
                }, 10000);
            }
            if (newValue == Worker.State.SUCCEEDED) stateLabel.setText("Finish");
;
        });
    }

    @FXML
    void searchButtonClick(ActionEvent event) {
        System.out.println(webView.getEngine().getHistory().getEntries().get(1).getUrl());//可行
    }

    @FXML
    void backButtonClick(ActionEvent event){
        try {
            webView.getEngine().getHistory().go(-1);
        } catch (Exception e){
            logger.info("No back page");
        }
    }

    @FXML
    void forwardButtonClick(ActionEvent event){
        try {
            webView.getEngine().getHistory().go(1);
        } catch (Exception e){
            logger.info("No forward page");
        }
    }

    @FXML
    void refreshButtonClick(ActionEvent event){
        webEngine.reload();
    }

    void init(){
    }
}
