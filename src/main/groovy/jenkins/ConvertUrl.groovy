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

        cli.h(longOpt: 'help', 'Show this help information and exit')
        cli.m(longOpt: 'markdown', 'Convert to Markdown format')
        cli.j(longOpt: 'jira', 'Convert to Jira format')
        cli.f(longOpt: 'file', args: 1, 'A file that contains a list of urls to convert')

        def options = cli.parse(args)

        if (options.h) {
            cli.usage()
            return
        }

        if (options.debug) {
            Util.setLogLevel(clazz, Level.DEBUG)
        }

        File urlFile = options.f ? new File(options.f) : null

        if (urlFile) {

            boolean firstUrl = true

            StringBuilder stringBuilder = new StringBuilder()

            urlFile.eachLine { url ->

                LOG.debug(url)

                url = parseUrl(url)

                LOG.debug("cleanUrl=$url")

                if (options.markdown) {

                    stringBuilder.append("* ").append(getText(url, Format.MARKDOWN)).append("\n")

                } else if (options.jira) {

                    stringBuilder.append("* ").append(getText(url, Format.JIRA)).append("\n")

                } else { // HTML

                    if (firstUrl) {
                        stringBuilder.append("<ul>").append("\n")
                        firstUrl = false
                    }

                    stringBuilder.append("<li>").append(getText(url, Format.HTML)).append("</li>").append("\n")
                }

            }

            if (options.markdown) {

                def markdownText = stringBuilder.toString()

                Toolkit.defaultToolkit.systemClipboard.setContents(new StringSelection(markdownText), null)

                LOG.debug(markdownText)

                LOG.info("Copied Markdown Format to clipboard")

            } else if (options.jira) {

                def jiraText = stringBuilder.toString()

                Toolkit.defaultToolkit.systemClipboard.setContents(new StringSelection(jiraText), null)

                LOG.debug(jiraText)

                LOG.info("Copied JIRA Format to clipboard")

            } else { // HTML

                stringBuilder.append("</ul>")

                def htmlText = stringBuilder.toString()

                Toolkit.defaultToolkit.systemClipboard.setContents(new HtmlInputStreamTransferable(htmlText), null)

                LOG.debug(htmlText)

                LOG.info("Copied HTML Format to clipboard")

            }

        } else {

            String url = Toolkit.defaultToolkit.systemClipboard.getData(DataFlavor.stringFlavor)
            if (options.arguments().isEmpty()) {

                LOG.debug("Copied value from clipboard '$url'")

            } else {

                url = options.arguments()[0]

            }

            LOG.info(url)

            url = parseUrl(url)

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

                def htmlText = getText(url, Format.HTML)

                Toolkit.defaultToolkit.systemClipboard.setContents(new HtmlInputStreamTransferable(htmlText), null)

                LOG.info("Copied HTML to clipboard")

            }
        }
    }

    static String parseUrl(String url) {
        url = url.replace("//", "/")
                .replace("http:/", "http://")

        if (!url.startsWith("http")) {

            throw new IllegalArgumentException("Missing 'http' from the URL '$url'")
        }

        if (!url.contains("job")) {

            throw new IllegalArgumentException("Missing 'job' from the URL '$url'")
        }

        url
    }

    static enum Format {
        HTML,
        MARKDOWN,
        JIRA
    }


    static getText(String url, Format format) {

        LOG.debug("getText(url='$url', format='${format.name()}')")

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
