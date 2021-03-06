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
		
		int div = 100; //valeur par d�faut
		int nbPages = doc.getNumberOfPages();
		
		System.out.println(nbPages/((Runtime.getRuntime().maxMemory())/Math.pow(1024, 3)));
		if(nbPages /(Runtime.getRuntime().maxMemory() / (1024 * 1024 * 1024)) >= 200) {
			/*
			 * TODO
			 * Problème de dépassement de mémoire de la JVM lors de la lecture des QRCodes : 
			 * revoir la formule, faire un graphe, montrer les données, benchmark de où ça casse, 
			 * pourquoi et comment
			 */
			div = (int) ((doc.getNumberOfPages() - (doc.getNumberOfPages() / (Runtime.getRuntime().maxMemory() / (1024 * 1024 * 1024) )))/3);

			if(div == 0)
				div = nbPages / 3;
		}
		
		System.out.println(div);

		
		Splitter splitter = new Splitter();
		
		int split = nbPages/4;
		
		if((nbPages / div) > 1)
			split = nbPages / div;
		System.out.println(split);
		splitter.setSplitAtPage(split);
		
		
		
		try {
			List<PDDocument> splittedDocument = splitter.split(doc);
			List<Path> pathsToMiniDocs = new ArrayList<Path>();
			for (int i = 0; i < splittedDocument.size(); i++) {
				/*
				 * TODO PDFBox n'est pas threadsafe Il faudrait essayer de lire un miniDoc par
				 * thread Afin que les threads ne se partagent pas le m�me doc
				 */
				Path temp = Files.createTempFile("miniDoc" + i, ".pdf");
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

			System.out.println(pathsToMiniDocs.size() / 4);

			int j = 0;
			while (j < pathsToMiniDocs.size() / 4) {
				System.out.println(j + "ème tour de boucle");
				PDDocument miniDoc1 = PDDocument.load(pathsToMiniDocs.get((j * 4)).toFile());
				PDDocument miniDoc2 = PDDocument.load(pathsToMiniDocs.get((j * 4) + 1).toFile());
				PDDocument miniDoc3 = PDDocument.load(pathsToMiniDocs.get((j * 4) + 2).toFile());
				PDDocument miniDoc4 = PDDocument.load(pathsToMiniDocs.get((j * 4) + 3).toFile());

				ExecutorService service = Executors.newFixedThreadPool(4);
				CountDownLatch latchThreads = new CountDownLatch(4);

				PDFRenderer pdf1 = new PDFRenderer(miniDoc1);
				PDFRenderer pdf2 = new PDFRenderer(miniDoc2);
				PDFRenderer pdf3 = new PDFRenderer(miniDoc3);
				PDFRenderer pdf4 = new PDFRenderer(miniDoc4);

				service.execute(new ReaderThread(reader, 0, miniDoc1.getNumberOfPages(), pdf1, latchThreads));
				service.execute(new ReaderThread(reader, 0, miniDoc2.getNumberOfPages(), pdf2, latchThreads));
				service.execute(new ReaderThread(reader, 0, miniDoc3.getNumberOfPages(), pdf3, latchThreads));
				service.execute(new ReaderThread(reader, 0, miniDoc4.getNumberOfPages(), pdf4, latchThreads));

				try {
					latchThreads.await();
				} catch (InterruptedException e) {
					System.out.println("PEUT PAS FERMER LES THREADS");
				}
				service.shutdown();

				miniDoc1.close();
				miniDoc2.close();
				miniDoc3.close();
				miniDoc4.close();

				System.gc();

				System.out.println("\n\n\nThread " + j + " fermé\n\n\n");

				j++;
			}

			for (int k = j * 4; k < pathsToMiniDocs.size(); k++) {
				System.out.println("Dernière ligne droite");
				ExecutorService service = Executors.newFixedThreadPool(1);
				CountDownLatch latchThreads = new CountDownLatch(1);

				PDDocument miniDoc = PDDocument.load(pathsToMiniDocs.get(k).toFile());

				PDFRenderer pdf = new PDFRenderer(miniDoc);

				service.execute(new ReaderThread(reader, 0, miniDoc.getNumberOfPages(), pdf, latchThreads));

				try {
					latchThreads.await();
				} catch (InterruptedException e) {
					System.out.println("PEUT PAS FERMER LES THREADS");
				}
				service.shutdown();

				miniDoc.close();

				System.gc();

				System.out.println("Near to close");
			}

		} catch (IOException e) {
			System.out.println("Erreur de split");
		}
		

	}
}