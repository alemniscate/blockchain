package blockchain;

import java.security.*;
import java.util.*;
import java.util.concurrent.*;
import java.io.*;

public class Main {
    private final static int POOL_SIZE = 1;
    private final static int NUMBER_OF_TASKS = 20;

    public static void main(String[] args) {
//        String fileName = "doc.txt";
        
//        Random random = new Random(17L);
        Random random = new Random();
        Transaction tr = new Transaction();
/*
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter how many zeros the hash must start with:");
        String input = scanner.nextLine();
        int zeros = Integer.parseInt(input);
        scanner.close();
*/
        GenerateKeys gk = new GenerateKeys(1024);
        gk.createKeys(); 

        Blockchain bc = Blockchain.getBlockchain();
        bc.setZeros(0);

        ExecutorService executor = Executors.newFixedThreadPool(POOL_SIZE);
        List<Future<Boolean>> futureList = new ArrayList<>();

        for (int i = 0; i < NUMBER_OF_TASKS; i++) {
            int taskNumber = i + 1;
            Future<Boolean> future = executor.submit(() -> {
                long timeStamp = new Date().getTime(); 
                int id = taskNumber;
                tr.award(id);

                String msg = tr.generate();
                byte[] signature = Message.sign(msg, gk.getPrivateKey());
                if (!VerifyMessage.verifySignature(msg, signature, gk.getPublicKey())) {
                    System.out.println("message error");
                }              
   
                String doc = "Block:\n";
                doc += "Created by miner" + taskNumber  + "\n";
                doc += "miner" + taskNumber + " gets 100 VC\n";
                doc += "Id: " + id + "\n";
                doc += "Timestamp: " + timeStamp + "\n";
                doc += "Magic number: " + Math.abs(random.nextInt()) + "\n";
                bc.generate(doc, msg);
    
                if (!bc.validate()) {
                    System.out.println("fail " + id);
                    return false;
                }
                return true;
            });
            futureList.add(future);
        }

        executor.shutdown();

        for (Future<Boolean> future: futureList) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                e.getMessage();
            }
        }
/* 
        File file = new File(fileName);
        if (file.exists()) {
            bc = Serialize.deSerialize(fileName);
        }
*/

//        Serialize.serialize(bc, fileName);

        bc.print(15);
    }
}

class Block implements Serializable {
    private static final long serialVersionUID = 1L;

    private String doc;
    private String hash;
    private long time;
    private int zeros;
    private String previous;
    private int increment;
    private String data;

    Block(String doc, String hash, long time, int zeros, String previous, int increment, String data) {
        this.doc = doc;
        this.hash = hash;
        this.time = time;
        this.zeros = zeros;
        this.previous = previous;
        this.increment = increment;
        this.data = data;
    }

    String getDoc() {
        return doc;
    }

    String getHash() {
        return hash;
    }

    long getTime() {
        return time;
    }

    int getZeros() {
        return zeros;
    }

    String getPrevious() {
        return previous;
    }

    int getIncrement() {
        return increment;
    }

    String getData() {
        return data;
    }

    void setDoc(String doc) {
        this.doc = doc;
    }

    void setData(String data) {
        this.data = data;
    }

    void setHash(String hash) {
        this.hash = hash;
    } 

    void setTime(long time) {
        this.time = time;
    }

    void setZeros(int zeros) {
        this.zeros = zeros;
    }

    void setPrevious(String previous) {
        this.previous = previous;
    }

    void setIncrement(int increment) {
        this.increment = increment;
    }
}

class Blockchain implements Serializable {
    private static final long serialVersionUID = 1L;

    private static Blockchain blockchain;
    private List<Block> chain;
    private int zeros = 0;
    private String hash = "0";
    private int counter = 0;

    private Blockchain(int zeros) {
        chain = new ArrayList<>();
        setZeros(zeros);
    }

    static Blockchain getBlockchain() {
        if (blockchain == null) {
            blockchain = new Blockchain(0);
        } 
        return blockchain;
    }

    int getZeros() {
        return zeros;
    }

    void setZeros(int zeros) {
        this.zeros = zeros;
    }

    String makePrefix(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            sb.append('0');
        }
        return sb.toString();
    }

    int calcIncrement() {
        counter++;
        if (counter % 5 == 0) {
            return 0;
        }
        if (counter % 3 == 0) {
            return -1;
        }
        return 1;
    }

    String generate(String doc, String data) {

        String previous = this.hash;
        long start = System.currentTimeMillis();
        String prefix = makePrefix(zeros);
        hash = prefix + StringUtil.applySha256(doc);
        hash = hash.substring(0, 64);
        long end = System.currentTimeMillis();
        long time = end - start;
        int increment = calcIncrement();
        zeros += increment;
        chain.add(new Block(doc, hash, time, zeros, previous, increment, data));
        return hash;
    }

    boolean validate() {
        for (Block block: chain) {
            int currentZeros = block.getZeros() - block.getIncrement();
            String prefix = makePrefix(currentZeros);
            String hash = prefix + StringUtil.applySha256(block.getDoc());
            hash = hash.substring(0, 64);
            if (!block.getHash().equals(hash)) {
                return false;
            }
        }
        return true;
    }

    void print() {
        print(chain.size()); 
    }
   
    void print(int n) {
        for (int i = 0; i < Math.min(n, chain.size()); i++) {
            Block block = chain.get(i);
            System.out.print(block.getDoc());
            System.out.println("Hash of the previous block:");
            System.out.println(block.getPrevious());
            System.out.println("Hash of the block:");
            System.out.println(block.getHash());
            System.out.println("Block data:");
            System.out.print(block.getData());
            System.out.println(String.format("Block was generating for %d mili seconds", block.getTime()));
            switch (block.getIncrement()) {
                case 1:
                    System.out.println("N was increased to " + block.getZeros());
                    break;
                case 0:
                    System.out.println("N stays the same");
                    break;
                case -1:
                    System.out.println("N was decreased by 1");
                    break;
            }
            System.out.println();
        }
    }

}

class StringUtil {
    /* Applies Sha256 to a string and returns a hash. */
    public static String applySha256(String input){
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            /* Applies sha256 to our input */
            byte[] hash = digest.digest(input.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte elem: hash) {
                String hex = Integer.toHexString(0xff & elem);
                if(hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        }
        catch(Exception e) {
            throw new RuntimeException(e);
        }
    }
}

class Serialize {

    static void serialize(Blockchain bc, String fileName) {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(fileName);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(bc);
            objectOutputStream.flush();
            objectOutputStream.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    static Blockchain deSerialize(String fileName) {
        try {
            FileInputStream fileInputStream = new FileInputStream(fileName);
            ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
            Blockchain bc = (Blockchain) objectInputStream.readObject();
            objectInputStream.close();
            return bc;
        } catch (IOException | ClassNotFoundException e) {
            System.out.println(e.getMessage());
            return null;
        }   
    }
}

class Message {
   
    static byte[] sign(String data, PrivateKey privateKey) throws InvalidKeyException, Exception{
        Signature rsa = Signature.getInstance("SHA1withRSA"); 
        rsa.initSign(privateKey);
        rsa.update(data.getBytes());
        return rsa.sign();
    }
}

class VerifyMessage {

    static boolean verifySignature(String msg, byte[] signature, PublicKey publicKey) throws Exception {
        byte[] data = msg.getBytes();
        Signature sig = Signature.getInstance("SHA1withRSA");
        sig.initVerify(publicKey);
        sig.update(data);
        return sig.verify(signature);
    }
}

class GenerateKeys {

    private KeyPairGenerator keyGen;
    private KeyPair pair;
    private PrivateKey privateKey;
    private PublicKey publicKey;

    GenerateKeys(int keylength) {
        try {
            this.keyGen = KeyPairGenerator.getInstance("RSA");
            this.keyGen.initialize(keylength);
        } catch (NoSuchAlgorithmException e) {
            System.out.println(e.getMessage());
        }
    }

    void createKeys() {
        this.pair = this.keyGen.generateKeyPair();
        this.privateKey = pair.getPrivate();
        this.publicKey = pair.getPublic();
    }

    PrivateKey getPrivateKey() {
        return this.privateKey;
    }

    PublicKey getPublicKey() {
        return this.publicKey;
    }
}

class User{

    private String name;
    private long balance;

    User(String name, long balance) {
        this.name = name;
        this.balance = balance;
    }

    User(String name) {
        this.name = name;
        balance = 100;
    }

    String getName() {
        return name;
    }

    long getBalance() {
        return balance;
    }

    void setName(String name) {
        this.name = name;
    }

    void setBalance(long balance) {
        this.balance = balance;
    }

    void sentMoneyTo(User other, long money) {
        long trans = Math.min(money, balance);
        other.addMoney(trans);
        subMoney(trans); 
    }

    void addMoney(long money) {
        balance += money;
    }

    void subMoney(long money) {
        balance -= money;
    }
}

class Transaction {

    List<User> users;
    Random random;

    Transaction() {
        random = new Random();

        users = new ArrayList<>();
        users.add(new User("miner1"));
        users.add(new User("miner2"));
        users.add(new User("miner3"));
        users.add(new User("miner4"));
        users.add(new User("miner5"));
        users.add(new User("miner6"));
        users.add(new User("miner7"));
        users.add(new User("miner8"));
        users.add(new User("miner9"));
        users.add(new User("miner10"));
        users.add(new User("miner12"));
        users.add(new User("miner13"));
        users.add(new User("miner14"));
        users.add(new User("miner15"));
        users.add(new User("miner16"));
        users.add(new User("miner17"));
        users.add(new User("miner18"));
        users.add(new User("miner19"));
        users.add(new User("miner20"));
        users.add(new User("Alice"));
        users.add(new User("Bob"));
        users.add(new User("CarShop"));
        users.add(new User("Director1"));
        users.add(new User("Eva"));
        users.add(new User("FastFood"));
        users.add(new User("Green"));
        users.add(new User("Hulu"));
        users.add(new User("Inco"));
        users.add(new User("Johe"));
        users.add(new User("Kate"));
        users.add(new User("Lucy"));
        users.add(new User("MobileShop"));
        users.add(new User("Nick"));
        users.add(new User("OrangeShop"));
        users.add(new User("PerlShop"));
        users.add(new User("QuantumComputer"));
        users.add(new User("RealEstate"));
        users.add(new User("SmartComputer"));
        users.add(new User("TigerShop"));
        users.add(new User("University"));
        users.add(new User("VirtualReality"));
        users.add(new User("Wowwow"));
        users.add(new User("XeonServer"));
        users.add(new User("YelloSubmarine"));
        users.add(new User("Zoo"));
    }

    void award(int id) {
        User a = users.get(id - 1);
        a.addMoney(100);
    }

    String generate() {
        String msgs = "";
        int n = Math.abs(random.nextInt(5)) + 1;
        for (int i = 0; i < n; i++) {
            msgs += getnerateOne();
        }
        return msgs;
    }

    String getnerateOne() {
        boolean okFlag = false;
        String msg = "";
        while (!okFlag) {
            int i1 = Math.abs(random.nextInt(users.size() - 1));
            int i2 = Math.abs(random.nextInt(users.size() - 1));
            int money = Math.abs(random.nextInt(30));
            if (i1 != i2) {
                if (users.get(i1).getBalance() >= money) {
                    okFlag = true;
                    User a = users.get(i1);
                    User b = users.get(i2);
                    a.sentMoneyTo(b, money);
                    msg = String.format("%s sent %d VC to %s\n", a.getName(), money, b.getName());
                }                            
            }
        }
        return msg;
    }
}