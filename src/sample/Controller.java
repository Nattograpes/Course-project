package sample;

import com.intellij.ide.ui.EditorOptionsTopHitProvider;
import com.intellij.refactoring.util.RadioUpDownListener;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.layout.Background;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebEvent;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.apache.commons.io.FilenameUtils;


import javax.net.ssl.*;
import javax.swing.*;
import java.awt.*;
import java.awt.TextArea;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.*;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

public class Controller implements Initializable {

    public static Worker.State state = null;

    private static final String tag = "Controller";

    public TextField emailAddressField;
    public TextField receiverField;
    public TextField titleField;
    public Button loginButton;
    public PasswordField passwordField;
    public Button sendButton;
    public javafx.scene.control.TextArea inf;
    public javafx.scene.control.TextArea msgArea;
    public Menu collectionMenu;
    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;
    private volatile boolean isLogin = true;
    private String senderEmail;
    private String password;
    private String text;
    private String receiverEmail;
    private String title;
    private String loginState = "登录成功";
    private String sendState = "发送成功";

    private File colFile;

    private ArrayList<String> collectionList;

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

    private ExecutorService threadPool;

    public static boolean firstDownload = true;

    private CookieStore cookieStore;

    public Controller() {
        this.threadPool = Executors.newFixedThreadPool(10);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        //日志相关
        try {
            LogUtils.setLogOutFile(new File("log.txt"));
            LogUtils.setLogOutTarget(true,true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //收藏夹相关
        this.colFile = new File("./src/data/collection.txt");
        this.collectionList = new ArrayList<>();
        //Cookie相关
        CookieManager cookieManager = new CookieManager();
        CookieHandler.setDefault(cookieManager);
        this.cookieStore = cookieManager.getCookieStore();
        //SSL相关
        TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustManager() {
            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }
            @Override
            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }
            @Override
            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
        }
        };
        // Install the all-trusting trust manager
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e){
            LogUtils.error(tag,e);
        }
        // Create all-trusting host name verifier
        HostnameVerifier allHostsValid = new HostnameVerifier() {
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        };

        // Install the all-trusting host verifier
        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        LogUtils.info(tag, "Initialized success");
    }


    @FXML
    void visitButtonClick(ActionEvent event) throws IOException {
        AtomicBoolean timeout = new AtomicBoolean(false);
        webEngine = webView.getEngine();
        String url = addressField.getText();
        if (!url.startsWith("http://")) url = "http://" + url;
        webEngine.load(url);
        webEngine.locationProperty().addListener((observable, oldValue, newValue) -> {
            addressField.setText(webEngine.getLocation());
            try {
                initStoreButton();
            } catch (IOException e) {
                LogUtils.error(tag, "InitStoreButtonFailed");
                LogUtils.error(tag, e);
            }
            getCookie();
            LogUtils.info(tag, "Visit:" + webEngine.getLocation());
        });
        initStoreButton();
        progressBar.progressProperty().bind(webEngine.getLoadWorker().progressProperty());
        webEngine.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
            String oldLocation = webEngine.getLocation();
            state = newValue;
            stateLabel.setText(newValue.toString());
            switch (newValue) {
                case RUNNING:
                    Timer timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            if (Controller.state == Worker.State.RUNNING && oldLocation == webEngine.getLocation()) {
                                Platform.runLater(() -> {
                                    try {
                                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                                        alert.setTitle("Information");
                                        alert.setHeaderText("Time out");
                                        alert.setContentText("Connection timed out, please try again.");
                                        alert.show();
                                        File file = new File("./src/html/timeout.html");
                                        timeout.set(true);
                                        webEngine.load(file.toURI().toString());
                                        LogUtils.warn(tag, "Time out");
                                    } catch (Exception e) {
                                        LogUtils.error(tag,e);
                                    }
                                });
                            }
                        }
                    }, 10000);
                    break;

                case SUCCEEDED: {
                    if (timeout.get()) {
                        stateLabel.setText("Time out");
                    } else stateLabel.setText("Finish");
                    timeout.set(false);
                    break;
                }

                case CANCELLED: {
                    String[] strings = {"doc", "xls", "zip", "exe", "rar", "pdf", "jar"};
                    for (String str : strings) {
                        if (FilenameUtils.getExtension(webEngine.getLocation()).equals(str)) {
                            stateLabel.setText("Downloading");
                            threadPool.submit(new DownloadThread(webEngine.getLocation()));
                            break;
                        }
                    }
                }
                break;
            }
        });
        LogUtils.info(tag,"Visit-button clicked");
    }


    @FXML
    void searchButtonClick(ActionEvent event) throws IOException {
        AtomicBoolean timeout = new AtomicBoolean(false);
        webEngine = webView.getEngine();
        String word = searchField.getText();
        String url = "http://www.baidu.com/s?wd=" + word;
        webEngine.load(url);
        webEngine.locationProperty().addListener((observable, oldValue, newValue) -> {
            addressField.setText(webEngine.getLocation());
            try {
                initStoreButton();
            } catch (IOException e) {
                LogUtils.error(tag, "InitStoreButtonFailed");
                LogUtils.error(tag, e);
            }
            LogUtils.info(tag, "Visit:" + webEngine.getLocation());
        });
        initStoreButton();
        progressBar.progressProperty().bind(webEngine.getLoadWorker().progressProperty());
        webEngine.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
            String oldLocation = webEngine.getLocation();
            state = newValue;
            stateLabel.setText(newValue.toString());
            switch (newValue) {
                case RUNNING:
                    Timer timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            if (Controller.state == Worker.State.RUNNING && oldLocation == webEngine.getLocation()) {
                                Platform.runLater(() -> {
                                    try {
                                        Platform.runLater(() -> {
                                            Alert alert = new Alert(Alert.AlertType.INFORMATION);
                                            alert.setTitle("Information");
                                            alert.setHeaderText("Time out");
                                            alert.setContentText("Connection timed out, please try again.");
                                            alert.show();
                                        });
                                        File file = new File("./src/html/timeout.html");
                                        timeout.set(true);
                                        webEngine.load(file.toURI().toString());
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    stateLabel.setText("time out");
                                });
                            }
                        }
                    }, 10000);
                    break;

                case SUCCEEDED: {
                    if (timeout.get()) {
                        stateLabel.setText("Time out");
                    } else stateLabel.setText("Finish");
                    timeout.set(false);
                    break;
                }

                case CANCELLED: {
                    String[] strings = {"doc", "xls", "zip", "exe", "rar", "pdf", "jar"};
                    for (String str : strings) {
                        if (FilenameUtils.getExtension(webEngine.getLocation()).equals(str)) {
                            stateLabel.setText("Downloading");
                            threadPool.submit(new DownloadThread(webEngine.getLocation()));
                            break;
                        }
                    }
                    break;
                }
            }
        });
        searchField.setText("");
        addressField.setText(webEngine.getLocation());
        LogUtils.info(tag,"Search-button clicked");
    }


    @FXML
    void backButtonClick(ActionEvent event) {
        try {
            webView.getEngine().getHistory().go(-1);
        } catch (Exception e) {

        }
        LogUtils.info(tag,"Back-button clicked");
    }

    @FXML
    void forwardButtonClick(ActionEvent event) {
        try {
            webView.getEngine().getHistory().go(1);
        } catch (Exception e) {

        }
        LogUtils.info(tag,"Forward-button clicked");
    }

    @FXML
    void refreshButtonClick(ActionEvent event) throws IOException {
        webEngine.reload();
        initStoreButton();
        LogUtils.info(tag,"Refresh-button clicked");
    }


    public void initStoreButton() throws IOException {
        String address = webEngine.getLocation();
        address = address.replaceAll("https://", "").replaceAll("http://", "");
        address = address.substring(0, address.length() - 1);
        if (!address.startsWith("www")) address = "www." + address;
        getCollection();
        if (collectionList.contains(address)) storeButton.setText("取消收藏");
        else storeButton.setText("收藏");
        collectionList.clear();
    }


    /**
     * ------------------------------------------------------------------------
     * Email
     * DZETRGFHLPWGPGDC
     */


    @FXML
    void emailItemClick(Event event) throws IOException {
        Stage stage = new Stage();
        Parent root = FXMLLoader.load(getClass().getResource("emailUI.fxml"));
        stage.setTitle("Email");
        stage.setScene(new Scene(root, 300, 700));
        stage.show();
        LogUtils.info(tag,"Email-item clicked");
    }


    private void login() {
        inf.appendText("正在登陆...\r\n");
        senderEmail = emailAddressField.getText();
        password = String.valueOf(passwordField.getText());
        loginState = tryLogin();
        if (!loginState.equals("登录成功")) {
            try {
                logout();
                isLogin = false;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void logout() throws IOException {
        //中断连接
        writer.println("rset");
        writer.flush();
        System.out.println(reader.readLine());
        writer.println("quit");
        writer.flush();
        System.out.println(reader.readLine());
    }

    private String tryLogin() {
        try {
            String msg;

            String userBase64 = Base64Util.EncodeBase64(senderEmail.getBytes());
            String passwordBase64 = Base64Util.EncodeBase64(password.getBytes());

            //发送端主机名
            writer.println("helo " + senderEmail);
            writer.flush();
            msg = reader.readLine();
            inf.appendText(msg + "\r\n");
            System.out.println(msg);

            //设置auth
            writer.println("auth login");
            writer.flush();
            msg = reader.readLine();
            inf.appendText(msg + "\r\n");
            System.out.println(msg);

            writer.println(userBase64);
            writer.flush();
            msg = reader.readLine();
            inf.appendText(msg + "\r\n");
            System.out.println(msg);

            writer.println(passwordBase64);
            writer.flush();
            msg = reader.readLine();
            inf.appendText(msg + "\r\n");
            System.out.println(msg);

            //发送端邮箱
            writer.println("mail from:<" + senderEmail + ">");
            writer.flush();
            msg = reader.readLine();
            inf.appendText(msg + "\r\n");
            System.out.println(msg);
            if (!msg.substring(0, 3).equals("235")) {
                System.out.println("fail");
                loginState = "登录失败";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return loginState;
    }


    public void loginClick(ActionEvent actionEvent) {
        try {
            this.socket = new Socket("smtp.163.com", 25);
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.writer = new PrintWriter(socket.getOutputStream(), true);
            login();
            if (!isLogin) {
                inf.appendText("登录失败，请重新尝试\r\n");
                passwordField.setText("");
                emailAddressField.setText("");
            } else {
                inf.appendText("登录成功\r\n");
                initToSendUI();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    private void initToSendUI() {
        passwordField.setEditable(false);
        passwordField.setDisable(true);
        emailAddressField.setEditable(false);
        emailAddressField.setDisable(true);
        inf.appendText("请输入邮件内容\r\n");
        loginButton.setDisable(true);
        sendButton.setDisable(false);
        receiverField.setDisable(false);
        receiverField.setEditable(true);
        msgArea.setDisable(false);
        msgArea.setEditable(true);
        titleField.setEditable(true);
        titleField.setDisable(false);
    }

    private void initToLoginUI() {
        msgArea.setText("");
        emailAddressField.setEditable(true);
        emailAddressField.setDisable(false);
        passwordField.setEditable(true);
        passwordField.setDisable(false);
        receiverField.setEditable(true);
        receiverField.setDisable(true);
        receiverField.setText("");
        msgArea.setDisable(true);
        msgArea.setEditable(false);
        loginButton.setDisable(false);
        sendButton.setDisable(true);
        titleField.setEditable(false);
        titleField.setText("");
        titleField.setDisable(true);
        inf.appendText("已自动退出，请重新登录\r\n");
    }

    public void sendClick(ActionEvent actionEvent) {
        send();
    }

    public void send() {
        receiverEmail = receiverField.getText();
        title = titleField.getText();
        text = msgArea.getText();
        senderEmail = emailAddressField.getText();
        password = String.valueOf(passwordField.getText());
        inf.appendText(sendState = trySend() + "\r\n");
        initToLoginUI();
    }


    private String trySend() {
        try {
            String msg;
            //接收端邮箱
            writer.println("rcpt to:<" + receiverEmail + ">");
            writer.flush();
            msg = reader.readLine();
            inf.appendText(msg + "\r\n");
            System.out.println(msg);

            //传输
            writer.println("data");
            writer.flush();
            msg = reader.readLine();
            inf.appendText(msg + "\r\n");
            System.out.println(msg);

            //抬头
            writer.println("subject:" + title);
            writer.println("from:" + senderEmail);
            writer.println("to:" + receiverEmail);
            writer.println();

            //内容主体
            writer.println(text);
            System.out.println(text);
            writer.flush();
            msg = reader.readLine();
            inf.appendText(msg + "\r\n");
            System.out.println(msg);

            //.结束
            writer.println(".");
            writer.flush();
            msg = reader.readLine();
            System.out.println(msg);

            writer.println("rset");
            writer.flush();
            msg = reader.readLine();
            System.out.println();
            writer.println("quit");
            writer.flush();
            msg = reader.readLine();
            System.out.println(msg);
            if (!msg.substring(0, 3).equals("221")) {
                sendState = "发送失败";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sendState;
    }

    /**
     * ---------------------------------------------------------------
     * 收藏
     */
    public void getCollection() throws IOException {
        String address;
        collectionList.clear();
        BufferedReader reader = new BufferedReader(new FileReader(colFile));
        while ((address = reader.readLine()) != null) {
            collectionList.add(address);
        }
        reader.close();
    }


    @FXML
    public void storeButtonClick(ActionEvent event) throws IOException {
        if (storeButton.getText().equals("收藏")) {
            String address = webEngine.getLocation();
            address = address.replaceAll("https://", "").replaceAll("http://", "");
            address = address.substring(0, address.length() - 1);
            if (!address.startsWith("www")) address = "www." + address;
            FileWriter writer = new FileWriter(colFile, true);
            writer.write("\r\n" + address);
            writer.flush();
            writer.close();
            storeButton.setText("取消收藏");
        } else {
            getCollection();
            String address = webEngine.getLocation();
            address = address.replaceAll("https://", "").replaceAll("http://", "");
            address = address.substring(0, address.length() - 1);
            if (!address.startsWith("www")) address = "www." + address;
            collectionList.remove(address);
            colFile.delete();
            colFile.createNewFile();
            FileWriter writer = new FileWriter(colFile);
            int i = 0;
            for (; i < (collectionList.size() - 1); i++) {
                writer.write(collectionList.get(i) + "\r\n");
            }
            writer.write(collectionList.get(i));
            writer.flush();
            writer.close();
            storeButton.setText("收藏");
        }
        LogUtils.info(tag,"Store-button clicked");
    }


    private void addCollectionItem() {
        collectionMenu.getItems().clear();
        for (String str : collectionList) {
            MenuItem item = new MenuItem(str);
            item.setOnAction((e) -> {
                addressField.setText(str);
                visitButton.fire();
            });
            collectionMenu.getItems().add(item);
        }
    }

    private void loadCollection() {
        try {
            String address;
            BufferedReader reader = new BufferedReader(new FileReader(colFile));
            while ((address = reader.readLine()) != null) {
                collectionList.add(address);
            }
            reader.close();
            addCollectionItem();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void menuShowing(Event event) {
        try {
            collectionList.clear();
            loadCollection();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * ------------------------------------------------------------------
     * Download
     */


    static class DownloadThread implements Callable {
        private JFrame frame;
        private java.awt.TextArea textArea;
        JProgressBar progressBar;
        private java.awt.Button continueButton;
        private Panel panelFieldButton;
        private java.awt.Button pauseButton;
        private java.awt.Button cancelButton;


        private static final int CANCEL = 0;
        private static final int PAUSE = 1;
        private static final int CONTINUE = 2;
        private static final int START = 3;
        private static final int FINISH = 4;
        private static final int FALSE = 5;
        private static final int PROGRESSBAR_MAX = 100;

        private RandomAccessFile outRAFile;
        private BufferedInputStream inputStream;
        private URL url;
        private File file;

        private volatile int taskState;
        private long dataLength;
        private volatile long offset;

        private boolean first = false;
        private int flag = 1;

        public DownloadThread(String urlString) {
            try {
                synchronized ((Object) firstDownload) {
                    if (firstDownload) {
                        firstDownload = false;
                        this.url = new URL(urlString);
                        this.file = new File("./" + FilenameUtils.getName(url.getPath()));
                        first = true;
                    } else {
                        firstDownload = true;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public Object call() {
            if (first) {
                int i = 1;
                while (file.exists()) {
                    file = new File("./" + FilenameUtils.getBaseName(url.getPath()) + "(" + i + ")." + FilenameUtils.getExtension(url.getPath()));
                    i++;
                }
                LogUtils.info(tag, "Download:" + file.getName());
                initDownloadUI();
                download();
            }
            return null;
        }

        private void initDownloadUI() {
            this.frame = new JFrame("Download test");
            this.continueButton = new java.awt.Button("Continue");
            this.pauseButton = new java.awt.Button("Pause");
            this.cancelButton = new java.awt.Button("Cancel");
            this.panelFieldButton = new Panel();
            this.textArea = new TextArea();
            this.progressBar = new JProgressBar();
            progressBar.setStringPainted(true);
            //this.textField = new java.awt.TextField(50);
            frame.setSize(700, 500);
            frame.setLayout(new BorderLayout());
            //panelFieldButton.add(textField);
            panelFieldButton.add(progressBar);
            panelFieldButton.add(pauseButton);
            panelFieldButton.add(continueButton);
            panelFieldButton.add(cancelButton);
            frame.add(panelFieldButton, BorderLayout.SOUTH);
            textArea.setEditable(false);
            frame.add(textArea, BorderLayout.CENTER);
            frame.pack();
            frame.setVisible(true);

            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    taskState = CANCEL;
                }
            });

            pauseButton.addActionListener(e -> {
                flag = 1;
                taskState = PAUSE;
            });

            continueButton.addActionListener(e -> {
                taskState = CONTINUE;
            });

            cancelButton.addActionListener(e -> {
                taskState = CANCEL;
            });
        }

        private Object download() {
            try {
                URLConnection connection = url.openConnection();
                this.outRAFile = new RandomAccessFile(file, "rw");
                this.inputStream = new BufferedInputStream(connection.getInputStream());

                dataLength = connection.getContentLengthLong();
                offset = 0;
                taskState = START;

                new Thread(new progressBarUITask()).start();

                while (taskState != CANCEL && taskState != FINISH && taskState != FALSE) {
                    if (taskState == START) {
                        textArea.append("Downloading " + FilenameUtils.getName(url.getPath()) + "\r\n");
                        taskState = startDownloadTask();
                    } else if (taskState == PAUSE) {
                        if (flag == 1) {
                            textArea.append("Download paused\r\n");
                            flag++;
                        }
                        Thread.sleep(1000);
                    } else if (taskState == CONTINUE) {
                        textArea.append("Download continued\r\n");
                        taskState = startDownloadTask();
                    }
                }


                if (taskState == FINISH) {
                    progressBar.setValue(100);
                    textArea.append("Download success\r\n");
                    LogUtils.info(tag, FilenameUtils.getName(file.getPath()) + " download success");
                } else if (taskState == CANCEL) {
                    progressBar.setValue(0);
                    outRAFile.close();
                    inputStream.close();
                    if (file.exists()) {
                        file.delete();
                    }
                    offset = 0;
                    textArea.append("Download canceled\r\n");
                    LogUtils.info(tag, FilenameUtils.getName(file.getPath()) + " download cancel");
                } else if (taskState == FALSE) {
                    textArea.append("Download failed\r\n");
                    LogUtils.info(tag, FilenameUtils.getName(file.getPath()) + " download fail");
                }
                inputStream.close();
                outRAFile.close();

                return null;
            } catch (Exception e) {
                LogUtils.error(tag, e);
            } finally {
                try {
                    if (taskState == FALSE) {
                        outRAFile.close();
                        inputStream.close();
                        if (file.exists()) {
                            file.delete();
                        }
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            return null;
        }

        private int startDownloadTask() throws IOException {
            try {
                while (offset < dataLength) {
                    if (taskState == CONTINUE) {
                        outRAFile.seek(offset);
                        outRAFile.write(inputStream.readNBytes(1024));
                        offset += 1024;
                    } else if (taskState == START) {
                        outRAFile.seek(0);
                        outRAFile.write(inputStream.readNBytes(1024));
                        offset += 1024;
                        taskState = CONTINUE;
                    } else if (taskState == PAUSE) {
                        return PAUSE;
                    } else if (taskState == CANCEL) {
                        progressBar.setValue(0);
                        return CANCEL;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            outRAFile.close();
            inputStream.close();
            if (taskState != PAUSE && taskState != CANCEL && offset < dataLength) {
                return FALSE;
            } else {
                return FINISH;
            }
        }

        class progressBarUITask implements Runnable {

            @Override
            public void run() {
                while (taskState != CANCEL && taskState != FINISH && taskState != FALSE) {
                    Dimension d = progressBar.getSize();
                    Rectangle rect = new Rectangle(0, 0, d.width, d.height);
                    progressBar.setValue((int) (((float)offset / (float)dataLength) * 100));
                    progressBar.paintImmediately(rect);
                    try {
                        Thread.sleep(700);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void getCookie(){
        for(HttpCookie cookie : cookieStore.getCookies()){
            System.out.println(cookie.getDomain() + ": " + cookie.getName() + ": " + cookie.getValue());
        }
        System.out.println();
    }

}
