package QRCodeLivrable.QRCodeBasique;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

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

		for (int i = deb; i < fin; i++) {
			try {
				BufferedImage bim = pdf.renderImageWithDPI(i, 350, ImageType.RGB);
				System.out.println(decodeQRCode(bim));

				/*
				 * ************************************ ************************************ ICI
				 * FAIRE LE TRAITEMENT POUR L'AJOUT AU MODELE DES STUDENTSHEETS
				 * ************************************
				 ************************************/
				bim = null;
			} catch (IOException e) {
				System.out.println("NO QR Code in page " + i);
			}

		}
	}

	/**
	 * Méthode qui lis une image afin d'y trouver un QRCode et de renvoyer sa valeur
	 * 
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
		 * DecodeHintType.TRY_HARDER permet de passer d'une dizaine de QRCodes non
		 * reconnus à quelques uns (pour 2000 pages)
		 */

		try {
			MultiFormatReader mfr = new MultiFormatReader();
			mfr.setHints(map);
			Result result = mfr.decodeWithState(bitmap);
			System.out.println(result.getText() + " orientation = " + qrCodeOrientation(result));
			return result.getText();
		} catch (NotFoundException e) {
			return "===================================================\n===================================================\n===================================================";
			/*
			 * FIXME Je ne sais pas pourquoi, des fois des QRCodes ne sont pas trouvés
			 * (alors qu'ils sont bien présents !) Par contre c'est idempotant Possibilit�
			 * que cela vienne des limites de la lib qui analyse les QRCodes
			 */
		}
	}

	/**
	 * Retourne l'orientation d'un QR Code sous la forme d'un angle compris entre ]-180;180]°
	 * @param result Résultat du dédodage du QR Code
	 * @return Orientation du QR Code
	 */
	private double qrCodeOrientation(Result result) {
		ResultPoint[] resultPoints = result.getResultPoints();
		ResultPoint a = resultPoints[1];
		ResultPoint b = resultPoints[2];
		ResultPoint c = resultPoints[0];
		
		float distanceX = b.getX() - a.getX();
		float distanceY = b.getY() - a.getY();
		double angle = Math.atan(distanceY / distanceX) * (180 / Math.PI);
		if (angle > 0 && a.getX() > b.getX() && a.getX() > c.getX()) {
			angle -= 180;
		} else if (angle <= 0 && b.getX() < a.getX() && b.getY() >= a.getY()) {
			angle += 180;
		}
		
		return angle;
	}

	public static void main(String[] args) throws Exception {

		QRCodeReader reader = new QRCodeReader();
		reader.readPDF();
	}
}
