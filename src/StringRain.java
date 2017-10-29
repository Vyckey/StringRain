import org.jetbrains.annotations.Contract;

import javax.swing.*;
import java.applet.Applet;
import java.applet.AudioClip;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;

public class StringRain extends JDialog {
    public static void main(String[] args) {
        StringRain r = new StringRain();
        r.setVisible(true);
        r.startRain();
    }

    private final static int MAX_RAIN_LINE_NUM = 108;
    private String resPath;
    private boolean isRaining, isColorful, hasMusic;
    private Dimension frameSize;
    private Color bgColor, fgColor;
    private AudioClip music;
    private char[] characters;
    private Font font;
    private int fontSize;
    private RainPanel panel;
    @SuppressWarnings({"rawtypes", "unchecked"})
    private ArrayList<RainLine> rainLines = new ArrayList<>();
    {
        resPath = System.getProperty("user.dir") + "/res/";
        isRaining = isColorful = hasMusic = false;
        frameSize = new Dimension();
        frameSize = Toolkit.getDefaultToolkit().getScreenSize();
        bgColor = Color.black;
        fgColor = Color.green;
        fontSize = 15;
        font = new Font("arial", Font.BOLD, fontSize);
        panel = new RainPanel();
    }

    private StringRain() {
        boolean suc = true;
        String message = "successfully";
        try {
            loadProperties();
            init();
        }
        catch (FileNotFoundException e) {
            suc = false;
            message = "Not found configure file";
        }
        catch (IOException e) {
            suc = false;
            message = "Not found font file";
        }
        catch (Exception e) {
            suc = false;
            message = "exception: " + e;
        }
        if (!suc) {
            JOptionPane.showMessageDialog(null, message,
                    "Failed Tips", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    private Color toColor(String color) {
        if (color == null || color.isEmpty()) return null;
        if (color.startsWith("#"))
            return new Color(Integer.valueOf(color.substring(1), 16));
        if (color.matches("[\\d]+[\\p{Blank}]*,[\\p{Blank}]*[\\d]+[\\p{Blank}]*,[\\p{Blank}]*[\\d]+")) {
            String[] rgb = color.split("[\\p{Blank}]*,[\\p{Blank}]*");
            if (rgb.length != 3) return null;
            return new Color(Integer.parseInt(rgb[0]), Integer.parseInt(rgb[1]), Integer.parseInt(rgb[2]));
        }
        return null;
    }

    private void loadProperties() throws Exception {
        Properties p = new Properties();
        File f = new File(resPath + "configure.properties");
        if (f.exists() && f.isFile()) {
            p.load(new FileInputStream(f));

            String strW = p.getProperty("width", "default").toLowerCase();
            if (!strW.equals("default")) frameSize.width = Integer.parseInt(strW);
            String strH = p.getProperty("height", "default").toLowerCase();
            if (!strH.equals("default")) frameSize.height = Integer.parseInt(strH);

            Color c;
            String strFg = p.getProperty("foreground","default").toLowerCase();
            if (!strFg.equals("default") && (c = toColor(strFg)) != null) {
                fgColor = c;
            }
            String strBg = p.getProperty("background", "default").toLowerCase();
            if (!strBg.equals("default") && (c = toColor(strBg)) != null) {
                bgColor = c;
            }

            String strCf = p.getProperty("colorful", "default").toLowerCase();
            if (!strCf.equals("default")) isColorful = Boolean.parseBoolean(strCf);
            String strMu = p.getProperty("music", "default");
            if (!strMu.equals("default")) {
                File musicFile = new File(resPath + strMu);
                if (musicFile.exists() && musicFile.isFile()) {
                    music = Applet.newAudioClip(musicFile.toURI().toURL());
                    hasMusic = true;
                }
            }

            String strFt = p.getProperty("font-file", "default");
            if (!strFt.equals("default")) {
                font = Font.createFont(Font.TRUETYPE_FONT,
                        getClass().getResourceAsStream(strFt)).deriveFont(Font.BOLD, fontSize);
            }
            String strFs = p.getProperty("font-size", "default");
            if (!strFs.equals("default")) {
                fontSize = Integer.parseInt(strFs);
            }

            String strCs = p.getProperty("characters", "default").toLowerCase();
            if (strCs.equals("default")) {
                characters = new char[126 - 33 + 1];
                for (int i = 0; i < characters.length; i++) {
                    characters[i] = (char)(i + 33);
                }
            }
            else characters = strCs.toCharArray();
        }
    }

    private void init() {
        setTitle("String Rain");
        setAlwaysOnTop(true);
        setSize(frameSize);
        setResizable(false);
        setUndecorated(true);
        BufferedImage cursor = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB_PRE);
        setCursor(Toolkit.getDefaultToolkit().createCustomCursor(cursor, new Point(8, 8), "Disable Cursor"));
        setLocationRelativeTo(null);

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.isAltDown() && e.getKeyCode() == KeyEvent.VK_F4 || e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    setVisible(false);
                    System.exit(0);
                }
            }
        });
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                if (isRaining()) stopRain();
                System.exit(0);
            }
        });
        add(panel, BorderLayout.CENTER);
    }
    private void startRain() {
        if (hasMusic) music.loop();
        for (int i = 0; i < MAX_RAIN_LINE_NUM; i++) {
            addRainLine();
        }
        isRaining = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (isRaining) panel.repaint();
            }
        }).start();
    }
    private void stopRain() {
        isRaining = false;
        if (hasMusic) music.stop();
    }

    private synchronized void addRainLine() {
        RainLine rl = new RainLine((int)(Math.random() * frameSize.width), (int)(Math.random() * -60 * fontSize),
                (int)(Math.random() * 8 + 2));
        rainLines.add(rl);
        new Thread(rl).start();
    }

    private char randomCharacter() {
        return characters[(int) (Math.random() * characters.length)];
    }

    private Dimension getFrameSize() {
        return frameSize;
    }

    public boolean isRaining() {
        return isRaining;
    }

    @Override
    public void setVisible(boolean aFlag) {
        super.setVisible(aFlag);
        if (!aFlag) stopRain();
    }
    private class RainPanel extends JPanel {
        private Color[] colorSet = new Color[] {
                new Color(255, 0, 0),
                new Color(255,165,0),
                new Color(255,255,0),
                new Color(0, 255, 0),
                new Color(0, 127, 0),
                new Color(0, 127, 255),
                new Color(139, 0, 255)};

        @Override
        public void paint(Graphics g) {
            if (!isRaining()) return;
            BufferedImage img = new BufferedImage(frameSize.width, frameSize.height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = (Graphics2D) img.getGraphics();
            g2d.setColor(bgColor);
            g2d.fillRect(0, 0, getFrameSize().width, getFrameSize().height);
            g2d.setColor(fgColor);

            @SuppressWarnings("unchecked")
            Collection<RainLine> crl = (Collection<RainLine>) rainLines.clone();
            for (Iterator<RainLine> it = crl.iterator(); it.hasNext();) {
                RainLine rl = it.next();
                if (rl.isFinished()) {
                    rainLines.remove(rl);
                    addRainLine();
                    continue;
                }
                g2d.setFont(font.deriveFont((float)fontSize));
                char[] cs = rl.getChars();
                int  x = rl.getX(), y = rl.getY() - cs.length * fontSize;
                for (int i = 0; i < cs.length; i++, y -= fontSize) {
                    if (isColorful) g2d.setColor(colorSet[i % colorSet.length]);
                    g2d.drawString(String.valueOf(cs[i]), x, y);
                }
            }
            g.drawImage(img, 0, 0, this);
        }

        private RainPanel() {}
    }

    private class RainLine implements Runnable {
        private final char[] chars;
        private boolean isFinished;
        private int x, y, speed;

        private RainLine(int x, int y, int speed) {
            this.isFinished = false;
            this.x = x;
            this.y = y;
            this.speed = (speed < 1) ? 1 : speed;
            int length = (int) (Math.random() * 40 + 10);
            chars = new char[length];
            for (int i = 0; i < length; i++) {
                chars[i] = randomCharacter();
            }
        }

        @Override
        public void run() {
            while (isRaining() && !isFinished) {
                if (y >= getFrameSize().height + chars.length * fontSize) isFinished = true;
                try {
                    Thread.sleep(speed);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                y += 2;
            }
        }

        private int getX() { return x; }
        private int getY() { return y; }
        private char[] getChars() { return chars; }
        private boolean isFinished() { return isFinished;}
    }
}
