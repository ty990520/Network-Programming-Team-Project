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
                            OutputStream os = socket.getOutputStream();
                            OutputStreamWriter osw = new OutputStreamWriter(os, "euc-kr");
                            PrintWriter pw = new PrintWriter(osw, true);
                            String buffer = null;

                            buffer = br.readLine();

                            if (buffer == null) {
                                System.out.println("[server] closed by client");
                                break;
                            }

                            System.out.println("[요청 처리: " + socket.getRemoteSocketAddress() + ": " + Thread.currentThread().getName() + "]");
                            System.out.println("[server] recieved : " + buffer);

                            DBDriver dbDriver = new DBDriver();

                            if (buffer.equals("1")) {
                                int flag = 0;
                                for (User loginUser : loginUsers) {
                                    if (loginUser.getThreadName().equals(Thread.currentThread().getName())) {   //로그아웃
                                        flag = 1;
                                        // 로그인 사용자 리스트에서 정보 지움
                                        loginUsers.remove(loginUser);
                                        pw.println("[LOGOUT] 로그아웃되었습니다.");
                                        break;
                                    }
                                }
                                if (flag == 0) {   //로그인
                                    pw.println("--------- 로그인 ---------\n사용자 아이디를 입력해주세요.");
                                    String userid = userInput(br);
                                    pw.println("사용자 패스워드를 입력해주세요.");
                                    String password = userInput(br);
                                    boolean login = false;

                                    login = dbDriver.checkPassword(userid, password);
                                    if (login) {
                                        pw.println("[LOGIN] 로그인을 완료하였습니다!");    //클라이언트 코드에서 flag로 로그아웃 제어
                                        dbDriver.DBSelectFindUser(userid, Thread.currentThread().getName());
                                        System.out.println("\t\tid\t|\tuserpw\t|\tage\t|\tphone\t|\tthreadName");
                                        System.out.println("------------------------------------------");
                                        for (User loginUser : loginUsers) {
                                            System.out.println("\t" + loginUser.getUserid() + "\t|\t" + loginUser.getUserpw() + "\t|\t" + loginUser.getAge() + "\t|\t" + loginUser.getPhone() + "\t|\t" + loginUser.getThreadName());
                                        }
                                    } else
                                        pw.println("[FAILURE] 로그인에 실패하였습니다. 다시 시도해주세요.");
                                }


                            } else if (buffer.equals("2")) {
                                pw.println("--------- 회원가입 ---------\n등록할 사용자 아이디를 입력해주세요.");
                                boolean check = true;
                                String userid = "";

                                while (check) {
                                    userid = userInput(br);
                                    if (userid == null) break;

                                    check = dbDriver.checkUserId(userid);

                                    if (check)
                                        pw.println("[FAILURE] 중복된 아이디가 있습니다. 다시 입력해주세요.");
                                    else
                                        break;
                                }
                                pw.println("등록할 사용자 패스워드를 입력해주세요.");
                                String password = userInput(br);
                                if (password == null) break;
                                pw.println("등록할 사용자의 나이를 입력해주세요.");
                                String age = userInput(br);
                                if (age == null) break;
                                pw.println("등록할 사용자 전화번호를 입력해주세요.");
                                String phone = userInput(br);
                                if (phone == null) break;

                                int result = dbDriver.DBInsert(userid, password, age, phone);
                                if (result != 0) {
                                    pw.println("[SUCCESS] 회원가입을 완료하였습니다!");
                                    System.out.println(userid + "님 회원가입 완료");
                                } else pw.println("[FAILURE] 회원가입에 실패하였습니다. 다시 시도해주세요.");
                            } else if (buffer.equals("3")) {
                                pw.println("--------- 전체 프로그램 조회 ---------\n"
                                        + "| 강의 번호\t| 강의명\t\t| 담당 기관\t| 담당 직원\t| 수강가능 연령\t| 신청 인원\t| 수강 인원\t|\n"
                                        + dbDriver.DBSelect() + "\n[1] 프로그램 신청\n[2] 프로그램 검색");
                                buffer = br.readLine();
                                if (buffer.equals("1")) {
                                    int loginFlag = 0;
                                    for (User loginUser : loginUsers) {
                                        /*로그인 안한 사용자 처리해야됨*/
                                        if (loginUser.getThreadName().equals(Thread.currentThread().getName())) { //로그인 사용자
                                            loginFlag = 1;
                                            pw.println("신청을 원하는 프로그램의 강의 번호를 입력해주세요.");
                                            int lectureId = Integer.parseInt(br.readLine());
                                            if (dbDriver.isNotFull(lectureId)) {                                              //검증1 : 프로그램 인원 제한 검증
                                                if (!dbDriver.alreadyRegister(lectureId, loginUser.getUserid())) {            //검증2 : 이미 등록한 사용자 검증
                                                    if (dbDriver.validationRegister(lectureId, loginUser.getAge())) {       //검증3 : 프로그램 신청 조건 만족 여부 검증
                                                        int result = dbDriver.registerLecture(loginUser.getUserid(), lectureId);
                                                        if (result == 1) {
                                                            dbDriver.DBUpdateLectureCnt(1, lectureId);
                                                            pw.println("[SUCCESS] 프로그램이 신청되었습니다.");
                                                        } else {
                                                            pw.println("[FAILURE] 프로그램 신청을 실패하였습니다.");
                                                        }
                                                    } else {
                                                        pw.println("[FAILURE] 해당 프로그램의 신청 조건이 만족되지 않았습니다.");
                                                    }
                                                } else {
                                                    pw.println("[FAILURE] 이미 신청한 프로그램입니다.");
                                                }
                                            } else {
                                                pw.println("[FAILURE] 프로그램 수강 가능 인원이 다 찼습니다.");
                                            }
                                            break;
                                        }
                                    }
                                    if (loginFlag == 0) { //비로그인 사용자
                                        pw.println("[FAILURE] 프로그램 신청을 위해 먼저 로그인해주세요.");
                                    }
                                } else if (buffer.equals("2")) {
                                    //프로그램 검색 코드
                                } else {
                                    pw.println("[FAILURE] 잘못된 입력입니다.");
                                }
                            } else if (buffer.equals("4")) {
                                String userid = "";
                                for (User loginUser : loginUsers) {
                                    if (loginUser.getThreadName().equals(Thread.currentThread().getName())) { //로그인 사용자
                                        userid = loginUser.getUserid();
                                        System.out.println(userid);
                                        break;
                                    }
                                }

                                pw.println("--------- 수강 신청 조회 ---------\n"
                                        + "| 강의 번호\t| 강의명\t\t| 담당 기관\t| 담당 직원\t| 수강가능 연령\t| 신청 인원\t| 수강 인원\t|\n"
                                        + dbDriver.DBSelectByUserId(userid) + "\n[1] 수강 신청 취소\n[2] 나가기");
                                buffer = br.readLine();
                                if (buffer.equals("1")) {
                                    pw.println("취소를 원하는 프로그램의 강의 번호를 입력해주세요.");
                                    int lectureId = Integer.parseInt(br.readLine());

                                    if (dbDriver.alreadyRegister(lectureId, userid)) {
                                        dbDriver.DBUpdateLectureCnt(2, lectureId);
                                        dbDriver.DBDelete(lectureId, userid);
                                        pw.println("[SUCCESS] 프로그램이 취소되었습니다.");
                                    } else
                                        pw.println("[FAILURE] 신청하지 않은 프로그램입니다.");
                                }
                                else if (buffer.equals("2")) {
                                    // 나가기
                                }
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
                    if (userdata.equals("")) {
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
        private final static String URL = "jdbc:mysql://localhost:3306/jdbc?serverTimezone=Asia/Seoul&useSSL=false";
        private final static String USER = "root";
        //        private final static String PASSWORD = "sun009538!@!";
        private final static String PASSWORD = "1234";

        public DBDriver() {
        }

        public String DBSelect() {
            Connection conn = null;
            PreparedStatement pstmt = null;
            Statement stmt = null;
            ResultSet rs = null;
            String result = "";
            try { //Reflection 방식
                conn = DriverManager.getConnection(URL, USER, PASSWORD); // Connection 객체 생성
                stmt = conn.createStatement(); // Statement 객체 생성

                String sql;
                sql = "select * from lecture";
                rs = stmt.executeQuery(sql);

                while (rs.next()) { // ResultSet에 저장된 데이터 얻기 (결과 2개 이상)
                    int lectureId = rs.getInt("lecture_id");
                    String lectureName = rs.getString("lecture_name");
                    String institution = rs.getString("institution");
                    String manager = rs.getString("manager");
                    int minAge = rs.getInt("min_age");
                    int cntParticipant = rs.getInt("cnt_participant");
                    int maxParticipant = rs.getInt("max_participant");

                    result += "| " + lectureId + "\t\t| " + lectureName + "\t| " + institution
                            + "\t| " + manager + "\t| " + minAge + "\t\t| "
                            + cntParticipant + "\t\t| " + maxParticipant + "\t\t|\n";
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
            return result;
        }

        public String DBSelectByUserId(String userid) {
            Connection conn = null;
            PreparedStatement pstmt = null;
            ResultSet rs = null;
            String result = "";
            try { //Reflection 방식
                conn = DriverManager.getConnection(URL, USER, PASSWORD); // Connection 객체

                String sql;

                sql = "select l.lecture_id, l.lecture_name, l.institution, l.manager, l.min_age, l.cnt_participant, l.max_participant " +
                        "from user u, lecture l, registration r " +
                        "where u.user_id = ? " +
                        "and r.user_id = ? " +
                        "and l.lecture_id = r.lecture_id";
                pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, userid);
                pstmt.setString(2, userid);
                rs = pstmt.executeQuery();

                while (rs.next()) { // ResultSet에 저장된 데이터 얻기 (결과 2개 이상)
                    int lectureId = rs.getInt("lecture_id");
                    String lectureName = rs.getString("lecture_name");
                    String institution = rs.getString("institution");
                    String manager = rs.getString("manager");
                    int minAge = rs.getInt("min_age");
                    int cntParticipant = rs.getInt("cnt_participant");
                    int maxParticipant = rs.getInt("max_participant");

                    result += "| " + lectureId + "\t\t| " + lectureName + "\t| " + institution
                            + "\t| " + manager + "\t| " + minAge + "\t\t| "
                            + cntParticipant + "\t\t| " + maxParticipant + "\t\t|\n";
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
                pstmtAndConnClose(conn, pstmt);
            }
            return result;
        }

        User DBSelectFindUser(String userid, String threadName) {
            Connection conn = null; // DB와 연결하기 위한 객체
            PreparedStatement pstmt = null;
            ResultSet rs = null;

            try {
                conn = DriverManager.getConnection(URL, USER, PASSWORD); // Connection 생성

                String sql;
                sql = "select * from user where user_id = ?";
                pstmt = conn.prepareStatement(sql); // PreParedStatement 객체 생성, 객체 생성시 SQL 문장 저장
                pstmt.setString(1, userid);
                rs = pstmt.executeQuery();


                if (rs.next()) {
                    String userpw = rs.getString("password");
                    int age = rs.getInt("age");
                    String phone = rs.getString("user_phone");
                    User loginUser = new User(userid, userpw, age, phone, threadName);
                    loginUsers.add(loginUser);
                } else
                    return null;

            } catch (SQLException e) {
                System.out.println("[SQL Error : " + e.getMessage() + "]");
            } finally {
                // 사용순서와 반대로 close 함
                if (rs != null) {
                    try {
                        rs.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
                pstmtAndConnClose(conn, pstmt);
            }
            return null;
        }

        public int DBInsert(String userid, String password, String age, String phone) {
            Connection conn = null; // DB와 연결하기 위한 객체
            PreparedStatement pstmt = null; // SQL 문을 데이터베이스에 보내기위한 객체
            int result = 0;

            try { //Reflection 방식
                conn = DriverManager.getConnection(URL, USER, PASSWORD); // Connection 생성

                String sql;
                sql = "insert into user(user_id, password, age, user_phone) values(?,?,?,?)";
                pstmt = conn.prepareStatement(sql); // PreParedStatement 객체 생성, 객체 생성시 SQL 문장 저장

                pstmt.setString(1, userid);
                pstmt.setString(2, password);
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

        public int registerLecture(String userid, int lectureId) {
            Connection conn = null; // DB와 연결하기 위한 객체
            PreparedStatement pstmt = null; // SQL 문을 데이터베이스에 보내기위한 객체
            int result = 0;

            try { //Reflection 방식
                conn = DriverManager.getConnection(URL, USER, PASSWORD); // Connection 생성

                String sql;
                sql = "insert into registration(user_id, lecture_id) values(?,?)";
                pstmt = conn.prepareStatement(sql); // PreParedStatement 객체 생성, 객체 생성시 SQL 문장 저장

                pstmt.setString(1, userid);
                pstmt.setInt(2, lectureId);

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

        void DBUpdateLectureCnt(int flag, int lectureId) {
            Connection conn = null; // DB와 연결하기 위한 객체
            PreparedStatement pstmt = null; // SQL 문을 데이터베이스에 보내기위한 객체
            try { //Reflection 방식
                conn = DriverManager.getConnection(URL, USER, PASSWORD); // Connection 생성

                String sql = "";
                if (flag == 1)
                    sql = "update lecture set cnt_participant = cnt_participant+1 where lecture_id = ?";
                else if (flag == 2)
                    sql = "update lecture set cnt_participant = cnt_participant-1 where lecture_id = ?";

                pstmt = conn.prepareStatement(sql); // PreParedStatement 객체 생성, 객체 생성시 SQL 문장 저장

                pstmt.setInt(1, lectureId);

                pstmt.executeUpdate(); // SQL 문장을 실행하고 결과를 리턴 (SQL 문장 실행 후, 변경된 row 수 int type 리턴)
            } catch (SQLException e) {
                System.out.println("[SQL Error : " + e.getMessage() + "]");
            } finally {
                // 사용순서와 반대로 close 함
                pstmtAndConnClose(conn, pstmt);
            }
        }

        void DBDelete(int lectureId, String userid) {
            Connection conn = null; // DB와 연결하기 위한 객체
            PreparedStatement pstmt = null; // SQL 문을 데이터베이스에 보내기위한 객체
            try { //Reflection 방식
                conn = DriverManager.getConnection(URL, USER, PASSWORD); // Connection 생성

                String sql;
                sql = "delete from registration where lecture_id = ? and user_id = ?";
                pstmt = conn.prepareStatement(sql); // PreParedStatement 객체 생성, 객체 생성시 SQL 문장 저장

                pstmt.setInt(1, lectureId);
                pstmt.setString(2, userid);

                int r = pstmt.executeUpdate(); // SQL 문장을 실행하고 결과를 리턴 (SQL 문장 실행 후, 변경된 row 수 int type 리턴)

            } catch (SQLException e) {
                System.out.println("[SQL Error : " + e.getMessage() + "]");
            } finally {
                // 사용순서와 반대로 close 함
                pstmtAndConnClose(conn, pstmt);
            }
        }

        void DBSelectLike(String target, String keyword) {
            Connection conn = null; // DB와 연결하기 위한 객체
            PreparedStatement pstmt = null; // SQL 문을 데이터베이스에 보내기위한 객체
            ResultSet rs = null;
            try { //Reflection 방식
                conn = DriverManager.getConnection(URL, USER, PASSWORD); // Connection 생성

                String sql;
                sql = "select * from lecture where " + target + " like ?";
                pstmt = conn.prepareStatement(sql); // PreParedStatement 객체 생성, 객체 생성시 SQL 문장 저장
                rs = pstmt.executeQuery(sql);
                pstmt.setString(1, "%" + keyword + "%");

                while (rs.next()) { // ResultSet에 저장된 데이터 얻기 (결과 2개 이상)
                    int lectureId = rs.getInt(1);
                    String lectureName = rs.getString(2);
                    String institution = rs.getString(3);
                    String manager = rs.getString(4);
                    int minAge = rs.getInt(5);
                    int cntParticipant = rs.getInt(6);
                    int maxParticipant = rs.getInt(7);
                    System.out.println(lectureId + "\t" + lectureName + "\t" + institution + "\t" + manager + "\t"
                            + minAge + "\t" + cntParticipant + "\t" + maxParticipant);
                }


                int r = pstmt.executeUpdate(); // SQL 문장을 실행하고 결과를 리턴 (SQL 문장 실행 후, 변경된 row 수 int type 리턴)

            } catch (SQLException e) {
                System.out.println("[SQL Error : " + e.getMessage() + "]");
            } finally {
                // 사용순서와 반대로 close 함
                pstmtAndConnClose(conn, pstmt);
            }
        }

        boolean checkUserId(String userid) {
            Connection conn = null; // DB와 연결하기 위한 객체
            PreparedStatement pstmt = null;
            ResultSet rs = null;

            try {
                conn = DriverManager.getConnection(URL, USER, PASSWORD); // Connection 생성

                String sql;
                sql = "select * from user where user_id = ?";
                pstmt = conn.prepareStatement(sql); // PreParedStatement 객체 생성, 객체 생성시 SQL 문장 저장
                pstmt.setString(1, userid);
                rs = pstmt.executeQuery();

                if (rs.next())
                    return true;
                else
                    return false;

            } catch (SQLException e) {
                System.out.println("[SQL Error : " + e.getMessage() + "]");
            } finally {
                // 사용순서와 반대로 close 함
                if (rs != null) {
                    try {
                        rs.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
                pstmtAndConnClose(conn, pstmt);
            }
            return false;
        }

        boolean checkPassword(String userid, String password) {
            Connection conn = null; // DB와 연결하기 위한 객체
            PreparedStatement pstmt = null; // SQL 문을 데이터베이스에 보내기위한 객체
            ResultSet rs = null;
            String dbpassword = "";

            try { //Reflection 방식
                conn = DriverManager.getConnection(URL, USER, PASSWORD); // Connection 생성

                String sql;
                sql = "select * from user where user_id = ?";
                pstmt = conn.prepareStatement(sql); // PreParedStatement 객체 생성, 객체 생성시 SQL 문장 저장

                pstmt.setString(1, userid);
                rs = pstmt.executeQuery();

                if (rs.next()) {
                    dbpassword = rs.getString(2);
                }

            } catch (SQLException e) {
                System.out.println("[SQL Error : " + e.getMessage() + "]");
            } finally {
                // 사용순서와 반대로 close 함
                if (rs != null) {
                    try {
                        rs.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
                pstmtAndConnClose(conn, pstmt);
            }

            if (dbpassword.equals(password))
                return true;
            else
                return false;
        }

        //검증1 - 신청 인원 제한 검증
        boolean isNotFull(int lectureId) {
            Connection conn = null; // DB와 연결하기 위한 객체
            PreparedStatement pstmt = null; // SQL 문을 데이터베이스에 보내기위한 객체
            ResultSet rs = null;

            try { //Reflection 방식
                conn = DriverManager.getConnection(URL, USER, PASSWORD); // Connection 생성

                String sql;
                sql = "SELECT * FROM jdbc.lecture  where lecture_id =? and max_participant>cnt_participant";
                pstmt = conn.prepareStatement(sql); // PreParedStatement 객체 생성, 객체 생성시 SQL 문장 저장

                pstmt.setInt(1, lectureId);
                rs = pstmt.executeQuery();

                if (rs.next()) {
                    return true;    //신청인원 여유 있음
                } else {
                    return false;   //신청인원 꽉 참
                }

            } catch (SQLException e) {
                System.out.println("[SQL Error : " + e.getMessage() + "]");
            } finally {
                // 사용순서와 반대로 close 함
                if (rs != null) {
                    try {
                        rs.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
                pstmtAndConnClose(conn, pstmt);
            }
            return false;
        }

        //검증2 - 프로그램 신청 조건을 만족하는지 검증
        boolean validationRegister(int lectureId, int age) {
            Connection conn = null; // DB와 연결하기 위한 객체
            PreparedStatement pstmt = null; // SQL 문을 데이터베이스에 보내기위한 객체
            ResultSet rs = null;
            String dbpassword = "";

            try { //Reflection 방식
                conn = DriverManager.getConnection(URL, USER, PASSWORD); // Connection 생성

                String sql;
                sql = "select * from lecture where lecture_id = ? and min_age <= ?";
                pstmt = conn.prepareStatement(sql); // PreParedStatement 객체 생성, 객체 생성시 SQL 문장 저장

                pstmt.setInt(1, lectureId);
                pstmt.setInt(2, age);
                rs = pstmt.executeQuery();

                if (rs.next()) {
                    return true;
                } else {
                    return false;
                }

            } catch (SQLException e) {
                System.out.println("[SQL Error : " + e.getMessage() + "]");
            } finally {
                // 사용순서와 반대로 close 함
                if (rs != null) {
                    try {
                        rs.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
                pstmtAndConnClose(conn, pstmt);
            }
            return false;
        }

        //검증3 - 이미 등록한 사용자는 아닌지 검증
        boolean alreadyRegister(int lectureId, String userid) {
            Connection conn = null; // DB와 연결하기 위한 객체
            PreparedStatement pstmt = null; // SQL 문을 데이터베이스에 보내기위한 객체
            ResultSet rs = null;

            try { //Reflection 방식
                conn = DriverManager.getConnection(URL, USER, PASSWORD); // Connection 생성

                String sql;
                sql = "select * from registration where lecture_id =? and user_id = ?";
                pstmt = conn.prepareStatement(sql); // PreParedStatement 객체 생성, 객체 생성시 SQL 문장 저장

                pstmt.setInt(1, lectureId);
                pstmt.setString(2, userid);
                rs = pstmt.executeQuery();

                if (rs.next()) {
                    return true;    //이미 등록한 사용자
                } else {
                    return false;   //신청이 가능한 사용자
                }

            } catch (SQLException e) {
                System.out.println("[SQL Error : " + e.getMessage() + "]");
            } finally {
                // 사용순서와 반대로 close 함
                if (rs != null) {
                    try {
                        rs.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
                pstmtAndConnClose(conn, pstmt);
            }
            return false;
        }
    }


    public static void main(String[] args) {
        startServer();
    }
}

