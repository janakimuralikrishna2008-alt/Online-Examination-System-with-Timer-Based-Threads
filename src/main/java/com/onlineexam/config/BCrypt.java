package com.onlineexam.config;

import java.security.SecureRandom;

/**
 * BCrypt password-hashing system based on OpenBSD Blowfish password hashing.
 * Pure Java implementation with zero external dependencies.
 * Fully compatible with standard BCrypt '$2a$' format.
 */
public class BCrypt {

    private static final int BCRYPT_SALT_LEN = 16;
    private static final int BLOWFISH_NUM_ROUNDS = 16;

    // P-box and S-boxes initialization tables
    private static final int[] P_ORIG = {
            0x243f6a88, 0x85a308d3, 0x13198a2e, 0x03707344, 0xa4093822, 0x299f31d0,
            0x082efa98, 0xec4e6c89, 0x452821e6, 0x38d01377, 0xbe5466cf, 0x34e90c6c,
            0xc0ac29b7, 0xc97c50dd, 0x442d504b, 0x3454b9ab, 0x074d2fe1, 0x768a701f
    };

    private static final int[] S_ORIG = {
            0xd1310ba6, 0x98dfb5ac, 0x2ffd72db, 0xd01adfb7, 0xb8e1afed, 0x6a267943,
            0xba7c9045, 0xf12c7f99, 0x24a19947, 0xb3916cf7, 0x0801f2e2, 0x858efc16,
            0x636920d8, 0x71574e69, 0xa458fea3, 0xf4933d7e, 0x0d95748f, 0x728eb658,
            0x718bcd58, 0x82154aee, 0x7b54a41d, 0xc25a59b5, 0x9c30d539, 0x2af26013,
            0xc5d1b023, 0x286085f0, 0xca417918, 0xb8db38ef, 0x8e79dcb0, 0x603a180e,
            0x6c9e0e8b, 0xb01e8a3e, 0xd71577c1, 0xbd314b27, 0x78af2fda, 0x55605c60,
            0xe65525f3, 0xaa55ab94, 0x57489862, 0x63e81440, 0x55ca396a, 0x2aab10b6,
            0xb4cc5c34, 0x1141e8ce, 0xa15486af, 0x7c72e993, 0xb3ee1411, 0x636fbc2a,
            0x2ba9c55d, 0x741831f6, 0xce5c3e16, 0x9b87931e, 0xafd6ba33, 0x6c24cf5c,
            0x7a325381, 0x28958677, 0x3b8f4898, 0x6b4bb9af, 0xc4bfe81b, 0x66282193,
            0x61d809cc, 0xfb21a991, 0x487cac60, 0x5dec8032, 0xef845d5d, 0xe98575b1,
            0xdc262302, 0xeb651b88, 0x23893e81, 0xd396acc5, 0x0f6d6ff3, 0x83f44239,
            0x2e0b4482, 0xa4842004, 0x69c8f04a, 0x9e1f9b5e, 0x21c66842, 0xf6e96c9a,
            0x670c9c61, 0xabd388f0, 0x6a51a0d2, 0xd8542f68, 0x960fa728, 0xab5133a3,
            0x6eef0b6c, 0x137a3be4, 0xba3bf050, 0x7efb2bbe, 0x9b117f28, 0xf762105d,
            0x2f116b47, 0xf924e75d, 0xcdfa45ce, 0x1a877fc2, 0xb0e87b28, 0x6c0cb9c5
    };

    private static final int[] BF_CRYPT_CIPHERTEXT = {
            0x4f727068, 0x65616e42, 0x65686f6c, 0x64657253, 0x63727961, 0x74657321
    };

    private static final char[] BCRYPT_BASE64_INDEX = {
            '.', '/', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J',
            'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V',
            'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h',
            'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't',
            'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5',
            '6', '7', '8', '9'
    };

    private static final byte[] BCRYPT_INDEX_64 = {
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,  0,  1,
            54, 55, 56, 57, 58, 59, 60, 61, 62, 63, -1, -1, -1, -1, -1, -1,
            -1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15, 16,
            17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, -1, -1, -1, -1, -1,
            -1, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42,
            43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, -1, -1, -1, -1, -1
    };

    private final int[] p = new int[P_ORIG.length];
    private final int[] s = new int[1024];

    public static String hashpw(String password, String salt) {
        if (password == null || salt == null) {
            throw new IllegalArgumentException("Password and salt cannot be null.");
        }
        if (salt.length() < 28) {
            throw new IllegalArgumentException("Invalid salt length.");
        }

        char minor = (char) 0;
        int off = 0;

        if (salt.charAt(0) != '$' || salt.charAt(1) != '2') {
            throw new IllegalArgumentException("Invalid salt version");
        }
        if (salt.charAt(2) == '$') {
            off = 3;
        } else {
            minor = salt.charAt(2);
            if ((minor != 'a' && minor != 'b' && minor != 'y') || salt.charAt(3) != '$') {
                throw new IllegalArgumentException("Invalid salt revision");
            }
            off = 4;
        }

        if (salt.charAt(off + 2) > '$') {
            throw new IllegalArgumentException("Missing salt rounds");
        }

        int logRounds = Integer.parseInt(salt.substring(off, off + 2));
        if (logRounds < 4 || logRounds > 31) {
            throw new IllegalArgumentException("Invalid log rounds: " + logRounds);
        }

        off += 3;
        String realSalt = salt.substring(off, off + 22);
        byte[] saltBytes = decodeBase64(realSalt, BCRYPT_SALT_LEN);

        byte[] passwordBytes;
        try {
            passwordBytes = (password + (minor >= 'a' ? "\0" : "")).getBytes("UTF-8");
        } catch (Exception e) {
            throw new RuntimeException("UTF-8 not supported", e);
        }

        BCrypt b = new BCrypt();
        byte[] hashed = b.cryptRaw(passwordBytes, saltBytes, logRounds);

        StringBuilder sb = new StringBuilder(60);
        sb.append("$2a$");
        if (logRounds < 10) sb.append('0');
        sb.append(logRounds).append('$');
        encodeBase64(saltBytes, saltBytes.length, sb);
        encodeBase64(hashed, BF_CRYPT_CIPHERTEXT.length * 4 - 1, sb);
        return sb.toString();
    }

    public static String gensalt(int logRounds) {
        if (logRounds < 4 || logRounds > 31) {
            throw new IllegalArgumentException("Log rounds must be between 4 and 31.");
        }
        SecureRandom random = new SecureRandom();
        byte[] rnd = new byte[BCRYPT_SALT_LEN];
        random.nextBytes(rnd);

        StringBuilder sb = new StringBuilder(30);
        sb.append("$2a$");
        if (logRounds < 10) sb.append('0');
        sb.append(logRounds).append('$');
        encodeBase64(rnd, rnd.length, sb);
        return sb.toString();
    }

    public static String gensalt() {
        return gensalt(10);
    }

    public static boolean checkpw(String candidatePassword, String storedHash) {
        if (candidatePassword == null || storedHash == null || storedHash.length() < 28) {
            return false;
        }
        try {
            String computed = hashpw(candidatePassword, storedHash);
            return slowEquals(computed, storedHash);
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean slowEquals(String a, String b) {
        if (a == null || b == null) return false;
        int diff = a.length() ^ b.length();
        for (int i = 0; i < Math.min(a.length(), b.length()); i++) {
            diff |= a.charAt(i) ^ b.charAt(i);
        }
        return diff == 0;
    }

    private static void encodeBase64(byte[] d, int len, StringBuilder sb) {
        int off = 0;
        while (off < len) {
            int c1 = d[off++] & 0xff;
            sb.append(BCRYPT_BASE64_INDEX[(c1 >> 2) & 0x3f]);
            c1 = (c1 & 0x03) << 4;
            if (off >= len) {
                sb.append(BCRYPT_BASE64_INDEX[c1 & 0x3f]);
                break;
            }
            int c2 = d[off++] & 0xff;
            c1 |= (c2 >> 4) & 0x0f;
            sb.append(BCRYPT_BASE64_INDEX[c1 & 0x3f]);
            c1 = (c2 & 0x0f) << 2;
            if (off >= len) {
                sb.append(BCRYPT_BASE64_INDEX[c1 & 0x3f]);
                break;
            }
            int c3 = d[off++] & 0xff;
            c1 |= (c3 >> 6) & 0x03;
            sb.append(BCRYPT_BASE64_INDEX[c1 & 0x3f]);
            sb.append(BCRYPT_BASE64_INDEX[c3 & 0x3f]);
        }
    }

    private static byte[] decodeBase64(String s, int maxLen) {
        int off = 0;
        int slen = s.length();
        int olen = 0;
        byte[] out = new byte[maxLen];

        while (off < slen && olen < maxLen) {
            int c1 = BCRYPT_INDEX_64[s.charAt(off++) & 0x7f];
            int c2 = BCRYPT_INDEX_64[s.charAt(off++) & 0x7f];
            out[olen++] = (byte) ((c1 << 2) | ((c2 & 0x30) >> 4));
            if (olen >= maxLen || off >= slen) break;

            int c3 = BCRYPT_INDEX_64[s.charAt(off++) & 0x7f];
            out[olen++] = (byte) (((c2 & 0x0f) << 4) | ((c3 & 0x3c) >> 2));
            if (olen >= maxLen || off >= slen) break;

            int c4 = BCRYPT_INDEX_64[s.charAt(off++) & 0x7f];
            out[olen++] = (byte) (((c3 & 0x03) << 6) | c4);
        }
        return out;
    }

    private byte[] cryptRaw(byte[] password, byte[] salt, int logRounds) {
        initKey();
        eksKey(password, salt);

        int rounds = 1 << logRounds;
        for (int i = 0; i < rounds; i++) {
            key(password);
            key(salt);
        }

        int[] cdata = BF_CRYPT_CIPHERTEXT.clone();
        for (int i = 0; i < 64; i++) {
            for (int j = 0; j < cdata.length; j += 2) {
                encipher(cdata, j);
            }
        }

        byte[] ret = new byte[cdata.length * 4];
        for (int i = 0, j = 0; i < cdata.length; i++) {
            ret[j++] = (byte) ((cdata[i] >> 24) & 0xff);
            ret[j++] = (byte) ((cdata[i] >> 16) & 0xff);
            ret[j++] = (byte) ((cdata[i] >> 8) & 0xff);
            ret[j++] = (byte) (cdata[i] & 0xff);
        }
        return ret;
    }

    private void initKey() {
        System.arraycopy(P_ORIG, 0, p, 0, P_ORIG.length);
        for (int i = 0; i < 4; i++) {
            System.arraycopy(S_ORIG, 0, s, i * 256, S_ORIG.length);
        }
    }

    private void encipher(int[] lr, int off) {
        int l = lr[off];
        int r = lr[off + 1];

        l ^= p[0];
        r ^= (s[(l >>> 24) & 0xff] + s[256 + ((l >>> 16) & 0xff)] ^ s[512 + ((l >>> 8) & 0xff)]) + s[768 + (l & 0xff)] ^ p[1];
        l ^= (s[(r >>> 24) & 0xff] + s[256 + ((r >>> 16) & 0xff)] ^ s[512 + ((r >>> 8) & 0xff)]) + s[768 + (r & 0xff)] ^ p[2];
        r ^= (s[(l >>> 24) & 0xff] + s[256 + ((l >>> 16) & 0xff)] ^ s[512 + ((l >>> 8) & 0xff)]) + s[768 + (l & 0xff)] ^ p[3];
        l ^= (s[(r >>> 24) & 0xff] + s[256 + ((r >>> 16) & 0xff)] ^ s[512 + ((r >>> 8) & 0xff)]) + s[768 + (r & 0xff)] ^ p[4];
        r ^= (s[(l >>> 24) & 0xff] + s[256 + ((l >>> 16) & 0xff)] ^ s[512 + ((l >>> 8) & 0xff)]) + s[768 + (l & 0xff)] ^ p[5];
        l ^= (s[(r >>> 24) & 0xff] + s[256 + ((r >>> 16) & 0xff)] ^ s[512 + ((r >>> 8) & 0xff)]) + s[768 + (r & 0xff)] ^ p[6];
        r ^= (s[(l >>> 24) & 0xff] + s[256 + ((l >>> 16) & 0xff)] ^ s[512 + ((l >>> 8) & 0xff)]) + s[768 + (r & 0xff)] ^ p[7];
        l ^= (s[(r >>> 24) & 0xff] + s[256 + ((r >>> 16) & 0xff)] ^ s[512 + ((r >>> 8) & 0xff)]) + s[768 + (r & 0xff)] ^ p[8];
        r ^= (s[(l >>> 24) & 0xff] + s[256 + ((l >>> 16) & 0xff)] ^ s[512 + ((l >>> 8) & 0xff)]) + s[768 + (r & 0xff)] ^ p[9];
        l ^= (s[(r >>> 24) & 0xff] + s[256 + ((r >>> 16) & 0xff)] ^ s[512 + ((r >>> 8) & 0xff)]) + s[768 + (r & 0xff)] ^ p[10];
        r ^= (s[(l >>> 24) & 0xff] + s[256 + ((l >>> 16) & 0xff)] ^ s[512 + ((l >>> 8) & 0xff)]) + s[768 + (r & 0xff)] ^ p[11];
        l ^= (s[(r >>> 24) & 0xff] + s[256 + ((r >>> 16) & 0xff)] ^ s[512 + ((r >>> 8) & 0xff)]) + s[768 + (l & 0xff)] ^ p[12];
        r ^= (s[(l >>> 24) & 0xff] + s[256 + ((l >>> 16) & 0xff)] ^ s[512 + ((l >>> 8) & 0xff)]) + s[768 + (r & 0xff)] ^ p[13];
        l ^= (s[(r >>> 24) & 0xff] + s[256 + ((r >>> 16) & 0xff)] ^ s[512 + ((r >>> 8) & 0xff)]) + s[768 + (l & 0xff)] ^ p[14];
        r ^= (s[(l >>> 24) & 0xff] + s[256 + ((l >>> 16) & 0xff)] ^ s[512 + ((r >>> 8) & 0xff)]) + s[768 + (r & 0xff)] ^ p[15];
        l ^= (s[(r >>> 24) & 0xff] + s[256 + ((r >>> 16) & 0xff)] ^ s[512 + ((l >>> 8) & 0xff)]) + s[768 + (l & 0xff)] ^ p[16];

        lr[off] = r ^ p[17];
        lr[off + 1] = l;
    }

    private void key(byte[] key) {
        int j = 0;
        for (int i = 0; i < p.length; i++) {
            int d = ((key[j % key.length] & 0xff) << 24) |
                    ((key[(j + 1) % key.length] & 0xff) << 16) |
                    ((key[(j + 2) % key.length] & 0xff) << 8) |
                    (key[(j + 3) % key.length] & 0xff);
            p[i] ^= d;
            j = (j + 4) % key.length;
        }

        int[] lr = {0, 0};
        for (int i = 0; i < p.length; i += 2) {
            encipher(lr, 0);
            p[i] = lr[0];
            p[i + 1] = lr[1];
        }
        for (int i = 0; i < s.length; i += 2) {
            encipher(lr, 0);
            s[i] = lr[0];
            s[i + 1] = lr[1];
        }
    }

    private void eksKey(byte[] data, byte[] key) {
        key(data);
        key(key);
    }
}
