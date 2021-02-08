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
 * Class for converting a Jenkins URL into html, jira, or markdown format and copying it to the clipboard which can
 * then be pasted into Jira or Teams or Bitbucket/GitHub.
 */
class ConvertUrl {

    public static final Logger LOG = LoggerFactory.getLogger(ConvertUrl.class)

    static void main(String[] args) {

        def clazz = ConvertUrl.class

        def cli = Util.getCliBuilder("${clazz.name} [options] url")

        cli.m(longOpt: 'markdown', 'Convert to Markdown format')
        cli.j(longOpt: 'jira', 'Convert to Jira format')

        def options = cli.parse(args)

        if (options.h) {
            cli.usage()
            return
        }

        if (options.debug) {
            Util.setLogLevel(clazz, Level.DEBUG)
        }

        String url = Toolkit.defaultToolkit.systemClipboard.getData(DataFlavor.stringFlavor)
        if (options.arguments().isEmpty()) {

            LOG.debug("Copied value from clipboard '$url'")

        } else {

            url = options.arguments()[0]
                    .replace("//", "/")
                    .replace("http:/", "http://")

        }

        LOG.info(url)

        if (!url.startsWith("http")) {

            throw new IllegalArgumentException("Missing 'http' from the URL '$url'")
        }

        if (!url.contains("job")) {

            throw new IllegalArgumentException("Missing 'job' from the URL '$url'")
        }

        LOG.debug("cleanUrl=$url")

        if (options.markdown) {

            def markdownText = getText(url, Format.MARKDOWN)

            Toolkit.defaultToolkit.systemClipboard.setContents(new StringSelection(markdownText), null)

            LOG.debug(markdownText)

            LOG.info("Copied Markdown Format to clipboard")

        } else if (options.jira) {

            def jiraText = getText(url, Format.JIRA)

            Toolkit.defaultToolkit.systemClipboard.setContents(new StringSelection(jiraText), null)

            LOG.debug(jiraText)

            LOG.info("Copied JIRA Format to clipboard")

        } else { // HTML

            def htmlText = getText(url)

            Toolkit.defaultToolkit.systemClipboard.setContents(new HtmlInputStreamTransferable(htmlText), null)

            LOG.info("Copied HTML to clipboard")

        }
    }

    static enum Format {
        HTML,
        MARKDOWN,
        JIRA
    }


    static getText(String url, Format format = Format.HTML) {

        LOG.debug("getHtml(url='$url')")

        def addressArray = url.split("/")

        def htmlFormat = ""

        def runningUrl = ""

        def foundJob = false

        def textFormat = ""

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
                if (format == Format.MARKDOWN) {
                    link = "[$displayText](${runningUrl})"
                } else if (format == Format.JIRA) {
                    link = "[$displayText|$runningUrl]"
                } else {
                    link = "<a href=\"${runningUrl}\">${displayText}</a>"
                }

                if (htmlFormat) {
                    link = " > $link"
                }

                if (textFormat) {
                    displayText = " > $displayText"
                }

                textFormat += displayText

                htmlFormat += link

            } else if ("job" == it) {
                foundJob = true
            }
        }

        LOG.info(textFormat)

        return htmlFormat
    }

    static class HtmlInputStreamTransferable implements Transferable {
        private final DataFlavor htmlDataFlavor
        private final String htmlText

        HtmlInputStreamTransferable(String htmlText) throws ClassNotFoundException {
            this.htmlText = htmlText
            this.htmlDataFlavor = new DataFlavor("text/html")
        }

        DataFlavor[] getTransferDataFlavors() {
            return [htmlDataFlavor]
        }

        boolean isDataFlavorSupported(DataFlavor flavor) {
            return "text/html" == flavor.getMimeType()

        }

        Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
            InputStream stringStream = new ByteArrayInputStream(htmlText.getBytes("utf-8"))
            return stringStream
        }
    }
}
