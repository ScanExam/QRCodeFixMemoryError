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

import javafx.util.Pair;

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
			System.out.println(result.getText() + " position = " + qrCodePosition(result, bim.getHeight()));
			System.out.println(result.getText() + " size = " + qrCodeSize(result));
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
	 * Retourne la position d'un QR code en pixel. On considère que le point
	 * d'origine du QR code est en bas à gauche et que les coordonnées (0, 0) sont
	 * sont bas à gauche de la page. Ne marche que si le code à un angle de 0 ou
	 * 180°.
	 * 
	 * @param result      Résultat du dédodage du QR code
	 * @param imageHeight Hauteur de l'image où se trouve le QR code
	 * @return Paire contenant les positions x et y du QR code
	 */
	private Pair<Integer, Integer> qrCodePosition(Result result, float imageHeight) {
		ResultPoint[] resultPoints = result.getResultPoints();
		ResultPoint a = resultPoints[1];
		ResultPoint b = resultPoints[2];
		ResultPoint c = resultPoints[0];
		float x = (b.getX() + a.getX()) / 2;
		float y = (c.getY() + a.getY()) / 2;

		// Point d'origine en bas à gauche
		double widthX2 = Math.pow(b.getX() - a.getX(), 2);
		double widthY2 = Math.pow(b.getY() - a.getY(), 2);
		double width = Math.sqrt(widthX2 + widthY2);
		double heightX2 = Math.pow(c.getX() - a.getX(), 2);
		double heightY2 = Math.pow(c.getY() - a.getY(), 2);
		double height = Math.sqrt(heightX2 + heightY2);
		width /= (4.0f / 3.0f);
		height /= (4.0f / 3.0f);
		x -= width;
		y += height;
		y -= imageHeight;

		return new Pair<Integer, Integer>((int) x, (int) -y);
	}

	/**
	 * Retourne la taille (longueur ou largueur, le QR code est carré) d'un QR code
	 * en pixel.
	 * 
	 * @param result Résultat du dédodage du QR code
	 * @return Taille du QR code
	 */
	private int qrCodeSize(Result result) {
		ResultPoint[] resultPoints = result.getResultPoints();
		ResultPoint a = resultPoints[1];
		ResultPoint b = resultPoints[2];
		ResultPoint c = resultPoints[0];

		double widthX2 = Math.pow(b.getX() - a.getX(), 2);
		double widthY2 = Math.pow(b.getY() - a.getY(), 2);
		float width = (float) Math.sqrt(widthX2 + widthY2);

		double heightX2 = Math.pow(c.getX() - a.getX(), 2);
		double heightY2 = Math.pow(c.getY() - a.getY(), 2);
		float height = (float) Math.sqrt(heightX2 + heightY2);
		width *= 1.5f;
		height *= 1.5f;

		return (int) ((width + height) / 2.0f);
	}

	/**
	 * Retourne l'orientation d'un QR code sous la forme d'un angle compris entre
	 * ]-180;180]°. Plus le QR code est orienté vers la droite plus il gagne de
	 * dégrés.
	 * 
	 * @param result Résultat du dédodage du QR code
	 * @return Orientation du QR code
	 */
	private float qrCodeOrientation(Result result) {
		ResultPoint[] resultPoints = result.getResultPoints();
		ResultPoint a = resultPoints[1];
		ResultPoint b = resultPoints[2];

		float distanceX = b.getX() - a.getX();
		float distanceY = b.getY() - a.getY();
		float angle = (float) (Math.atan(distanceY / distanceX) * (180 / Math.PI));
		if (angle > 0 && a.getX() > b.getX() && a.getY() >= b.getY()) {
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
