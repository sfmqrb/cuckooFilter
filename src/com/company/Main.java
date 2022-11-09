package cuckoo;

import java.util.Arrays;
import java.io.*;
import java.util.Scanner;


public class Main {
    public static void main(String[] args) {
        Test test;
        int[] numofSamples = new int[]{100000, 1000000};
        int[] bucketAddrSpaces = new int[]{12, 16, 20};
        int[] bucketSizes = new int[]{16, 64, 256};
        int[] maxNumsOfKicks = new int[]{100, 200, 400};
        int[] maxSampleByteLens = new int[]{32, 1024};

        for (int numOfSample : numofSamples)
            for (int bucketAddrSpace : bucketAddrSpaces)
                for (int bucketSize : bucketSizes)
                    for (int maxNumofKicks : maxNumsOfKicks)
                        for (int maxSampleByteLen : maxSampleByteLens) {
                            System.out.println("\n\n\n" +
                                    "numOfSample : " + numOfSample + "\n" +
                                    "bucketAddrSpace : " + bucketAddrSpace + "\n" +
                                    "bucketSize : " + bucketSize + "\n" +
                                    "maxNumofKicks : " + maxNumofKicks + "\n" +
                                    "maxSampleByteLen : " + maxSampleByteLen + "\n"
                            );
                            test = new Test(numOfSample, bucketAddrSpace, bucketSize, maxNumofKicks, maxSampleByteLen);
                            test.run();
                        }
    }
}

final class CONFIGS {
    static boolean firstTime = true;
    static byte[][] mainData;
    static int bucketAddressSpace;
    static int bucketFullOne;
    static int bucketSize;
    static int maxNumKicks;
    static int numOfSamples;
    static int maxSampleBytesLen;
}

class Cuckoo {
    Bucket[] buckets;

    public Cuckoo() {
        buckets = new Bucket[(int) Math.pow(2, CONFIGS.bucketAddressSpace)];
        for (int i = 0; i < buckets.length; i++) {
            buckets[i] = new Bucket();
        }
    }

    public boolean lookup(byte[] data) {
        byte fingerprint = Hash.getFingerPrint(data);
        int i1 = Hash.getInitialIndex(data);
        if (buckets[i1].getIndex(fingerprint) != -1) {
            return true;
        }
        int i2 = Hash.getOtherIndex(fingerprint, i1);
        return buckets[i2].getIndex(fingerprint) != -1;
    }

    public boolean insert(byte[] data) {
        byte fingerprint = Hash.getFingerPrint(data);
        int i1 = Hash.getInitialIndex(data);
        if (buckets[i1].insert(fingerprint)) {
            return true;
        }
        int i2 = Hash.getOtherIndex(fingerprint, i1);
        if (buckets[i2].insert(fingerprint)) {
            return true;
        }
        int iRandom = getRandomWithinRange(1) == 0 ? i1 : i2;
        return reinsert(fingerprint, CONFIGS.maxNumKicks, iRandom);
    }

    private boolean reinsert(byte fingerprint, int remainTrial, int i) {
        if (remainTrial == 0) {
            return false;
        }
        if (buckets[i].insert(fingerprint)) {
            return true;
        }
        int j = getRandomWithinRange(CONFIGS.bucketSize - 1);
        byte newFingerprint = buckets[i].bucket[j];
        int otherIndex = Hash.getOtherIndex(newFingerprint, i);
        buckets[i].bucket[j] = fingerprint;
        return reinsert(newFingerprint, remainTrial - 1, otherIndex);
    }

    public boolean remove(byte[] data) {
        byte fingerprint = Hash.getFingerPrint(data);
        int i1 = Hash.getInitialIndex(data);
        if (buckets[i1].remove(fingerprint)) {
            return true;
        }
        int i2 = Hash.getOtherIndex(fingerprint, i1);
        return buckets[i2].remove(fingerprint);
    }

    private int getRandomWithinRange(int max) {
        return (int) Math.floor(Math.random() * (max + 1) + 0);
    }

    public void printBuckets() {
        System.out.println("\nprinting Buckets: ");
        for (int i = 0; i < buckets.length; i++) {
            System.out.print("bucket" + i + " ");
            buckets[i].printBucket();
        }
    }
}

class CuckooSimpleTest {
    public void main() {
        Cuckoo cuckoo = new Cuckoo();
        byte[] data = new byte[]{1, 12};
        System.out.println("lookup false  : " + cuckoo.lookup(data));
        System.out.println("insert true  : " + cuckoo.insert(data));
        cuckoo.printBuckets();
        System.out.println("lookup true  : " + cuckoo.lookup(data));
        System.out.println("remove true  : " + cuckoo.remove(data));
        System.out.println("lookup false : " + cuckoo.lookup(data));
    }
}

class Bucket {
    int bucketSize = CONFIGS.bucketSize;
    byte[] bucket = new byte[this.bucketSize];

    public Bucket() {
        for (int i = 0; i < bucketSize; i++) {
            bucket[i] = (byte) 0;
        }
    }

    public boolean insert(byte fingerprint) {
        for (int i = 0; i < this.bucketSize; i++) {
            if (this.bucket[i] == 0) {
                this.bucket[i] = fingerprint;
                return true;
            }
        }
        return false;
    }

    public boolean remove(byte fingerprint) {
        for (int i = 0; i < this.bucketSize; i++) {
            if (this.bucket[i] == fingerprint) {
                this.bucket[i] = 0;
                return true;
            }
        }
        return false;
    }

    public int getIndex(byte fingerprint) {
        for (int i = 0; i < this.bucketSize; i++) {
            if (this.bucket[i] == fingerprint) {
                return i;
            }
        }
        return -1;
    }

    public void printBucket() {
        System.out.println(Arrays.toString(this.bucket));
    }
}

class Hash {
    static public byte getFingerPrint(byte[] data) {
        int hashed = getHash(data);
        byte fp = (byte) (hashed & 0xFF);
        if (fp == 0) {
            return (byte) (fp + 1);
        }
        return fp;
    }

    static public int getInitialIndex(byte[] data) {
        int hashed = getHash(data);
        return hashed & CONFIGS.bucketFullOne;
    }

    static public int getOtherIndex(byte fingerprint, int curIndex) {
        int hashed = getHash(new byte[]{fingerprint});
        return (hashed ^ curIndex) & CONFIGS.bucketFullOne;
    }

    static private int getHash(byte[] data) {
        int hashed = Arrays.hashCode(data);
        if ((hashed & CONFIGS.bucketFullOne) == 0) {
            return 1;
        }
        return hashed;
    }
}

class HashTest {
    public void main() {
        byte[] data = {1, 5, 4, 32, 33};
        byte fingerprint = Hash.getFingerPrint(data);
        System.out.println("fingerprint: " + fingerprint);
        int initIndex = Hash.getInitialIndex(data);
        System.out.println("init index : " + initIndex);
        int othIndex = Hash.getOtherIndex(fingerprint, initIndex);
        System.out.println("other index: " + othIndex);
        System.out.println("other index: " + Hash.getOtherIndex(fingerprint, othIndex));
    }
}

class Test {
    Cuckoo cuckoo;

    public Test(int numOfSamples, int bucketAddressSpace, int bucketSize, int maxNumKicks, int maxSampleBytesLen) {
        CONFIGS.bucketAddressSpace = bucketAddressSpace;
        CONFIGS.bucketFullOne = (int) Math.pow(2, bucketAddressSpace) - 1;
        CONFIGS.bucketSize = bucketSize;
        CONFIGS.maxNumKicks = maxNumKicks;
        CONFIGS.numOfSamples = numOfSamples;
        CONFIGS.maxSampleBytesLen = maxSampleBytesLen;

        cuckoo = new Cuckoo();
    }

    public void getData() {
        int n = 1000000;
        if (CONFIGS.firstTime) {
            CONFIGS.mainData = new byte[n][];
            try {
                System.out.println("generating data ...");
                for (int i = 0; i < n; i++) {
                    int lenOfByteArr = getRandomWithinRange(3, CONFIGS.maxSampleBytesLen);
                    byte[] data = RandomString.getAlphaNumericString(lenOfByteArr).getBytes("ASCII");
                    CONFIGS.mainData[i] = data;
                }
            } catch (IOException e) {
                System.out.println("An error occurred!");
                e.printStackTrace();
            }
            CONFIGS.firstTime = false;
        }
    }

    public void insertAll() {
        long start = System.currentTimeMillis();
        for (int i = 0; i < CONFIGS.numOfSamples; i++) {
            byte[] data = CONFIGS.mainData[i];
            boolean isSuccessful = cuckoo.insert(data);
            if (!isSuccessful) {
                System.out.println("insert was not successful");
                return;
//                System.exit(1);
            }
//            cuckoo.printBuckets();
        }
        long end = System.currentTimeMillis();
        System.out.println("insert " + (end - start) + " ms");
    }

    public void lookupAll() {
        long start = System.currentTimeMillis();
        for (int i = 0; i < CONFIGS.numOfSamples; i++) {
            byte[] data = CONFIGS.mainData[i];
            boolean isSuccessful = cuckoo.lookup(data);
            if (!isSuccessful) {
                System.out.println("lookup was not successful");
                return;
            }
//            cuckoo.printBuckets();
        }
        long end = System.currentTimeMillis();
        System.out.println("lookup " + (end - start) + " ms");
    }

    public void removeAll() {
        long start = System.currentTimeMillis();
        for (int i = 0; i < CONFIGS.numOfSamples; i++) {
            byte[] data = CONFIGS.mainData[i];
            boolean isSuccessful = cuckoo.remove(data);
            if (!isSuccessful) {
                System.out.println("remove was not successful");
                return;
            }
//            cuckoo.printBuckets();
        }
        long end = System.currentTimeMillis();
        System.out.println("remove " + (end - start) + " ms");
    }

    public void run() {
        this.getData();
        this.insertAll();
        this.lookupAll();
        this.removeAll();
    }

    private int getRandomWithinRange(int min, int max) {
        return (int) Math.floor(Math.random() * (max - min + 1) + min);
    }
}

class RandomString {
    static String getAlphaNumericString(int n) {
        String AlphaNumericString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                + "0123456789"
                + "abcdefghijklmnopqrstuvxyz";

        StringBuilder sb = new StringBuilder(n);

        for (int i = 0; i < n; i++) {
            int index
                    = (int) (AlphaNumericString.length()
                    * Math.random());

            sb.append(AlphaNumericString
                    .charAt(index));
        }

        return sb.toString();
    }
}