import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.*;

public class CustomProgressBar extends JFrame{
    private JButton startSimulationButton;
    private JProgressBar progressBar1;
    private JPanel panel;

    private Timer t;

    private static int TIMER_INTERVAL=1000; //in milliseconds, so 1 second
    private static int DEFAULT_FRAME_WIDTH = 500;
    private static int DEFAULT_FRAME_HEIGHT = 100;

    public CustomProgressBar(int maxEpochs) {
        super("Waiting for kohonen to finish");
        setContentPane(this.panel);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(DEFAULT_FRAME_WIDTH, DEFAULT_FRAME_HEIGHT);
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        this.setLocation(dim.width/2-this.getSize().width/2, dim.height/2-this.getSize().height/2);
        setVisible(true);


        progressBar1.setValue(0);
        progressBar1.setStringPainted(true);
        progressBar1.setMinimum(0);
        progressBar1.setMaximum(maxEpochs);
    }
    
    public void setEpoch(int epoch){
    	progressBar1.setValue(epoch);
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
    }

}
