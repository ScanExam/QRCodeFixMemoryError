package QRCodeLivrable.QRCodeBasique;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;

import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

public class QRCodeGenerator {
    private static final String QR_CODE_IMAGE_PATH = "./MyQRCode123.png";

    
    /**
     * @param text Le texte a encoder 
     * @param width  largeur de l'image 
     * @param height Hauteur de l'image
     * @param filePath Chemin du nouveau fichier 
     * 
     * Crée un QRCode (21 *21 carrés) de taille width * height contenant text stocké a filePath
     * 
     * @throws WriterException
     * @throws IOException
     */
    private static void generateQRCodeImage(String text, int width, int height, String filePath)
            throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);

        Path path = FileSystems.getDefault().getPath(filePath);
        MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path);
    }
    
    private static void insertInDoc(PDDocument doc, int nbCopies, int nbPagesSujet) throws IOException, WriterException {
    	
    	for(int i = 0; i < nbCopies; i++) {
    		for(int j = 0; j < nbPagesSujet; j++) {
    			insertQRCodeInPage("template_demo", j, doc, i, nbPagesSujet, QR_CODE_IMAGE_PATH);
    			System.out.println("Inserted in page " + (j + (i * nbPagesSujet)));
    		}
    	}
    	doc.save(new File("8pagesInserted.pdf"));
    }
    
    private static void insertQRCodeInPage(String name, int numPage, PDDocument doc, int numCopie, int nbPagesSujet, String pathImage) throws WriterException, IOException {
		String stringAEncoder = name + "_" + numCopie + "_" + numPage;

		generateQRCodeImage(stringAEncoder, 350, 350, pathImage);

		PDImageXObject pdImage = PDImageXObject.createFromFile(pathImage, doc);
		float scale = 0.3f;

		try(PDPageContentStream contentStream= new PDPageContentStream(doc, doc.getPage(numPage+ ((numCopie*nbPagesSujet))), AppendMode.APPEND, true,
						true)) {

			contentStream.drawImage(pdImage, 10, 10, pdImage.getWidth() * scale, pdImage.getHeight() * scale);

		}
	}
    
    private static void mergingAndInsert(int nombreCopies) throws IOException, WriterException {
    	File f = new File("pfo_example.pdf");
    	
    	PDDocument doc = PDDocument.load(f);
    	
    	int nbPagesSujet = doc.getNumberOfPages();
    	
    	MemoryUsageSetting memUsSett = MemoryUsageSetting.setupMainMemoryOnly();

        PDFMergerUtility ut = new PDFMergerUtility();
        
        for (int i = 0; i < nombreCopies; i++) {
            ut.addSource(f);
        }
    	
    	ut.setDestinationFileName("8pagesMerged.pdf");
    	
    	memUsSett.setTempDir(f);
    	
    	/*
    	 * [main] WARN org.apache.pdfbox.multipdf.PDFMergerUtility - Removed /IDTree from /Names dictionary, doesn't belong there
    	 * Ce warn est spécifique à PDFBox, qui est en réalité un bug de la lib (le tree doit être vérifié et non supprimé)
    	 * Bugfix depuis la version 2.0.23, et nous sommes en 2.0.21
    	 * Ne pose pas un problème lors de l'éxecution, sinon ça ne marcherait pas
    	 * https://stackoverflow.com/questions/66163296/why-i-get-the-warning-message-removed-idtree-from-names-dictionary-doesnt-b
    	 */
                
        ut.mergeDocuments(memUsSett);
        
        doc.close();
        
        PDDocument docMerged = PDDocument.load(new File("8pagesMerged.pdf"));
        
        insertInDoc(docMerged, nombreCopies, nbPagesSujet);
    }

    public static void main(String[] args) {
       try {
		mergingAndInsert(257);
		System.out.println("noice");
	} catch (IOException | WriterException e) {
		System.out.println("rip");
		e.printStackTrace();
	}
    }
}
