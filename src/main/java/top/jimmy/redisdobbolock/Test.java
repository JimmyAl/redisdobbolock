package top.jimmy.redisdobbolock;


public class Test {

    public static void main(String[] a){
        Service service = new Service();
        for (int i = 0; i < 50; i++ ){
            ThreadA threadA = new ThreadA(service);
            threadA.start();
        }
    }
}
