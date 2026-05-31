package net.kdt.pojavlaunch.value

import net.kdt.pojavlaunch.JMinecraftVersionList.Arguments.ArgValue.ArgRules

object MoJsonRule {
    @JvmStatic
    fun ruleSetCheck(rules: Array<ArgRules?>?): String {
        if (rules == null || rules.isEmpty()) return "allow"
        var result = "disallow"
        for (rule in rules) {
            if (rule == null) continue
            val action = rule.action ?: "allow"
            if (rule.os == null) {
                result = action
            } else if (rule.os!!.name == "android" || rule.os!!.name == "linux") {
                // Pojav identifies as android/linux
                result = action
            }
        }
        return result
    }
}
