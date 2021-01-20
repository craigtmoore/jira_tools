package jenkins

import ch.qos.logback.classic.Level
import common.Util
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException

/**
 * Class for converting a Jenkins URL into html (or markdown) format and copying it to the clipboard which can
 * then be pasted into Jira or Teams or Bitbucket/GitHub.
 */
class ConvertUrl {

    public static final Logger LOG = LoggerFactory.getLogger(ConvertUrl.class)

    static void main(String[] args) {

        def clazz = ConvertUrl.class

        def cli = Util.getCliBuilder("${clazz.name} [options] url")

        cli.m(longOpt: 'markdown', 'Convert to Markdown format')

        def options = cli.parse(args)

        if (options.h || !options.arguments()) {
            cli.usage()
            return
        }

        if (options.debug) {
            Util.setLogLevel(clazz, Level.DEBUG)
        }

        def url = options.arguments()[0]
                .replace("//", "/")
                .replace("http:/", "http://")

        LOG.debug("cleanUrl=$url")

        if (options.markdown) {

            def markdownText = getText(url, options.markdown)

            Toolkit.defaultToolkit.systemClipboard.setContents(new StringSelection(markdownText), null)

            LOG.info("Copied Markdown to clipboard")

        } else {

            def htmlText = getText(url)

            Toolkit.defaultToolkit.systemClipboard.setContents(new HtmlInputStreamTransferable(htmlText), null)

            LOG.info("Copied HTML to clipboard")

        }
    }


    static getText(String url, boolean isMarkdown = false) {

        LOG.debug("getHtml(url='$url')")

        def addressArray = url.split("/")

        def htmlFormat = ""

        def runningUrl = ""

        def foundJob = false

        addressArray.each {

            if (runningUrl) {
                runningUrl += "/$it"
            } else {
                runningUrl = it
            }

            if (foundJob && "job" != it && "junit" != it) {

                def displayText = it.replace("%20", " ")

                if (displayText =~ /^[0-9]+$/) {
                    displayText = "#" + displayText
                }

                def link
                if (isMarkdown) {
                    link = "[$displayText](${runningUrl})"
                } else {
                    link = "<a href=\"${runningUrl}\">${displayText}</a>"
                }

                if (htmlFormat) {
                    link = " > $link"
                }

                htmlFormat += "$link"

            } else if ("job" == it) {
                foundJob = true
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
            return "text/html" == flavor.getMimeType()

        }

        Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
            InputStream stringStream = new ByteArrayInputStream(_htmlText.getBytes("utf-8"))
            return stringStream
        }
    }
}