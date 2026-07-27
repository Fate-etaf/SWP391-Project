package com.swp5.library_management.utils;

import java.util.List;
import java.util.Locale;

public class PaymentTokenUtil {

    private static final long XOR_KEY = 0x2D3F5A7B9B8C7D6L; // 60-bit XOR key

    /**
     * Extracts digits from the librarian ID.
     */
    private static long extractLibrarianNumber(String librarianId) {
        if (librarianId == null) return 0;
        StringBuilder sb = new StringBuilder();
        for (char c : librarianId.toCharArray()) {
            if (Character.isDigit(c)) {
                sb.append(c);
            }
        }
        if (sb.length() == 0) return 0;
        try {
            return Long.parseLong(sb.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Generates a very short (10-12 character) secure alphanumeric token.
     * Output format: PAY<base36_hash>
     */
    public static String generateToken(String action, Integer id, String patronId, String librarianId) {
        if (action == null || id == null) {
            throw new IllegalArgumentException("Action and ID cannot be null");
        }
        
        // Map action to 2 bits
        long actionCode = 0;
        switch (action.trim().toUpperCase()) {
            case "T": actionCode = 0; break;
            case "M": actionCode = 1; break;
            case "D": actionCode = 2; break;
            case "F": actionCode = 3; break;
            default: throw new IllegalArgumentException("Invalid action: " + action);
        }

        long libNum = extractLibrarianNumber(librarianId);

        // Pack into 60 bits:
        // actionCode: bits 58-59 (2 bits)
        // id: bits 32-57 (26 bits, up to 67M)
        // libNum: bits 0-31 (32 bits, up to 4.2B)
        long packed = 0L;
        packed |= (actionCode & 3L) << 58;
        packed |= ((long) id & 0x3FFFFFFL) << 32;
        packed |= (libNum & 0xFFFFFFFFL);

        // Obfuscate (swap adjacent odd/even bit pairs inside the 60-bit range)
        long x = packed ^ XOR_KEY;
        long odd = x & 0x5555555555555555L & 0x0FFFFFFFFFFFFFFFL;
        long even = x & 0xAAAAAAAAAAAAAAAAL & 0x0FFFFFFFFFFFFFFFL;
        long obfuscated = (odd << 1) | (even >>> 1);
        obfuscated &= 0x0FFFFFFFFFFFFFFFL;

        // Encode to Base36
        return "PAY" + Long.toString(obfuscated, 36).toUpperCase(Locale.ROOT);
    }

    /**
     * Decodes the payment token back into a DecodedToken.
     * existingLibrarians is the list of User entities (to match librarian numbers).
     */
    public static DecodedToken decodeToken(String tokenStr, List<com.swp5.library_management.entity.User> existingLibrarians) {
        if (tokenStr == null || tokenStr.trim().length() < 4) {
            return null;
        }
        
        String clean = tokenStr.trim().toUpperCase(Locale.ROOT);
        if (!clean.startsWith("PAY")) {
            return null;
        }
        
        try {
            String base36Part = clean.substring(3);
            long obfuscated = Long.parseLong(base36Part, 36);

            // Reverse Obfuscate (swap odd/even bits back)
            long odd = (obfuscated & 0xAAAAAAAAAAAAAAAAL) >>> 1;
            long even = (obfuscated & 0x5555555555555555L) << 1;
            long x = odd | even;
            long packed = (x ^ XOR_KEY) & 0x0FFFFFFFFFFFFFFFL;

            // Unpack
            int actionCode = (int) ((packed >>> 58) & 3L);
            int id = (int) ((packed >>> 32) & 0x3FFFFFFL);
            long libNum = packed & 0xFFFFFFFFL;

            String action;
            switch (actionCode) {
                case 0: action = "T"; break;
                case 1: action = "M"; break;
                case 2: action = "D"; break;
                case 3: action = "F"; break;
                default: return null;
            }

            // Restore librarian ID
            String librarianId = "SYSTEM_AUTO";
            if (libNum > 0 && existingLibrarians != null) {
                String numStr = String.valueOf(libNum);
                for (com.swp5.library_management.entity.User user : existingLibrarians) {
                    String uId = user.getUserId();
                    if (uId != null) {
                        String cleanUId = uId.replaceAll("\\D+", "");
                        if (cleanUId.equals(numStr)) {
                            librarianId = uId;
                            break;
                        }
                    }
                }
            } else if (libNum > 0) {
                librarianId = "LIB" + String.format("%02d", libNum);
            }

            return new DecodedToken(action, id, null, librarianId, System.currentTimeMillis() / 1000L);
        } catch (Exception e) {
            return null;
        }
    }

    public static class DecodedToken {
        private final String action;
        private final Integer id;
        private final String patronId;
        private final String librarianId;
        private final long timestamp;

        public DecodedToken(String action, Integer id, String patronId, String librarianId, long timestamp) {
            this.action = action;
            this.id = id;
            this.patronId = patronId;
            this.librarianId = librarianId;
            this.timestamp = timestamp;
        }

        public String getAction() { return action; }
        public Integer getId() { return id; }
        public String getPatronId() { return patronId; }
        public String getLibrarianId() { return librarianId; }
        public long getTimestamp() { return timestamp; }
    }
}
