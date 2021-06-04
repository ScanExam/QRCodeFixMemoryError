package QRCodeLivrable.QRCodeBasique;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import org.apache.pdfbox.pdmodel.PDDocument;

import org.apache.pdfbox.rendering.*;

import java.awt.image.*;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class QRCodeReader {

	public void readPDF() throws IOException {
		PDDocument doc = PDDocument.load(new File("8pagesInserted.pdf"));
		
		createThread(doc);
	}
	
	public void createThread(PDDocument doc) {
		
		ReaderThreadManager manager = new ReaderThreadManager(doc, this);
		manager.start();
		
	}
	
	public void readPart(PDFRenderer pdf, int deb, int fin) {
		
		for(int i = deb; i < fin; i++) {
			try {
				BufferedImage bim = pdf.renderImageWithDPI(i, 350, ImageType.RGB);
				System.out.println(decodeQRCode(bim));
				
				/* ************************************
				 * ************************************
				 * ICI FAIRE LE TRAITEMENT POUR L'AJOUT
				 * AU MODELE DES STUDENTSHEETS
				 * ************************************
				 * ************************************/
				bim = null;
			} catch (IOException e) {
				System.out.println("NO QR Code in page " + i);
			}
			
		}
	}
	
	
	/**
	 * Méthode qui lis une image afin d'y trouver un QRCode et de renvoyer sa valeur
	 * @param bim l'image qui contient le QRCode
	 * @return la chaine de caractère lue dans le QRCode
	 * @throws IOException si le buffered image n'est pas valide
	 */
	public String decodeQRCode(BufferedImage bim) throws IOException {
		LuminanceSource source = new BufferedImageLuminanceSource(bim);
		BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

		Map<DecodeHintType, Object> map = new HashMap<DecodeHintType, Object>();
		map.put(DecodeHintType.TRY_HARDER, BarcodeFormat.QR_CODE);
		/*
		 * DecodeHintType.TRY_HARDER permet de passer d'une dizaine de QRCodes non reconnus à quelques uns (pour 2000 pages)
		 */

		try {
			MultiFormatReader mfr = new MultiFormatReader();
			mfr.setHints(map);
			Result result = mfr.decodeWithState(bitmap);
			return result.getText();
		} catch (NotFoundException e) {
			return "===================================================\n===================================================\n===================================================";
			/*
			 * FIXME
			 * Je ne sais pas pourquoi, des fois des QRCodes ne sont pas trouvés
			 * (alors qu'ils sont bien présents !)
			 * Par contre c'est idempotant
			 * Possibilit� que cela vienne des limites de la lib qui analyse les QRCodes
			 */
		}
	}
	

	public static void main(String[] args) throws Exception {
			
		QRCodeReader reader = new QRCodeReader();
		reader.readPDF();
	}
}
