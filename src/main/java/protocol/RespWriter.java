package protocol;

import java.io.PrintWriter;
import java.util.List;

public class RespWriter {
    private final PrintWriter out;

    public RespWriter(PrintWriter out) {
        this.out = out;
    }

    public PrintWriter getRawWriter() {
        return out;
    }

    public void writeSimpleString(String msg) {
        out.print("+" + msg + "\r\n");
        out.flush();
    }

    public void writeError(String error) {
        out.print("-" + error + "\r\n");
        out.flush();
    }

    public void writeInteger(long value) {
        out.print(":" + value + "\r\n");
        out.flush();
    }

    public void writeBulkString(String value) {
        if (value == null) {
            out.print("$-1\r\n");
        } else {
            out.print("$" + value.getBytes().length + "\r\n" + value + "\r\n");
        }
        out.flush();
    }

    public void writeNull() {
        writeBulkString(null);
    }

    public void writeArray(List<String> items) {
        if (items == null) {
            out.print("*-1\r\n");
            out.flush();
            return;
        }

        out.print("*" + items.size() + "\r\n");
        for (String item : items) {
            if (item == null) {
                out.print("$-1\r\n");
            } else {
                out.print("$" + item.getBytes().length + "\r\n" + item + "\r\n");
            }
        }
        out.flush();
    }

    public void writeArrayHeader(int count) {
        out.print("*" + count + "\r\n");
        out.flush();
    }
}