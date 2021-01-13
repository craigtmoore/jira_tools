package com.agfa.tools

import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException

class LinkToHtml {

    String url

    def run() {

        def addressArray = url.split("/")

        def maxJobNumber = url.contains("ei-master") ? 2 : 3

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

    static void main(String[] args) {

        if (!args) {
            println "Missing url"
            System.exit(1)
        }

        def htmlText = new LinkToHtml(url: args[0]).run()

        def transferable = new HtmlInputStreamTransferable(htmlText)

        Toolkit.defaultToolkit.systemClipboard.setContents(transferable, null)

        println "Copied HTML to clipboard"

    }

    static class HtmlInputStreamTransferable implements Transferable {
        private final DataFlavor _htmlDataFlavor;
        private final String _htmlText;

        HtmlInputStreamTransferable(String htmlText) throws ClassNotFoundException {
            _htmlText = htmlText;
            _htmlDataFlavor = new DataFlavor("text/html");
        }

        DataFlavor[] getTransferDataFlavors() {
            return [_htmlDataFlavor];
        }

        boolean isDataFlavorSupported(DataFlavor flavor) {
            return "text/html".equals(flavor.getMimeType());

        }

        Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
            InputStream stringStream = new ByteArrayInputStream(_htmlText.getBytes("utf-8"));
            return stringStream;
        }
    }


    /**
     * Opens the given file in the default web browser, if possible, otherwise it copies the file path to
     * the clipboard.
     * Adapted from: https://stackoverflow.com/a/25713715/529256
     *
     * @param file the file to open in the browser
     */
    static void openInBrowser(File file) {

        boolean openedFile = false;

        URI uri = file.toURI();

        try {

            Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;

            if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {

                System.out.println("\nOpening " + uri + " ...");

                desktop.browse(uri);

                openedFile = true;

            }

        } catch (Exception e) {

            System.err.println("Failed to open file, in browser: " + file.getAbsolutePath());

            e.printStackTrace();

        }

        if (!openedFile) {
            // Copy URI to the clipboard so the user can paste it into their browser

            StringSelection stringSelection = new StringSelection(uri.toString());

            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

            clipboard.setContents(stringSelection, null);

            System.err.println("Failed to open url: " + uri
                    + "\n>>> The URL has been copied to your clipboard, paste it in your browser to open");

        }
    }

}
