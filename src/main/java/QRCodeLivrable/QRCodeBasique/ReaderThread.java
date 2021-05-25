package QRCodeLivrable.QRCodeBasique;

import java.util.concurrent.CountDownLatch;

import org.apache.pdfbox.rendering.PDFRenderer;

public class ReaderThread extends Thread implements Runnable {

	QRCodeReader reader;
	PDFRenderer pdf;
	int deb, fin;
	CountDownLatch countDown;
	
	public ReaderThread(QRCodeReader reader, int deb, int fin, PDFRenderer pdf, CountDownLatch countDown) {
		this.reader = reader;
		this.deb = deb;
		this.fin = fin;
		this.pdf = pdf;
		this.countDown = countDown;
	}
	
	@Override
	public void run() {
		this.reader.readPart(this.pdf, this.deb, this.fin);
		this.countDown.countDown();
		System.gc();
	}
}
