import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import java.sql.*;
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
    static List<User> loginUsers = new Vector<User>();


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

            InetAddress inetAddress = InetAddress.getLocalHost();
            String localhost = inetAddress.getHostAddress();

            serverSocket.bind(new InetSocketAddress(localhost, PORT));

            System.out.println("[서버 시작: " + localhost + "]");
        } catch (Exception e) {
            if (!serverSocket.isClosed()) {
                stopServer();
            }
            return;
        }

        // 수락 작업 생성
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
//                System.out.println("[서버 시작]");
                while (true) {
                    try {
                        // 연결 수락
                        Socket socket = serverSocket.accept();
                        System.out.println("[연결 수락: " + socket.getRemoteSocketAddress() + ": " + Thread.currentThread().getName() + "]");
                        // 클라이언트 접속 요청 시 객체 하나씩 생성해서 저장
                        Client client = new Client(socket, mySharedData, lock);
                        connections.add(client);
                        System.out.println("[연결 개수: " + connections.size() + "]");
                    } catch (Exception e) {
                        if (!serverSocket.isClosed()) {
                            stopServer();
                        }
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
            while (iterator.hasNext()) {
                Client client = iterator.next();
                client.socket.close();
                iterator.remove();
            }
            // 서버 소켓 닫기
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            // 스레드풀 종료
            if (executorService != null && !executorService.isShutdown()) {
                executorService.shutdown();
            }
            System.out.println("[서버 멈춤]");
        } catch (Exception e) {
        }
    }

    static class Client {
        Socket socket;
        private final SharedData mySharedData;
        private final Lock lock;

        Client(Socket socket, SharedData mySharedData, Lock lock) {
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
                        while (true) {
                            InputStream is = socket.getInputStream();
                            InputStreamReader isr = new InputStreamReader(is, "euc-kr");
                            BufferedReader br = new BufferedReader(isr);
                            // outputStream 가져와서 StreamWriter, PrintWriter로 감싼다
                            OutputStream os = socket.getOutputStream();
                            OutputStreamWriter osw = new OutputStreamWriter(os, "euc-kr");
                            PrintWriter pw = new PrintWriter(osw, true);
                            // String intro = "\n : 줄 띄어쓰기 \n 안녕하세요";
                            String buffer = null;
                            int temp;

                            buffer = br.readLine();

                            if (buffer == null) {
                                System.out.println("[server] closed by client");
                                break;
                            }

                            System.out.println("[요청 처리: " + socket.getRemoteSocketAddress() + ": " + Thread.currentThread().getName() + "]");

                            //----------------------------------
                            System.out.println("[server] recieved : " + buffer);


                            //----------------------------------
                            if (buffer.equals("1")) {
                                pw.println("---------로그인---------");
                            } else if (buffer.equals("2")) {
                                pw.println("---------회원가입---------\n등록할 사용자 아이디를 입력해주세요. >> ");
                                String userid = userInput(br);
                                if (userid == null) break;
                                pw.println("등록할 사용자 패스워드를 입력해주세요. >> ");
                                String userpw = userInput(br);
                                if (userpw == null) break;
                                pw.println("등록할 사용자의 나이를 입력해주세요. >> ");
                                String age = userInput(br);
                                if (age == null) break;
                                pw.println("등록할 사용자 전화번호를 입력해주세요. >> ");
                                String phone = userInput(br);
                                if (phone == null) break;
                                DBDriver dbDriver = new DBDriver();
                                int result = dbDriver.DBInsert(userid, userpw, age, phone);
                                if (result != 0) {
                                    pw.println("회원가입을 완료하였습니다!");
                                    System.out.println(userid+"님 회원가입 완료");
                                } else pw.println("회원가입에 실패하였습니다. 다시 시도해주세요.");
                            } else if (buffer.equals("3")) {
                                System.out.println("메뉴 3번을 눌렀습니다.");

                            } else if (buffer.equals("4")) {
                                System.out.println("메뉴 4번을 눌렀습니다.");

                            } else if (buffer.equals("5")) {
                                System.out.println("메뉴 5번을 눌렀습니다.");
                            }


                            // 모든 클라이언트에게 데이터 보냄
//                            for (Client client : connections) {
//                                client.send(data);
//                            }
                        }
                    } catch (Exception e) {
                        try {
                            connections.remove(Client.this);
                            System.out.println("[클라이언트 통신 안됨: " + socket.getRemoteSocketAddress() + ": " + Thread.currentThread().getName() + "]");
                            socket.close();
                        } catch (IOException e2) {
                        }
                    }
                }

                private String userInput(BufferedReader br) throws IOException {
                    String userdata = br.readLine();
                    if (userdata == null) {
                        System.out.println("회원가입을 취소합니다.");
                        return null;
                    }
                    return userdata;
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
                        byte[] byteArr = data.getBytes("euc-kr");
                        OutputStream outputStream = socket.getOutputStream();
                        // 데이터 write
                        outputStream.write(byteArr);
                        outputStream.flush();
                    } catch (Exception e) {
                        try {
                            System.out.println("[클라이언트 통신 안됨: " + socket.getRemoteSocketAddress() + ": " + Thread.currentThread().getName() + "]");
                            connections.remove(Client.this);
                            socket.close();
                        } catch (IOException e2) {
                        }
                    }
                }
            };
            // 스레드풀에서 처리
            executorService.submit(runnable);
        }
    }

    public static class DBDriver {
        private final static String URL = "jdbc:mysql://192.168.205.63:3306/jdbc?serverTimezone=Asia/Seoul&useSSL=false";
        private final static String USER = "newuser";
        private final static String PASSWORD = "1234";

        public DBDriver() {
        }

        void DBSelect() {
            Connection conn = null;
            Statement stmt = null;
            ResultSet rs = null;
            try { //Reflection 방식
                conn = DriverManager.getConnection(URL, USER, PASSWORD); // Connection 객체 생성
                stmt = conn.createStatement(); // Statement 객체 생성

                String sql;
                sql = "select * from user";
                rs = stmt.executeQuery(sql);

                while (rs.next()) { // ResultSet에 저장된 데이터 얻기 (결과 2개 이상)
                    String userId = rs.getString("user_id");
                    String password = rs.getString("password");
                    int age = rs.getInt("age");
                    String userPhone = rs.getString("user_phone");
                    System.out.println(userId + "\t" + password + "\t" + age + "\t" + userPhone);
                }

//                 if(rs.next()) { // ResultSet에 저장된 데이터 얻기 (결과 1개)
//
//                 }
//                 else {
//
//                 }

            } catch (SQLException e) {
                System.out.println("SQL Error : " + e.getMessage());
            } finally {
                // 사용순서와 반대로 close 함
                if (rs != null) {
                    try {
                        rs.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
                if (stmt != null) {
                    try {
                        stmt.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
                if (conn != null) {
                    try {
                        conn.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        public int DBInsert(String userid, String userpw, String age, String phone) {
            Connection conn = null; // DB와 연결하기 위한 객체
            PreparedStatement pstmt = null; // SQL 문을 데이터베이스에 보내기위한 객체
            int result = 0;

            try { //Reflection 방식
                conn = DriverManager.getConnection(URL, USER, PASSWORD); // Connection 생성

                String sql;
                sql = "insert into user(user_id, password, age, user_phone) values(?,?,?,?)";
                pstmt = conn.prepareStatement(sql); // PreParedStatement 객체 생성, 객체 생성시 SQL 문장 저장

                pstmt.setString(1, userid);
                pstmt.setString(2, userpw);
                pstmt.setInt(3, Integer.parseInt(age));
                pstmt.setString(4, phone);

                result = pstmt.executeUpdate(); // SQL 문장을 실행하고 결과를 리턴 (SQL 문장 실행 후, 변경된 row 수 int type 리턴)

            } catch (SQLException e) {
                System.out.println("[SQL Error : " + e.getMessage() + "]");
            } finally {
                // 사용순서와 반대로 close 함
                pstmtAndConnClose(conn, pstmt);
            }
            return result;
        }

        private void pstmtAndConnClose(Connection conn, PreparedStatement pstmt) {
            if (pstmt != null) {
                try {
                    pstmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }

        void DBUpdate() {
            Connection conn = null; // DB와 연결하기 위한 객체
            PreparedStatement pstmt = null; // SQL 문을 데이터베이스에 보내기위한 객체
            try { //Reflection 방식
                conn = DriverManager.getConnection(URL, USER, PASSWORD); // Connection 생성

                String sql;
                sql = "update user set password=?, age=?, user_phone=? where user_id=?";
                pstmt = conn.prepareStatement(sql); // PreParedStatement 객체 생성, 객체 생성시 SQL 문장 저장

                pstmt.setString(1, "user123");
                pstmt.setString(2, "9876");
                pstmt.setInt(3, Integer.parseInt("24"));
                pstmt.setString(4, "010-3323-9876");

                int r = pstmt.executeUpdate(); // SQL 문장을 실행하고 결과를 리턴 (SQL 문장 실행 후, 변경된 row 수 int type 리턴)

            } catch (SQLException e) {
                System.out.println("[SQL Error : " + e.getMessage() + "]");
            } finally {
                // 사용순서와 반대로 close 함
                pstmtAndConnClose(conn, pstmt);
            }
        }

        void DBDelete() {
            Connection conn = null; // DB와 연결하기 위한 객체
            PreparedStatement pstmt = null; // SQL 문을 데이터베이스에 보내기위한 객체
            try { //Reflection 방식
                conn = DriverManager.getConnection(URL, USER, PASSWORD); // Connection 생성

                String sql;
                sql = "delete from user where id=?";
                pstmt = conn.prepareStatement(sql); // PreParedStatement 객체 생성, 객체 생성시 SQL 문장 저장

                pstmt.setString(1, "user1234");

                int r = pstmt.executeUpdate(); // SQL 문장을 실행하고 결과를 리턴 (SQL 문장 실행 후, 변경된 row 수 int type 리턴)

            } catch (SQLException e) {
                System.out.println("[SQL Error : " + e.getMessage() + "]");
            } finally {
                // 사용순서와 반대로 close 함
                pstmtAndConnClose(conn, pstmt);
            }
        }
    }

    public static void main(String[] args) {
        startServer();
    }
}

