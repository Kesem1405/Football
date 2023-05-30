import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {

    public MainFrame() {
        JFrame frame = new JFrame();
        frame.setSize(1000, 800);
        frame.setResizable(false);
        this.setTitle("LFL - Live football league");
        frame.setDefaultCloseOperation(EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        BackgroundPanel panel = new BackgroundPanel("backgroundVideo.gif");
        frame.add(panel);
        frame.setVisible(true);
        LeagueManager leagueManager = new LeagueManager();
        frame.setContentPane(leagueManager);
        leagueManager.setPreferredSize(new Dimension(800, 600));
        leagueManager.initializeUI();
        leagueManager.revalidate();
        leagueManager.repaint();
    }
}
