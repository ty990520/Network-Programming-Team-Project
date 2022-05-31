//import java.util.concurrent.locks.Lock;
//import java.util.concurrent.locks.ReentrantLock;
//
///*참고할 코드 - ConcurrentServer에 반영해놨음! */
//public class SharedMemoryAndLock {
//
//    public static void main(String[] args) {
//        final SharedData mySharedData = new SharedData(); // shared resource
//        final Lock lock = new ReentrantLock(); // lock instance
//
//        for (int i = 0; i < 10; i++) {
//            new Thread(new TestRunnable(mySharedData,lock)).start();
//        }
//    }
//
//}
//
//
//
//class TestRunnable implements Runnable {
//    private final SharedData mySharedData;
//    private final Lock lock;
//
//    public TestRunnable(SharedData mySharedData, Lock lock) {
//        this.mySharedData = mySharedData;
//        this.lock = lock;
//    }
//
//    @Override
//    public void run() {
//        try {
//            for (int i = 0; i < 100; i++) {
//                mySharedData.increase();
//            }
//
//            mySharedData.print();
//        } catch (Exception e) {
//            e.printStackTrace();
//        } finally {
//            lock.unlock();
//        }
//    }
//}