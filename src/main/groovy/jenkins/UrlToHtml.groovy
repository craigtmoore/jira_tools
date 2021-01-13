package jenkins

import ch.qos.logback.classic.Level
import common.Util
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException

/**
 * Class for taking in a Jenkins URL can converting into an html format that can be pasted into Jira or Teams.
 */
class UrlToHtml {

    public static final Logger LOG = LoggerFactory.getLogger(UrlToHtml.class)

    static void main(String[] args) {

        def clazz = UrlToHtml.class
        def cli = Util.getCliBuilder("${clazz.name} [options] url")
        def options = cli.parse(args)

        if (options.h || !options.arguments()) {
            cli.usage()
            return
        }

        if (options.debug) {
            Util.setLogLevel(clazz, Level.DEBUG)
        }

        def htmlText = getHtml(options.arguments()[0])

        def transferable = new HtmlInputStreamTransferable(htmlText)

        Toolkit.defaultToolkit.systemClipboard.setContents(transferable, null)

        LOG.info("Copied HTML to clipboard")

    }


    static getHtml(String url) {

        LOG.debug("getHtml(url='$url')")

        def addressArray = url.split("/")

        def maxJobNumber = url.contains("ei-master") ? 2 : 3

        LOG.debug("maxJobNumber=$maxJobNumber")

        boolean foundJob = false

        int numJobs = 0

        def htmlFormat = ""

        def runningUrl = ""

        addressArray.each {

            if (runningUrl) {
                runningUrl += "/$it"
            } else {
                runningUrl = it
            }

            if (foundJob || numJobs == maxJobNumber) {
                def displayText = it.replace("%20", " ")
                if (displayText =~ /[0-9]+/ && numJobs > 1) {
                    displayText = "#" + displayText
                }
                def link = "<a href=\"${runningUrl}\">${displayText}</a>"
                if (htmlFormat) {
                    link = " > $link"
                }
                htmlFormat += "$link"
                foundJob = false
            }

            if ("job".equals(it)) {
                foundJob = true
                numJobs += 1
            }
        }

        return htmlFormat
    }

    static class HtmlInputStreamTransferable implements Transferable {
        private final DataFlavor _htmlDataFlavor
        private final String _htmlText

        HtmlInputStreamTransferable(String htmlText) throws ClassNotFoundException {
            _htmlText = htmlText
            _htmlDataFlavor = new DataFlavor("text/html")
        }

        DataFlavor[] getTransferDataFlavors() {
            return [_htmlDataFlavor]
        }

        boolean isDataFlavorSupported(DataFlavor flavor) {
            return "text/html".equals(flavor.getMimeType())

        }

        Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
            InputStream stringStream = new ByteArrayInputStream(_htmlText.getBytes("utf-8"))
            return stringStream
        }
    }
}
