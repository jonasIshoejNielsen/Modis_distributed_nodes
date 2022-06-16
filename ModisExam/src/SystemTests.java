import java.io.IOException;

public class SystemTests {
    private static String[] controlNodes = new String[]{"7000", "localhost", "7001", "localhost", "7002", "localhost"};
    public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException {
        FontEndNode.useBetterPrints = true;
        ReplicaManager.debug   =false;
        test1();
    }
    public static void test0() throws IOException, ClassNotFoundException, InterruptedException {
        System.out.println("Test 0");
        System.out.println("Expected:");
        System.out.println("7010 send inc expect 1");
        System.out.println("7011 send get expect 1");
        System.out.println("7012 send inc expect 2");
        System.out.println("7013 send inc expect 3");
        System.out.println("7014 send get expect 3");
        System.out.println();
        System.out.println();
        System.out.println("Actual:");

        ReplicaManager replicaManager0 = new ReplicaManager(getArgs(new String[]{controlNodes[0], "0"}));
        ReplicaManager replicaManager1 = new ReplicaManager(getArgs(new String[]{controlNodes[2], "1"}));
        ReplicaManager replicaManager2 = new ReplicaManager(getArgs(new String[]{controlNodes[4], "2"}));
        sleep(1000);


        FontEndNode fontEndNode1 = new FontEndNode(getArgs(new String[]{"inc", "7010"}));
        FontEndNode fontEndNode2 = new FontEndNode(getArgs(new String[]{"get", "7011"}));
        FontEndNode fontEndNode3 = new FontEndNode(getArgs(new String[]{"inc", "7012"}));
        FontEndNode fontEndNode4 = new FontEndNode(getArgs(new String[]{"inc", "7013"}));
        FontEndNode fontEndNode5 = new FontEndNode(getArgs(new String[]{"get", "7014"}));

        sleep(5000);
        replicaManager0.stop();
        replicaManager1.stop();
        replicaManager2.stop();
    }

    public static void test1() throws IOException, ClassNotFoundException, InterruptedException {
        System.out.println("Test 1");
        System.out.println("Expected:");
        System.out.println("7010 send get expect 0");
        System.out.println("7011 send inc expect 1");
        System.out.println("ReplicaManager 0 stops");
        System.out.println("7012 send get expect 1");
        System.out.println("7013 send inc expect 2");
        System.out.println("ReplicaManager 1 stops");
        System.out.println("7014 send inc expect 3");
        System.out.println("7015 send get expect 3");
        System.out.println();
        System.out.println();
        System.out.println("Actual:");

        ReplicaManager replicaManager0 = new ReplicaManager(getArgs(new String[]{controlNodes[0], "0"}));
        ReplicaManager replicaManager1 = new ReplicaManager(getArgs(new String[]{controlNodes[2], "1"}));
        ReplicaManager replicaManager2 = new ReplicaManager(getArgs(new String[]{controlNodes[4], "2"}));
        sleep(1000);

        FontEndNode fontEndNode0 = new FontEndNode(getArgs(new String[]{"get", "7010"}));
        FontEndNode fontEndNode1 = new FontEndNode(getArgs(new String[]{"inc", "7011"}));
        replicaManager0.stop();
        FontEndNode fontEndNode2 = new FontEndNode(getArgs(new String[]{"get", "7012"}));
        FontEndNode fontEndNode3 = new FontEndNode(getArgs(new String[]{"inc", "7013"}));
        replicaManager1.stop();
        FontEndNode fontEndNode4 = new FontEndNode(getArgs(new String[]{"inc", "7014"}));
        FontEndNode fontEndNode5 = new FontEndNode(getArgs(new String[]{"get", "7015"}));

        sleep(5000);
        replicaManager2.stop();
    }

    public static void test2() throws IOException, ClassNotFoundException, InterruptedException {
        System.out.println("Test 2");
        System.out.println("Expected:");
        System.out.println("7010 send get expect 0");
        System.out.println("7011 send inc expect 1");
        System.out.println("ReplicaManager 2 stops");
        System.out.println("7012 send get expect 1");
        System.out.println("7013 send inc expect 2");
        System.out.println("ReplicaManager 1 stops");
        System.out.println("7014 send inc expect 3");
        System.out.println("7015 send get expect 3");
        System.out.println("7010 send get expect 3");
        System.out.println();
        System.out.println();
        System.out.println("Actual:");

        ReplicaManager replicaManager0 = new ReplicaManager(getArgs(new String[]{controlNodes[0], "0"}));
        ReplicaManager replicaManager1 = new ReplicaManager(getArgs(new String[]{controlNodes[2], "1"}));
        ReplicaManager replicaManager2 = new ReplicaManager(getArgs(new String[]{controlNodes[4], "2"}));
        sleep(1000);

        FontEndNode fontEndNode0 = new FontEndNode(getArgs(new String[]{"get", "7010"}));
        FontEndNode fontEndNode1 = new FontEndNode(getArgs(new String[]{"inc", "7011"}));
        replicaManager2.stop();
        FontEndNode fontEndNode2 = new FontEndNode(getArgs(new String[]{"get", "7012"}));
        FontEndNode fontEndNode3 = new FontEndNode(getArgs(new String[]{"inc", "7013"}));
        replicaManager1.stop();
        FontEndNode fontEndNode4 = new FontEndNode(getArgs(new String[]{"inc", "7014"}));
        FontEndNode fontEndNode5 = new FontEndNode(getArgs(new String[]{"get", "7015"}));
        FontEndNode fontEndNode6 = new FontEndNode(getArgs(new String[]{"get", "7010"}));

        sleep(5000);
        replicaManager0.stop();
    }

    //helper methods
    private static void sleep(int sleepAmmount){
        try {
            Thread.sleep(sleepAmmount);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    private static String[] getArgs(String[] args){
        String[] retunArray = new String[args.length+controlNodes.length];
        retunArray = addToArray(retunArray, args, 0);
        retunArray = addToArray(retunArray, controlNodes, args.length);
        return retunArray;
    }
    private static String[] addToArray(String[] retunArray, String[] args, int startIndex){
        for(int i =0; i<args.length; i++){
            retunArray[i+startIndex]=args[i];
        }
        return retunArray;
    }

}
