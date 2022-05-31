//import java.io.BufferedReader;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.InputStreamReader;
//import java.io.OutputStream;
//import java.io.OutputStreamWriter;
//import java.io.PrintWriter;
//import java.net.InetAddress;
//import java.net.InetSocketAddress;
//import java.net.ServerSocket;
//import java.net.Socket;
//import java.util.Scanner;
//
//public class TCPServer {
//
//    public static final int PORT = 9309;
//    public static void main(String[] args) {
//        ServerSocket serverSocket = null;
//
//        InputStream is = null;
//        InputStreamReader isr = null;
//        BufferedReader br = null;
//
//        OutputStream os = null;
//        OutputStreamWriter osw = null;
//        PrintWriter pw = null;
//        Scanner sc = new Scanner(System.in);
//
//        try {
//            //1. 서버 소켓 생성
//            serverSocket = new ServerSocket();
//            //2. Bind(SocketAddress(IpAddress + Port)
//
//            InetAddress inetAddress = InetAddress.getLocalHost();
//            String localhost = inetAddress.getHostAddress();
//
//            serverSocket.bind(new InetSocketAddress(localhost, PORT));
//
//            System.out.println("[server] binding " + localhost);
//            //3. accept(클라이언트 연결요청 기다림)
//            Socket socket = serverSocket.accept();
//            InetSocketAddress socketAddress = (InetSocketAddress) socket.getRemoteSocketAddress();
//            System.out.println("[server] connected by client");
//            System.out.println("[server] Connect with " + socketAddress.getHostString() + " " + socket.getPort());
//
//            while(true) {
//                //inputStream 가져와서 StreamReader와 BufferReader로 감싼다
//                is = socket.getInputStream();
//                isr = new InputStreamReader(is, "UTF-8");
//                br = new BufferedReader(isr);
//                //outputStream 가져와서 StreamWriter, PrintWriter로 감싼다
//                os = socket.getOutputStream();
//                osw = new OutputStreamWriter(os, "UTF-8");
//                pw = new PrintWriter(osw, true);
//
//                String buffer = null;
//                buffer = br.readLine(); //Blocking
//                if(buffer == null) {
//                    //normal exit = remote socket close()
//                    System.out.println("[server] closed by client");
//                    break;
//                }
//                System.out.println("[server] recieved : " + buffer);
//                pw.println(buffer);
//            }
//        }catch(IOException e) {
//            e.printStackTrace();
//        } finally {
//            try {
//                if(serverSocket != null && !serverSocket.isClosed())
//                    serverSocket.close();
//            }catch(Exception e) {
//                e.printStackTrace();
//            }
//        }
//        sc.close();
//
//    }
//
//}
