package net.kdt.pojavlaunch.value;

import net.kdt.pojavlaunch.Architecture;
import java.util.HashMap;
import java.util.Map;

public class MoJsonRule {
    public String action;
    public OSDescriptor os;
    public FeatureDescriptor features;

    public int getPrecedenceLevel() {
        int level = 1;
        if(os != null) level += os.getPrecedenceLevel();
        if(features != null) level += features.getPrecedenceLevel();
        return level;
    }

    public boolean matches(Map<String, Boolean> feats) {
        if(os != null && os.matches()) return true;
        if(features != null && features.matches(feats)) return true;
        return false;
    }

    public static String ruleSetCheck(MoJsonRule[] rules, Map<String, Boolean> features) {
        int precedenceLevel = 0;
        String action = "disallow";
        for(MoJsonRule rule : rules) {
            int ruleLevel = rule.getPrecedenceLevel();
            if(ruleLevel <= precedenceLevel) {
                continue;
            }
            if(rule.matches(features)) action = rule.action;
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

    public static class FeatureDescriptor extends HashMap<String, Boolean> {
        public boolean matches(Map<String, Boolean> features){
            if(features == null) return true;
            for(Entry<String, Boolean> feat : features.entrySet()) {
                if (containsKey(feat.getKey()) && feat.getValue().equals(get(feat.getKey())))
                    return feat.getValue();
            }
            return false;
        }
        public int getPrecedenceLevel(){
            return size();
        }
    }
}
