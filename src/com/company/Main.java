package cuckoo;

import java.util.Arrays;
import java.io.*;

public class Main {
    public static void main(String[] args) {
        Test test;

        // different parameters
        int[] numofSamples = new int[]{10000};
        int[] bucketAddrSpaces = new int[]{8, 12, 16};
        int[] bucketSizes = new int[]{16, 64, 256};
        int[] maxNumsOfKicks = new int[]{100};
        int[] maxSampleByteLens = new int[]{32};
        int[] fingerprintLens = new int[]{8};

        // brute-force over different parameters to generate output
        File file = new File("res.csv");
        try {
            FileWriter outputfile = new FileWriter(file);

            for (int numOfSample : numofSamples)
                for (int bucketAddressSpace : bucketAddrSpaces)
                    for (int bucketSize : bucketSizes)
                        for (int maxNumOfKicks : maxNumsOfKicks)
                            for (int maxSampleByteLen : maxSampleByteLens)
                                for (int fingerprintLen : fingerprintLens) {
                                    System.out.println("\n\n\n" +
                                            "numOfSample : " + numOfSample + "\n" +
                                            "bucketAddressSpace : " + bucketAddressSpace + "\n" +
                                            "bucketSize : " + bucketSize + "\n" +
                                            "maxNumOfKicks : " + maxNumOfKicks + "\n" +
                                            "maxSampleByteLen : " + maxSampleByteLen + "\n" +
                                            "fingerprintLen : " + fingerprintLen + "\n"
                                    );
                                    test = new Test(numOfSample, bucketAddressSpace, bucketSize,
                                            maxNumOfKicks, maxSampleByteLen, fingerprintLen);
                                    long[] times = test.run();

                                    String[] data = {
                                            String.valueOf(numOfSample),
                                            String.valueOf(bucketAddressSpace),
                                            String.valueOf(bucketSize),
                                            String.valueOf(maxNumOfKicks),
                                            String.valueOf(maxSampleByteLen),
                                            String.valueOf(fingerprintLen),
                                            String.valueOf(times[0] > 0 ? times[0] : -1),
                                            String.valueOf(times[1] > 0 ? times[1] : -1),
                                            String.valueOf(times[2] > 0 ? times[2] : -1),
                                            String.valueOf(times[0] < 0 ? -times[0] : -1),
                                            String.valueOf(times[1] < 0 ? -times[1] : -1),
                                            String.valueOf(times[2] < 0 ? -times[2] : -1),
                                    };
                                    outputfile.write(Arrays.toString(data) + "\n");
                                }
            outputfile.flush();
        } catch (IOException e) {
            e.printStackTrace();
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
    static int fingerprintLen;
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

    private boolean reinsert(byte fingerprint, int remainKicks, int i) {
        if (remainKicks == 0) {
            return false;
        }
        if (buckets[i].insert(fingerprint)) {
            return true;
        }
        int j = getRandomWithinRange(CONFIGS.bucketSize - 1);
        byte newFingerprint = buckets[i].bucket[j];
        int otherIndex = Hash.getOtherIndex(newFingerprint, i);
        buckets[i].bucket[j] = fingerprint;
        return reinsert(newFingerprint, remainKicks - 1, otherIndex);
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
        byte fp = CONFIGS.fingerprintLen == 8 ? (byte) (hashed & 0xFF) :
                CONFIGS.fingerprintLen == 4 ? (byte) (hashed & 0xF) :
                        CONFIGS.fingerprintLen == 2 ? (byte) (hashed & 0x7) : 1;
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

class Test {
    Cuckoo cuckoo;

    public Test(int numOfSamples, int bucketAddressSpace, int bucketSize, int maxNumKicks, int maxSampleBytesLen,
                int fingerprintLen) {
        CONFIGS.bucketAddressSpace = bucketAddressSpace;
        CONFIGS.bucketFullOne = (int) Math.pow(2, bucketAddressSpace) - 1;
        CONFIGS.bucketSize = bucketSize;
        CONFIGS.maxNumKicks = maxNumKicks;
        CONFIGS.numOfSamples = numOfSamples;
        CONFIGS.maxSampleBytesLen = maxSampleBytesLen;
        CONFIGS.fingerprintLen = fingerprintLen;

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

    public long insertAll() {
        long start = System.nanoTime();
        for (int i = 0; i < CONFIGS.numOfSamples; i++) {
            byte[] data = CONFIGS.mainData[i];
            boolean isSuccessful = cuckoo.insert(data);
            if (!isSuccessful) {
                System.out.println("insert was not successful at : " + (i + 1) + "th entry!");
                return -i - 1;
            }
        }
        long end = System.nanoTime();
        System.out.println("insert " + (end - start)  + " ns");
        return end - start;
    }

    public long lookupAll() {
        long start = System.nanoTime();
        for (int i = 0; i < CONFIGS.numOfSamples; i++) {
            byte[] data = CONFIGS.mainData[i];
            boolean isSuccessful = cuckoo.lookup(data);
            if (!isSuccessful) {
                System.out.println("lookup was not successful at : " + (i + 1) + "th entry!");
                return -i - 1;
            }
        }
        long end = System.nanoTime();
        System.out.println("lookup " + (end - start) + " ns");
        return end - start;
    }

    public long removeAll() {
        long start = System.nanoTime();
        for (int i = 0; i < CONFIGS.numOfSamples; i++) {
            byte[] data = CONFIGS.mainData[i];
            boolean isSuccessful = cuckoo.remove(data);
            if (!isSuccessful) {
                System.out.println("remove was not successful  at : " + (i + 1) + "th entry!");
                return -i - 1;
            }
        }
        long end = System.nanoTime();
        System.out.println("remove " + (end - start) + " ns");
        return end - start;
    }

    public long[] run() {
        this.getData();
        long timeInsert = this.insertAll();
        long timeLookup = this.lookupAll();
        long timeRemove = this.removeAll();
        System.out.println("=========================================");
        return new long[]{timeInsert, timeLookup, timeRemove};
    }

    private int getRandomWithinRange(int min, int max) {
        return (int) Math.floor(Math.random() * (max - min + 1) + min);
    }
}

class RandomString {
    static String getAlphaNumericString(int n) {
        String sampleStr = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvxyz";
        StringBuilder sb = new StringBuilder(n);

        for (int i = 0; i < n; i++) {
            int index = (int) (sampleStr.length() * Math.random());
            sb.append(sampleStr
                    .charAt(index));
        }
        return sb.toString();
    }
}