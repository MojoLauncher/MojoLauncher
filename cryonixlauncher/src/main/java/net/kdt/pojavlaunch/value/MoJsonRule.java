package net.kdt.pojavlaunch.value;

import net.kdt.pojavlaunch.Architecture;

public class MoJsonRule {
    public String action;
    public OSDescriptor os;

    public int getPrecedenceLevel() {
        if(os == null) return 1;
        return 1 + os.getPrecedenceLevel();
    }

    public boolean matches() {
        if(os == null) return true;
        else return os.matches();
    }

    public static String ruleSetCheck(MoJsonRule[] rules) {
        int precedenceLevel = 0;
        String action = "disallow";
        for(MoJsonRule rule : rules) {
            int ruleLevel = rule.getPrecedenceLevel();
            if(ruleLevel <= precedenceLevel) {
                continue;
            }
            if(rule.matches()) action = rule.action;
            precedenceLevel = ruleLevel;
        }
        return action;
    }

    public static class OSDescriptor {
        public String name;
        public String version;
        public String arch;

        public int getPrecedenceLevel() {
            int precedence = 0;
            if(name != null) precedence += 1;
            if(version != null) precedence += 2;
            if(arch != null) precedence += 3;
            return precedence;
        }

        private static boolean propertyMatches(String value, String expected) {
            if(value == null) return true;
            return value.equals(expected);
        }

        public boolean matches() {
            // TODO: version matching
            return propertyMatches(name, "linux") &&
                    propertyMatches(arch, Architecture.archAsString(Architecture.getDeviceArchitecture())) &&
                    version == null;
        }
    }
}
