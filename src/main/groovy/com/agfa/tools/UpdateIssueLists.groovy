package com.agfa.tools

import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.StringSelection

class UpdateIssueLists {

    static String getJiraQuery(String aString, String bString) {
        aString = aString.replaceAll(" ", "")
        bString = bString.replaceAll(" ", "")

        def aList = Arrays.asList(aString.split(","))
        println "aList.size() = ${aList.size()}"
        def aSet = new HashSet<>(aList)
        println "aSet.size() = ${aSet.size()}"
        def bList = Arrays.asList(bString.split(","))
        println "bList.size() = ${bList.size()}"

        aSet.addAll(bList)
        println "aSet.size() = ${aSet.size()}"

        return "key in (${aSet.join(", ")}) ORDER BY created DESC"
    }

    static void main(String[] args) {

        if (args.length < 2) {
            println "Expected to strings, but saw " + args.length
            System.exit(1)
        }

        def jiraQuery = getJiraQuery(args[0], args[1])

        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

        clipboard.setContents(new StringSelection(jiraQuery), null);

        println "Coped to clipboard:"
        println jiraQuery

    }
}
