package sample;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.function.Function;

public class Controller implements Initializable {

    @FXML
    private Pane mainPane;

    @FXML
    private Label scrambleLabel, timerLabel;

    @FXML
    private Label sessionLabel1, sessionLabel2;

    @FXML
    private Button buttonRight, buttonLeft;

    @FXML
    private ListView<String> listView;

    @FXML
    private Label ao5Label, ao12Label, ao100Label, bao5Label, bao12Label, bao100Label, meanLabel, medianLabel, standartDerivationLabel, bestLabel, worstLabel, solvesLabel;

    @Override
    public void initialize(URL fxmlFileLocation, ResourceBundle resources) {
        pressed = false;
        Platform.runLater(() -> {
            scrambleLabel.setText(generateScramble(27));
        });
        listView.setOnMouseClicked((event) -> {
            int index = listView.getSelectionModel().getSelectedIndex();
            if (index == -1 || this.currentSession().size() == 0) return;
            this.currentSession().remove(this.currentSession().size() - index - 1);
            saveSessions();
            updateSession();
        });
        loadSessions();
        updateSession();
        buttonLeft.setOnMouseClicked(event -> {
            arrowKeys(new KeyEvent(null, null, null, null,
                    null, KeyCode.LEFT, false, false, false, false));
        });
        buttonRight.setOnMouseClicked(event -> {
            arrowKeys(new KeyEvent(null, null, null, null,
                    null, KeyCode.RIGHT, false, false, false, false));
        });
        mainPane.setOnKeyPressed(event -> {
            if (dfa.current.key.equals("S") && (event.getCode() == KeyCode.RIGHT || event.getCode() == KeyCode.LEFT)) {
                arrowKeys(event);
                return;
            }
            if (dfa.current.key.equals("L")) {
                dfa.read("d");
                return;
            }
            if (event.getCode() == KeyCode.SPACE) {
                if (pressed) {
                    dfa.read("w");
                } else {
                    pressed ^= true;
                    dfa.read("d");
                }
            }
        });
        mainPane.setOnKeyReleased(event -> {
            if (dfa.current.key.equals("P")) {
                dfa.read("u");
                pressed = false;
                return;
            }
            if (event.getCode() == KeyCode.SPACE) {
                if (pressed) {
                    pressed = false;
                    dfa.read("u");
                }
            }
        });
    }

    public Map<Integer, Session> sessions;
    public int activeSession;

    private boolean pressed;
    private long timerStarted;
    private Timer timer;

    public DFA dfa = new DFA(new Node("S", new Tuple<String>("d", "W")), new Node("W", new Tuple<String>("u", "S"), new Tuple<String>("w", "R")),
            new Node("R", new Tuple<String>("u", "L")), new Node("L", new Tuple<String>("d", "P")), new Node("P", new Tuple<String>("u", "S"))) {
        @Override
        public void updateTask() {
            switch (dfa.current.key) {
                case "S":
                    timerLabel.setTextFill(Color.BLACK);
                    break;
                case "W":
                    timerLabel.setText("0.000");
                    timerLabel.setTextFill(Color.BLUE);
                    break;
                case "R":
                    timerLabel.setTextFill(Color.GREEN);
                    break;
                case "L":
                    timerLabel.setTextFill(Color.BLACK);
                    startTimer();
                    break;
                case "P":
                    stopTimer();
                    Platform.runLater(() -> {
                        scrambleLabel.setText(generateScramble(27));
                    });
                    break;
                default:
                    break;
            }
        }
    };


    private void loadSessions() {
        List<String> list = FileUtils.loadFile("cubetimer.sessions");
        this.activeSession = 0;
        if (list.size() > 0)
            this.activeSession = Integer.parseInt(list.remove(0));
        this.sessions = this.explode(list);
    }

    private void saveSessions() {
        FileUtils.saveFile("cubetimer.sessions", this.implode(this.sessions));
    }

    public void updateSession() {
        sessionLabel2.setText(this.currentSession().size() > 0 ? formatTime(this.currentSession().date) : "");
        this.buttonLeft.setDisable(this.activeSession == 0);
        ObservableList<String> list = FXCollections.observableArrayList();
        for (long l : this.currentSession()) {
            list.add(0, "" + formatSolveTime(l));
        }
        Platform.runLater(() -> {
            listView.setItems(list.size() == 0 ? FXCollections.observableArrayList("") : list);
            sessionLabel1.setText("Session " + this.activeSession);
        });
        updateTimes();
    }

    public void updateTimes() {
        //mean
        long mean = -1;
        if (this.currentSession().size() != 0) {
            for (long l : this.currentSession())
                mean += l;
            mean /= this.currentSession().size();
            mean = (this.currentSession().size() > 0 ? mean : -1);
        }
        meanLabel.setText("Mean: " + (mean == -1 ? "NaN" : formatSolveTime(mean)));
        //median
        List<Long> l = new ArrayList<Long>();
        l.addAll(this.currentSession());
        Collections.sort(l);
        long median = (l.size() == 0 ? -1 : l.size() == 1 ? l.get(0) : l.get(l.size() / 2 - 1));
        medianLabel.setText("Median: " + (median == -1 ? "NaN" : formatSolveTime(median)));
        //standart derivation
        long sdev = -1;
        if (this.currentSession().size() != 0) {
            long mean2 = 0;
            for (long temp : this.currentSession())
                mean2 += temp * temp;
            mean2 /= this.currentSession().size();
            sdev = (long) Math.sqrt(mean2 - mean * mean);
        }
        standartDerivationLabel.setText("σ: " + (sdev == -1 ? "NaN" : formatSolveTime(sdev)));
        // aoN
        ao5Label.setText("Ao5: " + (aoN(5) == -1 ? "NaN" : formatSolveTime(aoN(5))));
        ao12Label.setText("Ao12: " + (aoN(12) == -1 ? "NaN" : formatSolveTime(aoN(12))));
        ao100Label.setText("Ao100: " + (aoN(100) == -1 ? "NaN" : formatSolveTime(aoN(100))));
        // baoN
        bao5Label.setText("Best Ao5: " + (bestAoN(5) == -1 ? "NaN" : formatSolveTime(bestAoN(5))));
        bao12Label.setText("Best Ao12: " + (bestAoN(12) == -1 ? "NaN" : formatSolveTime(bestAoN(12))));
        bao100Label.setText("Best Ao100: " + (bestAoN(100) == -1 ? "NaN" : formatSolveTime(bestAoN(100))));
        //worst and best
        long worst = (l.size() == 0 ? -1 : Collections.max(this.currentSession()));
        worstLabel.setText("Worst: " + (worst == -1 ? "NaN" : formatSolveTime(worst)));
        long best = (l.size() == 0 ? -1 : Collections.min(this.currentSession()));
        bestLabel.setText("Best: " + (best == -1 ? "NaN" : formatSolveTime(best)));
        solvesLabel.setText("Solves: " + this.currentSession().size());
    }

    public Session currentSession() {
        return this.sessions.containsKey(this.activeSession) ? this.sessions.get(this.activeSession) : new Session(System.currentTimeMillis());
    }

    public String formatTime(long in) {
        SimpleDateFormat date = new SimpleDateFormat("EEE, d MMM yyyy, h:mm:ss a");
        return date.format(new Date(in));
    }

    private void arrowKeys(KeyEvent event) {
        activeSession = Math.max(0, activeSession + (event.getCode() == KeyCode.RIGHT ? 1 : -1));
        updateSession();
        saveSessions();
    }

    private boolean started;

    public void startTimer() {
        if (started) return;
        started = true;
        this.timer = new Timer();
        this.timerStarted = System.currentTimeMillis();
        this.timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> timerLabel.setText(formatSolveTime()));
            }
        }, 0, 1);
    }

    public void stopTimer() {
        if (!started) return;
        started = false;
        timer.cancel();
        long time = System.currentTimeMillis() - this.timerStarted;
        Platform.runLater(() -> timerLabel.setText(formatSolveTime(time)));
        //Save time
        Session s = currentSession();
        s.add(time);
        this.sessions.put(this.activeSession, s);
        this.updateSession();
        new Thread(() -> saveSessions()).run();
    }

    public static String formatSolveTime(long timeRaw) {
        int minutes = 0;
        while (timeRaw >= 60000) {
            timeRaw -= 60000;
            minutes++;
        }
        String time = "" + timeRaw;
        if (time.length() < 4) {
            while (time.length() < 3)
                time = "0" + time;
            time = "0." + time;
        } else {
            String s1 = time.substring(0, time.length() - 3);
            String s2 = time.substring(time.length() - 3, time.length());
            time = s1 + "." + s2;
        }
        if (minutes > 0) {
            while (time.length() < 6)
                time = "0" + time;
            time = "" + minutes + ":" + time;
        }
        return time;
    }

    private String formatSolveTime() {
        return formatSolveTime(System.currentTimeMillis() - this.timerStarted);
    }

    public static String generateScramble(int lenght) {
        String scramblechars = "";
        Random r = new Random();
        String res0 = "UFLDBR";
        String res1 = "2'";
        while (scramblechars.length() < lenght) {
            int cid = r.nextInt(6);
            if (scramblechars.length() > 0) {
                while (scramblechars.charAt(scramblechars.length() - 1) == res0
                        .charAt(cid))
                    cid = r.nextInt(6);
            }
            if (scramblechars.length() > 1) {
                while (scramblechars.charAt(scramblechars.length() - 1) == res0
                        .charAt(cid)
                        || scramblechars.charAt(scramblechars.length() - 2) == res0
                        .charAt((cid + 3) % 6)
                        || (scramblechars.charAt(scramblechars.length() - 2) == res0
                        .charAt(cid) && scramblechars
                        .charAt(scramblechars.length() - 1) == res0
                        .charAt((cid + 3) % 6))) {
                    cid = r.nextInt(6);
                }
            }
            scramblechars += res0.charAt(cid);
        }
        String scramble = "";
        for (int i = 0; i < scramblechars.length(); i++) {
            char c = scramblechars.charAt(i);
            int a = r.nextInt(3);
            scramble += c + (a > 1 ? "" : "" + res1.charAt(a)) + " ";
        }
        return scramble;
    }

    public long bestAoN(int n) {
        if (n > this.currentSession().size())
            return -1;
        long best = Integer.MAX_VALUE;
        for (int i = 0; i < this.currentSession().size() - n + 1; i++) {
            long aon = aoN(n, i);
            best = (aon < best ? aon : best);
        }
        return best;
    }

    public long aoN(int n, int offset) {
        if (n > this.currentSession().size())
            return -1;
        List<Long> list = new ArrayList<Long>();
        list.addAll(this.currentSession().subList(this.currentSession().size() - offset - n, this.currentSession().size() - offset));
        Collections.sort(list);
        list.remove(0);
        list.remove(list.size() - 1);
        long sum = list.stream().mapToLong(Long::longValue).sum();
        return sum / list.size();
    }

    public long aoN(int n) {
        return aoN(n, 0);
    }

    public List<String> implode(Map<Integer, Session> in) {
        List<String> out = new ArrayList<String>();
        out.add("" + this.activeSession);
        for (int key : in.keySet()) {
            Session s = in.get(key);
            if (s.size() > 0) {
                String linear = "";
                for (long time : s) {
                    linear += (":" + time);
                }
                linear = linear.replaceFirst(":", "");
                out.add("" + key + "," + s.date + "," + linear);
            }
        }
        return out;
    }

    public Map<Integer, Session> explode(List<String> in) {
        Map<Integer, Session> out = new HashMap<Integer, Session>();
        for (String s : in) {
            int key = Integer.parseInt(s.split(",")[0]);
            long date = Long.parseLong(s.split(",")[1]);
            Session session = new Session(date);
            String[] times = s.split(",")[2].split(":");
            for (String time : times) {
                session.add(Long.parseLong(time));
            }
            out.put(key, session);
        }
        return out;
    }

}

