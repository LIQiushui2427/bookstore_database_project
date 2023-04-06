package models.utils;

public class verifyInput {
    public static boolean isPositiveInteger(String input) {
        try {
            int number = Integer.parseInt(input);
            return number > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isISBN(String input) {
        return input.matches("\\d{1}-\\d{4}-\\d{4}-\\d{1}");
    }

    public static boolean isValidUid(String uid) {
        // Check if the string is non-empty and has a length of at most 10
        if (uid != null && !uid.isEmpty() && uid.length() <= 10) {
            return true;
        }
        return false;
    }
    
    public static boolean isValidName(String name) {
        // Check if the string is non-empty and has a length of at most 50
        if (name != null && !name.isEmpty() && name.length() <= 50) {
            // Check if the string contains any "%" or "_" characters
            if (!name.contains("%") && !name.contains("_")) {
                return true;
            }
        }
        return false;
    }

    public static boolean isValidAddress(String address) {
        // Check if the string is non-empty and has a length of at most 200
        if (address != null && !address.isEmpty() && address.length() <= 200) {
            // Check if the string contains any "%" or "_" characters
            if (!address.contains("%") && !address.contains("_")) {
                // Check if the string is properly formatted with comma-separated components
                String[] components = address.split(",");
                if (components.length >= 2 && components.length <= 6) {
                    return true;
                }
            }
        }
        return false;
    }
    
    
    
}

