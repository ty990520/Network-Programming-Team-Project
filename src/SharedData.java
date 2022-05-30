public class SharedData {
    private int value;

    public void increase() {
        value += 1;
    }

    public void print() {
        System.out.println(value);
    }
}

/*스레드들이 공유할 데이터
* 수정하면 됨! */