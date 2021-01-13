package jira

import ch.qos.logback.classic.Level
import common.Util
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.StringSelection

class UpdateIssueLists {

    public static final Logger LOG = LoggerFactory.getLogger(UpdateIssueLists.class)

    static String getJiraQuery(String aString, String bString) {

        aString = aString.replaceAll(" ", "")

        bString = bString.replaceAll(" ", "")

        def aList = Arrays.asList(aString.split(","))
        LOG.debug("aList.size() = ${aList.size()}")

        def aSet = new HashSet<>(aList)
        LOG.debug "aSet.size() = ${aSet.size()}"

        def bList = Arrays.asList(bString.split(","))
        LOG.debug "bList.size() = ${bList.size()}"

        aSet.addAll(bList)
        LOG.debug "aSet.size() = ${aSet.size()}"

        return "key in (${aSet.join(", ")}) ORDER BY created DESC"
    }

    static void main(String[] args) {

        def clazz = UpdateIssueLists.class
        def cli = Util.getCliBuilder("${clazz.name} [options] aList bList")
        def options = cli.parse(args)
        if (options.h || options.arguments().size() < 2) {
            cli.usage()
            return
        }

        if (options.debug) {
            Util.setLogLevel(clazz, Level.DEBUG)
        }


        def jiraQuery = getJiraQuery(options.arguments()[0], options.arguments()[1])

        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard()

        clipboard.setContents(new StringSelection(jiraQuery), null)

        LOG.info "Coped jira query to clipboard"
        LOG.debug jiraQuery

    }
}
