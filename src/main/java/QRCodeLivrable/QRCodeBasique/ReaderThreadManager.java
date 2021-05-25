package QRCodeLivrable.QRCodeBasique;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.pdfbox.multipdf.Splitter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

class ReaderThreadManager extends Thread implements Runnable {
	
	public PDDocument doc;
	public QRCodeReader reader;
	
	public ReaderThreadManager(PDDocument doc, QRCodeReader reader) {
		this.doc = doc;
		this.reader = reader;
	}
	
	@Override
	public void run() {
		Splitter splitter = new Splitter();
		splitter.setSplitAtPage(doc.getNumberOfPages()/100);
		try {
			List<PDDocument> splittedDocument = splitter.split(doc);
			List<Path> pathsToMiniDocs = new ArrayList<Path>();
			for(int i = 0; i < splittedDocument.size(); i++) {
				/*
				 * TODO
				 * PDFBox n'est pas threadsafe
				 * Il faudrait essayer de lire un miniDoc par thread
				 * Afin que les threads ne se partagent pas le même doc
				 */
				Path temp = Files.createTempFile("miniDoc"+i, ".pdf");
				pathsToMiniDocs.add(temp);
				System.out.println(temp);
				File fileTemp = temp.toFile();
				fileTemp.deleteOnExit();
				splittedDocument.get(i).save(fileTemp);
				splittedDocument.get(i).close();
			}
			
			this.doc.close();
			splittedDocument = null;
			
			System.gc();
			for(int j = 0; j < pathsToMiniDocs.size(); j++) {
				
				PDDocument miniDoc = PDDocument.load(pathsToMiniDocs.get(j).toFile());
				
				ExecutorService service = Executors.newFixedThreadPool(4);
				CountDownLatch latchThreads = new CountDownLatch(4);
				PDFRenderer pdf = new PDFRenderer(miniDoc);
				
				service.execute(new ReaderThread(reader, 0, (miniDoc.getNumberOfPages() / 4), pdf, latchThreads));
				service.execute(new ReaderThread(reader, (miniDoc.getNumberOfPages() / 4), (miniDoc.getNumberOfPages() / 2), pdf, latchThreads));
				service.execute(new ReaderThread(reader, (miniDoc.getNumberOfPages() / 2), (3 * miniDoc.getNumberOfPages() / 4), pdf, latchThreads));
				service.execute(new ReaderThread(reader, (3 * miniDoc.getNumberOfPages() / 4), miniDoc.getNumberOfPages(), pdf, latchThreads));
				
				try {
					latchThreads.await();
				} catch (InterruptedException e) {
					System.out.println("PEUT PAS FERMER LES THREADS");
				}
				service.shutdown();
				
				miniDoc.close();
				
				System.gc();
				
				System.out.println("\n\n\nThread " + j + " fermÃ©\n\n\n");
			}
				
		} catch (IOException e) {
			System.out.println("Erreur de split");
		}
		
		
	}
}