package sample;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LogUtils {

    /**
     * 每条 Log 的 tag 输出的最大长度, 超过部分将被截断
     */
    private static final int TAG_MAX_LENGTH = 20;

    /**
     * 每条 Log 的 message 输出的最大长度, 超过部分将被截断
     */
    private static final int MESSAGE_MAX_LENGTH = 1024;

    /**
     * 日期前缀格式化
     */
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MM/dd HH:mm:ss.SSS");

    /**
     * 是否输出到控制台, 默认输出
     */
    private static boolean isOutToConsole = true;

    /**
     * 是否输出到文件
     */
    private static boolean isOutToFile = false;

    /**
     * 日志输出文件, 追加到文件尾
     */
    private static File logOutFile;

    /**
     * 日志文件输出流, 追加到文件尾
     */
    private static RandomAccessFile logOutFileStream;


    public static synchronized void setLogOutFile(File logOutFile) throws IOException {
        LogUtils.logOutFile = logOutFile;
        if (logOutFileStream != null) {
            closeStream(logOutFileStream);
            logOutFileStream = null;
        }

        if (LogUtils.logOutFile != null) {
            try {
                logOutFileStream = new RandomAccessFile(LogUtils.logOutFile, "rw");
                logOutFileStream.seek(LogUtils.logOutFile.length());
            } catch (IOException e) {
                closeStream(logOutFileStream);
                logOutFileStream = null;
                throw e;
            }
        }
    }

    public static void setLogOutTarget(boolean isOutToConsole, boolean isOutToFile) {
        LogUtils.isOutToConsole = isOutToConsole;
        LogUtils.isOutToFile = isOutToFile;
    }

    public static void info(String tag, String message) {
        printLog(Level.INFO, tag, message, false);
    }

    public static void warn(String tag, String message) {
        printLog(Level.WARN, tag, message, false);
    }

    public static void error(String tag, String message) {
        printLog(Level.ERROR, tag, message, true);
    }

    public static void error(String tag, Exception e) {
        if (e == null) {
            error(tag, (String) null);
            return;
        }

        PrintStream printOut = null;

        try {
            ByteArrayOutputStream bytesBufOut = new ByteArrayOutputStream();
            printOut = new PrintStream(bytesBufOut);
            e.printStackTrace(printOut);
            printOut.flush();
            error(tag, new String(bytesBufOut.toByteArray(), "UTF-8"));

        } catch (Exception e1) {
            e1.printStackTrace();

        } finally {
            closeStream(printOut);
        }
    }

    private static void printLog(Level level, String tag, String message, boolean isOutToErr) {
        String log = DATE_FORMAT.format(new Date()) +
                " " +
                level.getTag() +
                "/" +
                checkTextLengthLimit(tag, TAG_MAX_LENGTH) +
                ": " +
                checkTextLengthLimit(message, MESSAGE_MAX_LENGTH);

        if (isOutToConsole) {
            outLogToConsole(isOutToErr, log);
        }
        if (isOutToFile) {
            outLogToFile(log);
        }
    }

    private static void outLogToConsole(boolean isOutToErr, String log) {
        if (isOutToErr) {
            System.err.println(log);
        } else {
            System.out.println(log);
        }
    }

    private static synchronized void outLogToFile(String log) {
        if (logOutFileStream != null) {
            try {
                logOutFileStream.write((log + "\r\n").getBytes("UTF-8"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static String checkTextLengthLimit(String text, int maxLength) {
        if ((text != null) && (text.length() > maxLength)) {
            text = text.substring(0, maxLength - 3) + "...";
        }
        return text;
    }

    private static void closeStream(Closeable stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (Exception e) {

            }
        }
    }

    public static enum Level {
        INFO("INFO", 1), WARN("WARN", 2), ERROR("ERROR", 3);

        private String tag;

        private int levelValue;

        private Level(String tag, int levelValue) {
            this.tag = tag;
            this.levelValue = levelValue;
        }

        public String getTag() {
            return tag;
        }

        public int getLevelValue() {
            return levelValue;
        }
    }

}
