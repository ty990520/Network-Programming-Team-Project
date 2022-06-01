public class User {
    String userid;
    String userpw;
    int age;
    String phone;
    String threadName;

    public User(String userid, String userpw, int age, String phone, String threadName) {
        this.userid = userid;
        this.userpw = userpw;
        this.age = age;
        this.phone = phone;
        this.threadName = threadName;
    }

    public String getUserid() {
        return userid;
    }

    public void setUserid(String userid) {
        this.userid = userid;
    }

    public String getUserpw() {
        return userpw;
    }

    public void setUserpw(String userpw) {
        this.userpw = userpw;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getThreadName() {
        return threadName;
    }

    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }
}
