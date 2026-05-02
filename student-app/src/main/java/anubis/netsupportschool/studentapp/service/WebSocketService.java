package anubis.netsupportschool.studentapp.service;

import anubis.netsupportschool.studentapp.ExamData;
import anubis.netsupportschool.studentapp.HeadlessStudentApplication;
import anubis.netsupportschool.studentapp.StudentApplication;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages the WebSocket connection to the Spring Boot backend.
 * Protocol (backend BaseMessage / JsonSubTypes):
 *   → REGISTER          student → server (on connect)
 *   → STUDENT_NAME      student → server
 *   → SUBMIT_ANSWERS    student → server
 *   ← LOCK              server  → student
 *   ← UNLOCK            server  → student
 *   ← START_EXAM        server  → student
 *   ← STOP_EXAM         server  → student
 *   ← TEST_LOGIN        server  → student (ignored / no-op)
 */
public class WebSocketService {

    private static final Logger log = Logger.getLogger(WebSocketService.class.getName());

    private final StudentApplication app;
    private WebSocket webSocket;
    private ScheduledExecutorService scheduler;

    private String studentName = null;          // set after STUDENT_NAME exchange
    private boolean connected  = false;

    // Buffer for partial frames
    private final StringBuilder frameBuffer = new StringBuilder();

    // ── Regex helpers (avoids JSON dependency) ───────────────────────────────
    private static final Pattern TYPE_PAT      = Pattern.compile("\"type\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern EXAM_ID_PAT   = Pattern.compile("\"examId\"\\s*:\\s*(\\d+)");
    private static final Pattern TITLE_PAT     = Pattern.compile("\"title\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern DURATION_PAT  = Pattern.compile("\"durationMinutes\"\\s*:\\s*(\\d+)");
    private static final Pattern REQ_NAME_PAT  = Pattern.compile("\"requireNameEntry\"\\s*:\\s*(true|false)");

    public WebSocketService(StudentApplication app) {
        this.app = app;
    }

    // ── Connection management ─────────────────────────────────────────────────

    public synchronized void connect(String ip, int port) {
        if (connected) return;    // already connected; ignore duplicate broadcasts

        String uri = "ws://" + ip + ":" + port + "/websocket";
        log.info("Connecting to " + uri);

        HttpClient client;

        try {
            client = HttpClient.newHttpClient();
            client.newWebSocketBuilder()
                    .buildAsync(URI.create(uri), new WsListener())
                    .thenAccept(ws -> {
                        this.webSocket = ws;
                        this.connected = true;
                        log.info("WebSocket connected.");
                        sendRegister();
                    })
                    .exceptionally(ex -> {
                        log.severe("WebSocket connect failed: " + ex.getMessage());
                        return null;
                    });
        } catch (Exception e) {
            log.severe("WebSocket connect error: " + e.getMessage());
        }
    }

    public void disconnect() {
        connected = false;
        if (scheduler != null) scheduler.shutdownNow();
        if (webSocket != null) {
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "bye");
        }

    }

    // ── Outgoing messages ─────────────────────────────────────────────────────

    /** {type:"REGISTER"} */
    private void sendRegister() {
        send("{\"type\":\"REGISTER\"}");
        app.onRegistered();
    }

    /** {type:"STUDENT_NAME", name:"..."} */
    public void sendStudentName(String name) {
        this.studentName = name;
        send("{\"type\":\"STUDENT_NAME\",\"name\":\"" + escape(name) + "\"}");
    }

    /**
     * {type:"SUBMIT_ANSWERS", examId:X, trigger:"...", answers:[{questionId:Y, selectedChoiceId:Z},...]}
     *
     * @param examId    the exam being submitted
     * @param answers   map of questionId → selectedChoiceId
     * @param trigger   ANSWER_CHANGE | TIME_ENDED | TUTOR_STOPPED
     */
    public void submitAnswers(long examId, Map<Long, Integer> answers, String trigger) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"type\":\"SUBMIT_ANSWERS\"");
        sb.append(",\"examId\":").append(examId);
        sb.append(",\"trigger\":\"").append(trigger).append("\"");
        sb.append(",\"answers\":[");

        boolean first = true;
        for (Map.Entry<Long, Integer> e : answers.entrySet()) {
            if (!first) sb.append(",");
            sb.append("{\"questionId\":").append(e.getKey())
              .append(",\"selectedChoiceId\":").append(e.getValue()).append("}");
            first = false;
        }
        sb.append("]}");
        send(sb.toString());
    }

    public String getStudentName() { return studentName; }

    // ── Incoming message dispatch ─────────────────────────────────────────────

    public void onMessage(String json) {
        log.info("WS received: " + json);

        String type = extract(TYPE_PAT, json);
        if (type == null) return;

        switch (type) {
            case "LOCK":
                app.showLock();
                break;
            case "UNLOCK":
                app.hideLock();
                break;
            case "START_EXAM":
                app.startExam(parseExam(json));
                break;
            case "STOP_EXAM":
                this.studentName = null;
                app.stopExam(true);
                break;
            case "DISCONNECT_ALL":
                // Server is clearing everyone — disconnect cleanly
                this.studentName = null;
                disconnect();
                // Restart UDP discovery to reconnect fresh
                app.restartDiscovery();
                break;
            case "TEST_LOGIN":
                app.testLogin();
                break;
            default:
                log.info("Unhandled message type: " + type);
        }
    }


    // ── Internal helpers ──────────────────────────────────────────────────────

    private void send(String json) {
        if (webSocket == null) return;
        webSocket.sendText(json, true)
                .exceptionally(ex -> { log.severe("Send error: " + ex.getMessage()); return null; });
    }

    private static String extract(Pattern p, String s) {
        Matcher m = p.matcher(s);
        return m.find() ? m.group(1) : null;
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /** Parse a START_EXAM JSON payload into an ExamData object. */
    private ExamData parseExam(String json) {
        long    examId   = Long.parseLong(Objects.requireNonNullElse(extract(EXAM_ID_PAT,  json), "0"));
        String  title    = Objects.requireNonNullElse(extract(TITLE_PAT,    json), "Exam");
        int     duration = Integer.parseInt(Objects.requireNonNullElse(extract(DURATION_PAT, json), "30"));
        boolean reqName  = "true".equals(extract(REQ_NAME_PAT, json));

        List<ExamData.Question> questions = parseQuestions(json);
        return new ExamData(examId, title, duration, reqName, questions);
    }

    /**
     * Minimal hand-rolled parser for the questions array.
     * Handles the structure:
     *   "questions":[{"questionId":1,"text":"...","choices":[{"choiceId":0,"text":"..."},...]},...]
     */
    private List<ExamData.Question> parseQuestions(String json) {
        List<ExamData.Question> list = new ArrayList<>();

        int start = json.indexOf("\"questions\"");
        if (start < 0) return list;

        // Find the opening '[' of the array
        int arrStart = json.indexOf('[', start);
        if (arrStart < 0) return list;

        // Walk through question objects
        int depth = 0;
        int objStart = -1;
        for (int i = arrStart; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') {
                if (depth == 0) objStart = i;
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && objStart >= 0) {
                    String qJson = json.substring(objStart, i + 1);
                    list.add(parseQuestion(qJson));
                    objStart = -1;
                }
            } else if (c == ']' && depth == 0) {
                break;   // end of questions array
            }
        }
        return list;
    }

    private static final Pattern Q_ID_PAT   = Pattern.compile("\"questionId\"\\s*:\\s*(\\d+)");
    private static final Pattern Q_TEXT_PAT = Pattern.compile("\"text\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");

    private ExamData.Question parseQuestion(String qJson) {
        long   id   = Long.parseLong(Objects.requireNonNullElse(extract(Q_ID_PAT,   qJson), "0"));
        String text = Objects.requireNonNullElse(extract(Q_TEXT_PAT, qJson), "");
        text = text.replace("\\\"", "\"").replace("\\\\", "\\");

        List<ExamData.Choice> choices = parseChoices(qJson);
        return new ExamData.Question(id, text, choices);
    }

    private static final Pattern C_ID_PAT   = Pattern.compile("\"choiceId\"\\s*:\\s*(\\d+)");
    private static final Pattern C_TEXT_PAT = Pattern.compile("\"text\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");

    private List<ExamData.Choice> parseChoices(String qJson) {
        List<ExamData.Choice> list = new ArrayList<>();

        int start = qJson.indexOf("\"choices\"");
        if (start < 0) return list;
        int arrStart = qJson.indexOf('[', start);
        if (arrStart < 0) return list;

        int depth = 0, objStart = -1;
        for (int i = arrStart; i < qJson.length(); i++) {
            char c = qJson.charAt(i);
            if (c == '{') {
                if (depth == 0) objStart = i;
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && objStart >= 0) {
                    String cJson = qJson.substring(objStart, i + 1);
                    int    cId   = Integer.parseInt(Objects.requireNonNullElse(extract(C_ID_PAT,   cJson), "0"));
                    String cText = Objects.requireNonNullElse(extract(C_TEXT_PAT, cJson), "");
                    cText = cText.replace("\\\"", "\"").replace("\\\\", "\\");
                    list.add(new ExamData.Choice(cId, cText));
                    objStart = -1;
                }
            } else if (c == ']' && depth == 0) {
                break;
            }
        }
        return list;
    }

    public void clearStudentName() {
        this.studentName = null;
    }

    // ── WebSocket Listener ────────────────────────────────────────────────────

    private class WsListener implements WebSocket.Listener {

        @Override
        public void onOpen(WebSocket ws) {
            log.info("WS onOpen");
            ws.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
            frameBuffer.append(data);
            if (last) {
                String msg = frameBuffer.toString();
                frameBuffer.setLength(0);
                onMessage(msg);
            }
            ws.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket ws, int statusCode, String reason) {
            log.info("WS closed: " + statusCode + " " + reason);
            connected = false;
            return null;
        }

        @Override
        public void onError(WebSocket ws, Throwable error) {
            log.severe("WS error: " + error.getMessage());
            connected = false;
        }
    }
}
