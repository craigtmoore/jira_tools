package common

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import jenkins.UrlToHtml
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.awt.*
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.StringSelection

class Util {

    /**
     * Opens the given file in the default web browser, if possible, otherwise it copies the file path to
     * the clipboard.
     * Adapted from: https://stackoverflow.com/a/25713715/529256
     *
     * @param file the file to open in the browser
     */
    static void openInBrowser(File file) {

        boolean openedFile = false

        URI uri = file.toURI()

        try {

            Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null

            if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {

                System.out.println("\nOpening " + uri + " ...")

                desktop.browse(uri)

                openedFile = true

            }

        } catch (Exception e) {

            System.err.println("Failed to open file, in browser: " + file.getAbsolutePath())

            e.printStackTrace()

        }

        if (!openedFile) {
            // Copy URI to the clipboard so the user can paste it into their browser

            StringSelection stringSelection = new StringSelection(uri.toString())

            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard()

            clipboard.setContents(stringSelection, null)

            System.err.println("Failed to open url: " + uri
                    + "\n>>> The URL has been copied to your clipboard, paste it in your browser to open")

        }
    }

    static CliBuilder getCliBuilder(String usageDescription) {
        def cli = new CliBuilder(usage: usageDescription)
        cli.d(longOpt: 'debug', 'show debug output')
        cli
    }

    static void setLogLevel(Class clazz, Level logLevel) {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory()
        Logger rootLogger = loggerContext.getLogger(clazz)
        rootLogger.setLevel(logLevel)
    }
}
