package sample;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class test extends Application{

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("emailUI.fxml"));
        primaryStage.setTitle("Email");
        primaryStage.setScene(new Scene(root, 300, 700));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);

    }

}






class JProgressBarDemo extends JFrame {
    public JProgressBarDemo(){
        this.setTitle("进度条的使用");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setBounds(100, 100, 250, 100);
        JPanel contentPane=new JPanel();
        contentPane.setBorder(new EmptyBorder(5,5,5,5));
        this.setContentPane(contentPane);
        contentPane.setLayout(new FlowLayout(FlowLayout.CENTER,5,5));
        final JProgressBar progressBar=new JProgressBar();
        progressBar.setStringPainted(true);
        new Thread(() -> {
            for(int i=0;i<=100;i++){
                try{
                    Thread.sleep(100);
                }catch(InterruptedException e){
                    e.printStackTrace();
                }
                progressBar.setValue(i);
            }
            progressBar.setString("升级完成！");
        }).start();
        contentPane.add(progressBar);
        this.setVisible(true);
    }
    public static void main(String[]args){
        new UITest().init();
        //JProgressBarDemo example=new JProgressBarDemo();
    }
}

class UITest{
    private JFrame frame;
    private java.awt.TextArea textArea;
    JProgressBar progressBar;
    //private java.awt.TextField textField;
    private java.awt.Button continueButton;
    private Panel panelFieldButton;
    private java.awt.Button pauseButton;
    private java.awt.Button cancelButton;

    public void init(){
        this.frame = new JFrame("Download test");
        this.continueButton = new java.awt.Button("Continue");
        this.pauseButton = new java.awt.Button("Pause");
        this.cancelButton = new java.awt.Button("Cancel");
        this.panelFieldButton = new Panel();
        this.textArea = new TextArea();
        JPanel contentPane=new JPanel();
        contentPane.setBorder(new EmptyBorder(5,5,5,5));
        this.progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        //this.textField = new java.awt.TextField(50);
        frame.setSize(700,500);
        frame.setLayout(new BorderLayout());
        //panelFieldButton.add(textField);
        panelFieldButton.add(progressBar);
        panelFieldButton.add(pauseButton);
        panelFieldButton.add(continueButton);
        panelFieldButton.add(cancelButton);
        frame.add(panelFieldButton,BorderLayout.SOUTH);
        textArea.setEditable(false);
        frame.add(textArea, BorderLayout.CENTER);
        frame.pack();
        frame.setVisible(true);
        for(int i=90;i<=110;i++){
            try{
                Thread.sleep(100);
            }catch(InterruptedException e){
                e.printStackTrace();
            }
            progressBar.setValue(i);
        }
        progressBar.setString("升级完成！");
    }
}
