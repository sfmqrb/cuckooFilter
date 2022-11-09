package cuckoo;

import java.util.Arrays;
import java.lang.Integer;


public class Main {
    public static void main(String[] args) {
        System.out.println("Cuckoo");
//        Bucket x = new Bucket();
//        x.insert((byte)2);
        new HashTest().main();
    }
}

final class CONFIGS {
    static int bucketAddressSpace = 16;
    static int bucketFullOne = (int) Math.pow(2, bucketAddressSpace) - 1;
    static int bucketSize = 4;
    static int maxNumKicks = 500;
}

class Cuckoo {
    Bucket[] buckets = new Bucket[(int) Math.pow(2, CONFIGS.bucketAddressSpace)];
    int count = 0;

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
        int iRandom = getRandomWithinRange(0, 1) == 0 ? i1 : i2;
        return reinsert(fingerprint, CONFIGS.maxNumKicks, iRandom);
    }

    private boolean reinsert(byte fingerprint, int remainTrial, int i) {
        if (remainTrial == 0) {
            return false;
        }
        if (buckets[i].insert(fingerprint)) {
            return true;
        }
        int j = getRandomWithinRange(0, CONFIGS.bucketSize - 1);
        byte newFingerprint = buckets[i].bucket[j];
        int otherIndex = Hash.getOtherIndex(newFingerprint, i);
        buckets[i].bucket[j] = fingerprint;
        return reinsert(newFingerprint, remainTrial - 1, otherIndex);
    }

    private int getRandomWithinRange(int min, int max) {
        return (int) Math.floor(Math.random() * (max - min + 1) + min);
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
        this.printBucket();
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
        return (byte) (hashed & 0xF);
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