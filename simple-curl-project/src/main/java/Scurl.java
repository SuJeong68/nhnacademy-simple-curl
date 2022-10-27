import java.io.*;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

public class Scurl {
    private static final String[] scurlMethod = { "POST", "PUT", "GET", "DELETE", "HEAD" };

    public static void main(String[] args) throws IOException, URISyntaxException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        StringBuilder builder = new StringBuilder();

        // s-curl start
        System.out.println("This is Scurl. ex) scurl http://httpbin.org/get");

        // Input while
        while (true) {
            // readline & split
            System.out.print("$ ");
            String input = bufferedReader.readLine();

            // Check scurl && url
            if (!input.substring(0, input.indexOf(" ")).equals("scurl")
                    || !isValid(input.substring(input.lastIndexOf(" ") + 1))) {
                continue;
            }

            // uri init
            URI uri = new URI(input.substring(input.lastIndexOf(" ") + 1));

            // -X
            builder.append(optionX(uri, input));

            // Host
            builder.append("Host: " + uri.getHost() + "\n");

            // -H
            if (input.contains("-H")) {
                builder.append(optionH(input));
            } else {
                if (builder.toString().startsWith("POST") || builder.toString().startsWith("PUT")) {
                    if (input.contains("-F"))
                        builder.append("Content-Type: multipart/form-data\n");
                    else
                        builder.append("Content-Type: application/x-www-form-urlencoded\n");
                }
            }

            // -d
            if (input.contains("-d")) {
                builder.append(optionD(input));
            }

            // -v
            Boolean check;
            if (input.contains("-v"))   check = true;
            else                        check = false;


            Socket socket = new Socket(uri.getHost(), 80);

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintStream out = new PrintStream(socket.getOutputStream());


            // -F
            if (input.contains("-F")) {
                builder.append(optionF(input));
            }

            System.out.println("==============================\n" + builder);
            out.println(builder);
            out.println();

            // -L
            if (input.contains("-L")) {
                String location = read(check, in);
                optionL(uri.getHost(), location, 0, check);
            } else {
                read(check, in);
            }

            builder.delete(0, builder.length());

        }
    }

    public static boolean isValid(String address) {
        try {
            URI uri = new URI(address);
            return true;
        } catch (URISyntaxException e) {
            return false;
        }
    }

    public static String read(boolean check, BufferedReader in) throws IOException {
        String line, location = null;
        while ((line = in.readLine()) != null) {
            if (line.contains("location:") || line.contains("Location:"))
                location = line.substring(10);
            if (line.contains("OK"))
                location = "OK";

            if (check == true)
                System.out.println(line);
            if (line.equals(""))
                check = true;
        }
        return location;
    }

    public static String optionX(URI uri, String input) {
        String typeX = uri.getPath().substring(1).toUpperCase();

        if (input.contains("-X")) {
            String[] splited = input.substring(input.indexOf("-X") + 3).split(" ");
            return splited[0] + " " + uri.getPath() + " HTTP/1.0\n";
        } else if (Arrays.asList(scurlMethod).contains(typeX)) {
            return typeX + " " + uri.getPath() + " HTTP/1.0\n";
        } else {
            return "GET " + uri.getPath() + " HTTP/1.0\n";
        }
    }

    public static String optionH(String input) {
        String sub = input.substring(input.indexOf("-H") + 4);
        sub = sub.substring(0, sub.indexOf("\""));

        return sub + "\n";
    }

    public static String optionD(String input) {
        String sub = input.substring(input.indexOf("-d") + 5);
        sub = sub.substring(0, sub.indexOf("\\\""));

        return "Content-Length: " + sub.length() + "\n\n" + sub + "\n";
    }

    public static String optionF(String input) {
        String sub = input.substring(input.indexOf("-F") + 4);
        sub = sub.substring(0, sub.lastIndexOf("\""));

        File file = new File(sub.substring(sub.indexOf("@") + 1));
        String header = "Content-Disposition: form-data\n"
                + "name: upload\n"
                + "filename: " + file.getName()
                + "\nContent-Length: " + file.length() + "\n\n";

        try (FileReader reader = new FileReader(file))
        {
            int line;
            while ((line = reader.read()) != -1) {
                header += (char) line;
            }
            header += "\n";
        } catch (IOException e) {
            e.printStackTrace();
        }

        return header;
    }

    public static String optionL(String host, String location, int count, boolean check) throws IOException {
        if (count == 5) {
            return "redirection error!";
        } else if (location == "OK") {
            return "";
        }

        Socket socket = new Socket(host, 80);
        PrintStream out = new PrintStream(socket.getOutputStream());
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        out.println("GET " + location + " HTTP/1.0\n" + "Host: " + host + "\n");
        out.println();

        location = read(check, in);

        return optionL(host, location, ++count, check);
    }
}