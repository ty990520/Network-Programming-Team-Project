import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

// concurrent server
// multi-thread / thread poot
// shared memory / Mutual Exclusion(lock)

public class ConcurrentServer {
    public static final int PORT = 9309;

    static ExecutorService executorService; // 스레드풀
    static ServerSocket serverSocket;
    static List<Client> connections = new Vector<Client>();


    static void startServer() { // 서버 시작 시 호출
        final SharedData mySharedData = new SharedData(); // shared resource
        final Lock lock = new ReentrantLock(); // lock instance

        // 스레드풀 생성
        executorService = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors()
        );

        // 서버 소켓 생성 및 바인딩
        try {
            serverSocket = new ServerSocket();
            serverSocket.bind(new InetSocketAddress("localhost", PORT));
        } catch(Exception e) {
            if(!serverSocket.isClosed()) { stopServer(); }
            return;
        }

        // 수락 작업 생성
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                System.out.println("[서버 시작]");
                while(true) {
                    try {
                        // 연결 수락
                        Socket socket = serverSocket.accept();
                        System.out.println("[연결 수락: " + socket.getRemoteSocketAddress()  + ": " + Thread.currentThread().getName() + "]");
                        // 클라이언트 접속 요청 시 객체 하나씩 생성해서 저장
                        Client client = new Client(socket,mySharedData, lock);
                        connections.add(client);
                        System.out.println("[연결 개수: " + connections.size() + "]");
                    } catch (Exception e) {
                        if(!serverSocket.isClosed()) { stopServer(); }
                        break;
                    }
                }
            }
        };
        // 스레드풀에서 처리
        executorService.submit(runnable);
    }

    static void stopServer() { // 서버 종료 시 호출
        try {
            // 모든 소켓 닫기
            Iterator<Client> iterator = connections.iterator();
            while(iterator.hasNext()) {
                Client client = iterator.next();
                client.socket.close();
                iterator.remove();
            }
            // 서버 소켓 닫기
            if(serverSocket!=null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            // 스레드풀 종료
            if(executorService!=null && !executorService.isShutdown()) {
                executorService.shutdown();
            }
            System.out.println("[서버 멈춤]");
        } catch (Exception e) { }
    }

    static class Client {
        Socket socket;
        private final SharedData mySharedData;
        private final Lock lock;

        Client(Socket socket,SharedData mySharedData, Lock lock) {
            this.socket = socket;
            this.mySharedData = mySharedData;
            this.lock = lock;
            receive();
        }

        void receive() {
            // 받기 작업 생성
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        while(true) {
                            byte[] byteArr = new byte[100];
                            InputStream inputStream = socket.getInputStream();

                            // 데이터 read
                            int readByteCount = inputStream.read(byteArr);

                            // 클라이언트가 정상적으로 Socket의 close()를 호출했을 경우
                            if(readByteCount == -1) {  throw new IOException(); }

                            System.out.println("[요청 처리: " + socket.getRemoteSocketAddress() + ": " + Thread.currentThread().getName() + "]");

                            // 문자열로 변환
                            String data = new String(byteArr, 0, readByteCount, "UTF-8");


                            /*여기에 코드 작성하면 됨!!!!!!*/


                            // 클라이언트가 stop server라고 보내오면 서버 종료
                            if(data.equals("stop server"))
                            {
                                stopServer();
                            }

                            // 모든 클라이언트에게 데이터 보냄
                            for(Client client : connections) {
                                client.send(data);
                            }
                        }
                    } catch(Exception e) {
                        try {
                            connections.remove(Client.this);
                            System.out.println("[클라이언트 통신 안됨: " + socket.getRemoteSocketAddress() + ": " + Thread.currentThread().getName() + "]");
                            socket.close();
                        } catch (IOException e2) {}
                    }
                }
            };
            // 스레드풀에서 처리
            executorService.submit(runnable);
        }

        void send(String data) {
            // 보내기 작업 생성
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        // 클라이언트로 데이터 보내기
                        byte[] byteArr = data.getBytes("UTF-8");
                        OutputStream outputStream = socket.getOutputStream();
                        // 데이터 write
                        outputStream.write(byteArr);
                        outputStream.flush();
                    } catch(Exception e) {
                        try {
                            System.out.println("[클라이언트 통신 안됨: " + socket.getRemoteSocketAddress() + ": " + Thread.currentThread().getName() + "]");
                            connections.remove(Client.this);
                            socket.close();
                        } catch (IOException e2) {}
                    }
                }
            };
            // 스레드풀에서 처리
            executorService.submit(runnable);
        }
    }

    public static void main(String[] args) {
        startServer();
    }

}
